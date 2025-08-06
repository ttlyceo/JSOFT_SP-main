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
package l2e.gameserver.model.actor.tasks.character;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;

public final class QueuedMagicUseTask implements Runnable
{
	private final Player _currPlayer;
	private final Skill _queuedSkill;
	private final boolean _isCtrlPressed;
	private final boolean _isShiftPressed;
	
	public QueuedMagicUseTask(Player currPlayer, Skill queuedSkill, boolean isCtrlPressed, boolean isShiftPressed)
	{
		_currPlayer = currPlayer;
		_queuedSkill = queuedSkill;
		_isCtrlPressed = isCtrlPressed;
		_isShiftPressed = isShiftPressed;
	}

	@Override
	public void run()
	{
		if (_currPlayer != null)
		{
			_currPlayer.useMagic(_queuedSkill, _isCtrlPressed, _isShiftPressed, true);
		}
	}
}