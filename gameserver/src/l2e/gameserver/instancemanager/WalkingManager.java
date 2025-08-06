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
package l2e.gameserver.instancemanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.util.Util;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.instancemanager.tasks.StartMovingTask;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.WalkInfo;
import l2e.gameserver.model.WalkRoute;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.tasks.npc.walker.ArrivedTask;
import l2e.gameserver.model.actor.templates.NpcWalkerTemplate;
import l2e.gameserver.model.holders.NpcRoutesHolder;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

public final class WalkingManager extends DocumentParser
{
	public static final byte REPEAT_GO_BACK = 0;
	public static final byte REPEAT_GO_FIRST = 1;
	public static final byte REPEAT_TELE_FIRST = 2;
	public static final byte REPEAT_RANDOM = 3;

	private final Map<String, WalkRoute> _routes = new HashMap<>();
	private final Map<Integer, WalkInfo> _activeRoutes = new HashMap<>();
	private final Map<Integer, NpcRoutesHolder> _routesToAttach = new HashMap<>();
	
	protected WalkingManager()
	{
		load();
	}
	
	@Override
	public final void load()
	{
		parseDatapackFile("data/stats/npcs/routes.xml");
		info("Loaded " + _routes.size() + " walking routes.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		final Node n = getCurrentDocument().getFirstChild();
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (d.getNodeName().equals("route"))
			{
				final String routeName = parseString(d.getAttributes(), "name");
				final boolean repeat = parseBoolean(d.getAttributes(), "repeat");
				final String repeatStyle = d.getAttributes().getNamedItem("repeatStyle").getNodeValue();
				byte repeatType;
				if (repeatStyle.equalsIgnoreCase("back"))
				{
					repeatType = REPEAT_GO_BACK;
				}
				else if (repeatStyle.equalsIgnoreCase("cycle"))
				{
					repeatType = REPEAT_GO_FIRST;
				}
				else if (repeatStyle.equalsIgnoreCase("conveyor"))
				{
					repeatType = REPEAT_TELE_FIRST;
				}
				else if (repeatStyle.equalsIgnoreCase("random"))
				{
					repeatType = REPEAT_RANDOM;
				}
				else
				{
					repeatType = -1;
				}

				final List<NpcWalkerTemplate> list = new ArrayList<>();
				for (Node r = d.getFirstChild(); r != null; r = r.getNextSibling())
				{
					if (r.getNodeName().equals("point"))
					{
						final NamedNodeMap attrs = r.getAttributes();
						final int x = parseInt(attrs, "X");
						final int y = parseInt(attrs, "Y");
						final int z = parseInt(attrs, "Z");
						final int delay = parseInt(attrs, "delay");

						String chatString = null;
						NpcStringId npcString = null;
						Node node = attrs.getNamedItem("string");
						if (node != null)
						{
							chatString = node.getNodeValue();
						}
						else
						{
							node = attrs.getNamedItem("npcString");
							if (node != null)
							{
								npcString = NpcStringId.getNpcStringId(node.getNodeValue());
								if (npcString == null)
								{
									warn("Unknown npcstring '" + node.getNodeValue() + ".");
									continue;
								}
							}
							else
							{
								node = attrs.getNamedItem("npcStringId");
								if (node != null)
								{
									npcString = NpcStringId.getNpcStringId(Integer.parseInt(node.getNodeValue()));
									if (npcString == null)
									{
										warn("Unknown npcstring '" + node.getNodeValue() + ".");
										continue;
									}
								}
							}
						}
						list.add(new NpcWalkerTemplate(0, npcString, chatString, x, y, z, delay, parseBoolean(attrs, "run")));
					}
					else if (r.getNodeName().equals("target"))
					{
						final NamedNodeMap attrs = r.getAttributes();
						try
						{
							final int npcId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							int x = 0, y = 0, z = 0;

							x = Integer.parseInt(attrs.getNamedItem("spawnX").getNodeValue());
							y = Integer.parseInt(attrs.getNamedItem("spawnY").getNodeValue());
							z = Integer.parseInt(attrs.getNamedItem("spawnZ").getNodeValue());

							final NpcRoutesHolder holder = _routesToAttach.containsKey(npcId) ? _routesToAttach.get(npcId) : new NpcRoutesHolder();
							holder.addRoute(routeName, new Location(x, y, z));
							_routesToAttach.put(npcId, holder);
						}
						catch (final Exception e)
						{
							warn("Error in target definition for route : " + routeName);
						}
					}
				}
				_routes.put(routeName, new WalkRoute(routeName, list, repeat, false, repeatType));
			}
		}
	}

	public boolean isOnWalk(Npc npc)
	{
		final Npc master = npc.getLeader() != null ? npc.getLeader() : npc;
		if (!isRegistered(master))
		{
			return false;
		}

		final WalkInfo walk = _activeRoutes.get(master.getObjectId());
		if (walk.isStoppedByAttack() || walk.isSuspended())
		{
			return false;
		}
		return true;
	}

	public WalkRoute getRoute(String route)
	{
		return _routes.get(route);
	}

	public boolean isRegistered(Npc npc)
	{
		return _activeRoutes.containsKey(npc.getObjectId());
	}

	public String getRouteName(Npc npc)
	{
		return _activeRoutes.containsKey(npc.getObjectId()) ? _activeRoutes.get(npc.getObjectId()).getRoute().getName() : "";
	}

	public void startMoving(final Npc npc, final String routeName)
	{
		if (_routes.containsKey(routeName) && (npc != null) && !npc.isDead())
		{
			if (!_activeRoutes.containsKey(npc.getObjectId()))
			{
				if ((npc.getAI().getIntention() == CtrlIntention.ACTIVE) || (npc.getAI().getIntention() == CtrlIntention.IDLE))
				{
					final WalkInfo walk = new WalkInfo(routeName);

					if (npc.isDebug())
					{
						walk.setLastAction(System.currentTimeMillis());
					}

					NpcWalkerTemplate node = walk.getCurrentNode();

					if (Util.checkIfInRange(40, node.getMoveX(), node.getMoveY(), node.getMoveZ(), npc, false))
					{
						walk.calculateNextNode(npc);
						node = walk.getCurrentNode();
					}

					npc.setIsRunning(node.getRunning());
					npc.getAI().setIntention(CtrlIntention.MOVING, new Location(node.getMoveX(), node.getMoveY(), node.getMoveZ(), 0), 0);
					walk.setWalkCheckTask(ThreadPoolManager.getInstance().scheduleAtFixedRate(new StartMovingTask(npc, routeName), 60000, 60000));
					_activeRoutes.put(npc.getObjectId(), walk);
				}
				else
				{
					ThreadPoolManager.getInstance().schedule(new StartMovingTask(npc, routeName), 60000);
				}
			}
			else
			{
				if (_activeRoutes.containsKey(npc.getObjectId()) && ((npc.getAI().getIntention() == CtrlIntention.ACTIVE) || (npc.getAI().getIntention() == CtrlIntention.IDLE)))
				{
					final WalkInfo walk = _activeRoutes.get(npc.getObjectId());

					if (walk == null)
					{
						return;
					}

					if (walk.isBlocked() || walk.isSuspended())
					{
						return;
					}

					walk.setBlocked(true);
					final NpcWalkerTemplate node = walk.getCurrentNode();
					npc.setIsRunning(node.getRunning());
					npc.getAI().setIntention(CtrlIntention.MOVING, new Location(node.getMoveX(), node.getMoveY(), node.getMoveZ(), 0), 0);
					walk.setBlocked(false);
					walk.setStoppedByAttack(false);
				}
			}
		}
	}

	public synchronized void cancelMoving(Npc npc)
	{
		if (_activeRoutes.containsKey(npc.getObjectId()))
		{
			final WalkInfo walk = _activeRoutes.remove(npc.getObjectId());
			walk.getWalkCheckTask().cancel(true);
		}
	}

	public void resumeMoving(final Npc npc)
	{
		if (!_activeRoutes.containsKey(npc.getObjectId()))
		{
			return;
		}

		final WalkInfo walk = _activeRoutes.get(npc.getObjectId());
		walk.setSuspended(false);
		walk.setStoppedByAttack(false);
		startMoving(npc, walk.getRoute().getName());
	}

	public void stopMoving(Npc npc, boolean suspend, boolean stoppedByAttack)
	{
		final Npc master = npc.getLeader() != null ? npc.getLeader() : npc;
		if (!isRegistered(master))
		{
			return;
		}

		final WalkInfo walk = _activeRoutes.get(master.getObjectId());
		walk.setSuspended(suspend);
		walk.setStoppedByAttack(stoppedByAttack);

		master.stopMove(null);
		master.getAI().setIntention(CtrlIntention.ACTIVE);
	}

	public void onArrived(final Npc npc)
	{
		if (_activeRoutes.containsKey(npc.getObjectId()))
		{
			if (npc.getTemplate().getEventQuests(QuestEventType.ON_NODE_ARRIVED) != null)
			{
				for (final Quest quest : npc.getTemplate().getEventQuests(QuestEventType.ON_NODE_ARRIVED))
				{
					quest.notifyNodeArrived(npc);
				}
			}
			final WalkInfo walk = _activeRoutes.get(npc.getObjectId());

			if ((walk.getCurrentNodeId() >= 0) && (walk.getCurrentNodeId() < walk.getRoute().getNodesCount()))
			{
				final NpcWalkerTemplate node = walk.getRoute().getNodeList().get(walk.getCurrentNodeId());
				if (npc.isInsideRadius(node.getMoveX(), node.getMoveY(), 10, false))
				{
					walk.calculateNextNode(npc);
					final int delay = node.getDelay();
					walk.setBlocked(true);

					if (node.getNpcString() != null)
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc, Say2.NPC_ALL, node.getNpcString()));
					}
					else
					{
						final String text = node.getChatText();
						if ((text != null) && !text.isEmpty())
						{
							npc.broadcastPacketToOthers(2000, new NpcSay(npc, Say2.NPC_ALL, text));
						}
					}

					if (npc.isDebug())
					{
						walk.setLastAction(System.currentTimeMillis());
					}
					ThreadPoolManager.getInstance().schedule(new ArrivedTask(npc, walk), 100 + (delay * 1000L));
				}
			}
		}
	}

	public void onDeath(Npc npc)
	{
		cancelMoving(npc);
	}

	public void onSpawn(Npc npc)
	{
		if (_routesToAttach.containsKey(npc.getId()))
		{
			final String routeName = _routesToAttach.get(npc.getId()).getRouteName(npc);
			if (!routeName.isEmpty())
			{
				startMoving(npc, routeName);
			}
		}
	}

	public static final WalkingManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final WalkingManager _instance = new WalkingManager();
	}
}