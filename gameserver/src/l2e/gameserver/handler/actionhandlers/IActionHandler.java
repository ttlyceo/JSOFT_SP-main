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
package l2e.gameserver.handler.actionhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.actor.Player;

public interface IActionHandler
{
	public static final Logger _log = LoggerFactory.getLogger(IActionHandler.class);
	
	public boolean action(Player activeChar, GameObject target, boolean interact, boolean shift);
	
	public InstanceType getInstanceType();
}