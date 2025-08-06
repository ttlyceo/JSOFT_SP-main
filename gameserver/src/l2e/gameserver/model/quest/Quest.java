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
package l2e.gameserver.model.quest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import l2e.gameserver.listener.AbstractFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.GameTimeController;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.QuestsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.listener.ManagedLoader;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.instance.TrapInstance;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.npc.MinionData;
import l2e.gameserver.model.actor.templates.npc.MinionTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.quest.QuestExperience;
import l2e.gameserver.model.actor.templates.quest.QuestRewardItem;
import l2e.gameserver.model.actor.templates.quest.QuestTemplate;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.interfaces.IL2Procedure;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.olympiad.CompetitionType;
import l2e.gameserver.model.reward.RewardList;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.SpawnTemplate;
import l2e.gameserver.model.spawn.SpawnTerritory;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExNpcQuestHtmlMessage;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SpecialCamera;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Quest extends ManagedLoader implements IIdentifiable
{
	public static final Logger _log = LoggerFactory.getLogger(Quest.class);

	private static Map<String, Quest> _allScripts = new ConcurrentHashMap<>();

	private volatile Map<String, List<QuestTimer>> _questTimers = null;
	private final Set<Integer> _questInvolvedNpcs = new HashSet<>();
	private final Map<QuestEventType, int[]> _questTypes = new ConcurrentHashMap<>();

	private final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock();
	private final WriteLock _writeLock = _rwLock.writeLock();
	private final ReadLock _readLock = _rwLock.readLock();
	
	private final int _questId;
	private final String _name;
	private final String _descr;
	private final byte _initialState = State.CREATED;
	protected boolean _onEnterWorld = false;
	private boolean _isCustom = false;
	private boolean _isOlympiadUse = false;
	private boolean _isUseSwitch = true;
	
	public int[] questItemIds = null;

	private static final String QUEST_DELETE_FROM_CHAR_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=?";
	private static final String QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=? AND var!=?";
	
	private static final int RESET_HOUR = 6;
	private static final int RESET_MINUTES = 30;
	
	public static enum QuestSound
	{
		ITEMSOUND_QUEST_ACCEPT(new PlaySound("ItemSound.quest_accept")), ITEMSOUND_QUEST_MIDDLE(new PlaySound("ItemSound.quest_middle")), ITEMSOUND_QUEST_FINISH(new PlaySound("ItemSound.quest_finish")), ITEMSOUND_QUEST_ITEMGET(new PlaySound("ItemSound.quest_itemget")), ITEMSOUND_QUEST_TUTORIAL(new PlaySound("ItemSound.quest_tutorial")), ITEMSOUND_QUEST_GIVEUP(new PlaySound("ItemSound.quest_giveup")), ITEMSOUND_QUEST_BEFORE_BATTLE(new PlaySound("ItemSound.quest_before_battle")), ITEMSOUND_QUEST_JACKPOT(new PlaySound("ItemSound.quest_jackpot")), ITEMSOUND_QUEST_FANFARE_1(new PlaySound("ItemSound.quest_fanfare_1")), ITEMSOUND_QUEST_FANFARE_2(new PlaySound("ItemSound.quest_fanfare_2")),
		
		ITEMSOUND_QUEST_FANFARE_MIDDLE(new PlaySound("ItemSound.quest_fanfare_middle")), ITEMSOUND_ARMOR_WOOD(new PlaySound("ItemSound.armor_wood_3")), ITEMSOUND_ARMOR_CLOTH(new PlaySound("ItemSound.item_drop_equip_armor_cloth")), AMDSOUND_ED_CHIMES(new PlaySound("AmdSound.ed_chimes_05")), HORROR_01(new PlaySound("horror_01")),
		
		AMBSOUND_HORROR_01(new PlaySound("AmbSound.dd_horror_01")), AMBSOUND_HORROR_03(new PlaySound("AmbSound.d_horror_03")), AMBSOUND_HORROR_15(new PlaySound("AmbSound.d_horror_15")),
		
		ITEMSOUND_ARMOR_LEATHER(new PlaySound("ItemSound.itemdrop_armor_leather")), ITEMSOUND_WEAPON_SPEAR(new PlaySound("ItemSound.itemdrop_weapon_spear")), AMBSOUND_MT_CREAK(new PlaySound("AmbSound.mt_creak01")), AMBSOUND_EG_DRON(new PlaySound("AmbSound.eg_dron_02")), SKILLSOUND_HORROR_02(new PlaySound("SkillSound5.horror_02")), CHRSOUND_MHFIGHTER_CRY(new PlaySound("ChrSound.MHFighter_cry")),
		
		AMDSOUND_WIND_LOOT(new PlaySound("AmdSound.d_wind_loot_02")), INTERFACESOUND_CHARSTAT_OPEN(new PlaySound("InterfaceSound.charstat_open_01")),
		
		AMDSOUND_HORROR_02(new PlaySound("AmdSound.dd_horror_02")), CHRSOUND_FDELF_CRY(new PlaySound("ChrSound.FDElf_Cry")),
		
		AMBSOUND_WINGFLAP(new PlaySound("AmbSound.t_wingflap_04")), AMBSOUND_THUNDER(new PlaySound("AmbSound.thunder_02")),
		
		AMBSOUND_DRONE(new PlaySound("AmbSound.ed_drone_02")), AMBSOUND_CRYSTAL_LOOP(new PlaySound("AmbSound.cd_crystal_loop")), AMBSOUND_PERCUSSION_01(new PlaySound("AmbSound.dt_percussion_01")), AMBSOUND_PERCUSSION_02(new PlaySound("AmbSound.ac_percussion_02")),
		
		ITEMSOUND_BROKEN_KEY(new PlaySound("ItemSound2.broken_key")),
		
		ITEMSOUND_SIREN(new PlaySound("ItemSound3.sys_siren")),
		
		ITEMSOUND_ENCHANT_SUCCESS(new PlaySound("ItemSound3.sys_enchant_success")), ITEMSOUND_ENCHANT_FAILED(new PlaySound("ItemSound3.sys_enchant_failed")),
		
		ITEMSOUND_SOW_SUCCESS(new PlaySound("ItemSound3.sys_sow_success")),
		
		SKILLSOUND_HORROR_1(new PlaySound("SkillSound5.horror_01")),
		
		SKILLSOUND_HORROR_2(new PlaySound("SkillSound5.horror_02")),
		
		SKILLSOUND_ANTARAS_FEAR(new PlaySound("SkillSound3.antaras_fear")),
		
		SKILLSOUND_JEWEL_CELEBRATE(new PlaySound("SkillSound2.jewel.celebrate")),
		
		SKILLSOUND_LIQUID_MIX(new PlaySound("SkillSound5.liquid_mix_01")), SKILLSOUND_LIQUID_SUCCESS(new PlaySound("SkillSound5.liquid_success_01")), SKILLSOUND_LIQUID_FAIL(new PlaySound("SkillSound5.liquid_fail_01")),
		
		ETCSOUND_ELROKI_SONG_FULL(new PlaySound("EtcSound.elcroki_song_full")), ETCSOUND_ELROKI_SONG_1ST(new PlaySound("EtcSound.elcroki_song_1st")), ETCSOUND_ELROKI_SONG_2ND(new PlaySound("EtcSound.elcroki_song_2nd")), ETCSOUND_ELROKI_SONG_3RD(new PlaySound("EtcSound.elcroki_song_3rd")),
		
		BS01_A(new PlaySound("BS01_A")), BS02_A(new PlaySound("BS02_A")), BS03_A(new PlaySound("BS03_A")), BS04_A(new PlaySound("BS04_A")), BS06_A(new PlaySound("BS06_A")), BS07_A(new PlaySound("BS07_A")), BS08_A(new PlaySound("BS08_A")), BS01_D(new PlaySound("BS01_D")), BS02_D(new PlaySound("BS02_D")), BS05_D(new PlaySound("BS05_D")), BS07_D(new PlaySound("BS07_D"));
		
		private final PlaySound _playSound;
		
		private static Map<String, PlaySound> soundPackets = new HashMap<>();
		
		private QuestSound(PlaySound playSound)
		{
			_playSound = playSound;
		}
		
		public static PlaySound getSound(String soundName)
		{
			if (soundPackets.containsKey(soundName))
			{
				return soundPackets.get(soundName);
			}
			
			for (final QuestSound qs : QuestSound.values())
			{
				if (qs._playSound.getSoundName().equals(soundName))
				{
					soundPackets.put(soundName, qs._playSound);
					return qs._playSound;
				}
			}
			
			_log.info("Missing QuestSound enum for sound: " + soundName);
			soundPackets.put(soundName, new PlaySound(soundName));
			return soundPackets.get(soundName);
		}
		
		public String getSoundName()
		{
			return _playSound.getSoundName();
		}
		
		public PlaySound getPacket()
		{
			return _playSound;
		}
	}
	
	public int getResetHour()
	{
		return RESET_HOUR;
	}
	
	public int getResetMinutes()
	{
		return RESET_MINUTES;
	}
	
	public static Collection<Quest> findAllEvents()
	{
		return _allScripts.values();
	}
	
	public Quest(int questId, String name, String descr)
	{
		_questId = questId;
		_name = name;
		_descr = descr;
		if (questId != 0)
		{
			QuestManager.getInstance().addQuest(this);
		}
		else
		{
			_allScripts.put(name, this);
		}
	}
	
	public void saveGlobalData()
	{
	}
	
	public static enum TrapAction
	{
		TRAP_TRIGGERED, TRAP_DETECTED, TRAP_DISARMED
	}
	
	@Override
	public int getId()
	{
		return _questId;
	}
	
	public QuestState newQuestState(Player player)
	{
		return new QuestState(this, player, getInitialState());
	}
	
	public QuestState getQuestState(Player player, boolean initIfNone)
	{
		final QuestState qs = player.getQuestState(_name);
		if ((qs != null) || !initIfNone)
		{
			return qs;
		}
		return newQuestState(player);
	}
	
	public byte getInitialState()
	{
		return _initialState;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getDescr(Player player)
	{
		if (_descr.equals(""))
		{
			final QuestTemplate template = QuestsParser.getInstance().getTemplate(_questId);
			if (template != null)
			{
				return template.getName(player.getLang());
			}
			return new ServerMessage("quest." + _questId, player.getLang()).toString();
		}
		return _descr;
	}
	
	public void startQuestTimer(String name, long time, Npc npc, Player player)
	{
		startQuestTimer(name, time, npc, player, false);
	}
	
	public void startQuestTimer(String name, long time, Npc npc, Player player, boolean repeating)
	{
		final List<QuestTimer> timers = getQuestTimers().computeIfAbsent(name, k -> new ArrayList<>(1));
		if (getQuestTimer(name, npc, player) == null)
		{
			_writeLock.lock();
			try
			{
				timers.add(new QuestTimer(this, name, time, npc, player, repeating));
			}
			finally
			{
				_writeLock.unlock();
			}
		}
	}
	
	public QuestTimer getQuestTimer(String name, Npc npc, Player player)
	{
		if (_questTimers == null)
		{
			return null;
		}
		
		final List<QuestTimer> timers = getQuestTimers().get(name);
		if (timers != null)
		{
			_readLock.lock();
			try
			{
				for (final QuestTimer timer : timers)
				{
					if (timer != null)
					{
						if (timer.isMatch(this, name, npc, player))
						{
							return timer;
						}
					}
				}
			}
			finally
			{
				_readLock.unlock();
			}
		}
		return null;
	}
	
	public void cancelQuestTimers(String name)
	{
		if (_questTimers == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = getQuestTimers().get(name);
		if (timers != null)
		{
			_writeLock.lock();
			try
			{
				for (final QuestTimer timer : timers)
				{
					if (timer != null)
					{
						timer.cancel();
					}
				}
				timers.clear();
			}
			finally
			{
				_writeLock.unlock();
			}
		}
	}
	
	public void cancelQuestTimers(Npc npc)
	{
		if (_questTimers == null)
		{
			return;
		}
		
		for (final List<QuestTimer> timers : getQuestTimers().values())
		{
			if (timers != null)
			{
				_writeLock.lock();
				try
				{
					for (final QuestTimer timer : timers)
					{
						if (timer != null && timer.getNpc() == npc)
						{
							timer.cancel();
						}
					}
					timers.clear();
				}
				finally
				{
					_writeLock.unlock();
				}
			}
		}
	}

	public void cancelQuestTimers(String name, int instanceId)
	{
		if (_questTimers == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = getQuestTimers().get(name);
		if (timers != null)
		{
			_writeLock.lock();
			try
			{
				for (final QuestTimer timer : timers)
				{
					if (timer != null && timer.getReflectionId() == instanceId)
					{
						timer.cancel();
					}
				}
			}
			finally
			{
				_writeLock.unlock();
			}
		}
	}
	
	public void cancelQuestTimer(String name, Npc npc, Player player)
	{
		final QuestTimer timer = getQuestTimer(name, npc, player);
		if (timer != null)
		{
			timer.cancelAndRemove();
		}
	}
	
	public void removeQuestTimer(QuestTimer timer)
	{
		if ((timer != null) && (_questTimers != null))
		{
			final List<QuestTimer> timers = getQuestTimers().get(timer.getName());
			if (timers != null)
			{
				_writeLock.lock();
				try
				{
					timers.remove(timer);
				}
				finally
				{
					_writeLock.unlock();
				}
			}
		}
	}
	
	public final Map<String, List<QuestTimer>> getQuestTimers()
	{
		if (_questTimers == null)
		{
			synchronized (this)
			{
				if (_questTimers == null)
				{
					_questTimers = new ConcurrentHashMap<>(1);
				}
			}
		}
		return _questTimers;
	}
	
	public final void notifyAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		String res = null;
		try
		{
			res = onAttack(npc, attacker, damage, isSummon, skill);
		}
		catch (final Exception e)
		{
			showError(attacker, Util.getStackTrace(e));
			return;
		}
		showResult(attacker, res);
	}
	
	public final void notifyDeath(Creature killer, Creature victim, QuestState qs)
	{
		String res = null;
		try
		{
			res = onDeath(killer, victim, qs);
		}
		catch (final Exception e)
		{
			showError(qs.getPlayer(), Util.getStackTrace(e));
		}
		showResult(qs.getPlayer(), res);
	}
	
	public final void notifyItemUse(Item item, Player player)
	{
		String res = null;
		try
		{
			res = onItemUse(item, player);
		}
		catch (final Exception e)
		{
			showError(player, Util.getStackTrace(e));
		}
		showResult(player, res);
	}
	
	public final void notifySpellFinished(Npc instance, Player player, Skill skill)
	{
		String res = null;
		try
		{
			res = onSpellFinished(instance, player, skill);
		}
		catch (final Exception e)
		{
			showError(player, Util.getStackTrace(e));
		}
		showResult(player, res);
	}
	
	public final void notifyTrapAction(TrapInstance trap, Creature trigger, TrapAction action)
	{
		String res = null;
		try
		{
			res = onTrapAction(trap, trigger, action);
		}
		catch (final Exception e)
		{
			if (trigger.getActingPlayer() != null)
			{
				showError(trigger.getActingPlayer(), Util.getStackTrace(e));
			}
			_log.warn("Exception on onTrapAction() in notifyTrapAction(): " + e.getMessage(), e);
			return;
		}
		if (trigger.getActingPlayer() != null)
		{
			showResult(trigger.getActingPlayer(), res);
		}
	}
	
	public final void notifySpawn(Npc npc)
	{
		try
		{
			onSpawn(npc);
		}
		catch (final Exception e)
		{
			_log.warn("Exception on onSpawn() in notifySpawn(): " + e.getMessage(), e);
		}
	}
	
	public final boolean notifyEvent(String event, Npc npc, Player player)
	{
		String res = null;
		try
		{
			if (event != null && Config.DISABLE_NPC_BYPASSES.contains(event.split(" ")[0]))
			{
				return false;
			}
			res = onAdvEvent(event, npc, player);
		}
		catch (final Exception e)
		{
			return showError(player, Util.getStackTrace(e));
		}
		return showResult(player, res);
	}
	
	public final void notifyEventReceived(String eventName, Npc sender, Npc receiver, GameObject reference)
	{
		try
		{
			onEventReceived(eventName, sender, receiver, reference);
		}
		catch (final Exception e)
		{
			_log.warn("Exception on onEventReceived() in notifyEventReceived(): " + e.getMessage(), e);
		}
	}
	
	public final void notifyEnterWorld(Player player)
	{
		String res = null;
		try
		{
			res = onEnterWorld(player);
		}
		catch (final Exception e)
		{
			showError(player, Util.getStackTrace(e));
		}
		showResult(player, res);
	}
	
	public final void notifyKill(Npc npc, Player killer, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onKill(npc, killer, isSummon);
		}
		catch (final Exception e)
		{
			_log.warn("Error with notifyKill at npcId: " + npc.getId());
			showError(killer, Util.getStackTrace(e));
		}
		showResult(killer, res);
	}
	
	public final boolean notifyKillByMob(Npc npc, Npc killer)
	{
		try
		{
			onKillByMob(npc, killer);
		}
		catch (final Exception e)
		{
			System.out.println(e);
			return false;
		}
		return true;
	}
	
	public final boolean notifyTalk(Npc npc, QuestState qs)
	{
		String res = null;
		try
		{
			res = onTalk(npc, qs.getPlayer());
		}
		catch (final Exception e)
		{
			return showError(qs.getPlayer(), Util.getStackTrace(e));
		}
		qs.getPlayer().setLastQuestNpcObject(npc.getObjectId());
		return showResult(qs.getPlayer(), res);
	}
	
	public final void notifyFirstTalk(Npc npc, Player player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch (final Exception e)
		{
			showError(player, Util.getStackTrace(e));
		}
		showResult(player, res);
	}
	
	public final void notifyAcquireSkillList(Npc npc, Player player)
	{
		String res = null;
		try
		{
			res = onAcquireSkillList(npc, player);
		}
		catch (final Exception e)
		{
			showError(player, Util.getStackTrace(e));
		}
		showResult(player, res);
	}
	
	public final void notifyAcquireSkillInfo(Npc npc, Player player, Skill skill)
	{
		String res = null;
		try
		{
			res = onAcquireSkillInfo(npc, player, skill);
		}
		catch (final Exception e)
		{
			showError(player, Util.getStackTrace(e));
		}
		showResult(player, res);
	}
	
	public final void notifyAcquireSkill(Npc npc, Player player, Skill skill, AcquireSkillType type)
	{
		String res = null;
		try
		{
			res = onAcquireSkill(npc, player, skill, type);
		}
		catch (final Exception e)
		{
			showError(player, Util.getStackTrace(e));
		}
		showResult(player, res);
	}
	
	public final boolean notifyItemTalk(ItemInstance item, Player player)
	{
		String res = null;
		try
		{
			res = onItemTalk(item, player);
			if (res != null)
			{
				if (res.equalsIgnoreCase("true"))
				{
					return true;
				}
				else if (res.equalsIgnoreCase("false"))
				{
					return false;
				}
			}
		}
		catch (final Exception e)
		{
			return showError(player, Util.getStackTrace(e));
		}
		return showResult(player, res);
	}
	
	public String onItemTalk(ItemInstance item, Player player)
	{
		return null;
	}
	
	public final boolean notifyItemEvent(ItemInstance item, Player player, String event)
	{
		String res = null;
		try
		{
			res = onItemEvent(item, player, event);
			if (res != null)
			{
				if (res.equalsIgnoreCase("true"))
				{
					return true;
				}
				else if (res.equalsIgnoreCase("false"))
				{
					return false;
				}
			}
		}
		catch (final Exception e)
		{
			return showError(player, Util.getStackTrace(e));
		}
		return showResult(player, res);
	}
	
	public final void notifySkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onSkillSee(npc, caster, skill, targets, isSummon);
		}
		catch (final Exception e)
		{
			showError(caster, Util.getStackTrace(e));
			return;
		}
		showResult(caster, res);
	}
	
	public final void notifyFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onFactionCall(npc, caller, attacker, isSummon);
		}
		catch (final Exception e)
		{
			showError(attacker, Util.getStackTrace(e));
		}
		showResult(attacker, res);
	}
	
	public final void notifyAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onAggroRangeEnter(npc, player, isSummon);
		}
		catch (final Exception e)
		{
			showError(player, Util.getStackTrace(e));
			return;
		}
		showResult(player, res);
	}
	
	public final void notifySeeCreature(Npc npc, Creature creature, boolean isSummon)
	{
		Player player = null;
		if (isSummon || creature.isPlayer())
		{
			player = creature.getActingPlayer();
		}
		String res = null;
		try
		{
			res = onSeeCreature(npc, creature, isSummon);
		}
		catch (final Exception e)
		{
			if (player != null)
			{
				showError(player, Util.getStackTrace(e));
			}
			return;
		}
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	public final void notifyEnterZone(Creature character, ZoneType zone)
	{
		final Player player = character.getActingPlayer();
		String res = null;
		try
		{
			res = onEnterZone(character, zone);
		}
		catch (final Exception e)
		{
			if (player != null)
			{
				showError(player, Util.getStackTrace(e));
			}
		}
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	public final void notifyExitZone(Creature character, ZoneType zone)
	{
		final Player player = character.getActingPlayer();
		String res = null;
		try
		{
			res = onExitZone(character, zone);
		}
		catch (final Exception e)
		{
			if (player != null)
			{
				showError(player, Util.getStackTrace(e));
			}
		}
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	public final void notifyOlympiadWin(Player winner, CompetitionType type)
	{
		try
		{
			onOlympiadWin(winner, type);
		}
		catch (final Exception e)
		{
			showError(winner, Util.getStackTrace(e));
		}
	}
	
	public final void notifyOlympiadLose(Player loser, CompetitionType type)
	{
		try
		{
			onOlympiadLose(loser, type);
		}
		catch (final Exception e)
		{
			showError(loser, Util.getStackTrace(e));
		}
	}
	
	public boolean notifyMoveFinished(Npc npc)
	{
		try
		{
			return onMoveFinished(npc);
		}
		catch (final Exception e)
		{
			_log.warn("Exception on onMoveFinished() in notifyMoveFinished(): " + e.getMessage(), e);
		}
		return false;
	}
	
	public final void notifyRouteFinished(Npc npc)
	{
		try
		{
			onRouteFinished(npc);
		}
		catch (final Exception e)
		{
			_log.warn("Exception on onRouteFinished() in notifyRouteFinished(): " + e.getMessage(), e);
		}
	}
	
	public final boolean notifyOnCanSeeMe(Npc npc, Player player)
	{
		try
		{
			return onCanSeeMe(npc, player);
		}
		catch (final Exception e)
		{
			_log.warn("Exception on onCanSeeMe() in notifyOnCanSeeMe(): " + e.getMessage(), e);
		}
		return false;
	}
	
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		return null;
	}
	
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		return onAttack(npc, attacker, damage, isSummon);
	}
	
	public String onDeath(Creature killer, Creature victim, QuestState qs)
	{
		return onAdvEvent("", ((killer instanceof Npc) ? ((Npc) killer) : null), qs.getPlayer());
	}
	
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (player != null)
		{
			final QuestState qs = player.getQuestState(getName());
			if (qs != null)
			{
				return onEvent(event, qs);
			}
		}
		return null;
	}
	
	public String onEvent(String event, QuestState qs)
	{
		return null;
	}
	
	public String onKill(Npc npc, QuestState qs)
	{
		return null;
	}
	
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		return null;
	}
	
	public String onKillByMob(Npc npc, Npc killer)
	{
		return null;
	}
	
	public String onTalk(Npc npc, Player talker)
	{
		return null;
	}
	
	public String onFirstTalk(Npc npc, Player player)
	{
		return null;
	}
	
	public String onItemEvent(ItemInstance item, Player player, String event)
	{
		return null;
	}
	
	public String onAcquireSkillList(Npc npc, Player player)
	{
		return null;
	}
	
	public String onAcquireSkillInfo(Npc npc, Player player, Skill skill)
	{
		return null;
	}
	
	public String onAcquireSkill(Npc npc, Player player, Skill skill, AcquireSkillType type)
	{
		return null;
	}
	
	public String onItemUse(Item item, Player player)
	{
		return null;
	}
	
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		return null;
	}
	
	public String onSpellFinished(Npc npc, Player player, Skill skill)
	{
		return null;
	}
	
	public String onTrapAction(TrapInstance trap, Creature trigger, TrapAction action)
	{
		return null;
	}
	
	public String onSpawn(Npc npc)
	{
		return null;
	}
	
	public String onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		return null;
	}
	
	public String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		return null;
	}
	
	public String onSeeCreature(Npc npc, Creature creature, boolean isSummon)
	{
		return null;
	}
	
	public String onEnterWorld(Player player)
	{
		return null;
	}
	
	public String onEnterZone(Creature character, ZoneType zone)
	{
		return null;
	}
	
	public String onExitZone(Creature character, ZoneType zone)
	{
		return null;
	}
	
	public String onEventReceived(String eventName, Npc sender, Npc receiver, GameObject reference)
	{
		return null;
	}
	
	public void onOlympiadWin(Player winner, CompetitionType type)
	{
		
	}
	
	public void onOlympiadLose(Player loser, CompetitionType type)
	{
		
	}
	
	public boolean onCanSeeMe(Npc npc, Player player)
	{
		return false;
	}
	
	public boolean showError(Player player, String error)
	{
		_log.warn(getClass().getSimpleName() + ": " + error);
		if ((player != null) && player.getAccessLevel().isGm())
		{
			final String res = "<html><body><title>Script error</title>" + error + "</body></html>";
			return showResult(player, res);
		}
		return false;
	}
	
	public boolean showResult(Player player, String res)
	{
		if ((res == null) || res.isEmpty() || (player == null))
		{
			return true;
		}
		
		if (res.endsWith(".htm") || res.endsWith(".html"))
		{
			showHtmlFile(player, res);
		}
		else if (res.startsWith("<html>"))
		{
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(player, 0, res);
			npcReply.replace("%playername%", player.getName(null));
			player.sendPacket(npcReply);
			player.sendActionFailed();
		}
		else
		{
			player.sendMessage(res);
		}
		return false;
	}
	
	public static final void playerEnter(Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ?");
			final var invalidQuestDataVar = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ? AND var = ?");
			final var ps1 = con.prepareStatement("SELECT name, value FROM character_quests WHERE charId = ? AND var = ?");
			ps1.setInt(1, player.getObjectId());
			ps1.setString(2, "<state>");
			rset = ps1.executeQuery();
			while (rset.next())
			{
				final String questId = rset.getString("name");
				final String statename = rset.getString("value");
				final Quest q = QuestManager.getInstance().getQuest(questId);
				if (q == null)
				{
					if (Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						statement.setInt(1, player.getObjectId());
						statement.setString(2, questId);
						statement.executeUpdate();
					}
					continue;
				}
				new QuestState(q, player, State.getStateId(statename));
			}
			
			if (statement != null)
			{
				statement.close();
			}
			ps1.close();
			rset.close();
			
			statement = con.prepareStatement("SELECT name, var, value FROM character_quests WHERE charId = ? AND var <> ?");
			statement.setInt(1, player.getObjectId());
			statement.setString(2, "<state>");
			rset = statement.executeQuery();
			while (rset.next())
			{
				final String questId = rset.getString("name");
				final String var = rset.getString("var");
				final String value = rset.getString("value");
				final QuestState qs = player.getQuestState(questId);
				if (qs == null)
				{
					if (Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestDataVar.setInt(1, player.getObjectId());
						invalidQuestDataVar.setString(2, questId);
						invalidQuestDataVar.setString(3, var);
						invalidQuestDataVar.executeUpdate();
					}
					continue;
				}
				qs.setInternal(var, value);
			}
			
			if (invalidQuestDataVar != null)
			{
				invalidQuestDataVar.close();
			}
		}
		catch (final Exception e)
		{
			_log.warn("could not insert char quest:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		for (final String name : _allScripts.keySet())
		{
			player.processQuestEvent(name, "enter");
		}
	}
	
	public final void saveGlobalQuestVar(String var, String value)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO quest_global_data (quest_name,var,value) VALUES (?,?,?)");
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.setString(3, value);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			_log.warn("could not insert global quest variable:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public final String loadGlobalQuestVar(String var)
	{
		String result = "";
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT value FROM quest_global_data WHERE quest_name = ? AND var = ?");
			statement.setString(1, getName());
			statement.setString(2, var);
			rset = statement.executeQuery();
			if (rset.first())
			{
				result = rset.getString(1);
			}
		}
		catch (final Exception e)
		{
			_log.warn("could not load global quest variable:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return result;
	}
	
	public final void deleteGlobalQuestVar(String var)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ? AND var = ?");
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			_log.warn("could not delete global quest variable:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public final void deleteAllGlobalQuestVars()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ?");
			statement.setString(1, getName());
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			_log.warn("could not delete global quest variables:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static void createQuestVarInDb(QuestState qs, String var, String value)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE value=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.setString(5, value);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			_log.warn("could not insert char quest:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static void updateQuestVarInDb(QuestState qs, String var, String value)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE charId=? AND name=? AND var = ?");
			statement.setString(1, value);
			statement.setInt(2, qs.getPlayer().getObjectId());
			statement.setString(3, qs.getQuestName());
			statement.setString(4, var);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			_log.warn("could not update char quest:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static void deleteQuestVarInDb(QuestState qs, String var)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=? AND name=? AND var=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			_log.warn("could not delete char quest:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static void deleteQuestInDb(QuestState qs, boolean repeatable)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(repeatable ? QUEST_DELETE_FROM_CHAR_QUERY : QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY);
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			if (!repeatable)
			{
				statement.setString(3, "<state>");
			}
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			_log.warn("could not delete char quest:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static void createQuestInDb(QuestState qs)
	{
		createQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	public static void updateQuestInDb(QuestState qs)
	{
		updateQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	public static String getNoQuestMsg(Player player)
	{
		return HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/noquest.htm");
	}
	
	public static String getAlreadyCompletedMsg(Player player)
	{
		return HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/alreadycompleted.htm");
	}
	
	public void addEventId(QuestEventType eventType, int... npcIds)
	{
		try
		{
			for (final int npcId : npcIds)
			{
				final NpcTemplate t = NpcsParser.getInstance().getTemplate(npcId);
				if (t != null)
				{
					t.addQuestEvent(eventType, this);
					_questInvolvedNpcs.add(npcId);
				}
			}
			_questTypes.put(eventType, npcIds);
		}
		catch (final Exception e)
		{
			_log.warn("Exception on addEventId(): " + e.getMessage(), e);
		}
	}
	
	public void addStartNpc(int npcId)
	{
		addEventId(QuestEventType.QUEST_START, npcId);
	}
	
	public void addFirstTalkId(int npcId)
	{
		addEventId(QuestEventType.ON_FIRST_TALK, npcId);
	}
	
	public void addTalkId(int npcId)
	{
		addEventId(QuestEventType.ON_TALK, npcId);
	}
	
	public void addKillId(int killId)
	{
		addEventId(QuestEventType.ON_KILL, killId);
	}
	
	public void addAttackId(int npcId)
	{
		addEventId(QuestEventType.ON_ATTACK, npcId);
	}
	
	public void addStartNpc(int... npcIds)
	{
		addEventId(QuestEventType.QUEST_START, npcIds);
	}
	
	public void addFirstTalkId(int... npcIds)
	{
		addEventId(QuestEventType.ON_FIRST_TALK, npcIds);
	}
	
	public void addAcquireSkillId(int... npcIds)
	{
		addEventId(QuestEventType.ON_SKILL_LEARN, npcIds);
	}
	
	public void addAttackId(int... npcIds)
	{
		addEventId(QuestEventType.ON_ATTACK, npcIds);
	}
	
	public void addKillId(int... killIds)
	{
		addEventId(QuestEventType.ON_KILL, killIds);
	}
	
	public void addKillId(Collection<Integer> killIds)
	{
		for (final int killId : killIds)
		{
			addEventId(QuestEventType.ON_KILL, killId);
		}
	}
	
	public void addTalkId(int... npcIds)
	{
		addEventId(QuestEventType.ON_TALK, npcIds);
	}
	
	public void addSpawnId(int... npcIds)
	{
		addEventId(QuestEventType.ON_SPAWN, npcIds);
	}
	
	public void addSkillSeeId(int... npcIds)
	{
		addEventId(QuestEventType.ON_SKILL_SEE, npcIds);
	}
	
	public void addSpellFinishedId(int... npcIds)
	{
		addEventId(QuestEventType.ON_SPELL_FINISHED, npcIds);
	}
	
	public void addTrapActionId(int... npcIds)
	{
		addEventId(QuestEventType.ON_TRAP_ACTION, npcIds);
	}
	
	public void addFactionCallId(int... npcIds)
	{
		addEventId(QuestEventType.ON_FACTION_CALL, npcIds);
	}
	
	public void addAggroRangeEnterId(int... npcIds)
	{
		addEventId(QuestEventType.ON_AGGRO_RANGE_ENTER, npcIds);
	}
	
	public void addSeeCreatureId(int... npcIds)
	{
		addEventId(QuestEventType.ON_SEE_CREATURE, npcIds);
	}
	
	public ZoneType[] addEnterZoneId(int... zoneIds)
	{
		final ZoneType[] value = new ZoneType[zoneIds.length];
		int i = 0;
		for (final int zoneId : zoneIds)
		{
			try
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
				if (zone != null)
				{
					zone.addQuestEvent(QuestEventType.ON_ENTER_ZONE, this);
				}
				value[i++] = zone;
			}
			catch (final Exception e)
			{
				_log.warn("Exception on addEnterZoneId(): " + e.getMessage(), e);
				continue;
			}
		}
		
		return value;
	}
	
	public ZoneType[] addExitZoneId(int... zoneIds)
	{
		final ZoneType[] value = new ZoneType[zoneIds.length];
		int i = 0;
		for (final int zoneId : zoneIds)
		{
			try
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
				if (zone != null)
				{
					zone.addQuestEvent(QuestEventType.ON_EXIT_ZONE, this);
				}
				value[i++] = zone;
			}
			catch (final Exception e)
			{
				_log.warn("Exception on addEnterZoneId(): " + e.getMessage(), e);
				continue;
			}
		}
		
		return value;
	}
	
	public ZoneType addExitZoneId(int zoneId)
	{
		try
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
			if (zone != null)
			{
				zone.addQuestEvent(QuestEventType.ON_EXIT_ZONE, this);
			}
			return zone;
		}
		catch (final Exception e)
		{
			_log.warn("Exception on addExitZoneId(): " + e.getMessage(), e);
			return null;
		}
	}
	
	public void addEventReceivedId(int... npcIds)
	{
		addEventId(QuestEventType.ON_EVENT_RECEIVED, npcIds);
	}
	
	public void addMoveFinishedId(int... npcIds)
	{
		addEventId(QuestEventType.ON_MOVE_FINISHED, npcIds);
	}
	
	public void addRouteFinishedId(int... npcIds)
	{
		addEventId(QuestEventType.ON_ROUTE_FINISHED, npcIds);
	}

	public void addCanSeeMeId(int... npcIds)
	{
		addEventId(QuestEventType.ON_CAN_SEE_ME, npcIds);
	}
	
	public void addCanSeeMeId(Collection<Integer> npcIds)
	{
		for (final int npcId : npcIds)
		{
			addEventId(QuestEventType.ON_CAN_SEE_ME, npcId);
		}
	}
	
	public Player getRandomPartyMember(Player player)
	{
		if (player == null)
		{
			return null;
		}
		final Party party = player.getParty();
		if (party == null || party.getMemberCount() < 2)
		{
			return player;
		}
		return party.getMembers().get(Rnd.get(party.getMembers().size()));
	}
	
	public Player getRandomPartyMember(Player player, int cond)
	{
		return getRandomPartyMember(player, "cond", String.valueOf(cond));
	}
	
	public Player getRandomPartyMember(Player player, String var, String value)
	{
		if (player == null)
		{
			return null;
		}
		
		if (var == null)
		{
			return getRandomPartyMember(player);
		}
		
		QuestState temp = null;
		final Party party = player.getParty();
		
		if (party == null || party.getMemberCount() < 2)
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && temp.isSet(var) && temp.get(var).equalsIgnoreCase(value))
			{
				return player;
			}
			
			return null;
		}
		
		final List<Player> candidates = new ArrayList<>();
		GameObject target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (final Player partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.get(var) != null) && (temp.get(var)).equalsIgnoreCase(value) && partyMember.isInsideRadius(target, 1500, true, false))
			{
				candidates.add(partyMember);
			}
		}
		
		if (candidates.isEmpty())
		{
			return null;
		}
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	public QuestState checkAllQuestCondition(Player player, Npc npc, String var)
	{
		if (player == null)
		{
			return null;
		}
		
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		if (st.get(var) == null)
		{
			return null;
		}
		
		if (npc == null)
		{
			return null;
		}
		
		if (!player.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false))
		{
			return null;
		}
		return st;
	}
	
	public QuestState checkPlayerCondition(Player player, Npc npc, String var, String value)
	{
		if (player == null)
		{
			return null;
		}
		
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		if ((st.get(var) == null) || (!value.equalsIgnoreCase(st.get(var))))
		{
			return null;
		}
		
		if (npc == null)
		{
			return null;
		}
		
		if (!player.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false))
		{
			return null;
		}
		return st;
	}
	
	public List<Player> getPartyMembers(Player player, Npc npc, String var, String value)
	{
		final ArrayList<Player> candidates = new ArrayList<>();
		
		if ((player != null) && (player.isInParty()))
		{
			for (final Player partyMember : player.getParty().getMembers())
			{
				if (partyMember != null)
				{
					if (checkPlayerCondition(partyMember, npc, var, value) != null)
					{
						candidates.add(partyMember);
					}
				}
			}
		}
		else if (checkPlayerCondition(player, npc, var, value) != null)
		{
			candidates.add(player);
		}
		return candidates;
	}
	
	public List<Player> getMembersCond(Player player, Npc npc, String var)
	{
		final ArrayList<Player> candidates = new ArrayList<>();
		
		if ((player != null) && (player.isInParty()))
		{
			for (final Player partyMember : player.getParty().getMembers())
			{
				if (partyMember != null)
				{
					if (checkAllQuestCondition(partyMember, npc, var) != null)
					{
						candidates.add(partyMember);
					}
				}
			}
		}
		else if (checkAllQuestCondition(player, npc, var) != null)
		{
			candidates.add(player);
		}
		return candidates;
	}
	
	public Player getRandomPartyMemberState(Player player, byte state)
	{
		if (player == null)
		{
			return null;
		}
		
		QuestState temp = null;
		final Party party = player.getParty();
		
		if (party == null || party.getMemberCount() < 2)
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state))
			{
				return player;
			}
			return null;
		}
		
		final List<Player> candidates = new ArrayList<>();
		
		GameObject target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (final Player partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state) && partyMember.isInsideRadius(target, 1500, true, false))
			{
				candidates.add(partyMember);
			}
		}
		
		if (candidates.isEmpty())
		{
			return null;
		}
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	public QuestState getRandomPartyMemberState(Player player, int condition, int playerChance, Npc target)
	{
		if ((player == null) || (playerChance < 1))
		{
			return null;
		}
		
		QuestState st = player.getQuestState(getName());
		if (!player.isInParty())
		{
			if (!checkPartyMemberConditions(st, condition, target))
			{
				return null;
			}
			if (!checkDistanceToTarget(player, target))
			{
				return null;
			}
			return st;
		}
		
		final List<QuestState> candidates = new ArrayList<>();
		if (checkPartyMemberConditions(st, condition, target) && (playerChance > 0))
		{
			for (int i = 0; i < playerChance; i++)
			{
				candidates.add(st);
			}
		}
		
		for (final Player member : player.getParty().getMembers())
		{
			if (member == player)
			{
				continue;
			}
			
			st = member.getQuestState(getName());
			if (checkPartyMemberConditions(st, condition, target))
			{
				candidates.add(st);
			}
		}
		
		if (candidates.isEmpty())
		{
			return null;
		}
		
		st = candidates.get(getRandom(candidates.size()));
		if (!checkDistanceToTarget(st.getPlayer(), target))
		{
			return null;
		}
		return st;
	}
	
	private boolean checkPartyMemberConditions(QuestState qs, int condition, Npc npc)
	{
		return ((qs != null) && ((condition == -1) ? qs.isStarted() : qs.isCond(condition)) && checkPartyMember(qs, npc));
	}
	
	private static boolean checkDistanceToTarget(Player player, Npc target)
	{
		return ((target == null) || Util.checkIfInRange(1500, player, target, true));
	}
	
	public boolean checkPartyMember(QuestState qs, Npc npc)
	{
		return true;
	}
	
	public static void showOnScreenMsg(Player player, String text, int time)
	{
		player.sendPacket(new ExShowScreenMessage(text, time));
	}
	
	public static void showOnScreenMsg(Player player, NpcStringId npcString, int position, int time, String... params)
	{
		player.sendPacket(new ExShowScreenMessage(npcString, position, time, params));
	}
	
	public static void showOnScreenMsg(Player player, SystemMessageId systemMsg, int position, int time, String... params)
	{
		player.sendPacket(new ExShowScreenMessage(systemMsg, position, time, params));
	}
	
	public String showHtmlFile(Player player, String fileName)
	{
		final int questId = getId();
		final String directory = getDescr(player).toLowerCase();
		final String scriptpath = "data/html/scripts/" + directory + "/" + getName() + "/" + fileName;
		final String questpath = "data/html/scripts/quests/" + getName() + "/" + fileName;
		String content = HtmCache.getInstance().getHtm(player, player.getLang(), questpath);
		if (content == null)
		{
			content = HtmCache.getInstance().getHtm(player, player.getLang(), scriptpath);
		}
		
		if (content == null)
		{
			content = "<html><body>My text is missing:<br>" + (questId > 0 ? questpath : scriptpath) + "</body></html>";
			_log.info("Cache[HTML]: Missing HTML page: " + (questId > 0 ? questpath : scriptpath));
		}
		
		if (player.getTarget() != null)
		{
			content = content.replaceAll("%objectId%", String.valueOf(player.getTarget().getObjectId()));
		}
		
		if (content != null)
		{
			final boolean isSwitch = _isUseSwitch;
			if (questId > 0)
			{
				final ExNpcQuestHtmlMessage npcReply = new ExNpcQuestHtmlMessage(5, questId);
				npcReply.setHtml(player, content, isSwitch);
				npcReply.replace("%playername%", player.getName(null));
				player.sendPacket(npcReply);
			}
			else
			{
				final NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
				npcReply.setHtml(player, content, isSwitch);
				npcReply.replace("%playername%", player.getName(null));
				player.sendPacket(npcReply);
			}
			_isUseSwitch = true;
			player.sendActionFailed();
		}
		return content;
	}
	
	public void setUseSwitch(boolean isUseSwitch)
	{
		_isUseSwitch = isUseSwitch;
	}
	
	public String getHtm(Player player, String lang, String fileName)
	{
		String filepath = "data/html/scripts/" + getDescr(player).toLowerCase() + "/" + getName() + "/" + fileName;
		String content = HtmCache.getInstance().getHtm(player, player.getLang(), filepath);
		if (content == null)
		{
			filepath = "data/html/scripts/quests/" + getName() + "/" + fileName;
			content = HtmCache.getInstance().getHtm(player, player.getLang(), filepath);
		}
		
		if (content == null)
		{
			content = "<html><body>My text is missing:<br>" + filepath + "</body></html>";
			_log.info("Cache[HTML]: Missing HTML page: " + filepath);
		}
		return content;
	}

	public static Npc addSpawn(int npcId, Location loc)
	{
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), false, 0, false, ReflectionManager.DEFAULT);
	}

	public static Npc addSpawn(int npcId, Location loc, Reflection r)
	{
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), false, 0, false, r);
	}
	
	public static Npc addSpawn(int npcId, Location loc, Reflection r, int randomOffset)
	{
		Location newLoc;
		if (randomOffset > 0)
		{
			newLoc = Location.findPointToStay(loc, 0, randomOffset, true).setH(loc.getHeading());
		}
		else
		{
			newLoc = loc;
		}
		return addSpawn(npcId, newLoc, r);
	}
	
	public static Npc addSpawn(int npcId, Creature cha)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0, false, ReflectionManager.DEFAULT);
	}
	
	public static Npc addSpawn(int npcId, Creature cha, boolean isSummonSpawn)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0, isSummonSpawn, ReflectionManager.DEFAULT);
	}
	
	public static Npc addSpawn(int npcId, Creature cha, long despawnDelay)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, despawnDelay, false, ReflectionManager.DEFAULT);
	}
	
	public static Npc addSpawn(int npcId, Location loc, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, Reflection r)
	{
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, despawnDelay, isSummonSpawn, r);
	}
	
	public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffSet, long despawnDelay)
	{
		return addSpawn(npcId, x, y, z, heading, randomOffSet, despawnDelay, false, ReflectionManager.DEFAULT);
	}
	
	public static Npc addSpawn(int npcId, Location loc, boolean randomOffSet, long despawnDelay)
	{
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffSet, despawnDelay, false, ReflectionManager.DEFAULT);
	}
	
	public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, isSummonSpawn, ReflectionManager.DEFAULT);
	}
	
	public static Npc addSpawn(int npcId, Location loc, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, despawnDelay, isSummonSpawn, ReflectionManager.DEFAULT);
	}
	
	public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, Reflection r)
	{
		Npc result = null;
		try
		{
			final NpcTemplate template = NpcsParser.getInstance().getTemplate(npcId);
			if (template != null)
			{
				if ((x == 0) && (y == 0))
				{
					_log.warn("Failed to adjust bad locks for quest spawn!  Spawn aborted!");
					return null;
				}
				if (randomOffset)
				{
					int offset;
					
					offset = Rnd.get(2);
					if (offset == 0)
					{
						offset = -1;
					}
					offset *= Rnd.get(50, 100);
					x += offset;
					
					offset = Rnd.get(2);
					if (offset == 0)
					{
						offset = -1;
					}
					offset *= Rnd.get(50, 100);
					y += offset;
				}
				final Spawner spawn = new Spawner(template);
				spawn.setReflection(r);
				spawn.setHeading(heading);
				spawn.setX(x);
				spawn.setY(y);
				if (template.getType().startsWith("Fly") || template.isType("Npc"))
				{
					spawn.setZ(z);
				}
				else
				{
					spawn.setZ(GeoEngine.getInstance().getSpawnHeight(x, y, z));
				}
				spawn.stopRespawn();
				result = spawn.spawnOne(isSummonSpawn);
				
				if (despawnDelay > 0)
				{
					result.scheduleDespawn(despawnDelay);
				}
				
				return result;
			}
		}
		catch (final Exception e1)
		{
			_log.warn("Could not spawn Npc " + npcId + " Error: " + e1.getMessage());
		}
		
		return null;
	}
	
	public static Npc addSpawn(int npcId, SpawnTerritory ter, long despawnDelay, boolean isSummonSpawn, Reflection r)
	{
		Npc result = null;
		try
		{
			final NpcTemplate template = NpcsParser.getInstance().getTemplate(npcId);
			if (template != null)
			{
				final SpawnTemplate tpl = new SpawnTemplate("none", 1, 0, 0);
				tpl.addSpawnRange(ter);
				
				final Spawner c = new Spawner(template);
				c.setAmount(1);
				c.setSpawnTemplate(tpl);
				c.setLocation(c.calcSpawnRangeLoc(template));
				c.setReflection(r);
				c.stopRespawn();
				result = c.spawnOne(isSummonSpawn);
				if (despawnDelay > 0)
				{
					result.scheduleDespawn(despawnDelay);
				}
				return result;
			}
		}
		catch (final Exception e1)
		{
			_log.warn("Could not spawn Npc " + npcId + " Error: " + e1.getMessage());
		}
		return null;
	}
	
	public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, Reflection r, int triggerId)
	{
		Npc result = null;
		try
		{
			final var template = NpcsParser.getInstance().getTemplate(npcId);
			if (template != null)
			{
				if ((x == 0) && (y == 0))
				{
					_log.warn("Failed to adjust bad locks for quest spawn!  Spawn aborted!");
					return null;
				}
				if (randomOffset)
				{
					int offset;
					
					offset = Rnd.get(2);
					if (offset == 0)
					{
						offset = -1;
					}
					offset *= Rnd.get(50, 100);
					x += offset;
					
					offset = Rnd.get(2);
					if (offset == 0)
					{
						offset = -1;
					}
					offset *= Rnd.get(50, 100);
					y += offset;
				}
				final Spawner spawn = new Spawner(template);
				spawn.setReflection(r);
				spawn.setHeading(heading);
				spawn.setX(x);
				spawn.setY(y);
				if (template.getType().startsWith("Fly") || template.isType("Npc"))
				{
					spawn.setZ(z);
				}
				else
				{
					spawn.setZ(GeoEngine.getInstance().getSpawnHeight(x, y, z));
				}
				spawn.stopRespawn();
				result = spawn.spawnOne(isSummonSpawn, triggerId);
				
				if (despawnDelay > 0)
				{
					result.scheduleDespawn(despawnDelay);
				}
				
				return result;
			}
		}
		catch (final Exception e1)
		{
			_log.warn("Could not spawn Npc " + npcId + " Error: " + e1.getMessage());
		}
		
		return null;
	}
	
	public TrapInstance addTrap(int trapId, int x, int y, int z, int heading, Skill skill, Reflection r)
	{
		final NpcTemplate npcTemplate = NpcsParser.getInstance().getTemplate(trapId);
		final TrapInstance trap = new TrapInstance(IdFactory.getInstance().getNextId(), npcTemplate, r, -1);
		trap.setCurrentHp(trap.getMaxHp());
		trap.setCurrentMp(trap.getMaxMp());
		trap.setIsInvul(true);
		trap.setHeading(heading);
		trap.spawnMe(x, y, z);
		
		return trap;
	}
	
	public void addMinion(MonsterInstance master, int minionId)
	{
		master.getMinionList().addMinion(new MinionData(new MinionTemplate(minionId, 1)), true);
	}
	
	public int[] getRegisteredItemIds()
	{
		return questItemIds;
	}
	
	public void registerQuestItems(int... items)
	{
		questItemIds = items;
	}
	
	@Override
	public String getScriptName()
	{
		return getName();
	}
	
	@Override
	public void setActive(boolean status)
	{
	}
	
	@Override
	public boolean reload()
	{
		unload();
		return super.reload();
	}
	
	@Override
	public boolean unload()
	{
		return unload(true);
	}
	
	protected void cancelAllQuestTimers()
	{
		if (_questTimers == null)
		{
			return;
		}
		
		for (final List<QuestTimer> timers : getQuestTimers().values())
		{
			_readLock.lock();
			try
			{
				for (final QuestTimer timer : timers)
				{
					timer.cancel();
				}
			}
			finally
			{
				_readLock.unlock();
			}
			timers.clear();
		}
		_questTimers.clear();
	}
	
	public boolean unload(boolean removeFromList)
	{
		saveGlobalData();
		if (_questTimers != null)
		{
			for (final List<QuestTimer> timers : getQuestTimers().values())
			{
				_readLock.lock();
				try
				{
					for (final QuestTimer timer : timers)
					{
						timer.cancel();
					}
				}
				finally
				{
					_readLock.unlock();
				}
				timers.clear();
			}
			getQuestTimers().clear();
		}
		
		for (final Integer npcId : _questInvolvedNpcs)
		{
			final NpcTemplate template = NpcsParser.getInstance().getTemplate(npcId.intValue());
			if (template != null)
			{
				template.removeQuest(this);
			}
		}
		_questInvolvedNpcs.clear();
		_questTypes.clear();
		
		if (removeFromList)
		{
			return QuestManager.getInstance().removeQuest(this);
		}
		return true;
	}
	
	public Set<Integer> getQuestInvolvedNpcs()
	{
		return _questInvolvedNpcs;
	}
	
	public int[] getNpcsType(QuestEventType type)
	{
		return _questTypes.get(type);
	}
	
	public void setOnEnterWorld(boolean val)
	{
		_onEnterWorld = val;
	}
	
	public boolean getOnEnterWorld()
	{
		return _onEnterWorld;
	}
	
	public void setIsCustom(boolean val)
	{
		_isCustom = val;
	}
	
	public boolean isCustomQuest()
	{
		return _isCustom;
	}
	
	public void setOlympiadUse(boolean val)
	{
		_isOlympiadUse = val;
	}
	
	public boolean isOlympiadUse()
	{
		return _isOlympiadUse;
	}
	
	public static long getQuestItemsCount(Player player, int itemId)
	{
		return player.getInventory().getInventoryItemCount(itemId, -1);
	}
	
	public long getQuestItemsCount(Player player, int... itemIds)
	{
		long count = 0;
		for (final ItemInstance item : player.getInventory().getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			for (final int itemId : itemIds)
			{
				if (item.getId() == itemId)
				{
					if ((count + item.getCount()) > Long.MAX_VALUE)
					{
						return Long.MAX_VALUE;
					}
					count += item.getCount();
				}
			}
		}
		return count;
	}
	
	public static boolean hasQuestItems(Player player, int itemId)
	{
		return player.getInventory().getItemByItemId(itemId) != null;
	}
	
	public static boolean hasQuestItems(Player player, int... itemIds)
	{
		final PcInventory inv = player.getInventory();
		for (final int itemId : itemIds)
		{
			if (inv.getItemByItemId(itemId) == null)
			{
				return false;
			}
		}
		return true;
	}
	
	public static int getEnchantLevel(Player player, int itemId)
	{
		final ItemInstance enchantedItem = player.getInventory().getItemByItemId(itemId);
		if (enchantedItem == null)
		{
			return 0;
		}
		return enchantedItem.getEnchantLevel();
	}
	
	public void giveAdena(Player player, long count, boolean applyRates)
	{
		if (applyRates)
		{
			rewardItems(player, PcInventory.ADENA_ID, count);
		}
		else
		{
			giveItems(player, PcInventory.ADENA_ID, count);
		}
	}
	
	public static void rewardItems(Player player, ItemHolder holder)
	{
		rewardItems(player, holder.getId(), holder.getCount());
	}
	
	public static void rewardItems(Player player, int itemId, long count)
	{
		if (count <= 0)
		{
			return;
		}
		
		final ItemInstance _tmpItem = ItemsParser.getInstance().createDummyItem(itemId);
		if (_tmpItem == null)
		{
			return;
		}
		
		try
		{
			if (itemId == PcInventory.ADENA_ID)
			{
				count *= Config.RATE_QUEST_REWARD_ADENA;
			}
			else if (Config.RATE_QUEST_REWARD_USE_MULTIPLIERS)
			{
				if (_tmpItem.isEtcItem())
				{
					switch (_tmpItem.getEtcItem().getItemType())
					{
						case POTION :
							count *= Config.RATE_QUEST_REWARD_POTION;
							break;
						case SCRL_ENCHANT_WP :
						case SCRL_ENCHANT_AM :
						case SCROLL :
							count *= Config.RATE_QUEST_REWARD_SCROLL;
							break;
						case RECIPE :
							count *= Config.RATE_QUEST_REWARD_RECIPE;
							break;
						case MATERIAL :
							count *= Config.RATE_QUEST_REWARD_MATERIAL;
							break;
						default :
							count *= Config.RATE_QUEST_REWARD;
					}
				}
			}
			else
			{
				count *= Config.RATE_QUEST_REWARD;
			}
		}
		catch (final Exception e)
		{
			count = Long.MAX_VALUE;
		}
		
		final ItemInstance item = player.getInventory().addItem("Quest", itemId, count, player, player.getTarget());
		if (item == null)
		{
			return;
		}
		
		sendItemGetMessage(player, item, count);
	}
	
	private static void sendItemGetMessage(Player player, ItemInstance item, long count)
	{
		if (item.getId() == PcInventory.ADENA_ID)
		{
			final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA);
			smsg.addItemNumber(count);
			player.sendPacket(smsg);
		}
		else
		{
			if (count > 1)
			{
				final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
				smsg.addItemName(item);
				smsg.addItemNumber(count);
				player.sendPacket(smsg);
			}
			else
			{
				final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
				smsg.addItemName(item);
				player.sendPacket(smsg);
			}
			player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
		}
	}
	
	public static void giveItems(Player player, int itemId, long count)
	{
		giveItems(player, itemId, count, 0);
	}
	
	protected static void giveItems(Player player, ItemHolder holder)
	{
		giveItems(player, holder.getId(), holder.getCount());
	}
	
	public static void giveItems(Player player, int itemId, long count, int enchantlevel)
	{
		if (count <= 0)
		{
			return;
		}
		
		final ItemInstance item = player.getInventory().addItem("Quest", itemId, count, player, player.getTarget());
		if (item == null)
		{
			return;
		}
		
		if ((enchantlevel > 0) && (itemId != PcInventory.ADENA_ID))
		{
			item.setEnchantLevel(enchantlevel);
		}
		
		sendItemGetMessage(player, item, count);
	}
	
	public static void giveItems(Player player, int itemId, long count, byte attributeId, int attributeLevel)
	{
		if (count <= 0)
		{
			return;
		}
		
		final ItemInstance item = player.getInventory().addItem("Quest", itemId, count, player, player.getTarget());
		
		if (item == null)
		{
			return;
		}
		
		if ((attributeId >= 0) && (attributeLevel > 0))
		{
			item.setElementAttr(attributeId, attributeLevel);
			if (item.isEquipped())
			{
				item.updateElementAttrBonus(player);
			}
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(item);
			player.sendPacket(iu);
		}
		sendItemGetMessage(player, item, count);
	}
	
	public boolean dropQuestItems(Player player, int itemId, int count, long neededCount, int dropChance, boolean sound)
	{
		return dropQuestItems(player, itemId, count, count, neededCount, dropChance, sound);
	}
	
	public static boolean dropQuestItems(Player player, int itemId, int minCount, int maxCount, long neededCount, int dropChance, boolean sound)
	{
		dropChance *= Config.RATE_QUEST_DROP / ((player.getParty() != null) ? player.getParty().getMemberCount() : 1);
		final long currentCount = getQuestItemsCount(player, itemId);
		
		if ((neededCount > 0) && (currentCount >= neededCount))
		{
			return true;
		}
		
		if (currentCount >= neededCount)
		{
			return true;
		}
		
		long itemCount = 0;
		final int random = Rnd.get(RewardList.MAX_CHANCE);
		
		while (random < dropChance)
		{
			if (minCount < maxCount)
			{
				itemCount += Rnd.get(minCount, maxCount);
			}
			else if (minCount == maxCount)
			{
				itemCount += minCount;
			}
			else
			{
				itemCount++;
			}
			dropChance -= RewardList.MAX_CHANCE;
		}
		
		if (itemCount > 0)
		{
			if ((neededCount > 0) && ((currentCount + itemCount) > neededCount))
			{
				itemCount = neededCount - currentCount;
			}
			
			if (!player.getInventory().validateCapacityByItemId(itemId))
			{
				return false;
			}
			
			player.addItem("Quest", itemId, itemCount, player.getTarget(), true);
			
			if (sound)
			{
				playSound(player, ((currentCount + itemCount) < neededCount) ? QuestSound.ITEMSOUND_QUEST_ITEMGET : QuestSound.ITEMSOUND_QUEST_MIDDLE);
			}
		}
		return ((neededCount > 0) && ((currentCount + itemCount) >= neededCount));
	}
	
	public static boolean takeItems(Player player, int itemId, long amount)
	{
		final ItemInstance item = player.getInventory().getItemByItemId(itemId);
		if (item == null)
		{
			return false;
		}
		
		if ((amount < 0) || (amount > item.getCount()))
		{
			amount = item.getCount();
		}
		
		if (item.isEquipped())
		{
			final ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
			final InventoryUpdate iu = new InventoryUpdate();
			for (final ItemInstance itm : unequiped)
			{
				iu.addModifiedItem(itm);
			}
			player.sendPacket(iu);
			player.broadcastCharInfo();
		}
		return player.destroyItemByItemId("Quest", itemId, amount, player, true);
	}
	
	public static long takeAllItems(Player player, int itemId, long amount)
	{
		final ItemInstance item = player.getInventory().getItemByItemId(itemId);
		if (item == null)
		{
			return 0;
		}
		
		if ((amount < 0) || (amount > item.getCount()))
		{
			amount = item.getCount();
		}
		
		if (item.isEquipped())
		{
			final ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
			final InventoryUpdate iu = new InventoryUpdate();
			for (final ItemInstance itm : unequiped)
			{
				iu.addModifiedItem(itm);
			}
			player.sendPacket(iu);
			player.broadcastCharInfo();
		}
		player.destroyItemByItemId("Quest", itemId, amount, player, true);
		
		return amount;
	}
	
	protected static boolean takeItems(Player player, ItemHolder holder)
	{
		return takeItems(player, holder.getId(), holder.getCount());
	}
	
	public static boolean takeItems(Player player, int amount, int... itemIds)
	{
		boolean check = true;
		if (itemIds != null)
		{
			for (final int item : itemIds)
			{
				check &= takeItems(player, item, amount);
			}
		}
		return check;
	}
	
	public void removeRegisteredQuestItems(Player player)
	{
		takeItems(player, -1, questItemIds);
	}
	
	public static void playSound(Player player, String sound)
	{
		player.sendPacket(QuestSound.getSound(sound));
	}
	
	public static void playSound(Player player, QuestSound sound)
	{
		player.sendPacket(sound.getPacket());
	}
	
	public static void addExpAndSp(Player player, long exp, int sp)
	{
		player.addExpAndSp((long) player.calcStat(Stats.EXPSP_RATE, exp * Config.RATE_QUEST_REWARD_XP, null, null), (int) player.calcStat(Stats.EXPSP_RATE, sp * Config.RATE_QUEST_REWARD_SP, null, null));
	}
	
	public static int getRandom(int max)
	{
		return Rnd.get(max);
	}
	
	public static int getRandom(int min, int max)
	{
		return Rnd.get(min, max);
	}
	
	public static boolean getRandomBoolean()
	{
		return Rnd.nextBoolean();
	}
	
	public static int getItemEquipped(Player player, int slot)
	{
		return player.getInventory().getPaperdollItemId(slot);
	}
	
	public static int getGameTicks()
	{
		return GameTimeController.getInstance().getGameTicks();
	}
	
	public final void executeForEachPlayer(Player player, final Npc npc, final boolean isSummon, boolean includeParty, boolean includeCommandChannel)
	{
		if ((includeParty || includeCommandChannel) && player.isInParty())
		{
			if (includeCommandChannel && player.getParty().isInCommandChannel())
			{
				player.getParty().getCommandChannel().forEachMember(new IL2Procedure<Player>()
				{
					@Override
					public boolean execute(Player member)
					{
						actionForEachPlayer(member, npc, isSummon);
						return true;
					}
				});
			}
			else if (includeParty)
			{
				player.getParty().forEachMember(new IL2Procedure<Player>()
				{
					@Override
					public boolean execute(Player member)
					{
						actionForEachPlayer(member, npc, isSummon);
						return true;
					}
				});
			}
		}
		else
		{
			actionForEachPlayer(player, npc, isSummon);
		}
	}
	
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
	}
	
	public void teleportPlayer(Player player, Location loc, Reflection r)
	{
		teleportPlayer(player, loc, r, true);
	}
	
	public void teleportPlayer(Player player, Location loc, Reflection r, boolean allowRandomOffset)
	{
		player.teleToLocation(loc, allowRandomOffset, r);
	}
	
	protected static boolean isIntInArray(int i, int[] ia)
	{
		for (final int v : ia)
		{
			if (i == v)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isDigit(String digit)
	{
		try
		{
			Integer.parseInt(digit);
			return true;
		}
		catch (final Exception e)
		{
			return false;
		}
	}
	
	public void addMoveFinishedId(int npcId)
	{
		addEventId(QuestEventType.ON_MOVE_FINISHED, npcId);
	}
	
	public void addNodeArrivedId(int... npcIds)
	{
		addEventId(QuestEventType.ON_NODE_ARRIVED, npcIds);
	}
	
	public final void notifyNodeArrived(Npc npc)
	{
		try
		{
			onNodeArrived(npc);
		}
		catch (final Exception e)
		{
			_log.warn("Exception on onNodeArrived() in notifyNodeArrived(): " + e.getMessage(), e);
		}
	}
	
	public void onNodeArrived(Npc npc)
	{
	}
	
	public boolean onMoveFinished(Npc npc)
	{
		return false;
	}
	
	public void onRouteFinished(Npc npc)
	{
	}
	
	public Npc spawnNpc(int npcId, int x, int y, int z, int heading, Reflection r)
	{
		final NpcTemplate npcTemplate = NpcsParser.getInstance().getTemplate(npcId);
		try
		{
			final Spawner npcSpawn = new Spawner(npcTemplate);
			npcSpawn.setX(x);
			npcSpawn.setY(y);
			npcSpawn.setZ(z);
			npcSpawn.setHeading(heading);
			npcSpawn.setAmount(1);
			npcSpawn.setReflection(r);
			SpawnParser.getInstance().addNewSpawn(npcSpawn);
			final Npc npc = npcSpawn.spawnOne(false);
			if (!r.isDefault())
			{
				r.addNpc(npc);
			}
			return npc;
		}
		catch (final Exception ignored)
		{}
		return null;
	}
	
	public Npc spawnNpc(int npcId, Location loc, int heading, Reflection r)
	{
		return spawnNpc(npcId, loc.getX(), loc.getY(), loc.getZ(), heading, r);
	}
	
	public Npc spawnNpc(int npcId, Location loc, int heading)
	{
		return spawnNpc(npcId, loc.getX(), loc.getY(), loc.getZ(), heading, ReflectionManager.DEFAULT);
	}
	
	public boolean hasAtLeastOneQuestItem(Player player, int... itemIds)
	{
		final PcInventory inv = player.getInventory();
		for (final int itemId : itemIds)
		{
			if (inv.getItemByItemId(itemId) != null)
			{
				return true;
			}
		}
		return false;
	}
	
	public static final void specialCamera(Player player, Creature creature, int force, int angle1, int angle2, int time, int range, int duration, int relYaw, int relPitch, int isWide, int relAngle)
	{
		player.sendPacket(new SpecialCamera(creature, force, angle1, angle2, time, range, duration, relYaw, relPitch, isWide, relAngle));
	}
	
	public static final void specialCameraEx(Player player, Creature creature, int force, int angle1, int angle2, int time, int duration, int relYaw, int relPitch, int isWide, int relAngle)
	{
		player.sendPacket(new SpecialCamera(creature, player, force, angle1, angle2, time, duration, relYaw, relPitch, isWide, relAngle));
	}
	
	public static final void specialCamera3(Player player, Creature creature, int force, int angle1, int angle2, int time, int range, int duration, int relYaw, int relPitch, int isWide, int relAngle, int unk)
	{
		player.sendPacket(new SpecialCamera(creature, force, angle1, angle2, time, range, duration, relYaw, relPitch, isWide, relAngle, unk));
	}
	
	public static boolean giveItemRandomly(Player player, int itemId, long amountToGive, long limit, double dropChance, boolean playSound)
	{
		return giveItemRandomly(player, null, itemId, amountToGive, amountToGive, limit, dropChance, playSound);
	}
	
	public static boolean giveItemRandomly(Player player, Npc npc, int itemId, long amountToGive, long limit, double dropChance, boolean playSound)
	{
		return giveItemRandomly(player, npc, itemId, amountToGive, amountToGive, limit, dropChance, playSound);
	}
	
	public static boolean giveItemRandomly(Player player, Npc npc, int itemId, long minAmount, long maxAmount, long limit, double dropChance, boolean playSound)
	{
		final long currentCount = getQuestItemsCount(player, itemId);
		
		if ((limit > 0) && (currentCount >= limit))
		{
			return true;
		}
		
		minAmount *= Config.RATE_QUEST_DROP;
		maxAmount *= Config.RATE_QUEST_DROP;
		dropChance *= Config.RATE_QUEST_DROP;
		
		long amountToGive = ((minAmount == maxAmount) ? minAmount : Rnd.get(minAmount, maxAmount));
		final double random = Rnd.nextDouble();
		if ((dropChance >= random) && (amountToGive > 0) && player.getInventory().validateCapacityByItemId(itemId))
		{
			if ((limit > 0) && ((currentCount + amountToGive) > limit))
			{
				amountToGive = limit - currentCount;
			}
			
			final ItemInstance item = player.addItem("Quest", itemId, amountToGive, npc, true);
			if (item != null)
			{
				if ((currentCount + amountToGive) == limit)
				{
					if (playSound)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					return true;
				}
				
				if (playSound)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				if (limit <= 0)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	// New support start
	public static void calcExpAndSp(Player player, int questId)
	{
		final QuestTemplate template = QuestsParser.getInstance().getTemplate(questId);
		if (template != null)
		{
			final QuestExperience reward = template.getExperienceRewards();
			if (reward != null)
			{
				double exp = reward.getExp() * player.getStat().getRExp();
				double sp = reward.getSp() * player.getStat().getRSp();
				exp *= reward.isExpRateable() ? Config.RATE_QUEST_REWARD_XP : reward.getExpRate();
				sp *= reward.isExpRateable() ? Config.RATE_QUEST_REWARD_SP : reward.getSpRate();
				player.addExpAndSp((long) player.calcStat(Stats.EXPSP_RATE, exp, null, null), (int) player.calcStat(Stats.EXPSP_RATE, sp, null, null));
			}
		}
	}
	
	public static void calcReward(Player player, int questId)
	{
		final QuestTemplate template = QuestsParser.getInstance().getTemplate(questId);
		if (template != null)
		{
			final List<QuestRewardItem> rewards = template.getRewards();
			if (rewards != null && !rewards.isEmpty())
			{
				for (final QuestRewardItem reward : rewards)
				{
					if (reward != null)
					{
						long amount = reward.getMaxCount() != 0 ? Rnd.get(reward.getMinCount(), reward.getMaxCount()) : reward.getMinCount();
						amount *= reward.isRateable() ? (Config.RATE_QUEST_REWARD * (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getQuestRewardRate() : player.getPremiumBonus().getQuestRewardRate())) : reward.getRate();
						final ItemInstance item = player.getInventory().addItem("Quest", reward.getId(), amount, player, player.getTarget());
						if (item == null)
						{
							return;
						}
						sendItemGetMessage(player, item, amount);
					}
				}
			}
		}
	}
	
	public static void calcReward(Player player, int questId, int variant)
	{
		final QuestTemplate template = QuestsParser.getInstance().getTemplate(questId);
		if (template != null)
		{
			final List<QuestRewardItem> rewards = template.getVariantRewards().get(variant);
			if (rewards != null && !rewards.isEmpty())
			{
				for (final QuestRewardItem reward : rewards)
				{
					if (reward != null)
					{
						long amount = reward.getMaxCount() != 0 ? Rnd.get(reward.getMinCount(), reward.getMaxCount()) : reward.getMinCount();
						amount *= reward.isRateable() ? (Config.RATE_QUEST_REWARD * (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getQuestRewardRate() : player.getPremiumBonus().getQuestRewardRate())) : reward.getRate();
						final ItemInstance item = player.getInventory().addItem("Quest", reward.getId(), amount, player, player.getTarget());
						if (item == null)
						{
							return;
						}
						sendItemGetMessage(player, item, amount);
					}
				}
			}
		}
	}
	
	public static void calcRewardPerItem(Player player, int questId, int variant, int totalAmount)
	{
		final QuestTemplate template = QuestsParser.getInstance().getTemplate(questId);
		if (template != null)
		{
			final List<QuestRewardItem> rewards = template.getVariantRewards().get(variant);
			if (rewards != null && !rewards.isEmpty())
			{
				for (final QuestRewardItem reward : rewards)
				{
					if (reward != null)
					{
						long amount = reward.getMaxCount() != 0 ? Rnd.get(reward.getMinCount(), reward.getMaxCount()) : reward.getMinCount();
						amount *= reward.isRateable() ? (Config.RATE_QUEST_REWARD * (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getQuestRewardRate() : player.getPremiumBonus().getQuestRewardRate())) : reward.getRate();
						amount *= totalAmount;
						final ItemInstance item = player.getInventory().addItem("Quest", reward.getId(), amount, player, player.getTarget());
						if (item == null)
						{
							return;
						}
						sendItemGetMessage(player, item, amount);
					}
				}
			}
		}
	}
	
	public static ItemHolder calcRewardPerItemHolder(Player player, int questId, int variant, int totalAmount)
	{
		final QuestTemplate template = QuestsParser.getInstance().getTemplate(questId);
		if (template != null)
		{
			final List<QuestRewardItem> rewards = template.getVariantRewards().get(variant);
			if (rewards != null && !rewards.isEmpty() && rewards.size() == 1)
			{
				for (final QuestRewardItem reward : rewards)
				{
					if (reward != null)
					{
						long amount = reward.getMaxCount() != 0 ? Rnd.get(reward.getMinCount(), reward.getMaxCount()) : reward.getMinCount();
						amount *= reward.isRateable() ? (Config.RATE_QUEST_REWARD * (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getQuestRewardRate() : player.getPremiumBonus().getQuestRewardRate())) : reward.getRate();
						amount *= totalAmount;
						return new ItemHolder(reward.getId(), amount);
					}
				}
			}
		}
		return null;
	}
	
	public static void calcRewardPerItem(Player player, int questId, int variant, int totalAmount, boolean isRandom)
	{
		final QuestTemplate template = QuestsParser.getInstance().getTemplate(questId);
		if (template != null)
		{
			final List<QuestRewardItem> rewards = template.getVariantRewards().get(variant);
			if (rewards != null && !rewards.isEmpty())
			{
				final QuestRewardItem reward = rewards.get(Rnd.get(rewards.size()));
				if (reward != null)
				{
					long amount = reward.getMaxCount() != 0 ? Rnd.get(reward.getMinCount(), reward.getMaxCount()) : reward.getMinCount();
					amount *= reward.isRateable() ? (Config.RATE_QUEST_REWARD * (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getQuestRewardRate() : player.getPremiumBonus().getQuestRewardRate())) : reward.getRate();
					amount *= totalAmount;
					final ItemInstance item = player.getInventory().addItem("Quest", reward.getId(), amount, player, player.getTarget());
					if (item == null)
					{
						return;
					}
					sendItemGetMessage(player, item, amount);
				}
			}
		}
	}
	
	public static void calcReward(Player player, int questId, int variant, boolean isRandom)
	{
		final QuestTemplate template = QuestsParser.getInstance().getTemplate(questId);
		if (template != null)
		{
			final List<QuestRewardItem> rewards = template.getVariantRewards().get(variant);
			if (rewards != null && !rewards.isEmpty())
			{
				if (isRandom)
				{
					final QuestRewardItem reward = rewards.get(Rnd.get(rewards.size()));
					if (reward != null)
					{
						long amount = reward.getMaxCount() != 0 ? Rnd.get(reward.getMinCount(), reward.getMaxCount()) : reward.getMinCount();
						amount *= reward.isRateable() ? (Config.RATE_QUEST_REWARD * (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getQuestRewardRate() : player.getPremiumBonus().getQuestRewardRate())) : reward.getRate();
						final ItemInstance item = player.getInventory().addItem("Quest", reward.getId(), amount, player, player.getTarget());
						if (item == null)
						{
							return;
						}
						sendItemGetMessage(player, item, amount);
					}
				}
				else
				{
					for (final QuestRewardItem reward : rewards)
					{
						if (reward != null)
						{
							long amount = reward.getMaxCount() != 0 ? Rnd.get(reward.getMinCount(), reward.getMaxCount()) : reward.getMinCount();
							amount *= reward.isRateable() ? (Config.RATE_QUEST_REWARD * (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getQuestRewardRate() : player.getPremiumBonus().getQuestRewardRate())) : reward.getRate();
							final ItemInstance item = player.getInventory().addItem("Quest", reward.getId(), amount, player, player.getTarget());
							if (item == null)
							{
								return;
							}
							sendItemGetMessage(player, item, amount);
						}
					}
				}
			}
		}
	}
	
	public static int getMinLvl(int questId)
	{
		final QuestTemplate template = QuestsParser.getInstance().getTemplate(questId);
		if (template != null)
		{
			return template.getMinLvl();
		}
		return 1;
	}
	
	public static int getMaxLvl(int questId)
	{
		final QuestTemplate template = QuestsParser.getInstance().getTemplate(questId);
		if (template != null)
		{
			return template.getMaxLvl();
		}
		return 85;
	}
	
	public StatsSet getQuestParams(int questId)
	{
		return QuestsParser.getInstance().getTemplate(questId).getParams();
	}
}