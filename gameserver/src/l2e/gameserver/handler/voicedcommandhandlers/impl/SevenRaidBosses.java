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
package l2e.gameserver.handler.voicedcommandhandlers.impl;

import l2e.gameserver.Config;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class SevenRaidBosses implements IVoicedCommandHandler
{
	private static final String[] commands =
	{
	        "7rb"
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (!Config.ALLOW_SEVENBOSSES_COMMAND)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("7rb"))
		{
			final QuestState st = activeChar.getQuestState("_254_LegendaryTales");
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setHtml(activeChar, buildHtml(st));
			activeChar.sendPacket(html);
		}
		return true;
	}
	
	private static final String buildHtml(QuestState st)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><head>");
		sb.append("<title>7 RaidBosses Status</title>");
		sb.append("</head>");
		sb.append("<body><br>");
		sb.append("<br>Your Quest (Legendary Tales) status:<br>");
		if (st == null)
		{
			sb.append("Quest is not started yet. Please visit Glimore in dragon valley in order to start it.");
			sb.append("<br>");
		}
		else
		{
			if (st.isCond(1))
			{
				for (final Bosses boss : Bosses.class.getEnumConstants())
				{
					sb.append(boss.getName() + ": ");
					sb.append(checkMask(st, boss) ? "<font color=\"00FF00\">Killed.</font>" : "<font color=\"FF0000\">Not killed.</font>");
					sb.append("<br>");
				}
			}
			else
			{
				sb.append("Legendary Tales quest is completed.");
				sb.append("<br>");
			}
		}
		sb.append("</body></html>");
		return sb.toString();
	}
	
	private static boolean checkMask(QuestState qs, Bosses boss)
	{
		final int pos = boss.getMask();
		return ((qs.getInt("raids") & pos) == pos);
	}
	
	public static enum Bosses
	{
		EMERALD_HORN("Emerald Horn"), DUST_RIDER("Dust Rider"), BLEEDING_FLY("Bleeding Fly"), BLACK_DAGGER("Blackdagger Wing"), SHADOW_SUMMONER("Shadow Summoner"), SPIKE_SLASHER("Spike Slasher"), MUSCLE_BOMBER("Muscle Bomber");
		
		private final String name;
		private final int _mask;
		
		private Bosses(String name)
		{
			this.name = name;
			_mask = 1 << ordinal();
		}
		
		public int getMask()
		{
			return _mask;
		}
		
		public String getName()
		{
			return name;
		}
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return commands;
	}
}