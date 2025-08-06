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
package l2e.gameserver.network.clientpackets.Interface;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.network.clientpackets.GameClientPacket;
import l2e.gameserver.network.serverpackets.Interface.ExDynamicPacket;

public class RequestDynamicPacket extends GameClientPacket
{
	private int _packetType = -1;
	private int _type = 0;
	private int _quetId = 0;
	protected int _farmId = 0;
	
    @Override
    protected void readImpl()
    {
    	_packetType = readC();
    	switch(_packetType)
    	{
			case 2 :
			{
				_type = readC();
				break;
			}
			case 3 :
			{
				_farmId = readD();
				break;
			}
			case 4 :
			{
				_farmId = readD();
				break;
			}
			case 10 :
    			_type = readC();
				if (_type == 1)
        		{
        			_quetId = readH();
        		}
    		break;
    	}
    }

	@Override
	protected void runImpl()
	{
		final var player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
	   	switch(_packetType)
    	{
			case 2 :
			{
				if (_type == 1)
				{
					// check valid to use
				}
				player.sendPacket(new ExDynamicPacket(_type, _type == 0 ? 0 : 43200)); // time
				break;
			}
			case 3 :
			{
				final var target = player.getTarget();
				if (target != null && target instanceof Creature && !((Creature) target).isDead())
				{
					if (GeoEngine.getInstance().canSeeTarget(player, target))
					{
						player.getAI().setIntention(CtrlIntention.ATTACK, target);
					}
					else
					{
						player.getAI().setIntention(CtrlIntention.MOVING, target.getLocation(), 0);
					}
				}
				else
				{
					final var npc = GameObjectsStorage.getNpc(_farmId);
					if (npc != null)
					{
						player.setTarget(npc);
						if (GeoEngine.getInstance().canSeeTarget(player, npc))
						{
							player.getAI().setIntention(CtrlIntention.ATTACK, npc);
						}
						else
						{
							player.getAI().setIntention(CtrlIntention.MOVING, npc.getLocation(), 0);
						}
					}
				}
				break;
			}
			case 4 :
			{
				final var skill = player.getKnownSkill(_farmId);
				if (skill != null)
				{
					if (player.isMoving() && player.getCurrentSkill() != null)
					{
						return;
					}
					
					if (player.checkDoCastConditions(skill, false))
					{
						player.useMagic(skill, true, false, false);
					}
				}
				break;
			}
			case 10 :
				if (player.getActiveDailyTasks().isEmpty())
				{
					DailyTaskManager.getInstance().addAllValidTasks(player);
				}
				
				if (_type == 1)
        		{
					final var playerTask = player.getDailyTaskTemplate(_quetId);
					if ((playerTask != null) && playerTask.isComplete() && !playerTask.isRewarded())
					{
						final var task = DailyTaskManager.getInstance().getDailyTask(_quetId);
						if (task.getRewards() != null && !task.getRewards().isEmpty())
						{
							for (final int i : task.getRewards().keySet())
							{
								player.addItem("Task Reward", i, task.getRewards().get(i), null, true);
							}
						}
						playerTask.setIsRewarded(true);
						player.updateDailyRewardStatus(playerTask);
					}
        		}
        		player.sendPacket(new ExDynamicPacket(player));
        		break;
    	}
	}
}
