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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.funcs.FuncTemplate;
import l2e.gameserver.model.skills.funcs.LambdaConst;
import l2e.gameserver.model.skills.options.Options;
import l2e.gameserver.model.skills.options.Options.AugmentationFilter;
import l2e.gameserver.model.skills.options.OptionsSkillHolder;
import l2e.gameserver.model.skills.options.OptionsSkillType;
import l2e.gameserver.model.stats.Stats;

public class OptionsParser extends DocumentParser
{
	private final Map<Integer, Options> _data = new HashMap<>();
	
	protected OptionsParser()
	{
		load();
	}

	@Override
	public synchronized void load()
	{
		parseDirectory("data/stats/skills/options", false);
		info("Loaded: " + _data.size() + " options.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		int id;
		Options op;
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("option".equalsIgnoreCase(d.getNodeName()))
					{
						id = parseInt(d.getAttributes(), "id");
						op = new Options(id);

						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							switch (cd.getNodeName())
							{
								case "for" :
								{
									for (Node fd = cd.getFirstChild(); fd != null; fd = fd.getNextSibling())
									{
										switch (fd.getNodeName())
										{
											case "add" :
											{
												parseFuncs(fd.getAttributes(), "Add", op);
												break;
											}
											case "mul" :
											{
												parseFuncs(fd.getAttributes(), "Mul", op);
												break;
											}
											case "basemul" :
											{
												parseFuncs(fd.getAttributes(), "BaseMul", op);
												break;
											}
											case "sub" :
											{
												parseFuncs(fd.getAttributes(), "Sub", op);
												break;
											}
											case "div" :
											{
												parseFuncs(fd.getAttributes(), "Div", op);
												break;
											}
											case "set" :
											{
												parseFuncs(fd.getAttributes(), "Set", op);
												break;
											}
										}
									}
									break;
								}
								case "active_skill" :
								{
									op.setActiveSkill(new SkillHolder(parseInt(cd.getAttributes(), "id"), parseInt(cd.getAttributes(), "level")));
									break;
								}
								case "passive_skill" :
								{
									op.setPassiveSkill(new SkillHolder(parseInt(cd.getAttributes(), "id"), parseInt(cd.getAttributes(), "level")));
									break;
								}
								case "attack_skill" :
								{
									op.addActivationSkill(new OptionsSkillHolder(parseInt(cd.getAttributes(), "id"), parseInt(cd.getAttributes(), "level"), parseDouble(cd.getAttributes(), "chance"), OptionsSkillType.ATTACK));
									break;
								}
								case "magic_skill" :
								{
									op.addActivationSkill(new OptionsSkillHolder(parseInt(cd.getAttributes(), "id"), parseInt(cd.getAttributes(), "level"), parseDouble(cd.getAttributes(), "chance"), OptionsSkillType.MAGIC));
									break;
								}
								case "critical_skill" :
								{
									op.addActivationSkill(new OptionsSkillHolder(parseInt(cd.getAttributes(), "id"), parseInt(cd.getAttributes(), "level"), parseDouble(cd.getAttributes(), "chance"), OptionsSkillType.CRITICAL));
									break;
								}
							}
						}
						_data.put(op.getId(), op);
					}
				}
			}
		}
	}

	private void parseFuncs(NamedNodeMap attrs, String func, Options op)
	{
		final Stats stat = Stats.valueOfXml(parseString(attrs, "stat"));
		final int ord = Integer.decode(parseString(attrs, "order"));
		final double val = parseDouble(attrs, "val");
		op.addFunc(new FuncTemplate(null, null, func, stat, ord, new LambdaConst(val)));
	}

	public Options getOptions(int id)
	{
		return _data.get(id);
	}

	public Collection<Options> getUniqueOptions(AugmentationFilter filter)
	{
		switch (filter)
		{
			case ACTIVE_SKILL :
			{
				final Map<Integer, Options> options = new HashMap<>();
				for (final Options option : _data.values())
				{
					if (option.hasActivationSkills() || option.hasPassiveSkill())
					{
						continue;
					}
					
					if (!option.hasActiveSkill())
					{
						continue;
					}
					
					for (final int id : Config.SERVICES_AUGMENTATION_DISABLED_LIST)
					{
						if (id == option.getId())
						{
							continue;
						}
					}
					
					if (!options.containsKey(option.getActiveSkill().getId()) || options.get(option.getActiveSkill().getId()).getActiveSkill().getLvl() < option.getActiveSkill().getLvl())
					{
						options.put(option.getActiveSkill().getId(), option);
					}
				}
				final List<Options> augs = new ArrayList<>(options.values());
				Collections.sort(augs, new ActiveSkillsComparator());
				return augs;
			}
			case PASSIVE_SKILL :
			{
				final Map<Integer, Options> options = new HashMap<>();
				for (final Options option : _data.values())
				{
					if (option.hasActivationSkills() || option.hasActiveSkill())
					{
						continue;
					}
					
					if (!option.hasPassiveSkill())
					{
						continue;
					}
					
					for (final int id : Config.SERVICES_AUGMENTATION_DISABLED_LIST)
					{
						if (id == option.getId())
						{
							continue;
						}
					}
					
					if (!options.containsKey(option.getPassiveSkill().getId()) || options.get(option.getPassiveSkill().getId()).getPassiveSkill().getLvl() < option.getPassiveSkill().getLvl())
					{
						options.put(option.getPassiveSkill().getId(), option);
					}
				}
				final List<Options> augs = new ArrayList<>(options.values());
				Collections.sort(augs, new PassiveSkillsComparator());
				return augs;
			}
			case CHANCE_SKILL :
			{
				final Map<Integer, Options> options = new HashMap<>();
				for (final Options option : _data.values())
				{
					if (option.hasPassiveSkill() || option.hasActiveSkill())
					{
						continue;
					}
					
					if (!option.hasActivationSkills())
					{
						continue;
					}
					
					for (final int id : Config.SERVICES_AUGMENTATION_DISABLED_LIST)
					{
						if (id == option.getId())
						{
							continue;
						}
					}

					if (!options.containsKey(option.getActivationsSkills().get(0).getId()) || options.get(option.getActivationsSkills().get(0).getId()).getActivationsSkills().get(0).getLvl() < option.getActivationsSkills().get(0).getLvl())
					{
						options.put(option.getActivationsSkills().get(0).getId(), option);
					}
				}
				final List<Options> augs = new ArrayList<>(options.values());
				Collections.sort(augs, new ChanceSkillsComparator());
				return augs;
			}
			case STATS :
			{
				final Map<Integer, Options> options = new HashMap<>();
				for (final Options option : _data.values())
				{
					for (final int id : Config.SERVICES_AUGMENTATION_DISABLED_LIST)
					{
						if (id == option.getId())
						{
							continue;
						}
					}
					
					switch (option.getId())
					{
						case 16341 :
						case 16342 :
						case 16343 :
						case 16344 :
							options.put(option.getId(), option);
							break;
					}
				}
				final List<Options> augs = new ArrayList<>(options.values());
				return augs;
			}
		}
		return _data.values();
	}
	
	public Collection<Options> getUniqueAvailableOptions(AugmentationFilter filter)
	{
		if (filter == AugmentationFilter.NONE)
		{
			return _data.values();
		}
		
		List<Options> augs = null;
		switch (filter)
		{
			case ACTIVE_SKILL :
			{
				final Map<Integer, Options> options = new HashMap<>();
				for (final Options option : _data.values())
				{
					if (option.hasActivationSkills() || option.hasPassiveSkill())
					{
						continue;
					}
					
					if (!option.hasActiveSkill())
					{
						continue;
					}
					
					if (!options.containsKey(option.getActiveSkill().getId()) || options.get(option.getActiveSkill().getId()).getActiveSkill().getLvl() < option.getActiveSkill().getLvl())
					{
						for (final int id : Config.SERVICES_AUGMENTATION_AVAILABLE_LIST)
						{
							if (id == option.getId())
							{
								options.put(option.getActiveSkill().getId(), option);
							}
						}
					}
				}
				augs = new ArrayList<>(options.values());
				Collections.sort(augs, new ActiveSkillsComparator());
				break;
			}
			case PASSIVE_SKILL :
			{
				final Map<Integer, Options> options = new HashMap<>();
				for (final Options option : _data.values())
				{
					if (option.hasActivationSkills() || option.hasActiveSkill())
					{
						continue;
					}
					
					if (!option.hasPassiveSkill())
					{
						continue;
					}
					
					if (!options.containsKey(option.getPassiveSkill().getId()) || options.get(option.getPassiveSkill().getId()).getPassiveSkill().getLvl() < option.getPassiveSkill().getLvl())
					{
						for (final int id : Config.SERVICES_AUGMENTATION_AVAILABLE_LIST)
						{
							if (id == option.getId())
							{
								options.put(option.getPassiveSkill().getId(), option);
							}
						}
					}
				}
				augs = new ArrayList<>(options.values());
				Collections.sort(augs, new PassiveSkillsComparator());
				break;
			}
			case CHANCE_SKILL :
			{
				final Map<Integer, Options> options = new HashMap<>();
				for (final Options option : _data.values())
				{
					if (option.hasPassiveSkill() || option.hasActiveSkill())
					{
						continue;
					}
					
					if (!option.hasActivationSkills())
					{
						continue;
					}
					
					if (!options.containsKey(option.getActivationsSkills().get(0).getId()) || options.get(option.getActivationsSkills().get(0).getId()).getActivationsSkills().get(0).getLvl() < option.getActivationsSkills().get(0).getLvl())
					{
						for (final int id : Config.SERVICES_AUGMENTATION_AVAILABLE_LIST)
						{
							if (id == option.getId())
							{
								options.put(option.getActivationsSkills().get(0).getId(), option);
							}
						}
					}
				}
				augs = new ArrayList<>(options.values());
				Collections.sort(augs, new ChanceSkillsComparator());
				break;
			}
			case STATS :
			{
				final Map<Integer, Options> options = new HashMap<>();
				for (final Options option : _data.values())
				{
					switch (option.getId())
					{
						case 16341 :
						case 16342 :
						case 16343 :
						case 16344 :
							for (final int id : Config.SERVICES_AUGMENTATION_AVAILABLE_LIST)
							{
								if (id == option.getId())
								{
									options.put(option.getId(), option);
								}
							}
							break;
					}
				}
				augs = new ArrayList<>(options.values());
				break;
			}
		}
		return augs;
	}
	
	protected static class ActiveSkillsComparator implements Comparator<Options>
	{
		@Override
		public int compare(Options left, Options right)
		{
			if (!left.hasActiveSkill() || !right.hasActiveSkill())
			{
				return 0;
			}
			return Integer.valueOf(left.getActiveSkill().getId()).compareTo(right.getActiveSkill().getId());
		}
	}
	
	protected static class PassiveSkillsComparator implements Comparator<Options>
	{
		@Override
		public int compare(Options left, Options right)
		{
			if (!left.hasPassiveSkill() || !right.hasPassiveSkill())
			{
				return 0;
			}
			return Integer.valueOf(left.getPassiveSkill().getId()).compareTo(right.getPassiveSkill().getId());
		}
	}
	
	protected static class ChanceSkillsComparator implements Comparator<Options>
	{
		@Override
		public int compare(Options left, Options right)
		{
			if (!left.hasActivationSkills() || !right.hasActivationSkills())
			{
				return 0;
			}
			return Integer.valueOf(left.getActivationsSkills().get(0).getId()).compareTo(right.getActivationsSkills().get(0).getId());
		}
	}

	public static final OptionsParser getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final OptionsParser _instance = new OptionsParser();
	}
}