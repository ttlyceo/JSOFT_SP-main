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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.DocumentBuilderFactory;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.geometry.Polygon;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.instance.RaidBossInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.spawn.SpawnTemplate;
import l2e.gameserver.model.spawn.SpawnTerritory;
import l2e.gameserver.model.spawn.Spawner;

public class HellboundManager extends LoggerObject
{
	private int _level = 0;
	private int _trust = 0;
	private int _maxTrust = 0;
	private int _minTrust = 0;

	private ScheduledFuture<?> _engine = null;
	private final List<HellboundSpawn> _population = new ArrayList<>();

	protected HellboundManager()
	{
		load();
		loadSpawns();
	}

	public final int getLevel()
	{
		return _level;
	}

	public final synchronized void updateTrust(int t, boolean useRates)
	{
		if (isLocked())
		{
			return;
		}

		int reward = t;
		if (useRates)
		{
			reward = (int) (t > 0 ? Config.RATE_HB_TRUST_INCREASE * t : Config.RATE_HB_TRUST_DECREASE * t);
		}

		final int trust = Math.max(_trust + reward, _minTrust);
		if (_maxTrust > 0)
		{
			_trust = Math.min(trust, _maxTrust);
		}
		else
		{
			_trust = trust;
		}
	}

	public final void setLevel(int lvl)
	{
		_level = lvl;
	}

	public final int getTrust()
	{
		return _trust;
	}

	public final int getMaxTrust()
	{
		return _maxTrust;
	}

	public final int getMinTrust()
	{
		return _minTrust;
	}

	public final void setMaxTrust(int trust)
	{
		_maxTrust = trust;
		if ((_maxTrust > 0) && (_trust > _maxTrust))
		{
			_trust = _maxTrust;
		}
	}

	public final void setMinTrust(int trust)
	{
		_minTrust = trust;
		if (_trust >= _maxTrust)
		{
			_trust = _minTrust;
		}
	}

	public final boolean isLocked()
	{
		return _level == 0;
	}

	public final void unlock()
	{
		if (_level == 0)
		{
			setLevel(1);
		}
	}

	public final void registerEngine(Runnable r, int interval)
	{
		if (_engine != null)
		{
			_engine.cancel(false);
		}
		_engine = ThreadPoolManager.getInstance().scheduleAtFixedRate(r, interval, interval);
	}

	public final void doSpawn()
	{
		int added = 0;
		int deleted = 0;
		for (final var spawnDat : _population)
		{
			try
			{
				if (spawnDat == null)
				{
					continue;
				}

				var npc = spawnDat.getLastSpawn();
				if (ArrayUtils.contains(spawnDat.getStages(), _level))
				{
					if (npc instanceof RaidBossInstance)
					{
						npc = spawnDat.getLastSpawn();
						if (npc == null)
						{
							RaidBossSpawnManager.getInstance().addNewSpawn(spawnDat, false);
							added++;
						}
					}
					else
					{
						spawnDat.startRespawn();
						npc = spawnDat.getLastSpawn();
						if (npc == null)
						{
							npc = spawnDat.doSpawn();
							added++;
							SpawnParser.getInstance().addRandomSpawnByNpc(spawnDat, npc.getTemplate());
						}
						else
						{
							if (npc.isDead())
							{
								npc.doRevive();
							}
							if (!npc.isVisible())
							{
								added++;
							}

							npc.setCurrentHp(npc.getMaxHp());
							npc.setCurrentMp(npc.getMaxMp());
						}
					}
				}
				else
				{
					spawnDat.stopRespawn();

					if ((npc != null) && npc.isVisible())
					{
						SpawnParser.getInstance().removeRandomSpawnByNpc(npc);
						npc.deleteMe();
						deleted++;
					}
				}
			}
			catch (final Exception e)
			{
				warn("" + e.getMessage());
			}
		}

		if (added > 0)
		{
			if (Config.DEBUG)
			{
				info("Spawned " + added + " NPCs.");
			}
		}
		if (deleted > 0)
		{
			if (Config.DEBUG)
			{
				info("Removed " + deleted + " NPCs.");
			}
		}
	}

	public final void cleanUp()
	{
		saveData();
		if (_engine != null)
		{
			_engine.cancel(true);
			_engine = null;
		}
		_population.clear();
	}

	private final void load()
	{
		_level = ServerVariables.getInt("HBLevel", 0);
		_trust = ServerVariables.getInt("HBTrust", 0);
	}

	public final void saveData()
	{
		ServerVariables.set("HBLevel", _level);
		ServerVariables.set("HBTrust", _trust);
	}

	protected void loadSpawns()
	{
		try
		{
			final var factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			final var file = new File(Config.DATAPACK_ROOT + "/data/stats/npcs/spawnZones/hellbound_spawnlist.xml");
			if (!file.exists())
			{
				error("Missing hellbound_spawnlist.xml. The quest wont work without it!");
				return;
			}

			final var doc = factory.newDocumentBuilder().parse(file);
			final var first = doc.getFirstChild();
			if ((first != null) && "list".equalsIgnoreCase(first.getNodeName()))
			{
				for (var n = first.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("spawn".equalsIgnoreCase(n.getNodeName()))
					{
						int totalCount = 0;
						
						final var attrs = n.getAttributes();
						final var att = attrs.getNamedItem("npcId");
						if (att == null)
						{
							error("Missing npcId in npc List, skipping");
							continue;
						}
						final int npcId = Integer.parseInt(attrs.getNamedItem("npcId").getNodeValue());
						Location spawnLoc = null;
						if (n.getAttributes().getNamedItem("loc") != null)
						{
							spawnLoc = Location.parseLoc(n.getAttributes().getNamedItem("loc").getNodeValue());
						}
						int count = 1;
						if (n.getAttributes().getNamedItem("count") != null)
						{
							count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
						}
						int respawn = 60;
						if (n.getAttributes().getNamedItem("respawn") != null)
						{
							respawn = Integer.parseInt(n.getAttributes().getNamedItem("respawn").getNodeValue());
						}
						int respawnRnd = 0;
						if (n.getAttributes().getNamedItem("respawn_random") != null)
						{
							respawnRnd = Integer.parseInt(n.getAttributes().getNamedItem("respawn_random").getNodeValue());
						}

						final var attr = n.getAttributes().getNamedItem("stage");
						final var st = new StringTokenizer(attr.getNodeValue(), ";");
						final int tokenCount = st.countTokens();
						final int[] stages = new int[tokenCount];
						for (int i = 0; i < tokenCount; i++)
						{
							final Integer value = Integer.decode(st.nextToken().trim());
							stages[i] = value;
						}

						SpawnTerritory territory = null;
						for (var s1 = n.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
						{
							if ("territory".equalsIgnoreCase(s1.getNodeName()))
							{
								final var poly = new Polygon();
								for (var s2 = s1.getFirstChild(); s2 != null; s2 = s2.getNextSibling())
								{
									if ("add".equalsIgnoreCase(s2.getNodeName()))
									{
										final int x = Integer.parseInt(s2.getAttributes().getNamedItem("x").getNodeValue());
										final int y = Integer.parseInt(s2.getAttributes().getNamedItem("y").getNodeValue());
										final int minZ = Integer.parseInt(s2.getAttributes().getNamedItem("zmin").getNodeValue());
										final int maxZ = Integer.parseInt(s2.getAttributes().getNamedItem("zmax").getNodeValue());
										poly.add(x, y).setZmin(minZ).setZmax(maxZ);
									}
								}
								territory = new SpawnTerritory().add(poly);

								if (!poly.validate())
								{
									warn("Invalid spawn territory : " + poly + '!');
									continue;
								}
							}
						}

						if (spawnLoc == null && territory == null)
						{
							warn("no spawn data for npc id : " + npcId + '!');
							continue;
						}

						HellboundSpawn spawnDat;
						final var template = NpcsParser.getInstance().getTemplate(npcId);
						if (template != null)
						{
							while (totalCount < count)
							{
								final var tpl = new SpawnTemplate("none", 1, respawn, respawnRnd);
								tpl.addSpawnRange(spawnLoc != null ? spawnLoc : territory);
								
								spawnDat = new HellboundSpawn(template);
								spawnDat.setAmount(1);
								spawnDat.setSpawnTemplate(tpl);
								spawnDat.setLocation(spawnDat.calcSpawnRangeLoc(template));
								spawnDat.setRespawnDelay(respawn, respawnRnd);
								spawnDat.setStages(stages);
								
								_population.add(spawnDat);
								totalCount++;
								SpawnParser.getInstance().addNewSpawn(spawnDat);
							}
						}
					}
				}
			}
			info("Loaded " + _population.size() + " spawn entries.");
		}
		catch (final Exception e)
		{
			warn("SpawnList could not be initialized.", e);
		}
	}

	public static final class HellboundSpawn extends Spawner
	{
		private int[] _stages;

		public HellboundSpawn(NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
		{
			super(mobTemplate);
		}

		public int[] getStages()
		{
			return _stages;
		}
		
		public void setStages(int[] stages)
		{
			_stages = stages;
		}
	}

	public static final HellboundManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final HellboundManager _instance = new HellboundManager();
	}
}