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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.DamageLimitParser;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.tasks.npc.trap.TrapTask;
import l2e.gameserver.model.actor.tasks.npc.trap.TrapTriggerTask;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.actor.templates.npc.DamageLimit;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.olympiad.OlympiadGameManager;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.Quest.TrapAction;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.NpcInfo.TrapInfo;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class TrapInstance extends Npc
{
	private static final int TICK = 1000;
	private boolean _hasLifeTime;
	private boolean _isInArena = false;
	private boolean _isTriggered;
	private final int _lifeTime;
	private Player _owner;
	private final List<Integer> _playersWhoDetectedMe = new ArrayList<>();
	private Skill _skill;
	private int _remainingTime;

	public TrapInstance(int objectId, NpcTemplate template, Reflection ref, int lifeTime)
	{
		super(objectId, template);
		setInstanceType(InstanceType.TrapInstance);
		setReflection(ref);
		for (final String lang : Config.MULTILANG_ALLOWED)
		{
			if (lang != null)
			{
				setName(lang, template.getName(lang) != null ? template.getName(lang) : template.getName(null));
			}
		}
		setIsInvul(false);
		
		_owner = null;
		_isTriggered = false;
		for (final Skill skill : template.getSkills().values())
		{
			if ((skill.getId() == 4072) || (skill.getId() == 4186) || (skill.getId() == 5267) || (skill.getId() == 5268) || (skill.getId() == 5269) || (skill.getId() == 5270) || (skill.getId() == 5271) || (skill.getId() == 5340) || (skill.getId() == 5422) || (skill.getId() == 5423) || (skill.getId() == 5424) || (skill.getId() == 5679))
			{
				_skill = skill;
				break;
			}
		}
		_hasLifeTime = lifeTime >= 0;
		_lifeTime = lifeTime != 0 ? lifeTime : 30000;
		_remainingTime = _lifeTime;
		if (_skill != null)
		{
			ThreadPoolManager.getInstance().schedule(new TrapTask(this), TICK);
		}
	}
	
	public TrapInstance(int objectId, NpcTemplate template, Player owner, int lifeTime)
	{
		this(objectId, template, owner.getReflection(), lifeTime);
		_owner = owner;
	}
	
	@Override
	public void broadcastPacket(GameServerPacket... packets)
	{
		World.getInstance().getAroundPlayers(this).stream().filter(p -> p != null && (_isTriggered || canBeSeen(p))).forEach(p -> p.sendPacket(packets));
	}
	
	@Override
	public void broadcastPacket(int range, GameServerPacket... packets)
	{
		World.getInstance().getAroundPlayers(this, range, 300).stream().filter(p -> p != null && (_isTriggered || canBeSeen(p))).forEach(p -> p.sendPacket(packets));
	}
	
	public boolean canBeSeen(Creature cha)
	{
		if ((cha != null) && _playersWhoDetectedMe.contains(cha.getObjectId()))
		{
			return true;
		}
		
		if ((_owner == null) || (cha == null))
		{
			return false;
		}
		if (cha == _owner)
		{
			return true;
		}
		
		if (cha.isPlayer())
		{
			if (((Player) cha).inObserverMode())
			{
				return false;
			}
			
			if (_owner.isInOlympiadMode() && ((Player) cha).isInOlympiadMode() && (((Player) cha).getOlympiadSide() != _owner.getOlympiadSide()))
			{
				return false;
			}
		}
		
		if (_isInArena)
		{
			return true;
		}
		
		if (_owner.isInParty() && cha.isInParty() && (_owner.getParty().getLeaderObjectId() == cha.getParty().getLeaderObjectId()))
		{
			return true;
		}
		return false;
	}
	
	public boolean checkTarget(Creature target)
	{
		if (!Skill.checkForAreaOffensiveSkills(this, target, _skill, _isInArena, this))
		{
			return false;
		}
		
		if ((target.isPlayer()) && ((Player) target).inObserverMode())
		{
			return false;
		}
		
		if ((_owner != null) && _owner.isInOlympiadMode())
		{
			final Player player = target.getActingPlayer();
			if ((player != null) && player.isInOlympiadMode() && (player.getOlympiadSide() == _owner.getOlympiadSide()))
			{
				return false;
			}
		}
		
		if (_isInArena)
		{
			return true;
		}
		
		if (_owner != null)
		{
			if (target instanceof Attackable)
			{
				return true;
			}
			
			final Player player = target.getActingPlayer();
			if ((player == null) || ((player.getPvpFlag() == 0) && (player.getKarma() == 0)))
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected void onDelete()
	{
		if (_owner != null)
		{
			_owner.setTrap(null);
			_owner = null;
		}
		super.onDelete();
	}
	
	@Override
	public Player getActingPlayer()
	{
		return _owner;
	}
	
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	public int getKarma()
	{
		return _owner != null ? _owner.getKarma() : 0;
	}
	
	public Player getOwner()
	{
		return _owner;
	}
	
	public byte getPvpFlag()
	{
		return _owner != null ? _owner.getPvpFlag() : 0;
	}
	
	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	public Skill getSkill()
	{
		return _skill;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return !canBeSeen(attacker);
	}
	
	@Override
	public boolean isTrap()
	{
		return true;
	}
	
	public boolean isTriggered()
	{
		return _isTriggered;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_isInArena = isInsideZone(ZoneId.PVP) && !isInsideZone(ZoneId.SIEGE);
		_playersWhoDetectedMe.clear();
	}
	
	@Override
	public void sendDamageMessage(Creature target, int damage, Skill skill, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss || (_owner == null))
		{
			return;
		}
		
		if (_owner.isInOlympiadMode() && (target.isPlayer()) && ((Player) target).isInOlympiadMode() && (((Player) target).getOlympiadGameId() == _owner.getOlympiadGameId()))
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(target.getActingPlayer(), damage);
		}
		
		if (Config.ALLOW_DAMAGE_LIMIT && target.isNpc())
		{
			final DamageLimit limit = DamageLimitParser.getInstance().getDamageLimit(target.getId());
			if (limit != null)
			{
				final int damageLimit = skill != null ? skill.isMagic() ? limit.getMagicDamage() : limit.getPhysicDamage() : limit.getDamage();
				if (damageLimit > 0 && damage > damageLimit)
				{
					damage = damageLimit;
				}
			}
		}
		
		if (target.isInvul() && !(target instanceof NpcInstance))
		{
			_owner.sendPacket(SystemMessageId.ATTACK_WAS_BLOCKED);
		}
		else
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DONE_S3_DAMAGE_TO_C2);
			sm.addCharName(this);
			sm.addCharName(target);
			sm.addNumber(damage);
			_owner.sendPacket(sm);
		}
	}
	
	@Override
	public void sendInfo(Player activeChar)
	{
		if (_isTriggered || canBeSeen(activeChar))
		{
			activeChar.sendPacket(new TrapInfo(this, activeChar));
		}
	}
	
	public void setDetected(Creature detector)
	{
		if (_isInArena)
		{
			if (detector.isPlayable())
			{
				sendInfo(detector.getActingPlayer());
			}
			return;
		}
		
		if ((_owner != null) && (_owner.getPvpFlag() == 0) && (_owner.getKarma() == 0))
		{
			return;
		}
		
		_playersWhoDetectedMe.add(detector.getObjectId());
		
		if (getTemplate().getEventQuests(QuestEventType.ON_TRAP_ACTION) != null)
		{
			for (final Quest quest : getTemplate().getEventQuests(QuestEventType.ON_TRAP_ACTION))
			{
				quest.notifyTrapAction(this, detector, TrapAction.TRAP_DETECTED);
			}
		}
		if (detector.isPlayable())
		{
			sendInfo(detector.getActingPlayer());
		}
	}
	
	public void triggerTrap(Creature target)
	{
		_isTriggered = true;
		broadcastPacket(new TrapInfo(this, null));
		setTarget(target);
		
		if (getTemplate().getEventQuests(QuestEventType.ON_TRAP_ACTION) != null)
		{
			for (final Quest quest : getTemplate().getEventQuests(QuestEventType.ON_TRAP_ACTION))
			{
				quest.notifyTrapAction(this, target, TrapAction.TRAP_TRIGGERED);
			}
		}
		
		ThreadPoolManager.getInstance().schedule(new TrapTriggerTask(this), 300);
	}
	
	public void unSummon()
	{
		if (_owner != null)
		{
			_owner.setTrap(null);
			_owner = null;
		}
		
		if (isVisible() && !isDead())
		{
			deleteMe();
		}
	}
	
	@Override
	public void updateAbnormalEffect()
	{
		
	}
	
	public boolean hasLifeTime()
	{
		return _hasLifeTime;
	}
	
	public void setHasLifeTime(boolean val)
	{
		_hasLifeTime = val;
	}
	
	public int getRemainingTime()
	{
		return _remainingTime;
	}
	
	public void setRemainingTime(int time)
	{
		_remainingTime = time;
	}
	
	public void setSkill(Skill skill)
	{
		_skill = skill;
	}
	
	public int getLifeTime()
	{
		return _lifeTime;
	}
}