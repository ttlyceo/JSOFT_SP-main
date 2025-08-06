package top.jsoft.jguard.utils.log;


import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.network.GameClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.jsoft.jguard.JGuardConfig;
import top.jsoft.jguard.manager.session.model.JClientData;
import top.jsoft.jguard.utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


/**
 * @author Akumu
 * @date 05.03.14
 */
@SuppressWarnings("resource")
public class DbLogger
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DbLogger.class);
	static
	{
		
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("SHOW TABLES LIKE 'auth_log'");
			rset = stmt.executeQuery();

			// table does not exist.
			if(!rset.next())
			{
				stmt = con.prepareStatement("CREATE TABLE `auth_log` (\n" +
						"  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
						"  `date` datetime NOT NULL,\n" +
						"  `account` varchar(14) NOT NULL,\n" +
						"  `hwid` varchar(48) NOT NULL,\n" +
                        "  `ip` varchar(16) NOT NULL,\n" +
						"  PRIMARY KEY (`id`)\n" +
						") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

				stmt.execute();
			}
		}
		catch (Exception e)
		{
			GuardLog.logException(e);
		}
		finally
		{
			DBUtils.closeQuietly(con);
			DBUtils.closeQuietly(stmt);
			DBUtils.closeQuietly(rset);
		}
	}

	public static void logAuth(JClientData cd, GameClient client)
	{
        if(!JGuardConfig.LogToDatabase)
            return;
        
        Connection con = null;
		PreparedStatement stmt = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("INSERT INTO auth_log (date, account, hwid, ip) VALUES(NOW(), ?, ?, ?)");
			stmt.setString(1, cd.account);
            stmt.setString(2, cd.hwid.plain);
            stmt.setString(3, client.getIPAddress());
            stmt.execute();
		}
		catch (Exception e)
		{
			LOGGER.error("Error logging auth for client: " + cd);
		}
		finally
		{
			DBUtils.closeQuietly(con);
			DBUtils.closeQuietly(stmt);
		}
		
	}
}
