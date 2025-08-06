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
package l2e.gameserver.data.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.model.skills.Skill;

/**
 * Rework by LordWinter 26.06.2013 Fixed by L2J Eternity-World
 */
public class SchemesParser extends LoggerObject
{
	private final Map<String, ArrayList<SkillInfo>> _buffs = new HashMap<>();

	protected SchemesParser()
	{
		load();
	}

	public void load()
	{
		_buffs.clear();
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			final File file = new File(Config.DATAPACK_ROOT, "data/stats/services/communityBuffer.xml");
			if (!file.exists())
			{
				warn("Couldn't find data/stats/services/" + file.getName());
				return;
			}

			final Document doc = factory.newDocumentBuilder().parse(file);
			NamedNodeMap attrs;
			String groupEn, groupRu;
			int id, level, adena, minLvl;

			for (Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
			{
				if ("list".equalsIgnoreCase(list.getNodeName()))
				{
					for (Node groups = list.getFirstChild(); groups != null; groups = groups.getNextSibling())
					{
						if ("group".equalsIgnoreCase(groups.getNodeName()))
						{
							attrs = groups.getAttributes();
							groupEn = attrs.getNamedItem("buffEn").getNodeValue();
							groupRu = attrs.getNamedItem("buffRu").getNodeValue();

							for (Node skills = groups.getFirstChild(); skills != null; skills = skills.getNextSibling())
							{
								if ("skill".equalsIgnoreCase(skills.getNodeName()))
								{
									attrs = skills.getAttributes();
									id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
									level = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
									adena = Integer.parseInt(attrs.getNamedItem("adena").getNodeValue());
									minLvl = Integer.parseInt(attrs.getNamedItem("minLvl").getNodeValue());

									final SkillInfo info = new SkillInfo();
									info._id = id;
									info._lvl = level;
									info._groupEn = groupEn;
									info._groupRu = groupRu;
									info._cost = adena;
									info._minLvl = minLvl;

									final Skill skill = SkillsParser.getInstance().getInfo(info._id, info._lvl);
									if (skill == null)
									{
										warn("Can't find skill id: " + info._id + " level: " + info._lvl + " in communityBuffer.xml");
										continue;
									}
									_buffs.putIfAbsent(info._groupEn, new ArrayList<>());
									_buffs.get(info._groupEn).add(info);
								}
							}
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error while loading buffs: " + e);
		}
		info("Loaded " + _buffs.size() + " buff templates.");
	}

	public Set<String> getBuffGroups()
	{
		return _buffs.keySet();
	}

	public ArrayList<SkillInfo> getBuffInfoByGroup(String group)
	{
		return _buffs.get(group);
	}

	public boolean buffsContainsSkill(int skillId, int skillLvl)
	{
		for (final ArrayList<SkillInfo> infos : _buffs.values())
		{
			for (final SkillInfo info : infos)
			{
				if ((skillId == info._id) && (skillLvl == info._lvl))
				{
					return true;
				}
			}
		}
		return false;
	}

	public boolean buffsIdContainsSkill(int skillId)
	{
		for (final ArrayList<SkillInfo> infos : _buffs.values())
		{
			for (final SkillInfo info : infos)
			{
				if (skillId == info._id)
				{
					return true;
				}
			}
		}
		return false;
	}

	public int getSkillFee(int skillId)
	{
		for (final ArrayList<SkillInfo> infos : _buffs.values())
		{
			for (final SkillInfo info : infos)
			{
				if (skillId == info._id)
				{
					return (int) info._cost;
				}
			}
		}
		return 0;
	}

	public int getSkillMinLvl(int skillId)
	{
		for (final ArrayList<SkillInfo> infos : _buffs.values())
		{
			for (final SkillInfo info : infos)
			{
				if (skillId == info._id)
				{
					return info._minLvl;
				}
			}
		}
		return 0;
	}

	public class SkillInfo
	{
		public int _id;
		public int _lvl;
		public String _groupEn;
		public String _groupRu;
		public long _cost;
		public int _minLvl;
	}

	public static SchemesParser getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final SchemesParser _instance = new SchemesParser();
	}
}