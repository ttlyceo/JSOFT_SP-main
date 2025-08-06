package l2e.gameserver.handler.admincommandhandlers.impl;

import java.util.Map;
import java.util.StringTokenizer;

import l2e.commons.util.GMAudit;
import l2e.commons.util.StringUtil;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class InstanceZone implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_instancezone", "admin_instancezone_clear"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final String target = (activeChar.getTarget() != null) ? activeChar.getTarget().getName(activeChar.getLang()) : "no-target";
		GMAudit.auditGMAction(activeChar.getName(null), command, target, "");
		
		if (command.startsWith("admin_instancezone_clear"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				
				st.nextToken();
				final Player player = GameObjectsStorage.getPlayer(st.nextToken());
				final int instanceId = Integer.parseInt(st.nextToken());
				final String name = ReflectionManager.getInstance().getReflectionName(activeChar, instanceId);
				ReflectionManager.getInstance().deleteReflectionTime(player, instanceId);
				activeChar.sendMessage("Instance zone " + name + " cleared for player " + player.getName(null));
				player.sendMessage("Admin cleared instance zone " + name + " for you");
				
				return true;
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Failed clearing instance time: " + e.getMessage());
				activeChar.sendMessage("Usage: //instancezone_clear <playername> [instanceId]");
				return false;
			}
		}
		else if (command.startsWith("admin_instancezone"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			command = st.nextToken();
			
			if (st.hasMoreTokens())
			{
				Player player = null;
				final String playername = st.nextToken();
				
				try
				{
					player = GameObjectsStorage.getPlayer(playername);
				}
				catch (final Exception e)
				{}
				
				if (player != null)
				{
					display(player, activeChar);
				}
				else
				{
					activeChar.sendMessage("The player " + playername + " is not online");
					activeChar.sendMessage("Usage: //instancezone [playername]");
					return false;
				}
			}
			else if (activeChar.getTarget() != null)
			{
				if (activeChar.getTarget() instanceof Player)
				{
					display((Player) activeChar.getTarget(), activeChar);
				}
			}
			else
			{
				display(activeChar, activeChar);
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void display(Player player, Player activeChar)
	{
		final Map<Integer, Long> instanceTimes = ReflectionManager.getInstance().getAllReflectionTimes(player);
		
		final StringBuilder html = StringUtil.startAppend(500 + instanceTimes.size() * 200, "<html><center><table width=260><tr>" + "<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" + "<td width=180><center>Character Instances</center></td>" + "<td width=40><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" + "</tr></table><br><font color=\"LEVEL\">Instances for ", player.getName(null), "</font><center><br>" + "<table>" + "<tr><td width=150>Name</td><td width=50>Time</td><td width=70>Action</td></tr>");
		
		for (final int id : instanceTimes.keySet())
		{
			int hours = 0;
			int minutes = 0;
			final long remainingTime = (instanceTimes.get(id) - System.currentTimeMillis()) / 1000;
			if (remainingTime > 0)
			{
				hours = (int) (remainingTime / 3600);
				minutes = (int) ((remainingTime % 3600) / 60);
			}
			
			StringUtil.append(html, "<tr><td>", ReflectionManager.getInstance().getReflectionName(activeChar, id), "</td><td>", String.valueOf(hours), ":", String.valueOf(minutes), "</td><td><button value=\"Clear\" action=\"bypass -h admin_instancezone_clear ", player.getName(null), " ", String.valueOf(id), "\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		}
		StringUtil.append(html, "</table></html>");
		
		final NpcHtmlMessage ms = new NpcHtmlMessage(1);
		ms.setHtml(activeChar, html.toString());
		
		activeChar.sendPacket(ms);
	}
}