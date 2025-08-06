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
package l2e.gameserver.handler.voicedcommandhandlers.impl;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.instancemanager.CoupleManager;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.listener.player.impl.EngageAnswerListener;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.SetupGauge;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Wedding implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "divorce", "engage", "gotolove"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (activeChar == null || !Config.ALLOW_WEDDING)
		{
			return false;
		}
		if (command.startsWith("engage"))
		{
			return engage(activeChar);
		}
		else if (command.startsWith("divorce"))
		{
			return divorce(activeChar);
		}
		else if (command.startsWith("gotolove"))
		{
			return goToLove(activeChar);
		}
		return false;
	}
	
	public boolean divorce(Player activeChar)
	{
		if (activeChar.getPartnerId() == 0)
		{
			return false;
		}
		
		final int _partnerId = activeChar.getPartnerId();
		final int _coupleId = activeChar.getCoupleId();
		long AdenaAmount = 0;
		
		if (activeChar.isMarried())
		{
			activeChar.sendMessage("You are now divorced.");
			
			AdenaAmount = (activeChar.getAdena() / 100) * Config.WEDDING_DIVORCE_COSTS;
			activeChar.getInventory().reduceAdena("Wedding", AdenaAmount, activeChar, null);
			
		}
		else
		{
			activeChar.sendMessage("You have broken up as a couple.");
		}
		
		final Player partner = GameObjectsStorage.getPlayer(_partnerId);
		if (partner != null)
		{
			partner.setPartnerId(0);
			if (partner.isMarried())
			{
				partner.sendMessage("Your spouse has decided to divorce you.");
			}
			else
			{
				partner.sendMessage("Your fiance has decided to break the engagement with you.");
			}
			
			if (AdenaAmount > 0)
			{
				partner.addAdena("WEDDING", AdenaAmount, null, false);
			}
		}
		CoupleManager.getInstance().deleteCouple(_coupleId);
		return true;
	}
	
	private boolean engage(Player activeChar)
	{
		if (activeChar.getTarget() == null)
		{
			activeChar.sendMessage("You have no one targeted.");
			return false;
		}
		else if (!(activeChar.getTarget().isPlayer()))
		{
			activeChar.sendMessage("You can only ask another player to engage you.");
			return false;
		}
		else if (activeChar.getPartnerId() != 0)
		{
			activeChar.sendMessage("You are already engaged.");
			if (Config.WEDDING_PUNISH_INFIDELITY)
			{
				activeChar.startAbnormalEffect(AbnormalEffect.BIG_HEAD);
				int skillId;
				
				int skillLevel = 1;
				
				if (activeChar.getLevel() > 40)
				{
					skillLevel = 2;
				}
				
				if (activeChar.isMageClass())
				{
					skillId = 4362;
				}
				else
				{
					skillId = 4361;
				}
				
				final Skill skill = SkillsParser.getInstance().getInfo(skillId, skillLevel);
				if (activeChar.getFirstEffect(skill) == null)
				{
					skill.getEffects(activeChar, activeChar, false);
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
			}
			return false;
		}
		final Player ptarget = (Player) activeChar.getTarget();
		
		if (ptarget.getObjectId() == activeChar.getObjectId())
		{
			activeChar.sendMessage("Is there something wrong with you, are you trying to go out with youself?");
			return false;
		}
		
		if (ptarget.isMarried())
		{
			activeChar.sendMessage("Player already married.");
			return false;
		}
		
		if (ptarget.isEngageRequest())
		{
			activeChar.sendMessage("Player already asked by someone else.");
			return false;
		}
		
		if (ptarget.getPartnerId() != 0)
		{
			activeChar.sendMessage("Player already engaged with someone else.");
			return false;
		}
		
		if (ptarget.getAppearance().getSex() == activeChar.getAppearance().getSex())
		{
			activeChar.sendMessage("Gay marriage is not allowed on this server!");
			return false;
		}
		
		boolean FoundOnFriendList = false;
		int objectId;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=?");
			statement.setInt(1, ptarget.getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				objectId = rset.getInt("friendId");
				if (objectId == activeChar.getObjectId())
				{
					FoundOnFriendList = true;
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("could not read friend data:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (!FoundOnFriendList)
		{
			activeChar.sendMessage("The player you want to ask is not on your friends list, you must first be on each others friends list before you choose to engage.");
			return false;
		}
		ptarget.setEngageRequest(true, activeChar.getObjectId());
		ptarget.sendConfirmDlg(new EngageAnswerListener(ptarget), 0, activeChar.getName(null) + " is asking to engage you. Do you want to start a new relationship?");
		return true;
	}
	
	public boolean goToLove(Player activeChar)
	{
		if (!activeChar.isMarried())
		{
			activeChar.sendMessage("You're not married.");
			return false;
		}
		
		if (activeChar.getPartnerId() == 0)
		{
			activeChar.sendMessage("Couldn't find your fiance in the Database - Inform a Gamemaster.");
			_log.error("Married but couldn't find parter for " + activeChar.getName(null));
			return false;
		}
		
		if (EpicBossManager.getInstance().getZone(activeChar) != null)
		{
			activeChar.sendMessage("You are inside a Boss Zone.");
			return false;
		}
		
		if (activeChar.isCombatFlagEquipped())
		{
			activeChar.sendMessage("While you are holding a Combat Flag or Territory Ward you can't go to your love!");
			return false;
		}
		
		if (activeChar.isCursedWeaponEquipped())
		{
			activeChar.sendMessage("While you are holding a Cursed Weapon you can't go to your love!");
			return false;
		}
		
		if (EpicBossManager.getInstance().getZone(activeChar) != null)
		{
			activeChar.sendMessage("You are inside a Boss Zone.");
			return false;
		}
		
		if (activeChar.isJailed())
		{
			activeChar.sendMessage("You are in Jail!");
			return false;
		}
		
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("You are in the Olympiad now.");
			return false;
		}
		
		if (activeChar.isInDuel())
		{
			activeChar.sendMessage("You are in a duel!");
			return false;
		}
		
		if (activeChar.inObserverMode())
		{
			activeChar.sendMessage("You are in the observation.");
			return false;
		}
		
		if ((SiegeManager.getInstance().getSiege(activeChar) != null) && SiegeManager.getInstance().getSiege(activeChar).getIsInProgress())
		{
			activeChar.sendMessage("You are in a siege, you cannot go to your partner.");
			return false;
		}
		
		if (activeChar.isFestivalParticipant())
		{
			activeChar.sendMessage("You are in a festival.");
			return false;
		}
		
		if (activeChar.isInParty() && activeChar.getParty().isInDimensionalRift())
		{
			activeChar.sendMessage("You are in the dimensional rift.");
			return false;
		}
		
		for (final AbstractFightEvent e : activeChar.getFightEvents())
		{
			if (e != null && !e.canUseEscape(activeChar))
			{
				activeChar.sendActionFailed();
				return false;
			}
		}

		var partyTournament = activeChar.getPartyTournament();
		if (partyTournament != null && !partyTournament.canUseEscape(activeChar))
		{
			activeChar.sendActionFailed();
			return false;
		}

		if (!AerialCleftEvent.getInstance().onEscapeUse(activeChar.getObjectId()))
		{
			activeChar.sendActionFailed();
			return false;
		}
		
		if (activeChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			activeChar.sendMessage("You are in area which blocks summoning.");
			return false;
		}
		
		final Player partner = GameObjectsStorage.getPlayer(activeChar.getPartnerId());
		if ((partner == null) || !partner.isOnline())
		{
			activeChar.sendMessage("Your partner is not online.");
			return false;
		}
		
		if (activeChar.getReflectionId() != partner.getReflectionId())
		{
			activeChar.sendMessage("Your partner is in another World!");
			return false;
		}
		
		if (partner.isJailed())
		{
			activeChar.sendMessage("Your partner is in Jail.");
			return false;
		}
		
		if (partner.isCursedWeaponEquipped())
		{
			activeChar.sendMessage("Your partner is holding a Cursed Weapon and you can't go to your love!");
			return false;
		}
		
		if (EpicBossManager.getInstance().getZone(partner) != null)
		{
			activeChar.sendMessage("Your partner is inside a Boss Zone.");
			return false;
		}
		
		if (partner.isInOlympiadMode())
		{
			activeChar.sendMessage("Your partner is in the Olympiad now.");
			return false;
		}
		
		if (partner.isInDuel())
		{
			activeChar.sendMessage("Your partner is in a duel.");
			return false;
		}
		
		if (partner.isFestivalParticipant())
		{
			activeChar.sendMessage("Your partner is in a festival.");
			return false;
		}
		
		if (partner.isInParty() && partner.getParty().isInDimensionalRift())
		{
			activeChar.sendMessage("Your partner is in dimensional rift.");
			return false;
		}
		
		if (partner.inObserverMode())
		{
			activeChar.sendMessage("Your partner is in the observation.");
			return false;
		}
		
		if ((SiegeManager.getInstance().getSiege(partner) != null) && SiegeManager.getInstance().getSiege(partner).getIsInProgress())
		{
			activeChar.sendMessage("Your partner is in a siege, you cannot go to your partner.");
			return false;
		}
		
		if (partner.isIn7sDungeon() && !activeChar.isIn7sDungeon())
		{
			final int playerCabal = SevenSigns.getInstance().getPlayerCabal(activeChar.getObjectId());
			final boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
			final int compWinner = SevenSigns.getInstance().getCabalHighestScore();
			
			if (isSealValidationPeriod)
			{
				if (playerCabal != compWinner)
				{
					activeChar.sendMessage("Your Partner is in a Seven Signs Dungeon and you are not in the winner Cabal!");
					return false;
				}
			}
			else
			{
				if (playerCabal == SevenSigns.CABAL_NULL)
				{
					activeChar.sendMessage("Your Partner is in a Seven Signs Dungeon and you are not registered!");
					return false;
				}
			}
		}

		for (final AbstractFightEvent e : partner.getFightEvents())
		{
			if (e != null && !e.canUseEscape(partner))
			{
				activeChar.sendActionFailed();
				return false;
			}
		}

		var partyTournamentPartner = partner.getPartyTournament();
		if (partyTournamentPartner != null && !partyTournamentPartner.canUseEscape(partner))
		{
			activeChar.sendActionFailed();
			return false;
		}

		if (!AerialCleftEvent.getInstance().onEscapeUse(partner.getObjectId()))
		{
			activeChar.sendActionFailed();
			return false;
		}
		
		if (partner.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			activeChar.sendMessage("Your partner is in area which blocks summoning.");
			return false;
		}
		
		final int teleportTimer = Config.WEDDING_TELEPORT_DURATION * 1000;
		activeChar.sendMessage("After " + (teleportTimer / 60000) + " min. you will be teleported to your partner.");
		activeChar.getInventory().reduceAdena("Wedding", Config.WEDDING_TELEPORT_PRICE, activeChar, null);
		
		activeChar.getAI().setIntention(CtrlIntention.IDLE);
		activeChar.setTarget(activeChar);
		activeChar.disableAllSkills();
		
		activeChar.broadcastPacket(900, new MagicSkillUse(activeChar, 1050, 1, teleportTimer, 0));
		activeChar.sendPacket(new SetupGauge(activeChar, 0, teleportTimer));
		
		final EscapeFinalizer ef = new EscapeFinalizer(activeChar, partner, partner.isIn7sDungeon());
		activeChar.setSkillCast(ThreadPoolManager.getInstance().schedule(ef, teleportTimer));
		activeChar.forceIsCasting(System.currentTimeMillis() + (teleportTimer / 2));
		
		return true;
	}
	
	private class EscapeFinalizer implements Runnable
	{
		private final Player _activeChar;
		private final Player _partner;
		private final boolean _to7sDungeon;
		
		EscapeFinalizer(Player activeChar, Player partner, boolean to7sDungeon)
		{
			_activeChar = activeChar;
			_partner = partner;
			_to7sDungeon = to7sDungeon;
		}
		
		@Override
		public void run()
		{
			if (_activeChar.isDead())
			{
				return;
			}
			
			if ((SiegeManager.getInstance().getSiege(_partner.getX(), _partner.getY(), _partner.getZ()) != null) && SiegeManager.getInstance().getSiege(_partner.getX(), _partner.getY(), _partner.getZ()).getIsInProgress())
			{
				_activeChar.sendMessage("Your partner is in siege, you can't go to your partner.");
				return;
			}
			_activeChar.setIsIn7sDungeon(_to7sDungeon);
			_activeChar.enableAllSkills();
			_activeChar.setIsCastingNow(false);
			
			try
			{
				_activeChar.teleToLocation(_partner.getLocation(), true, _partner.getReflection());
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}