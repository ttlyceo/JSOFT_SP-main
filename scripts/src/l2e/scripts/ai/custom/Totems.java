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
package l2e.scripts.ai.custom;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.Spawner;
import l2e.scripts.ai.AbstractNpcAI;

public class Totems extends AbstractNpcAI
{
	private static final int TOTEM_OF_BODY = 143;
	private static final int TOTEM_OF_SPIRIT = 144;
	private static final int TOTEM_OF_BRAVERY = 145;
	private static final int TOTEM_OF_FORTITUDE = 146;

	private Totems(String name, String descr)
	{
		super(name, descr);

		addFirstTalkId(TOTEM_OF_BODY, TOTEM_OF_SPIRIT, TOTEM_OF_BRAVERY, TOTEM_OF_FORTITUDE);
		addSpawnId(TOTEM_OF_BODY, TOTEM_OF_SPIRIT, TOTEM_OF_BRAVERY, TOTEM_OF_FORTITUDE);
		findNpcs();
	}
	
	private void findNpcs()
	{
		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn != null && spawn.getLastSpawn() != null && (spawn.getLastSpawn().getId() >= 143 && spawn.getLastSpawn().getId() <= 146))
			{
				addTask(spawn.getLastSpawn());
			}
		}
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return null;
	}

	@Override
	public String onSpawn(Npc npc)
	{
		addTask(npc);
		return super.onSpawn(npc);
	}

	private void addTask(Npc npc)
	{
		final SkillHolder holder;
		switch (npc.getId())
		{
			case TOTEM_OF_BODY:
			{
				holder = new SkillHolder(23308, 1);
				break;
			}
			case TOTEM_OF_SPIRIT:
			{
				holder = new SkillHolder(23309, 1);
				break;
			}
			case TOTEM_OF_BRAVERY:
			{
				holder = new SkillHolder(23310, 1);
				break;
			}
			case TOTEM_OF_FORTITUDE:
			{
				holder = new SkillHolder(23311, 1);
				break;
			}
			default:
			{
				return;
			}
		}
		ThreadPoolManager.getInstance().schedule(new TotemAI(npc, holder), 1000);
	}

	private class TotemAI implements Runnable
	{
		private final Npc _npc;
		private final SkillHolder _holder;

		protected TotemAI(Npc npc, SkillHolder holder)
		{
			_npc = npc;
			_holder = holder;
		}

		@Override
		public void run()
		{
			if ((_npc == null) || !_npc.isVisible() || (_holder == null) || (_holder.getSkill() == null))
			{
				return;
			}

			final Skill skill = _holder.getSkill();
			for (final Player player : World.getInstance().getAroundPlayers(_npc, skill.getAffectRange(), 400))
			{
				if (player.getFirstEffect(skill.getId()) == null)
				{
					skill.getEffects(player, player, false);
				}
			}
			ThreadPoolManager.getInstance().schedule(this, 1000);
		}
	}

	public static void main(String[] args)
	{
		new Totems(Totems.class.getSimpleName(), "ai");
	}
}
