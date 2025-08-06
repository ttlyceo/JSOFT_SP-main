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
package l2e.gameserver.model.stats;

import java.io.File;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Creature;

public enum BaseStats
{
	STR(new STR()), INT(new INT()), DEX(new DEX()), WIT(new WIT()), CON(new CON()), MEN(new MEN()), NULL(new NULL());

	private static final Logger _log = LoggerFactory.getLogger(BaseStats.class);

	protected static final double[] STRbonus = new double[Config.BASE_STR_LIMIT];
	protected static final double[] INTbonus = new double[Config.BASE_INT_LIMIT];
	protected static final double[] DEXbonus = new double[Config.BASE_DEX_LIMIT];
	protected static final double[] WITbonus = new double[Config.BASE_WIT_LIMIT];
	protected static final double[] CONbonus = new double[Config.BASE_CON_LIMIT];
	protected static final double[] MENbonus = new double[Config.BASE_MEN_LIMIT];

	private final BaseStat _stat;

	public final String getValue()
	{
		return _stat.getClass().getSimpleName();
	}

	private BaseStats(BaseStat s)
	{
		_stat = s;
	}

	public final double calcBonus(Creature actor)
	{
		if (actor != null)
		{
			if (actor.isNpc() && ((!actor.isRaid() && !actor.isRaidMinion() && !Config.CALC_NPC_STATS) || (actor.isRaid() && !actor.isRaidMinion() && !Config.CALC_RAID_STATS)))
			{
				return 1;
			}
			return _stat.calcBonus(actor);
		}
		return 1;
	}
	
	public double calcChanceMod(Creature actor)
	{
		return _stat.calcBonus(actor);
	}

	public static final BaseStats valueOfXml(String name)
	{
		name = name.intern();
		for (final BaseStats s : values())
		{
			if (s.getValue().equalsIgnoreCase(name))
			{
				return s;
			}
		}
		throw new NoSuchElementException("Unknown name '" + name + "' for enum BaseStats");
	}

	private interface BaseStat
	{
		public double calcBonus(Creature actor);
	}

	protected static final class STR implements BaseStat
	{
		@Override
		public final double calcBonus(Creature actor)
		{
			final int stat = actor.getSTR();
			return STRbonus[stat >= Config.BASE_STR_LIMIT ? Config.BASE_RESET_STR : stat];
		}
	}

	protected static final class INT implements BaseStat
	{
		@Override
		public final double calcBonus(Creature actor)
		{
			final int stat = actor.getINT();
			return INTbonus[stat >= Config.BASE_INT_LIMIT ? Config.BASE_RESET_INT : stat];
		}
	}

	protected static final class DEX implements BaseStat
	{
		@Override
		public final double calcBonus(Creature actor)
		{
			final int stat = actor.getDEX();
			return DEXbonus[stat >= Config.BASE_DEX_LIMIT ? Config.BASE_RESET_DEX : stat];
		}
	}

	protected static final class WIT implements BaseStat
	{
		@Override
		public final double calcBonus(Creature actor)
		{
			final int stat = actor.getWIT();
			return WITbonus[stat >= Config.BASE_WIT_LIMIT ? Config.BASE_RESET_WIT : stat];
		}
	}

	protected static final class CON implements BaseStat
	{
		@Override
		public final double calcBonus(Creature actor)
		{
			final int stat = actor.getCON();
			return CONbonus[stat >= Config.BASE_CON_LIMIT ? Config.BASE_RESET_CON : stat];
		}
	}

	protected static final class MEN implements BaseStat
	{
		@Override
		public final double calcBonus(Creature actor)
		{
			final int stat = actor.getMEN();
			return MENbonus[stat >= Config.BASE_MEN_LIMIT ? Config.BASE_RESET_MEN : stat];
		}
	}

	protected static final class NULL implements BaseStat
	{
		@Override
		public final double calcBonus(Creature actor)
		{
			return 1f;
		}
	}

	static
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		final File file = new File(Config.DATAPACK_ROOT, "data/stats/chars/statBonus.xml");
		Document doc = null;

		if (file.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(file);
			}
			catch (final Exception e)
			{
				_log.warn("[BaseStats] Could not parse file: " + e.getMessage(), e);
			}

			if (doc != null)
			{
				String statName;
				int val;
				double bonus;
				NamedNodeMap attrs;
				for (Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
				{
					if ("list".equalsIgnoreCase(list.getNodeName()))
					{
						for (Node stat = list.getFirstChild(); stat != null; stat = stat.getNextSibling())
						{
							statName = stat.getNodeName();
							for (Node value = stat.getFirstChild(); value != null; value = value.getNextSibling())
							{
								if ("stat".equalsIgnoreCase(value.getNodeName()))
								{
									attrs = value.getAttributes();
									try
									{
										val = Integer.parseInt(attrs.getNamedItem("value").getNodeValue());
										bonus = Double.parseDouble(attrs.getNamedItem("bonus").getNodeValue());
									}
									catch (final Exception e)
									{
										_log.error("[BaseStats] Invalid stats value: " + value.getNodeValue() + ", skipping");
										continue;
									}

									if ("STR".equalsIgnoreCase(statName))
									{
										STRbonus[val] = bonus;
									}
									else if ("INT".equalsIgnoreCase(statName))
									{
										INTbonus[val] = bonus;
									}
									else if ("DEX".equalsIgnoreCase(statName))
									{
										DEXbonus[val] = bonus;
									}
									else if ("WIT".equalsIgnoreCase(statName))
									{
										WITbonus[val] = bonus;
									}
									else if ("CON".equalsIgnoreCase(statName))
									{
										CONbonus[val] = bonus;
									}
									else if ("MEN".equalsIgnoreCase(statName))
									{
										MENbonus[val] = bonus;
									}
									else
									{
										_log.error("[BaseStats] Invalid stats name: " + statName + ", skipping");
									}
								}
							}
						}
					}
				}
			}
		}
		else
		{
			throw new Error("[BaseStats] File not found: " + file.getName());
		}
	}
}
