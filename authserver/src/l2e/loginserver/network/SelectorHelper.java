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
package l2e.loginserver.network;

import java.nio.channels.SocketChannel;

import org.nio.impl.IAcceptFilter;
import org.nio.impl.IClientFactory;
import org.nio.impl.IMMOExecutor;
import org.nio.impl.MMOConnection;

import l2e.loginserver.IpBanManager;
import l2e.loginserver.ThreadPoolManager;
import l2e.loginserver.network.serverpackets.Init;

public class SelectorHelper implements IMMOExecutor<LoginClient>, IClientFactory<LoginClient>, IAcceptFilter
{
	@Override
	public void execute(Runnable r)
	{
		ThreadPoolManager.getInstance().execute(r);
	}

	@Override
	public LoginClient create(MMOConnection<LoginClient> con)
	{
		final LoginClient client = new LoginClient(con);
		client.sendPacket(new Init(client));
		ThreadPoolManager.getInstance().schedule(() ->
		{
			client.closeNow(false);
		}, 60000L);
		return client;
	}

	@Override
	public boolean accept(SocketChannel sc)
	{
		return !IpBanManager.getInstance().isIpBanned(sc.socket().getInetAddress().getHostAddress());
	}
}