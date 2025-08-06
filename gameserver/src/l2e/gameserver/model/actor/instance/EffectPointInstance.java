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

import l2e.gameserver.ai.character.CharacterAI;
import l2e.gameserver.ai.character.EffectPointAI;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.l2skills.SkillSignet;
import l2e.gameserver.model.skills.l2skills.SkillSignetCasttime;
import l2e.gameserver.model.zone.ZoneId;

public class EffectPointInstance extends Npc
{
	private final Player _owner;
	private Skill _skill;
	private Skill _mainSkill;
	private final boolean _srcInArena;
	private boolean _isCastTime = false;
	
	public EffectPointInstance(int objectId, NpcTemplate template, Creature owner, Skill skill)
	{
		super(objectId, template);
		setInstanceType(InstanceType.EffectPointInstance);
		setIsInvul(false);
		_owner = owner == null ? null : owner.getActingPlayer();
		_srcInArena = _owner != null ? _owner.isInsideZone(ZoneId.PVP) && !_owner.isInsideZone(ZoneId.SIEGE) : false;
		if (owner != null)
		{
			setReflection(owner.getReflection());
		}
		
		if (skill != null && (skill instanceof SkillSignet || skill instanceof SkillSignetCasttime))
		{
			_skill = SkillsParser.getInstance().getInfo(skill.getEffectId(), skill.getLevel());
			_mainSkill = skill;
			_isCastTime = skill instanceof SkillSignetCasttime;
		}
	}

	@Override
	public Player getActingPlayer()
	{
		return _owner;
	}

	@Override
	public void onAction(Player player, boolean interact, boolean shift)
	{
		player.sendActionFailed();
	}

	@Override
	public void onActionShift(Player player)
	{
		if (player == null)
		{
			return;
		}

		player.sendActionFailed();
	}
	
	@Override
	protected CharacterAI initAI()
	{
		return new EffectPointAI(this);
	}
	
	public boolean isInSrcInArena()
	{
		return _srcInArena;
	}
	
	public Skill getSkill()
	{
		return _skill;
	}
	
	public Skill getMainSkill()
	{
		return _mainSkill;
	}
	
	public boolean isCastTime()
	{
		return _isCastTime;
	}
}