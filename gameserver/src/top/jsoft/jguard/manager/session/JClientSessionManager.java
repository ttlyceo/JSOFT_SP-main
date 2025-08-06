package top.jsoft.jguard.manager.session;

import l2e.gameserver.network.GameClient;
import top.jsoft.jguard.manager.session.model.HWID;
import top.jsoft.jguard.manager.session.model.JClientData;
import top.jsoft.jguard.manager.session.model.JClientSession;

import java.util.Collection;
import java.util.HashMap;


/**
 * @author Akumu
 * @date 26.10.13
 */
public class JClientSessionManager
{
	private static final HashMap<HWID, JClientSession> _sessions = new HashMap<HWID, JClientSession>();
	private static final HashMap<GameClient, JClientData> _storage = new HashMap<GameClient, JClientData>();

	private static final HashMap<HWID, JClientSession> _OLDsessions = new HashMap<HWID, JClientSession>();
	
	
	/**
	 *
	 * @param client - объект клиента
	 * @return refId сессии клиента
	 */
	public static JClientData getClientData(GameClient client)
	{
		return _storage.get(client);
	}

	/**
	 * Складываем в коллекцию int ключ для клиента.
	 * Ключ является индексом доступа к сессии клиента, в случае отсутствия возможности хранить дополнительные данные в объекте клиента.
	 * {@link ClientSessionManager}
	 * @param client - объект клиента
	 * @param cd - clientData
	 */
	public static void setClientData(GameClient client, JClientData cd)
	{
		_storage.put(client, cd);
	}

	/**
	 * ВОзвращает объект сессии, к которой пренадлежит клиент
	 * @param client - объект клиента
	 * @return объект сессии
	 */
	public static JClientSession getSession(GameClient client)
	{
		return getSession(getClientData(client));
	}

	public static JClientSession getSession(JClientData cd)
	{
		return getSession(cd.hwid);
	}

	public static JClientSession getSession(HWID hwid)
	{
		return _sessions.get(hwid);
	}

	public static void putSession(JClientSession cs)
	{
		_sessions.put(cs.hwid, cs);
	}

	public static Collection<JClientSession> getAllSessions()
	{
		return _sessions.values();
	}
	
	
	
	
	
	/**
	 * ВОзвращает объект сессии, к которой пренадлежит клиент
	 * @param client - объект клиента
	 * @return объект сессии
	 */
	public static JClientSession getOLDSession(GameClient client)
	{
		return getOLDSession(getClientData(client));
	}

	public static JClientSession getOLDSession(JClientData cd)
	{
		return getOLDSession(cd.hwid);
	}

	public static JClientSession getOLDSession(HWID hwid)
	{
		return _OLDsessions.get(hwid);
	}

	public static void putOLDSession(JClientSession cs)
	{
		_OLDsessions.put(cs.hwid, cs);
	}

	public static Collection<JClientSession> getAllOLDSessions()
	{
		return _OLDsessions.values();
	}
}