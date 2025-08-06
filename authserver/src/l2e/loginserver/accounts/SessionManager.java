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
package l2e.loginserver.accounts;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import l2e.loginserver.ThreadPoolManager;
import l2e.loginserver.network.SessionKey;

public class SessionManager
{
	public final class Session
	{
		private final Account _account;
		private final SessionKey _skey;
		private final long _expireTime;
		private final String _ip;

		private Session(Account account, String ip)
		{
			_account = account;
			_ip = ip;
			_skey = SessionKey.create();
			_expireTime = System.currentTimeMillis() + 60000L;
		}

		public SessionKey getSessionKey()
		{
			return _skey;
		}

		public Account getAccount()
		{
			return _account;
		}
		
		public String getIP()
		{
			return _ip;
		}

		public long getExpireTime()
		{
			return _expireTime;
		}
	}

	private final Map<SessionKey, Session> sessions = new HashMap<>();
	private final Lock lock = new ReentrantLock();

	private SessionManager()
	{
		ThreadPoolManager.getInstance().scheduleAtFixedRate(() ->
		{
			lock.lock();
			try
			{
				final long currentMillis = System.currentTimeMillis();
				Session session;
				for(final Iterator<Session> itr = sessions.values().iterator(); itr.hasNext();)
				{
					session = itr.next();
					if(session.getExpireTime() < currentMillis)
					{
						itr.remove();
					}
				}
			}
			finally
			{
				lock.unlock();
			}
		}, 30000L, 30000L);
	}

	public Session openSession(Account account, String ip, int requestId)
	{
		lock.lock();
		try
		{
			final Session session = new Session(account, ip);
			sessions.put(session.getSessionKey(), session);
			return session;
		}
		finally
		{
			lock.unlock();
		}
	}

	public Session closeSession(SessionKey skey)
	{
		lock.lock();
		try
		{
			return sessions.remove(skey);
		}
		finally
		{
			lock.unlock();
		}
	}

	public Session getSessionByName(String name)
	{
		for(final Session session : sessions.values())
		{
			if (session._account.getLogin().equalsIgnoreCase(name))
			{
				return session;
			}
		}
		return null;
	}
	
	public static final SessionManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SessionManager _instance = new SessionManager();
	}
}
