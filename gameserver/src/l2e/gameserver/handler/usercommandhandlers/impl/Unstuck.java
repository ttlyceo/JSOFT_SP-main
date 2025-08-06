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
package l2e.gameserver.handler.usercommandhandlers.impl;


import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.usercommandhandlers.IUserCommandHandler;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.SetupGauge;

public class Unstuck implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
	        52
	};

	@Override
	public boolean useUserCommand(int id, Player activeChar)
	{
		for (final AbstractFightEvent e : activeChar.getFightEvents())
		{
			if (e != null && !e.canUseEscape(activeChar))
			{
				activeChar.sendActionFailed();
				return false;
			}
		}

		var e = activeChar.getPartyTournament();
		if (e != null && !e.canUseEscape(activeChar))
		{
			activeChar.sendActionFailed();
			return false;
		}

		if (!AerialCleftEvent.getInstance().onEscapeUse(activeChar.getObjectId()))
		{
			activeChar.sendActionFailed();
			return false;
		}

		if (activeChar.isJailed())
		{
			activeChar.sendMessage("You cannot use this function while you are jailed.");
			return false;
		}

		final int unstuckTimer = (activeChar.getAccessLevel().isGm() ? 1000 : Config.UNSTUCK_INTERVAL * 1000);

		if (activeChar.isCastingNow() || activeChar.isFearing() || activeChar.isMuted() || activeChar.isMovementDisabled() || activeChar.isMuted() || activeChar.isAlikeDead() || activeChar.isInOlympiadMode() || activeChar.inObserverMode() || activeChar.isCombatFlagEquipped())
		{
			return false;
		}
		activeChar.forceIsCasting(System.currentTimeMillis() + (unstuckTimer / 2));

		final Skill escape = SkillsParser.getInstance().getInfo(2099, 1);
		final Skill GM_escape = SkillsParser.getInstance().getInfo(2100, 1);
		if (activeChar.getAccessLevel().isGm())
		{
			if (GM_escape != null)
			{
				activeChar.doCast(GM_escape);
				return true;
			}
			activeChar.sendMessage("You use Escape: 1 second.");
		}
		else if ((Config.UNSTUCK_INTERVAL == 300) && (escape != null))
		{
			activeChar.doCast(escape);
			return true;
		}
		else
		{
			if (Config.UNSTUCK_INTERVAL > 100)
			{
				activeChar.sendMessage("You use Escape: " + (unstuckTimer / 60000) + " minutes.");
			}
			else
			{
				activeChar.sendMessage("You use Escape: " + (unstuckTimer / 1000) + " seconds.");
			}
			activeChar.doCast(escape);
		}
		activeChar.getAI().setIntention(CtrlIntention.IDLE);
		activeChar.setTarget(activeChar);
		activeChar.disableAllSkills();

		activeChar.broadcastPacket(900, new MagicSkillUse(activeChar, 1050, 1, unstuckTimer, 0));
		activeChar.sendPacket(new SetupGauge(activeChar, 0, unstuckTimer));
		activeChar.setSkillCast(ThreadPoolManager.getInstance().schedule(new EscapeFinalizer(activeChar), unstuckTimer));

		return true;
	}

	private class EscapeFinalizer implements Runnable
	{
		private final Player _activeChar;

		protected EscapeFinalizer(Player activeChar)
		{
			_activeChar = activeChar;
		}

		@Override
		public void run()
		{
			if (_activeChar.isDead())
			{
				return;
			}

			_activeChar.setIsIn7sDungeon(false);
			_activeChar.enableAllSkills();
			_activeChar.setIsCastingNow(false);
			_activeChar.teleToLocation(TeleportWhereType.TOWN, true, ReflectionManager.DEFAULT);
		}
	}

	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}