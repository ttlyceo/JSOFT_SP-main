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
package l2e.gameserver;

import org.slf4j.Logger;

public class EternityWorld
{
	public static final int _revision = 2575;
	
	public static void getTeamInfo(Logger log)
	{
		final var revision = _revision;

		log.info("           Server: ........... High Five 5");
		log.info("         Revision: ........... " + revision);
	}
}