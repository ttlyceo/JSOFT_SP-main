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
package l2e.loginserver.crypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.checksum.CheckSumAPI;
import l2e.commons.checksum.algorithm.AbstractChecksum;

public class PasswordHash
{
	private static final Logger _log = LoggerFactory.getLogger(PasswordHash.class);

	private final String _name;

	public PasswordHash(String name)
	{
		_name = name;
	}

	public boolean compare(String password, String expected)
	{
		try
		{
			return encrypt(password).equals(expected);
		}
		catch(final Exception e)
		{
			_log.warn(_name + ": encryption error!", e);
			return false;
		}
	}
	public String encrypt(String password) throws Exception
	{
		final AbstractChecksum checksum = CheckSumAPI.getChecksumInstance(_name);
		checksum.setEncoding("BASE64");
		checksum.update(password.getBytes());
		return checksum.format("#CHECKSUM");
	}
}