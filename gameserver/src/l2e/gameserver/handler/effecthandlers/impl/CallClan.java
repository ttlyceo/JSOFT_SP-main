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
package l2e.gameserver.handler.effecthandlers.impl;

import l2e.gameserver.model.holders.SummonRequestHolder;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ConfirmDlg;

public class CallClan extends Effect
{
	public CallClan(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.CALLPC;
	}

	@Override
	public boolean calcSuccess()
	{
		return true;
	}

	@Override
	public boolean onStart()
	{
		final var activeChar = getEffector().getActingPlayer();
		if (activeChar == null)
		{
			return false;
		}
		
		final var clan = activeChar.getClan();
		if (clan != null)
		{
			for (final var member : clan.getMembers())
			{
				if (member == null)
				{
					continue;
				}
				
				final var clanMember = member.getPlayerInstance();
				if (clanMember == null || !clanMember.isOnline())
				{
					continue;
				}
				
				if (CallPc.checkSummonTargetStatus(clanMember, activeChar))
				{
					if (activeChar != clanMember)
					{
						if (clanMember.getBlockPartyRecall())
						{
							clanMember.addScript(new SummonRequestHolder(activeChar, getSkill(), false));
							final var confirm = new ConfirmDlg(SystemMessageId.C1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId());
							confirm.addCharName(activeChar);
							confirm.addZoneName(activeChar.getX(), activeChar.getY(), activeChar.getZ());
							confirm.addTime(30000);
							confirm.addRequesterId(activeChar.getObjectId());
							clanMember.sendPacket(confirm);
						}
						else
						{
							clanMember.teleToLocation(activeChar.getX(), activeChar.getY(), activeChar.getZ(), true, activeChar.getReflection());
						}
					}
				}
			}
			return true;
		}
		return false;
	}
}