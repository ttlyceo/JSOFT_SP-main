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
package l2e.gameserver.model.zone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.listener.Listener;
import l2e.commons.listener.ListenerList;
import l2e.commons.time.cron.SchedulingPattern;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.ReflectionParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.listener.other.OnZoneEnterLeaveListener;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.interfaces.ILocational;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public abstract class ZoneType
{
	protected static final Logger _log = LoggerFactory.getLogger(ZoneType.class);

	private final int _id;
	protected ZoneForm _zone;
	protected Map<Integer, Creature> _characterList;

	private boolean _checkAffected = false;
	
	private String _name = null;
	private String _type = null;
	private boolean _enabled = true;
	private final List<ZoneId> _zoneId = new ArrayList<>();
	private Reflection _reflection = ReflectionManager.DEFAULT;
	private int _reflectionId = _reflection.getId();
	private int _reflectionTemplateId = 0;
	private int _minLvl;
	private int _maxLvl;
	private int[] _race;
	private int[] _class;
	private char _classType;
	private Map<QuestEventType, List<Quest>> _questEvents;
	private InstanceType _target = InstanceType.Creature;
	private boolean _allowStore;
	private boolean _allowSellBuff;
	private AbstractZoneSettings _settings;
	private final ZoneListenerList _listeners = new ZoneListenerList();
	private double _hpLimit = -1;
	private double _mpLimit = -1;
	private double _cpLimit = -1;
	private double _pAtkLimit = -1;
	private double _pDefLimit = -1;
	private double _pAtkSpeedLimit = -1;
	private double _mAtkLimit = -1;
	private double _mDefLimit = -1;
	private double _mAtkSpeedLimit = -1;
	private double _critDmgLimit = -1;
	private double _runSpeedLimit = -1;
	private double _walkSpeedLimit = -1;
	private int _accuracyLimit = -1;
	private double _critHitLimit = -1;
	private double _mCritHitLimit = -1;
	private int _evasionLimit = -1;
	private double _pvpPhysSkillDmgLimit = -1;
	private double _pvpPhysSkillDefLimit = -1;
	private double _pvpPhysDefLimit = -1;
	private double _pvpPhysDmgLimit = -1;
	private double _pvpMagicDmgLimit = -1;
	private double _pvpMagicDefLimit = -1;
	
	private SchedulingPattern _activePattern;
	private int _activeTime;
	private String _activeSpawnGroups;
	private ServerMessage _activeAnnounce;
	private boolean _activeBanish = false;
	private ServerMessage _deactiveAnnounce;
	private boolean _deactiveBanish = false;
	private boolean _banishIfNotActive = false;
	private ScheduledFuture<?> _activeTask = null;
	private String _enterMessage = null;
	private String _exitMessage = null;
	
	protected ZoneType(int id)
	{
		_id = id;
		
		_characterList = new ConcurrentHashMap<>();

		_minLvl = 0;
		_maxLvl = 0xFF;

		_classType = 0;

		_race = null;
		_class = null;
		_allowStore = false;
		_allowSellBuff = false;
	}
	
	public class ZoneListenerList extends ListenerList<ZoneType>
	{
		public void onEnter(final Creature actor)
		{
			if (!getListeners().isEmpty())
			{
				getListeners().stream().forEach(l -> ((OnZoneEnterLeaveListener) l).onZoneEnter(ZoneType.this, actor));
			}
		}

		public void onLeave(final Creature actor)
		{
			if (!getListeners().isEmpty())
			{
				getListeners().stream().forEach(l -> ((OnZoneEnterLeaveListener) l).onZoneLeave(ZoneType.this, actor));
			}
		}
	}

	public int getId()
	{
		return _id;
	}

	public void setParameter(String name, String value)
	{
		_checkAffected = true;

		if (name.equals("name"))
		{
			_name = value;
		}
		else if (name.equalsIgnoreCase("default_enabled"))
		{
			_enabled = Boolean.parseBoolean(value);
		}
		else if (name.equals("reflectionId"))
		{
			_reflectionId = Integer.parseInt(value);
		}
		else if (name.equals("reflectionTemplateId"))
		{
			_reflectionTemplateId = Integer.parseInt(value);
		}
		else if (name.equals("affectedLvlMin"))
		{
			_minLvl = Integer.parseInt(value);
		}
		else if (name.equals("affectedLvlMax"))
		{
			_maxLvl = Integer.parseInt(value);
		}
		else if (name.equals("affectedRace"))
		{
			if (_race == null)
			{
				_race = new int[1];
				_race[0] = Integer.parseInt(value);
			}
			else
			{
				final int[] temp = new int[_race.length + 1];

				int i = 0;
				for (; i < _race.length; i++)
				{
					temp[i] = _race[i];
				}

				temp[i] = Integer.parseInt(value);

				_race = temp;
			}
		}
		else if (name.equals("affectedClassId"))
		{
			if (_class == null)
			{
				_class = new int[1];
				_class[0] = Integer.parseInt(value);
			}
			else
			{
				final int[] temp = new int[_class.length + 1];

				int i = 0;
				for (; i < _class.length; i++)
				{
					temp[i] = _class[i];
				}

				temp[i] = Integer.parseInt(value);

				_class = temp;
			}
		}
		else if (name.equals("affectedClassType"))
		{
			if (value.equals("Fighter"))
			{
				_classType = 1;
			}
			else
			{
				_classType = 2;
			}
		}
		else if (name.equals("targetClass"))
		{
			_target = Enum.valueOf(InstanceType.class, value);
		}
		else if (name.equals("allowStore"))
		{
			_allowStore = Boolean.parseBoolean(value);
		}
		else if (name.equals("allowSellBuff"))
		{
			_allowSellBuff = Boolean.parseBoolean(value);
		}
		else if (name.equals("hpLimit"))
		{
			_hpLimit = Double.parseDouble(value);
			if (_hpLimit > 0)
			{
				addZoneId(ZoneId.HP_LIMIT);
			}
		}
		else if (name.equals("mpLimit"))
		{
			_mpLimit = Double.parseDouble(value);
			if (_mpLimit > 0)
			{
				addZoneId(ZoneId.MP_LIMIT);
			}
		}
		else if (name.equals("cpLimit"))
		{
			_cpLimit = Double.parseDouble(value);
			if (_cpLimit > 0)
			{
				addZoneId(ZoneId.CP_LIMIT);
			}
		}
		else if (name.equals("pAtkLimit"))
		{
			_pAtkLimit = Double.parseDouble(value);
			if (_pAtkLimit > 0)
			{
				addZoneId(ZoneId.P_ATK_LIMIT);
			}
		}
		else if (name.equals("pDefLimit"))
		{
			_pDefLimit = Double.parseDouble(value);
			if (_pDefLimit > 0)
			{
				addZoneId(ZoneId.P_DEF_LIMIT);
			}
		}
		else if (name.equals("pAtkSpeedLimit"))
		{
			_pAtkSpeedLimit = Double.parseDouble(value);
			if (_pAtkSpeedLimit > 0)
			{
				addZoneId(ZoneId.ATK_SPEED_LIMIT);
			}
		}
		else if (name.equals("mAtkLimit"))
		{
			_mAtkLimit = Double.parseDouble(value);
			if (_mAtkLimit > 0)
			{
				addZoneId(ZoneId.M_ATK_LIMIT);
			}
		}
		else if (name.equals("mDefLimit"))
		{
			_mDefLimit = Double.parseDouble(value);
			if (_mDefLimit > 0)
			{
				addZoneId(ZoneId.M_DEF_LIMIT);
			}
		}
		else if (name.equals("mAtkSpeedLimit"))
		{
			_mAtkSpeedLimit = Double.parseDouble(value);
			if (_mAtkSpeedLimit > 0)
			{
				addZoneId(ZoneId.M_ATK_SPEED_LIMIT);
			}
		}
		else if (name.equals("critDmgLimit"))
		{
			_critDmgLimit = Double.parseDouble(value);
			if (_critDmgLimit > 0)
			{
				addZoneId(ZoneId.CRIT_DMG_LIMIT);
			}
		}
		else if (name.equals("runSpeedLimit"))
		{
			_runSpeedLimit = Double.parseDouble(value);
			if (_runSpeedLimit > 0)
			{
				addZoneId(ZoneId.RUN_SPEED_LIMIT);
			}
		}
		else if (name.equals("walkSpeedLimit"))
		{
			_walkSpeedLimit = Double.parseDouble(value);
			if (_walkSpeedLimit > 0)
			{
				addZoneId(ZoneId.WALK_SPEED_LIMIT);
			}
		}
		else if (name.equals("accuracyLimit"))
		{
			_accuracyLimit = Integer.parseInt(value);
			if (_accuracyLimit > 0)
			{
				addZoneId(ZoneId.ACCURACY_LIMIT);
			}
		}
		else if (name.equals("critHitLimit"))
		{
			_critHitLimit = Double.parseDouble(value);
			if (_critHitLimit > 0)
			{
				addZoneId(ZoneId.CRIT_HIT_LIMIT);
			}
		}
		else if (name.equals("mCritHitLimit"))
		{
			_mCritHitLimit = Double.parseDouble(value);
			if (_mCritHitLimit > 0)
			{
				addZoneId(ZoneId.MCRIT_HIT_LIMIT);
			}
		}
		else if (name.equals("evasionLimit"))
		{
			_evasionLimit = Integer.parseInt(value);
			if (_evasionLimit > 0)
			{
				addZoneId(ZoneId.EVASION_LIMIT);
			}
		}
		else if (name.equals("pvpPhysSkillDmgLimit"))
		{
			_pvpPhysSkillDmgLimit = Double.parseDouble(value);
			if (_pvpPhysSkillDmgLimit > 0)
			{
				addZoneId(ZoneId.PVP_PHYS_SKILL_DMG_LIMIT);
			}
		}
		else if (name.equals("pvpPhysSkillDefLimit"))
		{
			_pvpPhysSkillDefLimit = Double.parseDouble(value);
			if (_pvpPhysSkillDefLimit > 0)
			{
				addZoneId(ZoneId.PVP_PHYS_SKILL_DEF_LIMIT);
			}
		}
		else if (name.equals("pvpPhysDefLimit"))
		{
			_pvpPhysDefLimit = Double.parseDouble(value);
			if (_pvpPhysDefLimit > 0)
			{
				addZoneId(ZoneId.PVP_PHYS_DEF_LIMIT);
			}
		}
		else if (name.equals("pvpPhysDmgLimit"))
		{
			_pvpPhysDmgLimit = Double.parseDouble(value);
			if (_pvpPhysDmgLimit > 0)
			{
				addZoneId(ZoneId.PVP_PHYS_DMG_LIMIT);
			}
		}
		else if (name.equals("pvpMagicDmgLimit"))
		{
			_pvpMagicDmgLimit = Double.parseDouble(value);
			if (_pvpMagicDmgLimit > 0)
			{
				addZoneId(ZoneId.PVP_MAGIC_DMG_LIMIT);
			}
		}
		else if (name.equals("pvpMagicDefLimit"))
		{
			_pvpMagicDefLimit = Double.parseDouble(value);
			if (_pvpMagicDefLimit > 0)
			{
				addZoneId(ZoneId.PVP_MAGIC_DEF_LIMIT);
			}
		}
		else if (name.equals("activePattern"))
		{
			_activePattern = value == null || value.isEmpty() ? null : new SchedulingPattern(value);
		}
		else if (name.equals("activeTime"))
		{
			_activeTime = Integer.parseInt(value);
		}
		else if (name.equals("activeSpawnGroups"))
		{
			_activeSpawnGroups = value;
		}
		else if (name.equals("activeAnnounce"))
		{
			_activeAnnounce = new ServerMessage(value, true);
		}
		else if (name.equals("activeBanish"))
		{
			_activeBanish = Boolean.parseBoolean(value);
		}
		else if (name.equals("deactiveAnnounce"))
		{
			_deactiveAnnounce = new ServerMessage(value, true);
		}
		else if (name.equals("deactiveBanish"))
		{
			_deactiveBanish = Boolean.parseBoolean(value);
		}
		else if (name.equals("banishIfNotActive"))
		{
			_banishIfNotActive = Boolean.parseBoolean(value);
		}
		else if (name.equals("enterMessage"))
		{
			_enterMessage = value;
		}
		else if (name.equals("exitMessage"))
		{
			_exitMessage = value;
		}
		else
		{
			_log.info(getClass().getSimpleName() + ": Unknown parameter - " + name + " in zone: " + getId());
		}
	}

	private boolean isAffected(Creature character)
	{
		if ((character.getLevel() < _minLvl) || (character.getLevel() > _maxLvl))
		{
			return false;
		}

		if (!character.isInstanceType(_target))
		{
			return false;
		}

		if (character.isPlayer())
		{
			if (_classType != 0)
			{
				if (((Player) character).isMageClass())
				{
					if (_classType == 1)
					{
						return false;
					}
				}
				else if (_classType == 2)
				{
					return false;
				}
			}

			if (_race != null)
			{
				boolean ok = false;

				for (final int element : _race)
				{
					if (((Player) character).getRace().ordinal() == element)
					{
						ok = true;
						break;
					}
				}

				if (!ok)
				{
					return false;
				}
			}

			if (_class != null)
			{
				boolean ok = false;

				for (final int _clas : _class)
				{
					if (((Player) character).getClassId().ordinal() == _clas)
					{
						ok = true;
						break;
					}
				}

				if (!ok)
				{
					return false;
				}
			}
		}
		return true;
	}

	public void setZone(ZoneForm zone)
	{
		if (_zone != null)
		{
			throw new IllegalStateException("Zone already set");
		}
		_zone = zone;
	}

	public ZoneForm getZone()
	{
		return _zone;
	}

	public void setName(String name)
	{
		_name = name;
	}

	public String getName()
	{
		return _name;
	}

	public Reflection getReflection()
	{
		return _reflection;
	}
	
	public int getReflectionId()
	{
		return _reflectionId;
	}
	
	public int getReflectionTemplateId()
	{
		return _reflectionTemplateId;
	}

	public boolean isInsideZone(ILocational loc)
	{
		return _zone.isInsideZone(loc.getX(), loc.getY(), loc.getZ());
	}

	public boolean isInsideZone(int x, int y)
	{
		return _zone.isInsideZone(x, y, _zone.getHighZ());
	}

	public boolean isInsideZone(int x, int y, int z)
	{
		return _zone.isInsideZone(x, y, z);
	}

	public boolean isInsideZone(int x, int y, int z, Reflection r)
	{
		if (_reflection.isDefault() || (_reflection.getId() == r.getId()))
		{
			return _zone.isInsideZone(x, y, z);
		}
		return false;
	}

	public boolean isInsideZone(GameObject object)
	{
		return isInsideZone(object.getX(), object.getY(), object.getZ(), object.getReflection());
	}

	public double getDistanceToZone(int x, int y)
	{
		return getZone().getDistanceToZone(x, y);
	}

	public double getDistanceToZone(GameObject object)
	{
		return getZone().getDistanceToZone(object.getX(), object.getY());
	}

	public void revalidateInZone(Creature character, boolean isForce)
	{
		if (_checkAffected)
		{
			if (!isAffected(character))
			{
				return;
			}
		}

		if (isInsideZone(character))
		{
			final var notFound = !_characterList.containsKey(character.getObjectId());
			if (notFound || isForce)
			{
				if (notFound)
				{
					_characterList.put(character.getObjectId(), character);
				}
				
				if (isEnabled())
				{
					final var quests = getQuestByEvent(QuestEventType.ON_ENTER_ZONE);
					if (quests != null)
					{
						quests.stream().forEach(q -> q.notifyEnterZone(character, this));
					}
					onEnter(character);
					_listeners.onEnter(character);
					if ((character != null) && (character.isPlayer()))
					{
						if (_enterMessage != null && !_enterMessage.isEmpty())
						{
							character.sendMessage(new ServerMessage(_enterMessage, character.getActingPlayer().getLang()).toString());
						}
						else
						{
							if (character.getActingPlayer().isGM())
							{
								character.sendMessage("Entered the zone " + getName());
							}
						}
					}
				}
				
				if (_banishIfNotActive && !isEnabled())
				{
					if (character.isPlayer())
					{
						final TeleportWhereType type = TeleportWhereType.TOWN;
						character.teleToLocation(type, true, ReflectionManager.DEFAULT);
					}
				}
			}
		}
		else
		{
			removeCharacter(character);
		}
	}

	public void removeCharacter(Creature character)
	{
		if (_characterList.containsKey(character.getObjectId()))
		{
			if (isEnabled())
			{
				final var quests = getQuestByEvent(QuestEventType.ON_EXIT_ZONE);
				if (quests != null)
				{
					quests.stream().forEach(q -> q.notifyExitZone(character, this));
				}
				onExit(character);
				
				_characterList.remove(character.getObjectId());
				_listeners.onLeave(character);
				
				if ((character != null) && (character.isPlayer()))
				{
					if (_exitMessage != null && !_exitMessage.isEmpty())
					{
						character.sendMessage(new ServerMessage(_exitMessage, character.getActingPlayer().getLang()).toString());
					}
					else
					{
						if (character.getActingPlayer().isGM())
						{
							character.sendMessage("Left the area " + getName());
						}
					}
				}
			}
			else
			{
				_characterList.remove(character.getObjectId());
			}
		}
	}

	public boolean isCharacterInZone(Creature character)
	{
		return _characterList.containsKey(character.getObjectId());
	}

	public AbstractZoneSettings getSettings()
	{
		return _settings;
	}

	public void setSettings(AbstractZoneSettings settings)
	{
		if (_settings != null)
		{
			_settings.clear();
		}
		_settings = settings;
	}

	protected abstract void onEnter(Creature character);

	protected abstract void onExit(Creature character);

	public void onDieInside(Creature character)
	{
	}

	public void onReviveInside(Creature character)
	{
	}

	public void onPlayerLoginInside(Player player)
	{
	}

	public void onPlayerLogoutInside(Player player)
	{
	}

	public Map<Integer, Creature> getCharacters()
	{
		return _characterList;
	}

	public Collection<Creature> getCharactersInside()
	{
		return _characterList.values();
	}

	public List<Player> getPlayersInside()
	{
		final List<Player> players = new ArrayList<>();
		for (final Creature ch : _characterList.values())
		{
			if ((ch != null) && ch.isPlayer())
			{
				players.add(ch.getActingPlayer());
			}
		}
		return players;
	}
	
	public List<Player> getPlayersInside(Reflection ref)
	{
		final List<Player> players = new ArrayList<>();
		for (final var ch : _characterList.values())
		{
			if ((ch != null) && ch.isPlayer() && ch.getReflectionId() == ref.getId())
			{
				players.add(ch.getActingPlayer());
			}
		}
		return players;
	}

	public void addQuestEvent(QuestEventType EventType, Quest q)
	{
		if (_questEvents == null)
		{
			_questEvents = new HashMap<>();
		}
		var questByEvents = _questEvents.get(EventType);
		if (questByEvents == null)
		{
			questByEvents = new ArrayList<>();
		}
		if (!questByEvents.contains(q))
		{
			questByEvents.add(q);
		}
		_questEvents.put(EventType, questByEvents);
	}

	public List<Quest> getQuestByEvent(QuestEventType EventType)
	{
		if (_questEvents == null)
		{
			return null;
		}
		return _questEvents.get(EventType);
	}

	public void broadcastPacket(GameServerPacket packet)
	{
		final var players = getPlayersInside();
		if (players.isEmpty())
		{
			return;
		}
		players.stream().filter(p -> p != null).forEach(p -> p.sendPacket(packet));
	}

	public InstanceType getTargetType()
	{
		return _target;
	}

	public void setTargetType(InstanceType type)
	{
		_target = type;
		_checkAffected = true;
	}

	public boolean isAllowStore()
	{
		return _allowStore;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + _id + "]";
	}

	public void visualizeZone(int z)
	{
		getZone().visualizeZone(z);
	}
	
	public <T extends Listener<ZoneType>> boolean addListener(final T listener)
	{
		return _listeners.add(listener);
	}

	public <T extends Listener<ZoneType>> boolean removeListener(final T listener)
	{
		return _listeners.remove(listener);
	}
	
	public void generateReflection()
	{
		final var template = ReflectionParser.getInstance().getReflectionId(_reflectionTemplateId);
		if (template != null)
		{
			_reflection = ReflectionManager.getInstance().createDynamicReflection(template);
			_reflectionId = _reflection.getId();
		}
	}
	
	public void setType(String type)
	{
		_type = type;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public void addZoneId(ZoneId id)
	{
		_zoneId.add(id);
	}
	
	public List<ZoneId> getZoneId()
	{
		return _zoneId;
	}
	
	public double getHpLimit()
	{
		return _hpLimit;
	}
	
	public double getMpLimit()
	{
		return _mpLimit;
	}
	
	public double getCpLimit()
	{
		return _cpLimit;
	}
	
	public double getPAtkLimit()
	{
		return _pAtkLimit;
	}
	
	public double getPDefLimit()
	{
		return _pDefLimit;
	}
	
	public double getAtkSpeedLimit()
	{
		return _pAtkSpeedLimit;
	}
	
	public double getMAtkLimit()
	{
		return _mAtkLimit;
	}
	
	public double getMDefLimit()
	{
		return _mDefLimit;
	}
	
	public double getMAtkSpeedLimit()
	{
		return _mAtkSpeedLimit;
	}
	
	public double getCritDmgLimit()
	{
		return _critDmgLimit;
	}
	
	public double getRunSpeedLimit()
	{
		return _runSpeedLimit;
	}
	
	public double getWalkSpeedLimit()
	{
		return _walkSpeedLimit;
	}
	
	public int getAccuracyLimit()
	{
		return _accuracyLimit;
	}
	
	public double getCritHitLimit()
	{
		return _critHitLimit;
	}
	
	public double getMCritHitLimit()
	{
		return _mCritHitLimit;
	}
	
	public int getEvasionLimit()
	{
		return _evasionLimit;
	}
	
	public double getPvpPhysSkillDmgLimit()
	{
		return _pvpPhysSkillDmgLimit;
	}
	
	public double getPvpPhysSkillDefLimit()
	{
		return _pvpPhysSkillDefLimit;
	}
	
	public double getPvpPhysDefLimit()
	{
		return _pvpPhysDefLimit;
	}
	
	public double getPvpPhysDmgLimit()
	{
		return _pvpPhysDmgLimit;
	}
	
	public double getPvpMagicDmgLimit()
	{
		return _pvpMagicDmgLimit;
	}
	
	public double getPvpMagicDefLimit()
	{
		return _pvpMagicDefLimit;
	}
	
	public void setIsEnabled(boolean state)
	{
		_enabled = state;
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public SchedulingPattern getSchedulePattern()
	{
		return _activePattern;
	}
	
	public int getActiveTime()
	{
		return _activeTime;
	}
	
	public String getActiveSpawnGroups()
	{
		return _activeSpawnGroups;
	}
	
	public ServerMessage getActiveAnnounce()
	{
		return _activeAnnounce;
	}
	
	public ServerMessage getDeActiveAnnounce()
	{
		return _deactiveAnnounce;
	}
	
	public void clearTask()
	{
		if (_activeTask != null)
		{
			_activeTask.cancel(false);
			_activeTask = null;
		}
	}
	
	public void calcActivationTime()
	{
		clearTask();
		long activeTime = ServerVariables.getLong("activeZone_" + getId() + "", 0);
		if (activeTime > System.currentTimeMillis())
		{
			if (!isEnabled())
			{
				activeTimeZone(true, (activeTime - System.currentTimeMillis()));
			}
			else
			{
				_activeTask = ThreadPoolManager.getInstance().schedule(new SwitchStatusTask(false), activeTime - System.currentTimeMillis());
				if (Config.TIME_ZONE_DEBUG)
				{
					_log.info(getName() != null && !getName().isEmpty() ? "Zone[" + getName() + "] will be deactive in " + new Date(activeTime) + "" : "Zone ID[" + getId() + "] will be deactive in " + new Date(activeTime) + "");
				}
			}
		}
		else
		{
			if (getSchedulePattern() != null)
			{
				activeTime = getSchedulePattern().next(System.currentTimeMillis());
				_activeTask = ThreadPoolManager.getInstance().schedule(new SwitchStatusTask(true), activeTime - System.currentTimeMillis());
				if (Config.TIME_ZONE_DEBUG)
				{
					_log.info(getName() != null && !getName().isEmpty() ? "Zone[" + getName() + "] will be active in " + new Date(activeTime) + "" : "Zone ID[" + getId() + "> will be active in " + new Date(activeTime) + "");
				}
			}
		}
	}
	
	public void activeTimeZone(boolean isActive, long deactiveTime)
	{
		final var players = getPlayersInside();
		clearTask();
		final String spawnGroup = getActiveSpawnGroups();
		String[] group = null;
		if (spawnGroup != null && !spawnGroup.isEmpty())
		{
			group = spawnGroup.split(";");
		}
		
		if (isActive)
		{
			setIsEnabled(isActive);
			if (getActiveAnnounce() != null)
			{
				Announcements.getInstance().announceToAll(getActiveAnnounce());
			}
			
			for (final var player : players)
			{
				if (player != null)
				{
					if (_activeBanish)
					{
						final TeleportWhereType type = TeleportWhereType.TOWN;
						player.teleToLocation(type, true, ReflectionManager.DEFAULT);
					}
					else
					{
						revalidateInZone(player, true);
					}
				}
			}
			
			if (group != null)
			{
				for (final String name : group)
				{
					if (name != null)
					{
						SpawnParser.getInstance().spawnGroup(name);
					}
				}
			}
			_activeTask = ThreadPoolManager.getInstance().schedule(new SwitchStatusTask(false), deactiveTime > 0 ? deactiveTime : (_activeTime * 60000));
			if (deactiveTime <= 0)
			{
				ServerVariables.set("activeZone_" + getId() + "", (System.currentTimeMillis() + (_activeTime * 60000)));
			}
			
			if (Config.TIME_ZONE_DEBUG)
			{
				_log.info(getName() != null && !getName().isEmpty() ? "Zone[" + getName() + "] will be deactive in " + new Date((System.currentTimeMillis() + (_activeTime * 60000))) + "" : "Zone ID[" + getId() + "] will be deactive in " + new Date((System.currentTimeMillis() + (_activeTime * 60000))) + "");
			}
		}
		else
		{
			if (getDeActiveAnnounce() != null)
			{
				Announcements.getInstance().announceToAll(getDeActiveAnnounce());
			}
			
			for (final var player : players)
			{
				if (player != null)
				{
					if (_deactiveBanish)
					{
						final TeleportWhereType type = TeleportWhereType.TOWN;
						player.teleToLocation(type, true, ReflectionManager.DEFAULT);
					}
					removeCharacter(player);
				}
			}
			
			setIsEnabled(isActive);
			if (group != null)
			{
				for (final String name : group)
				{
					if (name != null)
					{
						SpawnParser.getInstance().despawnGroup(name);
					}
				}
			}
			
			if (getSchedulePattern() != null)
			{
				final long activeTime = getSchedulePattern().next(System.currentTimeMillis());
				_activeTask = ThreadPoolManager.getInstance().schedule(new SwitchStatusTask(true), activeTime - System.currentTimeMillis());
				if (Config.TIME_ZONE_DEBUG)
				{
					_log.info(getName() != null && !getName().isEmpty() ? "Zone[" + getName() + "] will be active in " + new Date(activeTime) + "" : "Zone ID[" + getId() + "] will be deactive in " + new Date(activeTime) + "");
				}
			}
		}
	}
	
	public class SwitchStatusTask implements Runnable
	{
		private final boolean _isActive;
		
		public SwitchStatusTask(boolean isActive)
		{
			_isActive = isActive;
		}
		
		@Override
		public void run()
		{
			activeTimeZone(_isActive, 0);
		}
	}
	
	public boolean isAllowSellBuff()
	{
		return _allowSellBuff;
	}
}