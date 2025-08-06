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
package l2e.scripts.quests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import l2e.commons.util.Util;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.serverpackets.SpecialCamera;

/**
 * Rework by LordWinter 13.12.2020
 */
public class _144_PailakaInjuredDragon extends Quest
{
	private static int buff_counter = 5;
	private static boolean _hasDoneAnimation = false;
	
	private static final int[][] BUFFS =
	{
	        {
	                4357, 2
			},
			{
			        4342, 2
			},
			{
			        4356, 3
			},
			{
			        4355, 3
			},
			{
			        4351, 6
			},
			{
			        4345, 3
			},
			{
			        4358, 3
			},
			{
			        4359, 3
			},
			{
			        4360, 3
			},
			{
			        4352, 2
			},
			{
			        4354, 4
			},
			{
			        4347, 6
			}
	};
	
	private static final List<PailakaDrop> DROPLIST = new ArrayList<>();
	static
	{
		DROPLIST.add(new PailakaDrop(13033, 80));
		DROPLIST.add(new PailakaDrop(13032, 30));
	}
	
	public _144_PailakaInjuredDragon(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32499);
		addFirstTalkId(32499, 32502, 32509, 32512);
		addTalkId(32499, 32502, 32509, 32512);
		
		addAggroRangeEnterId(18660);
		addAttackId(18660);
		addKillId(18637, 18643, 18651, 18647, 18660, 18636, 18642, 18646, 18654, 18635, 18657, 18653, 18649, 18650, 18655, 18659, 18658, 18656, 18652, 18640, 18645, 18648, 18644, 18641);
		
		questItemIds = new int[]
		{
		        13052, 13053, 13054, 13056, 13057, 13032, 13033
		};
	}
	
	private static final void dropItem(Npc mob, Player player)
	{
		Collections.shuffle(DROPLIST);
		for (final PailakaDrop pd : DROPLIST)
		{
			if (getRandom(100) < pd.getChance())
			{
				((MonsterInstance) mob).dropSingleItem(player, pd.getItemID(), getRandom(1, 6));
				return;
			}
		}
	}
	
	private static void giveBuff(Npc npc, Player player, int skillId, int level)
	{
		npc.setTarget(player);
		npc.doCast(SkillsParser.getInstance().getInfo(skillId, level));
		buff_counter--;
		return;
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return getNoQuestMsg(player);
		}
		
		final int cond = st.getCond();
		if (event.equalsIgnoreCase("32499-02.htm"))
		{
			if (cond == 0)
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32499-05.htm"))
		{
			if (cond == 1)
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("32502-05.htm"))
		{
			if (cond == 2)
			{
				st.setCond(3, true);
				if (!st.hasQuestItems(13052))
				{
					st.giveItems(13052, 1);
				}
			}
		}
		else if (event.equalsIgnoreCase("32509-02.htm"))
		{
			switch (cond)
			{
				case 2 :
				case 3 :
					return "32509-07.htm";
				case 4 :
					st.setCond(5, true);
					st.takeItems(13052, 1);
					st.takeItems(13056, 1);
					st.giveItems(13053, 1);
					return "32509-02.htm";
				case 5 :
					return "32509-01.htm";
				case 6 :
					st.setCond(7, true);
					st.takeItems(13053, 1);
					st.takeItems(13057, 1);
					st.giveItems(13054, 1);
					return "32509-03.htm";
				case 7 :
					return "32509-03.htm";
				default :
					break;
			}
		}
		else if (event.equalsIgnoreCase("32509-06.htm"))
		{
			if (buff_counter < 1)
			{
				return "32509-05.htm";
			}
		}
		else if (event.equalsIgnoreCase("32512-02.htm"))
		{
			st.exitQuest(false, true);
			final Reflection inst = ReflectionManager.getInstance().getReflection(npc.getReflectionId());
			if (inst != null)
			{
				inst.setDuration(300000);
				inst.setEmptyDestroyTime(0);
			}
			player.setVitalityPoints(20000, true);
			st.calcExpAndSp(getId());
			st.calcReward(getId());
		}
		else if (event.startsWith("buff"))
		{
			if (buff_counter > 0)
			{
				final int nr = Integer.parseInt(event.split("buff")[1]);
				giveBuff(npc, player, BUFFS[nr - 1][0], BUFFS[nr - 1][1]);
				return "32509-06.htm";
			}
			return "32509-05.htm";
		}
		else if (event.equalsIgnoreCase("latana_animation"))
		{
			_hasDoneAnimation = true;
			npc.abortAttack();
			npc.abortCast();
			npc.getAI().setIntention(CtrlIntention.IDLE);
			player.abortAttack();
			player.abortCast();
			player.stopMove(null);
			player.setTarget(null);
			if (player.hasSummon())
			{
				player.getSummon().abortAttack();
				player.getSummon().abortCast();
				player.getSummon().stopMove(null);
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
			}
			
			player.sendPacket(new SpecialCamera(npc, 600, 0, 0, 1000, 11000, 1, 0, 1, 0, 0));
			startQuestTimer("latana_animation2", 1000, npc, player);
			return null;
		}
		else if (event.equalsIgnoreCase("latana_animation2"))
		{
			npc.doCast(SkillsParser.getInstance().getInfo(5759, 1));
			npc.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player, 0);
			return null;
		}
		return event;
	}
	
	@Override
	public final String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return getNoQuestMsg(player);
		}
		
		final int cond = st.getCond();
		switch (npc.getId())
		{
			case 32499 :
				switch (st.getState())
				{
					case State.CREATED :
						if (player.getLevel() < getMinLvl(getId()))
						{
							return "32499-no.htm";
						}
						if (player.getLevel() > getMaxLvl(getId()))
						{
							return "32499-no.htm";
						}
						return "32499-01.htm";
					case State.STARTED :
						if (player.getLevel() < getMinLvl(getId()))
						{
							return "32499-no.htm";
						}
						if (player.getLevel() > getMaxLvl(getId()))
						{
							return "32499-no.htm";
						}
						if (cond > 1)
						{
							return "32499-06.htm";
						}
					case State.COMPLETED :
						return "32499-completed.htm";
					default :
						return "32499-no.htm";
				}
			case 32502 :
				if (cond > 2)
				{
					return "32502-05.htm";
				}
				return "32502-01.htm";
			case 32509 :
				return "32509-00.htm";
			case 32512 :
				if (st.getState() == State.COMPLETED)
				{
					return "32512-03.htm";
				}
				else
				{
					return "32512-01.htm";
				}
		}
		return getNoQuestMsg(player);
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if ((st == null) || (st.getState() != State.STARTED))
		{
			return null;
		}
		
		final int cond = st.getCond();
		switch (npc.getId())
		{
			case 18636 :
			case 18635 :
				if ((cond == 3) && st.hasQuestItems(13052) && !st.hasQuestItems(13056) && (getRandom(100) < 20))
				{
					st.setCond(4, true);
					st.giveItems(13056, 1);
				}
				spawnMageBehind(npc, player, 18644);
				checkIfLastInWall(npc);
				break;
			case 18642 :
				if ((cond == 3) && st.hasQuestItems(13052) && !st.hasQuestItems(13056) && (getRandom(100) < 25))
				{
					st.setCond(4, true);
					st.giveItems(13056, 1);
				}
				spawnMageBehind(npc, player, 18641);
				checkIfLastInWall(npc);
				break;
			case 18653 :
				if ((cond == 3) && st.hasQuestItems(13052) && !st.hasQuestItems(13056) && (getRandom(100) < 30))
				{
					st.setCond(4, true);
					st.giveItems(13056, 1);
				}
				spawnMageBehind(npc, player, 18640);
				checkIfLastInWall(npc);
				break;
			case 18654 :
			case 18646 :
				if ((cond == 3) && st.hasQuestItems(13052) && !st.hasQuestItems(13056) && (getRandom(100) < 40))
				{
					st.setCond(4, true);
					st.giveItems(13056, 1);
				}
				spawnMageBehind(npc, player, 18648);
				checkIfLastInWall(npc);
				break;
			case 18649 :
			case 18650 :
				if ((cond == 5) && st.hasQuestItems(13053) && !st.hasQuestItems(13057) && (getRandom(100) < 20))
				{
					st.setCond(6, true);
					st.giveItems(13057, 1);
				}
				spawnMageBehind(npc, player, 18645);
				checkIfLastInWall(npc);
				break;
			case 18659 :
				if ((cond == 5) && st.hasQuestItems(13053) && !st.hasQuestItems(13057) && (getRandom(100) < 25))
				{
					st.setCond(6, true);
					st.giveItems(13057, 1);
				}
				spawnMageBehind(npc, player, 18658);
				checkIfLastInWall(npc);
				break;
			case 18655 :
				if ((cond == 5) && st.hasQuestItems(13053) && !st.hasQuestItems(13057) && (getRandom(100) < 30))
				{
					st.setCond(6, true);
					st.giveItems(13057, 1);
				}
				spawnMageBehind(npc, player, 18656);
				checkIfLastInWall(npc);
				break;
			case 18657 :
				if ((cond == 5) && st.hasQuestItems(13053) && !st.hasQuestItems(13057) && (getRandom(100) < 40))
				{
					st.setCond(6, true);
					st.giveItems(13057, 1);
				}
				spawnMageBehind(npc, player, 18652);
				checkIfLastInWall(npc);
				break;
			case 18660 :
				st.setCond(8, true);
				addSpawn(32512, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0, false, npc.getReflection());
				break;
			case 18637 :
			case 18643 :
			case 18651 :
			case 18647 :
				dropItem(npc, player);
				break;
			default :
				break;
		}
		return super.onKill(npc, player, isSummon);
	}
	
	private final void spawnMageBehind(Npc npc, Player player, int mageId)
	{
		final double rads = Math.toRadians(Util.convertHeadingToDegree(npc.getSpawn().getHeading()) + 180);
		final int mageX = (int) (npc.getX() + (150 * Math.cos(rads)));
		final int mageY = (int) (npc.getY() + (150 * Math.sin(rads)));
		final Npc mageBack = addSpawn(mageId, mageX, mageY, npc.getZ(), npc.getSpawn().getHeading(), false, 0, true, npc.getReflection());
		mageBack.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, player, 1000);
	}
	
	private final void checkIfLastInWall(Npc npc)
	{
		final Collection<Npc> knowns = World.getInstance().getAroundNpc(npc, 700, 200);
		for (final Npc knownNpc : knowns)
		{
			if (knownNpc.isDead())
			{
				continue;
			}
			
			switch (npc.getId())
			{
				case 18636 :
				case 18635 :
				case 18642 :
					switch (knownNpc.getId())
					{
						case 18636 :
						case 18635 :
						case 18642 :
							return;
					}
					break;
				case 18653 :
				case 18654 :
				case 18646 :
					switch (knownNpc.getId())
					{
						case 18653 :
						case 18654 :
						case 18646 :
							return;
					}
					break;
				case 18649 :
				case 18650 :
				case 18659 :
					switch (knownNpc.getId())
					{
						case 18649 :
						case 18650 :
						case 18659 :
							return;
					}
					break;
				case 18655 :
				case 18657 :
					switch (knownNpc.getId())
					{
						case 18655 :
						case 18657 :
							return;
					}
					break;
			}
		}
		
		for (final Creature npcs : knowns)
		{
			if (!(npcs instanceof Npc))
			{
				continue;
			}
			
			if (npcs.isDead())
			{
				continue;
			}
			
			final Npc knownNpc = (Npc) npcs;
			
			switch (npc.getId())
			{
				case 18636 :
				case 18635 :
				case 18642 :
					switch (knownNpc.getId())
					{
						case 18644 :
						case 18641 :
							knownNpc.abortCast();
							knownNpc.deleteMe();
							break;
					}
					break;
				case 18653 :
				case 18654 :
				case 18646 :
					switch (knownNpc.getId())
					{
						case 18640 :
						case 18648 :
							knownNpc.abortCast();
							knownNpc.deleteMe();
							break;
					}
					break;
				case 18649 :
				case 18650 :
				case 18659 :
					switch (knownNpc.getId())
					{
						case 18645 :
						case 18658 :
							knownNpc.abortCast();
							knownNpc.deleteMe();
							break;
					}
					break;
				case 18655 :
				case 18657 :
					switch (knownNpc.getId())
					{
						case 18656 :
						case 18652 :
							knownNpc.abortCast();
							knownNpc.deleteMe();
							break;
					}
					break;
			}
		}
	}
	
	@Override
	public final String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if ((st == null) || (st.getState() != State.STARTED))
		{
			return null;
		}
		
		if (isSummon)
		{
			return null;
		}
		
		switch (npc.getId())
		{
			case 18660 :
				if (!_hasDoneAnimation)
				{
					startQuestTimer("latana_animation", 600, npc, player);
					return null;
				}
				break;
		}
		return super.onAggroRangeEnter(npc, player, isSummon);
	}
	
	@Override
	public final String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (attacker == null)
		{
			return super.onAttack(npc, attacker, damage, isSummon);
		}
		
		switch (npc.getId())
		{
			case 18660 :
				if (!_hasDoneAnimation)
				{
					final QuestState st = attacker.getQuestState(getName());
					if ((st == null) || (st.getState() != State.STARTED))
					{
						return super.onAttack(npc, attacker, damage, isSummon);
					}
					startQuestTimer("latana_animation", 600, npc, attacker);
					return null;
				}
				break;
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	private static class PailakaDrop
	{
		private final int _itemId;
		private final int _chance;
		
		public PailakaDrop(int itemId, int chance)
		{
			_itemId = itemId;
			_chance = chance;
		}
		
		public int getItemID()
		{
			return _itemId;
		}
		
		public int getChance()
		{
			return _chance;
		}
	}
	
	public static void main(String[] args)
	{
		new _144_PailakaInjuredDragon(144, _144_PailakaInjuredDragon.class.getSimpleName(), "");
	}
}