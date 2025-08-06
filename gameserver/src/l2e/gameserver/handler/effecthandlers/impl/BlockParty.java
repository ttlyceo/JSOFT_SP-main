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

import l2e.gameserver.instancemanager.PunishmentManager;
import l2e.gameserver.model.punishment.PunishmentAffect;
import l2e.gameserver.model.punishment.PunishmentSort;
import l2e.gameserver.model.punishment.PunishmentTemplate;
import l2e.gameserver.model.punishment.PunishmentType;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public final class BlockParty extends Effect
{
	private PunishmentTemplate _template;
	
	public BlockParty(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}

	@Override
	public boolean onStart()
	{
		if ((getEffected() == null) || !getEffected().isPlayer())
		{
			return false;
		}
		_template = new PunishmentTemplate(0, String.valueOf(getEffected().getObjectId()), getEffected().getName(null), PunishmentSort.CHARACTER, PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN, 0, "Party banned by bot report", "system", true);
		if (!PunishmentManager.getInstance().addPunishment(getEffected().getActingPlayer(), null, _template, false))
		{
			_template = null;
			return false;
		}
		return true;
	}

	@Override
	public void onExit()
	{
		if (_template != null)
		{
			PunishmentManager.getInstance().stopPunishment(getEffected().getActingPlayer().getClient(), _template);
		}
	}
}