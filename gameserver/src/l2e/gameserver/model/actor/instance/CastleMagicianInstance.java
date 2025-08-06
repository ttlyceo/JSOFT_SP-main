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
package l2e.gameserver.model.actor.instance;

import java.util.List;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.SquadTrainer;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.AcquireSkillDone;
import l2e.gameserver.network.serverpackets.ExAcquirableSkillListByClass;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class CastleMagicianInstance extends NpcInstance implements SquadTrainer
{
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;
	
	public CastleMagicianInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);

		setInstanceType(InstanceType.CastleMagicianInstance);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		player.sendActionFailed();
		String filename = "data/html/castlemagician/magician-no.htm";

		final int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			{
				filename = "data/html/castlemagician/magician-busy.htm";
			}
			else if (condition == COND_OWNER)
			{
				if (val == 0)
				{
					filename = "data/html/castlemagician/magician.htm";
				}
				else
				{
					filename = "data/html/castlemagician/magician-" + val + ".htm";
				}
			}
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (final IndexOutOfBoundsException ioobe)
			{}
			catch (final NumberFormatException nfe)
			{}
			showChatWindow(player, val);
			return;
		}
		else if (command.startsWith("ExchangeKE"))
		{
			String filename = null;
			int item;
			final int i0 = Rnd.get(100);
			if (i0 < 5)
			{
				final int i1 = Rnd.get(100);
				if (i1 < 5)
				{
					item = 9931;
				}
				else if (i1 <= 50)
				{
					item = 9932;
				}
				else if (i1 <= 75)
				{
					item = 10416;
				}
				else
				{
					item = 10417;
				}
			}
			else if (i0 <= 15)
			{
				switch (Rnd.get(5))
				{
					case 1 :
						item = 9917;
						break;
					case 2 :
						item = 9918;
						break;
					case 3 :
						item = 9928;
						break;
					case 4 :
						item = 9929;
						break;
					default :
						item = 9920;

				}
			}
			else if (i0 <= 30)
			{
				switch (Rnd.get(8))
				{
					case 1 :
						item = 9916;
						break;
					case 2 :
						item = 9916;
						break;
					case 3 :
						item = 9924;
						break;
					case 4 :
						item = 9925;
						break;
					case 5 :
						item = 9926;
						break;
					case 6 :
						item = 9927;
						break;
					case 7 :
						item = 10518;
						break;
					default :
						item = 10424;
				}
			}
			else
			{
				switch (Rnd.get(46))
				{
					case 0 :
						item = 9914;
						break;
					case 1 :
						item = 9915;
						break;
					case 2 :
						item = 9920;
						break;
					case 3 :
						item = 9920;
						break;
					case 4 :
						item = 9921;
						break;
					case 5 :
						item = 9922;
						break;
					case 6 :
						item = 9933;
						break;
					case 7 :
						item = 9934;
						break;
					case 8 :
						item = 9935;
						break;
					case 9 :
						item = 9936;
						break;
					case 10 :
						item = 9937;
						break;
					case 11 :
						item = 9938;
						break;
					case 12 :
						item = 9939;
						break;
					case 13 :
						item = 9940;
						break;
					case 14 :
						item = 9941;
						break;
					case 15 :
						item = 9942;
						break;
					case 16 :
						item = 9943;
						break;
					case 17 :
						item = 9944;
						break;
					case 18 :
						item = 9945;
						break;
					case 19 :
						item = 9946;
						break;
					case 20 :
						item = 9947;
						break;
					case 21 :
						item = 9948;
						break;
					case 22 :
						item = 9949;
						break;
					case 23 :
						item = 9950;
						break;
					case 24 :
						item = 9965;
						break;
					case 25 :
						item = 9952;
						break;
					case 26 :
						item = 9953;
						break;
					case 27 :
						item = 9954;
						break;
					case 28 :
						item = 9955;
						break;
					case 29 :
						item = 9956;
						break;
					case 30 :
						item = 9957;
						break;
					case 31 :
						item = 9958;
						break;
					case 32 :
						item = 9959;
						break;
					case 33 :
						item = 9960;
						break;
					case 34 :
						item = 9961;
						break;
					case 35 :
						item = 9962;
						break;
					case 36 :
						item = 9963;
						break;
					case 37 :
						item = 9964;
						break;
					case 38 :
						item = 10418;
						break;
					case 39 :
						item = 10420;
						break;
					case 40 :
						item = 10519;
						break;
					case 41 :
						item = 10422;
						break;
					case 42 :
						item = 10423;
						break;
					case 43 :
						item = 10419;
						break;
					default :
						item = 10421;
				}
			}

			if (player.exchangeItemsById("ExchangeKE", this, 9912, (10 * Config.RATE_TALISMAN_ITEM_MULTIPLIER), item, Config.RATE_TALISMAN_MULTIPLIER, true))
			{
				filename = "data/html/castlemagician/magician-KE-Exchange.htm";
			}
			else
			{
				filename = "data/html/castlemagician/magician-no-KE.htm";
			}
			showChatWindow(player, filename);
		}
		else if (command.equals("gotoleader"))
		{
			if (player.getClan() != null)
			{
				final Player clanLeader = player.getClan().getLeader().getPlayerInstance();
				if (clanLeader == null)
				{
					return;
				}

				if (clanLeader.getFirstEffect(EffectType.CLAN_GATE) != null)
				{
					if (!validateGateCondition(clanLeader, player))
					{
						return;
					}

					player.teleToLocation(clanLeader.getX(), clanLeader.getY(), clanLeader.getZ(), false, ReflectionManager.DEFAULT);
					return;
				}
				showChatWindow(player, "data/html/castlemagician/magician-nogate.htm");
			}
		}
		else if (command.equals("subskills"))
		{
			if (player.isClanLeader())
			{
				final List<SkillLearn> skills = SkillTreesParser.getInstance().getAvailableSubPledgeSkills(player.getClan());
				final ExAcquirableSkillListByClass asl = new ExAcquirableSkillListByClass(AcquireSkillType.SUBPLEDGE);
				int count = 0;

				for (final SkillLearn s : skills)
				{
					if (SkillsParser.getInstance().getInfo(s.getId(), s.getLvl()) != null)
					{
						asl.addSkill(s.getId(), s.getGetLevel(), s.getLvl(), s.getLvl(), s.getLevelUpSp(), 0);
						++count;
					}
				}

				if (count == 0)
				{
					player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
					player.sendPacket(AcquireSkillDone.STATIC);
				}
				else
				{
					player.sendPacket(asl);
				}
			}
			else
			{
				showChatWindow(player, "data/html/castlemagician/magician-nosquad.htm");
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	protected int validateCondition(Player player)
	{
		if (player.canOverrideCond(PcCondOverride.CASTLE_CONDITIONS))
		{
			return COND_OWNER;
		}
		if ((getCastle() != null) && (getCastle().getId() > 0))
		{
			if (player.getClan() != null)
			{
				if (getCastle().getZone().isActive())
				{
					return COND_BUSY_BECAUSE_OF_SIEGE;
				}
				else if (getCastle().getOwnerId() == player.getClanId())
				{
					return COND_OWNER;
				}
			}
		}
		return COND_ALL_FALSE;
	}

	private static final boolean validateGateCondition(Player clanLeader, Player player)
	{
		if (clanLeader.isAlikeDead())
		{
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}

		if (clanLeader.isInStoreMode())
		{
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}

		if (clanLeader.isRooted() || clanLeader.isInCombat())
		{
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}

		if (clanLeader.isInOlympiadMode())
		{
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}

		if (clanLeader.isFestivalParticipant())
		{
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}

		if (clanLeader.inObserverMode())
		{
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}

		if (clanLeader.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}

		if (clanLeader.getReflectionId() > 0)
		{
			if (!Config.ALLOW_SUMMON_TO_INSTANCE || ReflectionManager.getInstance().getReflection(player.getReflectionId()).isSummonAllowed())
			{
				player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
				return false;
			}
		}

		if (player.isIn7sDungeon())
		{
			final int targetCabal = SevenSigns.getInstance().getPlayerCabal(clanLeader.getObjectId());
			if (SevenSigns.getInstance().isSealValidationPeriod())
			{
				if (targetCabal != SevenSigns.getInstance().getCabalHighestScore())
				{
					player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
					return false;
				}
			}
			else
			{
				if (targetCabal == SevenSigns.CABAL_NULL)
				{
					player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
					return false;
				}
			}
		}

		for (final AbstractFightEvent e : player.getFightEvents())
		{
			if (e != null && !e.canUseEscape(player))
			{
				player.sendMessage("You on Event, teleporting disabled.");
				return false;
			}
		}

		var partyTournament = player.getPartyTournament();
		if (partyTournament != null && !partyTournament.canUseEscape(player))
		{
			player.sendMessage("You on Event, teleporting disabled.");
			return false;
		}

		for (final AbstractFightEvent e : clanLeader.getFightEvents())
		{
			if (e != null && !e.canUseEscape(clanLeader))
			{
				player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
				return false;
			}
		}

		var partyTournamentClanLeader = clanLeader.getPartyTournament();
		if (partyTournamentClanLeader != null && !partyTournamentClanLeader.canUseEscape(clanLeader))
		{
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}

		if (!AerialCleftEvent.getInstance().onEscapeUse(player.getObjectId()))
		{
			player.sendMessage("You on Aerial Cleft Event, teleporting disabled.");
			return false;
		}
		
		if (!AerialCleftEvent.getInstance().onEscapeUse(clanLeader.getObjectId()))
		{
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
		return true;
	}

	@Override
	public void showSubUnitSkillList(Player player)
	{
		onBypassFeedback(player, "subskills");
	}
}