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
package l2e.gameserver.model.skills.engines;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.file.filter.XMLFilter;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.engines.items.DocumentItem;
import l2e.gameserver.model.skills.engines.skills.DocumentSkill;

public class DocumentEngine
{
	private static final Logger _log = LoggerFactory.getLogger(DocumentEngine.class);
	
	private final List<File> _itemFiles = new ArrayList<>();
	private final List<File> _skillFiles = new ArrayList<>();
	
	public static DocumentEngine getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected DocumentEngine()
	{
		hashFiles("data/stats/items/items", _itemFiles);
		if (Config.CUSTOM_ITEMS)
		{
			hashFiles("data/stats/items/items/custom", _itemFiles);
		}
		hashFiles("data/stats/skills/skills", _skillFiles);
		if (Config.CUSTOM_SKILLS)
		{
			hashFiles("data/stats/skills/skills/custom", _skillFiles);
		}
	}
	
	private void hashFiles(String dirname, List<File> hash)
	{
		final File dir = new File(Config.DATAPACK_ROOT, dirname);
		if (!dir.exists())
		{
			_log.warn("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}
		final File[] files = dir.listFiles(new XMLFilter());
		for (final File f : files)
		{
			hash.add(f);
		}
	}
	
	public List<Skill> loadSkills(File file)
	{
		if (file == null)
		{
			_log.warn("Skill file not found.");
			return null;
		}
		final DocumentSkill doc = new DocumentSkill(file);
		doc.parse();
		return doc.getSkills();
	}
	
	public void loadAllSkills(final Map<Integer, Skill> allSkills)
	{
		int count = 0;
		for (final File file : _skillFiles)
		{
			final List<Skill> s = loadSkills(file);
			if (s == null)
			{
				continue;
			}
			for (final Skill skill : s)
			{
				allSkills.put(SkillsParser.getSkillHashCode(skill), skill);
				count++;
			}
		}
		_log.info("SkillsParser: Loaded " + count + " skill templates.");
	}
	
	public List<Item> loadItems()
	{
		final List<Item> list = new ArrayList<>();
		for (final File f : _itemFiles)
		{
			final DocumentItem document = new DocumentItem(f);
			document.parse();
			list.addAll(document.getItemList());
		}
		return list;
	}
	
	private static class SingletonHolder
	{
		protected static final DocumentEngine _instance = new DocumentEngine();
	}
}