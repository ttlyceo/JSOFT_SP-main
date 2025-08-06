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
package l2e.scripts.instances;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Util;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.SoDManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.scripts.quests._693_DefeatingDragonkinRemnants;

/**
 * Created by LordWinter 20.12.2020
 */
public class SoDMountedTroop extends AbstractReflection
{
	private class MTWorld extends ReflectionWorld
	{
		private long startTime;
		private int reflectionId;
		
		public MTWorld()
		{
		}
	}
	
	private static final int[] ENTRANCE_ROOM_DOORS =
	{
	        12240001, 12240002
	};
	
	private static final int[] _templates =
	{
	        123, 124, 125, 126,
	};
	

	public SoDMountedTroop(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32527);
		addTalkId(32527);
		addKillId(18703);
		addKillId(18784, 18785, 18786, 18787, 18788, 18789, 18790);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc, int templateId)
	{
		if (enterInstance(player, npc, new MTWorld(), templateId))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			((MTWorld) world).startTime = System.currentTimeMillis();
			((MTWorld) world).reflectionId = templateId;
		}
	}
	
	@Override
	protected void onTeleportEnter(Player player, ReflectionTemplate template, ReflectionWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
		else
		{
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
	}
	
	@Override
	protected boolean checkSoloType(Player player, Npc npc, ReflectionTemplate template)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		if (SoDManager.getInstance().isAttackStage())
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _693_DefeatingDragonkinRemnants.class.getSimpleName() + "/32527-15.htm");
			player.sendPacket(html);
			return false;
		}
		return super.checkSoloType(player, npc, template);
	}
	
	@Override
	protected boolean checkPartyType(Player player, Npc npc, ReflectionTemplate template)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		if (SoDManager.getInstance().isAttackStage())
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _693_DefeatingDragonkinRemnants.class.getSimpleName() + "/32527-15.htm");
			player.sendPacket(html);
			return false;
		}
		return super.checkPartyType(player, npc, template);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (npc.getId() == 32527 && Util.isDigit(event) && ArrayUtils.contains(_templates, Integer.valueOf(event)))
		{
			enterInstance(player, npc, Integer.valueOf(event));
			return null;
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getPlayerWorld(player);
		if (tmpworld == null || !(tmpworld instanceof MTWorld))
		{
			return super.onKill(npc, player, isSummon);
		}

		final MTWorld world = (MTWorld) tmpworld;
		if (world != null)
		{
			if (npc.getId() == 18786 && world.getStatus() == 0)
			{
				world.incStatus();
				for (final int i : ENTRANCE_ROOM_DOORS)
				{
					world.getReflection().openDoor(i);
				}
			}
			
			if (checkNpcsStatus(npc, world))
			{
				final Reflection inst = ReflectionManager.getInstance().getReflection(tmpworld.getReflectionId());
				if (inst != null)
				{
					inst.setDuration(300000);
					if (world.getAllowed() != null)
					{
						final long timeDiff = (System.currentTimeMillis() - world.startTime) / 60000L;
						for (final int playerId : world.getAllowed())
						{
							final Player pl = GameObjectsStorage.getPlayer(playerId);
							if (pl != null && pl.isOnline() && pl.getReflectionId() == world.getReflectionId())
							{
								final QuestState qst = pl.getQuestState(_693_DefeatingDragonkinRemnants.class.getSimpleName());
								if (qst != null)
								{
									qst.setCond(2, true);
									qst.set("timeDiff", String.valueOf(timeDiff));
									qst.set("reflectionId", world.reflectionId);
								}
							}
						}
					}
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	private boolean checkNpcsStatus(Npc npc, MTWorld wrld)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(wrld.getReflectionId());
		if (inst != null)
		{
			for (final Npc n : inst.getNpcs())
			{
				if (n != null && n.getReflectionId() == wrld.getReflectionId())
				{
					if (!n.isDead())
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public static void main(String[] args)
	{
		new SoDMountedTroop(SoDMountedTroop.class.getSimpleName(), "instances");
	}
}