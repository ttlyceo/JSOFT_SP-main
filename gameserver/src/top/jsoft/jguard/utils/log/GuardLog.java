package top.jsoft.jguard.utils.log;

import l2e.gameserver.network.GameClient;
import org.slf4j.LoggerFactory;
import top.jsoft.jguard.manager.session.model.JClientData;


/**
 * @author Akumu
 * @date 10.01.14
 */
public class GuardLog
{
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GuardLog.class);
    //private static final Logger _log = Logger.getLogger("GENERAL");
    //private static final Logger _logAuth = Logger.getLogger("AUTH");

    private static final LogFormatter _format = new LogFormatter();

    static
	{
		/*try
		{
			FileHandler general = new FileHandler(JGuardConfig.JGUARD_DIR + "logs/general.log", true);
            general.setFormatter(_format);

			_log.addHandler(general);

            FileHandler auth = new FileHandler(JGuardConfig.JGUARD_DIR + "logs/auth.log", true);
            auth.setFormatter(_format);

            
            _logAuth.addHandler(auth);
		}
		catch (Exception e)
		{
			//_log.warning("Error! Log handler could not be created!");
			LOGGER.error("Error! Log handler could not be created!");
		}*/
	}

	/*public static Logger getLogger()
	{
		return _log;
	}*/

    public static synchronized void logAuth(JClientData cd, GameClient client)
    {
        //if(JGuardConfig.LogToFile)
		//{
		//	_logAuth.info(String.format("Account [%s] / IP [%s] / HWID [%s]", cd.account, client.getIPAddress(), cd.hwid.plain));
		//}
		//else
		{
			LOGGER.info(String.format("Account [%s] / IP [%s] / HWID [%s]", cd.account, client.getIPAddress(), cd.hwid.plain));
		}
    }

	public static void logException(Exception e)
	{
		LOGGER.error("Exception occurred: " + e);
	}
}
