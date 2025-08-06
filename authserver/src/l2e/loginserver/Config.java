/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 *
 */
package l2e.loginserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.net.IPSettings;
import l2e.commons.util.Files;
import l2e.commons.util.LoginSettings;
import l2e.commons.util.Rnd;
import l2e.loginserver.crypt.PasswordHash;
import l2e.loginserver.crypt.ScrambledKeyPair;

public class Config
{
	private static final Logger _log = LoggerFactory.getLogger(Config.class);

	public static final String PERSONAL_FILE = "./config/network/personal.ini";
	public static final String LOGIN_CONFIGURATION_FILE = "./config/network/loginserver.ini";
	public static final String SERVER_NAMES_FILE = "./config/servername.xml";
	private final static HashMap<String, String> _personalConfigs = new HashMap<>();

	public static File DATAPACK_ROOT;
	public static long GAME_SERVER_PING_DELAY;
	public static int GAME_SERVER_PING_RETRY;
	public static String DATABASE_DRIVER;
	public static int DATABASE_MAX_CONNECTIONS;
	public static String DATABASE_URL;
	public static String DATABASE_LOGIN;
	public static String DATABASE_PASSWORD;
	public static String DEFAULT_PASSWORD_HASH;
	public static String LEGACY_PASSWORD_HASH;
	public static int LOGIN_BLOWFISH_KEYS;
	public static int LOGIN_RSA_KEYPAIRS;
	public static boolean ACCEPT_NEW_GAMESERVER;
	public static boolean AUTO_CREATE_ACCOUNTS;
	public static String ANAME_TEMPLATE;
	public static String APASSWD_TEMPLATE;
	public static final Map<Integer, String> SERVER_NAMES = new HashMap<>();
	public static int LOGIN_TRY_BEFORE_BAN;
	public static long LOGIN_TRY_TIMEOUT;
	public static long IP_BAN_TIME;
	private static ScrambledKeyPair[] _keyPairs;
	private static byte[][] _blowfishKeys;
	public static PasswordHash DEFAULT_CRYPT;
	public static PasswordHash[] LEGACY_CRYPT;
	public static boolean CHEAT_PASSWORD_CHECK;
	public static boolean ALLOW_ENCODE_PASSWORD;
	public static boolean SHOW_LICENCE;
	public static boolean LOGIN_SERVER_SCHEDULE_RESTART;
	public static long LOGIN_SERVER_SCHEDULE_RESTART_TIME;
	public static ProxyServerConfig[] PROXY_SERVERS_CONFIGS;
	public static double FAKE_ONLINE;
	public static int FAKE_ONLINE_MULTIPLIER;
	public static List<String> WHITE_IPS = new ArrayList<>();

	public static void load()
	{
		_log.info("Loading configs...");
		final InputStream is = null;
		try
		{
			loadPersonalSettings(_personalConfigs);
			IPSettings.getInstance().loadLoginSettings();
			loadConfiguration(is);
			loadServerProxies();
			loadServerNames();
		}
		finally
		{
			try
			{
				if (is != null)
				{
					is.close();
				}
			}
			catch (final Exception e)
			{}
		}
	}

	public final static void initCrypt() throws Exception
	{
		DEFAULT_CRYPT = new PasswordHash(Config.DEFAULT_PASSWORD_HASH);
		final List<PasswordHash> legacy = new ArrayList<>();
		for(final String method : Config.LEGACY_PASSWORD_HASH.split(";"))
		{
			if(!method.equalsIgnoreCase(Config.DEFAULT_PASSWORD_HASH))
			{
				legacy.add(new PasswordHash(method));
			}
		}
		LEGACY_CRYPT = legacy.toArray(new PasswordHash[legacy.size()]);

		_log.info("Loaded " + Config.DEFAULT_PASSWORD_HASH + " as default crypt.");

		_keyPairs = new ScrambledKeyPair[Config.LOGIN_RSA_KEYPAIRS];

		final KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
		final RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
		keygen.initialize(spec);

		for(int i = 0; i < _keyPairs.length; i++)
		{
			_keyPairs[i] = new ScrambledKeyPair(keygen.generateKeyPair());
		}

		_log.info("Cached " + _keyPairs.length + " KeyPairs for RSA communication...");

		_blowfishKeys = new byte[Config.LOGIN_BLOWFISH_KEYS][16];

		for(int i = 0; i < _blowfishKeys.length; i++)
		{
			for(int j = 0; j < _blowfishKeys[i].length; j++)
			{
				_blowfishKeys[i][j] = (byte) (Rnd.get(255) + 1);
			}
		}

		_log.info("Restored " + _blowfishKeys.length + " keys for Blowfish communication...");
	}

	private static void loadPersonalSettings(HashMap<String, String> map)
	{
		map.clear();
		final Pattern LINE_PATTERN = Pattern.compile("^(((?!=).)+)=(.*?)$");
		Scanner scanner = null;
		try
		{
			final File file = new File(PERSONAL_FILE);
			final String content = Files.readFile(file);
			scanner = new Scanner(content);
			
			String line;
			while (scanner.hasNextLine())
			{
				line = scanner.nextLine();
				if (line.startsWith("#"))
				{
					continue;
				}
				
				final Matcher m = LINE_PATTERN.matcher(line);
				if (m.find())
				{
					final String name = m.group(1).replaceAll(" ", "");
					final String value = m.group(3).replaceAll(" ", "");
					map.put(name, value);
				}
			}
		}
		catch (final IOException e1)
		{
			_log.warn("Config: " + e1.getMessage());
			throw new Error("Failed to Load " + PERSONAL_FILE + " File.");
		}
		finally
		{
			try
			{
				scanner.close();
			}
			catch (final Exception e)
			{}
		}
	}
	
	public final static void loadServerNames()
	{
		SERVER_NAMES.clear();
		try
		{
			final File file = new File(SERVER_NAMES_FILE);
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);
			
			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					for (Node list = n1.getFirstChild(); list != null; list = list.getNextSibling())
					{
						if (list.getNodeName().equalsIgnoreCase("server"))
						{
							final Integer id = Integer.valueOf(list.getAttributes().getNamedItem("id").getNodeValue());
							final String name = list.getAttributes().getNamedItem("name").getNodeValue();
							SERVER_NAMES.put(id, name);
						}
					}
				}
			}
			_log.info("Loaded " + SERVER_NAMES.size() + " server name(s).");
		}
		catch (
		    NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			_log.warn("servername.xml could not be initialized.", e);
		}
		catch (
		    IOException | IllegalArgumentException e)
		{
			_log.warn("IOException or IllegalArgumentException.", e);
		}
	}

	public static void loadConfiguration(InputStream is)
	{
		try
		{
			final LoginSettings serverSettings = new LoginSettings();
			is = new FileInputStream(new File(LOGIN_CONFIGURATION_FILE));
			serverSettings.load(is);
			
			try
			{
				DATAPACK_ROOT = new File(serverSettings.getProperty("DatapackRoot", ".").replaceAll("\\\\", "/")).getCanonicalFile();
			}
			catch (final IOException e)
			{
				_log.warn("Error setting datapack root!", e);
				DATAPACK_ROOT = new File(".");
			}
			DATABASE_DRIVER = serverSettings.getProperty("Driver", "org.mariadb.jdbc.Driver");
			DATABASE_URL = serverSettings.getProperty("URL", "jdbc:mariadb://l2e?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC");
			DATABASE_LOGIN = serverSettings.getProperty("Login", "root");
			DATABASE_PASSWORD = serverSettings.getProperty("Password", "");
			DATABASE_MAX_CONNECTIONS = Integer.parseInt(serverSettings.getProperty("MaximumDbConnections", "3"));
			LOGIN_BLOWFISH_KEYS = Integer.parseInt(serverSettings.getProperty("BlowFishKeys", "20"));
			LOGIN_RSA_KEYPAIRS = Integer.parseInt(serverSettings.getProperty("RSAKeyPairs", "10"));
			ACCEPT_NEW_GAMESERVER = Boolean.parseBoolean(serverSettings.getProperty("AcceptNewGameServer", "true"));
			DEFAULT_PASSWORD_HASH = serverSettings.getProperty("PasswordHash", "whirlpool2");
			LEGACY_PASSWORD_HASH = serverSettings.getProperty("LegacyPasswordHash", "sha1");
			AUTO_CREATE_ACCOUNTS = Boolean.parseBoolean(serverSettings.getProperty("AutoCreateAccounts", "true"));
			ANAME_TEMPLATE = serverSettings.getProperty("AccountTemplate", "[A-Za-z0-9]{4,14}");
			APASSWD_TEMPLATE = serverSettings.getProperty("PasswordTemplate", "[A-Za-z0-9]{4,16}");
			LOGIN_TRY_BEFORE_BAN = Integer.parseInt(serverSettings.getProperty("LoginTryBeforeBan", "10"));
			LOGIN_TRY_TIMEOUT = Long.parseLong(serverSettings.getProperty("LoginTryTimeout", "5")) * 1000L;
			IP_BAN_TIME = Long.parseLong(serverSettings.getProperty("IpBanTime", "300")) * 1000L;
			GAME_SERVER_PING_DELAY = Long.parseLong(serverSettings.getProperty("GameServerPingDelay", "30")) * 1000L;
			GAME_SERVER_PING_RETRY = Integer.parseInt(serverSettings.getProperty("GameServerPingRetry", "4"));
			CHEAT_PASSWORD_CHECK = Boolean.parseBoolean(serverSettings.getProperty("CheatPasswordCheck", "false"));
			ALLOW_ENCODE_PASSWORD = Boolean.parseBoolean(serverSettings.getProperty("AllowEncodePasswords", "True"));
			SHOW_LICENCE = Boolean.parseBoolean(serverSettings.getProperty("ShowLicence", "true"));
			LOGIN_SERVER_SCHEDULE_RESTART = Boolean.parseBoolean(serverSettings.getProperty("LoginRestartSchedule", "False"));
			LOGIN_SERVER_SCHEDULE_RESTART_TIME = Long.parseLong(serverSettings.getProperty("LoginRestartTime", "24"));
			FAKE_ONLINE = Double.parseDouble(serverSettings.getProperty("FakeOnline", "1.0"));
			FAKE_ONLINE_MULTIPLIER = Integer.parseInt(serverSettings.getProperty("FakeOnlineMultiplier", "0"));
			final String[] props = serverSettings.getProperty("WhiteIpList", "127.0.0.1").split(";");
			WHITE_IPS = new ArrayList<>(props.length);
			if (props.length != 0)
			{
				for (final String name : props)
				{
					WHITE_IPS.add(name);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("Config: " + e.getMessage());
			throw new Error("Failed to Load " + LOGIN_CONFIGURATION_FILE + "File.");
		}
	}

	public static ScrambledKeyPair getScrambledRSAKeyPair()
	{
		return _keyPairs[Rnd.get(_keyPairs.length)];
	}

	public static byte[] getBlowfishKey()
	{
		return _blowfishKeys[Rnd.get(_blowfishKeys.length)];
	}
	
	public static HashMap<String, String> getPersonalConfigs()
	{
		return _personalConfigs;
	}
	
	public static class ProxyServerConfig
	{
		private final int _origServerId;
		private final int _proxyServerId;
		private final String _porxyHost;
		private final int _proxyPort;
		private final boolean _hideMain;
		private int _minAccessLevel;
		private int _maxAccessLevel;
		
		public ProxyServerConfig(int origServerId, int proxyServerId, String porxyHost, int proxyPort, boolean hideMain, String accessLevels)
		{
			_origServerId = origServerId;
			_proxyServerId = proxyServerId;
			_porxyHost = porxyHost;
			_proxyPort = proxyPort;
			_hideMain = hideMain;
			final String[] diff = accessLevels.split("-");
			if (diff.length > 1)
			{
				_minAccessLevel = Integer.parseInt(diff[0]);
				_maxAccessLevel = Integer.parseInt(diff[1]);
			}
			else
			{
				_minAccessLevel = Integer.parseInt(accessLevels);
				_maxAccessLevel = Integer.parseInt(accessLevels);
			}
		}
		
		public int getOrigServerId()
		{
			return _origServerId;
		}
		
		public int getProxyId()
		{
			return _proxyServerId;
		}
		
		public String getPorxyHost()
		{
			return _porxyHost;
		}
		
		public int getProxyPort()
		{
			return _proxyPort;
		}
		
		public int getMinAccessLevel()
		{
			return _minAccessLevel;
		}
		
		public int getMaxAccessLevel()
		{
			return _maxAccessLevel;
		}
		
		public boolean isHideMain()
		{
			return _hideMain;
		}
	}
	
	private static void loadServerProxies()
	{
		final List<ProxyServerConfig> proxyServersConfigs = new ArrayList<>();
		try
		{
			final File file = new File("./config/proxyservers.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);
			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					for (Node d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
					{
						if ("proxyServer".equalsIgnoreCase(d1.getNodeName()))
						{
							final int origSrvId = Integer.parseInt(d1.getAttributes().getNamedItem("origId").getNodeValue());
							final int proxySrvId = Integer.parseInt(d1.getAttributes().getNamedItem("proxyId").getNodeValue());
							final String proxyHost = d1.getAttributes().getNamedItem("proxyHost").getNodeValue();
							final int proxyPort = Integer.parseInt(d1.getAttributes().getNamedItem("proxyPort").getNodeValue());
							final boolean hideMain = Boolean.parseBoolean(d1.getAttributes().getNamedItem("isHideMain").getNodeValue());
							final String accessLevels = d1.getAttributes().getNamedItem("accessLevels").getNodeValue();
							final ProxyServerConfig psc = new ProxyServerConfig(origSrvId, proxySrvId, proxyHost, proxyPort, hideMain, accessLevels);
							proxyServersConfigs.add(psc);
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("Can't load proxy server's config", e);
		}
		PROXY_SERVERS_CONFIGS = proxyServersConfigs.toArray(new ProxyServerConfig[proxyServersConfigs.size()]);
	}
}
