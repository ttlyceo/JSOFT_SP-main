package top.jsoft.jguard;

import l2e.gameserver.handler.admincommandhandlers.AdminCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.serverpackets.LoginFail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.jsoft.jguard.commands.AdminHWID;
import top.jsoft.jguard.commands.HttpCMD;
import top.jsoft.jguard.commands.JGuardMenu;
import top.jsoft.jguard.database.ProtectionDAO;
import top.jsoft.jguard.manager.HWIDBanManager;
import top.jsoft.jguard.manager.HWIDListManager;
import top.jsoft.jguard.manager.bans.JBanManager;
import top.jsoft.jguard.manager.bans.model.JBan;
import top.jsoft.jguard.manager.session.JClientSessionManager;
import top.jsoft.jguard.manager.session.model.HWID;
import top.jsoft.jguard.manager.session.model.JClientData;
import top.jsoft.jguard.manager.session.model.JClientSession;
import top.jsoft.jguard.utils.log.DbLogger;
import top.jsoft.jguard.utils.log.GuardLog;

import java.util.Map;

/**
 * Created by psygrammator
 * group jsoft.top
 */
public class JGuard {
    public static final String DEFAULT_HWID = "DEFAULT_HWID_STRING";
    private static final Logger LOGGER = LoggerFactory.getLogger(JGuard.class);
    private static Map<String, String> procBufInfo;

    public static void load()
    {
        JGuardConfig.load();
        if(JGuard.isProtectEnabled())
        {
            LOGGER.info("=[Game Guard]=========================================");
            LOGGER.info("JGuardHelper: Init ...");
            if(JGuardConfig.JGUARD_ENABLED_HWID_REQUEST)
                LOGGER.info("JGuardHelper: HWID activate.");
            HWIDListManager.getInstance();
            HWIDBanManager.getInstance();
            JBanManager.store();
            JGuard.loadProcBufInfo();
            AdminCommandHandler.getInstance().registerHandler(new AdminHWID());
            VoicedCommandHandler.getInstance().registerHandler(new HttpCMD());
            AdminCommandHandler.getInstance().registerHandler(new JGuardMenu());
        }
    }
    public static JGuard getInstance() {
        return LazyHolder.INSTANCE;
    }

    public static boolean isProtectEnabled() {
        return JGuardConfig.JGUARD_ENABLED;
    }

    public void enterToServer(final GameClient client, final String HWID) {
        if (isProtectEnabled() && JGuardConfig.JGUARD_ENABLED_HWID_REQUEST) {
            if (!HWID.isEmpty()) {
                switch (HWIDBanManager.getInstance().checkIsHWIDBanned(client)) {
                    case NONE:
                        HWIDListManager.getInstance().storeHWID(client);
                        break;
                    case ACCOUNT_BAN:
                        client.close(new LoginFail(LoginFail.INCORRECT_ACCOUNT_INFO_CONTACT_CUSTOMER_SUPPORT));
                        break;
                    case PLAYER_BAN: //TODO[K] - додумать вариант с ограничением доступа на перса
                        HWIDListManager.getInstance().storeHWID(client);
                        //client.close(new LoginFail(LoginFail.INCORRECT_ACCOUNT_INFO_CONTACT_CUSTOMER_SUPPORT));
                        break;
                }
            } else if (!JGuardConfig.JGUARD_KICK_EMPTY_HWID)
                client.setHWID(DEFAULT_HWID);
            else {
                LOGGER.info("Account: " + client.getLogin() + " not verified in HWID! Closed connection.");
                client.close(new LoginFail(LoginFail.INCORRECT_ACCOUNT_INFO_CONTACT_CUSTOMER_SUPPORT));
            }
        }
    }

    public void storeProcBufInfo(final String login, final String buf) {
        if (procBufInfo.get(login) == null) {
            procBufInfo.put(login, buf);
            ProtectionDAO.getInstance().storeProcBufInfo(login, buf);
        } else if (!procBufInfo.get(login).equalsIgnoreCase(buf)) {
            procBufInfo.replace(login, buf);
            ProtectionDAO.getInstance().updateProcBufInfo(login, buf);
        }
    }

    public static void loadProcBufInfo() {
        procBufInfo = ProtectionDAO.getInstance().loadProcBufInfo();
    }

    private static JBan checkBan(final JClientData cd)
    {
        if (cd == null) return null;

        JBan ban = JBanManager.getBan(cd.hwid);

        if (ban == null)
        {
            JBan accountBan = JBanManager.checkAccount(cd.account);
            if (JGuardConfig.BanlistAccountBan && accountBan != null)
            {
                LOGGER.warn(String.format("HWID [%s] tried to log-in to banned account [%s].", cd.hwid.plain, cd.account));
                return accountBan;
            }
        }
        else
        {
            if (JGuardConfig.BanlistAccountAppend && !ban.findAccount(cd.account))
            {
                ban.addAccount(cd.account);
                LOGGER.warn(String.format("Banned HWID [%s] added account [%s] to his banlist.", cd.hwid.plain, cd.account));
            }

            LOGGER.warn(String.format("Banned HWID [%s] tried login to account [%s]", cd.hwid, cd.account));
            return ban;
        }

        return null;
    }


    //public static MsgPacket checkClient(GameClientState client, byte[] data)
    //public static IOMsgPacket checkClientAuth(GameClient client,String _acc,int cheat)
    public static boolean checkClientAuth(GameClient client, String _acc)
    {
        // перезагрузка конфига, если это потребуется
        JGuardConfig.reload();

        if(client.getIPAddress().equalsIgnoreCase("WTF?"))
        {
            LOGGER.warn("Account: " + _acc + " have null connection.");
            return false;
        }
        String account;
        HWID hwid;
        short langId = 0;

        hwid = HWID.fromString(client.getHWID());
        account = _acc;

        JClientData cd = new JClientData(hwid, account, langId);
        //IOClientData cd = new IOClientData(client.getHWid(), client.getAccountName(), langId);


        // проверка на бан
        JBan ban = checkBan(cd);
        if (ban != null)
        {
            if (ban.type == JBan.Type.TEMP)
            {
                //return IOMsgPacket.MsgType.TEMP_BAN.paket;
                long bannedUntil = (ban.bannedUntil - System.currentTimeMillis()) / 60000;
                if(bannedUntil<1)
                {
                    bannedUntil = 1;
                }
                //String text = String.format("You are banned for [%d] minutes.", bannedUntil);
                //IOMsgPacket msg = new IOMsgPacket(IOMsgPacket.MsgType.TEMP_BAN, text,true);
                return false;
            }
            else
            {
                return false;
                //return IOMsgPacket.MsgType.BANNED_ALREADY.paket;
            }
        }

        // привязываем данные к клиенту
        JClientSessionManager.setClientData(client, cd);

        // ищем сессию
        JClientSession session = JClientSessionManager.getSession(cd);

        // первый вход с компьютера? создаем новую сессию...
        if (session == null)
        {
            session = new JClientSession(cd);
            JClientSessionManager.putSession(session); // добавляем сессию в общий пул
            //IOClientSessionManager.putOLDSession(session); // добавляем сессию в общий пул
        }

        // возможен ли вход еще одного окна на сервер?
        if (!session.canLogin())
        {
            LOGGER.info(String.format("Client [%s] has reached maximum number of game instances. (%d)", cd.account, session.getCount()));
            return false;
        }

        DbLogger.logAuth(cd, client);
        GuardLog.logAuth(cd, client);

        client.setHWID(cd.hwid.plain);

        // добавляем клиента в сессию
        session.addClient(cd, client);

        return true;
    }

    public enum GameGuardResponse {
        NONE,
        NORMAL_RESPONSE,
        KICK_RESPONSE,
        USED_BOT_RESPONS,
        GET_SN_IS_FALSE_RESPONSE,
        SN_NULL_LENGHT_RESPONSE,
        SP_OBJECT_CHANGED_RESPONSE,
        REQUEST_REVISION_VALIDATE,
        NOT_VALID_HOST_INFO
    }

    public enum BotResponse {
        NONE,
        L2TOWER,
        ADRENALIN,
        OTHER_SOFT,
        OTHER
    }

    private static class LazyHolder {
        private static final JGuard INSTANCE = new JGuard();
    }
}
