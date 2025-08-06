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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateKey;

import org.nio.impl.MMOClient;
import org.nio.impl.MMOConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.loginserver.Config;
import l2e.loginserver.accounts.Account;
import l2e.loginserver.crypt.LoginCrypt;
import l2e.loginserver.crypt.ScrambledKeyPair;
import l2e.loginserver.network.serverpackets.AccountKicked;
import l2e.loginserver.network.serverpackets.AccountKicked.AccountKickedReason;
import l2e.loginserver.network.serverpackets.LoginFail;
import l2e.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import l2e.loginserver.network.serverpackets.LoginServerPacket;

public final class LoginClient extends MMOClient<MMOConnection<LoginClient>>
{
	private static final Logger _log = LoggerFactory.getLogger(LoginClient.class);

	public static enum LoginClientState
	{
		CONNECTED,
		AUTHED_GG,
		AUTHED,
		DISCONNECTED
	}

	private static final int PROTOCOL_VERSION = 0xc621;

	private LoginClientState _state;

	private LoginCrypt _loginCrypt;
	private ScrambledKeyPair _scrambledPair;
	private byte[] _blowfishKey;

	private String _login;
	private SessionKey _skey;
	private Account _account;
	private final String _ipAddr;

	private int _sessionId;

	private boolean _passwordCorrect;

	public LoginClient(MMOConnection<LoginClient> con)
	{
		super(con);
		_state = LoginClientState.CONNECTED;
		_scrambledPair = Config.getScrambledRSAKeyPair();
		_blowfishKey = Config.getBlowfishKey();
		_loginCrypt = new LoginCrypt();
		_loginCrypt.setKey(_blowfishKey);
		_sessionId = con.hashCode();
		_ipAddr = getIpAddr();
		_passwordCorrect = false;
	}

	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		boolean ret;
		try
		{
			ret = _loginCrypt.decrypt(buf.array(), buf.position(), size);
		}
		catch(final IOException e)
		{
			_log.warn("", e);
			closeNow(true);
			return false;
		}

		if(!ret)
		{
			closeNow(true);
		}
		return ret;
	}

	@Override
	public boolean encrypt(ByteBuffer buf, int size)
	{
		final int offset = buf.position();
		try
		{
			size = _loginCrypt.encrypt(buf.array(), offset, size);
		}
		catch(final IOException e)
		{
			_log.warn("", e);
			return false;
		}

		buf.position(offset + size);
		return true;
	}

	public LoginClientState getState()
	{
		return _state;
	}

	public void setState(LoginClientState state)
	{
		_state = state;
	}

	public byte[] getBlowfishKey()
	{
		return _blowfishKey;
	}

	public byte[] getScrambledModulus()
	{
		return _scrambledPair.getScrambledModulus();
	}

	public RSAPrivateKey getRSAPrivateKey()
	{
		return (RSAPrivateKey) _scrambledPair.getKeyPair().getPrivate();
	}

	public String getLogin()
	{
		return _login;
	}

	public void setLogin(String login)
	{
		_login = login;
	}

	public Account getAccount()
	{
		return _account;
	}

	public void setAccount(Account account)
	{
		_account = account;
	}

	public SessionKey getSessionKey()
	{
		return _skey;
	}

	public void setSessionKey(SessionKey skey)
	{
		_skey = skey;
	}

	public void setSessionId(int val)
	{
		_sessionId = val;
	}

	public int getSessionId()
	{
		return _sessionId;
	}

	public void setPasswordCorrect(boolean val)
	{
		_passwordCorrect = val;
	}

	public boolean isPasswordCorrect()
	{
		return _passwordCorrect;
	}

	public void sendPacket(LoginServerPacket lsp)
	{
		if(isConnected())
		{
			getConnection().sendPacket(lsp);
		}
	}

	public void close(LoginFailReason reason)
	{
		if(isConnected())
		{
			getConnection().close(new LoginFail(reason));
		}
	}

	public void close(AccountKickedReason reason)
	{
		if(isConnected())
		{
			getConnection().close(new AccountKicked(reason));
		}
	}

	public void close(LoginServerPacket lsp)
	{
		if(isConnected())
		{
			getConnection().close(lsp);
		}
	}

	@Override
	public void onDisconnection()
	{
		_state = LoginClientState.DISCONNECTED;
		_skey = null;
		_loginCrypt = null;
		_scrambledPair = null;
		_blowfishKey = null;
	}

	@Override
	public String toString()
	{
		switch(_state)
		{
			case AUTHED:
				return "[ Account : " + getLogin() + " IP: " + getIpAddress() + "]";
			default:
				return "[ State : " + getState() + " IP: " + getIpAddress() + "]";
		}
	}

	public String getIpAddress()
	{
		return _ipAddr;
	}

	@Override
	protected void onForcedDisconnection()
	{
	}

	public int getProtocol()
	{
		return PROTOCOL_VERSION;
	}
}