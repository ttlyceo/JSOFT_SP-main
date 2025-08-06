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

import l2e.gameserver.Config;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;

public class LostCaptainInstance extends RaidBossInstance
{
	public LostCaptainInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
		setIsRaid(true);
	}

	@Override
	protected void onDeath(Creature killer)
	{
		super.onDeath(killer);
		final ReflectionWorld r = ReflectionManager.getInstance().getWorld(getReflectionId());
		if (r != null)
		{
			if (r.getAllowed() != null)
			{
				for (final int objectId : r.getAllowed())
				{
					final Player player = GameObjectsStorage.getPlayer(objectId);
					if (player != null)
					{
						if (!Config.ALT_KAMALOKA_ESSENCE_PREMIUM_ONLY || player.hasPremiumBonus())
						{
							switch (r.getTemplateId())
							{
								case 59 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 8, player, true);
									break;
								case 62 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 9, player, true);
									break;
								case 65 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 11, player, true);
									break;
								case 68 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 13, player, true);
									break;
								case 71 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 15, player, true);
									break;
								case 73 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 5, player, true);
									break;
								case 74 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 7, player, true);
									break;
								case 75 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 8, player, true);
									break;
								case 76 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 12, player, true);
									break;
								case 77 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 15, player, true);
									break;
								case 78 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 18, player, true);
									break;
								case 79 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 18, player, true);
									break;
								case 134 :
									player.addItem("Reward by LostCaptainInstance die", 13002, 19, player, true);
									break;
							}
						}
						player.getCounters().addAchivementInfo("lostCaptainKiller", getId(), -1, false, true, false);
					}
				}
			}
		}
	}
}
