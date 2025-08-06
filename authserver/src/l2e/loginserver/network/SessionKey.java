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

import l2e.commons.util.Rnd;

public class SessionKey
{
	public final int _playOkID1;
	public final int _playOkID2;
	public final int _loginOkID1;
	public final int _loginOkID2;
	private final int _hashCode;

	public SessionKey(int loginOK1, int loginOK2, int playOK1, int playOK2)
	{
		_playOkID1 = playOK1;
		_playOkID2 = playOK2;
		_loginOkID1 = loginOK1;
		_loginOkID2 = loginOK2;

		int hashCode = playOK1;
		hashCode *= 17;
		hashCode += playOK2;
		hashCode *= 37;
		hashCode += loginOK1;
		hashCode *= 51;
		hashCode += loginOK2;

		_hashCode = hashCode;
	}

	public boolean checkLoginPair(int loginOk1, int loginOk2)
	{
		return _loginOkID1 == loginOk1 && _loginOkID2 == loginOk2;
	}

	public final static SessionKey create()
	{
		return new SessionKey(Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt());
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null)
		{
			return false;
		}
		if(o.getClass() == this.getClass())
		{
			final SessionKey skey = (SessionKey) o;
			return _playOkID1 == skey._playOkID1 && _playOkID2 == skey._playOkID2 && skey.checkLoginPair(_loginOkID1, _loginOkID2);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return _hashCode;
	}

	@Override
	public String toString()
	{
		return new StringBuilder().append("[playOkID1: ").append(_playOkID1).append(" playOkID2: ").append(_playOkID2).append(" loginOkID1: ").append(_loginOkID1).append(" loginOkID2: ").append(_loginOkID2).append("]").toString();
	}
}