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
package l2e.gameserver.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.data.parser.OptionsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.options.Options;

public final class Augmentation
{
	private int _effectsId = 0;
	private AugmentationStatBoni _boni = null;
	
	public Augmentation(int effects)
	{
		_effectsId = effects;
		_boni = new AugmentationStatBoni(_effectsId);
	}
	
	public static class AugmentationStatBoni
	{
		private static final Logger _log = LoggerFactory.getLogger(AugmentationStatBoni.class);
		private final List<Options> _options = new ArrayList<>();
		private boolean _active;

		public AugmentationStatBoni(int augmentationId)
		{
			_active = false;
			final int[] stats = new int[2];
			stats[0] = 0x0000FFFF & augmentationId;
			stats[1] = (augmentationId >> 16);

			for (final int stat : stats)
			{
				final Options op = OptionsParser.getInstance().getOptions(stat);
				if (op != null)
				{
					_options.add(op);
				}
				else
				{
					_log.warn(getClass().getSimpleName() + ": Couldn't find option: " + stat);
				}
			}
		}

		public void applyBonus(Player player)
		{
			if (_active)
			{
				return;
			}

			for (final Options op : _options)
			{
				op.apply(player);
			}

			_active = true;
		}

		public void removeBonus(Player player)
		{
			if (!_active)
			{
				return;
			}

			for (final Options op : _options)
			{
				op.remove(player);
			}

			_active = false;
		}
	}

	public int getAttributes()
	{
		return _effectsId;
	}

	public int getAugmentationId()
	{
		return _effectsId;
	}

	public void applyBonus(Player player)
	{
		_boni.applyBonus(player);
	}

	public void removeBonus(Player player)
	{
		_boni.removeBonus(player);
	}
}