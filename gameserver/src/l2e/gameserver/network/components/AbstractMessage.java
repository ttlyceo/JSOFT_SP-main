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
package l2e.gameserver.network.components;

import java.io.PrintStream;
import java.util.Arrays;

import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.GameServerPacket;

@SuppressWarnings("unchecked")
public abstract class AbstractMessage<T extends AbstractMessage<?>> extends GameServerPacket
{
	private static final SMParam[] EMPTY_PARAM_ARRAY = new SMParam[0];
	
	private static final class SMParam
	{
		private final byte _type;
		private final Object _value;
		
		public SMParam(final byte type, final Object value)
		{
			_type = type;
			_value = value;
		}
		
		public final byte getType()
		{
			return _type;
		}
		
		public final String getStringValue()
		{
			return (String) _value;
		}
		
		public final int getIntValue()
		{
			return ((Integer) _value).intValue();
		}
		
		public final long getLongValue()
		{
			return ((Long) _value).longValue();
		}
		
		public final int[] getIntArrayValue()
		{
			return (int[]) _value;
		}
	}
	
	private static final byte TYPE_SYSTEM_STRING = 13;
	private static final byte TYPE_PLAYER_NAME = 12;
	private static final byte TYPE_DOOR_NAME = 11;
	private static final byte TYPE_INSTANCE_NAME = 10;
	private static final byte TYPE_ELEMENT_NAME = 9;
	private static final byte TYPE_ZONE_NAME = 7;
	private static final byte TYPE_LONG_NUMBER = 6;
	private static final byte TYPE_CASTLE_NAME = 5;
	private static final byte TYPE_SKILL_NAME = 4;
	private static final byte TYPE_ITEM_NAME = 3;
	private static final byte TYPE_NPC_NAME = 2;
	private static final byte TYPE_INT_NUMBER = 1;
	private static final byte TYPE_TEXT = 0;
	
	private SMParam[] _params;
	private final SystemMessageId _smId;
	private int _paramIndex;
	
	public AbstractMessage(SystemMessageId smId)
	{
		if (smId == null)
		{
			throw new NullPointerException("SystemMessageId cannot be null!");
		}
		_smId = smId;
		_params = smId.getParamCount() > 0 ? new SMParam[smId.getParamCount()] : EMPTY_PARAM_ARRAY;
	}
	
	public final int getId()
	{
		return _smId.getId();
	}
	
	public final SystemMessageId getSystemMessageId()
	{
		return _smId;
	}
	
	private final void append(SMParam param)
	{
		if (_paramIndex >= _params.length)
		{
			_params = Arrays.copyOf(_params, _paramIndex + 1);
			_smId.setParamCount(_paramIndex + 1);
			_log.info("Wrong parameter count '" + (_paramIndex + 1) + "' for SystemMessageId: " + _smId);
		}
		
		_params[_paramIndex++] = param;
	}
	
	public final T addString(final String text)
	{
		append(new SMParam(TYPE_TEXT, text));
		return (T) this;
	}
	
	public final T addCastleId(final int number)
	{
		append(new SMParam(TYPE_CASTLE_NAME, number));
		return (T) this;
	}
	
	public final T addInt(final int number)
	{
		append(new SMParam(TYPE_INT_NUMBER, number));
		return (T) this;
	}
	
	public final T addLong(final long number)
	{
		append(new SMParam(TYPE_LONG_NUMBER, number));
		return (T) this;
	}
	
	public final T addNumber(final int number)
	{
		append(new SMParam(TYPE_INT_NUMBER, number));
		return (T) this;
	}
	
	public final T addItemNumber(final long number)
	{
		append(new SMParam(TYPE_LONG_NUMBER, number));
		return (T) this;
	}
	
	public final T addCharName(final Creature cha)
	{
		if (cha.isNpc())
		{
			final Npc npc = (Npc) cha;
			// if (getClient().getActiveChar() != null)
			// {
			// return
			// addString(getClient().getActiveChar().getLang().equalsIgnoreCase("en")
			// ? npc.getTemplate().getName() : npc.getTemplate().getNameRu());
			// }
			return addNpcName(npc);
		}
		else if (cha.isPlayer())
		{
			return addPcName(cha.getActingPlayer());
		}
		else if (cha.isSummon())
		{
			final Summon summon = (Summon) cha;
			// if (getClient().getActiveChar() != null)
			// {
			// return
			// addString(getClient().getActiveChar().getLang().equalsIgnoreCase("en")
			// ? summon.getTemplate().getName() :
			// summon.getTemplate().getNameRu());
			// }
			return addNpcName(summon);
		}
		else if (cha.isDoor())
		{
			final DoorInstance door = (DoorInstance) cha;
			return addDoorName(door.getId());
		}
		return addString(cha.getName(null));
	}
	
	public final T addPcName(final Player pc)
	{
		append(new SMParam(TYPE_PLAYER_NAME, pc.getAppearance().getVisibleName()));
		return (T) this;
	}
	
	public final T addDoorName(int doorId)
	{
		append(new SMParam(TYPE_DOOR_NAME, doorId));
		return (T) this;
	}
	
	public final T addNpcName(Npc npc)
	{
		return addNpcName(npc.getTemplate());
	}
	
	public final T addNpcName(final Summon npc)
	{
		return addNpcName(npc.getId());
	}
	
	public final T addNpcName(final NpcTemplate template)
	{
		if (template.isCustom())
		{
			return addString(template.getName(null));
		}
		return addNpcName(template.getId());
	}
	
	public final T addNpcName(final int id)
	{
		append(new SMParam(TYPE_NPC_NAME, 1000000 + id));
		return (T) this;
	}
	
	public T addItemName(final ItemInstance item)
	{
		return addItemName(item.getId());
	}
	
	public T addItemName(final Item item)
	{
		return addItemName(item.getId());
	}
	
	public final T addItemName(final int id)
	{
		final Item item = ItemsParser.getInstance().getTemplate(id);
		if (item != null && item.getDisplayId() != id)
		{
			return addString(item.getName(null));
		}
		append(new SMParam(TYPE_ITEM_NAME, id));
		return (T) this;
	}
	
	public final T addZoneName(final int x, final int y, final int z)
	{
		append(new SMParam(TYPE_ZONE_NAME, new int[]
		{
			x,
			y,
			z
		}));
		return (T) this;
	}
	
	public final T addSkillName(final Effect effect)
	{
		return addSkillName(effect.getSkill());
	}
	
	public final T addSkillName(final Skill skill)
	{
		if (skill.getId() != skill.getDisplayId())
		{
			return addString(skill.getName(null));
		}
		return addSkillName(skill.getId(), skill.getLevel());
	}
	
	public final T addSkillName(final int id)
	{
		return addSkillName(id, 1);
	}
	
	public final T addSkillName(final int id, final int lvl)
	{
		append(new SMParam(TYPE_SKILL_NAME, new int[]
		{
			id,
			lvl
		}));
		return (T) this;
	}
	
	public final T addElemental(final int type)
	{
		append(new SMParam(TYPE_ELEMENT_NAME, type));
		return (T) this;
	}
	
	public final T addSystemString(final int type)
	{
		append(new SMParam(TYPE_SYSTEM_STRING, type));
		return (T) this;
	}
	
	public final T addInstanceName(final int type)
	{
		append(new SMParam(TYPE_INSTANCE_NAME, type));
		return (T) this;
	}
	
	protected final void writeInfo()
	{
		writeD(getId());
		writeD(_params.length);
		SMParam param;
		for (int i = 0; i < _paramIndex; i++)
		{
			param = _params[i];
			
			writeD(param.getType());
			switch (param.getType())
			{
				case TYPE_TEXT:
				case TYPE_PLAYER_NAME:
				{
					writeS(param.getStringValue());
					break;
				}
				
				case TYPE_LONG_NUMBER:
				{
					writeQ(param.getLongValue());
					break;
				}
				
				case TYPE_ITEM_NAME:
				case TYPE_CASTLE_NAME:
				case TYPE_INT_NUMBER:
				case TYPE_NPC_NAME:
				case TYPE_ELEMENT_NAME:
				case TYPE_SYSTEM_STRING:
				case TYPE_INSTANCE_NAME:
				case TYPE_DOOR_NAME:
				{
					writeD(param.getIntValue());
					break;
				}
				
				case TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					writeD(array[0]);
					writeD(array[1]);
					break;
				}
				
				case TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					writeD(array[0]);
					writeD(array[1]);
					writeD(array[2]);
					break;
				}
			}
		}
	}
	
	public final void printMe(PrintStream out)
	{
		out.println(0x62);
		
		out.println(getId());
		out.println(_params.length);
		
		for (final SMParam param : _params)
		{
			switch (param.getType())
			{
				case TYPE_TEXT:
				case TYPE_PLAYER_NAME:
				{
					out.println(param.getStringValue());
					break;
				}
				
				case TYPE_LONG_NUMBER:
				{
					out.println(param.getLongValue());
					break;
				}
				
				case TYPE_ITEM_NAME:
				case TYPE_CASTLE_NAME:
				case TYPE_INT_NUMBER:
				case TYPE_NPC_NAME:
				case TYPE_ELEMENT_NAME:
				case TYPE_SYSTEM_STRING:
				case TYPE_INSTANCE_NAME:
				case TYPE_DOOR_NAME:
				{
					out.println(param.getIntValue());
					break;
				}
				
				case TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					out.println(array[0]);
					out.println(array[1]);
					break;
				}
				
				case TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					out.println(array[0]);
					out.println(array[1]);
					out.println(array[2]);
					break;
				}
			}
		}
	}
}