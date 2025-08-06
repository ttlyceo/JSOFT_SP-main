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
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneId;
import l2e.scripts.ai.AbstractNpcAI;

public class BirthdayCake extends AbstractNpcAI
{
	private static final int BIRTHDAY_CAKE_24 = 106;
	private static final int BIRTHDAY_CAKE = 139;

	protected BirthdayCake(String name, String descr)
	{
		super(name, descr);

		addFirstTalkId(BIRTHDAY_CAKE, BIRTHDAY_CAKE_24);
		addSpawnId(BIRTHDAY_CAKE, BIRTHDAY_CAKE_24);
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
			case BIRTHDAY_CAKE:
			{
				holder = new SkillHolder(22035, 1);
				break;
			}
			case BIRTHDAY_CAKE_24:
			{
				holder = new SkillHolder(22250, 1);
				break;
			}
			default:
			{
				return;
			}
		}

		ThreadPoolManager.getInstance().schedule(new BirthdayCakeAI(npc, holder), 1000);
	}

	protected class BirthdayCakeAI implements Runnable
	{
		private final Npc _npc;
		private final SkillHolder _holder;

		protected BirthdayCakeAI(Npc npc, SkillHolder holder)
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

			if (!_npc.isInsideZone(ZoneId.PEACE))
			{
				final Skill skill = _holder.getSkill();
				switch (_npc.getId())
				{
					case BIRTHDAY_CAKE:
					{
						for (final Player player : World.getInstance().getAroundPlayers(_npc, skill.getAffectRange(), 200))
						{
							skill.getEffects(_npc, player, false);
						}
						break;
					}
					case BIRTHDAY_CAKE_24:
					{
						final Player player = _npc.getSummoner().getActingPlayer();
						if (player == null)
						{
							ThreadPoolManager.getInstance().schedule(this, 1000);
							return;
						}

						if (!player.isInParty())
						{
							if (player.isInsideRadius(_npc, skill.getAffectRange(), true, true))
							{
								skill.getEffects(_npc, player, false);
							}
						}
						else
						{
							for (final Player member : player.getParty().getMembers())
							{
								if ((member != null) && member.isInsideRadius(_npc, skill.getAffectRange(), true, true))
								{
									skill.getEffects(_npc, member, false);
								}
							}
						}
						break;
					}
				}
			}
			ThreadPoolManager.getInstance().schedule(this, 1000);
		}
	}

	public static void main(String[] args)
	{
		new BirthdayCake(BirthdayCake.class.getSimpleName(), "ai/npc");
	}
}
