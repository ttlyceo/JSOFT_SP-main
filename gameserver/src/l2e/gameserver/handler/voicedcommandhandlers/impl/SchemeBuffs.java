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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.communityhandlers.impl.CommunityBuffer;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.service.buffer.SchemeBuff;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.FunPvpZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class SchemeBuffs implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "buff"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (Config.ALLOW_SCHEMES_FOR_PREMIUMS && !player.hasPremiumBonus())
		{
			CommunityBuffer.getInstance().sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.ONLY_FOR_PREMIUM", player.getLang()).toString());
			return false;
		}
		
		if (!checkCondition(player))
		{
			return false;
		}
		
		if (player.isActionsDisabled() || player.isOutOfControl())
		{
			return false;
		}
		
		if (target != null && !(target.isEmpty()))
		{
			final StringTokenizer st = new StringTokenizer(target);
			String name = null;
			if (st.hasMoreTokens())
			{
				name = st.nextToken();
			}
			
			if (name != null)
			{
				if (!player.checkFloodProtection("BUFFSDELAY", "buffs_delay"))
				{
					return false;
				}
				
				List<? extends SchemeBuff> schemesBuffs = (player.getBuffSchemeByName(name) != null) ? player.getBuffSchemeByName(name).getBuffs() : null;
				if (schemesBuffs == null)
				{
					final int groupId = CommunityBuffer.getInstance().getGroupId(name);
					if (groupId > 0)
					{
						schemesBuffs = CommunityBuffer.getInstance().getSetBuffs(groupId);
					}
				}

				if (schemesBuffs == null) {

					player.sendMessage(new ServerMessage("CommunityBuffer.NEED_CREATE_SCHEME", player.getLang()).toString());
					return false;
				}
				
				final List<Integer> buffs = new ArrayList<>();
				final List<Integer> levels = new ArrayList<>();
				
				for (final SchemeBuff buff : schemesBuffs)
				{
					final int id = buff.getSkillId();
					final int level = player.hasPremiumBonus() ? buff.getPremiumLevel() : buff.getLevel();
					
					if (!CommunityBuffer.getInstance().isValidSkill(player, id, level))
					{
						Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " try to cheat with Community Buffer!");
						return false;
					}
					
					switch (CommunityBuffer.getInstance().getBuffType(player, id))
					{
						case "buff" :
							buffs.add(id);
							levels.add(level);
							break;
						case "resist" :
							buffs.add(id);
							levels.add(level);
							break;
						case "song" :
							buffs.add(id);
							levels.add(level);
							break;
						case "dance" :
							buffs.add(id);
							levels.add(level);
							break;
						case "chant" :
							buffs.add(id);
							levels.add(level);
							break;
						case "others" :
							buffs.add(id);
							levels.add(level);
							break;
						case "special" :
							buffs.add(id);
							levels.add(level);
							break;
					}
				}
				
				if (buffs.size() == 0)
				{
					CommunityBuffer.getInstance().sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.NO_BUFFS", player.getLang()).toString());
					return false;
				}
				
				if (!Config.FREE_ALL_BUFFS && (player.getLevel() > Config.COMMUNITY_FREE_BUFF_LVL))
				{
					final int price = buffs.size() * Config.BUFF_AMOUNT;
					if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM) == null)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return false;
					}
						
					if (player.getInventory().getItemByItemId(Config.BUFF_ID_ITEM).getCount() < price)
					{
						final ServerMessage message = new ServerMessage("CommunityBuffer.NECESSARY_ITEMS", player.getLang());
						message.add(price);
						message.add(Util.getItemName(player, Config.BUFF_ID_ITEM));
						CommunityBuffer.getInstance().sendErrorMessageToPlayer(player, message.toString());
						return false;
					}
					player.destroyItemByItemId("SchemeBuffer", Config.BUFF_ID_ITEM, price, player, true);
				}
					
				double hp = 0.;
				double mp = 0.;
				double cp = 0.;
				double s_hp = 0.;
				double s_mp = 0.;
				final var canHeal = CommunityBuffer.getInstance().canHeal(player);
				if (!canHeal)
				{
					hp = player.getCurrentHp();
					mp = player.getCurrentMp();
					cp = player.getCurrentCp();
					if (player.hasSummon())
					{
						
						s_hp = player.getSummon().getCurrentHp();
						s_mp = player.getSummon().getCurrentMp();
					}
				}
				
				for (int i = 0; i < buffs.size(); ++i)
				{
					if (CommunityBuffer.getInstance().isItemRemoveSkill(player, buffs.get(i), levels.get(i)))
					{
						if (!CommunityBuffer.getInstance().checkItemsForSkill(player, buffs.get(i), levels.get(i), false))
						{
							continue;
						}
					}
					
					final Skill skill = SkillsParser.getInstance().getInfo(buffs.get(i), levels.get(i));
					if (skill != null)
					{
						final int buffTime = CommunityBuffer.getInstance().getBuffTime(player, skill.getId(), skill.getLevel());
						if (buffTime > 0 && skill.hasEffects())
						{
							final Env env = new Env();
							env.setCharacter(player);
							env.setTarget(player);
							env.setSkill(skill);
							
							Effect ef;
							for (final EffectTemplate et : skill.getEffectTemplates())
							{
								ef = et.getEffect(env);
								if (ef != null)
								{
									ef.setAbnormalTime(buffTime * 60);
									ef.scheduleEffect(true);
								}
							}
						}
						else
						{
							skill.getEffects(player, player, false);
						}
						
						if (Config.ALLOW_SUMMON_AUTO_BUFF && player.hasSummon())
						{
							if (buffTime > 0 && skill.hasEffects())
							{
								final var env = new Env();
								env.setCharacter(player);
								env.setTarget(player.getSummon());
								env.setSkill(skill);
								
								Effect ef;
								for (final var et : skill.getEffectTemplates())
								{
									ef = et.getEffect(env);
									if (ef != null)
									{
										ef.setAbnormalTime(buffTime * 60);
										ef.scheduleEffect(true);
									}
								}
							}
							else
							{
								skill.getEffects(player, player.getSummon(), false);
							}
						}
					}
				}
				
				if (!canHeal)
				{
					player.setCurrentHpMp(hp, mp);
					player.setCurrentCp(cp);
					if (player.hasSummon())
					{
						
						player.getSummon().setCurrentHpMp(s_hp, s_mp);
					}
				}
				return true;
			}
			else
			{
				CommunityBuffer.getInstance().sendErrorMessageToPlayer(player, new ServerMessage("CommunityBuffer.INVALID_SCHEME", player.getLang()).toString());
				return false;
			}
		}
		return true;
	}
	
	private static boolean checkCondition(Player player)
	{
		if (player == null)
		{
			return false;
		}
		
		if (player.isInKrateisCube() || player.getUCState() > 0 || player.isBlocked() || player.isCursedWeaponEquipped() || player.isInDuel() || player.isFlying() || player.isJailed() || player.isInOlympiadMode() || player.inObserverMode() || player.isAlikeDead() || player.isDead())
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return false;
		}
		
		if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())))
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return false;
		}
		if (player.isInsideZone(ZoneId.PVP) && !player.isInFightEvent())
		{
			if (player.isInsideZone(ZoneId.FUN_PVP))
			{
				final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
				if (zone != null && !zone.canUseCbBuffs())
				{
					return false;
				}
			}
			else
			{
				if (player.isInsideZone(ZoneId.SIEGE) && !Config.ALLOW_COMMUNITY_BUFF_IN_SIEGE)
				{
					player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
					return false;
				}
			}
		}
		if (player.isInCombat() || player.isCastingNow() || player.isAttackingNow())
		{
			player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
			return false;
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}