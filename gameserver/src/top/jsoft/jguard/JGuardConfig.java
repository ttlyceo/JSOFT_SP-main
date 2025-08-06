package top.jsoft.jguard;

import l2e.gameserver.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by psygrammator
 * group jsoft.top
 */
public class JGuardConfig {
    public static String JGUARD_DIR = "./config/jsoft/jguard/";
    private static final File ConfigurationFile = new File(JGUARD_DIR, "jguard.properties");
    private static final AtomicLong lastModified = new AtomicLong(ConfigurationFile.lastModified());

    public static boolean JGUARD_ENABLED;
    public static boolean JGUARD_DEBUG;
    public static boolean JGUARD_ENABLED_HWID_REQUEST;
    public static boolean JGUARD_KICK_EMPTY_HWID;
    public static boolean JGUARD_BAN_HWID;
    public static int JGUARD_REVISION_NUMBER;
    public static int JGUARD_LIMIT_LOGIN;
    public static boolean BanlistAccountBan;
    public static boolean BanlistAccountAppend;
    public static boolean LogToDatabase;


    public static void load()
    {
        try
        {
            Properties prop = new Properties();
            InputStream is = new FileInputStream(ConfigurationFile);
            prop.load(is);
            is.close();

            var isGuard = Config.PROTECTION.equalsIgnoreCase("JGUARD");

            JGUARD_ENABLED = isGuard && Boolean.parseBoolean(prop.getProperty("JGuardEnabled", "false"));
            JGUARD_DEBUG = isGuard && Boolean.parseBoolean(prop.getProperty("JGuardDebug", "false"));
            JGUARD_ENABLED_HWID_REQUEST = isGuard && Boolean.parseBoolean(prop.getProperty("JGuardEnabledHWIDRequest", "true"));
            JGUARD_KICK_EMPTY_HWID = isGuard && Boolean.parseBoolean(prop.getProperty("JGuardKickEmptyHWID", "true"));
            JGUARD_REVISION_NUMBER = Integer.parseInt(prop.getProperty("JGuardRevisionNumber", "-1"));
            JGUARD_BAN_HWID = Boolean.parseBoolean(prop.getProperty("JGuardBanHWID", "true"));
            JGUARD_LIMIT_LOGIN = Integer.parseInt(prop.getProperty("JGuardLimitSession", "0"));

            BanlistAccountAppend = Boolean.parseBoolean(prop.getProperty("BanlistAccountAppend", "true"));
            BanlistAccountBan = Boolean.parseBoolean(prop.getProperty("BanlistAccountBan", "true"));
            LogToDatabase = Boolean.parseBoolean(prop.getProperty("LogToDatabase", "true"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void reload()
    {
        long modified = ConfigurationFile.lastModified();

        if (lastModified.getAndSet(modified) != modified)
        {
            load();
        }
    }

}
