package top.jsoft.jguard.commands;

import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.jsoft.jguard.manager.bans.JBanManager;
import top.jsoft.jguard.manager.bans.model.JBan;
import top.jsoft.jguard.manager.session.JClientSessionManager;
import top.jsoft.jguard.manager.session.model.HWID;
import top.jsoft.jguard.manager.session.model.JClientSession;
import top.jsoft.jguard.network.HttpPacket;
import top.jsoft.jguard.utils.Strings;
import top.jsoft.jguard.utils.log.GuardLog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @author ncs.SpawN
 * @date 06.18.17
 */
public class JGuardMenu implements IAdminCommandHandler
{
	private static final Logger LOGGER = LoggerFactory.getLogger(JGuardMenu.class);
	private static enum Commands
	{
		/*admin_sg,
		admin_guard,
		ig_ban,
		ig_unban,
		ig_find,
		ig_bans,
		ig_show,
		ig_kick_session;*/
		
		admin_jg,
		admin_jguard,
		admin_jg_ban,
		admin_jg_unban,
		admin_jg_find,
		admin_jg_bans,
		admin_jg_show,
		admin_jg_kick_session,
		admin_jg_reload_text,
		admin_jg_http,
		admin_jg_http_hide;

		public static String[] names()
		{
			Commands[] states = values();
			String[] names = new String[states.length];

			for (int i = 0; i < states.length; i++)
				names[i] = states[i].name();

			return names;
		}
	}
	/*
	private static final String[] commands =
	{
		"ig",
		"io",
		"iog",
		"ioguard",
		"guard",
		"ig_ban",
		"ig_unban",
		"ig_find",
		"ig_bans",
		"ig_show",
		"ig_kick_session",
		"ig_reload_text",
		"ig_http",
		"ig_http_hide"
	};*/
	
	@Override
	public String[] getAdminCommandList()
	{
		return Commands.names();
	}

	private static enum QueryType
	{
		player,
		hwid
	}

	@Override
	public boolean useAdminCommand(String _params, Player player)
	{

		if(player == null)
		{
			return false;
		}
		
		try
		{
			String[] params = _params.split(" ");
			//Commands command = (Commands) anEnum;
			final String command = params[0];
			//System.out.println(command);
			//final Commands command = Commands.valueOf(params[0])

			switch (command)
			{
				case ("admin_jg"):
				case ("admin_jguard"):
				{
					sendMainPage(player);
					break;
				}
				case("admin_jg_http"):
				{
					Player target = null;
					if(params.length == 3)
					{
						
						if(GameObjectsStorage.getPlayer(params[1]) != null && GameObjectsStorage.getPlayer(params[1]).isPlayer())
						{
							target = GameObjectsStorage.getPlayer(params[1]).getActingPlayer();
							target.sendMessage("Open: "+params[2]);
							player.sendMessage("SendOpen: ["+target.getName(null)+"] "+params[2]);
							target.sendPacket(new HttpPacket(params[2],true));
						}
						else
						{
							player.sendMessage("Usage: [CMD] //jg_http <player> <http>");
						}
						sendMainPage(player);
						return true;
					}
					
					if(params.length == 2)
					{
						if(player.getTarget() != null && player.getTarget().isPlayer())
						{
							target = player.getTarget().getActingPlayer();
							target.sendMessage("Open: "+params[1]);
							player.sendMessage("SendOpen: ["+target.getName(null)+"] "+params[1]);
							target.sendPacket(new HttpPacket(params[1],true));
						}
						else
						{
							player.sendMessage("Usage: [Target] and [CMD] //jg_http <http>");
						}
						sendMainPage(player);
						return true;
					}
					player.sendMessage("Usage: [CMD] //jg_http <player> <http>");
					player.sendMessage("Usage: [Target] and [CMD] //jg_http <http>");
					break;
				}
				case("admin_jg_http_hide"):
				{
					Player target = null;
					if(params.length == 2)
					{
						
						if(GameObjectsStorage.getPlayer(params[1]) != null && GameObjectsStorage.getPlayer(params[1]).isPlayer())
						{
							target = GameObjectsStorage.getPlayer(params[1]).getActingPlayer();
							target.sendMessage("Open: "+params[2]);
							player.sendMessage("SendOpen: ["+target.getName(null)+"] "+params[2]);
							target.sendPacket(new HttpPacket(params[2],false));
						}
						else
						{
							player.sendMessage("Usage: [CMD] //jg_http_hide <player> <http>");
						}
						sendMainPage(player);
						return true;
					}
					
					if(params.length == 1)
					{
						if(player.getTarget() != null && player.getTarget().isPlayer())
						{
							target = player.getTarget().getActingPlayer();
							target.sendMessage("Open: "+params[1]);
							player.sendMessage("SendOpen: ["+target.getName(null)+"] "+params[1]);
							target.sendPacket(new HttpPacket(params[1],false));
						}
						else
						{
							player.sendMessage("Usage: [Target] and [CMD] //jg_http_hide <http>");
						}
						sendMainPage(player);
						return true;
					}
					player.sendMessage("Usage: [CMD] //jg_http_hide <player> <http>");
					player.sendMessage("Usage: [Target] and [CMD] //jg_http_hide <http>");
					break;
				}
				case ("admin_jg_find"):
				{
					//if(params.length != 3)
					if(params.length < 2)
					{
						player.sendMessage("Usage: //jg_find <hwid|player> <query>");
						sendMainPage(player);
						return true;
					}

					QueryType type = QueryType.valueOf(params[1]);
					String query;

					if(params.length == 3)
					{
						query = params[2];
					}
					else
					{
						query = "";
					}

					query = query.toLowerCase();

					List<JClientSession> result = new ArrayList<JClientSession>();
					Collection<JClientSession> allSessions = JClientSessionManager.getAllSessions();

					for(JClientSession session : allSessions)
					{
						switch (type)
						{
							case hwid:
							{
								if(session.hwid().contains(query))
									result.add(session);
								break;
							}

							case player:
							{
								for(GameClient client : session.getClients())
								{
									if(client.getState() == GameClient.GameClientState.IN_GAME)
									{
										Player p = client.getActiveChar();
										if(p != null && p.getName(null).toLowerCase().contains(query))
										{
											result.add(session);
											break;
										}
									}
									else if(client.getState() == GameClient.GameClientState.AUTHED)
									{
										String p = client.getLogin() ;
										if(p != null && p.toLowerCase().contains(query))
										{
											result.add(session);
											break;
										}
									}
									/*else
									if(client.getState() == GameClientState.CONNECTED)
									{
										String p = client.getLogin() ;
										if(p != null && p.toLowerCase().contains(query))
										{
											result.add(session);
											break;
										}
									}*/
								}
								
								/*
								if(result.isEmpty())
								{
									if(session.contains(query))
										result.add(session);
								}*/
								break;
							}
						}
					}


					NpcHtmlMessage html;

					if(result.size() == 0)
					{
						html = new NpcHtmlMessage(5);
						html.setHtml(player, buildPage(JGTemplate.PageMain.load()));
						player.sendMessage("Not result.");
					}
					else if(result.size() == 1) {// если результат один - отправляемся сразу к нему
						html = new NpcHtmlMessage(5);
						html.setHtml(player, buildSessionPage(result.get(0)));
					}
					else {
						html = new NpcHtmlMessage(5);
						html.setHtml(player, buildSearchPage(query, result));
					}
					player.sendPacket(html);
					break;
				}

				case ("admin_jg_bans"):
				{
                    int page = 0;
                    String query = null;

                    if(params.length > 1)
                    {
                        try
                        {
                            page = Integer.parseInt(params[1]);

                            if(page < 0)
                                page = 0;
                        }
                        catch (Exception e){}
                    }

                    if(params.length > 2) {
						query = params[2];

						if(query.isEmpty())
							query = null;
					}

					sendBansPage(player, page, query);
					break;
				}

				case ("admin_jg_show"):
				{
					if(params.length != 2)
					{
						player.sendMessage("Usage: //jg_show <session_id>");
						sendMainPage(player);
						return true;
					}

					HWID hwid = HWID.fromString(params[1]);
					JClientSession session = JClientSessionManager.getSession(hwid);

					if(session == null)
						return false;

					NpcHtmlMessage html = new NpcHtmlMessage(5);
					html.setHtml(player, buildSessionPage(session));
					player.sendPacket(html);
					break;
				}

				case ("admin_jg_kick_session"):
				{
					if(params.length != 2)
					{
						player.sendMessage("Usage: //jg_kick_session <session_id>");
						sendMainPage(player);
						return true;
					}

					HWID hwid = HWID.fromString(params[1]);
					JClientSession session = JClientSessionManager.getSession(hwid);

					if(session == null)
						return true;

					session.disconnect();

					player.sendMessage("All players from guard session have been kicked.");
					sendMainPage(player);
					break;
				}
				
				case ("admin_jg_kick"):
				{
					break;
				}

				case ("admin_jg_ban"):
				{
					if(params.length < 3)
					{
						player.sendMessage("Usage: //jg_ban <hwid|player> <victim> [reason]");
						sendBansPage(player, 0, null);
						return true;
					}

					QueryType type = QueryType.valueOf(params[1]);

					switch (type)
					{
						case hwid:
						{
							String reason = null;
							String hwid_string = params[2];
							if(params.length > 3)
								reason = Strings.joinStrings(" ", params, 3);

							HWID hwid = HWID.fromString(hwid_string);

							if(hwid == null)
							{
								player.sendMessage(String.format("Hwid [%s] has bad format.", hwid_string));
								sendBansPage(player, 0, null);
								return true;
							}

							JBan ban = new JBan(hwid, reason, player.getName(null));
                            JBanManager.addBan(ban);

							player.sendMessage(String.format("Hwid [%s] has been banned.", hwid));

							JClientSession session = JClientSessionManager.getSession(hwid);

							if(session != null)
								session.disconnect();

							if(reason != null)
								LOGGER.info(String.format("Admin [%s] has banned HWID [%S], reason: [%s]", player.getName(null), hwid, reason));
							else
								LOGGER.info(String.format("Admin [%s] has banned HWID [%S]", player.getName(null), hwid));
							break;
						}

						case player:
						{
							String playerName = params[2];
							String reason = null;

							if(params.length > 3)
								reason = Strings.joinStrings(" ", params, 3);

							Player pc = GameObjectsStorage.getPlayer(playerName);

							if(pc == null)
							{
								player.sendMessage(String.format("Player [%s] was not found!", playerName));
								break;
							}

							JClientSession session = JClientSessionManager.getSession(pc.getClient());

							if(session == null)
							{
								String error = String.format("Error! Session for player [%s] does not exist!", playerName);
								player.sendMessage(error);
								LOGGER.info(error);
								break;
							}

							JBan ban = new JBan(session.hwid, reason, player.getName(null));
							JBanManager.addBan(ban);

							player.sendMessage(String.format("Hwid %s has been banned.", session.hwid()));

							session.disconnect();

							LOGGER.info(String.format("Admin [%s] has banned player [%s] by hwid. HWID: [%S]", player.getName(null), playerName, session.hwid()));
							break;
						}
					}

					sendBansPage(player, 0, null);
					break;
				}

				case ("admin_jg_unban"):
				{
					if(params.length != 2)
					{
						player.sendMessage("Usage: //jg_unban <hwid>");
						sendBansPage(player, 0, null);
						return true;
					}

					String hwid = params[1];

					JBanManager.removeBan(hwid);

					player.sendMessage(String.format("Hwid [%s] has been un-banned.", hwid));

					LOGGER.info(String.format("Admin [%s] has removed ban from HWID [%S]", player.getName(null), hwid));
					sendMainPage(player);
					break;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Error in JGuard menu");
			GuardLog.logException(e);
			player.sendMessage("JGuard menu has crashed! See game server logs!");
			return true;
		}

		return true;
	}

	private static final int BANS_PER_PAGE = 10;
	private static SimpleDateFormat banTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	private static SimpleDateFormat licenseTimeFormatter = new SimpleDateFormat("dd-MM-yyyy");

	private String buildBansPage(Player player, int pageNumSys, String query)
	{
		int count = 0;
		StringBuilder records = new StringBuilder();
		final String bansRecord = JGTemplate.RecordBan.load();

        if(query != null)
            query = query.toLowerCase();

		int pageNumVis = pageNumSys + 1;
        int itemsToSkip = pageNumSys * BANS_PER_PAGE;

		Collection<JBan> bans = JBanManager.getBans();

		if(query != null)
		{
			Iterator<JBan> it = bans.iterator();
			while(it.hasNext())
			{
				JBan ban = it.next();
				boolean skip = true;

				if(ban.hwid.plain.contains(query))
					skip = false;

				if(ban.comment != null && ban.comment.toLowerCase().contains(query))
					skip = false;

				String[] accouns = ban.getAccountNames();
				if(accouns.length > 0)
				{
					for(String account : accouns)
						if(account.toLowerCase().contains(query))
						{
							skip = false;
							break;
						}
				}

				if(skip)
					it.remove();
			}
		}

		for(JBan ban : bans)
		{
            if(itemsToSkip > 0 && itemsToSkip-- > 0)
                continue;

			/**
			 * %hwid% - hwid duh!
			 * %reason%
			 * %date% - ban date
			 * %gm% - admin name
			 * %end% - ban end date
			 * %accs% - account list
			 */

			String record = bansRecord;
			record = record.replaceAll("%hwid%", ban.hwid.plain);
			record = record.replaceAll("%reason%", ban.comment != null ? ban.comment : "Не указано");

			record = record.replaceAll("%date%", banTimeFormatter.format(new Date(ban.time)));
			record = record.replaceAll("%end%", ban.bannedUntil > 0 ? banTimeFormatter.format(new Date(ban.bannedUntil)) : "Навсегда");

			//String gm = "Защита";
			//String gm = "[Admin] "+player.getName(null);
			
			String gm = "[Admin]";

			if(ban.GMNAME != null && !ban.GMNAME.isEmpty())
			{
				if(ban.GMNAME.contains("[TEMPORARY]") || ban.GMNAME.contains("[DELAYED]") || ban.GMNAME.contains("[REALTIME]") || ban.GMNAME.contains("NONE"))
				{
					gm = ban.GMNAME;
				}
				else
				{
					gm += " "+ban.GMNAME;
				}
			}
			

			if(ban.gmObjId > 0)
			{
				Player p = GameObjectsStorage.getPlayer(ban.gmObjId);

				if(p == null)
					p = Player.load(ban.gmObjId);

				if(p != null)
					gm = p.getName(null);
				else
					gm = String.format("? (objId %d)", ban.gmObjId);
			}

			String banstr = Strings.joinStrings(", ", ban.getAccountNames());
			if(banstr.length() == 0)
				banstr = ban.hwid.plain;

			record = record.replaceAll("%gm%", gm);
			record = record.replaceAll("%ban%", banstr);

			records.append(record);

			if(++count == BANS_PER_PAGE)
				break;
		}

        if(records.length() == 0)
        {
            player.sendMessage("Banlist is empty!");
            return buildPage(JGTemplate.PageMain.load());
        }

        int bansSize = bans.size();

		int mod = bansSize % BANS_PER_PAGE; // остаток
        int totalPages = (bansSize - mod) / BANS_PER_PAGE; // ровное деление
		if(mod > 0) totalPages++; // остаток на новой странице

		player.sendMessage(String.format("Displaying ban page %d of %d. Total bans: %d", pageNumVis, totalPages, bansSize));

		int prev_page = 0;
		int next_page = 0;
		int cur_page = pageNumVis;
		int max_page = totalPages;

		if (pageNumSys > 0)
			prev_page = pageNumSys - 1;

		if(totalPages - 1 > pageNumSys)
			next_page = pageNumSys + 1;
		else
			next_page = totalPages - 1;

		/**
		 * %count% - num banned hwids
		 * %records% - ban records
		 * %query% - search query
		 * %page_prev%
		 * %page_next%
		 * %page_cur%
		 * %page_max%
		 */

		String page = JGTemplate.PageBans.load();
		//page = page.replace("%search%", query == null ? "" : BANS_SEARCH.replaceAll("%query%", query));

		page = page.replace("%count%", String.valueOf(bans.size()));
		page = page.replace("%records%", records);
		page = page.replace("%query%", query == null ? "" : query);
		page = page.replace("%page_prev%", String.valueOf(prev_page));
		page = page.replace("%page_next%", String.valueOf(next_page));
		page = page.replace("%page_cur%", String.valueOf(cur_page));
		page = page.replace("%page_max%", String.valueOf(max_page));


		return buildPage(page);
	}

	private String buildSearchPage(String query, List<JClientSession> results, int page)
	{
		return buildSearchPage(query,results);
	}
	
	private String buildSearchPage(String query, List<JClientSession> results)
	{
		StringBuilder sb = new StringBuilder();
		final String searchRecord = JGTemplate.RecordSearch.load();

		for(JClientSession session : results)
		{
			int i = 0;
			int max = session.getCount();
			StringBuilder players = new StringBuilder();

			for(GameClient client : session.getClients())
			{
				if(client.getActiveChar() == null || client.getActiveChar().getName(null)== null || client.getActiveChar().getName(null).equalsIgnoreCase(""))
				{
					if(client.getLogin() != null && !client.getLogin() .equalsIgnoreCase(""))
					{
						players.append("\"[ReLogin] Account: "+client.getLogin() +"\"");
					}
					else
					{
						players.append("\"NONE\"");
					}
				}
				else
				{
					players.append("\"[Online] Player: "+client.getActiveChar().getName(null)+"\"");
				}

				if(i < max)
					players.append(",");

				i++;
			}

			String record = searchRecord;
			record = record.replace("%hwid%", session.hwid());
			record = record.replace("%sid%", session.hwid());
			record = record.replace("%players%", players);
			sb.append(record);
		}

		String page = JGTemplate.PageSearch.load();

		page = page.replace("%count%", String.valueOf(results.size()));
		page = page.replace("%records%", sb);
		page = page.replace("%query%", query);

		return buildPage(page);
	}

	private String buildSessionPage(JClientSession session)
	{
		StringBuilder sb = new StringBuilder();
		final String sessionRecord = JGTemplate.RecordSession.load();
		final String sessionRecordRelog = JGTemplate.RecordSessionRelog.load();

		for(GameClient client : session.getClients())
		{
			if(client.getState() != GameClient.GameClientState.IN_GAME && client.getState() != GameClient.GameClientState.AUTHED)
				continue;

			if(client.getState() == GameClient.GameClientState.IN_GAME)
			{
				String record = sessionRecord;
				record = record.replaceAll("%player_name%", client.getActiveChar().getName(null));
				record = record.replaceAll("%acc_name%", client.getActiveChar().getAccountName());
				sb.append(record);
			}
			else if(client.getState() == GameClient.GameClientState.AUTHED)
			{
				String record = sessionRecordRelog;
				record = record.replaceAll("%player_name%", "NONE");
				record = record.replaceAll("%acc_name%", client.getLogin());
				record = record.replace("%sid%", session.hwid());
				sb.append(record);
			}
		}

		String page = JGTemplate.PageSession.load();
		page = page.replace("%online%", String.valueOf(session.getCount()));
		page = page.replaceAll("%hwid%", session.hwid());
		page = page.replace("%records%", sb);
		page = page.replace("%sid%", session.hwid());

		return buildPage(page);
	}

	private String buildPage(CharSequence content)
	{
				//String date = licenseTimeFormatter.format(new Date(IOUpdateManager.getInstance().LicenseExpiry));
				// LICENSE INFORMATION #######################################################
		 		String date_string = "";
		 		String color = "00FF00";
		 		String license = "";
		 		try
		 		{
		 			Date date;
		 			//Date date_2;
		 	        int time_stamp = (int)(System.currentTimeMillis()/1000L);
		 	        //int time_stamp_2 = Integer.parseInt(Player.Decode.License_Strings[4]);
		 	  		long millisecondsDate = time_stamp*1000L;
		 	  		//long millisecondsDate_2 = time_stamp_2*1000L;
		 	  	    date = new Date(millisecondsDate);
		 	  	    //date_2 = new Date(millisecondsDate_2);
		 	  	    
		 	  	    SimpleDateFormat formating = new SimpleDateFormat("yyyy-MM-dd");
		 	  	    
		 	  	    //String.valueOf(formating.format(date))
		 			
		 			//SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd");
		 			Date date_license_off = formating.parse(date_string);
		 			Date date_now = formating.parse(String.valueOf(formating.format(date)));
		 			
		 			//Date date_now_2 = formating.parse(String.valueOf(formating.format(date_2)));
		 			
		 			//Duration oneDay = Duration.between(date_now, date_license_off);
		 			//Duration.between(date_now.atTime(0, 0), date_license_off.atTime(0, 0)).toDays(); // another option
		 			double dayDiff = (date_license_off.getTime() - date_now.getTime()) / (1000*60*60*24);		
		 			
		 			if(dayDiff>15)
		 			{
		 				color = "00FF00";
		 			}
		 			else
		 			if(dayDiff>10)
		 			{
		 				color = "FFFF00";
		 			}
		 			else
		 			if(dayDiff>5)
		 			{
		 				color = "FF4400";
		 			}
		 			else
		 			{
		 				color = "FF0000";
		 			}

		 			license = license+"<br1><font color=\"999999\">Лицензия до</font> <font color=\"00bbFF\">"+date_string+"</font> <font color=\"999999\">истечет через:</font> <font color=\""+color+"\">"+((int)dayDiff)+" дн.</font> <font color=\"999999\">";
		 			
		 		}
				catch(Exception e)
				{
				}
				//html.replace("%LICENSE%", license);
				// LICENSE INFORMATION #######################################################
		
		
		//String date =  "";
		return JGTemplate.Main.load().replace("%content%", content).replace("%expire_time%", license);
		//return IoTemplate.Main.load().replace("%content%", content);
	}

	private void sendMainPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(5);
		html.setHtml(player, buildPage(JGTemplate.PageMain.load()));
		player.sendPacket(html);
	}

	private void sendBansPage(Player player, int page, String query)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(5);
		html.setHtml(player, buildBansPage(player, page, query));
		player.sendPacket(html);
	}

	private static enum JGTemplate
	{
		Main(null, "index.tpl", false),
		PageBans("%content%", "page_bans.tpl", false),
		PageMain("%content%", "page_main.tpl", false),
		PageSearch("%content%", "page_search.tpl", false),
		PageSession("%content%", "page_session.tpl", false),
		RecordBan("%records%", "record_ban.tpl", true),
		RecordSearch("%records%", "record_search.tpl", true),
		RecordSession("%records%", "record_session.tpl", true),
		RecordSessionRelog("%records%", "record_session_relog.tpl", true);;

		JGTemplate(String tag, String name, boolean isRecord) {
			this.tag = tag;
			this.name = name;
			this.isRecord = isRecord;
		}

		public String tag;
		public String name;
		public boolean isRecord;

		public String load()
		{
			return loadTemplate(name);
		}
	}

	private static String loadTemplate(String templateName)
	{
		return loadTemplate(new File("./config/jsoft/jguard/html/", templateName));
	}

	private static String loadTemplate(File file)
	{
		if(file == null || !file.exists())
			return null;

		TplFilter tf = new TplFilter();
		if(!tf.accept(file))
			return null;

		FileInputStream fis = null;
		BufferedInputStream bis = null;

		String content = null;
		try {
			fis = new FileInputStream(file);
			bis = new BufferedInputStream(fis);
			int e1 = bis.available();
			byte[] raw = new byte[e1];
			bis.read(raw);
			content = new String(raw, "UTF-8");
			content = content.replaceAll("\r\n", "\n");
		}
		catch (Exception e)
		{
			GuardLog.logException(e);
			return null;
		}
		finally
		{
			try { if(fis != null) { fis.close(); }
			} catch (Exception e) {}

			try { if(bis != null) { bis.close(); }
			} catch (Exception e) { }
		}

		return content;
	}

	static class TplFilter implements FileFilter
	{
		public boolean accept(File file) {
			return !file.isDirectory() && file.getName().endsWith(".tpl");
		}
	}



}
