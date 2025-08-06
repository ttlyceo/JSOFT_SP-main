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
package l2e.scripts.events;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.TimeUtils;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.WorldEventParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.listener.actor.OnDeathListener;
import l2e.gameserver.listener.player.OnPlayerEnterListener;
import l2e.gameserver.listener.player.OnPlayerExitListener;
import l2e.gameserver.listener.player.PlayerListenerList;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractWorldEvent;
import l2e.gameserver.model.entity.events.model.template.WorldEventSpawn;
import l2e.gameserver.model.entity.events.model.template.WorldEventTemplate;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Updated by LordWinter 13.07.2020
 */
public class Hitman extends AbstractWorldEvent
{
	private static boolean _isActive = false;
	private WorldEventTemplate _template = null;
	private ScheduledFuture<?> _eventTask = null;
	
	private final List<Npc> _npcList = new ArrayList<>();

	private Map<Integer, PlayerToAssasinate> _targets;
	private Map<String, Integer> _currency;
	private static DecimalFormat f = new DecimalFormat(",##0,000");
	private static Integer maxPerPage = Config.HITMAN_MAX_PER_PAGE;
	private final int MIN_MAX_CLEAN_RATE = Config.HITMAN_SAVE_TARGET * 60000;
	
	private final EnterListener _enterListener = new EnterListener();
	private final ExitListener _exitListener = new ExitListener();
	private final DeathListener _deathListener = new DeathListener();

	public Hitman(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(51);
		addFirstTalkId(51);
		addTalkId(51);
		
		_template = WorldEventParser.getInstance().getEvent(10);
		if (_template != null && !_isActive)
		{
			if (_template.isNonStop())
			{
				eventStart(-1, false);
			}
			else
			{
				final long startTime = calcEventStartTime(_template, false);
				final long expireTime = calcEventStopTime(_template, false);
				if (startTime <= System.currentTimeMillis() && expireTime > System.currentTimeMillis() || (expireTime < startTime && expireTime > System.currentTimeMillis()))
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
				else
				{
					checkTimerTask(startTime);
				}
			}
		}
	}

	@Override
	public boolean isEventActive()
	{
		return _isActive;
	}

	@Override
	public WorldEventTemplate getEventTemplate()
	{
		return _template;
	}
	
	@Override
	public boolean eventStart(long totalTime, boolean force)
	{
		if (_isActive || totalTime == 0)
		{
			return false;
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		_isActive = true;
		PlayerListenerList.addGlobal(_enterListener);
		PlayerListenerList.addGlobal(_exitListener);
		PlayerListenerList.addGlobal(_deathListener);
		
		final List<WorldEventSpawn> spawnList = _template.getSpawnList();
		if (spawnList != null && !spawnList.isEmpty())
		{
			spawnList.stream().forEach(spawn -> _npcList.add(addSpawn(spawn.getNpcId(), spawn.getLocation().getX(), spawn.getLocation().getY(), spawn.getLocation().getZ(), spawn.getLocation().getHeading(), false, 0)));
		}

		_targets = load();
		loadCurrency();
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new AISystem(), MIN_MAX_CLEAN_RATE, MIN_MAX_CLEAN_RATE);
		
		final var msg = new ServerMessage("EventHitman.START", true);
		Announcements.getInstance().announceToAll(msg);
		
		if (totalTime > 0)
		{
			_eventTask = ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					eventStop();
				}
			}, totalTime);
			_log.info("Event " + _template.getName(null) + " will end in: " + TimeUtils.toSimpleFormat(System.currentTimeMillis() + totalTime));
		}
		return true;
	}

	@Override
	public boolean eventStop()
	{
		if (!_isActive)
		{
			return false;
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		_isActive = false;
		PlayerListenerList.removeGlobal(_enterListener);
		PlayerListenerList.removeGlobal(_exitListener);
		PlayerListenerList.removeGlobal(_deathListener);
		

		if (!_npcList.isEmpty())
		{
			_npcList.stream().filter(n -> n != null).forEach(n -> n.deleteMe());
		}
		_npcList.clear();
		
		final var msg = new ServerMessage("EventHitman.STOP", true);
		Announcements.getInstance().announceToAll(msg);
		
		checkTimerTask(calcEventStartTime(_template, false));
		
		return true;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final StringTokenizer st = new StringTokenizer(event, " ");
		final String currentcommand = st.nextToken();
		try
		{
			if (currentcommand.startsWith("showList"))
			{
				final int p = Integer.parseInt(st.nextToken());
				parseWindow(player, npc, showListWindow(player, npc, p));
				return null;
			}
			else if (currentcommand.startsWith("showInfo"))
			{
				final int playerId = Integer.parseInt(st.nextToken());
				final int p = Integer.parseInt(st.nextToken());
				parseWindow(player, npc, showInfoWindow(player, npc, playerId, p));
				return null;
			}
			else if (currentcommand.startsWith("showAddList"))
			{
				final var html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, player.getLang(), "data/html/scripts/events/" + getName() + "/51-1.htm");
				parseWindow(player, npc, html);
				return null;
			}
			else if (currentcommand.startsWith("addList"))
			{
				final String name = st.nextToken();
				long amount = Long.parseLong(st.nextToken());
				final int itemId = _currency.get(st.nextToken());
				if (amount <= 0)
				{
					amount = 1L;
				}
				putHitOn(player, name, amount, itemId);
				return null;
			}
			else if (currentcommand.startsWith("removeList"))
			{
				final String name = st.nextToken();
				cancelAssasination(name, player);
				htmltext = npc.getId() + ".htm";
			}
		}
		catch (final Exception e)
		{
			player.sendMessage((new ServerMessage("Hitman.MAKE_SURE", player.getLang())).toString());
			return null;
		}
		return htmltext;
	}
	
	public void parseWindow(Player player, Npc npc, NpcHtmlMessage html)
	{
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		html.replace("%npc_name%", npc.getName(player.getLang()));
		html.replace("%player_name%", player.getName(null));
		player.sendPacket(html);
	}

	public NpcHtmlMessage showAddList(Player player, Npc npc, String list)
	{
		final var html = new NpcHtmlMessage(npc.getObjectId());
		final var content = new StringBuilder();
		content.append("<html>");
		content.append("<body>");
		content.append("<center>");
		content.append("<img src=\"L2Font-e.mini_logo-e\" width=\"245\" height=\"80\">");
		content.append("<img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
		content.append("<br>" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.ORDER_TARGET") + "<br1>");
		content.append("<table width=\"256\">");
		content.append("<tr>");
		content.append("<td width=\"256\" align=\"center\">" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.NAME") + "<br1>");
		content.append("<edit var=\"name\" width=\"150\" height=\"15\">");
		content.append("</td>");
		content.append("</tr>");
		content.append("<tr>");
		content.append("<td wi dth=\"256\" align=\"center\">" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.CURRENCY") + "<br1>");
		content.append("<combobox width=\"180\" var=\"currency\" list=\"Adena;Coin_of_Luck;Golden_Apiga\">");
		content.append("</td>");
		content.append("</tr>");
		content.append("<tr>");
		content.append("<td width=\"256\" align=\"center\">" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.AMOUNT") + "<br1>");
		content.append("<edit var=\"bounty\" width=\"150\" height=\"15\">");
		content.append("</td>");
		content.append("</tr>");
		content.append("</table>");
		content.append("<br>");
		content.append("<button value=" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.ADD") + " action=\"bypass -h npc_%objectId%_addList $name $bounty $currency\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"95\" height=\"21\">");
		content.append("<br>" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.IF_DEL_TARGET") + "<br1>");
		content.append("<table width=\"240\">");
		content.append("<tr>");
		content.append("<td width=\"60\">" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.NAME") + ":</td>");
		content.append("<td><edit var=\"remname\" width=\"110\" height=\"15\"></td>");
		content.append("<td><button value=" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.DELETE") + " action=\"bypass -h npc_%objectId%_removeList $remname\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"60\" height=\"21\"></td>");
		content.append("</tr>");
		content.append("</table>");
		content.append("<br>");
		content.append("<br>");
		content.append("<button value=" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.BACK") + " action=\"bypass -h npc_%objectId%_Chat 0\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"80\" height=\"21\"><br1>");
		content.append("<img src=\"L2UI_CH3.herotower_deco\" widt h=\"256\" height=\"32\">");
		content.append("<img src=\"l2ui.bbs_lineage2\" height=\"16\" width=\"80\">");
		content.append("</center>");
		content.append("</body>");
		content.append("</html>");

		html.setHtml(player, content.toString());
		return html;
	}

	private String generateButtonPage(int page, int select)
	{
		String text = "";

		if (page == 1)
		{
			return text;
		}

		text += "<table><tr>";
		for (int i = 1; i <= page; i++)
		{
			final String v = (i == select ? String.valueOf(i) + "*" : String.valueOf(i));
			text += "<td><button value=\"P" + v + "\"" + "action=\"bypass -h npc_%objectId%_showList " + i + "\" back=\"L2UI_CT1.Button_DF_Down\"" + "fore=\"L2UI_CT1.Button_DF\" width=35 height=21></td>";
			text += ((i % 8) == 0 ? "</tr><tr>" : "");
		}
		text += "</tr></table>";
		return text;
	}

	private NpcHtmlMessage showListWindow(Player player, Npc npc, int p)
	{
		final var html = new NpcHtmlMessage(npc.getObjectId());
		final var content = new StringBuilder("<html><body><center>");

		content.append("<img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
		content.append("<br>");

		content.append("<table>");
		content.append("<tr><td align=\"center\"><font color=AAAAAA>" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.AGENCY") + "</font></td></tr>");
		content.append("<tr><td align=\"center\"><img src=\"L2UI.SquareWhite\" width=\"261\" height=\"1\"></td></tr>");
		content.append("<tr><td align=\"center\">");
		final List<PlayerToAssasinate> list = new ArrayList<>();
		list.addAll(getTargetsOnline().values());

		if (list.size() > 0)
		{
			final int countPag = (int) Math.ceil((double) list.size() / (double) maxPerPage);
			final int startRow = (maxPerPage * (p - 1));
			final int stopRow = startRow + maxPerPage;
			int countReg = 0;
			final String pages = generateButtonPage(countPag, p);

			content.append(pages);
			content.append("<table bgcolor=\"000000\">");
			content.append("<tr><td width=\"60\" align=\"center\">" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.TARGET") + "</td>");
			content.append("<td width=\"125\" align=\"center\"><font color=\"F2FEBF\">" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.REWARD") + "</font></td>");
			content.append("<td width=\"115\" align=\"center\"><font color=\"00CC00\">" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.CURRENCY") + "</font></td></tr>");

			for (final var pta : list)
			{
				if (pta == null)
				{
					break;
				}

				if (countReg >= stopRow)
				{
					break;
				}

				if ((countReg >= startRow) && (countReg < stopRow))
				{
					content.append("<tr><td align=\"center\">" + pta.getName() + "</td>");
					content.append("<td align=\"center\">" + (pta.getBounty() > 999 ? f.format(pta.getBounty()) : pta.getBounty()) + "</td>");
					content.append("<td align=\"center\">" + pta.getItemName(player) + "</td></tr>");
				}

				countReg++;
			}
			content.append("<tr><td height=\"3\"> </td><td height=\"3\"> </td><td height=\"3\"> </td></tr>");
			content.append("</table><br1>");

			content.append(pages);
		}
		else
		{
			content.append("" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.NO_TARGET") + "");
		}

		content.append("</td></tr>");
		content.append("<tr><td align=\"center\"><img src=\"L2UI.SquareWhite\" width=\"261\" height=\"1\"></td></tr>");
		content.append("</table>");

		content.append("<button value=" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.BACK") + " action=\"bypass -h npc_%objectId%_Chat 0\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"55\" height=\"21\">");
		content.append("<br><font color=\"cc9900\"><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\"></font><br1>");
		content.append("</center></body></html>");
		html.setHtml(player, content.toString());

		return html;
	}

	public NpcHtmlMessage showInfoWindow(Player player, Npc npc, int objectId, int p)
	{
		final var html = new NpcHtmlMessage(npc.getObjectId());
		final var pta = getTargets().get(objectId);
		final var target = GameObjectsStorage.getPlayer(pta.getName());
		final var content = new StringBuilder("<html><body><center>");

		content.append("<img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
		content.append("<table>");
		content.append("<tr><td align=\"center\"><font color=\"AAAAAA\">" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.TARGET") + ": " + pta.getName() + "</font></td></tr>");
		content.append("<tr><td align=\"center\"><img src=\"L2UI.SquareWhite\" width=\"261\" height=\"1\"></td></tr>");
		content.append("<tr><td align=\"center\">");

		if (target != null)
		{
			content.append("<table bgcolor=\"000000\"><tr><td>");
			content.append("" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.INFO") + ".<br>");
			content.append("<br><br>");
			content.append("" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.TARGET") + ": <font color=\"D74B18\">" + pta.getName() + "</font><br1>");
			content.append("" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.REWARD") + " <font color=\"D74B18\">" + (pta.getBounty() > 999 ? f.format(pta.getBounty()) : pta.getBounty()) + " " + pta.getItemName(player) + "</font><br1>");
			content.append("</td></tr></table>");
		}
		else
		{
			content.append("Player went offline.");
		}

		content.append("</td></tr>");
		content.append("<tr><td align=\"center\"><img src=\"L2UI.SquareWhite\" width=\"261\" height=\"1\"></td></tr>");
		content.append("</table>");
		content.append("<button value=" + ServerStorage.getInstance().getString(player.getLang(), "Hitman.BACK") + " action=\"bypass -h npc_%objectId%_showList " + p + "\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"100\" height=\"21\">");
		content.append("<br><font color=\"cc9900\"><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\"></font><br1>");
		content.append("</center></body></html>");
		html.setHtml(player, content.toString());

		return html;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}

	private void loadCurrency()
	{
		if (_currency == null)
		{
			_currency = new HashMap<>();
		}
		try
		{
			for (final int itemId : Config.HITMAN_CURRENCY)
			{
				_currency.put(getCurrencyName(itemId).trim().replaceAll(" ", "_"), itemId);
			}
		}
		catch (final Exception e)
		{
		}
	}
	
	private String getCurrencyName(Integer itemId)
	{
		return ItemsParser.getInstance().getTemplate(itemId).getName("en");
	}

	private Map<Integer, PlayerToAssasinate> load()
	{
		final Map<Integer, PlayerToAssasinate> map = new HashMap<>();

		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var st = con.prepareStatement("SELECT targetId, clientId, target_name, itemId, bounty, pending_delete FROM hitman_list");
			final var rs = st.executeQuery();
			while (rs.next())
			{
				final int targetId = rs.getInt("targetId");
				final int clientId = rs.getInt("clientId");
				final String target_name = rs.getString("target_name");
				final int itemId = rs.getInt("itemId");
				final Long bounty = rs.getLong("bounty");
				final boolean pending = rs.getInt("pending_delete") == 1;

				if (pending)
				{
					removeTarget(targetId, false);
				}
				else
				{
					map.put(targetId, new PlayerToAssasinate(targetId, clientId, itemId, bounty, target_name));
				}
			}
			_log.info("Event " + _template.getName(null) + ": Loaded - " + map.size() + " Assassination Target(s)...");
		}
		catch (final Exception e)
		{
			_log.warn("Hitman: " + e.getCause());
			return new HashMap<>();
		}
		return map;
	}

	private void rewardAssassin(Player activeChar, Player target, int itemId, Long bounty)
	{
		final var inv = activeChar.getInventory();
		SystemMessage systemMessage;

		if (ItemsParser.getInstance().createDummyItem(itemId).isStackable())
		{
			inv.addItem("Hitman", itemId, bounty, activeChar, target);
			if (bounty > 1)
			{
				systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
				systemMessage.addItemName(itemId);
				systemMessage.addItemNumber(bounty);
			}
			else
			{
				systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
				systemMessage.addItemName(itemId);
			}
			activeChar.sendPacket(systemMessage);
		}
		else
		{
			for (int i = 0; i < bounty; ++i)
			{
				inv.addItem("Hitman", itemId, 1, activeChar, target);
				systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
				systemMessage.addItemName(itemId);
				activeChar.sendPacket(systemMessage);
			}
		}
	}

	public void save()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			for (final var pta : _targets.values())
			{
				final var st = con.prepareStatement("REPLACE INTO hitman_list (targetId,clientId,target_name,itemId,bounty,pending_delete) VALUES (?,?,?,?,?,?)");
				st.setInt(1, pta.getObjectId());
				st.setInt(2, pta.getClientId());
				st.setString(3, pta.getName());
				st.setInt(4, pta.getItemId());
				st.setLong(5, pta.getBounty());
				st.setInt(6, pta.isPendingDelete() ? 1 : 0);
				st.execute();
				st.close();
			}
		}
		catch (final Exception e)
		{
			_log.warn("Hitman: " + e);
		}
	}

	public void putHitOn(Player client, String playerName, Long bounty, Integer itemId)
	{
		final var player = GameObjectsStorage.getPlayer(playerName);
		if (client.getHitmanTargets().size() >= Config.HITMAN_TARGETS_LIMIT)
		{
			client.sendMessage(new ServerMessage("Hitman.OUR_CLIENT", client.getLang()).toString());
			return;
		}
		else if (client.getInventory().getInventoryItemCount(itemId, -1) < bounty)
		{
			client.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			return;
		}
		else if ((player == null) && CharNameHolder.getInstance().doesCharNameExist(playerName))
		{
			final Integer targetId = Integer.parseInt(getOfflineData(playerName, 0)[0]);

			if (_targets.containsKey(targetId))
			{
				client.sendMessage((new ServerMessage("Hitman.ALREADY_HIT", client.getLang())).toString());
				return;
			}
			_targets.put(targetId, new PlayerToAssasinate(targetId, client.getObjectId(), itemId, bounty, playerName));
			client.destroyItemByItemId("Hitman", itemId, bounty, client, true);
			client.addHitmanTarget(targetId);
			if (Config.HITMAN_ANNOUNCE)
			{
				final ServerMessage msg = new ServerMessage("Hitman.ANNOUNCE_PAID", true);
				msg.add(client.getName(null));
				msg.add((bounty > 999 ? f.format(bounty) : bounty));
				msg.add(getCurrencyName(itemId));
				msg.add(playerName);
				Announcements.getInstance().announceToAll(msg);
			}
		}
		else if ((player != null) && CharNameHolder.getInstance().doesCharNameExist(playerName))
		{
			if (_targets.containsKey(player.getObjectId()))
			{
				client.sendMessage((new ServerMessage("Hitman.ALREADY_HIT", client.getLang())).toString());
				return;
			}
			
			if (!Config.HITMAN_TAKE_KARMA)
			{
				player.setIsWithoutCursed(true);
			}
			player.sendMessage((new ServerMessage("Hitman.HIT_YOU", client.getLang())).toString());
			_targets.put(player.getObjectId(), new PlayerToAssasinate(player, client.getObjectId(), itemId, bounty));
			client.destroyItemByItemId("Hitman", itemId, bounty, client, true);
			client.addHitmanTarget(player.getObjectId());
			if (Config.HITMAN_ANNOUNCE)
			{
				final ServerMessage msg = new ServerMessage("Hitman.ANNOUNCE_PAID", true);
				msg.add(client.getName(null));
				msg.add((bounty > 999 ? f.format(bounty) : bounty));
				msg.add(getCurrencyName(itemId));
				msg.add(playerName);
				Announcements.getInstance().announceToAll(msg);
			}
		}
		else
		{
			client.sendMessage((new ServerMessage("Hitman.NAME_INVALID", client.getLang())).toString());
		}
	}

	public class AISystem implements Runnable
	{
		@Override
		public void run()
		{
			for (final var target : _targets.values())
			{
				if (target.isPendingDelete())
				{
					removeTarget(target.getObjectId(), true);
				}
			}
			save();
		}
	}

	public void removeTarget(int obId, boolean live)
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var st = con.prepareStatement("DELETE FROM hitman_list WHERE targetId = ?");
			st.setInt(1, obId);
			st.execute();

			if (live)
			{
				_targets.remove(obId);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Hitman: " + e);
		}
	}

	public void cancelAssasination(String name, Player client)
	{
		final var target = GameObjectsStorage.getPlayer(name);
		if (client.getHitmanTargets().isEmpty())
		{
			if (!Config.HITMAN_TAKE_KARMA)
			{
				client.setIsWithoutCursed(false);
			}
			client.sendMessage((new ServerMessage("Hitman.DONT_OWN", client.getLang())).toString());
			return;
		}
		
		if (target == null)
		{
			final int tgtObjectId = CharNameHolder.getInstance().getIdByName(name);
			if (tgtObjectId > 0)
			{
				boolean found = false;
				for (final int objId : client.getHitmanTargets())
				{
					if (objId == tgtObjectId)
					{
						found = true;
						break;
					}
				}
				
				final var pta = _targets.get(tgtObjectId);
				if (!found || !_targets.containsKey(pta.getObjectId()))
				{
					client.sendMessage((new ServerMessage("Hitman.NO_HIT", client.getLang())).toString());
				}
				else if (pta.getClientId() == client.getObjectId())
				{
					client.removeHitmanTarget(pta.getObjectId());
					removeTarget(pta.getObjectId(), true);
					client.sendMessage((new ServerMessage("Hitman.CANCEL_HIT", client.getLang())).toString());
				}
				else
				{
					client.sendMessage((new ServerMessage("Hitman.NO_ACTUAL_TARGET", client.getLang())).toString());
				}
			}
			else
			{
				client.sendMessage((new ServerMessage("Hitman.NAME_INVALID", client.getLang())).toString());
			}
		}
		else
		{
			boolean found = false;
			for (final int objId : client.getHitmanTargets())
			{
				if (objId == target.getObjectId())
				{
					found = true;
					break;
				}
			}
			
			final var pta = _targets.get(target.getObjectId());
			if (!found || !_targets.containsKey(pta.getObjectId()))
			{
				client.sendMessage((new ServerMessage("Hitman.NO_HIT", client.getLang())).toString());
			}
			else if (pta.getClientId() == client.getObjectId())
			{
				client.removeHitmanTarget(pta.getObjectId());
				removeTarget(pta.getObjectId(), true);
				client.sendMessage((new ServerMessage("Hitman.CANCEL_HIT", client.getLang())).toString());
				target.sendMessage((new ServerMessage("Hitman.YOUR_HIT_CANCEL", target.getLang())).toString());
			}
			else
			{
				client.sendMessage((new ServerMessage("Hitman.NO_ACTUAL_TARGET", client.getLang())).toString());
			}
		}
	}

	public static String[] getOfflineData(String name, int objId)
	{
		final String[] set = new String[2];

		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var st = con.prepareStatement(objId > 0 ? "SELECT charId, char_name FROM characters WHERE charId = ?" : "SELECT charId, char_name FROM characters WHERE char_name = ?");

			if (objId > 0)
			{
				st.setInt(1, objId);
			}
			else
			{
				st.setString(1, name);
			}

			final var rs = st.executeQuery();
			while (rs.next())
			{
				set[0] = String.valueOf(rs.getInt("charId"));
				set[1] = rs.getString("char_name");
			}
			st.close();
			rs.close();
		}
		catch (final Exception e)
		{
			_log.warn("Hitman: " + e);
		}
		return set;
	}

	public Map<Integer, PlayerToAssasinate> getTargets()
	{
		return _targets;
	}

	public Map<Integer, PlayerToAssasinate> getTargetsOnline()
	{
		final Map<Integer, PlayerToAssasinate> online = new HashMap<>();
		for (final var objId : _targets.keySet())
		{
			final var pta = _targets.get(objId);
			if (pta.isOnline() && !pta.isPendingDelete())
			{
				online.put(objId, pta);
			}
		}
		return online;
	}

	public static class PlayerToAssasinate
	{
		private int _objectId;
		private int _clientId;
		private String _name;
		private int _itemId;
		private Long _bounty;
		private boolean _online;
		private boolean _pendingDelete;
		
		public PlayerToAssasinate(Player target, int clientId, int itemId, Long bounty)
		{
			_objectId = target.getObjectId();
			_clientId = clientId;
			_name = target.getName(null);
			_itemId = itemId;
			_bounty = bounty;
			_online = target.isOnline();
		}

		public PlayerToAssasinate(int objectId, int clientId, int itemId, Long bounty, String name)
		{
			_objectId = objectId;
			_clientId = clientId;
			_name = name;
			_itemId = itemId;
			_bounty = bounty;
			_online = false;
		}

		public void setObjectId(int objectId)
		{
			_objectId = objectId;
		}

		public int getObjectId()
		{
			return _objectId;
		}

		public void setName(String name)
		{
			_name = name;
		}

		public String getName()
		{
			return _name;
		}

		public void setItemId(int itemId)
		{
			_itemId = itemId;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public String getItemName(Player player)
		{
			return ItemsParser.getInstance().getTemplate(getItemId()).getName(player.getLang());
		}

		public void setBounty(Long vol)
		{
			_bounty = vol;
		}

		public void incBountyBy(Long vol)
		{
			_bounty += vol;
		}

		public void decBountyBy(Long vol)
		{
			_bounty -= vol;
		}

		public long getBounty()
		{
			return _bounty;
		}

		public void setOnline(boolean online)
		{
			_online = online;
		}

		public boolean isOnline()
		{
			return _online;
		}

		public void setClientId(int clientId)
		{
			_clientId = clientId;
		}

		public int getClientId()
		{
			return _clientId;
		}

		public void setPendingDelete(boolean pendingDelete)
		{
			_pendingDelete = pendingDelete;
		}

		public boolean isPendingDelete()
		{
			return _pendingDelete;
		}
	}
	
	@Override
	public void startTimerTask(long time)
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		_eventTask = ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				final long expireTime = calcEventStopTime(_template, false);
				if (expireTime > System.currentTimeMillis())
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
			}
		}, (time - System.currentTimeMillis()));
		_log.info("Event " + _template.getName(null) + " will start in: " + TimeUtils.toSimpleFormat(time));
	}
	
	@Override
	public boolean isReloaded()
	{
		if (isEventActive())
		{
			return false;
		}
		
		_template = WorldEventParser.getInstance().getEvent(10);
		if (_template != null)
		{
			if (_template.isNonStop())
			{
				eventStart(-1, false);
			}
			else
			{
				final long startTime = calcEventStartTime(_template, false);
				final long expireTime = calcEventStopTime(_template, false);
				if (startTime <= System.currentTimeMillis() && expireTime > System.currentTimeMillis() || (expireTime < startTime && expireTime > System.currentTimeMillis()))
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
				else
				{
					checkTimerTask(startTime);
				}
			}
			return true;
		}
		return false;
	}
	
	private class ExitListener implements OnPlayerExitListener
	{
		@Override
		public void onPlayerExit(Player player)
		{
			if (_targets.containsKey(player.getObjectId()))
			{
				_targets.get(player.getObjectId()).setOnline(false);
			}
		}
	}
	
	private class EnterListener implements OnPlayerEnterListener
	{
		@Override
		public void onPlayerEnter(Player player)
		{
			if (_targets.containsKey(player.getObjectId()))
			{
				_targets.get(player.getObjectId()).setOnline(true);
				if (!Config.HITMAN_TAKE_KARMA)
				{
					player.setIsWithoutCursed(true);
				}
				player.sendMessage((new ServerMessage("Hitman.ASK_MURDER", player.getLang())).toString());
			}
			
			if (!player.getHitmanTargets().isEmpty())
			{
				for (final int charId : player.getHitmanTargets())
				{
					if (!_targets.containsKey(charId))
					{
						final var msg = new ServerMessage("Hitman.TARGET_ELIMINATE", player.getLang());
						msg.add(CharNameHolder.getInstance().getNameById(charId));
						player.sendMessage(msg.toString());
						player.removeHitmanTarget(charId);
					}
					else
					{
						final var msg = new ServerMessage("Hitman.TARGET_STILL", player.getLang());
						msg.add(CharNameHolder.getInstance().getNameById(charId));
						player.sendMessage(msg.toString());
					}
				}
			}
		}
	}
	
	private class DeathListener implements OnDeathListener
	{
		@Override
		public void onDeath(Creature actor, Creature killer)
		{
			if (actor == null || killer == null)
			{
				return;
			}
			
			final var target = actor.getActingPlayer();
			final var assassin = killer.getActingPlayer();
			if (target == null || assassin == null)
			{
				return;
			}
			
			if (_targets.containsKey(target.getObjectId()))
			{
				final int assassinClan = assassin.getClanId();
				final int assassinAlly = assassin.getAllyId();
				
				if (Config.HITMAN_SAME_TEAM)
				{
					if (((assassinClan != 0) && (assassinClan == target.getClanId())) || ((assassinAlly != 0) && (assassinAlly == target.getAllyId())))
					{
						assassin.sendMessage((new ServerMessage("Hitman.CONSIDERED", assassin.getLang())).toString());
						assassin.sendMessage((new ServerMessage("Hitman.SAME_CLAN", assassin.getLang())).toString());
						return;
					}
				}
				
				final var pta = _targets.get(target.getObjectId());
				final String name = getOfflineData(null, pta.getClientId())[1];
				final var client = GameObjectsStorage.getPlayer(name);
				
				final var msg1 = new ServerMessage("Hitman.RECEIVE_REWARD", target.getLang());
				msg1.add(assassin.getName(null));
				target.sendMessage(msg1.toString());
				
				if (client != null)
				{
					final var msg = new ServerMessage("Hitman.ASSASIN_REQUEST", client.getLang());
					msg.add(target.getName(null));
					client.sendMessage(msg.toString());
					client.removeHitmanTarget(target.getObjectId());
				}
				assassin.sendMessage(new ServerMessage("Hitman.MURDER", assassin.getLang()).toString());
				rewardAssassin(assassin, target, pta.getItemId(), pta.getBounty());
				removeTarget(pta.getObjectId(), true);
			}
		}
	}

	public static void main(String[] args)
	{
		new Hitman(Hitman.class.getSimpleName(), "events");
	}
}