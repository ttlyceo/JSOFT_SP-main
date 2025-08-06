package top.jsoft.jguard.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author Akumu
 * @date 05.03.14
 */
public class DBUtils
{
	public static final void closeQuietly(Connection con)
	{
		if (con != null)
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
	}

	public static final void closeQuietly(Statement stmt)
	{
		if (stmt != null)
			try
			{
				stmt.close();
			}
			catch (Exception e)
			{
			}
	}

	public static final void closeQuietly(ResultSet rset)
	{
		if (rset != null)
			try
			{
				rset.close();
			}
			catch (Exception e)
			{
			}
	}
}