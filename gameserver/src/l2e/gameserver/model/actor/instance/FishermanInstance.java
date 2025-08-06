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

import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.games.FishingChampionship;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.AcquireSkillDone;
import l2e.gameserver.network.serverpackets.ExAcquirableSkillListByClass;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class FishermanInstance extends MerchantInstance
{
	public FishermanInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
		setInstanceType(InstanceType.FishermanInstance);
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
		
		return "data/html/fisherman/" + pom + ".htm";
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.equalsIgnoreCase("FishSkillList"))
		{
			showFishSkillList(player);
		}
		else if (command.startsWith("FishingChampionship"))
		{
			showChampScreen(player);
		}
		else if (command.startsWith("FishingReward"))
		{
			FishingChampionship.getInstance().getReward(player);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public static void showFishSkillList(Player player)
	{
		final List<SkillLearn> skills = SkillTreesParser.getInstance().getAvailableFishingSkills(player);
		final ExAcquirableSkillListByClass asl = new ExAcquirableSkillListByClass(AcquireSkillType.FISHING);
		
		int count = 0;
		
		for (final SkillLearn s : skills)
		{
			final Skill sk = SkillsParser.getInstance().getInfo(s.getId(), s.getLvl());
			
			if (sk == null)
			{
				continue;
			}
			count++;
			asl.addSkill(s.getId(), s.getGetLevel(), s.getLvl(), s.getLvl(), s.getLevelUpSp(), 1);
		}
		
		if (count == 0)
		{
			final int minlLevel = SkillTreesParser.getInstance().getMinLevelForNewSkill(player, SkillTreesParser.getInstance().getFishingSkillTree());
			
			if (minlLevel > 0)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);
				sm.addNumber(minlLevel);
				player.sendPacket(sm);
			}
			else
			{
				player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
			}
			player.sendPacket(AcquireSkillDone.STATIC);
		}
		else
		{
			player.sendPacket(asl);
		}
	}
	
	public void showChampScreen(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		String str = "<html><head><title>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ROYAL_TOURNAMENT") + "</title></head>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "L2FishermanInstance.GUILD_OF_FISHERS") + ":<br><br>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "L2FishermanInstance.HELLO") + "<br>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "L2FishermanInstance.YOUR_NAME_IN_LIST") + "<br>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "L2FishermanInstance.REMEMBER") + "<br>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "L2FishermanInstance.BE_NOT_UPSET") + "<br>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "L2FishermanInstance.MESSAGE") + " " + FishingChampionship.getInstance().getTimeRemaining() + " " + ServerStorage.getInstance().getString(player.getLang(), "L2FishermanInstance.MIN") + "!<br>";
		str = str + "<center><a action=\"bypass -h npc_%objectId%_FishingReward\">" + ServerStorage.getInstance().getString(player.getLang(), "L2FishermanInstance.WIN_PRIZE") + "</a><br></center>";
		str = str + "<table width=280 border=0 bgcolor=\"000000\"><tr><td width=70 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + "</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.FISHERMAN") + "</td><td width=80 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.LENGTH") + "</td></tr></table><table width=280>";
		for (int x = 1; x <= 5; x++)
		{
			str = str + "<tr><td width=70 align=center>" + x + " " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td>";
			str = str + "<td width=110 align=center>" + FishingChampionship.getInstance().getWinnerName(player, x) + "</td>";
			str = str + "<td width=80 align=center>" + FishingChampionship.getInstance().getFishLength(x) + "</td></tr>";
		}
		str = str + "<td width=80 align=center>0</td></tr></table><br>";
		str = str + "" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PRIZES_LIST") + "<br><table width=280 border=0 bgcolor=\"000000\"><tr><td width=70 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + "</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PRIZE") + "</td><td width=80 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.AMOUNT") + "</td></tr></table><table width=280>";
		str = str + "<tr><td width=70 align=center>1 " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ADENA") + "</td><td width=80 align=center>800000</td></tr><tr><td width=70 align=center>2 " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ADENA") + "</td><td width=80 align=center>500000</td></tr><tr><td width=70 align=center>3 " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ADENA") + "</td><td width=80 align=center>300000</td></tr>";
		str = str + "<tr><td width=70 align=center>4 " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ADENA") + "</td><td width=80 align=center>200000</td></tr><tr><td width=70 align=center>5 " + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.PLACES") + ":</td><td width=110 align=center>" + ServerStorage.getInstance().getString(player.getLang(), "FishingChampionship.ADENA") + "</td><td width=80 align=center>100000</td></tr></table></body></html>";
		html.setHtml(player, str);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
}