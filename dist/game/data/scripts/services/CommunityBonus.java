package services;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.impl.AbstractCommunity;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * | Created by psygrammator
 * | @team jsoft
 */
public class CommunityBonus extends AbstractCommunity implements ICommunityBoardHandler {
    @Override
    public String[] getBypassCommands() {
        return new String[] { "_bbsbonusinfo" };
    }

    @Override
    public void onBypassCommand(String bypass, Player player) {
        String[] cmd = bypass.split(":");
        if (cmd[0].startsWith("_bbsbonusinfo")) {
            if (cmd.length > 1) {

                if (!getCheckAndPick(player, Config.STATS_UP_SYSTEM_PRICEID, Config.STATS_UP_SYSTEM_PRICECOUNT, true)) {
                    return;
                }
                switch (cmd[1]) {

                    case "pAtk":
                        changeVar(player, "pAtk", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "mAtk":
                        changeVar(player, "mAtk", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "pDef":
                        changeVar(player, "pDef", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "mDef":
                        changeVar(player, "mDef", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "pAtkSpd":
                        changeVar(player, "pAtkSpd", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "mAtkSpd":
                        changeVar(player, "mAtkSpd", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "mHp":
                        changeVar(player, "mHp", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "mCp":
                        changeVar(player, "mCp", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "mMp":
                        changeVar(player, "mMp", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "runSpd":
                        changeVar(player, "runSpd", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "pCritRate":
                        changeVar(player, "pCritRate", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "mCritRate":
                        changeVar(player, "mCritRate", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "evasion":
                        changeVar(player, "evasion", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "accuracy":
                        changeVar(player, "accuracy", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "WIT":
                        changeVar(player, "WIT", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "INT":
                        changeVar(player, "INT", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "MEN":
                        changeVar(player, "MEN", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "STR":
                        changeVar(player, "STR", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "DEX":
                        changeVar(player, "DEX", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    case "CON":
                        changeVar(player, "CON", Config.STATS_UP_SYSTEM_TIMES);
                        player.broadcastUserInfo(true);
                        break;
                    default:
                        player.sendMessage("WTF???");
                        return;
                }

            }
            double pAtk = player.getPAtk(null);
            double mAtk = player.getMAtk(null, null);
            double pDef = player.getPDef(null);
            double mDef = player.getMDef(null, null);
            double pAtkSpd = player.getPAtkSpd();
            double mAtkSpd = player.getMAtkSpd();
            double maxHp = player.getMaxHp();
            double maxCp = player.getMaxCp();
            double maxMp = player.getMaxMp();
            int evasion = player.getEvasionRate(null);
            int accuracy = player.getAccuracy();

            int WIT = player.getWIT();
            int INT = player.getINT();
            int MEN = player.getMEN();
            int STR = player.getSTR();
            int DEX = player.getDEX();
            int CON = player.getCON();

//            double fireResist = player.calcStat(Element.FIRE.getDefence(), 0.0D, null, null);
//            double windResist = player.calcStat(Element.WIND.getDefence(), 0.0D, null, null);
//            double waterResist = player.calcStat(Element.WATER.getDefence(), 0.0D, null, null);
//            double earthResist = player.calcStat(Element.EARTH.getDefence(), 0.0D, null, null);
//            double holyResist = player.calcStat(Element.HOLY.getDefence(), 0.0D, null, null);
//            double unholyResist = player.calcStat(Element.UNHOLY.getDefence(), 0.0D, null, null);


            double runSpd = player.getRunSpeed();
            double pCritRate = player.getCriticalHit(null, null);
            double mCritRate = player.getMCriticalHit(null, null);

            NumberFormat df = NumberFormat.getInstance(Locale.ENGLISH);
            df.setMaximumFractionDigits(1);
            df.setMinimumFractionDigits(1);

            String msg = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/bonus/index.htm");
            msg = msg.replace("%pAtk%", df.format(pAtk));
            msg = msg.replace("%pAtkPers%", df.format(player.getVarInt("pAtk", 0)));
            msg = msg.replace("%mAtk%", df.format(mAtk));
            msg = msg.replace("%mAtkPers%", df.format(player.getVarInt("mAtk", 0)));
            msg = msg.replace("%pDef%", df.format(pDef));
            msg = msg.replace("%pDefPers%", df.format(player.getVarInt("pDef", 0)));
            msg = msg.replace("%mDef%", df.format(mDef));
            msg = msg.replace("%mDefPers%", df.format(player.getVarInt("mDef", 0)));
            msg = msg.replace("%pAtkSpd%", df.format(pAtkSpd));
            msg = msg.replace("%pAtkSpdPers%", df.format(player.getVarInt("pAtkSpd", 0)));
            msg = msg.replace("%mAtkSpd%", df.format(mAtkSpd));
            msg = msg.replace("%mAtkSpdPers%", df.format(player.getVarInt("mAtkSpd", 0)));

            msg = msg.replace("%mHp%", df.format(maxHp));
            msg = msg.replace("%mCp%", df.format(maxCp));
            msg = msg.replace("%mMp%", df.format(maxMp));
            msg = msg.replace("%mHpPers%", df.format(player.getVarInt("mHp", 0)));
            msg = msg.replace("%mCpPers%", df.format(player.getVarInt("mCp", 0)));
            msg = msg.replace("%mMpPers%", df.format(player.getVarInt("mMp", 0)));

            msg = msg.replace("%runSpd%", df.format(runSpd));
            msg = msg.replace("%pCritRate%", df.format(pCritRate));
            msg = msg.replace("%mCritRate%", df.format(mCritRate));
            msg = msg.replace("%runSpdPers%", df.format(player.getVarInt("runSpd", 0)));
            msg = msg.replace("%pCritPers%", df.format(player.getVarInt("pCritRate", 0)));
            msg = msg.replace("%mCritPers%", df.format(player.getVarInt("mCritRate", 0)));

            msg = msg.replace("%evasion%", df.format(evasion));
            msg = msg.replace("%evasionPers%", df.format(player.getVarInt("evasion", 0)));
            msg = msg.replace("%accuracy%", df.format(accuracy));
            msg = msg.replace("%accuracyPers%", df.format(player.getVarInt("accuracy", 0)));

            msg = msg.replace("%wit%", df.format(WIT));
            msg = msg.replace("%witPers%", df.format(player.getVarInt("WIT", 0)));

            msg = msg.replace("%men%", df.format(MEN));
            msg = msg.replace("%menPers%", df.format(player.getVarInt("MEN", 0)));

            msg = msg.replace("%int%", df.format(INT));
            msg = msg.replace("%intPers%", df.format(player.getVarInt("INT", 0)));

            msg = msg.replace("%str%", df.format(STR));
            msg = msg.replace("%strPers%", df.format(player.getVarInt("STR", 0)));

            msg = msg.replace("%dex%", df.format(DEX));
            msg = msg.replace("%dexPers%", df.format(player.getVarInt("DEX", 0)));

            msg = msg.replace("%con%", df.format(CON));
            msg = msg.replace("%conPers%", df.format(player.getVarInt("CON", 0)));

            msg = msg.replace("%price%", String.format("%d %s", Config.STATS_UP_SYSTEM_PRICECOUNT, ItemsParser.getInstance().getTemplate(Config.STATS_UP_SYSTEM_PRICEID).getName(null)));

            separateAndSend(msg, player);
        }
    }

    @Override
    public void onWriteCommand(String bypass, String arg1, String arg2, String arg3, String arg4, String arg5, Player player) {

    }

    public static boolean getCheckAndPick(Player player, int itemid, long count, boolean sendMessage) {
        if (count == 0L) {
            return true;
        }
        if (player.getInventory().getInventoryItemCount(itemid, -1) >= count) {
            player.getInventory().destroyItemByItemId(itemid, count, "getCheckAndPick");
        } else {
            if (sendMessage) {
                enoughtItem(player, itemid, count);
            }
            return false;
        }
        if (sendMessage) {
            final ServerMessage serverMessage = new ServerMessage("util.getpay", player.getLang());
            serverMessage.add(Util.formatPay(player, count, itemid));
            player.sendMessage(serverMessage.toString());
        }
        return true;
    }

    protected static void enoughtItem(Player player, int itemid, long count) {
        final ServerMessage serverMessage = new ServerMessage("util.enoughItemCount", player.getLang());
        serverMessage.add(Util.formatPay(player, count, itemid));
        player.sendPacket(new ExShowScreenMessage(serverMessage.toString(), 5000));
        player.sendMessage(serverMessage.toString());
    }


    public void changeVar(Player player, String variables, long period) {
        long times = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(period);
        if (player.getVar(variables) == null) {
            player.setVar(variables, 1, times);
        }
        else if (player.getVarInt(variables) == 100) {
            player.sendMessage(new ServerMessage("common.Error", player.getLang()).toString());
        }
        else {
            player.setVar(variables, player.getVarInt(variables) + 1, times);
        }
    }

    public static void main(String[] args)
    {
        if(Config.STATS_UP_SYSTEM && CommunityBoardHandler.getInstance().getHandler("_bbsbonusinfo") == null)
            CommunityBoardHandler.getInstance().registerHandler(new CommunityBonus());
    }
}
