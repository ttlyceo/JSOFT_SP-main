package top.jsoft.jguard.manager;

import l2e.gameserver.network.GameClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.jsoft.jguard.database.ProtectionDAO;
import top.jsoft.jguard.manager.hwid.HWIDInfo;

import java.util.Map;

/**
 * Created by psygrammator
 * group jsoft.top
 */
public class HWIDListManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(HWIDListManager.class);

    private Map<Integer, HWIDInfo> listHWID;

    private HWIDListManager() {
        load();

        LOGGER.info("HWIDManager: Loaded " + listHWID.size() + " HWIDs");
    }

    public static HWIDListManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void load() {
        listHWID = ProtectionDAO.getInstance().loadHWIDList();
    }

    public void storeHWID(final GameClient client) {
        int counterHWIDInfo = listHWID.size();
        boolean founded = false;
        for (int i = 0; i < listHWID.size(); i++) {
            if (listHWID.get(i).getHWID().equals(client.getHWID())) {
                founded = true;
                counterHWIDInfo = i;
                break;
            }
        }
        final HWIDInfo hInfo = new HWIDInfo(counterHWIDInfo);
        hInfo.setHWID(client.getHWID());
        hInfo.setLogin(client.getLogin());
        ProtectionDAO.getInstance().storeHWID(client, founded);
        if (!founded)
            listHWID.put(counterHWIDInfo, hInfo);

        //LOGGER.info(String.format("Account: [%s] IP: [%s] HWID: [%s]", client.getAccountName(),  client.getIPAddress(), client.getHWID()));
    }

    private static class LazyHolder {
        private static final HWIDListManager INSTANCE = new HWIDListManager();
    }
}
