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
package l2e.gameserver.instancemanager;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.sieges.CastleTemplate;
import l2e.gameserver.model.actor.templates.sieges.SiegeSpawn;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.Siege;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public class SiegeManager extends DocumentParser
{
	private final Map<Integer, CastleTemplate> _castleTemplates = new HashMap<>();

	protected SiegeManager()
	{
		load();
	}
	
	@Override
	public final void load()
	{
		_castleTemplates.clear();
		parseDirectory("data/stats/sieges", false);
		info("Loaded " + _castleTemplates.size() + " castle templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		NamedNodeMap attrs;
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("castle".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						
						final StatsSet params = new StatsSet();
						final List<SiegeSpawn> controlTowers = new ArrayList<>();
						final List<SiegeSpawn> flameTowers = new ArrayList<>();
						
						final List<ItemHolder> pvpRewards = new ArrayList<>();
						final List<ItemHolder> castleCaptureRewards = new ArrayList<>();
						final List<ItemHolder> castleDefenceRewards = new ArrayList<>();
						
						boolean allowCastlePvpReward = false;
						boolean allowCaptureCastleReward = false;
						boolean allowDefenceCastleReward = false;
						boolean allowCastlePvpRewardForParty = false;
						boolean allowCaptureCastleRewardForLeader = false;
						boolean allowDefenceCastleRewardForLeader = false;
						
						final int castleId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								params.set(name, attrs.getNamedItem(name) != null ? attrs.getNamedItem(name).getNodeValue() : attrs.getNamedItem("nameEn") != null ? attrs.getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						params.set("enableSiege", Boolean.parseBoolean(attrs.getNamedItem("enableSiege").getNodeValue()));
						
						for (Node cat = d.getFirstChild(); cat != null; cat = cat.getNextSibling())
						{
							if ("set".equalsIgnoreCase(cat.getNodeName()))
							{
								attrs = cat.getAttributes();
								final String name = attrs.getNamedItem("name").getNodeValue();
								final String value = attrs.getNamedItem("value").getNodeValue();
								params.set(attrs.getNamedItem("name").getNodeValue(), value);
								if (name.equals("mercenariesLimit"))
								{
									MercTicketManager.MERCS_MAX_PER_CASTLE[castleId - 1] = Integer.parseInt(value);
								}
							}
							else if ("flameTowers".equalsIgnoreCase(cat.getNodeName()))
							{
								for (Node s = cat.getFirstChild(); s != null; s = s.getNextSibling())
								{
									if ("npc".equalsIgnoreCase(s.getNodeName()))
									{
										attrs = s.getAttributes();
										
										final int npcId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
										final var location = Location.parseLoc(attrs.getNamedItem("loc").getNodeValue());
										final int hp = Integer.parseInt(attrs.getNamedItem("hp").getNodeValue());
										flameTowers.add(new SiegeSpawn(castleId, location, npcId, hp));
									}
								}
							}
							else if ("controlTowers".equalsIgnoreCase(cat.getNodeName()))
							{
								for (Node s = cat.getFirstChild(); s != null; s = s.getNextSibling())
								{
									if ("npc".equalsIgnoreCase(s.getNodeName()))
									{
										attrs = s.getAttributes();
										
										final int npcId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
										final var location = Location.parseLoc(attrs.getNamedItem("loc").getNodeValue());
										final int hp = Integer.parseInt(attrs.getNamedItem("hp").getNodeValue());
										controlTowers.add(new SiegeSpawn(castleId, location, npcId, hp));
									}
								}
							}
							else if ("params".equalsIgnoreCase(cat.getNodeName()))
							{
								for (Node s = cat.getFirstChild(); s != null; s = s.getNextSibling())
								{
									if ("set".equalsIgnoreCase(s.getNodeName()))
									{
										attrs = s.getAttributes();
										params.set(attrs.getNamedItem("name").getNodeValue(), attrs.getNamedItem("value").getNodeValue());
									}
								}
							}
							else if ("pvp".equalsIgnoreCase(cat.getNodeName()))
							{
								attrs = cat.getAttributes();
								
								allowCastlePvpReward = Boolean.parseBoolean(attrs.getNamedItem("allowReward").getNodeValue());
								allowCastlePvpRewardForParty = Boolean.parseBoolean(attrs.getNamedItem("allowForParty").getNodeValue());
								for (Node s = cat.getFirstChild(); s != null; s = s.getNextSibling())
								{
									if ("item".equalsIgnoreCase(s.getNodeName()))
									{
										attrs = s.getAttributes();
										
										final int itemId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
										final long count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
										final double chance = Integer.parseInt(attrs.getNamedItem("chance").getNodeValue());
										pvpRewards.add(new ItemHolder(itemId, count, chance));
									}
								}
							}
							else if ("capture".equalsIgnoreCase(cat.getNodeName()))
							{
								attrs = cat.getAttributes();
								
								allowCaptureCastleReward = Boolean.parseBoolean(attrs.getNamedItem("allowReward").getNodeValue());
								allowCaptureCastleRewardForLeader = Boolean.parseBoolean(attrs.getNamedItem("onlyForLeader").getNodeValue());
								for (Node s = cat.getFirstChild(); s != null; s = s.getNextSibling())
								{
									if ("item".equalsIgnoreCase(s.getNodeName()))
									{
										attrs = s.getAttributes();
										
										final int itemId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
										final long count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
										final double chance = Integer.parseInt(attrs.getNamedItem("chance").getNodeValue());
										castleCaptureRewards.add(new ItemHolder(itemId, count, chance));
									}
								}
							}
							else if ("defence".equalsIgnoreCase(cat.getNodeName()))
							{
								attrs = cat.getAttributes();
								
								allowDefenceCastleReward = Boolean.parseBoolean(attrs.getNamedItem("allowReward").getNodeValue());
								allowDefenceCastleRewardForLeader = Boolean.parseBoolean(attrs.getNamedItem("onlyForLeader").getNodeValue());
								for (Node s = cat.getFirstChild(); s != null; s = s.getNextSibling())
								{
									if ("item".equalsIgnoreCase(s.getNodeName()))
									{
										attrs = s.getAttributes();
										
										final int itemId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
										final long count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
										final double chance = Integer.parseInt(attrs.getNamedItem("chance").getNodeValue());
										castleDefenceRewards.add(new ItemHolder(itemId, count, chance));
									}
								}
							}
						}
						final var template = new CastleTemplate(castleId, params, controlTowers, flameTowers, allowCastlePvpReward, allowCaptureCastleReward, allowDefenceCastleReward, allowCastlePvpRewardForParty, allowCaptureCastleRewardForLeader, allowDefenceCastleRewardForLeader, pvpRewards, castleCaptureRewards, castleDefenceRewards);
						_castleTemplates.put(castleId, template);
						final var castle = CastleManager.getInstance().getCastleById(castleId);
						if (castle != null)
						{
							castle.setTemplate(template);
						}
					}
				}
			}
		}
	}

	public final void addSiegeSkills(Player character)
	{
		for (final Skill sk : SkillsParser.getInstance().getSiegeSkills(character.isNoble(), character.getClan().getCastleId() > 0))
		{
			character.addSkill(sk, false);
		}
	}

	public final boolean checkIsRegistered(Clan clan, int castleid)
	{
		if (clan == null)
		{
			return false;
		}

		if (clan.getCastleId() > 0)
		{
			return true;
		}

		boolean register = false;
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("SELECT clan_id FROM siege_clans where clan_id=? and castle_id=?");
			statement.setInt(1, clan.getId());
			statement.setInt(2, castleid);
			final ResultSet rs = statement.executeQuery();

			while (rs.next())
			{
				register = true;
				break;
			}

			rs.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("checkIsRegistered(): " + e.getMessage(), e);
		}
		return register;
	}

	public final void removeSiegeSkills(Player character)
	{
		for (final Skill sk : SkillsParser.getInstance().getSiegeSkills(character.isNoble(), character.getClan().getCastleId() > 0))
		{
			character.removeSkill(sk);
		}
	}
	
	public final List<SiegeSpawn> getControlTowerSpawnList(int castleId)
	{
		final var template = _castleTemplates.get(castleId);
		return template != null ? template.getControlTowers() : Collections.emptyList();
	}

	public final List<SiegeSpawn> getFlameTowerSpawnList(int castleId)
	{
		final var template = _castleTemplates.get(castleId);
		return template != null ? template.getFlameTowers() : Collections.emptyList();
	}

	public final Siege getSiege(GameObject activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}

	public final Siege getSiege(int x, int y, int z)
	{
		for (final Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle.getSiege().checkIfInZone(x, y, z))
			{
				return castle.getSiege();
			}
		}
		return null;
	}

	public final List<Siege> getSieges()
	{
		final List<Siege> sieges = new ArrayList<>();
		for (final var castle : CastleManager.getInstance().getCastles())
		{
			final var tpl = _castleTemplates.get(castle.getId());
			if (tpl != null && tpl.isEnableSiege())
			{
				sieges.add(castle.getSiege());
			}
		}
		return sieges;
	}
	
	public static final SiegeManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final SiegeManager _instance = new SiegeManager();
	}
}