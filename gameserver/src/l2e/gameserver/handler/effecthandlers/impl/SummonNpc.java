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
package l2e.gameserver.handler.effecthandlers.impl;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ChronoMonsterInstance;
import l2e.gameserver.model.actor.instance.DecoyInstance;
import l2e.gameserver.model.actor.instance.EffectPointInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.serverpackets.NpcInfo.Info;

public class SummonNpc extends Effect
{
	private final int _despawnDelay;
	private final int _npcId;
	private final int _npcCount;
	private final boolean _randomOffset;
	private final boolean _isSummonSpawn;
	
	public SummonNpc(Env env, EffectTemplate template)
	{
		super(env, template);
		_despawnDelay = template.getParameters().getInteger("despawnDelay", 20000);
		_npcId = template.getParameters().getInteger("npcId", 0);
		_npcCount = template.getParameters().getInteger("npcCount", 1);
		_randomOffset = template.getParameters().getBool("randomOffset", false);
		_isSummonSpawn = template.getParameters().getBool("isSummonSpawn", false);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}
	
	@Override
	public boolean onStart()
	{
		if ((getEffected() == null) || !getEffected().isPlayer() || getEffected().isAlikeDead() || getEffected().getActingPlayer().inObserverMode())
		{
			return false;
		}
		
		if ((_npcId <= 0) || (_npcCount <= 0))
		{
			_log.warn(SummonNpc.class.getSimpleName() + ": Invalid NPC Id or count skill Id: " + getSkill().getId());
			return false;
		}
		
		final Player player = getEffected().getActingPlayer();
		if (player.isMounted())
		{
			return false;
		}
		
		final NpcTemplate npcTemplate = NpcsParser.getInstance().getTemplate(_npcId);
		if (npcTemplate == null)
		{
			_log.warn(SummonNpc.class.getSimpleName() + ": Spawn of the nonexisting NPC Id: " + _npcId + ", skill Id:" + getSkill().getId());
			return false;
		}
		
		switch (npcTemplate.getType())
		{
			case "Decoy" :
			{
				final DecoyInstance decoy = new DecoyInstance(IdFactory.getInstance().getNextId(), npcTemplate, player, _despawnDelay);
				decoy.setCurrentHp(decoy.getMaxHp());
				decoy.setCurrentMp(decoy.getMaxMp());
				decoy.setHeading(player.getHeading());
				decoy.setReflection(player.getReflection());
				decoy.spawnMe(player.getX(), player.getY(), player.getZ());
				player.setDecoy(decoy);
				break;
			}
			case "EffectPoint" :
			{
				final EffectPointInstance effectPoint = new EffectPointInstance(IdFactory.getInstance().getNextId(), npcTemplate, player, getSkill());
				effectPoint.setCurrentHp(effectPoint.getMaxHp());
				effectPoint.setCurrentMp(effectPoint.getMaxMp());
				int x = player.getX();
				int y = player.getY();
				int z = player.getZ();
				
				if (getSkill().getTargetType() == TargetType.GROUND)
				{
					final Location wordPosition = player.getActingPlayer().getCurrentSkillWorldPosition();
					if (wordPosition != null)
					{
						x = wordPosition.getX();
						y = wordPosition.getY();
						z = wordPosition.getZ();
					}
				}
				getSkill().getEffects(player, effectPoint, false);
				effectPoint.setIsInvul(true);
				effectPoint.spawnMe(x, y, z);
				break;
			}
			default :
			{
				Spawner spawn;
				try
				{
					spawn = new Spawner(npcTemplate);
				}
				catch (final Exception e)
				{
					_log.warn(SummonNpc.class.getSimpleName() + ": " + e.getMessage());
					return false;
				}
				
				spawn.setReflection(player.getReflection());
				spawn.setHeading(-1);
				
				Location loc = null;
				if (_randomOffset)
				{
					loc = Location.findPointToStayPet(player, 50, 70);
					spawn.setX(loc.getX());
					spawn.setY(loc.getY());
				}
				else
				{
					spawn.setX(player.getX());
					spawn.setY(player.getY());
				}
				spawn.setZ(loc != null ? loc.getZ() : player.getZ());
				spawn.stopRespawn();
				
				final Npc npc = spawn.doSpawn(_isSummonSpawn, 0);
				for (final String lang : Config.MULTILANG_ALLOWED)
				{
					if (lang != null)
					{
						npc.setName(lang, npcTemplate.getName(lang) != null ? npcTemplate.getName(lang) : npcTemplate.getName(null));
						npc.setTitle(lang, npcTemplate.getTitle(lang) != null ? npcTemplate.getTitle(lang) : npcTemplate.getTitle(null));
					}
				}
				npc.setSummoner(player);
				if (_despawnDelay > 0)
				{
					npc.scheduleDespawn(_despawnDelay);
				}
				if (npc instanceof ChronoMonsterInstance)
				{
					((ChronoMonsterInstance) npc).setOwner(player);
					npc.setGlobalTitle(player.getName(null));
					npc.broadcastPacket(new Info(npc, null));
				}
				npc.setIsRunning(false);
			}
		}
		return true;
	}
}