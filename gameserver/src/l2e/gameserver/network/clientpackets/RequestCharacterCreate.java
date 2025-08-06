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
package l2e.gameserver.network.clientpackets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import l2e.commons.log.Log;
import l2e.commons.util.Util;
import l2e.fake.FakePlayerNameManager;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.parser.*;
import l2e.gameserver.handler.communityhandlers.impl.CommunityBuffer;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.appearance.PcAppearance;
import l2e.gameserver.model.actor.stat.PcStat;
import l2e.gameserver.model.actor.templates.player.PcTemplate;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.items.PcItemTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.skills.PcBuffTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.serverpackets.CharacterCreateFail;
import l2e.gameserver.network.serverpackets.CharacterCreateSuccess;
import l2e.gameserver.network.serverpackets.CharacterSelectionInfo;
import l2e.gameserver.network.serverpackets.NewCharacterFail;

public final class RequestCharacterCreate extends GameClientPacket
{
	private String _name;
	protected int _race;
	private byte _sex;
	private int _classId;
	protected int _int;
	protected int _str;
	protected int _con;
	protected int _men;
	protected int _dex;
	protected int _wit;
	private byte _hairStyle;
	private byte _hairColor;
	private byte _face;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
		_race = readD();
		_sex = (byte) readD();
		_classId = readD();
		_int = readD();
		_str = readD();
		_con = readD();
		_men = readD();
		_dex = readD();
		_wit = readD();
		_hairStyle = (byte) readD();
		_hairColor = (byte) readD();
		_face = (byte) readD();
	}
	
	@Override
	protected void runImpl()
	{
		if ((_name.length() < 1) || (_name.length() > 16))
		{
			if (Config.DEBUG)
			{
				_log.info("Character Creation Failure: Character name " + _name + " is invalid. Message generated: Your title cannot exceed 16 characters in length. Please try again.");
			}
			
			sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_16_ENG_CHARS));
			return;
		}
		
		if (Config.FORBIDDEN_NAMES.length > 1)
		{
			for (final String st : Config.FORBIDDEN_NAMES)
			{
				if (_name.toLowerCase().contains(st.toLowerCase()))
				{
					sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_INCORRECT_NAME));
					return;
				}
			}
		}
		
		if (!Util.isAlphaNumeric(_name) || !isValidName(_name))
		{
			if (Config.DEBUG)
			{
				_log.info("Character Creation Failure: Character name " + _name + " is invalid. Message generated: Incorrect name. Please try again.");
			}
			
			sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_INCORRECT_NAME));
			return;
		}
		
		if ((_face > 2) || (_face < 0))
		{
			_log.warn("Character Creation Failure: Character face " + _face + " is invalid. Possible client hack. " + getClient());
			
			sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_CREATION_FAILED));
			return;
		}
		
		if ((_hairStyle < 0) || ((_sex == 0) && (_hairStyle > 4)) || ((_sex != 0) && (_hairStyle > 6)))
		{
			_log.warn("Character Creation Failure: Character hair style " + _hairStyle + " is invalid. Possible client hack. " + getClient());
			
			sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_CREATION_FAILED));
			return;
		}
		
		if ((_hairColor > 3) || (_hairColor < 0))
		{
			_log.warn("Character Creation Failure: Character hair color " + _hairColor + " is invalid. Possible client hack. " + getClient());
			
			sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_CREATION_FAILED));
			return;
		}
		
		Player newChar = null;
		PcTemplate template = null;
		
		synchronized (CharNameHolder.getInstance())
		{
			if ((CharNameHolder.getInstance().accountCharNumber(getClient().getLogin()) >= Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT) && (Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT != 0))
			{
				if (Config.DEBUG)
				{
					_log.info("Max number of characters reached. Creation failed.");
				}
				
				sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_TOO_MANY_CHARACTERS));
				return;
			}
			else if (CharNameHolder.getInstance().doesCharNameExist(_name) || (Config.ALLOW_FAKE_PLAYERS && FakePlayerNameManager.getInstance().doesCharNameExist(_name)))
			{
				if (Config.DEBUG)
				{
					_log.info("Character Creation Failure: Message generated: You cannot create another character. Please delete the existing character and try again.");
				}
				
				sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_NAME_ALREADY_EXISTS));
				return;
			}
			
			template = CharTemplateParser.getInstance().getTemplate(_classId);
			
			if ((template == null) || (ClassId.getClassId(_classId).level() > 0))
			{
				if (Config.DEBUG)
				{
					_log.info("Character Creation Failure: " + _name + " classId: " + _classId + " Template: " + template + " Message generated: Your character creation has failed.");
				}
				
				sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_CREATION_FAILED));
				return;
			}
			final PcAppearance app = new PcAppearance(_face, _hairColor, _hairStyle, _sex != 0);
			newChar = Player.create(template, getClient().getLogin(), _name, app);
		}
		
		getClient().setCharCreation(true);
		
		try
		{
			newChar.setCurrentHp(newChar.getMaxHp());
			newChar.setCurrentMp(newChar.getMaxMp());
			sendPacket(new CharacterCreateSuccess());
			initNewChar(getClient(), newChar);
			Log.addLogGame("CHARACTER CREATE:", "created!", newChar.getName(null));
		}
		catch (final Exception e)
		{
			sendPacket(new NewCharacterFail());
			_log.warn("Exception on create new char(): " + e.getMessage(), e);
		}
	}
	
	private boolean isValidName(String text)
	{
		boolean result = true;
		final String test = text;
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.CNAME_TEMPLATE);
		}
		catch (final PatternSyntaxException e)
		{
			_log.warn("ERROR : Character name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		final Matcher regexp = pattern.matcher(test);
		if (!regexp.matches())
		{
			result = false;
		}
		return result;
	}
	
	private void initNewChar(GameClient client, Player newChar)
	{
		if (Config.DEBUG)
		{
			_log.info("Character init start");
		}
		
		if (Config.STARTING_ADENA > 0)
		{
			newChar.addAdena("Init", Config.STARTING_ADENA, null, false);
		}
		
		final PcTemplate template = newChar.getTemplate();
		
		if (Config.ALLOW_NEW_CHAR_CUSTOM_POSITION)
		{
			newChar.setXYZInvisible(Config.NEW_CHAR_POSITION_X, Config.NEW_CHAR_POSITION_Y, Config.NEW_CHAR_POSITION_Z);
		}
		else
		{
			final Location createLoc = template.getCreationPoint();
			newChar.setXYZInvisible(createLoc.getX(), createLoc.getY(), createLoc.getZ());
		}
		newChar.setGlobalTitle(Config.ALLOW_NEW_CHARACTER_TITLE ? Config.NEW_CHARACTER_TITLE : "");
		newChar.setLang(newChar.getLang(client));
		if (Config.NEW_CHAR_IS_NOBLE)
		{
			Olympiad.addNoble(newChar);
			newChar.setNoble(true);
		}
		
		if (Config.ENABLE_VITALITY)
		{
			newChar.setVitalityPoints(Math.min(Config.STARTING_VITALITY_POINTS, PcStat.MAX_VITALITY_POINTS), true);
		}
		if (Config.STARTING_LEVEL > 1)
		{
			newChar.getStat().addLevel((byte) (Config.STARTING_LEVEL - 1), true);
		}
		if (Config.STARTING_SP > 0)
		{
			newChar.getStat().addSp(Config.STARTING_SP);
		}
		
		if (template.hasInitialEquipment())
		{
			ItemInstance item;
			for (final PcItemTemplate ie : template.getInitialEquipment())
			{
				item = newChar.getInventory().addItem("Multisell", ie.getId(), ie.getCount(), newChar, null);
				if (item != null && !item.isStackable())
				{
					if (ie.getAugmentId() > 0)
					{
						item.setAugmentation(new Augmentation(ie.getAugmentId()));
					}
					
					if (ie.getElementals() != null && !ie.getElementals().isEmpty())
					{
						final String[] elements = ie.getElementals().split(";");
						for (final String el : elements)
						{
							final String[] element = el.split(":");
							if (element != null)
							{
								item.setElementAttr(Byte.parseByte(element[0]), Integer.parseInt(element[1]));
							}
						}
					}
					
					if (ie.getDurability() > 0)
					{
						item.setTime(ie.getDurability());
					}
					item.setEnchantLevel(ie.getEnchant());
					item.updateDatabase();
					
					if (item.isEquipable() && ie.isEquipped())
					{
						newChar.getInventory().equipItem(item);
					}
				}
			}
		}

		if (InitialBuffParser.getInstance().isActivated() && template.hasInitialBuff())
		{
			Skill skill;
			for (final PcBuffTemplate buffTemplate : template.getInitialBuff())
			{
				skill = SkillsParser.getInstance().getInfo(buffTemplate.getSkillId(), buffTemplate.getSkillLevel());
				if (skill != null)
				{
					final int buffTime = buffTemplate.getBuffTime();
					if (buffTime > 0 && skill.hasEffects())
					{
						final Env env = new Env();
						env.setCharacter(newChar);
						env.setTarget(newChar);
						env.setSkill(skill);

						Effect ef;
						for (final EffectTemplate et : skill.getEffectTemplates())
						{
							ef = et.getEffect(env);
							if (ef != null)
							{
								ef.setAbnormalTime(buffTime * 60);
								ef.scheduleEffect(true);
							}
						}
					}
					else
					{
						skill.getEffects(newChar, newChar, false);
					}
				}
			}
		}
		
		for (final SkillLearn skill : SkillTreesParser.getInstance().getAvailableSkills(newChar, newChar.getClassId(), false, true))
		{
			newChar.addSkill(SkillsParser.getInstance().getInfo(skill.getId(), skill.getLvl()), true);
			if (Config.DEBUG)
			{
				_log.info("Adding starter skill:" + skill.getId() + " / " + skill.getLvl());
			}
		}
		
		InitialShortcutParser.getInstance().registerAllShortcuts(newChar);
		
		if (!Config.DISABLE_TUTORIAL)
		{
			startTutorialQuest(newChar);
		}
		newChar.updateAcessStatus(true);
		newChar.store();
		newChar.deleteMe();
		final CharacterSelectionInfo cl = new CharacterSelectionInfo(client.getLogin(), client.getSessionId().playOkID1);
		client.setCharSelection(cl.getCharInfo());
		client.setCharCreation(false);
		if (Config.DEBUG)
		{
			_log.info("Character init end");
		}
	}
	
	public void startTutorialQuest(Player player)
	{
		final QuestState qs = player.getQuestState("_255_Tutorial");
		Quest q = null;
		if (qs == null)
		{
			q = QuestManager.getInstance().getQuest("_255_Tutorial");
		}
		if (q != null)
		{
			q.newQuestState(player).setState(State.STARTED);
		}
	}
}