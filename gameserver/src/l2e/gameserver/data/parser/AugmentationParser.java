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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.actor.templates.items.GemStone;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.LifeStone;
import l2e.gameserver.model.actor.templates.items.LifeStoneGrade;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.options.Options;
import l2e.gameserver.model.stats.StatsSet;

public class AugmentationParser extends LoggerObject
{
	private static final int STAT_BLOCKSIZE = 3640;
	private static final int STAT_SUBBLOCKSIZE = 91;
	public static final int MIN_SKILL_ID = STAT_BLOCKSIZE * 4;
	
	private static final int BLUE_START = 14561;
	private static final int SKILLS_BLOCKSIZE = 178;
	
	private static final int BASESTAT_STR = 16341;
	private static final int BASESTAT_MEN = 16344;
	
	private static final int ACC_START = 16669;
	private static final int ACC_BLOCKS_NUM = 10;
	private static final int ACC_STAT_SUBBLOCKSIZE = 21;
	
	private static final int ACC_RING_START = ACC_START;
	private static final int ACC_RING_SKILLS = 18;
	private static final int ACC_RING_BLOCKSIZE = ACC_RING_SKILLS + (4 * ACC_STAT_SUBBLOCKSIZE);
	private static final int ACC_RING_END = (ACC_RING_START + (ACC_BLOCKS_NUM * ACC_RING_BLOCKSIZE)) - 1;
	
	private static final int ACC_EAR_START = ACC_RING_END + 1;
	private static final int ACC_EAR_SKILLS = 18;
	private static final int ACC_EAR_BLOCKSIZE = ACC_EAR_SKILLS + (4 * ACC_STAT_SUBBLOCKSIZE);
	private static final int ACC_EAR_END = (ACC_EAR_START + (ACC_BLOCKS_NUM * ACC_EAR_BLOCKSIZE)) - 1;
	
	private static final int ACC_NECK_START = ACC_EAR_END + 1;
	private static final int ACC_NECK_SKILLS = 24;
	private static final int ACC_NECK_BLOCKSIZE = ACC_NECK_SKILLS + (4 * ACC_STAT_SUBBLOCKSIZE);
	
	private final List<List<Integer>> _blueSkills = new ArrayList<>(10);
	private final List<List<Integer>> _purpleSkills = new ArrayList<>(10);
	private final List<List<Integer>> _redSkills = new ArrayList<>(10);
	private final List<List<Integer>> _yellowSkills = new ArrayList<>(10);
	
	private final List<AugmentationChance> _augmentationWeaponChances = new ArrayList<>();
	private final List<AugmentationChance> _augmentationArmorChances = new ArrayList<>();
	private final List<augmentationChanceAcc> _augmentationChancesAcc = new ArrayList<>();
	private final Map<Integer, LifeStone> _lifeStones = new HashMap<>();
	private final Map<Integer, SkillHolder> _allSkills = new HashMap<>();
	private final Map<Integer, LifeStoneGrade> _lifeStoneGrades = new HashMap<>();
	private final Map<Integer, GemStone> _gemStones = new HashMap<>();
	
	private boolean _isRetailAugmentation;
	private boolean _isRetailAugmentationAccessory;
	private int _baseStatChance = 1;
	private int[] _forbiddenList;
	private StatsSet _params;
	
	protected AugmentationParser()
	{
		for (int i = 0; i < 10; i++)
		{
			_blueSkills.add(new ArrayList<>());
			_purpleSkills.add(new ArrayList<>());
			_redSkills.add(new ArrayList<>());
			_yellowSkills.add(new ArrayList<>());
		}
		
		load();
		
		info("Loaded: " + _lifeStones.size() + " life stones.");
		
		if (!_isRetailAugmentation)
		{
			for (int i = 0; i < 10; i++)
			{
				info("Loaded: " + _blueSkills.get(i).size() + " blue, " + _purpleSkills.get(i).size() + " purple and " + _redSkills.get(i).size() + " red skills for lifeStoneLevel " + i);
			}
		}
		else
		{
			info("Loaded: " + _augmentationWeaponChances.size() + " weapon augmentations.");
			info("Loaded: " + _augmentationArmorChances.size() + " armor augmentations.");
			info("Loaded: " + _augmentationChancesAcc.size() + " accessory augmentations.");
		}
	}
	
	public class AugmentationChance
	{
		private final String _WeaponType;
		private final int _StoneId;
		private final int _VariationId;
		private final int _CategoryChance;
		private final int _AugmentId;
		private final float _AugmentChance;
		
		public AugmentationChance(String WeaponType, int StoneId, int VariationId, int CategoryChance, int AugmentId, float AugmentChance)
		{
			_WeaponType = WeaponType;
			_StoneId = StoneId;
			_VariationId = VariationId;
			_CategoryChance = CategoryChance;
			_AugmentId = AugmentId;
			_AugmentChance = AugmentChance;
		}
		
		public String getWeaponType()
		{
			return _WeaponType;
		}
		
		public int getStoneId()
		{
			return _StoneId;
		}
		
		public int getVariationId()
		{
			return _VariationId;
		}
		
		public int getCategoryChance()
		{
			return _CategoryChance;
		}
		
		public int getAugmentId()
		{
			return _AugmentId;
		}
		
		public float getAugmentChance()
		{
			return _AugmentChance;
		}
	}
	
	public class augmentationChanceAcc
	{
		private final String _WeaponType;
		private final int _StoneId;
		private final int _VariationId;
		private final int _CategoryChance;
		private final int _AugmentId;
		private final float _AugmentChance;
		
		public augmentationChanceAcc(String WeaponType, int StoneId, int VariationId, int CategoryChance, int AugmentId, float AugmentChance)
		{
			_WeaponType = WeaponType;
			_StoneId = StoneId;
			_VariationId = VariationId;
			_CategoryChance = CategoryChance;
			_AugmentId = AugmentId;
			_AugmentChance = AugmentChance;
		}
		
		public String getWeaponType()
		{
			return _WeaponType;
		}
		
		public int getStoneId()
		{
			return _StoneId;
		}
		
		public int getVariationId()
		{
			return _VariationId;
		}
		
		public int getCategoryChance()
		{
			return _CategoryChance;
		}
		
		public int getAugmentId()
		{
			return _AugmentId;
		}
		
		public float getAugmentChance()
		{
			return _AugmentChance;
		}
	}
	
	private final void load()
	{
		try
		{
			final var factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final var file = new File(Config.DATAPACK_ROOT + "/data/stats/skills/augmentation/lifestone_map.xml");
			if (!file.exists())
			{
				warn("ERROR The augmentation lifestone map file is missing.");
				return;
			}
			
			final var doc = factory.newDocumentBuilder().parse(file);
			
			for (var n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (var d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("configs".equalsIgnoreCase(d.getNodeName()))
						{
							_params = new StatsSet();
							for (var sp = d.getFirstChild(); sp != null; sp = sp.getNextSibling())
							{
								if ("set".equalsIgnoreCase(sp.getNodeName()))
								{
									_params.set(sp.getAttributes().getNamedItem("name").getNodeValue(), sp.getAttributes().getNamedItem("value").getNodeValue());
								}
							}
						}
						else if ("lifeStones".equalsIgnoreCase(d.getNodeName()))
						{
							var attrs = d.getAttributes();
							_isRetailAugmentation = Boolean.parseBoolean(attrs.getNamedItem("isRetailAugmentation").getNodeValue());
							_isRetailAugmentationAccessory = Boolean.parseBoolean(attrs.getNamedItem("isRetailAugmentationAccessory").getNodeValue());
							_baseStatChance = Integer.parseInt(attrs.getNamedItem("baseStatChance").getNodeValue());
							for (var c = d.getFirstChild(); c != null; c = c.getNextSibling())
							{
								if ("grade".equalsIgnoreCase(c.getNodeName()))
								{
									attrs = c.getAttributes();
									final var id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
									final var skillChance = Integer.parseInt(attrs.getNamedItem("skillChance").getNodeValue());
									final var growChance = Integer.parseInt(attrs.getNamedItem("growChance").getNodeValue());
									final String[] retail = attrs.getNamedItem("retailChance").getNodeValue().split(",");
									final int[] retailChance = new int[retail.length];
									for (int i = 0; i < 4; i++)
									{
										retailChance[i] = Integer.parseInt(retail[i]);
									}
									_lifeStoneGrades.put(id, new LifeStoneGrade(id, skillChance, growChance, retailChance));
								}
							}
						}
						else if ("forbidden".equalsIgnoreCase(d.getNodeName()))
						{
							final String[] list = d.getAttributes().getNamedItem("list").getNodeValue().split(",");
							_forbiddenList = new int[list.length];
							for (int i = 0; i < list.length; i++)
							{
								_forbiddenList[i] = Integer.parseInt(list[i]);
							}
							Arrays.sort(_forbiddenList);
						}
						else if ("gemStones".equalsIgnoreCase(d.getNodeName()))
						{
							for (var c = d.getFirstChild(); c != null; c = c.getNextSibling())
							{
								if ("item".equalsIgnoreCase(c.getNodeName()))
								{
									final var attrs = c.getAttributes();
									final var grade = Integer.parseInt(attrs.getNamedItem("grade").getNodeValue());
									final var gemId = Integer.parseInt(attrs.getNamedItem("gemId").getNodeValue());
									final var count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
									final var accessoryCount = Integer.parseInt(attrs.getNamedItem("accessoryCount").getNodeValue());
									_gemStones.put(grade, new GemStone(grade, gemId, count, accessoryCount));
								}
							}
						}
						else if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							final var attrs = d.getAttributes();
							final var itemId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							final var grade = Integer.parseInt(attrs.getNamedItem("grade").getNodeValue());
							final var level = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
							final var minLvl = Integer.parseInt(attrs.getNamedItem("minLvl").getNodeValue());
							final var isWeaponAugment = Boolean.parseBoolean(attrs.getNamedItem("isWeaponAugment").getNodeValue());
							final var isArmorAugment = Boolean.parseBoolean(attrs.getNamedItem("isArmorAugment").getNodeValue());
							_lifeStones.put(itemId, new LifeStone(grade, level, minLvl, isWeaponAugment, isArmorAugment));
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			warn("ERROR parsing lifestone_map.xml.", e);
			return;
		}
		
		if (!_isRetailAugmentation)
		{
			try
			{
				int badAugmantData = 0;
				final var factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				
				final var file = new File(Config.DATAPACK_ROOT + "/data/stats/skills/augmentation/augmentation_skillmap.xml");
				if (!file.exists())
				{
					warn("ERROR The augmentation skillmap file is missing.");
					return;
				}
				
				final var doc = factory.newDocumentBuilder().parse(file);
				
				for (var n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("list".equalsIgnoreCase(n.getNodeName()))
					{
						for (var d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("augmentation".equalsIgnoreCase(d.getNodeName()))
							{
								var attrs = d.getAttributes();
								var skillId = 0;
								final var augmentationId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
								var skillLvL = 0;
								String type = "blue";
								
								for (var cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("skillId".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										skillId = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									}
									else if ("skillLevel".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										skillLvL = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									}
									else if ("type".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										type = attrs.getNamedItem("val").getNodeValue();
									}
								}
								if (skillId == 0)
								{
									badAugmantData++;
									continue;
								}
								else if (skillLvL == 0)
								{
									badAugmantData++;
									continue;
								}
								final int k = (augmentationId - BLUE_START) / SKILLS_BLOCKSIZE;
								
								if (type.equalsIgnoreCase("blue"))
								{
									_blueSkills.get(k).add(augmentationId);
								}
								else if (type.equalsIgnoreCase("purple"))
								{
									_purpleSkills.get(k).add(augmentationId);
								}
								else
								{
									_redSkills.get(k).add(augmentationId);
								}
								
								_allSkills.put(augmentationId, new SkillHolder(skillId, skillLvL));
							}
						}
					}
				}
				if (badAugmantData != 0)
				{
					info("" + badAugmantData + " bad skill(s) were skipped.");
				}
			}
			catch (final Exception e)
			{
				warn("ERROR parsing augmentation_skillmap.xml.", e);
				return;
			}
		}
		else
		{
			final var factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final var aFile = new File(Config.DATAPACK_ROOT + "/data/stats/skills/augmentation/retailchances.xml");
			if (aFile.exists())
			{
				Document aDoc = null;
				
				try
				{
					aDoc = factory.newDocumentBuilder().parse(aFile);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
					return;
				}
				String aWeaponType = null;
				var aStoneId = 0;
				var aVariationId = 0;
				var aCategoryChance = 0;
				var aAugmentId = 0;
				float aAugmentChance = 0;
				
				for (var l = aDoc.getFirstChild(); l != null; l = l.getNextSibling())
				{
					if (l.getNodeName().equals("list"))
					{
						NamedNodeMap aNodeAttributes = null;
						
						for (var n = l.getFirstChild(); n != null; n = n.getNextSibling())
						{
							if (n.getNodeName().equals("weapon"))
							{
								aNodeAttributes = n.getAttributes();
								
								aWeaponType = aNodeAttributes.getNamedItem("type").getNodeValue();
								
								for (var c = n.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if (c.getNodeName().equals("stone"))
									{
										aNodeAttributes = c.getAttributes();
										
										aStoneId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());
										
										for (var v = c.getFirstChild(); v != null; v = v.getNextSibling())
										{
											if (v.getNodeName().equals("variation"))
											{
												aNodeAttributes = v.getAttributes();
												
												aVariationId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());
												
												for (var j = v.getFirstChild(); j != null; j = j.getNextSibling())
												{
													if (j.getNodeName().equals("category"))
													{
														aNodeAttributes = j.getAttributes();
														
														aCategoryChance = Integer.parseInt(aNodeAttributes.getNamedItem("probability").getNodeValue());
														
														for (var e = j.getFirstChild(); e != null; e = e.getNextSibling())
														{
															if (e.getNodeName().equals("augment"))
															{
																aNodeAttributes = e.getAttributes();
																
																aAugmentId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());
																aAugmentChance = Float.parseFloat(aNodeAttributes.getNamedItem("chance").getNodeValue());
																
																_augmentationWeaponChances.add(new AugmentationChance(aWeaponType, aStoneId, aVariationId, aCategoryChance, aAugmentId, aAugmentChance));
															}
														}
													}
												}
											}
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
				warn("ERROR The retailchances.xml data file is missing.");
				return;
			}
		}
		
		if (isAllowArmorAugmentation())
		{
			final var factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final var aFile = new File(Config.DATAPACK_ROOT + "/data/stats/skills/augmentation/armor_augmentation.xml");
			if (aFile.exists())
			{
				Document aDoc = null;
				
				try
				{
					aDoc = factory.newDocumentBuilder().parse(aFile);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
					return;
				}
				String aWeaponType = null;
				var aStoneId = 0;
				var aVariationId = 0;
				var aCategoryChance = 0;
				var aAugmentId = 0;
				float aAugmentChance = 0;
				
				for (var l = aDoc.getFirstChild(); l != null; l = l.getNextSibling())
				{
					if (l.getNodeName().equals("list"))
					{
						NamedNodeMap aNodeAttributes = null;
						
						for (var n = l.getFirstChild(); n != null; n = n.getNextSibling())
						{
							if (n.getNodeName().equals("weapon"))
							{
								aNodeAttributes = n.getAttributes();
								
								aWeaponType = aNodeAttributes.getNamedItem("type").getNodeValue();
								
								for (var c = n.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if (c.getNodeName().equals("stone"))
									{
										aNodeAttributes = c.getAttributes();
										
										aStoneId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());
										
										for (var v = c.getFirstChild(); v != null; v = v.getNextSibling())
										{
											if (v.getNodeName().equals("variation"))
											{
												aNodeAttributes = v.getAttributes();
												
												aVariationId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());
												
												for (var j = v.getFirstChild(); j != null; j = j.getNextSibling())
												{
													if (j.getNodeName().equals("category"))
													{
														aNodeAttributes = j.getAttributes();
														
														aCategoryChance = Integer.parseInt(aNodeAttributes.getNamedItem("probability").getNodeValue());
														
														for (var e = j.getFirstChild(); e != null; e = e.getNextSibling())
														{
															if (e.getNodeName().equals("augment"))
															{
																aNodeAttributes = e.getAttributes();
																
																aAugmentId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());
																aAugmentChance = Float.parseFloat(aNodeAttributes.getNamedItem("chance").getNodeValue());
																
																_augmentationArmorChances.add(new AugmentationChance(aWeaponType, aStoneId, aVariationId, aCategoryChance, aAugmentId, aAugmentChance));
															}
														}
													}
												}
											}
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
				warn("ERROR The armor_augmentation.xml data file is missing.");
			}
		}
		
		if (_isRetailAugmentationAccessory)
		{
			final var factory3 = DocumentBuilderFactory.newInstance();
			factory3.setValidating(false);
			factory3.setIgnoringComments(true);
			
			final var aFile3 = new File(Config.DATAPACK_ROOT + "/data/stats/skills/augmentation/retailchances_accessory.xml");
			if (aFile3.exists())
			{
				Document aDoc = null;
				
				try
				{
					aDoc = factory3.newDocumentBuilder().parse(aFile3);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
					return;
				}
				String aWeaponType = null;
				var aStoneId = 0;
				var aVariationId = 0;
				var aCategoryChance = 0;
				var aAugmentId = 0;
				float aAugmentChance = 0;
				
				for (var l = aDoc.getFirstChild(); l != null; l = l.getNextSibling())
				{
					if (l.getNodeName().equals("list"))
					{
						NamedNodeMap aNodeAttributes = null;
						for (var n = l.getFirstChild(); n != null; n = n.getNextSibling())
						{
							if (n.getNodeName().equals("weapon"))
							{
								aNodeAttributes = n.getAttributes();
								
								aWeaponType = aNodeAttributes.getNamedItem("type").getNodeValue();
								
								for (var c = n.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if (c.getNodeName().equals("stone"))
									{
										aNodeAttributes = c.getAttributes();
										
										aStoneId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());
										
										for (var v = c.getFirstChild(); v != null; v = v.getNextSibling())
										{
											if (v.getNodeName().equals("variation"))
											{
												aNodeAttributes = v.getAttributes();
												
												aVariationId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());
												
												for (var j = v.getFirstChild(); j != null; j = j.getNextSibling())
												{
													if (j.getNodeName().equals("category"))
													{
														aNodeAttributes = j.getAttributes();
														
														aCategoryChance = Integer.parseInt(aNodeAttributes.getNamedItem("probability").getNodeValue());
														
														for (var e = j.getFirstChild(); e != null; e = e.getNextSibling())
														{
															if (e.getNodeName().equals("augment"))
															{
																aNodeAttributes = e.getAttributes();
																
																aAugmentId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());
																aAugmentChance = Float.parseFloat(aNodeAttributes.getNamedItem("chance").getNodeValue());
																
																_augmentationChancesAcc.add(new augmentationChanceAcc(aWeaponType, aStoneId, aVariationId, aCategoryChance, aAugmentId, aAugmentChance));
															}
														}
													}
												}
											}
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
				warn("ERROR The retailchances_accessory.xml data file is missing.");
				return;
			}
		}
	}
	
	public Augmentation generateRandomAugmentation(int lifeStoneLevel, int lifeStoneGrade, int bodyPart, int lifeStoneId, ItemInstance targetItem)
	{
		if (targetItem.isWeapon())
		{
			return generateRandomWeaponAugmentation(lifeStoneLevel, lifeStoneGrade, lifeStoneId, targetItem);
		}
		else if (targetItem.isJewel())
		{
			return generateRandomAccessoryAugmentation(lifeStoneLevel, lifeStoneGrade, bodyPart, lifeStoneId);
		}
		return generateRandomArmorAugmentation(lifeStoneLevel, lifeStoneGrade, lifeStoneId, targetItem);
	}
	
	private Augmentation generateRandomArmorAugmentation(int lifeStoneLevel, int lifeStoneGrade, int lifeStoneId, ItemInstance item)
	{
		var stat12 = 0;
		var stat34 = 0;
		if (_isRetailAugmentation)
		{
			final List<AugmentationChance> _selectedChances12 = new ArrayList<>();
			final List<AugmentationChance> _selectedChances34 = new ArrayList<>();
			for (final var ac : _augmentationArmorChances)
			{
				if (ac.getWeaponType().equals("warrior") && (ac.getStoneId() == lifeStoneId))
				{
					if (ac.getVariationId() == 1)
					{
						_selectedChances12.add(ac);
					}
					else
					{
						_selectedChances34.add(ac);
					}
				}
			}
			var r = Rnd.get(10000);
			var s = 10000;
			for (final var ac : _selectedChances12)
			{
				if (s > r)
				{
					s -= (ac.getAugmentChance() * 100);
					stat12 = ac.getAugmentId();
				}
			}
			
			final int[] gradeChance = _lifeStoneGrades.get(lifeStoneGrade).getRetailChance();
			var c = Rnd.get(100);
			if (c < gradeChance[0])
			{
				c = 55;
			}
			else if (c < (gradeChance[0] + gradeChance[1]))
			{
				c = 35;
			}
			else if (c < (gradeChance[0] + gradeChance[1] + gradeChance[2]))
			{
				c = 7;
			}
			else
			{
				c = 3;
			}
			final List<AugmentationChance> _selectedChances34final = new ArrayList<>();
			for (final var ac : _selectedChances34)
			{
				if (ac.getCategoryChance() == c)
				{
					_selectedChances34final.add(ac);
				}
			}
			r = Rnd.get(10000);
			s = 10000;
			for (final var ac : _selectedChances34final)
			{
				if (s > r)
				{
					s -= (ac.getAugmentChance() * 100);
					stat34 = ac.getAugmentId();
				}
			}
			return new Augmentation(((stat34 << 16) + stat12));
		}
		var generateSkill = false;
		var generateGlow = false;
		
		lifeStoneLevel = Math.min(lifeStoneLevel, 9);
		final var lGrade = _lifeStoneGrades.get(lifeStoneGrade);
		if (lGrade != null)
		{
			if (Rnd.chance(lGrade.getSkillChance()))
			{
				generateSkill = true;
			}
			
			if (Rnd.chance(lGrade.getGrowChance()))
			{
				generateGlow = true;
			}
		}
		
		if (!generateSkill && Rnd.chance(_baseStatChance))
		{
			stat34 = Rnd.get(BASESTAT_STR, BASESTAT_MEN);
		}
		
		var resultColor = Rnd.get(0, 100);
		if ((stat34 == 0) && !generateSkill)
		{
			if (resultColor <= ((15 * lifeStoneGrade) + 40))
			{
				resultColor = 1;
			}
			else
			{
				resultColor = 0;
			}
		}
		else
		{
			if ((resultColor <= ((10 * lifeStoneGrade) + 5)) || (stat34 != 0))
			{
				resultColor = 3;
			}
			else if (resultColor <= ((10 * lifeStoneGrade) + 10))
			{
				resultColor = 1;
			}
			else
			{
				resultColor = 2;
			}
		}
		
		if (generateSkill)
		{
			switch (resultColor)
			{
				case 1 :
					stat34 = _blueSkills.get(lifeStoneLevel).get(Rnd.get(0, _blueSkills.get(lifeStoneLevel).size() - 1));
					break;
				case 2 :
					stat34 = _purpleSkills.get(lifeStoneLevel).get(Rnd.get(0, _purpleSkills.get(lifeStoneLevel).size() - 1));
					break;
				case 3 :
					stat34 = _redSkills.get(lifeStoneLevel).get(Rnd.get(0, _redSkills.get(lifeStoneLevel).size() - 1));
					break;
			}
		}
		
		int offset;
		if (stat34 == 0)
		{
			final var temp = Rnd.get(2, 3);
			final var colorOffset = (resultColor * (10 * STAT_SUBBLOCKSIZE)) + (temp * STAT_BLOCKSIZE) + 1;
			offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + colorOffset;
			
			stat34 = Rnd.get(offset, (offset + STAT_SUBBLOCKSIZE) - 1);
			if (generateGlow && (lifeStoneGrade >= 2))
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + ((temp - 2) * STAT_BLOCKSIZE) + (lifeStoneGrade * (10 * STAT_SUBBLOCKSIZE)) + 1;
			}
			else
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + ((temp - 2) * STAT_BLOCKSIZE) + (Rnd.get(0, 1) * (10 * STAT_SUBBLOCKSIZE)) + 1;
			}
		}
		else
		{
			if (!generateGlow)
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + (Rnd.get(0, 1) * STAT_BLOCKSIZE) + 1;
			}
			else
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + (Rnd.get(0, 1) * STAT_BLOCKSIZE) + (((lifeStoneGrade + resultColor) / 2) * (10 * STAT_SUBBLOCKSIZE)) + 1;
			}
		}
		stat12 = Rnd.get(offset, (offset + STAT_SUBBLOCKSIZE) - 1);
		
		if (Config.DEBUG)
		{
			info("Augmentation success: stat12=" + stat12 + "; stat34=" + stat34 + "; resultColor=" + resultColor + "; level=" + lifeStoneLevel + "; grade=" + lifeStoneGrade);
		}
		return new Augmentation(((stat34 << 16) + stat12));
	}
	
	private Augmentation generateRandomWeaponAugmentation(int lifeStoneLevel, int lifeStoneGrade, int lifeStoneId, ItemInstance item)
	{
		var stat12 = 0;
		var stat34 = 0;
		if (_isRetailAugmentation)
		{
			if (((Weapon) item.getItem()).isMagicWeapon())
			{
				final List<AugmentationChance> _selectedChances12 = new ArrayList<>();
				final List<AugmentationChance> _selectedChances34 = new ArrayList<>();
				for (final var ac : _augmentationWeaponChances)
				{
					if (ac.getWeaponType().equals("mage") && (ac.getStoneId() == lifeStoneId))
					{
						if (ac.getVariationId() == 1)
						{
							_selectedChances12.add(ac);
						}
						else
						{
							_selectedChances34.add(ac);
						}
					}
				}
				var r = Rnd.get(10000);
				var s = 10000;
				for (final var ac : _selectedChances12)
				{
					if (s > r)
					{
						s -= (ac.getAugmentChance() * 100);
						stat12 = ac.getAugmentId();
					}
				}
				final int[] gradeChance = _lifeStoneGrades.get(lifeStoneGrade).getRetailChance();
				var c = Rnd.get(100);
				if (c < gradeChance[0])
				{
					c = 55;
				}
				else if (c < (gradeChance[0] + gradeChance[1]))
				{
					c = 35;
				}
				else if (c < (gradeChance[0] + gradeChance[1] + gradeChance[2]))
				{
					c = 7;
				}
				else
				{
					c = 3;
				}
				final List<AugmentationChance> _selectedChances34final = new ArrayList<>();
				for (final var ac : _selectedChances34)
				{
					if (ac.getCategoryChance() == c)
					{
						_selectedChances34final.add(ac);
					}
				}
				
				r = Rnd.get(10000);
				s = 10000;
				
				for (final var ac : _selectedChances34final)
				{
					if (s > r)
					{
						s -= (ac.getAugmentChance() * 100);
						stat34 = ac.getAugmentId();
					}
				}
			}
			else
			{
				final List<AugmentationChance> _selectedChances12 = new ArrayList<>();
				final List<AugmentationChance> _selectedChances34 = new ArrayList<>();
				for (final var ac : _augmentationWeaponChances)
				{
					if (ac.getWeaponType().equals("warrior") && (ac.getStoneId() == lifeStoneId))
					{
						if (ac.getVariationId() == 1)
						{
							_selectedChances12.add(ac);
						}
						else
						{
							_selectedChances34.add(ac);
						}
					}
				}
				var r = Rnd.get(10000);
				var s = 10000;
				for (final var ac : _selectedChances12)
				{
					if (s > r)
					{
						s -= (ac.getAugmentChance() * 100);
						stat12 = ac.getAugmentId();
					}
				}
				final int[] gradeChance = _lifeStoneGrades.get(lifeStoneGrade).getRetailChance();
				var c = Rnd.get(100);
				if (c < gradeChance[0])
				{
					c = 55;
				}
				else if (c < (gradeChance[0] + gradeChance[1]))
				{
					c = 35;
				}
				else if (c < (gradeChance[0] + gradeChance[1] + gradeChance[2]))
				{
					c = 7;
				}
				else
				{
					c = 3;
				}
				final List<AugmentationChance> _selectedChances34final = new ArrayList<>();
				for (final var ac : _selectedChances34)
				{
					if (ac.getCategoryChance() == c)
					{
						_selectedChances34final.add(ac);
					}
				}
				r = Rnd.get(10000);
				s = 10000;
				for (final var ac : _selectedChances34final)
				{
					if (s > r)
					{
						s -= (ac.getAugmentChance() * 100);
						stat34 = ac.getAugmentId();
					}
				}
			}
			return new Augmentation(((stat34 << 16) + stat12));
		}
		var generateSkill = false;
		var generateGlow = false;
		
		lifeStoneLevel = Math.min(lifeStoneLevel, 9);
		final var lGrade = _lifeStoneGrades.get(lifeStoneGrade);
		if (lGrade != null)
		{
			if (Rnd.chance(lGrade.getSkillChance()))
			{
				generateSkill = true;
			}
			
			if (Rnd.chance(lGrade.getGrowChance()))
			{
				generateGlow = true;
			}
		}
		
		if (!generateSkill && Rnd.chance(_baseStatChance))
		{
			stat34 = Rnd.get(BASESTAT_STR, BASESTAT_MEN);
		}
		
		var resultColor = Rnd.get(0, 100);
		if ((stat34 == 0) && !generateSkill)
		{
			if (resultColor <= ((15 * lifeStoneGrade) + 40))
			{
				resultColor = 1;
			}
			else
			{
				resultColor = 0;
			}
		}
		else
		{
			if ((resultColor <= ((10 * lifeStoneGrade) + 5)) || (stat34 != 0))
			{
				resultColor = 3;
			}
			else if (resultColor <= ((10 * lifeStoneGrade) + 10))
			{
				resultColor = 1;
			}
			else
			{
				resultColor = 2;
			}
		}
		
		if (generateSkill)
		{
			switch (resultColor)
			{
				case 1 :
					stat34 = _blueSkills.get(lifeStoneLevel).get(Rnd.get(0, _blueSkills.get(lifeStoneLevel).size() - 1));
					break;
				case 2 :
					stat34 = _purpleSkills.get(lifeStoneLevel).get(Rnd.get(0, _purpleSkills.get(lifeStoneLevel).size() - 1));
					break;
				case 3 :
					stat34 = _redSkills.get(lifeStoneLevel).get(Rnd.get(0, _redSkills.get(lifeStoneLevel).size() - 1));
					break;
			}
		}
		
		int offset;
		if (stat34 == 0)
		{
			final var temp = Rnd.get(2, 3);
			final var colorOffset = (resultColor * (10 * STAT_SUBBLOCKSIZE)) + (temp * STAT_BLOCKSIZE) + 1;
			offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + colorOffset;
			
			stat34 = Rnd.get(offset, (offset + STAT_SUBBLOCKSIZE) - 1);
			if (generateGlow && (lifeStoneGrade >= 2))
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + ((temp - 2) * STAT_BLOCKSIZE) + (lifeStoneGrade * (10 * STAT_SUBBLOCKSIZE)) + 1;
			}
			else
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + ((temp - 2) * STAT_BLOCKSIZE) + (Rnd.get(0, 1) * (10 * STAT_SUBBLOCKSIZE)) + 1;
			}
		}
		else
		{
			if (!generateGlow)
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + (Rnd.get(0, 1) * STAT_BLOCKSIZE) + 1;
			}
			else
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + (Rnd.get(0, 1) * STAT_BLOCKSIZE) + (((lifeStoneGrade + resultColor) / 2) * (10 * STAT_SUBBLOCKSIZE)) + 1;
			}
		}
		stat12 = Rnd.get(offset, (offset + STAT_SUBBLOCKSIZE) - 1);
		
		if (Config.DEBUG)
		{
			info("Augmentation success: stat12=" + stat12 + "; stat34=" + stat34 + "; resultColor=" + resultColor + "; level=" + lifeStoneLevel + "; grade=" + lifeStoneGrade);
		}
		return new Augmentation(((stat34 << 16) + stat12));
	}
	
	private Augmentation generateRandomAccessoryAugmentation(int lifeStoneLevel, int lifeStoneGrade, int bodyPart, int lifeStoneId)
	{
		var stat12 = 0;
		var stat34 = 0;
		if (_isRetailAugmentationAccessory)
		{
			final List<augmentationChanceAcc> _selectedChances12 = new ArrayList<>();
			final List<augmentationChanceAcc> _selectedChances34 = new ArrayList<>();
			for (final var ac : _augmentationChancesAcc)
			{
				if (ac.getWeaponType().equals("warrior") && (ac.getStoneId() == lifeStoneId))
				{
					if (ac.getVariationId() == 1)
					{
						_selectedChances12.add(ac);
					}
					else
					{
						_selectedChances34.add(ac);
					}
				}
			}
			var r = Rnd.get(10000);
			var s = 10000;
			for (final var ac : _selectedChances12)
			{
				if (s > r)
				{
					s -= (ac.getAugmentChance() * 100);
					stat12 = ac.getAugmentId();
				}
			}
			var c = Rnd.get(100);
			if (c < 55)
			{
				c = 55;
			}
			else if (c < 90)
			{
				c = 35;
			}
			else if (c < 99)
			{
				c = 9;
			}
			else
			{
				c = 1;
			}
			final List<augmentationChanceAcc> _selectedChances34final = new ArrayList<>();
			for (final var ac : _selectedChances34)
			{
				if (ac.getCategoryChance() == c)
				{
					_selectedChances34final.add(ac);
				}
			}
			r = Rnd.get(10000);
			s = 10000;
			for (final var ac : _selectedChances34final)
			{
				if (s > r)
				{
					s -= (ac.getAugmentChance() * 100);
					stat34 = ac.getAugmentId();
				}
			}
			
			return new Augmentation(((stat34 << 16) + stat12));
		}
		lifeStoneLevel = Math.min(lifeStoneLevel, 9);
		var base = 0;
		var skillsLength = 0;
		
		switch (bodyPart)
		{
			case Item.SLOT_LR_FINGER :
				base = ACC_RING_START + (ACC_RING_BLOCKSIZE * lifeStoneLevel);
				skillsLength = ACC_RING_SKILLS;
				break;
			case Item.SLOT_LR_EAR :
				base = ACC_EAR_START + (ACC_EAR_BLOCKSIZE * lifeStoneLevel);
				skillsLength = ACC_EAR_SKILLS;
				break;
			case Item.SLOT_NECK :
				base = ACC_NECK_START + (ACC_NECK_BLOCKSIZE * lifeStoneLevel);
				skillsLength = ACC_NECK_SKILLS;
				break;
			default :
				return null;
		}
		
		final var resultColor = Rnd.get(0, 3);
		
		stat12 = Rnd.get(ACC_STAT_SUBBLOCKSIZE);
		Options op = null;
		final var lGrade = _lifeStoneGrades.get(lifeStoneGrade);
		if (lGrade != null && Rnd.chance(lGrade.getSkillChance()))
		{
			stat34 = base + Rnd.get(skillsLength);
			op = OptionsParser.getInstance().getOptions(stat34);
		}
		
		if ((op == null) || (!op.hasActiveSkill() && !op.hasPassiveSkill() && !op.hasActivationSkills()))
		{
			stat34 = (stat12 + 1 + Rnd.get(ACC_STAT_SUBBLOCKSIZE - 1)) % ACC_STAT_SUBBLOCKSIZE;
			stat34 = base + skillsLength + (ACC_STAT_SUBBLOCKSIZE * resultColor) + stat34;
		}
		
		stat12 = base + skillsLength + (ACC_STAT_SUBBLOCKSIZE * resultColor) + stat12;
		
		if (Config.DEBUG)
		{
			info("Accessory augmentation success: stat12=" + stat12 + "; stat34=" + stat34 + "; level=" + lifeStoneLevel);
		}
		return new Augmentation(((stat34 << 16) + stat12));
	}

	public int generateRandomSecondaryAugmentation()
	{
		final var offset = 9 * STAT_SUBBLOCKSIZE + Rnd.get(0, 1) * STAT_BLOCKSIZE + (3 + 2) / 2 * 10 * STAT_SUBBLOCKSIZE + 1;
		
		return Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
	}
	
	public Map<Integer, LifeStone> getLifeStones()
	{
		return _lifeStones;
	}
	
	public LifeStone getLifeStone(int itemId)
	{
		return _lifeStones.get(itemId);
	}
	
	public boolean isAllowArmorAugmentation()
	{
		return getParams().getBool("allowArmorAugmentation");
	}
	
	public int[] getForbiddenList()
	{
		return _forbiddenList;
	}
	
	public StatsSet getParams()
	{
		return _params;
	}
	
	public int getGemStoneId(int itemGrade)
	{
		if (_gemStones.containsKey(itemGrade))
		{
			return _gemStones.get(itemGrade).getGemId();
		}
		return 0;
	}
	
	public int getGemStoneCount(int itemGrade, int lifeStoneGrade)
	{
		if (_gemStones.containsKey(itemGrade))
		{
			return lifeStoneGrade == 4 ? _gemStones.get(itemGrade).getAccessoryCount() : _gemStones.get(itemGrade).getCount();
		}
		return 0;
	}
	
	public static final AugmentationParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AugmentationParser _instance = new AugmentationParser();
	}
}