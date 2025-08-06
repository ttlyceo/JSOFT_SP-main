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
package l2e.gameserver.model.zone.type;

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.zone.ZoneType;

public class AbnormalZone extends ZoneType
{
	private AbnormalEffect[] _abnormalEffect = null;

	public AbnormalZone(int id)
	{
		super(id);
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("abnormalVisualEffect"))
		{
			final String[] specialEffects = value.split(",");
			_abnormalEffect = new AbnormalEffect[specialEffects.length];
			for (int i = 0; i < specialEffects.length; i++)
			{
				_abnormalEffect[i] = AbnormalEffect.getByName(specialEffects[i]);
			}
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(Creature character)
	{
		if (_abnormalEffect != null)
		{
			for (final AbnormalEffect eff : _abnormalEffect)
			{
				if (eff != null && eff != AbnormalEffect.NONE)
				{
					character.startAbnormalEffect(eff);
				}
			}
		}
	}

	@Override
	protected void onExit(Creature character)
	{
		if (_abnormalEffect != null)
		{
			for (final AbnormalEffect eff : _abnormalEffect)
			{
				if (eff != null && eff != AbnormalEffect.NONE)
				{
					character.stopAbnormalEffect(eff);
				}
			}
		}
	}

	@Override
	public void onDieInside(Creature character)
	{
		onExit(character);
	}

	@Override
	public void onReviveInside(Creature character)
	{
		onEnter(character);
	}
}