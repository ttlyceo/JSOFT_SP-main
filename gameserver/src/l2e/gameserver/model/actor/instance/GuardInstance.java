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

import java.util.List;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.World;
import l2e.gameserver.model.WorldRegion;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.network.serverpackets.SocialAction;

public class GuardInstance extends Attackable
{
	public GuardInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.GuardInstance);
	}

	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		if (attacker.isMonster())
		{
			return true;
		}
		return super.isAutoAttackable(attacker, isPoleAttack);
	}

	@Override
	public void returnHome()
	{
		if (!isInsideRadius(getSpawn().getX(), getSpawn().getY(), 150, false))
		{
			clearAggroList(true);
			getAI().setIntention(CtrlIntention.MOVING, getSpawn().getLocation(), 0);
		}
	}

	@Override
	public void onSpawn()
	{
		setIsNoRndWalk(true);
		super.onSpawn();
		
		if (getId() == 30332 || getId() == 30071 || getId() == 30916 || getId() == 30196 || getId() == 31981 || getId() == 31340)
		{
			setIsInvul(true);
			disableCoreAI(true);
		}

		if (isGlobalAI())
		{
			if (getAI().getIntention() == CtrlIntention.IDLE)
			{
				getAI().setIntention(CtrlIntention.ACTIVE, null);
			}
		}
		else
		{
			final WorldRegion region = World.getInstance().getRegion(getX(), getY(), getZ());
			if ((region != null) && (!region.isActive()) && !isGlobalAI())
			{
				((DefaultAI) getAI()).stopAITask();
			}
		}
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		return "data/html/guard/" + pom + ".htm";
	}

	@Override
	public void onAction(Player player, boolean interact, boolean shift)
	{
		if (!canTarget(player))
		{
			return;
		}

		if (getObjectId() != player.getTargetId())
		{
			player.setTarget(this);
		}
		else if (interact)
		{
			if (containsTarget(player))
			{
				player.getAI().setIntention(CtrlIntention.ATTACK, this);
			}
			else
			{
				if (!canInteract(player))
				{
					player.getAI().setIntention(CtrlIntention.INTERACT, this);
				}
				else
				{
					broadcastPacket(new SocialAction(getObjectId(), Rnd.nextInt(8)));
					player.setLastFolkNPC(this);
					
					final List<Quest> qlsa = getTemplate().getEventQuests(QuestEventType.QUEST_START);
					final List<Quest> qlst = getTemplate().getEventQuests(QuestEventType.ON_FIRST_TALK);

					if ((qlsa != null) && !qlsa.isEmpty())
					{
						player.setLastQuestNpcObject(getObjectId());
					}

					if ((qlst != null) && (qlst.size() == 1))
					{
						qlst.get(0).notifyFirstTalk(this, player);
					}
					else
					{
						showChatWindow(player, 0);
					}
				}
			}
		}
		player.sendActionFailed();
	}
}