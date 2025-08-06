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
package l2e.scripts.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ClassListParser;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;

public final class SubClassSkills extends Quest
{
	// arrays must be sorted
	private static final int[] _allCertSkillIds =
	{
	        631, 632, 633, 634, 637, 638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 650, 651, 652, 653, 654, 655, 656, 657, 658, 659, 660, 661, 662, 799, 800, 801, 802, 803, 804, 1489, 1490, 1491
	};
	private static final int[][] _certSkillsByLevel =
	{
	        {
	                631, 632, 633, 634
			},
			{
			        631, 632, 633, 634
			},
			{
			        637, 638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 650, 651, 652, 653, 654, 655, 799, 800, 801, 802, 803, 804, 1489, 1490, 1491
			},
			{
			        656, 657, 658, 659, 660, 661, 662
			}
	};
	
	private static final int[] _allCertItemIds =
	{
	        10280, 10281, 10282, 10283, 10284, 10285, 10286, 10287, 10288, 10289, 10290, 10291, 10292, 10293, 10294, 10612
	};
	private static final int[][] _certItemsByLevel =
	{
	        {
	                10280
			},
			{
			        10280
			},
			{
			        10612, 10281, 10282, 10283, 10284, 10285, 10286, 10287
			},
			{
			        10288, 10289, 10290, 10291, 10292, 10293, 10294
			}
	};
	
	private static final String[] VARS =
	{
	        "EmergentAbility65-", "EmergentAbility70-", "ClassAbility75-", "ClassAbility80-"
	};
	
	public SubClassSkills(int id, String name, String descr)
	{
		super(id, name, descr);
		setOnEnterWorld(true);
	}
	
	@Override
	public String onEnterWorld(Player player)
	{
		if (player.getReflection().isDefault())
		{
			final var effect = player.getFirstEffect(8239);
			if (effect != null)
			{
				effect.exit();
			}
		}
		
		if (!Config.SECURITY_SKILL_CHECK || player.canOverrideCond(PcCondOverride.SKILL_CONDITIONS))
		{
			return null;
		}
		
		final List<Skill> certSkills = getCertSkills(player);
		if (player.isSubClassActive())
		{
			if (certSkills != null)
			{
				for (final Skill s : certSkills)
				{
					Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " has cert skill on subclass :" + s.getName(null) + "(" + s.getId() + "/" + s.getLevel() + "), class:" + ClassListParser.getInstance().getClass(player.getClassId()).getClassName());
					
					if (Config.SECURITY_SKILL_CHECK_CLEAR)
					{
						player.removeSkill(s);
					}
				}
			}
			return null;
		}
		
		final int[][] cSkills = new int[certSkills.size()][2];
		for (int i = certSkills.size(); --i >= 0;)
		{
			final Skill skill = certSkills.get(i);
			cSkills[i][0] = skill.getId();
			cSkills[i][1] = skill.getLevel();
		}
		
		final List<ItemInstance> certItems = getCertItems(player);
		final int[][] cItems = new int[certItems.size()][2];
		for (int i = certItems.size(); --i >= 0;)
		{
			final ItemInstance item = certItems.get(i);
			cItems[i][0] = item.getObjectId();
			cItems[i][1] = (int) Math.min(item.getCount(), Integer.MAX_VALUE);
		}
		
		QuestState st = player.getQuestState("SubClassSkills");
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		String qName, qValue;
		int id, index;
		for (int i = VARS.length; --i >= 0;)
		{
			for (int j = Config.MAX_SUBCLASS; j > 0; j--)
			{
				qName = VARS[i] + String.valueOf(j);
				qValue = st.getGlobalQuestVar(qName);
				if ((qValue == null) || qValue.isEmpty())
				{
					continue;
				}
				
				if (qValue.endsWith(";"))
				{
					try
					{
						id = Integer.parseInt(qValue.replace(";", ""));
						
						Skill skill = null;
						if (certSkills != null)
						{
							if (cSkills != null)
							{
								for (index = certSkills.size(); --index >= 0;)
								{
									if (cSkills[index][0] == id)
									{
										skill = certSkills.get(index);
										cSkills[index][1]--;
										break;
									}
								}
							}
							if (skill != null)
							{
								if (!ArrayUtils.contains(_certSkillsByLevel[i], id))
								{
									Util.handleIllegalPlayerAction(player, "Invalid cert variable WITH skill:" + qName + "=" + qValue + " - skill does not match certificate level");
								}
							}
							else
							{
								Util.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - skill not found");
							}
						}
						else
						{
							Util.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - no certified skills found");
						}
					}
					catch (final NumberFormatException e)
					{
						Util.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - not a number");
					}
				}
				else
				{
					try
					{
						id = Integer.parseInt(qValue);
						if (id == 0)
						{
							continue;
						}
						
						ItemInstance item = null;
						if (certItems != null)
						{
							if (cItems != null)
							{
								for (index = certItems.size(); --index >= 0;)
								{
									if (cItems[index][0] == id)
									{
										item = certItems.get(index);
										cItems[index][1]--;
										break;
									}
								}
							}
							if (item != null)
							{
								if (!ArrayUtils.contains(_certItemsByLevel[i], item.getId()))
								{
									Util.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - item found but does not match certificate level");
								}
							}
							else
							{
								Util.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - item not found");
							}
						}
						else
						{
							Util.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - no cert item found in inventory");
						}
						
					}
					catch (final NumberFormatException e)
					{
						Util.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - not a number");
					}
				}
			}
		}
		
		if ((certSkills != null) && (cSkills != null))
		{
			for (int i = cSkills.length; --i >= 0;)
			{
				if (cSkills[i][1] == 0)
				{
					continue;
				}
				
				final Skill skill = certSkills.get(i);
				if (cSkills[i][1] > 0)
				{
					if (cSkills[i][1] == skill.getLevel())
					{
						Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " has invalid cert skill :" + skill.getName(null) + "(" + skill.getId() + "/" + skill.getLevel() + ")");
					}
					else
					{
						Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " has invalid cert skill :" + skill.getName(null) + "(" + skill.getId() + "/" + skill.getLevel() + "), level too high");
					}
					
					if (Config.SECURITY_SKILL_CHECK_CLEAR)
					{
						player.removeSkill(skill);
					}
				}
				else
				{
					Util.handleIllegalPlayerAction(player, "Invalid cert skill :" + skill.getName(null) + "(" + skill.getId() + "/" + skill.getLevel() + "), level too low");
				}
			}
		}
		
		if ((certItems != null) && (cItems != null))
		{
			for (int i = cItems.length; --i >= 0;)
			{
				if (cItems[i][1] == 0)
				{
					continue;
				}
				
				final ItemInstance item = certItems.get(i);
				Util.handleIllegalPlayerAction(player, "Invalid cert item without variable or with wrong count:" + item.getObjectId());
			}
		}
		
		return null;
	}
	
	private List<Skill> getCertSkills(Player player)
	{
		final List<Skill> tmp = new ArrayList<>();
		for (final Skill s : player.getAllSkills())
		{
			if ((s != null) && (Arrays.binarySearch(_allCertSkillIds, s.getId()) >= 0))
			{
				tmp.add(s);
			}
		}
		return tmp;
	}
	
	private List<ItemInstance> getCertItems(Player player)
	{
		final List<ItemInstance> tmp = new ArrayList<>();
		for (final ItemInstance i : player.getInventory().getItems())
		{
			if ((i != null) && (Arrays.binarySearch(_allCertItemIds, i.getId()) >= 0))
			{
				tmp.add(i);
			}
		}
		return tmp;
	}
	
	public static void main(String[] args)
	{
		new SubClassSkills(-1, "SubClassSkills", "custom");
	}
}
