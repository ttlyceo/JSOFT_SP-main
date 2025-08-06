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
import java.util.StringTokenizer;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.SquadTrainer;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.AcquireSkillDone;
import l2e.gameserver.network.serverpackets.ExAcquirableSkillListByClass;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class FortSupportCaptainInstance extends MerchantInstance implements SquadTrainer
{
	public FortSupportCaptainInstance(int objectID, NpcTemplate template)
	{
		super(objectID, template);

		setInstanceType(InstanceType.FortSupportCaptainInstance);
	}

	private static final int[] TalismanIds =
	{
	        9914, 9915, 9917, 9918, 9919, 9920, 9921, 9922, 9923, 9924, 9926, 9927, 9928, 9930, 9931, 9932, 9933, 9934, 9935, 9936, 9937, 9938, 9939, 9940, 9941, 9942, 9943, 9944, 9945, 9946, 9947, 9948, 9949, 9950, 9951, 9952, 9953, 9954, 9955, 9956, 9957, 9958, 9959, 9960, 9961, 9962, 9963, 9964, 9965, 9966, 10141, 10142, 10158
	};

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (player.getLastFolkNPC().getObjectId() != getObjectId())
		{
			return;
		}

		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();

		String par = "";
		if (st.countTokens() >= 1)
		{
			par = st.nextToken();
		}

		if (actualCommand.equalsIgnoreCase("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(par);
			}
			catch (final IndexOutOfBoundsException ioobe)
			{}
			catch (final NumberFormatException nfe)
			{}
			showMessageWindow(player, val);
		}
		else if (actualCommand.equalsIgnoreCase("ExchangeKE"))
		{
			final int itemId = TalismanIds[Rnd.get(TalismanIds.length)];
			if (player.exchangeItemsById("FortSupportUnitExchangeKE", this, 9912, (10 * Config.RATE_TALISMAN_ITEM_MULTIPLIER), itemId, Config.RATE_TALISMAN_MULTIPLIER, true))
			{
				showChatWindow(player, "data/html/fortress/supportunit-talisman.htm");
			}
			else
			{
				showChatWindow(player, "data/html/fortress/supportunit-noepau.htm");
			}
		}
		else if (command.equals("subskills"))
		{
			if (player.isClanLeader())
			{
				final List<SkillLearn> skills = SkillTreesParser.getInstance().getAvailableSubPledgeSkills(player.getClan());
				final ExAcquirableSkillListByClass asl = new ExAcquirableSkillListByClass(AcquireSkillType.SUBPLEDGE);
				int count = 0;

				for (final SkillLearn s : skills)
				{
					if (SkillsParser.getInstance().getInfo(s.getId(), s.getLvl()) != null)
					{
						asl.addSkill(s.getId(), s.getGetLevel(), s.getLvl(), s.getLvl(), s.getLevelUpSp(), 0);
						count++;
					}
				}

				if (count == 0)
				{
					player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
					player.sendPacket(AcquireSkillDone.STATIC);
				}
				else
				{
					player.sendPacket(asl);
				}
			}
			else
			{
				showChatWindow(player, "data/html/fortress/supportunit-nosquad.htm");
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	@Override
	public void showChatWindow(Player player)
	{
		if ((player.getClan() == null) || (getFort().getOwnerClan() == null) || (player.getClan() != getFort().getOwnerClan()))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, player.getLang(), "data/html/fortress/supportunit-noclan.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			return;
		}
		showMessageWindow(player, 0);
	}

	private void showMessageWindow(Player player, int val)
	{
		String filename;

		if (val == 0)
		{
			filename = "data/html/fortress/supportunit.htm";
		}
		else
		{
			filename = "data/html/fortress/supportunit-" + val + ".htm";
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getId()));
		if (getFort().getOwnerClan() != null)
		{
			html.replace("%clanname%", getFort().getOwnerClan().getName());
		}
		else
		{
			html.replace("%clanname%", "NPC");
		}
		player.sendPacket(html);
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	@Override
	public void showSubUnitSkillList(Player player)
	{
		onBypassFeedback(player, "subskills");
	}
}