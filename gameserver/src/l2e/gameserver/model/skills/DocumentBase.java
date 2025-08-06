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
package l2e.gameserver.model.skills;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.base.PlayerState;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.items.type.ArmorType;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.skills.conditions.Condition;
import l2e.gameserver.model.skills.conditions.ConditionAgathionItemId;
import l2e.gameserver.model.skills.conditions.ConditionAngelCatEventActive;
import l2e.gameserver.model.skills.conditions.ConditionChangeWeapon;
import l2e.gameserver.model.skills.conditions.ConditionGameChance;
import l2e.gameserver.model.skills.conditions.ConditionGameTime;
import l2e.gameserver.model.skills.conditions.ConditionGameTime.CheckGameTime;
import l2e.gameserver.model.skills.conditions.ConditionLogicAnd;
import l2e.gameserver.model.skills.conditions.ConditionLogicNot;
import l2e.gameserver.model.skills.conditions.ConditionLogicOr;
import l2e.gameserver.model.skills.conditions.ConditionMinDistance;
import l2e.gameserver.model.skills.conditions.ConditionPeaceZone;
import l2e.gameserver.model.skills.conditions.ConditionPlayerActiveEffectId;
import l2e.gameserver.model.skills.conditions.ConditionPlayerActiveSkillId;
import l2e.gameserver.model.skills.conditions.ConditionPlayerAgathionId;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCallPc;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCanEscape;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCanPossessHolything;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCanRefuelAirship;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCanSummon;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCanSummonSiegeGolem;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCanSweep;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCanTakePcBangPoints;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCanTransform;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCanUntransform;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCharges;
import l2e.gameserver.model.skills.conditions.ConditionPlayerClassIdRestriction;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCloakStatus;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCombat;
import l2e.gameserver.model.skills.conditions.ConditionPlayerCp;
import l2e.gameserver.model.skills.conditions.ConditionPlayerDualDagger;
import l2e.gameserver.model.skills.conditions.ConditionPlayerEnergy;
import l2e.gameserver.model.skills.conditions.ConditionPlayerFlyMounted;
import l2e.gameserver.model.skills.conditions.ConditionPlayerGrade;
import l2e.gameserver.model.skills.conditions.ConditionPlayerHasCastle;
import l2e.gameserver.model.skills.conditions.ConditionPlayerHasClanHall;
import l2e.gameserver.model.skills.conditions.ConditionPlayerHasFort;
import l2e.gameserver.model.skills.conditions.ConditionPlayerHasPet;
import l2e.gameserver.model.skills.conditions.ConditionPlayerHasServitor;
import l2e.gameserver.model.skills.conditions.ConditionPlayerHp;
import l2e.gameserver.model.skills.conditions.ConditionPlayerInFightEvent;
import l2e.gameserver.model.skills.conditions.ConditionPlayerInsideZoneId;
import l2e.gameserver.model.skills.conditions.ConditionPlayerInstanceId;
import l2e.gameserver.model.skills.conditions.ConditionPlayerInvSize;
import l2e.gameserver.model.skills.conditions.ConditionPlayerIsClanLeader;
import l2e.gameserver.model.skills.conditions.ConditionPlayerIsHero;
import l2e.gameserver.model.skills.conditions.ConditionPlayerLandingZone;
import l2e.gameserver.model.skills.conditions.ConditionPlayerLevel;
import l2e.gameserver.model.skills.conditions.ConditionPlayerLevelRange;
import l2e.gameserver.model.skills.conditions.ConditionPlayerMp;
import l2e.gameserver.model.skills.conditions.ConditionPlayerPkCount;
import l2e.gameserver.model.skills.conditions.ConditionPlayerPledgeClass;
import l2e.gameserver.model.skills.conditions.ConditionPlayerRace;
import l2e.gameserver.model.skills.conditions.ConditionPlayerRangeFromNpc;
import l2e.gameserver.model.skills.conditions.ConditionPlayerReflectionEntry;
import l2e.gameserver.model.skills.conditions.ConditionPlayerServitorNpcId;
import l2e.gameserver.model.skills.conditions.ConditionPlayerSex;
import l2e.gameserver.model.skills.conditions.ConditionPlayerSiegeSide;
import l2e.gameserver.model.skills.conditions.ConditionPlayerSouls;
import l2e.gameserver.model.skills.conditions.ConditionPlayerState;
import l2e.gameserver.model.skills.conditions.ConditionPlayerSubclass;
import l2e.gameserver.model.skills.conditions.ConditionPlayerTransformationId;
import l2e.gameserver.model.skills.conditions.ConditionPlayerVehicleMounted;
import l2e.gameserver.model.skills.conditions.ConditionPlayerWeight;
import l2e.gameserver.model.skills.conditions.ConditionSiegeZone;
import l2e.gameserver.model.skills.conditions.ConditionSlotItemId;
import l2e.gameserver.model.skills.conditions.ConditionTargetAbnormal;
import l2e.gameserver.model.skills.conditions.ConditionTargetActiveEffectId;
import l2e.gameserver.model.skills.conditions.ConditionTargetActiveSkillId;
import l2e.gameserver.model.skills.conditions.ConditionTargetAggro;
import l2e.gameserver.model.skills.conditions.ConditionTargetClassIdRestriction;
import l2e.gameserver.model.skills.conditions.ConditionTargetInvSize;
import l2e.gameserver.model.skills.conditions.ConditionTargetLevel;
import l2e.gameserver.model.skills.conditions.ConditionTargetLevelRange;
import l2e.gameserver.model.skills.conditions.ConditionTargetMyPartyExceptMe;
import l2e.gameserver.model.skills.conditions.ConditionTargetNpcId;
import l2e.gameserver.model.skills.conditions.ConditionTargetNpcType;
import l2e.gameserver.model.skills.conditions.ConditionTargetPercentCp;
import l2e.gameserver.model.skills.conditions.ConditionTargetPercentHp;
import l2e.gameserver.model.skills.conditions.ConditionTargetPercentMp;
import l2e.gameserver.model.skills.conditions.ConditionTargetPlayable;
import l2e.gameserver.model.skills.conditions.ConditionTargetRace;
import l2e.gameserver.model.skills.conditions.ConditionTargetRaceId;
import l2e.gameserver.model.skills.conditions.ConditionTargetUsesWeaponKind;
import l2e.gameserver.model.skills.conditions.ConditionTargetWard;
import l2e.gameserver.model.skills.conditions.ConditionTargetWeight;
import l2e.gameserver.model.skills.conditions.ConditionUsingItemType;
import l2e.gameserver.model.skills.conditions.ConditionUsingSkill;
import l2e.gameserver.model.skills.conditions.ConditionWithSkill;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.funcs.FuncTemplate;
import l2e.gameserver.model.skills.funcs.Lambda;
import l2e.gameserver.model.skills.funcs.LambdaCalc;
import l2e.gameserver.model.skills.funcs.LambdaConst;
import l2e.gameserver.model.skills.funcs.LambdaStats;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.stats.StatsSet;

public abstract class DocumentBase
{
	protected final Logger _log = LoggerFactory.getLogger(getClass());
	
	private final File _file;
	protected Map<String, String[]> _tables = new HashMap<>();
	
	protected DocumentBase(File pFile)
	{
		_file = pFile;
	}
	
	public Document parse()
	{
		Document doc = null;
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(_file);
			parseDocument(doc);
		}
		catch (final Exception e)
		{
			_log.warn("Error loading file " + _file, e);
		}
		return doc;
	}
	
	protected abstract void parseDocument(Document doc);
	
	protected abstract StatsSet getStatsSet();
	
	protected abstract String getTableValue(String name);
	
	protected abstract String getTableValue(String name, int idx);
	
	protected void resetTable()
	{
		_tables.clear();
	}
	
	protected void setTable(String name, String[] table)
	{
		_tables.put(name, table);
	}
	
	protected void parseTemplate(Node n, Object template)
	{
		Condition condition = null;
		n = n.getFirstChild();
		if (n == null)
		{
			return;
		}
		if ("cond".equalsIgnoreCase(n.getNodeName()))
		{
			condition = parseCondition(n.getFirstChild(), template);
			final Node msg = n.getAttributes().getNamedItem("msg");
			final Node msgId = n.getAttributes().getNamedItem("msgId");
			if ((condition != null) && (msg != null))
			{
				condition.setMessage(msg.getNodeValue());
			}
			else if ((condition != null) && (msgId != null))
			{
				condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
				final Node addName = n.getAttributes().getNamedItem("addName");
				if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
				{
					condition.addName();
				}
			}
			n = n.getNextSibling();
		}
		for (; n != null; n = n.getNextSibling())
		{
			if ("add".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Add", condition);
			}
			else if ("sub".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Sub", condition);
			}
			else if ("mul".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Mul", condition);
			}
			else if ("basemul".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "BaseMul", condition);
			}
			else if ("div".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Div", condition);
			}
			else if ("set".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Set", condition);
			}
			else if ("share".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Share", condition);
			}
			else if ("enchant".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Enchant", condition);
			}
			else if ("enchanthp".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "EnchantHp", condition);
			}
			else if ("enchantadd".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "EnchantAdd", condition);
			}
			else if ("enchantmul".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "EnchantMul", condition);
			}
			else if ("effect".equalsIgnoreCase(n.getNodeName()))
			{
				if (template instanceof EffectTemplate)
				{
					throw new RuntimeException("Nested effects");
				}
				attachEffect(n, template, condition);
			}
		}
	}
	
	protected void attachFunc(Node n, Object template, String name, Condition attachCond)
	{
		final Stats stat = Stats.valueOfXml(n.getAttributes().getNamedItem("stat").getNodeValue());
		final String order = n.getAttributes().getNamedItem("order").getNodeValue();
		final Lambda lambda = getLambda(n, template);
		final int ord = Integer.decode(getValue(order, template));
		final Condition applayCond = parseCondition(n.getFirstChild(), template);
		final FuncTemplate ft = new FuncTemplate(attachCond, applayCond, name, stat, ord, lambda);
		if (template instanceof Item)
		{
			((Item) template).attach(ft);
		}
		else if (template instanceof Skill)
		{
			((Skill) template).attach(ft);
		}
		else if (template instanceof EffectTemplate)
		{
			((EffectTemplate) template).attach(ft);
		}
	}
	
	protected void attachLambdaFunc(Node n, Object template, LambdaCalc calc)
	{
		String name = n.getNodeName();
		final StringBuilder sb = new StringBuilder(name);
		sb.setCharAt(0, Character.toUpperCase(name.charAt(0)));
		name = sb.toString();
		final Lambda lambda = getLambda(n, template);
		final FuncTemplate ft = new FuncTemplate(null, null, name, null, calc.funcs.length, lambda);
		calc.addFunc(ft.getFunc(new Env(), calc));
	}
	
	protected void attachEffect(Node n, Object template, Condition attachCond)
	{
		final NamedNodeMap attrs = n.getAttributes();
		final StatsSet set = new StatsSet();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node att = attrs.item(i);
			set.set(att.getNodeName(), getValue(att.getNodeValue(), template));
		}
		
		StatsSet parameters = parseParameters(n.getFirstChild(), template);
		if (parameters == null)
		{
			parameters = new StatsSet();
		}
		final Lambda lambda = getLambda(n, template);
		final Condition applayCond = parseCondition(n.getFirstChild(), template);
		
		if (template instanceof IIdentifiable)
		{
			set.set("id", ((IIdentifiable) template).getId());
		}
		
		byte abnormalLvl = 0;
		String abnormalType = "none";
		if (attrs.getNamedItem("abnormalType") != null)
		{
			abnormalType = attrs.getNamedItem("abnormalType").getNodeValue();
		}
		if (attrs.getNamedItem("abnormalLvl") != null)
		{
			abnormalLvl = Byte.parseByte(getValue(attrs.getNamedItem("abnormalLvl").getNodeValue(), template));
		}
		
		final EffectTemplate effectTemplate = new EffectTemplate(attachCond, applayCond, lambda, abnormalType, abnormalLvl, set, parameters);
		parseTemplate(n, effectTemplate);
		if (template instanceof Item)
		{
			((Item) template).attach(effectTemplate);
		}
		else if (template instanceof Skill)
		{
			final Skill sk = (Skill) template;
			if (set.getInteger("self", 0) == 1)
			{
				sk.attachSelf(effectTemplate);
			}
			else if (sk.isPassive())
			{
				sk.attachPassive(effectTemplate);
			}
			else
			{
				sk.attach(effectTemplate);
			}
		}
	}
	
	private StatsSet parseParameters(Node n, Object template)
	{
		StatsSet parameters = null;
		while ((n != null))
		{
			if ((n.getNodeType() == Node.ELEMENT_NODE) && "param".equals(n.getNodeName()))
			{
				if (parameters == null)
				{
					parameters = new StatsSet();
				}
				final NamedNodeMap params = n.getAttributes();
				for (int i = 0; i < params.getLength(); i++)
				{
					final Node att = params.item(i);
					parameters.set(att.getNodeName(), getValue(att.getNodeValue(), template));
				}
			}
			n = n.getNextSibling();
		}
		return parameters;
	}
	
	protected Condition parseCondition(Node n, Object template)
	{
		while ((n != null) && (n.getNodeType() != Node.ELEMENT_NODE))
		{
			n = n.getNextSibling();
		}
		
		Condition condition = null;
		if (n != null)
		{
			switch (n.getNodeName())
			{
				case "and" :
				{
					condition = parseLogicAnd(n, template);
					break;
				}
				case "or" :
				{
					condition = parseLogicOr(n, template);
					break;
				}
				case "not" :
				{
					condition = parseLogicNot(n, template);
					break;
				}
				case "player" :
				{
					condition = parsePlayerCondition(n, template);
					break;
				}
				case "target" :
				{
					condition = parseTargetCondition(n, template);
					break;
				}
				case "using" :
				{
					condition = parseUsingCondition(n);
					break;
				}
				case "game" :
				{
					condition = parseGameCondition(n);
					break;
				}
			}
		}
		return condition;
	}
	
	protected Condition parseLogicAnd(Node n, Object template)
	{
		final ConditionLogicAnd cond = new ConditionLogicAnd();
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				cond.add(parseCondition(n, template));
			}
		}
		if ((cond.conditions == null) || (cond.conditions.length == 0))
		{
			_log.error("Empty <and> condition in " + _file);
		}
		return cond;
	}
	
	protected Condition parseLogicOr(Node n, Object template)
	{
		final ConditionLogicOr cond = new ConditionLogicOr();
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				cond.add(parseCondition(n, template));
			}
		}
		if ((cond.conditions == null) || (cond.conditions.length == 0))
		{
			_log.error("Empty <or> condition in " + _file);
		}
		return cond;
	}
	
	protected Condition parseLogicNot(Node n, Object template)
	{
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				return new ConditionLogicNot(parseCondition(n, template));
			}
		}
		_log.error("Empty <not> condition in " + _file);
		return null;
	}
	
	protected Condition parsePlayerCondition(Node n, Object template)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("races".equalsIgnoreCase(a.getNodeName()))
			{
				final String[] racesVal = a.getNodeValue().split(",");
				final Race[] races = new Race[racesVal.length];
				for (int r = 0; r < racesVal.length; r++)
				{
					if (racesVal[r] != null)
					{
						races[r] = Race.valueOf(racesVal[r]);
					}
				}
				cond = joinAnd(cond, new ConditionPlayerRace(races));
			}
			else if ("level".equalsIgnoreCase(a.getNodeName()))
			{
				final int lvl = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerLevel(lvl));
			}
			else if ("levelRange".equalsIgnoreCase(a.getNodeName()))
			{
				final String[] range = getValue(a.getNodeValue(), template).split(";");
				if (range.length == 2)
				{
					final int[] lvlRange = new int[2];
					lvlRange[0] = Integer.decode(getValue(a.getNodeValue(), template).split(";")[0]);
					lvlRange[1] = Integer.decode(getValue(a.getNodeValue(), template).split(";")[1]);
					cond = joinAnd(cond, new ConditionPlayerLevelRange(lvlRange));
				}
			}
			else if ("resting".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.RESTING, val, new ArrayList<>()));
			}
			else if ("flying".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.FLYING, val, new ArrayList<>()));
			}
			else if ("moving".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.MOVING, val, new ArrayList<>()));
			}
			else if ("running".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.RUNNING, val, new ArrayList<>()));
			}
			else if ("standing".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.STANDING, val, new ArrayList<>()));
			}
			else if ("behind".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.BEHIND, val, new ArrayList<>()));
			}
			else if ("front".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.FRONT, val, new ArrayList<>()));
			}
			else if ("chaotic".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.CHAOTIC, val, new ArrayList<>()));
			}
			else if ("olympiad".equalsIgnoreCase(a.getNodeName()))
			{
				final List<Integer> classId = new ArrayList<>();
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				for (Node o = n.getFirstChild(); o != null; o = o.getNextSibling())
				{
					if ("class".equalsIgnoreCase(o.getNodeName()))
					{
						final String ids = o.getAttributes().getNamedItem("id").getNodeValue();
						for (final String id : ids.split(","))
						{
							classId.add(Integer.decode(getValue(id, null)));
						}
					}
				}
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.OLYMPIAD, val, classId));
			}
			else if ("agathionEnergy".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerEnergy(val));
			}
			else if ("ishero".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerIsHero(val));
			}
			else if ("transformationId".equalsIgnoreCase(a.getNodeName()))
			{
				final int id = Integer.parseInt(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerTransformationId(id));
			}
			else if ("hp".equalsIgnoreCase(a.getNodeName()))
			{
				final int hp = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerHp(hp));
			}
			else if ("mp".equalsIgnoreCase(a.getNodeName()))
			{
				final int hp = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerMp(hp));
			}
			else if ("cp".equalsIgnoreCase(a.getNodeName()))
			{
				final int cp = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerCp(cp));
			}
			else if ("grade".equalsIgnoreCase(a.getNodeName()))
			{
				final int expIndex = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerGrade(expIndex));
			}
			else if ("pkCount".equalsIgnoreCase(a.getNodeName()))
			{
				final int expIndex = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerPkCount(expIndex));
			}
			else if ("peacezone".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPeaceZone(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("siegezone".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionSiegeZone(Boolean.parseBoolean(a.getNodeValue()), true));
			}
			else if ("siegeside".equalsIgnoreCase(a.getNodeName()))
			{
				final int value = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerSiegeSide(value));
			}
			else if ("charges".equalsIgnoreCase(a.getNodeName()))
			{
				final int value = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerCharges(value));
			}
			else if ("souls".equalsIgnoreCase(a.getNodeName()))
			{
				final int value = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerSouls(value));
			}
			else if ("weight".equalsIgnoreCase(a.getNodeName()))
			{
				final int weight = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerWeight(weight));
			}
			else if ("invSize".equalsIgnoreCase(a.getNodeName()))
			{
				final int size = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerInvSize(size));
			}
			else if ("isClanLeader".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerIsClanLeader(val));
			}
			else if ("pledgeClass".equalsIgnoreCase(a.getNodeName()))
			{
				final int pledgeClass = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerPledgeClass(pledgeClass));
			}
			else if ("clanHall".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				final ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionPlayerHasClanHall(array));
			}
			else if ("fort".equalsIgnoreCase(a.getNodeName()))
			{
				final int fort = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerHasFort(fort));
			}
			else if ("castle".equalsIgnoreCase(a.getNodeName()))
			{
				final int castle = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerHasCastle(castle));
			}
			else if ("sex".equalsIgnoreCase(a.getNodeName()))
			{
				final int sex = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerSex(sex));
			}
			else if ("flyMounted".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerFlyMounted(val));
			}
			else if ("vehicleMounted".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerVehicleMounted(val));
			}
			else if ("landingZone".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerLandingZone(val));
			}
			else if ("active_effect_id".equalsIgnoreCase(a.getNodeName()))
			{
				final int effect_id = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerActiveEffectId(effect_id));
			}
			else if ("active_effect_id_lvl".equalsIgnoreCase(a.getNodeName()))
			{
				final String val = getValue(a.getNodeValue(), template);
				final int effect_id = Integer.decode(getValue(val.split(",")[0], template));
				final int effect_lvl = Integer.decode(getValue(val.split(",")[1], template));
				cond = joinAnd(cond, new ConditionPlayerActiveEffectId(effect_id, effect_lvl));
			}
			else if ("active_skill_id".equalsIgnoreCase(a.getNodeName()))
			{
				final int skill_id = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerActiveSkillId(skill_id));
			}
			else if ("active_skill_id_lvl".equalsIgnoreCase(a.getNodeName()))
			{
				final String val = getValue(a.getNodeValue(), template);
				final int skill_id = Integer.decode(getValue(val.split(",")[0], template));
				final int skill_lvl = Integer.decode(getValue(val.split(",")[1], template));
				cond = joinAnd(cond, new ConditionPlayerActiveSkillId(skill_id, skill_lvl));
			}
			else if ("class_id_restriction".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				final ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionPlayerClassIdRestriction(array));
			}
			else if ("subclass".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerSubclass(val));
			}
			else if ("instanceId".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				final ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionPlayerInstanceId(array));
			}
			else if ("agathionId".equalsIgnoreCase(a.getNodeName()))
			{
				final int agathionId = Integer.decode(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerAgathionId(agathionId));
			}
			else if ("agathionItems".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				final ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionAgathionItemId(array));
			}
			else if ("cloakStatus".equalsIgnoreCase(a.getNodeName()))
			{
				final int val = Integer.parseInt(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerCloakStatus(val));
			}
			else if ("hasPet".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				final ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionPlayerHasPet(array));
			}
			else if ("hasservitor".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerHasServitor());
			}
			else if ("servitorNpcId".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				final ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionPlayerServitorNpcId(array));
			}
			else if ("npcIdRadius".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				if (st.countTokens() == 3)
				{
					final String[] ids = st.nextToken().split(";");
					final int[] npcIds = new int[ids.length];
					for (int index = 0; index < ids.length; index++)
					{
						npcIds[index] = Integer.parseInt(getValue(ids[index], template));
					}
					final int radius = Integer.parseInt(st.nextToken());
					final boolean val = Boolean.parseBoolean(st.nextToken());
					cond = joinAnd(cond, new ConditionPlayerRangeFromNpc(npcIds, radius, val));
				}
			}
			else if ("callPc".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerCallPc(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("canEscape".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerCanEscape(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("canPossessHolything".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerCanPossessHolything(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("canRefuelAirship".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerCanRefuelAirship(Integer.parseInt(a.getNodeValue())));
			}
			else if ("cansummon".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerCanSummon(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("cansummonsiegegolem".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerCanSummonSiegeGolem(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("canSweep".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerCanSweep(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("canTransform".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerCanTransform(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("canUntransform".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerCanUntransform(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("insideZoneId".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				final ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionPlayerInsideZoneId(array));
			}
			else if ("reflectionEntry".equalsIgnoreCase(a.getNodeName()))
			{
				final String val = getValue(a.getNodeValue(), template);
				final int type = Integer.decode(getValue(val.split(":")[0], template));
				final int attempts = Integer.decode(getValue(val.split(":")[1], template));
				cond = joinAnd(cond, new ConditionPlayerReflectionEntry(type, attempts));
			}
			else if ("canPcBangPoints".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerCanTakePcBangPoints(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("isAngelCatEventActive".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionAngelCatEventActive(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("isInFightEvent".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerInFightEvent(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("canUseDualDaggers".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerDualDagger(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("isValidTarget".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionTargetWard(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("isInCombat".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionPlayerCombat(Boolean.parseBoolean(a.getNodeValue())));
			}
		}
		
		if (cond == null)
		{
			_log.error("Unrecognized <player> condition in " + _file);
		}
		
		return cond;
	}
	
	protected Condition parseTargetCondition(Node n, Object template)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("aggro".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetAggro(val));
			}
			else if ("siegezone".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionSiegeZone(Boolean.parseBoolean(a.getNodeValue()), false));
			}
			else if ("level".equalsIgnoreCase(a.getNodeName()))
			{
				final int lvl = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionTargetLevel(lvl));
			}
			else if ("levelRange".equalsIgnoreCase(a.getNodeName()))
			{
				final String[] range = getValue(a.getNodeValue(), template).split(";");
				if (range.length == 2)
				{
					final int[] lvlRange = new int[2];
					lvlRange[0] = Integer.decode(getValue(a.getNodeValue(), template).split(";")[0]);
					lvlRange[1] = Integer.decode(getValue(a.getNodeValue(), template).split(";")[1]);
					cond = joinAnd(cond, new ConditionTargetLevelRange(lvlRange));
				}
			}
			else if ("myPartyExceptMe".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionTargetMyPartyExceptMe(Boolean.parseBoolean(a.getNodeValue())));
			}
			else if ("playable".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionTargetPlayable());
			}
			else if ("percentHP".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionTargetPercentHp(parseNumber(a.getNodeValue()).intValue()));
			}
			else if ("percentMP".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionTargetPercentMp(parseNumber(a.getNodeValue()).intValue()));
			}
			else if ("percentCP".equalsIgnoreCase(a.getNodeName()))
			{
				cond = joinAnd(cond, new ConditionTargetPercentCp(parseNumber(a.getNodeValue()).intValue()));
			}
			else if ("class_id_restriction".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				final ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionTargetClassIdRestriction(array));
			}
			else if ("active_effect_id".equalsIgnoreCase(a.getNodeName()))
			{
				final int effect_id = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionTargetActiveEffectId(effect_id));
			}
			else if ("active_effect_id_lvl".equalsIgnoreCase(a.getNodeName()))
			{
				final String val = getValue(a.getNodeValue(), template);
				final int effect_id = Integer.decode(getValue(val.split(",")[0], template));
				final int effect_lvl = Integer.decode(getValue(val.split(",")[1], template));
				cond = joinAnd(cond, new ConditionTargetActiveEffectId(effect_id, effect_lvl));
			}
			else if ("active_skill_id".equalsIgnoreCase(a.getNodeName()))
			{
				final int skill_id = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionTargetActiveSkillId(skill_id));
			}
			else if ("active_skill_id_lvl".equalsIgnoreCase(a.getNodeName()))
			{
				final String val = getValue(a.getNodeValue(), template);
				final int skill_id = Integer.decode(getValue(val.split(",")[0], template));
				final int skill_lvl = Integer.decode(getValue(val.split(",")[1], template));
				cond = joinAnd(cond, new ConditionTargetActiveSkillId(skill_id, skill_lvl));
			}
			else if ("abnormal".equalsIgnoreCase(a.getNodeName()))
			{
				final int abnormalId = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionTargetAbnormal(abnormalId));
			}
			else if ("mindistance".equalsIgnoreCase(a.getNodeName()))
			{
				final int distance = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionMinDistance(distance * distance));
			}
			else if ("race_id".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				final ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionTargetRaceId(array));
			}
			else if ("races".equalsIgnoreCase(a.getNodeName()))
			{
				final String[] racesVal = a.getNodeValue().split(",");
				final Race[] races = new Race[racesVal.length];
				for (int r = 0; r < racesVal.length; r++)
				{
					if (racesVal[r] != null)
					{
						races[r] = Race.valueOf(racesVal[r]);
					}
				}
				cond = joinAnd(cond, new ConditionTargetRace(races));
			}
			else if ("using".equalsIgnoreCase(a.getNodeName()))
			{
				int mask = 0;
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					for (final WeaponType wt : WeaponType.values())
					{
						if (wt.toString().equals(item))
						{
							mask |= wt.mask();
							break;
						}
					}
					for (final ArmorType at : ArmorType.values())
					{
						if (at.toString().equals(item))
						{
							mask |= at.mask();
							break;
						}
					}
				}
				cond = joinAnd(cond, new ConditionTargetUsesWeaponKind(mask));
			}
			else if ("npcId".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				final ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionTargetNpcId(array));
			}
			else if ("npcType".equalsIgnoreCase(a.getNodeName()))
			{
				final String values = getValue(a.getNodeValue(), template).trim();
				final String[] valuesSplit = values.split(",");
				
				final InstanceType[] types = new InstanceType[valuesSplit.length];
				InstanceType type;
				
				for (int j = 0; j < valuesSplit.length; j++)
				{
					type = Enum.valueOf(InstanceType.class, valuesSplit[j]);
					if (type == null)
					{
						throw new IllegalArgumentException("Instance type not recognized: " + valuesSplit[j]);
					}
					types[j] = type;
				}
				
				cond = joinAnd(cond, new ConditionTargetNpcType(types));
			}
			else if ("weight".equalsIgnoreCase(a.getNodeName()))
			{
				final int weight = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionTargetWeight(weight));
			}
			else if ("invSize".equalsIgnoreCase(a.getNodeName()))
			{
				final int size = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionTargetInvSize(size));
			}
		}
		if (cond == null)
		{
			_log.error("Unrecognized <target> condition in " + _file);
		}
		return cond;
	}
	
	protected Condition parseUsingCondition(Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("kind".equalsIgnoreCase(a.getNodeName()))
			{
				int mask = 0;
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					final int old = mask;
					final String item = st.nextToken().trim();
					if (ItemsParser._weaponTypes.containsKey(item))
					{
						mask |= ItemsParser._weaponTypes.get(item).mask();
					}
					
					if (ItemsParser._armorTypes.containsKey(item))
					{
						mask |= ItemsParser._armorTypes.get(item).mask();
					}
					
					if (old == mask)
					{
						_log.info("[parseUsingCondition=\"kind\"] Unknown item type name: " + item);
					}
				}
				cond = joinAnd(cond, new ConditionUsingItemType(mask));
			}
			else if ("skill".equalsIgnoreCase(a.getNodeName()))
			{
				final int id = Integer.parseInt(a.getNodeValue());
				cond = joinAnd(cond, new ConditionUsingSkill(id));
			}
			else if ("slotitem".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ";");
				final int id = Integer.parseInt(st.nextToken().trim());
				final int slot = Integer.parseInt(st.nextToken().trim());
				int enchant = 0;
				if (st.hasMoreTokens())
				{
					enchant = Integer.parseInt(st.nextToken().trim());
				}
				cond = joinAnd(cond, new ConditionSlotItemId(slot, id, enchant));
			}
			else if ("weaponChange".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionChangeWeapon(val));
			}
		}
		if (cond == null)
		{
			_log.error("Unrecognized <using> condition in " + _file);
		}
		return cond;
	}
	
	protected Condition parseGameCondition(Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("skill".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionWithSkill(val));
			}
			if ("night".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.parseBoolean(a.getNodeValue());
				cond = joinAnd(cond, new ConditionGameTime(CheckGameTime.NIGHT, val));
			}
			if ("chance".equalsIgnoreCase(a.getNodeName()))
			{
				final int val = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionGameChance(val));
			}
		}
		if (cond == null)
		{
			_log.error("Unrecognized <game> condition in " + _file);
		}
		return cond;
	}
	
	protected void parseTable(Node n)
	{
		final NamedNodeMap attrs = n.getAttributes();
		final String name = attrs.getNamedItem("name").getNodeValue();
		if (name.charAt(0) != '#')
		{
			throw new IllegalArgumentException("Table name must start with #");
		}
		final StringTokenizer data = new StringTokenizer(n.getFirstChild().getNodeValue());
		final List<String> array = new ArrayList<>(data.countTokens());
		while (data.hasMoreTokens())
		{
			array.add(data.nextToken());
		}
		setTable(name, array.toArray(new String[array.size()]));
	}
	
	protected void parseBeanSet(Node n, StatsSet set, Integer level)
	{
		final String name = n.getAttributes().getNamedItem("name").getNodeValue().trim();
		final String value = n.getAttributes().getNamedItem("val").getNodeValue().trim();
		final char ch = value.isEmpty() ? ' ' : value.charAt(0);
		if ((ch == '#') || (ch == '-') || Character.isDigit(ch))
		{
			set.set(name, String.valueOf(getValue(value, level)));
		}
		else
		{
			set.set(name, value);
		}
	}
	
	protected void setExtractableSkillData(StatsSet set, String value)
	{
		set.set("capsuled_items_skill", value);
	}
	
	protected Lambda getLambda(Node n, Object template)
	{
		final Node nval = n.getAttributes().getNamedItem("val");
		if (nval != null)
		{
			final String val = nval.getNodeValue();
			if (val.charAt(0) == '#')
			{
				return new LambdaConst(Double.parseDouble(getTableValue(val)));
			}
			else if (val.charAt(0) == '$')
			{
				if (val.equalsIgnoreCase("$player_level"))
				{
					return new LambdaStats(LambdaStats.StatsType.PLAYER_LEVEL);
				}
				if (val.equalsIgnoreCase("$target_level"))
				{
					return new LambdaStats(LambdaStats.StatsType.TARGET_LEVEL);
				}
				if (val.equalsIgnoreCase("$player_max_hp"))
				{
					return new LambdaStats(LambdaStats.StatsType.PLAYER_MAX_HP);
				}
				if (val.equalsIgnoreCase("$player_max_mp"))
				{
					return new LambdaStats(LambdaStats.StatsType.PLAYER_MAX_MP);
				}
				final StatsSet set = getStatsSet();
				final String field = set.getString(val.substring(1));
				if (field != null)
				{
					return new LambdaConst(Double.parseDouble(getValue(field, template)));
				}
				throw new IllegalArgumentException("Unknown value " + val);
			}
			else
			{
				return new LambdaConst(Double.parseDouble(val));
			}
		}
		final LambdaCalc calc = new LambdaCalc();
		n = n.getFirstChild();
		while ((n != null) && (n.getNodeType() != Node.ELEMENT_NODE))
		{
			n = n.getNextSibling();
		}
		if ((n == null) || !"val".equals(n.getNodeName()))
		{
			throw new IllegalArgumentException("Value not specified");
		}
		
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() != Node.ELEMENT_NODE)
			{
				continue;
			}
			attachLambdaFunc(n, template, calc);
		}
		return calc;
	}
	
	protected String getValue(String value, Object template)
	{
		if (value.charAt(0) == '#')
		{
			if (template instanceof Skill)
			{
				return getTableValue(value);
			}
			else if (template instanceof Integer)
			{
				return getTableValue(value, ((Integer) template).intValue());
			}
			else
			{
				throw new IllegalStateException();
			}
		}
		return value;
	}
	
	protected Number parseNumber(String value)
	{
		if (value.charAt(0) == '#')
		{
			value = getTableValue(value).toString();
		}
		try
		{
			if (value.equalsIgnoreCase("max"))
			{
				return Double.POSITIVE_INFINITY;
			}
			if (value.equalsIgnoreCase("min"))
			{
				return Double.NEGATIVE_INFINITY;
			}

			if (value.indexOf('.') == -1)
			{
				int radix = 10;
				if (value.length() > 2 && value.substring(0, 2).equalsIgnoreCase("0x"))
				{
					value = value.substring(2);
					radix = 16;
				}
				return Integer.valueOf(value, radix);
			}
			return Double.valueOf(value);
		}
		catch (final NumberFormatException e)
		{
			return null;
		}
	}
	
	protected Condition joinAnd(Condition cond, Condition c)
	{
		if (cond == null)
		{
			return c;
		}
		if (cond instanceof ConditionLogicAnd)
		{
			((ConditionLogicAnd) cond).add(c);
			return cond;
		}
		final ConditionLogicAnd and = new ConditionLogicAnd();
		and.add(cond);
		and.add(c);
		return and;
	}
}