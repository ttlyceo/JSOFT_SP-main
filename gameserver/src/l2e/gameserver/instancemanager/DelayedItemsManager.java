package l2e.gameserver.instancemanager;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author psygrammator
 */
public class DelayedItemsManager implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DelayedItemsManager.class);
    private static final Object _lock = new Object();
    private static DelayedItemsManager _instance;

    private int last_payment_id;

    public DelayedItemsManager() {
        last_payment_id = 0;
        Connection con = null;
        try {
            con = DatabaseFactory.getInstance().getConnection();
            last_payment_id = get_last_payment_id(con);
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            DbUtils.closeQuietly(con);
        }
        ThreadPoolManager.getInstance().schedule(this, 10000L);
    }

    public static DelayedItemsManager getInstance() {
        if (_instance == null) {
            _instance = new DelayedItemsManager();
        }
        return _instance;
    }

    private int get_last_payment_id(final Connection con) {
        PreparedStatement st = null;
        ResultSet rset = null;
        int result = last_payment_id;
        try {
            st = con.prepareStatement("SELECT MAX(payment_id) AS last FROM items_delayed");
            rset = st.executeQuery();
            if (rset.next()) {
                result = rset.getInt("last");
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            DbUtils.closeQuietly(st, rset);
        }
        return result;
    }

    @Override
    public void run() {
        Player player;
        Connection con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = DatabaseFactory.getInstance().getConnection();
            final int last_payment_id_temp = get_last_payment_id(con);
            if (last_payment_id_temp != last_payment_id) {
                synchronized (_lock) {
                    st = con.prepareStatement("SELECT DISTINCT owner_id FROM items_delayed WHERE payment_status=0 AND payment_id > ?");
                    st.setInt(1, last_payment_id);
                    rset = st.executeQuery();
                    while (rset.next()) {
                        if ((player = GameObjectsStorage.getPlayer(rset.getInt("owner_id"))) != null) {
                            loadDelayed(player, true);
                        }
                    }
                    last_payment_id = last_payment_id_temp;
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            DbUtils.closeQuietly(con, st, rset);
        }
        ThreadPoolManager.getInstance().schedule(this, 10000L);
    }

    public int loadDelayed(final Player player, final boolean notify) {
        if (player == null) {
            return 0;
        }
        final int player_id = player.getObjectId();
        final PcInventory inv = player.getInventory();
        if (inv == null) {
            return 0;
        }
        int restored_counter = 0;
        Connection con = null;
        PreparedStatement st = null;
        PreparedStatement st_delete = null;
        ResultSet rset = null;
        synchronized (_lock) {
            try {
                con = DatabaseFactory.getInstance().getConnection();
                st = con.prepareStatement("SELECT * FROM items_delayed WHERE owner_id=? AND payment_status=0");
                st.setInt(1, player_id);
                rset = st.executeQuery();
                st_delete = con.prepareStatement("UPDATE items_delayed SET payment_status=1 WHERE payment_id=?");
                while (rset.next()) {
                    final int ITEM_ID = rset.getInt("item_id");
                    final long ITEM_COUNT = rset.getLong("count");
                    final int ITEM_ENCHANT = rset.getInt("enchant_level");
                    final int PAYMENT_ID = rset.getInt("payment_id");
                    final int FLAGS = rset.getInt("flags");
                    final boolean stackable = ItemsParser.getInstance().getTemplate(ITEM_ID).isStackable();
                    boolean success = false;
                    for (int i = 0; i < (stackable ? 1L : ITEM_COUNT); ++i) {
                        final ItemInstance item = ItemsParser.getInstance().createItem(ITEM_ID);
                        if (item.isStackable()) {
                            item.setCount(ITEM_COUNT);
                        } else {
                            item.setEnchantLevel(ITEM_ENCHANT);
                        }
                        item.setItemLocation(ItemInstance.ItemLocation.INVENTORY);
                        //item.setCustomFlags(FLAGS);
                        if (ITEM_COUNT > 0L) {
                            final ItemInstance newItem = inv.addItem(item, "DelayedAdd");
                            if (newItem == null) {
                                LOGGER.warn("Unable to delayed create item " + ITEM_ID + " request " + PAYMENT_ID);
                                continue;
                            }
                        }
                        success = true;
                        ++restored_counter;
                        if (notify && ITEM_COUNT > 0L) {
                            player.sendPacket(SystemMessage.obtainItems(ITEM_ID, stackable ? ITEM_COUNT : 1L, ITEM_ENCHANT));
                        }
                        player.sendMessage((new ServerMessage("DelayedItemsManager.ItemSendMessage", player.getLang())).toString());
                    }
                    if (!success) {
                        continue;
                    }
//                    Log.add("<add owner_id=" + player_id + " item_id=" + ITEM_ID + " count=" + ITEM_COUNT + " enchant_level=" + ITEM_ENCHANT + " payment_id=" + PAYMENT_ID + "/>", "delayed_add");
                    st_delete.setInt(1, PAYMENT_ID);
                    st_delete.execute();
                }
            } catch (Exception e) {
                LOGGER.error("Could not load delayed items for player " + player + "!", e);
            } finally {
                DbUtils.closeQuietly(st_delete);
                DbUtils.closeQuietly(con, st, rset);
            }
        }
        return restored_counter;
    }

    public void addDelayed(final int ownerObjId, final int itemTypeId, final int amount, final int enchant, final String desc) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DatabaseFactory.getInstance().getConnection();
            pstmt = con.prepareStatement("INSERT INTO \t`items_delayed`\t(\t`payment_id`, \t\t`owner_id`, \t\t`item_id`, \t\t`count`, \t\t`enchant_level`, \t\t`flags`, \t\t`payment_status`, \t\t`description`\t) SELECT \tMAX(`payment_id`) + 1, \t?, ?, ?, ?, 0, 0, ? \tFROM `items_delayed`");
            pstmt.setInt(1, ownerObjId);
            pstmt.setInt(2, itemTypeId);
            pstmt.setInt(3, amount);
            pstmt.setInt(4, enchant);
            pstmt.setString(5, desc);
            pstmt.executeUpdate();
        } catch (SQLException se) {
            LOGGER.error("Could not add delayed items " + itemTypeId + " " + amount + "(+" + enchant + ") for objId " + ownerObjId + " desc \"" + desc + "\" !", se);
        } finally {
            DbUtils.closeQuietly(con, pstmt);
        }
    }
}
