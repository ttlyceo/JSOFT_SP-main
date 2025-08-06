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
package l2e.gameserver.network.clientpackets;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.SquadTrainer;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.FishermanInstance;
import l2e.gameserver.model.actor.instance.NpcInstance;
import l2e.gameserver.model.actor.instance.VillageMasterInstance;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.AcquireSkillDone;
import l2e.gameserver.network.serverpackets.ExStorageMaxCount;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeSkillList;

public final class RequestAcquireSkill extends GameClientPacket
{
	private static final String[] QUEST_VAR_NAMES =
	{
	        "EmergentAbility65-", "EmergentAbility70-", "ClassAbility75-", "ClassAbility80-"
	};
	
	private int _id;
	private int _level;
	private AcquireSkillType _skillType;
	private int _subType;
	
	@Override
	protected void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = AcquireSkillType.getAcquireSkillType(readD());
		if (_skillType == AcquireSkillType.SUBPLEDGE)
		{
			_subType = readD();
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if ((_level < 1) || (_level > Integer.MAX_VALUE) || (_id < 1) || (_id > Integer.MAX_VALUE))
		{
			Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " sent wrong Packet Data in Aquired Skill");
			return;
		}
		
		final Npc trainer = activeChar.getLastFolkNPC();
		if ((_skillType != AcquireSkillType.CUSTOM && (trainer == null || (trainer != null && !trainer.isNpc()) || (trainer != null && !trainer.canInteract(activeChar)))))
		{
			return;
		}
		
		if ((activeChar.getWeightPenalty() >= 3) || !activeChar.isInventoryUnder90(true))
		{
			activeChar.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
			return;
		}
		
		final Skill skill = SkillsParser.getInstance().getInfo(_id, _level);
		if (skill == null)
		{
			_log.warn(RequestAcquireSkill.class.getSimpleName() + ": Player " + activeChar.getName(null) + " is trying to learn a null skill Id: " + _id + " level: " + _level + "!");
			return;
		}
		
		final int prevSkillLevel = activeChar.getSkillLevel(_id);
		if ((prevSkillLevel > 0) && !((_skillType == AcquireSkillType.TRANSFER) || (_skillType == AcquireSkillType.SUBPLEDGE)))
		{
			if (prevSkillLevel == _level)
			{
				_log.warn("Player " + activeChar.getName(null) + " is trying to learn a skill that already knows, Id: " + _id + " level: " + _level + "!");
				return;
			}
			else if (prevSkillLevel != (_level - 1))
			{
				activeChar.sendPacket(SystemMessageId.PREVIOUS_LEVEL_SKILL_NOT_LEARNED);
				Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " is requesting skill Id: " + _id + " level " + _level + " without knowing it's previous level!");
				return;
			}
		}
		
		final SkillLearn s = SkillTreesParser.getInstance().getSkillLearn(_skillType, _id, _level, activeChar);
		if (s == null)
		{
			return;
		}
		
		switch (_skillType)
		{
			case CLASS :
			case CUSTOM :
			{
				if (checkPlayerSkill(activeChar, trainer, s, _skillType))
				{
					giveSkill(activeChar, trainer, skill);
				}
				break;
			}
			case TRANSFORM :
			{
				if (!canTransform(activeChar))
				{
					activeChar.sendPacket(SystemMessageId.NOT_COMPLETED_QUEST_FOR_SKILL_ACQUISITION);
					Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " is requesting skill Id: " + _id + " level " + _level + " without required quests!");
					return;
				}
				
				if (checkPlayerSkill(activeChar, trainer, s, _skillType))
				{
					giveSkill(activeChar, trainer, skill);
				}
				break;
			}
			case FISHING :
			{
				if (checkPlayerSkill(activeChar, trainer, s, _skillType))
				{
					giveSkill(activeChar, trainer, skill);
				}
				break;
			}
			case PLEDGE :
			{
				if (!activeChar.isClanLeader())
				{
					return;
				}
				
				final Clan clan = activeChar.getClan();
				final int repCost = s.getLevelUpSp();
				if (clan.getReputationScore() >= repCost)
				{
					if (Config.LIFE_CRYSTAL_NEEDED)
					{
						for (final ItemHolder item : s.getRequiredItems(_skillType))
						{
							if (!activeChar.destroyItemByItemId("Consume", item.getId(), item.getCount(), trainer, false))
							{
								activeChar.sendPacket(SystemMessageId.ITEM_OR_PREREQUISITES_MISSING_TO_LEARN_SKILL);
								VillageMasterInstance.showPledgeSkillList(activeChar);
								return;
							}
							
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
							sm.addItemName(item.getId());
							sm.addItemNumber(item.getCount());
							activeChar.sendPacket(sm);
						}
					}
					
					clan.takeReputationScore(repCost, true);
					
					final SystemMessage cr = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					cr.addNumber(repCost);
					activeChar.sendPacket(cr);
					
					clan.addNewSkill(skill);
					
					clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
					
					activeChar.sendPacket(new AcquireSkillDone());
					
					VillageMasterInstance.showPledgeSkillList(activeChar);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE);
					VillageMasterInstance.showPledgeSkillList(activeChar);
				}
				break;
			}
			case SUBPLEDGE :
			{
				if (!activeChar.isClanLeader())
				{
					return;
				}
				
				final Clan clan = activeChar.getClan();
				if ((clan.getFortId() == 0) && (clan.getCastleId() == 0))
				{
					return;
				}
				
				if (trainer instanceof SquadTrainer)
				{
					if (!clan.isLearnableSubPledgeSkill(skill, _subType))
					{
						activeChar.sendPacket(SystemMessageId.SQUAD_SKILL_ALREADY_ACQUIRED);
						Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " is requesting skill Id: " + _id + " level " + _level + " without knowing it's previous level!");
						return;
					}
					
					final int rep = s.getLevelUpSp();
					if (clan.getReputationScore() < rep)
					{
						activeChar.sendPacket(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE);
						return;
					}
					
					for (final ItemHolder item : s.getRequiredItems(_skillType))
					{
						if (!activeChar.destroyItemByItemId("SubSkills", item.getId(), item.getCount(), trainer, false))
						{
							activeChar.sendPacket(SystemMessageId.ITEM_OR_PREREQUISITES_MISSING_TO_LEARN_SKILL);
							return;
						}
						
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(item.getId());
						sm.addItemNumber(item.getCount());
						activeChar.sendPacket(sm);
					}
					
					if (rep > 0)
					{
						clan.takeReputationScore(rep, true);
						final SystemMessage cr = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
						cr.addNumber(rep);
						activeChar.sendPacket(cr);
					}
					
					clan.addNewSkill(skill, _subType);
					clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
					activeChar.sendPacket(new AcquireSkillDone());
					
					((SquadTrainer) trainer).showSubUnitSkillList(activeChar);
				}
				break;
			}
			case TRANSFER :
			{
				if (checkPlayerSkill(activeChar, trainer, s, _skillType))
				{
					giveSkill(activeChar, trainer, skill);
				}
				break;
			}
			case SUBCLASS :
			{
				if (activeChar.isSubClassActive())
				{
					activeChar.sendPacket(SystemMessageId.SKILL_NOT_FOR_SUBCLASS);
					Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " is requesting skill Id: " + _id + " level " + _level + " while Sub-Class is active!");
					return;
				}
				
				QuestState st = activeChar.getQuestState("SubClassSkills");
				if (st == null)
				{
					final Quest subClassSkilllsQuest = QuestManager.getInstance().getQuest("SubClassSkills");
					if (subClassSkilllsQuest != null)
					{
						st = subClassSkilllsQuest.newQuestState(activeChar);
					}
					else
					{
						_log.warn("Null SubClassSkills quest, for Sub-Class skill Id: " + _id + " level: " + _level + " for player " + activeChar.getName(null) + "!");
						return;
					}
				}
				
				for (final String varName : QUEST_VAR_NAMES)
				{
					for (int i = 1; i <= Config.MAX_SUBCLASS; i++)
					{
						final String itemOID = st.getGlobalQuestVar(varName + i);
						if (!itemOID.isEmpty() && !itemOID.endsWith(";") && !itemOID.equals("0"))
						{
							if (Util.isDigit(itemOID))
							{
								final int itemObjId = Integer.parseInt(itemOID);
								final ItemInstance item = activeChar.getInventory().getItemByObjectId(itemObjId);
								if (item != null)
								{
									for (final ItemHolder itemIdCount : s.getRequiredItems(_skillType))
									{
										if (item.getId() == itemIdCount.getId())
										{
											if (checkPlayerSkill(activeChar, trainer, s, _skillType))
											{
												giveSkill(activeChar, trainer, skill);
												st.saveGlobalQuestVar(varName + i, skill.getId() + ";");
											}
											return;
										}
									}
								}
								else
								{
									_log.warn("Inexistent item for object Id " + itemObjId + ", for Sub-Class skill Id: " + _id + " level: " + _level + " for player " + activeChar.getName(null) + "!");
								}
							}
							else
							{
								_log.warn("Invalid item object Id " + itemOID + ", for Sub-Class skill Id: " + _id + " level: " + _level + " for player " + activeChar.getName(null) + "!");
							}
						}
					}
				}
				activeChar.sendPacket(SystemMessageId.ITEM_OR_PREREQUISITES_MISSING_TO_LEARN_SKILL);
				showSkillList(trainer, activeChar);
				break;
			}
			case COLLECT :
			{
				if (checkPlayerSkill(activeChar, trainer, s, _skillType))
				{
					giveSkill(activeChar, trainer, skill);
				}
				break;
			}
			default :
			{
				_log.warn("Recived Wrong Packet Data in Aquired Skill, unknown skill type:" + _skillType);
				break;
			}
		}
	}
	
	private boolean checkPlayerSkill(Player player, Npc trainer, SkillLearn s, AcquireSkillType type)
	{
		if (s != null)
		{
			if ((s.getId() == _id) && (s.getLvl() == _level))
			{
				if (s.getGetLevel() > player.getLevel())
				{
					player.sendPacket(SystemMessageId.YOU_DONT_MEET_SKILL_LEVEL_REQUIREMENTS);
					Util.handleIllegalPlayerAction(player, "" + player.getName(null) + ", level " + player.getLevel() + " is requesting skill Id: " + _id + " level " + _level + " without having minimum required level, " + s.getGetLevel() + "!");
					return false;
				}
				
				final int levelUpSp = s.getCalculatedLevelUpSp(player.getClassId(), player.getLearningClass());
				if ((levelUpSp > 0) && (levelUpSp > player.getSp()))
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL);
					showSkillList(trainer, player);
					return false;
				}
				
				if (!Config.DIVINE_SP_BOOK_NEEDED && (_id == Skill.SKILL_DIVINE_INSPIRATION))
				{
					return true;
				}
				
				if (!s.getPreReqSkills().isEmpty())
				{
					for (final SkillHolder skill : s.getPreReqSkills())
					{
						if (player.getSkillLevel(skill.getId()) != skill.getLvl())
						{
							if (skill.getId() == Skill.SKILL_ONYX_BEAST_TRANSFORMATION)
							{
								player.sendPacket(SystemMessageId.YOU_MUST_LEARN_ONYX_BEAST_SKILL);
							}
							else
							{
								player.sendPacket(SystemMessageId.ITEM_OR_PREREQUISITES_MISSING_TO_LEARN_SKILL);
							}
							return false;
						}
					}
				}
				
				if (!s.getRequiredItems(type).isEmpty())
				{
					long reqItemCount = 0;
					for (final ItemHolder item : s.getRequiredItems(type))
					{
						reqItemCount = player.getInventory().getInventoryItemCount(item.getId(), -1);
						if (reqItemCount < item.getCount())
						{
							player.sendPacket(SystemMessageId.ITEM_OR_PREREQUISITES_MISSING_TO_LEARN_SKILL);
							showSkillList(trainer, player);
							return false;
						}
					}
					
					for (final ItemHolder itemIdCount : s.getRequiredItems(type))
					{
						if (!player.destroyItemByItemId("SkillLearn", itemIdCount.getId(), itemIdCount.getCount(), trainer, true))
						{
							Util.handleIllegalPlayerAction(player, "" + player.getName(null) + ", level " + player.getLevel() + " lose required item Id: " + itemIdCount.getId() + " to learn skill while learning skill Id: " + _id + " level " + _level + "!");
						}
					}
				}
				
				if (levelUpSp > 0)
				{
					player.setSp(player.getSp() - levelUpSp);
					player.sendStatusUpdate(false, false, StatusUpdate.SP);
				}
				return true;
			}
		}
		return false;
	}
	
	private void giveSkill(Player player, Npc trainer, Skill skill)
	{
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.LEARNED_SKILL_S1);
		sm.addSkillName(skill);
		player.sendPacket(sm);
		
		player.sendPacket(new AcquireSkillDone());
		
		player.addSkill(skill, true);
		player.sendSkillList(false);
		
		player.updateShortCuts(_id, _level);
		showSkillList(trainer, player);
		
		if ((_id >= 1368) && (_id <= 1372))
		{
			player.sendPacket(new ExStorageMaxCount(player));
		}
		
		if (trainer != null && trainer.getTemplate().getEventQuests().containsKey(QuestEventType.ON_SKILL_LEARN))
		{
			for (final Quest quest : trainer.getTemplate().getEventQuests(QuestEventType.ON_SKILL_LEARN))
			{
				quest.notifyAcquireSkill(trainer, player, skill, _skillType);
			}
		}
	}
	
	private void showSkillList(Npc trainer, Player player)
	{
		if ((_skillType == AcquireSkillType.TRANSFORM) || (_skillType == AcquireSkillType.SUBCLASS) || (_skillType == AcquireSkillType.TRANSFER))
		{
			return;
		}
		
		if (_skillType == AcquireSkillType.CUSTOM)
		{
			if (trainer != null)
			{
				NpcInstance.showCustomSkillList(player, player.getLearningGroupId());
			}
			else
			{
				final var handler = CommunityBoardHandler.getInstance().getHandler("_bbsCustomSkillGroup");
				if (handler != null)
				{
					handler.onBypassCommand("_bbsCustomSkillGroup " + player.getLearningGroupId() + "", player);
				}
			}
		}
		else
		{
			if (trainer != null && trainer instanceof FishermanInstance)
			{
				FishermanInstance.showFishSkillList(player);
			}
			else
			{
				if (trainer != null && trainer.canInteract(player))
				{
					NpcInstance.showSkillList(player, trainer, player.getLearningClass());
				}
			}
		}
	}
	
	public static boolean canTransform(Player player)
	{
		if (Config.ALLOW_TRANSFORM_WITHOUT_QUEST)
		{
			return true;
		}
		final QuestState st = player.getQuestState("_136_MoreThanMeetsTheEye");
		return (st != null) && st.isCompleted();
	}
}