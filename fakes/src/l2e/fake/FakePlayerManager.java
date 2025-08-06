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
package l2e.fake;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import l2e.commons.util.Rnd;
import l2e.fake.model.FakeSupport;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.FakeArmorParser;
import l2e.gameserver.data.parser.FakeClassesParser;
import l2e.gameserver.data.parser.FakeLocationParser;
import l2e.gameserver.data.parser.FakePassiveLocationParser;
import l2e.gameserver.data.parser.FakeSkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.FakeLocTemplate;
import l2e.gameserver.model.actor.templates.player.FakePassiveLocTemplate;
import l2e.gameserver.model.actor.templates.player.FakeTraderTemplate;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.GameClient.GameClientState;
import l2e.gameserver.network.serverpackets.PrivateStoreBuyMsg;
import l2e.gameserver.network.serverpackets.PrivateStoreSellMsg;

public class FakePlayerManager
{
	private final List<FakePlayer> _fakePlayer = new ArrayList<>();
	private Future<?> _spawnPassiveTask;
	private Future<?> _spawnActiveTask;
	private int _passiveAmount = 0;
	
	public FakePlayerManager()
	{
		if (Config.ALLOW_FAKE_PLAYERS)
		{
			FakeClassesParser.getInstance();
			FakePlayerNameManager.getInstance();
			FakeLocationParser.getInstance();
			FakePassiveLocationParser.getInstance();
			FakeArmorParser.getInstance();
			FakeSkillsParser.getInstance();
			if (Config.ALLOW_SPAWN_FAKE_PLAYERS)
			{
				FakePoolManager.getInstance().schedule(new Runnable()
				{
					@Override
					public void run()
					{
						_spawnPassiveTask = FakePoolManager.getInstance().scheduleAtFixedRate(new PassiveTask(), Config.FAKE_PASSIVE_INTERVAL, Config.FAKE_PASSIVE_INTERVAL);
						_spawnActiveTask = FakePoolManager.getInstance().scheduleAtFixedRate(new ActiveTask(), Config.FAKE_ACTIVE_INTERVAL, Config.FAKE_ACTIVE_INTERVAL);
					}
				}, Config.FAKE_SPAWN_DELAY);
			}
		}
	}
	
	public FakePlayer spawnPlayer()
	{
		final FakeLocTemplate rndLoc = FakeLocationParser.getInstance().getRandomSpawnLoc();
		if (rndLoc != null)
		{
			final FakePlayer activeChar = FakeSupport.createRandomFakePlayer(rndLoc);
			final GameClient client = new GameClient(null);
			client.setDetached(true);
			client.setActiveChar(activeChar);
			activeChar.setOnlineStatus(true, false);
			client.setLogin(activeChar.getAccountNamePlayer());
			
			if (Config.PLAYER_SPAWN_PROTECTION > 0)
			{
				activeChar.setProtection(true);
			}
			activeChar.setFakeLocation(rndLoc);
			rndLoc.setCurrentAmount(rndLoc.getCurrentAmount() + 1);
			activeChar.setX(rndLoc.getLocation().getX());
			activeChar.setY(rndLoc.getLocation().getY());
			activeChar.setZ(rndLoc.getLocation().getZ());
			client.setState(GameClientState.IN_GAME);
			activeChar.setClient(client);
			activeChar.spawnMe();
			activeChar.onPlayerEnter();
			activeChar.heal();
			activeChar.setOnlineStatus(true, true);
			activeChar.broadcastCharInfo();
			return activeChar;
		}
		return null;
	}
	
	public void respawnPassivePlayer()
	{
		final FakePlayer fakePlayer = spawnPassivePlayer();
		if (fakePlayer != null)
		{
			fakePlayer.assignDefaultAI(true);
		}
	}
	
	public void respawnPassiveTrader(List<FakeTraderTemplate> traders)
	{
		final FakePlayer fakePlayer = spawnPassiveTrader(traders);
		if (fakePlayer != null)
		{
			fakePlayer.assignDefaultTraderAI();
		}
	}
	
	public FakePlayer spawnPassiveTrader(List<FakeTraderTemplate> traders)
	{
		if (traders.size() <= 0)
		{
			return null;
		}
		
		final FakeTraderTemplate tpl = traders.get(Rnd.get(traders.size()));
		if (tpl != null)
		{
			final FakePlayer activeChar = FakeSupport.createRandomPassiveTrader(tpl);
			final GameClient client = new GameClient(null);
			client.setDetached(true);
			client.setActiveChar(activeChar);
			activeChar.setOnlineStatus(true, false);
			client.setLogin(activeChar.getAccountNamePlayer());
			
			if (Config.PLAYER_SPAWN_PROTECTION > 0)
			{
				activeChar.setProtection(true);
			}
			
			final Location loc = tpl.getLocation() != null ? tpl.getLocation() : tpl.getTerritory().getRandomLoc(false);
			activeChar.setX(loc.getX());
			activeChar.setY(loc.getY());
			activeChar.setZ(loc.getZ());
			activeChar.setHeading(loc.getHeading() > 0 ? loc.getHeading() : Rnd.get(65535));
			client.setState(GameClientState.IN_GAME);
			activeChar.setClient(client);
			activeChar.spawnMe();
			activeChar.onPlayerEnter();
			activeChar.heal();
			activeChar.setOnlineStatus(true, true);
			activeChar.broadcastCharInfo();
			traders.remove(tpl);
			ThreadPoolManager.getInstance().schedule(() ->
			{
    			if (!tpl.getAddItems().isEmpty())
    			{
    				for (final ItemHolder holder : tpl.getAddItems())
    				{
						if (holder != null)
    					{
    						final ItemInstance item = activeChar.getInventory().addItem("fakeItem", holder.getId(), holder.getCount(), activeChar, null);
    						if (item != null && holder.getEnchantLevel() > 0)
    						{
    							item.setEnchantLevel(holder.getEnchantLevel());
    						}
    					}
    				}
    			}
    			
    			if (!tpl.getTradeList().isEmpty())
    			{
    				for (final ItemHolder holder : tpl.getTradeList())
    				{
    					if (holder != null)
    					{
    						final ItemInstance item = activeChar.getInventory().getItemByItemId(holder.getId());
    						if (item != null)
    						{
    							if (tpl.getType().equals("SELL") && item.getCount() >= holder.getCount())
    							{
    								activeChar.getSellList().addItem(item.getObjectId(), holder.getCount(), (long) holder.getChance());
    							}
    							else if (tpl.getType().equals("BUY"))
    							{
									activeChar.getBuyList().addItemByItemId(item.getId(), 0, holder.getCount(), (long) holder.getChance(), null, -1, -9999, -2, 0, new int[]
									{
									        0, 0, 0, 0, 0, 0
									});
    							}
    						}
    					}
    				}
    				
					boolean canSit = false;
    				if (tpl.getType().equals("SELL"))
    				{
    					activeChar.getSellList().setTitle(tpl.getMessage());
						canSit = activeChar.getSellList().getItems().length > 0;
    				}
    				else if (tpl.getType().equals("BUY"))
    				{
    					activeChar.getBuyList().setTitle(tpl.getMessage());
						canSit = activeChar.getBuyList().getItems().length > 0;
    				}
    				
					if (canSit)
					{
						activeChar.setPrivateStoreType(tpl.getType().equals("SELL") ? Player.STORE_PRIVATE_SELL : Player.STORE_PRIVATE_BUY);
						activeChar.setIsInStoreNow(true);
						activeChar.sitDown();
						activeChar.broadcastCharInfo();
						switch (activeChar.getPrivateStoreType())
						{
							case Player.STORE_PRIVATE_SELL :
								activeChar.broadcastPacket(new PrivateStoreSellMsg(activeChar));
								break;
							case Player.STORE_PRIVATE_BUY :
								activeChar.broadcastPacket(new PrivateStoreBuyMsg(activeChar));
								break;
						}
					}
    			}
			}, Rnd.get(8000, 15000));
			return activeChar;
		}
		return null;
	}
	
	public FakePlayer spawnPassivePlayer()
	{
		final FakePassiveLocTemplate rndLoc = FakePassiveLocationParser.getInstance().getRandomSpawnLoc();
		if (rndLoc != null)
		{
			final FakePlayer activeChar = FakeSupport.createRandomPassiveFakePlayer(rndLoc);
			final GameClient client = new GameClient(null);
			client.setDetached(true);
			client.setActiveChar(activeChar);
			activeChar.setOnlineStatus(true, false);
			client.setLogin(activeChar.getAccountNamePlayer());
			
			if (Config.PLAYER_SPAWN_PROTECTION > 0)
			{
				activeChar.setProtection(true);
			}
			activeChar.setFakeTerritoryLocation(rndLoc);
			rndLoc.setCurrentAmount(rndLoc.getCurrentAmount() + 1);
			
			final Location loc = rndLoc.getTerritory().getRandomLoc(false);
			activeChar.setX(loc.getX());
			activeChar.setY(loc.getY());
			activeChar.setZ(loc.getZ());
			activeChar.setHeading(Rnd.get(65535));
			client.setState(GameClientState.IN_GAME);
			activeChar.setClient(client);
			activeChar.spawnMe();
			activeChar.onPlayerEnter();
			activeChar.heal();
			activeChar.setOnlineStatus(true, true);
			activeChar.broadcastCharInfo();
			return activeChar;
		}
		return null;
	}
	
	public FakePlayer spawnRndPlayer(FakeLocTemplate loc)
	{
		if (loc != null)
		{
			final FakePlayer activeChar = FakeSupport.createRandomFakePlayer(loc);
			final GameClient client = new GameClient(null);
			client.setDetached(true);
			client.setActiveChar(activeChar);
			activeChar.setOnlineStatus(true, false);
			client.setLogin(activeChar.getAccountNamePlayer());
			
			if (Config.PLAYER_SPAWN_PROTECTION > 0)
			{
				activeChar.setProtection(true);
			}
			activeChar.setFakeLocation(loc);
			loc.setCurrentAmount(loc.getCurrentAmount() + 1);
			final Location pos = Location.findAroundPosition(loc.getLocation().getX(), loc.getLocation().getY(), loc.getLocation().getZ(), 100, 200, ReflectionManager.DEFAULT);
			activeChar.setX(pos.getX());
			activeChar.setY(pos.getY());
			activeChar.setZ(pos.getZ());
			client.setState(GameClientState.IN_GAME);
			activeChar.setClient(client);
			activeChar.spawnMe();
			activeChar.onPlayerEnter();
			activeChar.heal();
			activeChar.setOnlineStatus(true, true);
			activeChar.broadcastCharInfo();
			return activeChar;
		}
		return null;
	}
	
	public void despawnFakePlayer(int objectId)
	{
		final Player player = GameObjectsStorage.getPlayer(objectId);
		if (player instanceof FakePlayer)
		{
			final FakePlayer fakePlayer = (FakePlayer) player;
			if (removeFakePlayers(fakePlayer))
			{
				fakePlayer.kick();
			}
		}
	}
	
	public void addFakePlayers(FakePlayer player)
	{
		_fakePlayer.add(player);
	}
	
	public boolean removeFakePlayers(FakePlayer player)
	{
		if (_fakePlayer.contains(player))
		{
			return _fakePlayer.remove(player);
		}
		return false;
	}
	
	public List<FakePlayer> getFakePlayers()
	{
		return _fakePlayer;
	}
	
	private class PassiveTask implements Runnable
	{
		@Override
		public void run()
		{
			final List<FakeTraderTemplate> traders = FakePassiveLocationParser.getInstance().getFakeTraders();
			if (traders.size() > 0)
			{
				respawnPassiveTrader(traders);
			}
			
			final List<FakePassiveLocTemplate> passive = FakePassiveLocationParser.getInstance().getPassiveLocations();
			final int checkAmount = FakePassiveLocationParser.getInstance().getTotalAmount();
			if (passive.size() > 0)
			{
				if (_passiveAmount < checkAmount)
				{
					respawnPassivePlayer();
					_passiveAmount++;
				}
				else
				{
					if (traders.size() <= 0)
					{
						stopPassiveTask();
						return;
					}
				}
			}
			else
			{
				if (traders.size() <= 0)
				{
					stopPassiveTask();
					return;
				}
			}
		}
	}
	
	private class ActiveTask implements Runnable
	{
		@Override
		public void run()
		{
			final int checkAmount = Config.FAKE_PLAYERS_AMOUNT > FakeLocationParser.getInstance().getTotalAmount() ? FakeLocationParser.getInstance().getTotalAmount() : Config.FAKE_PLAYERS_AMOUNT;
			if (getFakePlayers().size() >= checkAmount || FakeLocationParser.getInstance().getSpawnLocations().size() <= 0)
			{
				stopActiveTask();
				return;
			}
			
			final FakePlayer fakePlayer = spawnPlayer();
			if (fakePlayer != null)
			{
				fakePlayer.assignDefaultAI(false);
				addFakePlayers(fakePlayer);
			}
		}
	}
	
	private void stopPassiveTask()
	{
		if (_spawnPassiveTask != null)
		{
			_spawnPassiveTask.cancel(true);
			_spawnPassiveTask = null;
		}
	}
	
	private void stopActiveTask()
	{
		if (_spawnActiveTask != null)
		{
			_spawnActiveTask.cancel(true);
			_spawnActiveTask = null;
		}
	}
	
	public static FakePlayerManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FakePlayerManager _instance = new FakePlayerManager();
	}
}
