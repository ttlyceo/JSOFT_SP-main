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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.instancemanager.DoubleSessionManager;
import l2e.gameserver.instancemanager.RewardManager;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.instancemanager.TerritoryWarManager.TerritoryNPCSpawn;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.TerritoryWard;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;

public class TerritoryWarSuperClass extends Quest
{
	private static Map<Integer, TerritoryWarSuperClass> _forTheSakeScripts = new HashMap<>();
	private static Map<Integer, TerritoryWarSuperClass> _protectTheScripts = new HashMap<>();
	private static Map<Integer, TerritoryWarSuperClass> _killTheScripts = new HashMap<>();

	public static String qn = "TerritoryWarSuperClass";

	public int CATAPULT_ID;
	public int TERRITORY_ID;
	public int[] LEADER_IDS;
	public int[] GUARD_IDS;
	public NpcStringId[] npcString =
	{};

	public int[] NPC_IDS;

	public int[] CLASS_IDS;
	public int RANDOM_MIN;
	public int RANDOM_MAX;

	public void registerKillIds()
	{
		addKillId(CATAPULT_ID);
		for (final int mobid : LEADER_IDS)
		{
			addKillId(mobid);
		}
		for (final int mobid : GUARD_IDS)
		{
			addKillId(mobid);
		}
	}

	public void registerAttackIds()
	{
		for (final int mobid : NPC_IDS)
		{
			addAttackId(mobid);
		}
	}

	@Override
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		if (ArrayUtils.contains(targets, npc))
		{
			if (skill.getId() == 847)
			{
				if (TerritoryWarManager.getInstance().getHQForClan(caster.getClan()) != npc)
				{
					return super.onSkillSee(npc, caster, skill, targets, isSummon);
				}
				
				final TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(caster);
				if (ward == null)
				{
					return super.onSkillSee(npc, caster, skill, targets, isSummon);
				}
				if ((caster.getSiegeSide() - 80) == ward.getOwnerCastleId())
				{
					for (final TerritoryNPCSpawn wardSpawn : TerritoryWarManager.getInstance().getTerritory(ward.getOwnerCastleId()).getOwnedWard())
					{
						if (wardSpawn.getId() == ward.getTerritoryId())
						{
							wardSpawn.setNPC(wardSpawn.getNpc().getSpawn().doSpawn());
							ward.unSpawnMe();
							ward.setNpc(wardSpawn.getNpc());
						}
					}
				}
				else
				{
					if (!TerritoryWarManager.getInstance().canStealWard(caster))
					{
						caster.sendMessage("Your castle owns the maximum amount of wards!");
						return super.onSkillSee(npc, caster, skill, targets, isSummon);
					}
					ward.unSpawnMe();
					ward.setNpc(TerritoryWarManager.getInstance().addTerritoryWard(ward.getTerritoryId(), caster.getSiegeSide() - 80, ward.getOwnerCastleId(), true));
					ward.setOwnerCastleId(caster.getSiegeSide() - 80);
					TerritoryWarManager.getInstance().getTerritory(caster.getSiegeSide() - 80).getQuestDone()[1]++;
					caster.getCounters().addAchivementInfo("stealTerritoryWards", 0, -1, false, false, true);
				}
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}

	public int getTerritoryIdForThisNPCId(int npcid)
	{
		return 0;
	}

	@Override
	public String onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if ((npc.getCurrentHp() == npc.getMaxHp()) && ArrayUtils.contains(NPC_IDS, npc.getId()))
		{
			final int territoryId = getTerritoryIdForThisNPCId(npc.getId());
			if ((territoryId >= 81) && (territoryId <= 89))
			{
				for (final Player pl : GameObjectsStorage.getPlayers())
				{
					if (pl.getSiegeSide() == territoryId)
					{
						QuestState st = pl.getQuestState(getName());
						if (st == null)
						{
							st = newQuestState(pl);
						}
						if (st.getState() != State.STARTED)
						{
							st.setCond(1);
							st.setState(State.STARTED, false);
						}
					}
				}
			}
		}
		return super.onAttack(npc, player, damage, isSummon);
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == CATAPULT_ID)
		{
			TerritoryWarManager.getInstance().territoryCatapultDestroyed(TERRITORY_ID - 80);
			TerritoryWarManager.getInstance().giveTWPoint(killer, TERRITORY_ID, 4);
			TerritoryWarManager.getInstance().announceToParticipants(new ExShowScreenMessage(npcString[0], 2, 10000), 135000, 13500);
			handleBecomeMercenaryQuest(killer, true);
			if (killer != null)
			{
				killer.getCounters().addAchivementInfo("killCatapultAtTw", 0, -1, false, false, false);
			}
		}
		else if (ArrayUtils.contains(LEADER_IDS, npc.getId()))
		{
			TerritoryWarManager.getInstance().giveTWPoint(killer, TERRITORY_ID, 3);
		}

		if ((killer.getSiegeSide() != TERRITORY_ID) && (TerritoryWarManager.getInstance().getTerritory(killer.getSiegeSide() - 80) != null))
		{
			TerritoryWarManager.getInstance().getTerritory(killer.getSiegeSide() - 80).getQuestDone()[0]++;
		}
		return super.onKill(npc, killer, isSummon);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if ((npc != null) || (player != null))
		{
			return null;
		}
		final StringTokenizer st = new StringTokenizer(event, " ");
		event = st.nextToken();
		if (event.equalsIgnoreCase("setNextTWDate"))
		{
			final Calendar startTWDate = Calendar.getInstance();
			startTWDate.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
			startTWDate.set(Calendar.HOUR_OF_DAY, 20);
			startTWDate.set(Calendar.MINUTE, 0);
			startTWDate.set(Calendar.SECOND, 0);
			if (startTWDate.getTimeInMillis() < System.currentTimeMillis())
			{
				startTWDate.add(Calendar.DAY_OF_MONTH, 7);
			}
			
			if (Config.ALLOW_CHECK_SEVEN_SIGN_STATUS)
			{
				if (!SevenSigns.getInstance().isDateInSealValidPeriod(startTWDate))
				{
					startTWDate.add(Calendar.DAY_OF_MONTH, 7);
				}
			}
			ServerVariables.set("TerritoryWarDate", startTWDate.getTimeInMillis());
			TerritoryWarManager.getInstance().setTWStartTimeInMillis(startTWDate.getTimeInMillis());
			_log.info("TerritoryWarManager: Next battle " + startTWDate.getTime());
		}
		else if (event.equalsIgnoreCase("setTWDate") && st.hasMoreTokens())
		{
			final Calendar startTWDate = Calendar.getInstance();
			startTWDate.setTimeInMillis(Long.parseLong(st.nextToken()));
			ServerVariables.set("TerritoryWarDate", startTWDate.getTimeInMillis());
			TerritoryWarManager.getInstance().setTWStartTimeInMillis(startTWDate.getTimeInMillis());
			_log.info("TerritoryWarManager: Next battle " + startTWDate.getTime());
		}
		return null;
	}

	private void handleKillTheQuest(Player player)
	{
		QuestState st = player.getQuestState(getName());
		int kill = 1;
		int max = 10;
		if (st == null)
		{
			st = newQuestState(player);
		}
		if (!st.isCompleted())
		{
			if (!st.isStarted())
			{
				st.setState(State.STARTED);
				st.set("cond", "1");
				st.set("kill", "1");
				max = getRandom(RANDOM_MIN, RANDOM_MAX);
				st.set("max", String.valueOf(max));
			}
			else
			{
				kill = st.getInt("kill") + 1;
				max = st.getInt("max");
			}
			
			if (kill >= max)
			{
				TerritoryWarManager.getInstance().giveTWQuestPoint(player);
				st.addExpAndSp(534000, 51000);
				st.set("doneDate", String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_YEAR)));
				st.setState(State.COMPLETED);
				st.exitQuest(true);
				player.sendPacket(new ExShowScreenMessage(npcString[1], 2, 10000));
			}
			else
			{
				st.set("kill", String.valueOf(kill));
				player.sendPacket(new ExShowScreenMessage(npcString[0], 2, 10000, String.valueOf(max), String.valueOf(kill)));
			}
		}
		else if (st.getInt("doneDate") != Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
		{
			st.setState(State.STARTED);
			st.set("cond", "1");
			st.set("kill", "1");
			max = getRandom(RANDOM_MIN, RANDOM_MAX);
			st.set("max", String.valueOf(max));
			player.sendPacket(new ExShowScreenMessage(npcString[0], 2, 10000, String.valueOf(max), String.valueOf(kill)));
		}
	}

	private static void handleBecomeMercenaryQuest(Player player, boolean catapult)
	{
		int enemyCount = 10, catapultCount = 1;
		QuestState st = player.getQuestState(_147_PathtoBecominganEliteMercenary.class.getSimpleName());
		if ((st != null) && st.isCompleted())
		{
			st = player.getQuestState(_148_PathtoBecominganExaltedMercenary.class.getSimpleName());
			enemyCount = 30;
			catapultCount = 2;
		}

		if ((st != null) && st.isStarted())
		{
			if (catapult)
			{
				if (st.isCond(1) || st.isCond(2))
				{
					int count = st.getInt("catapult");
					count++;
					st.set("catapult", String.valueOf(count));
					if (count >= catapultCount)
					{
						if (st.isCond(1))
						{
							st.setCond(3);
						}
						else
						{
							st.setCond(4);
						}
					}
				}
			}
			else
			{
				if (st.isCond(1) || st.isCond(3))
				{
					int _kills = st.getInt("kills");
					_kills++;
					st.set("kills", String.valueOf(_kills));
					if (_kills >= enemyCount)
					{
						if (st.isCond(1))
						{
							st.setCond(2);
						}
						else
						{
							st.setCond(4);
						}
					}
				}
			}
		}
	}

	private void handleStepsForHonor(Player player)
	{
		int kills = 0;
		int cond = 0;

		final QuestState _sfh = player.getQuestState("_176_StepsForHonor");
		if ((_sfh != null) && (_sfh.getState() == State.STARTED))
		{
			cond = _sfh.getInt("cond");
			if ((cond == 1) || (cond == 3) || (cond == 5) || (cond == 7))
			{
				kills = _sfh.getInt("kills");
				kills++;
				_sfh.set("kills", String.valueOf(kills));
				if ((cond == 1) && (kills >= 9))
				{
					_sfh.set("cond", "2");
					_sfh.set("kills", "0");
				}
				else if ((cond == 3) && (kills >= 18))
				{
					_sfh.set("cond", "4");
					_sfh.set("kills", "0");
				}
				else if ((cond == 5) && (kills >= 27))
				{
					_sfh.set("cond", "6");
					_sfh.set("kills", "0");
				}
				else if ((cond == 7) && (kills >= 36))
				{
					_sfh.set("cond", "8");
					_sfh.unset("kills");
				}
			}
		}
	}

	@Override
	public String onDeath(Creature killer, Creature victim, QuestState qs)
	{
		if ((killer == victim) || !(victim.isPlayer()) || (victim.getLevel() < 61) || !victim.isInsideZone(ZoneId.SIEGE))
		{
			return "";
		}
		final Player actingPlayer = killer.getActingPlayer();
		if ((actingPlayer != null) && (qs.getPlayer() != null))
		{
			if (actingPlayer.getUCState() > 0 || actingPlayer.isInFightEvent())
			{
				return "";
			}
			
			if (actingPlayer.getParty() != null)
			{
				for (final Player pl : actingPlayer.getParty().getMembers())
				{
					if ((pl.getSiegeSide() == qs.getPlayer().getSiegeSide()) || (pl.getSiegeSide() == 0) || !Util.checkIfInRange(2000, killer, pl, false))
					{
						continue;
					}
					if (pl == actingPlayer)
					{
						handleStepsForHonor(actingPlayer);
						handleBecomeMercenaryQuest(actingPlayer, false);
					}
					handleKillTheQuest(pl);
				}
			}
			else if ((actingPlayer.getSiegeSide() != qs.getPlayer().getSiegeSide()) && (actingPlayer.getSiegeSide() > 0))
			{
				handleKillTheQuest(actingPlayer);
				handleStepsForHonor(actingPlayer);
				handleBecomeMercenaryQuest(actingPlayer, false);
			}
			TerritoryWarManager.getInstance().giveTWPoint(actingPlayer, qs.getPlayer().getSiegeSide(), 1);
			if (DoubleSessionManager.getInstance().check(actingPlayer, victim))
			{
				RewardManager.getInstance().checkTerritoryWarReward(actingPlayer, victim.getActingPlayer());
				actingPlayer.getCounters().addAchivementInfo("pvpKillsAtTw", 0, -1, false, false, false);
			}
		}
		return "";
	}

	@Override
	public String onEnterWorld(Player player)
	{
		final int territoryId = TerritoryWarManager.getInstance().getRegisteredTerritoryId(player);
		if (territoryId > 0)
		{
			final TerritoryWarSuperClass territoryQuest = _forTheSakeScripts.get(territoryId);
			QuestState st = player.getQuestState(territoryQuest.getName());
			if (st == null)
			{
				st = territoryQuest.newQuestState(player);
			}
			st.setState(State.STARTED, false);
			st.setCond(1);

			if (player.getLevel() >= 61)
			{
				final TerritoryWarSuperClass killthe = _killTheScripts.get(player.getClassId().getId());
				if (killthe != null)
				{
					st = player.getQuestState(killthe.getName());
					if (st == null)
					{
						st = killthe.newQuestState(player);
					}
					player.addNotifyQuestOfDeath(st);
				}
				else
				{
					_log.warn("TerritoryWar: Missing Kill the quest for player " + player.getName(null) + " whose class id: " + player.getClassId().getId());
				}
			}
		}
		return null;
	}

	@Override
	public void setOnEnterWorld(boolean val)
	{
		super.setOnEnterWorld(val);

		for (final Player player : GameObjectsStorage.getPlayers())
		{
			if (player.getSiegeSide() > 0)
			{
				final TerritoryWarSuperClass territoryQuest = _forTheSakeScripts.get(player.getSiegeSide());
				if (territoryQuest == null)
				{
					continue;
				}

				QuestState st = player.hasQuestState(territoryQuest.getName()) ? player.getQuestState(territoryQuest.getName()) : territoryQuest.newQuestState(player);

				if (val)
				{
					st.setState(State.STARTED, false);
					st.setCond(1);
					if (player.getLevel() >= 61)
					{
						final TerritoryWarSuperClass killthe = _killTheScripts.get(player.getClassId().getId());
						if (killthe != null)
						{
							st = player.getQuestState(killthe.getName());
							if (st == null)
							{
								st = killthe.newQuestState(player);
							}
							player.addNotifyQuestOfDeath(st);
						}
						else
						{
							_log.warn("TerritoryWar: Missing Kill the quest for player " + player.getName(null) + " whose class id: " + player.getClassId().getId());
						}
					}
				}
				else
				{
					st.exitQuest(false);
					for (final Quest q : _protectTheScripts.values())
					{
						st = player.getQuestState(q.getName());
						if (st != null)
						{
							st.exitQuest(false);
						}
					}

					final TerritoryWarSuperClass killthe = _killTheScripts.get(player.getClassIndex());
					if (killthe != null)
					{
						st = player.getQuestState(killthe.getName());
						if (st != null)
						{
							player.removeNotifyQuestOfDeath(st);
						}
					}
				}
			}
		}
	}

	public TerritoryWarSuperClass(int questId, String name, String descr)
	{
		super(questId, name, descr);

		if (questId < 0)
		{
			addSkillSeeId(36590);
		}
	}

	public static void main(String[] args)
	{
		new TerritoryWarSuperClass(-1, qn, "Territory_War");

		final TerritoryWarSuperClass gludio = new _717_FortheSakeoftheTerritoryGludio();
		_forTheSakeScripts.put(gludio.TERRITORY_ID, gludio);
		final TerritoryWarSuperClass dion = new _718_FortheSakeoftheTerritoryDion();
		_forTheSakeScripts.put(dion.TERRITORY_ID, dion);
		final TerritoryWarSuperClass giran = new _719_FortheSakeoftheTerritoryGiran();
		_forTheSakeScripts.put(giran.TERRITORY_ID, giran);
		final TerritoryWarSuperClass oren = new _720_FortheSakeoftheTerritoryOren();
		_forTheSakeScripts.put(oren.TERRITORY_ID, oren);
		final TerritoryWarSuperClass aden = new _721_FortheSakeoftheTerritoryAden();
		_forTheSakeScripts.put(aden.TERRITORY_ID, aden);
		final TerritoryWarSuperClass innadril = new _722_FortheSakeoftheTerritoryInnadril();
		_forTheSakeScripts.put(innadril.TERRITORY_ID, innadril);
		final TerritoryWarSuperClass goddard = new _723_FortheSakeoftheTerritoryGoddard();
		_forTheSakeScripts.put(goddard.TERRITORY_ID, goddard);
		final TerritoryWarSuperClass rune = new _724_FortheSakeoftheTerritoryRune();
		_forTheSakeScripts.put(rune.TERRITORY_ID, rune);
		final TerritoryWarSuperClass schuttgart = new _725_FortheSakeoftheTerritorySchuttgart();
		_forTheSakeScripts.put(schuttgart.TERRITORY_ID, schuttgart);
		final TerritoryWarSuperClass catapult = new _729_Protecttheterritorycatapult();
		_protectTheScripts.put(catapult.getId(), catapult);
		final TerritoryWarSuperClass military = new _731_ProtecttheMilitaryAssociationLeader();
		_protectTheScripts.put(military.getId(), military);
		final TerritoryWarSuperClass religious = new _732_ProtecttheReligiousAssociationLeader();
		_protectTheScripts.put(religious.getId(), religious);
		final TerritoryWarSuperClass supplies = new _730_ProtecttheSuppliesSafe();
		_protectTheScripts.put(supplies.getId(), supplies);
		final TerritoryWarSuperClass knights = new _734_Piercethroughashield();
		for (final int i : knights.CLASS_IDS)
		{
			_killTheScripts.put(i, knights);
		}
		final TerritoryWarSuperClass warriors = new _735_Makespearsdull();
		for (final int i : warriors.CLASS_IDS)
		{
			_killTheScripts.put(i, warriors);
		}
		final TerritoryWarSuperClass wizards = new _736_Weakenmagic();
		for (final int i : wizards.CLASS_IDS)
		{
			_killTheScripts.put(i, wizards);
		}
		final TerritoryWarSuperClass priests = new _737_DenyBlessings();
		for (final int i : priests.CLASS_IDS)
		{
			_killTheScripts.put(i, priests);
		}
		final TerritoryWarSuperClass keys = new _738_DestroyKeyTargets();
		for (final int i : keys.CLASS_IDS)
		{
			_killTheScripts.put(i, keys);
		}
	}
}
