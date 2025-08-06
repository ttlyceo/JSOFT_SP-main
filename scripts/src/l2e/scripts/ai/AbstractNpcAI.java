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
package l2e.scripts.ai;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.SocialAction;

public abstract class AbstractNpcAI extends Quest
{
	public Logger _log = LoggerFactory.getLogger(getClass());
	
	public AbstractNpcAI(String name, String descr)
	{
		super(-1, name, descr);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	public void registerMobs(int... mobs)
	{
		addAttackId(mobs);
		addKillId(mobs);
		addSpawnId(mobs);
		addSpellFinishedId(mobs);
		addSkillSeeId(mobs);
		addAggroRangeEnterId(mobs);
		addFactionCallId(mobs);
	}
	
	public void registerMobs(int[] mobs, QuestEventType... types)
	{
		for (final QuestEventType type : types)
		{
			addEventId(type, mobs);
		}
	}
	
	public void registerMobs(Iterable<Integer> mobs, QuestEventType... types)
	{
		for (final int id : mobs)
		{
			for (final QuestEventType type : types)
			{
				addEventId(type, id);
			}
		}
	}
	
	protected void broadcastNpcSay(Npc npc, int type, NpcStringId stringId, String... parameters)
	{
		final NpcSay say = new NpcSay(npc.getObjectId(), type, npc.getTemplate().getIdTemplate(), stringId);
		if (parameters != null)
		{
			for (final String parameter : parameters)
			{
				say.addStringParameter(parameter);
			}
		}
		npc.broadcastPacketToOthers(2000, say);
	}
	
	protected void broadcastNpcSay(Npc npc, int type, String text)
	{
		npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), type, npc.getTemplate().getIdTemplate(), text));
	}
	
	protected void broadcastNpcSay(Npc npc, int type, NpcStringId stringId)
	{
		npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), type, npc.getTemplate().getIdTemplate(), stringId));
	}
	
	protected void broadcastNpcSay(Npc npc, int type, String text, int radius)
	{
		npc.broadcastPacketToOthers(radius, new NpcSay(npc.getObjectId(), type, npc.getTemplate().getIdTemplate(), text));
	}
	
	protected void broadcastNpcSay(Npc npc, int type, NpcStringId stringId, int radius)
	{
		npc.broadcastPacketToOthers(radius, new NpcSay(npc.getObjectId(), type, npc.getTemplate().getIdTemplate(), stringId));
	}
	
	protected void broadcastSocialAction(Creature character, int actionId)
	{
		character.broadcastPacketToOthers(2000, new SocialAction(character.getObjectId(), actionId));
	}
	
	protected void broadcastSocialAction(Creature character, int actionId, int radius)
	{
		character.broadcastPacketToOthers(radius, new SocialAction(character.getObjectId(), actionId));
	}
	
	protected void attackPlayer(Attackable npc, Playable playable)
	{
		attackPlayer(npc, playable, 999);
	}
	
	protected void attackPlayer(Npc npc, Playable target, int desire)
	{
		if (npc instanceof Attackable)
		{
			((Attackable) npc).addDamageHate(target, 0, desire);
		}
		npc.setIsRunning(true);
		npc.getAI().setIntention(CtrlIntention.ATTACK, target);
	}
	
	public Player setRandomPlayerTarget(Npc npc)
	{
		final ArrayList<Player> result = new ArrayList<>();
		for (final Player obj : World.getInstance().getAroundPlayers(npc))
		{
			if (!(obj.getZ() < (npc.getZ() - 100)) && !(obj.getZ() > (npc.getZ() + 100)) && !obj.isDead())
			{
				result.add(obj);
			}
		}
		if (!result.isEmpty() && (result.size() != 0))
		{
			return result.get(getRandom(result.size()));
		}
		return null;
	}
	
	protected void addSkillCastDesire(Npc npc, GameObject target, SkillHolder skill, int desire)
	{
		addSkillCastDesire(npc, target, skill.getSkill(), desire);
	}
	
	protected void addSkillCastDesire(Npc npc, GameObject target, Skill skill, int desire)
	{
		if (npc.isAttackable() && (target != null) && target.isCreature())
		{
			((Attackable) npc).addDamageHate((Creature) target, 0, desire);
		}
		npc.setTarget(target != null ? target : npc);
		npc.doCast(skill);
	}
	
	protected void addMoveToDesire(Npc npc, Location loc, int desire)
	{
		npc.getAI().setIntention(CtrlIntention.MOVING, loc);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getRandomEntry(T... array)
	{
		if (array.length == 0)
		{
			return null;
		}
		return array[getRandom(array.length)];
	}
	
	public static <T> T getRandomEntry(List<T> list)
	{
		if (list.isEmpty())
		{
			return null;
		}
		return list.get(getRandom(list.size()));
	}
	
	public static int getRandomEntry(int... array)
	{
		return array[getRandom(array.length)];
	}
	
	public static void main(String[] args)
	{
	}
}
