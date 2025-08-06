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
import java.util.concurrent.Future;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.instancemanager.FourSepulchersManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SocialAction;

public class SepulcherNpcInstance extends NpcInstance
{
	protected Future<?> _closeTask = null;
	protected Future<?> _spawnNextMysteriousBoxTask = null;
	protected Future<?> _spawnMonsterTask = null;
	
	private static final String HTML_FILE_PATH = "data/html/SepulcherNpc/";
	private static final int HALLS_KEY = 7260;
	
	public SepulcherNpcInstance(int objectID, NpcTemplate template)
	{
		super(objectID, template);
		setInstanceType(InstanceType.SepulcherNpcInstance);
		setShowSummonAnimation(true);
		
		if (_closeTask != null)
		{
			_closeTask.cancel(true);
		}
		if (_spawnNextMysteriousBoxTask != null)
		{
			_spawnNextMysteriousBoxTask.cancel(true);
		}
		if (_spawnMonsterTask != null)
		{
			_spawnMonsterTask.cancel(true);
		}
		_closeTask = null;
		_spawnNextMysteriousBoxTask = null;
		_spawnMonsterTask = null;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		setShowSummonAnimation(false);
	}
	
	@Override
	protected void onDelete()
	{
		if (_closeTask != null)
		{
			_closeTask.cancel(true);
			_closeTask = null;
		}
		if (_spawnNextMysteriousBoxTask != null)
		{
			_spawnNextMysteriousBoxTask.cancel(true);
			_spawnNextMysteriousBoxTask = null;
		}
		if (_spawnMonsterTask != null)
		{
			_spawnMonsterTask.cancel(true);
			_spawnMonsterTask = null;
		}
		super.onDelete();
	}
	
	@Override
	public void onAction(Player player, boolean interact, boolean shift)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			if (Config.DEBUG)
			{
				_log.info("new target selected:" + getObjectId());
			}
			player.setTarget(this);
		}
		else if (interact)
		{
			if (isAutoAttackable(player, false) && !isAlikeDead())
			{
				if (Math.abs(player.getZ() - getZ()) < 400)
				{
					player.getAI().setIntention(CtrlIntention.ATTACK, this);
				}
				else
				{
					player.sendActionFailed();
				}
			}
			
			if (!isAutoAttackable(player, false))
			{
				if (!canInteract(player))
				{
					player.getAI().setIntention(CtrlIntention.INTERACT, this);
				}
				else
				{
					final SocialAction sa = new SocialAction(getObjectId(), Rnd.get(8));
					broadcastPacket(sa);
					
					doAction(player);
				}
			}
			player.sendActionFailed();
		}
	}
	
	private void doAction(Player player)
	{
		if (isDead())
		{
			player.sendActionFailed();
			return;
		}
		
		switch (getId())
		{
			case 31468 :
			case 31469 :
			case 31470 :
			case 31471 :
			case 31472 :
			case 31473 :
			case 31474 :
			case 31475 :
			case 31476 :
			case 31477 :
			case 31478 :
			case 31479 :
			case 31480 :
			case 31481 :
			case 31482 :
			case 31483 :
			case 31484 :
			case 31485 :
			case 31486 :
			case 31487 :
				setIsInvul(false);
				doDie(player);
				if (_spawnMonsterTask != null)
				{
					_spawnMonsterTask.cancel(true);
				}
				_spawnMonsterTask = ThreadPoolManager.getInstance().schedule(new SpawnMonster(getId()), 3500);
				break;
			
			case 31455 :
			case 31456 :
			case 31457 :
			case 31458 :
			case 31459 :
			case 31460 :
			case 31461 :
			case 31462 :
			case 31463 :
			case 31464 :
			case 31465 :
			case 31466 :
			case 31467 :
				setIsInvul(false);
				doDie(player);
				if ((player.getParty() != null) && !player.getParty().isLeader(player))
				{
					player = player.getParty().getLeader();
				}
				player.addItem("Quest", HALLS_KEY, 1, player, true);
				break;
			
			default :
			{
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
		player.sendActionFailed();
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
		
		return HTML_FILE_PATH + pom + ".htm";
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		final String filename = getHtmlPath(getId(), val);
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		player.sendActionFailed();
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (isBusy())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, player.getLang(), "data/html/npcbusy.htm");
			html.replace("%busymessage%", getBusyMessage());
			html.replace("%npcname%", getName(player.getLang()));
			html.replace("%playername%", player.getName(null));
			player.sendPacket(html);
		}
		else if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (final IndexOutOfBoundsException ioobe)
			{}
			catch (final NumberFormatException nfe)
			{}
			showChatWindow(player, val);
		}
		else if (command.startsWith("open_gate"))
		{
			final ItemInstance hallsKey = player.getInventory().getItemByItemId(HALLS_KEY);
			if (hallsKey == null)
			{
				showHtmlFile(player, "Gatekeeper-no.htm");
			}
			else if (FourSepulchersManager.getInstance().isAttackTime())
			{
				switch (getId())
				{
					case 31929 :
					case 31934 :
					case 31939 :
					case 31944 :
						FourSepulchersManager.getInstance().spawnShadow(getId());
					default :
					{
						openNextDoor(getId());
						if (player.getParty() != null)
						{
							for (final Player mem : player.getParty().getMembers())
							{
								if ((mem != null) && (mem.getInventory().getItemByItemId(HALLS_KEY) != null))
								{
									mem.destroyItemByItemId("Quest", HALLS_KEY, mem.getInventory().getItemByItemId(HALLS_KEY).getCount(), mem, true);
								}
							}
						}
						else
						{
							player.destroyItemByItemId("Quest", HALLS_KEY, hallsKey.getCount(), player, true);
						}
					}
				}
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public void openNextDoor(int npcId)
	{
		final int doorId = FourSepulchersManager.getInstance().getHallGateKeepers().get(npcId);
		final DoorParser _DoorParser = DoorParser.getInstance();
		_DoorParser.getDoor(doorId).openMe();
		
		if (_closeTask != null)
		{
			_closeTask.cancel(true);
		}
		_closeTask = ThreadPoolManager.getInstance().schedule(new CloseNextDoor(doorId), 10000);
		if (_spawnNextMysteriousBoxTask != null)
		{
			_spawnNextMysteriousBoxTask.cancel(true);
		}
		_spawnNextMysteriousBoxTask = ThreadPoolManager.getInstance().schedule(new SpawnNextMysteriousBox(npcId), 0);
	}
	
	private static class CloseNextDoor implements Runnable
	{
		final DoorParser _DoorParser = DoorParser.getInstance();
		
		private final int _DoorId;
		
		public CloseNextDoor(int doorId)
		{
			_DoorId = doorId;
		}
		
		@Override
		public void run()
		{
			try
			{
				_DoorParser.getDoor(_DoorId).closeMe();
			}
			catch (final Exception e)
			{
				_log.warn(e.getMessage());
			}
		}
	}
	
	private static class SpawnNextMysteriousBox implements Runnable
	{
		private final int _NpcId;
		
		public SpawnNextMysteriousBox(int npcId)
		{
			_NpcId = npcId;
		}
		
		@Override
		public void run()
		{
			FourSepulchersManager.getInstance().spawnMysteriousBox(_NpcId);
		}
	}
	
	private static class SpawnMonster implements Runnable
	{
		private final int _NpcId;
		
		public SpawnMonster(int npcId)
		{
			_NpcId = npcId;
		}
		
		@Override
		public void run()
		{
			FourSepulchersManager.getInstance().spawnMonster(_NpcId);
		}
	}
	
	public void sayInShout(NpcStringId msg, int time)
	{
		if (msg == null)
		{
			return;
		}
		
		if (msg.getId() == 1000456)
		{
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (player != null)
				{
					if (Util.checkIfInRange(15000, player, this, true))
					{
						final ServerMessage msgs = new ServerMessage("SepulcherNpc.MIN_PASSED", player.getLang());
						msgs.add(time);
						player.sendPacket(new CreatureSay(0, Say2.NPC_SHOUT, getName(player.getLang()), msgs.toString()));
					}
				}
			}
		}
		else
		{
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (player != null)
				{
					if (Util.checkIfInRange(15000, player, this, true))
					{
						player.sendPacket(new CreatureSay(0, Say2.NPC_SHOUT, getName(player.getLang()), msg));
					}
				}
			}
		}
	}
	
	public void showHtmlFile(Player player, String file)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), "data/html/SepulcherNpc/" + file);
		html.replace("%npcname%", getName(player.getLang()));
		player.sendPacket(html);
	}
}