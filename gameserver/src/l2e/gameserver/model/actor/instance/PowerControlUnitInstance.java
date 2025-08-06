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
package l2e.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Created by LordWinter
 */
public class PowerControlUnitInstance extends NpcInstance
{
	public static final int LIMIT = 3;

	public static final int COND_NO_ENTERED = 0;
	public static final int COND_ENTERED = 1;
	public static final int COND_ALL_OK = 2;
	public static final int COND_FAIL = 3;
	public static final int COND_TIMEOUT = 4;

	private final int[] _generated = new int[LIMIT];
	private int _index;
	private int _tryCount;
	private long _invalidatePeriod;

	public PowerControlUnitInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final StringTokenizer token = new StringTokenizer(command);
		token.nextToken();
		final int val = Integer.parseInt(token.nextToken());

		if (player.getClassId() == ClassId.warsmith || player.getClassId() == ClassId.maestro)
		{
			if(_tryCount == 0)
			{
				_tryCount++;
			}
			else
			{
				_index++;
			}
		}
		else
		{
			if(_generated[_index] == val)
			{
				_index++;
			}
			else
			{
				_tryCount++;
			}
		}
		showChatWindow(player, 0);
	}


	@Override
	public void onSpawn()
	{
		super.onSpawn();
		generate();
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		final NpcHtmlMessage message = new NpcHtmlMessage(getObjectId());

		if(_invalidatePeriod > 0 && _invalidatePeriod < System.currentTimeMillis())
		{
			generate();
		}

		final int cond = getCond();
		switch(cond)
		{
			case COND_ALL_OK:
				message.setFile(player, player.getLang(), "data/html/fortress/fortress_inner_controller002.htm");
				if (getFort().getSiege().getIsInProgress())
				{
					getFort().getSiege().killedPowerUnit(this);
					final Spawner spawn = getFort().getSiege().getMainMachine().get(0);
					final MainMachineInstance machineInstance = (MainMachineInstance) spawn.getLastSpawn();
					if(machineInstance != null)
					{
						machineInstance.powerOff(this);
					}
					onDecay();
				}
				break;
			case COND_TIMEOUT:
				message.setFile(player, player.getLang(), "data/html/fortress/fortress_inner_controller003.htm");
				break;
			case COND_FAIL:
				message.setFile(player, player.getLang(), "data/html/fortress/fortress_inner_controller003.htm");
				_invalidatePeriod = System.currentTimeMillis() + 30000L;
				break;
			case COND_ENTERED:
				message.setFile(player, player.getLang(), "data/html/fortress/fortress_inner_controller004.htm");
				message.replaceNpcString("%password%", _index == 0 ? NpcStringId.PASSWORD_HAS_NOT_BEEN_ENTERED : _index == 1 ? NpcStringId.FIRST_PASSWORD_HAS_BEEN_ENTERED : NpcStringId.SECOND_PASSWORD_HAS_BEEN_ENTERED);
				message.replaceNpcString("%try_count%", NpcStringId.ATTEMPT_S1_3_IS_IN_PROGRESS_THIS_IS_THE_THIRD_ATTEMPT_ON_S1, _tryCount);
				break;
			case COND_NO_ENTERED:
				message.setFile(player, player.getLang(), "data/html/fortress/fortress_inner_controller001.htm");
				break;
		}
		message.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(message);
	}

	private void generate()
	{
		_invalidatePeriod = 0;
		_tryCount = 0;
		_index = 0;

		for(int i = 0; i < _generated.length; i++)
		{
			_generated[i] = -1;
		}

		int j = 0;
		while(j != LIMIT)
		{
			final int val = Rnd.get(0, 9);
			if(ArrayUtils.contains(_generated, val))
			{
				continue;
			}
			_generated[j ++] = val;
		}
	}

	private int getCond()
	{
		if(_invalidatePeriod > System.currentTimeMillis())
		{
			return COND_TIMEOUT;
		}
		else if (_tryCount >= LIMIT)
		{
			return COND_FAIL;
		}
		else if (_index == 0 && _tryCount == 0)
		{
			return COND_NO_ENTERED;
		}
		else if (_index == LIMIT)
		{
			return COND_ALL_OK;
		}
		return COND_ENTERED;
	}

	public int[] getGenerated()
	{
		return _generated;
	}
}