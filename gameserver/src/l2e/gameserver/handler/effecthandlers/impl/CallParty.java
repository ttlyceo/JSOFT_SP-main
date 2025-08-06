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

import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SummonRequestHolder;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ConfirmDlg;

public class CallParty extends Effect
{
	public CallParty(Env env, EffectTemplate template)
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
		final Party party = getEffector().getParty();
		if (party != null)
		{
			for (final Player partyMember : party.getMembers())
			{
				if (CallPc.checkSummonTargetStatus(partyMember, getEffector().getActingPlayer()))
				{
					if (getEffector() != partyMember)
					{
						if (partyMember.getBlockPartyRecall())
						{
							partyMember.addScript(new SummonRequestHolder(getEffector().getActingPlayer(), getSkill(), false));
							final ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.C1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId());
							confirm.addCharName(getEffector());
							confirm.addZoneName(getEffector().getX(), getEffector().getY(), getEffector().getZ());
							confirm.addTime(30000);
							confirm.addRequesterId(getEffector().getObjectId());
							partyMember.sendPacket(confirm);
						}
						else
						{
							partyMember.teleToLocation(getEffector().getX(), getEffector().getY(), getEffector().getZ(), true, getEffector().getReflection());
						}
					}
				}
			}
			return true;
		}
		return false;
	}
}