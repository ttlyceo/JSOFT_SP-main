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


import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Updated by LordWinter 14.08.2020
 **/
public class _311_ExpulsionOfEvilSpirits extends Quest
{
	private Npc _varangka;
	private Npc _varangkaMinion1;
	private Npc _varangkaMinion2;
	protected Npc _altar;
	private long respawnTime = 0;
	
	public _311_ExpulsionOfEvilSpirits(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32655);
		addTalkId(32655);
		addKillId(22691, 22692, 22693, 22694, 22695, 22696, 22697, 22698, 22699, 22701, 22702, 18808, 18809, 18810);
		
		questItemIds = new int[]
		{
		        14881, 14882
		};
		
		addEnterZoneId(20201);
		addAttackId(18811);
		
		try
		{
			respawnTime = Long.valueOf(loadGlobalQuestVar("VarangkaRespawn"));
		}
		catch (final Exception e)
		{
		
		}
		saveGlobalQuestVar("VarangkaRespawn", String.valueOf(respawnTime));
		if ((respawnTime == 0) || ((respawnTime - System.currentTimeMillis()) < 0))
		{
			startQuestTimer("altarSpawn", 5000, null, null);
		}
		else
		{
			startQuestTimer("altarSpawn", respawnTime - System.currentTimeMillis(), null, null);
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("altarSpawn"))
		{
			if (!checkIfSpawned(18811))
			{
				_altar = addSpawn(18811, 74120, -101920, -960, 32760, false, 0);
				_altar.setIsInvul(true);
				saveGlobalQuestVar("VarangkaRespawn", String.valueOf(0));
				for (final Player pc : World.getInstance().getAroundPlayers(_altar, 1200, 200))
				{
					ThreadPoolManager.getInstance().schedule(new zoneCheck(pc), 1000);
				}
			}
			return null;
		}
		else if (event.equalsIgnoreCase("minion1") && checkIfSpawned(18808))
		{
			if (!checkIfSpawned(18809) && checkIfSpawned(18808))
			{
				_varangkaMinion1 = addSpawn(18809, player.getX() + Rnd.get(10, 50), player.getY() + Rnd.get(10, 50), -967, 0, false, 0);
				_varangkaMinion1.setRunning();
				((Attackable) _varangkaMinion1).addDamageHate(_varangka.getTarget().getActingPlayer(), 1, 99999);
				_varangkaMinion1.getAI().setIntention(CtrlIntention.ATTACK, _varangka.getTarget().getActingPlayer());
			}
			return null;
		}
		else if (event.equalsIgnoreCase("minion2"))
		{
			if (!checkIfSpawned(18810) && checkIfSpawned(18808))
			{
				_varangkaMinion2 = addSpawn(18810, player.getX() + Rnd.get(10, 50), player.getY() + Rnd.get(10, 50), -967, 0, false, 0);
				_varangkaMinion2.setRunning();
				((Attackable) _varangkaMinion2).addDamageHate(_varangka.getTarget().getActingPlayer(), 1, 99999);
				_varangkaMinion2.getAI().setIntention(CtrlIntention.ATTACK, _varangka.getTarget().getActingPlayer());
			}
			return null;
		}
		
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		if (player.getLevel() < getMinLvl(getId()))
		{
			return null;
		}
		
		switch (event)
		{
			case "32655-03.htm" :
			case "32655-15.htm" :
			{
				htmltext = event;
				break;
			}
			case "32655-04.htm" :
			{
				if (qs.isCreated())
				{
					qs.startQuest();
				}
				htmltext = event;
				break;
			}
			case "32655-11.htm" :
			{
				if (getQuestItemsCount(player, 14881) >= 10)
				{
					takeItems(player, 14881, 10);
					giveItems(player, 14848, 1);
					htmltext = event;
				}
				else
				{
					htmltext = "32655-12.htm";
				}
				break;
			}
			case "32655-13.htm" :
			{
				if (!hasQuestItems(player, 14881) && (getQuestItemsCount(player, 14882) >= 10))
				{
					qs.exitQuest(true, true);
					htmltext = event;
				}
				else
				{
					htmltext = "32655-14.htm";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, 1, 2, npc);
		if (qs != null)
		{
			if (npc.getId() == 18808)
			{
				if ((qs.getCond() != 1))
				{
					return null;
				}
				qs.takeItems(14848, 1);
				if (_altar != null)
				{
					_altar.doDie(killer);
					_altar = null;
				}
				_varangka = null;
				if (checkIfSpawned(18809))
				{
					if (_varangkaMinion1 != null)
					{
						_varangkaMinion1.doDie(killer);
					}
				}
				if (checkIfSpawned(18810))
				{
					if (_varangkaMinion2 != null)
					{
						_varangkaMinion2.doDie(killer);
					}
				}
				cancelQuestTimers("minion1");
				cancelQuestTimers("minion2");
				_varangkaMinion1 = null;
				_varangkaMinion2 = null;
				final long respawn = Rnd.get(14400000, 28800000);
				saveGlobalQuestVar("VarangkaRespawn", String.valueOf(System.currentTimeMillis() + respawn));
				startQuestTimer("altarSpawn", respawn, null, null);
				return super.onKill(npc, killer, isSummon);
			}
			else if (npc.getId() == (18809))
			{
				_varangkaMinion1 = null;
				startQuestTimer("minion1", Rnd.get(60000, 120000), npc, killer);
				return super.onKill(npc, killer, isSummon);
			}
			else if (npc.getId() == (18810))
			{
				_varangkaMinion2 = null;
				startQuestTimer("minion2", Rnd.get(60000, 120000), npc, killer);
				return super.onKill(npc, killer, isSummon);
			}
			qs.calcDoDropItems(getId(), 14881, npc.getId(), Integer.MAX_VALUE);
			qs.calcDoDropItems(getId(), 14882, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs == null)
		{
			return htmltext;
		}
		
		if (qs.isCreated())
		{
			htmltext = (player.getLevel() >= getMinLvl(getId())) ? "32655-01.htm" : "32655-02.htm";
		}
		else if (qs.isStarted())
		{
			htmltext = !hasQuestItems(player, 14881, 14882) ? "32655-05.htm" : "32655-06.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onAttack(Npc npc, Player player, int damage, boolean isPet)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		if ((st.getQuestItemsCount(14848) > 0) && (Rnd.get(100) < 20))
		{
			if ((_varangka == null) && !checkIfSpawned(18808))
			{
				_varangka = addSpawn(18808, 74914, -101922, -967, 0, false, 0);
				if ((_varangkaMinion1 == null) && !checkIfSpawned(18809))
				{
					_varangkaMinion1 = addSpawn(18809, 74914 + Rnd.get(10, 50), -101922 + Rnd.get(10, 50), -967, 0, false, 0);
				}
				if ((_varangkaMinion2 == null) && !checkIfSpawned(18810))
				{
					_varangkaMinion2 = addSpawn(18810, 74914 + Rnd.get(10, 50), -101922 + Rnd.get(10, 50), -967, 0, false, 0);
				}
				final ZoneType zone = ZoneManager.getInstance().getZoneById(20201);
				for (final Creature c : zone.getCharactersInside())
				{
					if (c instanceof Attackable)
					{
						if ((c.getId() >= 18808) && (c.getId() <= (18810)))
						{
							c.setRunning();
							((Attackable) c).addDamageHate(player, 1, 99999);
							c.getAI().setIntention(CtrlIntention.ATTACK, player);
						}
					}
				}
			}
		}
		else if (st.getQuestItemsCount(14848) == 0)
		{
			ThreadPoolManager.getInstance().schedule(new zoneCheck(player), 1000);
		}
		return super.onAttack(npc, player, damage, isPet);
	}
	
	@Override
	public String onEnterZone(Creature character, ZoneType zone)
	{
		if (character.isPlayer())
		{
			ThreadPoolManager.getInstance().schedule(new zoneCheck(character.getActingPlayer()), 1000);
		}
		return super.onEnterZone(character, zone);
	}
	
	private class zoneCheck implements Runnable
	{
		private final Player _player;
		
		protected zoneCheck(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_altar != null)
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneById(20201);
				if (zone.isCharacterInZone(_player))
				{
					final QuestState st = _player.getQuestState(getName());
					if (st == null)
					{
						castDebuff(_player);
						ThreadPoolManager.getInstance().schedule(new zoneCheck(_player), 3000);
					}
					else if (st.getQuestItemsCount(14848) == 0)
					{
						castDebuff(_player);
						ThreadPoolManager.getInstance().schedule(new zoneCheck(_player), 3000);
					}
				}
			}
		}
		
		private void castDebuff(Player player)
		{
			final int skillId = 6148;
			final int skillLevel = 1;
			if (player.getFirstEffect(skillId) != null)
			{
				player.stopSkillEffects(skillId);
			}
			final Skill skill = SkillsParser.getInstance().getInfo(skillId, skillLevel);
			skill.getEffects(_altar, player, false);
			_altar.broadcastPacketToOthers(2000, new MagicSkillUse(_altar, player, skillId, skillLevel, 1000, 0));
		}
	}
	
	private boolean checkIfSpawned(int npcId)
	{
		final ZoneType zone = ZoneManager.getInstance().getZoneById(20201);
		for (final Creature c : zone.getCharactersInside())
		{
			if (c.getId() == npcId)
			{
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args)
	{
		new _311_ExpulsionOfEvilSpirits(311, _311_ExpulsionOfEvilSpirits.class.getSimpleName(), "");
	}
}