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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.SkillLearn.SubClassData;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.base.SocialClass;
import l2e.gameserver.model.base.SubClass;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public final class SkillTreesParser extends DocumentParser
{
	private final Map<ClassId, Map<Integer, SkillLearn>> _classSkillTrees = new HashMap<>();
	private final Map<ClassId, List<Integer>> _originalClassSkillIdTrees = new HashMap<>();
	private final Map<ClassId, Map<Integer, SkillLearn>> _originalClassSkillTrees = new HashMap<>();
	private final Map<ClassId, Map<Integer, SkillLearn>> _transferSkillTrees = new HashMap<>();
	private final Map<Integer, SkillLearn> _collectSkillTree = new HashMap<>();
	private final Map<Integer, SkillLearn> _fishingSkillTree = new HashMap<>();
	private final Map<Integer, SkillLearn> _pledgeSkillTree = new HashMap<>();
	private final Map<Integer, SkillLearn> _subClassSkillTree = new HashMap<>();
	private final Map<Integer, SkillLearn> _subPledgeSkillTree = new HashMap<>();
	private final Map<Integer, SkillLearn> _transformSkillTree = new HashMap<>();
	private final Map<Integer, SkillLearn> _commonSkillTree = new HashMap<>();
	private final Map<Integer, SkillLearn> _nobleSkillTree = new HashMap<>();
	private final Map<Integer, SkillLearn> _heroSkillTree = new HashMap<>();
	private final Map<Integer, SkillLearn> _gameMasterSkillTree = new HashMap<>();
	private final Map<Integer, SkillLearn> _gameMasterAuraSkillTree = new HashMap<>();
	private final Map<Integer, List<SkillLearn>> _mutiProffSkills = new HashMap<>();
	private final Map<Integer, Map<Integer, SkillLearn>> _customSkillTree = new HashMap<>();

	private Map<Integer, int[]> _skillsByClassIdHashCodes;
	private Map<Integer, int[]> _skillsByRaceHashCodes;
	private final Map<Integer, ArrayList<Integer>> _restrictedSkills = new HashMap<>();
	private Map<Integer, Long> _totalSpConsumingForClassSkills;
	
	private int[] _allSkillsHashCodes;
	
	private boolean _loading = true;
	
	private final Map<ClassId, ClassId> _parentClassMap = new HashMap<>();
	
	protected SkillTreesParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_loading = true;
		_classSkillTrees.clear();
		_originalClassSkillIdTrees.clear();
		_originalClassSkillTrees.clear();
		_collectSkillTree.clear();
		_fishingSkillTree.clear();
		_pledgeSkillTree.clear();
		_subClassSkillTree.clear();
		_subPledgeSkillTree.clear();
		_transferSkillTrees.clear();
		_transformSkillTree.clear();
		_nobleSkillTree.clear();
		_heroSkillTree.clear();
		_gameMasterSkillTree.clear();
		_gameMasterAuraSkillTree.clear();
		_mutiProffSkills.clear();
		_customSkillTree.clear();
		final var isCompare = Config.COMPARE_SKILL_PRICE;
		if (isCompare)
		{
			_totalSpConsumingForClassSkills = new HashMap<>();
		}
		parseDirectory(new File(Config.DATAPACK_ROOT, "data/stats/skills/skillTrees/"));
		
		if (isCompare)
		{
			reassignSpClassSkillTree();
			_totalSpConsumingForClassSkills.clear();
			_totalSpConsumingForClassSkills = null;
		}
		generateCheckArrays();
		
		_loading = false;
		
		report();
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		NamedNodeMap attrs;
		Node attr;
		String type = null;
		int cId = -1;
		int groupId = 0;
		int parentClassId = -1;
		ClassId classId = null;
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("skillTree".equalsIgnoreCase(d.getNodeName()))
					{
						final Map<Integer, SkillLearn> classSkillTree = new HashMap<>();
						final Map<Integer, SkillLearn> trasferSkillTree = new HashMap<>();
						final Map<Integer, SkillLearn> customSkillTree = new HashMap<>();
						
						type = d.getAttributes().getNamedItem("type").getNodeValue();
						attr = d.getAttributes().getNamedItem("classId");
						groupId = d.getAttributes().getNamedItem("groupId") != null ? Integer.parseInt(d.getAttributes().getNamedItem("groupId").getNodeValue()) : 0;
						if (attr != null)
						{
							cId = Integer.parseInt(attr.getNodeValue());
							classId = ClassId.values()[cId];
						}
						else
						{
							cId = -1;
						}
						
						attr = d.getAttributes().getNamedItem("parentClassId");
						if (attr != null)
						{
							parentClassId = Integer.parseInt(attr.getNodeValue());
							if ((cId > -1) && (cId != parentClassId) && (parentClassId > -1) && !_parentClassMap.containsKey(classId))
							{
								_parentClassMap.put(classId, ClassId.values()[parentClassId]);
							}
						}
						
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("skill".equalsIgnoreCase(c.getNodeName()))
							{
								final StatsSet learnSkillSet = new StatsSet();
								attrs = c.getAttributes();
								for (int i = 0; i < attrs.getLength(); i++)
								{
									attr = attrs.item(i);
									learnSkillSet.set(attr.getNodeName(), attr.getNodeValue());
								}
								
								final SkillLearn skillLearn = new SkillLearn(learnSkillSet);
								for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
								{
									attrs = b.getAttributes();
									switch (b.getNodeName())
									{
										case "item" :
											skillLearn.addRequiredItem(new ItemHolder(parseInt(attrs, "id"), parseInt(attrs, "count")));
											break;
										case "preRequisiteSkill" :
											skillLearn.addPreReqSkill(new SkillHolder(parseInt(attrs, "id"), parseInt(attrs, "lvl")));
											break;
										case "race" :
											skillLearn.addRace(Race.valueOf(b.getTextContent()));
											break;
										case "residenceId" :
											skillLearn.addResidenceId(Integer.valueOf(b.getTextContent()));
											break;
										case "socialClass" :
											skillLearn.setSocialClass(Enum.valueOf(SocialClass.class, b.getTextContent()));
											break;
										case "subClassConditions" :
											skillLearn.addSubclassConditions(parseInt(attrs, "slot"), parseInt(attrs, "lvl"));
											break;
									}
								}
								
								if (type.equalsIgnoreCase("classSkillTree"))
								{
									if (Config.ALT_GAME_SKILL_LEARN && Config.ALT_GAME_SKILL_LEARN_ITEM_COSTS != null)
									{
										final var item = Config.ALT_GAME_SKILL_LEARN_ITEM_COSTS[classId.level()];
										if (item != null)
										{
											skillLearn.addRequiredItem(new ItemHolder(item[0], item[1]));
										}
									}
								}
								
								final int skillHashCode = SkillsParser.getSkillHashCode(skillLearn.getId(), skillLearn.getLvl());
								switch (type)
								{
									case "customSkillTree" :
									{
										customSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "classSkillTree" :
									{
										if (cId != -1)
										{
											classSkillTree.put(skillHashCode, skillLearn);
											if (_totalSpConsumingForClassSkills != null)
											{
												_totalSpConsumingForClassSkills.put(skillHashCode, Math.max(skillLearn.getLevelUpSp(), _totalSpConsumingForClassSkills.getOrDefault(skillHashCode, 0L)));
											}
										}
										else
										{
											_commonSkillTree.put(skillHashCode, skillLearn);
										}
										break;
									}
									case "transferSkillTree" :
									{
										trasferSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "collectSkillTree" :
									{
										_collectSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "fishingSkillTree" :
									{
										_fishingSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "pledgeSkillTree" :
									{
										_pledgeSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "subClassSkillTree" :
									{
										_subClassSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "subPledgeSkillTree" :
									{
										_subPledgeSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "transformSkillTree" :
									{
										_transformSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "nobleSkillTree" :
									{
										_nobleSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "heroSkillTree" :
									{
										_heroSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "gameMasterSkillTree" :
									{
										_gameMasterSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "gameMasterAuraSkillTree" :
									{
										_gameMasterAuraSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									default :
									{
										warn("Unknown Skill Tree type: " + type + "!");
									}
								}
							}
						}
						
						if (type.equals("transferSkillTree"))
						{
							_transferSkillTrees.put(classId, trasferSkillTree);
						}
						else if (type.equals("classSkillTree") && (cId > -1))
						{
							if (!_classSkillTrees.containsKey(classId))
							{
								_classSkillTrees.put(classId, classSkillTree);
							}
							else
							{
								_classSkillTrees.get(classId).putAll(classSkillTree);
							}
						}
						else if (type.equals("customSkillTree"))
						{
							if (!_customSkillTree.containsKey(groupId))
							{
								_customSkillTree.put(groupId, customSkillTree);
							}
							else
							{
								_customSkillTree.get(groupId).putAll(customSkillTree);
							}
						}
					}
					else if ("originalSkillTree".equalsIgnoreCase(d.getNodeName()))
					{
						final List<Integer> classSkillTree = new ArrayList<>();
						final Map<Integer, SkillLearn> classSkills = new HashMap<>();
						
						type = d.getAttributes().getNamedItem("type").getNodeValue();
						attr = d.getAttributes().getNamedItem("classId");
						if (attr != null)
						{
							cId = Integer.parseInt(attr.getNodeValue());
							classId = ClassId.values()[cId];
						}
						else
						{
							cId = -1;
						}
						
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("skill".equalsIgnoreCase(c.getNodeName()))
							{
								final StatsSet learnSkillSet = new StatsSet();
								attrs = c.getAttributes();
								for (int i = 0; i < attrs.getLength(); i++)
								{
									attr = attrs.item(i);
									learnSkillSet.set(attr.getNodeName(), attr.getNodeValue());
								}
								
								final SkillLearn skillLearn = new SkillLearn(learnSkillSet);
								
								final int skillId = Integer.parseInt(c.getAttributes().getNamedItem("skillId").getNodeValue());
								if (!classSkillTree.contains(skillId))
								{
									classSkillTree.add(skillId);
								}
								
								final int skillHashCode = SkillsParser.getSkillHashCode(skillLearn.getId(), skillLearn.getLvl());
								switch (type)
								{
									case "classSkillTree" :
									{
										if (cId != -1)
										{
											classSkills.put(skillHashCode, skillLearn);
										}
										break;
									}
								}
							}
						}
						
						if (type.equals("classSkillTree") && (cId > -1))
						{
							if (!_originalClassSkillTrees.containsKey(classId))
							{
								_originalClassSkillTrees.put(classId, classSkills);
							}
							else
							{
								_originalClassSkillTrees.get(classId).putAll(classSkills);
							}
							
							if (!_originalClassSkillIdTrees.containsKey(classId))
							{
								_originalClassSkillIdTrees.put(classId, classSkillTree);
							}
							else
							{
								_originalClassSkillIdTrees.get(classId).addAll(classSkillTree);
							}
						}
					}
				}
			}
		}
	}
	
	private void reassignSpClassSkillTree()
	{
		if (_totalSpConsumingForClassSkills == null || _totalSpConsumingForClassSkills.size() == 0)
		{
			return;
		}
		AcquireSkillType nonIgnoreAcquireType = null;
		for (final var acquireSkillType : AcquireSkillType.values())
		{
			if (Config.DISABLED_ITEMS_FOR_ACQUIRE_TYPES.isEmpty() || !Config.DISABLED_ITEMS_FOR_ACQUIRE_TYPES.contains(acquireSkillType))
			{
				nonIgnoreAcquireType = acquireSkillType;
				break;
			}
		}
		final Map<ClassId, Map<Integer, SkillLearn>> updatedMap = new HashMap<>(_classSkillTrees.size());
		for (final ClassId classId : _classSkillTrees.keySet())
		{
			final Map<Integer, SkillLearn> skillLearnMap = _classSkillTrees.getOrDefault(classId, Collections.emptyMap());
			if (skillLearnMap.size() == 0)
			{
				continue;
			}
			final Map<Integer, SkillLearn> updSkillLearn = new HashMap<>(skillLearnMap.size());
			for (final int skillHash : skillLearnMap.keySet())
			{
				SkillLearn skillLearn = skillLearnMap.get(skillHash);
				if (skillLearn.getLevelUpSp() < _totalSpConsumingForClassSkills.get(skillHash))
				{
					final StatsSet skillLearnSet = new StatsSet();
					skillLearnSet.set("skillName", skillLearn.getName());
					skillLearnSet.set("skillId", skillLearn.getId());
					skillLearnSet.set("skillLvl", skillLearn.getLvl());
					skillLearnSet.set("getLevel", skillLearn.getGetLevel());
					skillLearnSet.set("autoGet", skillLearn.isAutoGet());
					skillLearnSet.set("levelUpSp", _totalSpConsumingForClassSkills.get(skillHash));
					skillLearnSet.set("residenceSkill", skillLearn.isResidencialSkill());
					skillLearnSet.set("learnedByNpc", skillLearn.isLearnedByNpc());
					skillLearnSet.set("learnedByFS", skillLearn.isLearnedByFS());
					final List<ItemHolder> requiredItems = nonIgnoreAcquireType == null ? Collections.emptyList() : skillLearn.getRequiredItems(nonIgnoreAcquireType);
					final var races = skillLearn.getRaces();
					final var requiredSkills = skillLearn.getPreReqSkills();
					final var requiredSocialClass = skillLearn.getSocialClass();
					final var residencesId = skillLearn.getResidenceIds();
					final var subClassConditions = skillLearn.getSubClassConditions();
					
					skillLearn = new SkillLearn(skillLearnSet);
					
					for (final var requiredItem : requiredItems)
					{
						skillLearn.addRequiredItem(requiredItem);
					}
					for (final var requiredRace : races)
					{
						skillLearn.addRace(requiredRace);
					}
					for (final var requiredSkill : requiredSkills)
					{
						skillLearn.addPreReqSkill(requiredSkill);
					}
					skillLearn.setSocialClass(requiredSocialClass);
					for (final int residenceId : residencesId)
					{
						skillLearn.addResidenceId(residenceId);
					}
					for (final var condition : subClassConditions)
					{
						if (condition == null)
						{
							continue;
						}
						skillLearn.addSubclassConditions(condition.getSlot(), condition.getLvl());
					}
				}
				updSkillLearn.put(skillHash, skillLearn);
			}
			updatedMap.put(classId, updSkillLearn);
		}
		_classSkillTrees.clear();
		_classSkillTrees.putAll(updatedMap);
	}
	
	public Map<Integer, SkillLearn> getCompleteClassSkillTree(ClassId classId)
	{
		final Map<Integer, SkillLearn> skillTree = new HashMap<>();
		skillTree.putAll(_commonSkillTree);
		while ((classId != null) && (_classSkillTrees.get(classId) != null))
		{
			skillTree.putAll(_classSkillTrees.get(classId));
			classId = _parentClassMap.get(classId);
		}
		return skillTree;
	}
	
	public Map<Integer, SkillLearn> getAllClassSkillTree(ClassId classId)
	{
		final Map<Integer, SkillLearn> skillTree = new HashMap<>();
		skillTree.putAll(_commonSkillTree);
		if (_classSkillTrees.get(classId) != null)
		{
			skillTree.putAll(_classSkillTrees.get(classId));
			if (classId.level() == 1)
			{
				classId = _parentClassMap.get(classId);
				if (classId != null)
				{
					skillTree.putAll(_classSkillTrees.get(classId));
				}
			}
		}
		return skillTree;
	}
	
	public List<Integer> getAllOriginalClassSkillIdTree(ClassId classId)
	{
		final List<Integer> skillTree = new ArrayList<>();
		while ((classId != null) && (_originalClassSkillIdTrees.get(classId) != null))
		{
			skillTree.addAll(_originalClassSkillIdTrees.get(classId));
			classId = _parentClassMap.get(classId);
		}
		return skillTree;
	}
	
	public Map<Integer, SkillLearn> getAllOriginalClassSkillTree(ClassId classId)
	{
		final Map<Integer, SkillLearn> skillTree = new HashMap<>();
		while ((classId != null) && (_originalClassSkillTrees.get(classId) != null))
		{
			skillTree.putAll(_originalClassSkillTrees.get(classId));
			classId = _parentClassMap.get(classId);
		}
		return skillTree;
	}
	
	public Map<Integer, SkillLearn> getTransferSkillTree(ClassId classId)
	{
		if (classId.level() >= 3)
		{
			return getTransferSkillTree(classId.getParent());
		}
		return _transferSkillTrees.get(classId);
	}
	
	public Map<Integer, SkillLearn> getCommonSkillTree()
	{
		return _commonSkillTree;
	}
	
	public Map<Integer, SkillLearn> getCollectSkillTree()
	{
		return _collectSkillTree;
	}
	
	public Map<Integer, SkillLearn> getFishingSkillTree()
	{
		return _fishingSkillTree;
	}
	
	public Map<Integer, SkillLearn> getPledgeSkillTree()
	{
		return _pledgeSkillTree;
	}
	
	public Map<Integer, SkillLearn> getSubClassSkillTree()
	{
		return _subClassSkillTree;
	}
	
	public Map<Integer, SkillLearn> getSubPledgeSkillTree()
	{
		return _subPledgeSkillTree;
	}
	
	public Map<Integer, SkillLearn> getTransformSkillTree()
	{
		return _transformSkillTree;
	}
	
	public Map<Integer, Skill> getNobleSkillTree()
	{
		final Map<Integer, Skill> tree = new HashMap<>();
		final SkillsParser st = SkillsParser.getInstance();
		for (final Entry<Integer, SkillLearn> e : _nobleSkillTree.entrySet())
		{
			tree.put(e.getKey(), st.getInfo(e.getValue().getId(), e.getValue().getLvl()));
		}
		return tree;
	}
	
	public Map<Integer, Skill> getHeroSkillTree()
	{
		final Map<Integer, Skill> tree = new HashMap<>();
		final SkillsParser st = SkillsParser.getInstance();
		for (final Entry<Integer, SkillLearn> e : _heroSkillTree.entrySet())
		{
			tree.put(e.getKey(), st.getInfo(e.getValue().getId(), e.getValue().getLvl()));
		}
		return tree;
	}
	
	public Map<Integer, Skill> getGMSkillTree()
	{
		final Map<Integer, Skill> tree = new HashMap<>();
		final SkillsParser st = SkillsParser.getInstance();
		for (final Entry<Integer, SkillLearn> e : _gameMasterSkillTree.entrySet())
		{
			tree.put(e.getKey(), st.getInfo(e.getValue().getId(), e.getValue().getLvl()));
		}
		return tree;
	}
	
	public Map<Integer, Skill> getGMAuraSkillTree()
	{
		final Map<Integer, Skill> tree = new HashMap<>();
		final SkillsParser st = SkillsParser.getInstance();
		for (final Entry<Integer, SkillLearn> e : _gameMasterAuraSkillTree.entrySet())
		{
			tree.put(e.getKey(), st.getInfo(e.getValue().getId(), e.getValue().getLvl()));
		}
		return tree;
	}
	
	public List<SkillLearn> getAvailableSkills(Player player, ClassId classId, boolean includeByFs, boolean includeAutoGet)
	{
		final List<SkillLearn> result = new ArrayList<>();
		final Map<Integer, SkillLearn> skills = getCompleteClassSkillTree(classId);
		if (skills.isEmpty())
		{
			warn("Skilltree for class " + classId + " is not defined!");
			return result;
		}
		
		for (final SkillLearn skill : skills.values())
		{
			if (skill.getGetLevel() > Config.PLAYER_MAXIMUM_LEVEL)
			{
				continue;
			}
			
			final var isAutoget = (includeAutoGet && !skill.isBlockAutoGet()) || (includeByFs && skill.isLearnedByFS());
			if (((includeAutoGet && skill.isAutoGet()) || (includeByFs && skill.isLearnedByFS()) || skill.isLearnedByNpc()) && (player.getLevel() >= skill.getGetLevel()))
			{
				final var oldSkill = player.getKnownSkill(skill.getId());
				if (isAutoget)
				{
					if (oldSkill == null || (oldSkill != null && oldSkill.getLevel() < skill.getLvl()))
					{
						result.add(skill);
					}
				}
				else
				{
					if (oldSkill != null)
					{
						if (oldSkill.getLevel() == (skill.getLvl() - 1))
						{
							result.add(skill);
						}
					}
					else if (skill.getLvl() == 1)
					{
						result.add(skill);
					}
				}
			}
		}
		return result;
	}
	
	public Collection<Skill> getAllAvailableSkills(Player player, ClassId classId, boolean includeByFs, boolean includeAutoGet)
	{
		final Map<Integer, Skill> skillList = new HashMap<>();
		final var learnable = getAvailableSkills(player, classId, includeByFs, includeAutoGet);
		if (learnable.isEmpty())
		{
			return Collections.emptyList();
		}
		
		for (final var skillLearn : learnable)
		{
			final var skill = SkillsParser.getInstance().getInfo(skillLearn.getId(), skillLearn.getLvl());
			if ((skill == null) || ((skill.getId() == Skill.SKILL_DIVINE_INSPIRATION) && !Config.AUTO_LEARN_DIVINE_INSPIRATION))
			{
				continue;
			}
			
			final var sk = skillList.get(skill.getId());
			if (sk == null || sk.getLevel() < skill.getLevel())
			{
				skillList.put(skill.getId(), skill);
			}
		}
		return skillList.values();
	}
	
	public List<SkillLearn> getAvailableAutoGetSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		final Map<Integer, SkillLearn> skills = getCompleteClassSkillTree(player.getClassId());
		if (skills.isEmpty())
		{
			warn("Skill Tree for this class Id(" + player.getClassId() + ") is not defined!");
			return result;
		}
		
		final Race race = player.getRace();
		for (final SkillLearn skill : skills.values())
		{
			if (!skill.getRaces().isEmpty() && !skill.getRaces().contains(race))
			{
				continue;
			}
			
			if (skill.isAutoGet() && (player.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = player.getSkills().get(skill.getId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() < skill.getLvl())
					{
						result.add(skill);
					}
				}
				else
				{
					result.add(skill);
				}
			}
		}
		return result;
	}
	
	public List<SkillLearn> getAvailableFishingSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		final Race playerRace = player.getRace();
		for (final SkillLearn skill : _fishingSkillTree.values())
		{
			if (!skill.getRaces().isEmpty() && !skill.getRaces().contains(playerRace))
			{
				continue;
			}
			
			if (skill.isLearnedByNpc() && (player.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = player.getSkills().get(skill.getId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getLvl() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getLvl() == 1)
				{
					result.add(skill);
				}
			}
		}
		return result;
	}
	
	public List<SkillLearn> getAvailableCollectSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (final SkillLearn skill : _collectSkillTree.values())
		{
			final Skill oldSkill = player.getSkills().get(skill.getId());
			if (oldSkill != null)
			{
				if (oldSkill.getLevel() == (skill.getLvl() - 1))
				{
					result.add(skill);
				}
			}
			else if (skill.getLvl() == 1)
			{
				result.add(skill);
			}
		}
		return result;
	}
	
	public List<SkillLearn> getAvailableTransferSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		ClassId classId = player.getClassId();
		
		if (classId.level() == 3)
		{
			classId = classId.getParent();
		}
		
		if (!_transferSkillTrees.containsKey(classId))
		{
			return result;
		}
		
		for (final SkillLearn skill : _transferSkillTrees.get(classId).values())
		{
			if (player.getKnownSkill(skill.getId()) == null)
			{
				result.add(skill);
			}
		}
		return result;
	}
	
	public List<SkillLearn> getAvailableTransformSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		final Race race = player.getRace();
		for (final SkillLearn skill : _transformSkillTree.values())
		{
			if ((player.getLevel() >= skill.getGetLevel()) && (skill.getRaces().isEmpty() || skill.getRaces().contains(race)))
			{
				final Skill oldSkill = player.getSkills().get(skill.getId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getLvl() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getLvl() == 1)
				{
					result.add(skill);
				}
			}
		}
		return result;
	}
	
	public List<SkillLearn> getAvailablePledgeSkills(Clan clan)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (final SkillLearn skill : _pledgeSkillTree.values())
		{
			if (!skill.isResidencialSkill() && (clan.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = clan.getSkills().get(skill.getId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getLvl() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getLvl() == 1)
				{
					result.add(skill);
				}
			}
		}
		return result;
	}
	
	public List<SkillLearn> getAvailableSubPledgeSkills(Clan clan)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (final SkillLearn skill : _subPledgeSkillTree.values())
		{
			if ((clan.getLevel() >= skill.getGetLevel()) && clan.isLearnableSubSkill(skill.getId(), skill.getLvl()))
			{
				result.add(skill);
			}
		}
		return result;
	}
	
	public List<SkillLearn> getAvailableSubClassSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (final SkillLearn skill : _subClassSkillTree.values())
		{
			if (player.getLevel() >= skill.getGetLevel())
			{
				List<SubClassData> subClassConds = null;
				for (final SubClass subClass : player.getSubClasses().values())
				{
					subClassConds = skill.getSubClassConditions();
					if (!subClassConds.isEmpty() && (subClass.getClassIndex() <= subClassConds.size()) && (subClass.getClassIndex() == subClassConds.get(subClass.getClassIndex() - 1).getSlot()) && (subClassConds.get(subClass.getClassIndex() - 1).getLvl() <= subClass.getLevel()))
					{
						final Skill oldSkill = player.getSkills().get(skill.getId());
						if (oldSkill != null)
						{
							if (oldSkill.getLevel() == (skill.getLvl() - 1))
							{
								result.add(skill);
							}
						}
						else if (skill.getLvl() == 1)
						{
							result.add(skill);
						}
					}
				}
			}
		}
		return result;
	}
	
	public boolean isSubClassSkill(int skillId)
	{
		for (final SkillLearn skill : _subClassSkillTree.values())
		{
			if (skill != null && skill.getId() == skillId)
			{
				return true;
			}
		}
		return false;
	}
	
	public List<SkillLearn> getAvailableResidentialSkills(int residenceId)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (final SkillLearn skill : _pledgeSkillTree.values())
		{
			if (skill.isResidencialSkill() && skill.getResidenceIds().contains(residenceId))
			{
				result.add(skill);
			}
		}
		return result;
	}
	
	public Map<Integer, SkillLearn> getCustomSkills(int groupId)
	{
		if (_customSkillTree.containsKey(groupId))
		{
			return _customSkillTree.get(groupId);
		}
		else
		{
			_customSkillTree.put(groupId, new HashMap<>());
			return _customSkillTree.get(groupId);
		}
	}
	
	public List<SkillLearn> getAvailableCustomSkills(Player player, int groupId)
	{
		final List<SkillLearn> result = new ArrayList<>();
		if (_customSkillTree.containsKey(groupId))
		{
			for (final SkillLearn skill : _customSkillTree.get(groupId).values())
			{
				final Skill oldSkill = player.getSkills().get(skill.getId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getLvl() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getLvl() == 1)
				{
					result.add(skill);
				}
			}
		}
		return result;
	}
	
	public SkillLearn getSkillLearn(AcquireSkillType skillType, int id, int lvl, Player player)
	{
		SkillLearn sl = null;
		switch (skillType)
		{
			case CLASS :
				sl = getClassSkill(id, lvl, player.getLearningClass());
				break;
			case TRANSFORM :
				sl = getTransformSkill(id, lvl);
				break;
			case FISHING :
				sl = getFishingSkill(id, lvl);
				break;
			case PLEDGE :
				sl = getPledgeSkill(id, lvl);
				break;
			case SUBPLEDGE :
				sl = getSubPledgeSkill(id, lvl);
				break;
			case TRANSFER :
				sl = getTransferSkill(id, lvl, player.getClassId());
				break;
			case SUBCLASS :
				sl = getSubClassSkill(id, lvl);
				break;
			case COLLECT :
				sl = getCollectSkill(id, lvl);
				break;
			case CUSTOM :
				sl = getCustomSkill(id, lvl, player.getLearningGroupId());
				break;
		}
		return sl;
	}
	
	public SkillLearn getTransformSkill(int id, int lvl)
	{
		return _transformSkillTree.get(SkillsParser.getSkillHashCode(id, lvl));
	}
	
	public SkillLearn getClassSkill(int id, int lvl, ClassId classId)
	{
		return getCompleteClassSkillTree(classId).get(SkillsParser.getSkillHashCode(id, lvl));
	}
	
	public SkillLearn getFishingSkill(int id, int lvl)
	{
		return _fishingSkillTree.get(SkillsParser.getSkillHashCode(id, lvl));
	}
	
	public SkillLearn getPledgeSkill(int id, int lvl)
	{
		return _pledgeSkillTree.get(SkillsParser.getSkillHashCode(id, lvl));
	}
	
	public SkillLearn getSubPledgeSkill(int id, int lvl)
	{
		return _subPledgeSkillTree.get(SkillsParser.getSkillHashCode(id, lvl));
	}
	
	public SkillLearn getTransferSkill(int id, int lvl, ClassId classId)
	{
		if (classId.getParent() != null)
		{
			final ClassId parentId = classId.getParent();
			if (_transferSkillTrees.get(parentId) != null)
			{
				return _transferSkillTrees.get(parentId).get(SkillsParser.getSkillHashCode(id, lvl));
			}
		}
		return null;
	}
	
	public SkillLearn getSubClassSkill(int id, int lvl)
	{
		return _subClassSkillTree.get(SkillsParser.getSkillHashCode(id, lvl));
	}
	
	public SkillLearn getCommonSkill(int id, int lvl)
	{
		return _commonSkillTree.get(SkillsParser.getSkillHashCode(id, lvl));
	}
	
	public SkillLearn getCollectSkill(int id, int lvl)
	{
		return _collectSkillTree.get(SkillsParser.getSkillHashCode(id, lvl));
	}
	
	public SkillLearn getCustomSkill(int id, int lvl, int groupId)
	{
		if (_customSkillTree.containsKey(groupId))
		{
			return _customSkillTree.get(groupId).get(SkillsParser.getSkillHashCode(id, lvl));
		}
		return null;
	}
	
	public int getMinLevelForNewSkill(Player player, Map<Integer, SkillLearn> skillTree)
	{
		int minLevel = 0;
		if (skillTree.isEmpty())
		{
			warn("SkillTree is not defined for getMinLevelForNewSkill!");
		}
		else
		{
			for (final SkillLearn s : skillTree.values())
			{
				if (s.isLearnedByNpc() && (player.getLevel() < s.getGetLevel()))
				{
					if ((minLevel == 0) || (minLevel > s.getGetLevel()))
					{
						minLevel = s.getGetLevel();
					}
				}
			}
		}
		return minLevel;
	}
	
	public boolean isNotCheckSkill(Player player, int id, int lvl)
	{
		if (lvl >= 100)
		{
			lvl = SkillsParser.getInstance().getMaxLevel(id);
		}
		
		SkillLearn sl = getTransformSkill(id, lvl);
		if (sl != null && player.isTransformed())
		{
			return true;
		}
		
		if (isGMSkill(id, lvl) && player.isGM())
		{
			return true;
		}
		
		sl = getFishingSkill(id, lvl);
		if (sl != null)
		{
			return true;
		}
		
		sl = getCommonSkill(id, lvl);
		if (sl != null)
		{
			return true;
		}
		
		sl = getPledgeSkill(id, lvl);
		if (sl != null && player.getClan() != null)
		{
			return true;
		}
		
		sl = getSubPledgeSkill(id, lvl);
		if (sl != null && player.getClan() != null)
		{
			return true;
		}
		
		sl = getTransferSkill(id, lvl, player.getClassId());
		if (sl != null)
		{
			return true;
		}
		
		sl = getSubClassSkill(id, lvl);
		if (sl != null)
		{
			return true;
		}
		
		sl = getCollectSkill(id, lvl);
		if (sl != null)
		{
			return true;
		}
		
		sl = getNobleSkill(id, lvl);
		if (sl != null && player.isNoble())
		{
			return true;
		}
		
		if (isHeroSkill(id, lvl) && player.isHero())
		{
			return true;
		}
		return false;
	}
	
	public SkillLearn getNobleSkill(int id, int lvl)
	{
		return _nobleSkillTree.get(SkillsParser.getSkillHashCode(id, lvl));
	}
	
	public boolean isHeroSkill(int skillId, int skillLevel)
	{
		if (_heroSkillTree.containsKey(SkillsParser.getSkillHashCode(skillId, skillLevel)))
		{
			return true;
		}
		
		for (final SkillLearn skill : _heroSkillTree.values())
		{
			if ((skill.getId() == skillId) && (skillLevel == -1))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isGMSkill(int skillId, int skillLevel)
	{
		final Map<Integer, SkillLearn> gmSkills = new HashMap<>();
		gmSkills.putAll(_gameMasterSkillTree);
		gmSkills.putAll(_gameMasterAuraSkillTree);
		if (gmSkills.containsKey(SkillsParser.getSkillHashCode(skillId, skillLevel)))
		{
			return true;
		}
		
		for (final SkillLearn skill : gmSkills.values())
		{
			if ((skill.getId() == skillId) && (skillLevel == -1))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isClanSkill(int skillId, int skillLevel)
	{
		final int hashCode = SkillsParser.getSkillHashCode(skillId, skillId);
		if (ClanParser.getInstance().isClanSkill(skillId))
		{
			return true;
		}
		return _pledgeSkillTree.containsKey(hashCode) || _subPledgeSkillTree.containsKey(hashCode);
	}
	
	public void addSkills(Player gmchar, boolean auraSkills)
	{
		final Collection<SkillLearn> skills = auraSkills ? _gameMasterAuraSkillTree.values() : _gameMasterSkillTree.values();
		final SkillsParser st = SkillsParser.getInstance();
		for (final SkillLearn sl : skills)
		{
			gmchar.addSkill(st.getInfo(sl.getId(), sl.getLvl()), false);
		}
	}
	
	private void generateCheckArrays()
	{
		int i;
		int[] array;
		
		Map<Integer, SkillLearn> tempMap;
		final Set<ClassId> keySet = _classSkillTrees.keySet();
		_skillsByClassIdHashCodes = new HashMap<>(keySet.size());
		for (final ClassId cls : keySet)
		{
			i = 0;
			tempMap = getCompleteClassSkillTree(cls);
			array = new int[tempMap.size()];
			for (final int h : tempMap.keySet())
			{
				array[i++] = h;
			}
			tempMap.clear();
			Arrays.sort(array);
			_skillsByClassIdHashCodes.put(cls.ordinal(), array);
		}
		
		final List<Integer> list = new ArrayList<>();
		_skillsByRaceHashCodes = new HashMap<>(Race.values().length);
		for (final Race r : Race.values())
		{
			for (final SkillLearn s : _fishingSkillTree.values())
			{
				if (s.getRaces().contains(r))
				{
					list.add(SkillsParser.getSkillHashCode(s.getId(), s.getLvl()));
				}
			}
			
			for (final SkillLearn s : _transformSkillTree.values())
			{
				if (s.getRaces().contains(r))
				{
					list.add(SkillsParser.getSkillHashCode(s.getId(), s.getLvl()));
				}
			}
			
			i = 0;
			array = new int[list.size()];
			for (final int s : list)
			{
				array[i++] = s;
			}
			Arrays.sort(array);
			_skillsByRaceHashCodes.put(r.ordinal(), array);
			list.clear();
		}
		
		for (final SkillLearn s : _commonSkillTree.values())
		{
			if (s.getRaces().isEmpty())
			{
				list.add(SkillsParser.getSkillHashCode(s.getId(), s.getLvl()));
			}
		}
		
		for (final SkillLearn s : _fishingSkillTree.values())
		{
			if (s.getRaces().isEmpty())
			{
				list.add(SkillsParser.getSkillHashCode(s.getId(), s.getLvl()));
			}
		}
		
		for (final SkillLearn s : _transformSkillTree.values())
		{
			if (s.getRaces().isEmpty())
			{
				list.add(SkillsParser.getSkillHashCode(s.getId(), s.getLvl()));
			}
		}
		
		for (final SkillLearn s : _collectSkillTree.values())
		{
			list.add(SkillsParser.getSkillHashCode(s.getId(), s.getLvl()));
		}
		
		_allSkillsHashCodes = new int[list.size()];
		int j = 0;
		for (final int hashcode : list)
		{
			_allSkillsHashCodes[j++] = hashcode;
		}
		Arrays.sort(_allSkillsHashCodes);
	}
	
	public boolean isSkillAllowed(Player player, Skill skill)
	{
		if (skill.isExcludedFromCheck())
		{
			return true;
		}
		
		if (player.isGM() && skill.isGMSkill())
		{
			return true;
		}
		
		if (_loading)
		{
			return true;
		}
		
		if (isNotCheckSkill(player, skill.getId(), skill.getLevel()))
		{
			return true;
		}
		
		final int maxLvl = SkillsParser.getInstance().getMaxLevel(skill.getId());
		final int hashCode = SkillsParser.getSkillHashCode(skill.getId(), Math.min(skill.getLevel(), maxLvl));
		
		if (Arrays.binarySearch(_skillsByClassIdHashCodes.get(player.getClassId().ordinal()), hashCode) >= 0)
		{
			return true;
		}
		
		if (Arrays.binarySearch(_skillsByRaceHashCodes.get(player.getRace().ordinal()), hashCode) >= 0)
		{
			return true;
		}
		
		if (Arrays.binarySearch(_allSkillsHashCodes, hashCode) >= 0)
		{
			return true;
		}
		
		if (getTransferSkill(skill.getId(), Math.min(skill.getLevel(), maxLvl), player.getClassId()) != null)
		{
			return true;
		}
		return false;
	}
	
	private void report()
	{
		int classSkillTreeCount = 0;
		for (final Map<Integer, SkillLearn> classSkillTree : _classSkillTrees.values())
		{
			classSkillTreeCount += classSkillTree.size();
		}
		
		int customSkillTreeCount = 0;
		for (final Map<Integer, SkillLearn> customSkillTree : _customSkillTree.values())
		{
			customSkillTreeCount += customSkillTree.size();
		}
		
		int trasferSkillTreeCount = 0;
		for (final Map<Integer, SkillLearn> trasferSkillTree : _transferSkillTrees.values())
		{
			trasferSkillTreeCount += trasferSkillTree.size();
		}
		
		int dwarvenOnlyFishingSkillCount = 0;
		for (final SkillLearn fishSkill : _fishingSkillTree.values())
		{
			if (fishSkill.getRaces().contains(Race.Dwarf))
			{
				dwarvenOnlyFishingSkillCount++;
			}
		}
		
		int resSkillCount = 0;
		for (final SkillLearn pledgeSkill : _pledgeSkillTree.values())
		{
			if (pledgeSkill.isResidencialSkill())
			{
				resSkillCount++;
			}
		}
		
		if (Config.DEBUG)
		{
			info("Loaded " + classSkillTreeCount + " Class Skills for " + _classSkillTrees.size() + " Class Skill Trees.");
			if (_originalClassSkillTrees.size() > 0)
			{
				info("Loaded " + classSkillTreeCount + " Original Class Skills for " + _originalClassSkillTrees.size() + " Class Skill Trees.");
			}
			info("Loaded " + _subClassSkillTree.size() + " Sub-Class Skills.");
			info("Loaded " + trasferSkillTreeCount + " Transfer Skills for " + _transferSkillTrees.size() + " Transfer Skill Trees.");
			info("Loaded " + _fishingSkillTree.size() + " Fishing Skills, " + dwarvenOnlyFishingSkillCount + " Dwarven only Fishing Skills.");
			info("Loaded " + _collectSkillTree.size() + " Collect Skills.");
			info("Loaded " + _pledgeSkillTree.size() + " Pledge Skills, " + (_pledgeSkillTree.size() - resSkillCount) + " for Pledge and " + resSkillCount + " Residential.");
			info("Loaded " + _subPledgeSkillTree.size() + " Sub-Pledge Skills.");
			info("Loaded " + _transformSkillTree.size() + " Transform Skills.");
			info("Loaded " + _nobleSkillTree.size() + " Noble Skills.");
			info("Loaded " + _heroSkillTree.size() + " Hero Skills.");
			info("Loaded " + _gameMasterSkillTree.size() + " Game Master Skills.");
			info("Loaded " + _gameMasterAuraSkillTree.size() + " Game Master Aura Skills.");
			final int commonSkills = _commonSkillTree.size();
			if (commonSkills > 0)
			{
				info("Loaded " + commonSkills + " Common Skills to all classes.");
			}
			
			if (_customSkillTree.size() > 0)
			{
				info("Loaded " + customSkillTreeCount + " custom skills for " + _customSkillTree.size() + " group Skill Trees.");
			}
		}
		loadRestrictedSkills();
		collectAllClassesSkill();
	}
	
	private void loadRestrictedSkills()
	{
		final Map<Integer, ArrayList<Integer>> allowedSkillIds = new HashMap<>();
		
		for (final ClassId classid : ClassId.values())
		{
			if (classid.getRace() == null)
			{
				continue;
			}
			
			final Map<Integer, SkillLearn> skills = getCompleteClassSkillTree(classid);
			final ArrayList<Integer> skillIds = new ArrayList<>();
			
			for (final SkillLearn sk : skills.values())
			{
				if (!skillIds.contains(sk.getId()))
				{
					skillIds.add(sk.getId());
				}
			}
			allowedSkillIds.put(classid.getId(), skillIds);
		}
		for (final ClassId classId : ClassId.values())
		{
			if (classId.getRace() == null)
			{
				continue;
			}
			final ArrayList<Integer> skillIds = new ArrayList<>();
			for (final ClassId classid : ClassId.values())
			{
				if ((classid == classId) || (classid.getRace() == null) || classId.childOf(classid))
				{
					continue;
				}
				for (final Integer skillId : allowedSkillIds.get(classid.getId()))
				{
					if (!skillIds.contains(skillId))
					{
						skillIds.add(skillId);
					}
				}
			}
			for (final Integer skillId : allowedSkillIds.get(classId.getId()))
			{
				if (skillIds.contains(skillId))
				{
					skillIds.remove(skillId);
				}
			}
			_restrictedSkills.put(classId.getId(), skillIds);
		}

		if (Config.DEBUG)
		{
			info("Loaded " + _restrictedSkills.size() + " classes with restricted skill(s).");
		}
	}
	
	private void collectAllClassesSkill()
	{
		for (final var skills : _classSkillTrees.values())
		{
			if (skills != null)
			{
				for (final SkillLearn skill : skills.values())
				{
					if (skill != null)
					{
						if (!_mutiProffSkills.isEmpty())
						{
							for (final var list : _mutiProffSkills.values())
							{
								for (final var sk : list)
								{
									if ((sk.getId() == skill.getId()) && (sk.getLvl() == skill.getLvl()))
									{
										continue;
									}
								}
							}
						}
						if (!_mutiProffSkills.containsKey(skill.getId()))
						{
							_mutiProffSkills.put(skill.getId(), new LinkedList<>());
						}
						_mutiProffSkills.get(skill.getId()).add(skill);
					}
				}
			}
		}
		
		if (Config.DEBUG)
		{
			info("Loaded " + _mutiProffSkills.size() + " multiproff skills.");
		}
	}
	
	public boolean checkClassesSkill(Player player, int id, int lvl)
	{
		final var list = _mutiProffSkills.get(id);
		if (list == null || list.isEmpty())
		{
			return false;
		}
		
		for (final var skill : list)
		{
			if (skill != null && skill.getLvl() == lvl)
			{
				if (lvl == 1)
				{
					return true;
				}
				else
				{
					final var oldSkill = player.getKnownSkill(id);
					if (oldSkill != null)
					{
						if (oldSkill.getLevel() == (skill.getLvl() - 1))
						{
							return true;
						}
					}
				}
				return false;
			}
		}
		return false;
	}
	
	public List<SkillLearn> checkMultiSkill(Player player, ClassId classId, int skillId)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (final SkillLearn skill : SkillTreesParser.getInstance().getAllClassSkillTree(classId).values())
		{
			if (skill != null)
			{
				if (skill.getId() != skillId)
				{
					continue;
				}
				
				if (_mutiProffSkills.containsKey(skill.getId()) && !result.contains(skill))
				{
					result.add(skill);
				}
			}
		}
		return result;
	}
	
	public boolean isAllowMultiSkill(int skillId)
	{
		return _mutiProffSkills.containsKey(skillId);
	}
	
	public boolean checkClanSkill(Player player, int id, int lvl)
	{
		if (player.getClan() == null || id <= 0 || lvl <= 0)
		{
			return false;
		}
		
		for (final SkillLearn skill : getAvailablePledgeSkills(player.getClan()))
		{
			if (skill != null)
			{
				if ((skill.getId() == id) && (skill.getLvl() == lvl))
				{
					if (lvl == 1)
					{
						return true;
					}
					else
					{
						final Skill oldSkill = player.getClan().getKnownSkill(id);
						if (oldSkill != null)
						{
							if (oldSkill.getLevel() == (skill.getLvl() - 1))
							{
								return true;
							}
						}
					}
					return false;
				}
			}
		}
		return false;
	}
	
	public boolean checkValidClassSkills(int id, ClassId classId)
	{
		for (final SkillLearn skill : SkillTreesParser.getInstance().getAllClassSkillTree(classId).values())
		{
			if (skill != null && skill.getId() == id)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean checkAllValidClassSkills(int id, ClassId classId)
	{
		for (final SkillLearn skill : SkillTreesParser.getInstance().getCompleteClassSkillTree(classId).values())
		{
			if (skill != null && skill.getId() == id)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean checkLimitClassSkills(Player player, Skill sk, int activeLimit, int passiveLimit, int chanceLimit)
	{
		if (sk == null || player == null)
		{
			return false;
		}
		
		if (player.getKnownSkill(sk.getId()) != null)
		{
			return true;
		}
		
		var activeSkills = 0;
		var passiveSkills = 0;
		var chanceSkills = 0;
		for (final var skill : player.getAllSkills())
		{
			if (skill.isCustom() || isNotCheckSkill(player, skill.getId(), skill.getLevel()) || checkAllValidClassSkills(skill.getId(), player.getClassId()) || skill.isItemSkill())
			{
				continue;
			}
			
			if (skill.isTriggeredSkill() || skill.isChance())
			{
				chanceSkills++;
			}
			else if (skill.isPassive() && !skill.isTriggeredSkill() && !skill.isChance())
			{
				passiveSkills++;
			}
			else
			{
				activeSkills++;
			}
		}
		
		if (sk.isTriggeredSkill() || sk.isChance())
		{
			if (chanceSkills >= chanceLimit)
			{
				player.sendMessage("You have exceeded limit of learning chance skills! Limit - " + chanceLimit);
				return false;
			}
			return true;
		}
		
		if (sk.isPassive() && !sk.isTriggeredSkill() && !sk.isChance())
		{
			if (passiveSkills >= passiveLimit)
			{
				player.sendMessage("You have exceeded limit of learning passive skills! Limit - " + passiveLimit);
				return false;
			}
			return true;
		}
		
		if (activeSkills >= activeLimit)
		{
			player.sendMessage("You have exceeded limit of learning active skills! Limit - " + activeLimit);
			return false;
		}
		return true;
	}
	
	public ArrayList<Integer> getRestrictedSkills(ClassId classId)
	{
		return _restrictedSkills.get(classId.getId());
	}
	
	public static SkillTreesParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillTreesParser _instance = new SkillTreesParser();
	}
}