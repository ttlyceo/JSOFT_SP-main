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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpBanManager
{
	private static final Logger _log = LoggerFactory.getLogger(IpBanManager.class);

	private class IpSession
	{
		public int tryCount;
		public long lastTry;
		public long banExpire;
	}

	private final Map<String, IpSession> ips = new HashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	private IpBanManager()
	{
		ThreadPoolManager.getInstance().scheduleAtFixedRate(() ->
		{
			final long currentMillis = System.currentTimeMillis();
			
			writeLock.lock();
			try
			{
				IpSession session;
				for(final Iterator<IpSession> itr = ips.values().iterator(); itr.hasNext();)
				{
					session = itr.next();
					if(session.banExpire < currentMillis && session.lastTry < currentMillis - Config.LOGIN_TRY_TIMEOUT)
					{
						itr.remove();
					}
				}
			}
			finally
			{
				writeLock.unlock();
			}
		}, 1000L, 1000L);
	}

	public boolean isIpBanned(String ip)
	{
		if (Config.WHITE_IPS.contains(ip))
		{
			return false;
		}
		
		readLock.lock();
		try
		{
			IpSession ipsession;
			if((ipsession = ips.get(ip)) == null)
			{
				return false;
			}

			return ipsession.banExpire > System.currentTimeMillis();
		}
		finally
		{
			readLock.unlock();
		}
	}

	public boolean tryLogin(String ip, boolean success)
	{
		if (Config.WHITE_IPS.contains(ip))
		{
			return true;
		}
		
		writeLock.lock();
		try
		{
			IpSession ipsession;
			if((ipsession = ips.get(ip)) == null)
			{
				ips.put(ip, ipsession = new IpSession());
			}

			final long currentMillis = System.currentTimeMillis();

			if(currentMillis - ipsession.lastTry < Config.LOGIN_TRY_TIMEOUT)
			{
				success = false;
			}

			if(success)
			{
				if(ipsession.tryCount > 0)
				{
					ipsession.tryCount--;
				}
			}
			else
			{
				if(ipsession.tryCount < Config.LOGIN_TRY_BEFORE_BAN)
				{
					ipsession.tryCount++;
				}
			}

			ipsession.lastTry = currentMillis;
			if(ipsession.tryCount == Config.LOGIN_TRY_BEFORE_BAN)
			{
				_log.warn("IpBanManager: " + ip + " banned for " + Config.IP_BAN_TIME / 1000L + " seconds.");
				ipsession.banExpire = currentMillis + Config.IP_BAN_TIME;
				return false;
			}
			return true;
		}
		finally
		{
			writeLock.unlock();
		}
	}
	
	public static final IpBanManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final IpBanManager _instance = new IpBanManager();
	}
}
