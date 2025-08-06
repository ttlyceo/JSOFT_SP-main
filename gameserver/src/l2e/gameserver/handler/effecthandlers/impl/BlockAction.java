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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.data.parser.BotReportParser;
import l2e.gameserver.instancemanager.PunishmentManager;
import l2e.gameserver.model.punishment.PunishmentAffect;
import l2e.gameserver.model.punishment.PunishmentSort;
import l2e.gameserver.model.punishment.PunishmentTemplate;
import l2e.gameserver.model.punishment.PunishmentType;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public final class BlockAction extends Effect
{
	private final List<Integer> _blockedActions;
	private final List<PunishmentTemplate> _templates;

	public BlockAction(Env env, EffectTemplate template)
	{
		super(env, template);
		final String[] rawActions = template.getParameters().getString("blockedActions").split(",");
		_blockedActions = new ArrayList<>(rawActions.length);
		for (final String act : rawActions)
		{
			int id = -1;
			try
			{
				id = Integer.parseInt(act);
				_blockedActions.add(id);
			}
			catch (final Exception e)
			{}
		}
		_templates = new ArrayList<>();
	}
	
	@Override
	public boolean onStart()
	{
		if ((getEffected() == null) || !getEffected().isPlayer())
		{
			return false;
		}
		
		if (_blockedActions.contains(BotReportParser.PARTY_ACTION_BLOCK_ID))
		{
			final var template = new PunishmentTemplate(0, String.valueOf(getEffected().getObjectId()), getEffected().getName(null), PunishmentSort.CHARACTER, PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN, 0, "block action debuff", "system", true);
			if (PunishmentManager.getInstance().addPunishment(getEffected().getActingPlayer(), null, template, false))
			{
				_templates.add(template);
			}
		}
		
		if (_blockedActions.contains(BotReportParser.CHAT_BLOCK_ID))
		{
			final var template = new PunishmentTemplate(0, String.valueOf(getEffected().getObjectId()), getEffected().getName(null), PunishmentSort.CHARACTER, PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN, 0, "block action debuff", "system", true);
			if (PunishmentManager.getInstance().addPunishment(getEffected().getActingPlayer(), null, template, false))
			{
				_templates.add(template);
			}
		}
		return true;
	}
	
	@Override
	public void onExit()
	{
		if (!_templates.isEmpty())
		{
			_templates.stream().filter(t -> t != null).forEach(i -> PunishmentManager.getInstance().stopPunishment(getEffected().getActingPlayer().getClient(), i));
		}
	}
	
	@Override
	public boolean checkCondition(Object id)
	{
		return !_blockedActions.contains(id);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.ACTION_BLOCK;
	}
}