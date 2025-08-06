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
package l2e.scripts.vehicles;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.instancemanager.AirShipManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.AirShipInstance;
import l2e.gameserver.model.actor.templates.VehicleTemplate;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

public class AirShipGludioGracia extends Quest implements Runnable
{
	private static final int[] CONTROLLERS =
	{
	                32607,
	                32609
	};

	private static final int GLUDIO_DOCK_ID = 10;
	private static final int GRACIA_DOCK_ID = 11;

	private static final Location OUST_GLUDIO = new Location(-149379, 255246, -80);
	private static final Location OUST_GRACIA = new Location(-186563, 243590, 2608);

	private static final VehicleTemplate[] GLUDIO_TO_WARPGATE =
	{
	        new VehicleTemplate(-151216, 252544, 231), new VehicleTemplate(-160416, 256144, 222), new VehicleTemplate(-167888, 256720, -509, 0, 41035)
	};

	private static final VehicleTemplate[] WARPGATE_TO_GRACIA =
	{
	        new VehicleTemplate(-169776, 254800, 282), new VehicleTemplate(-171824, 250048, 425), new VehicleTemplate(-172608, 247728, 398), new VehicleTemplate(-174544, 246176, 39), new VehicleTemplate(-179440, 243648, 1337), new VehicleTemplate(-182608, 243952, 2739), new VehicleTemplate(-184960, 245120, 2694), new VehicleTemplate(-186944, 244560, 2617)
	};

	private static final VehicleTemplate[] GRACIA_TO_WARPGATE =
	{
	        new VehicleTemplate(-187808, 244992, 2672), new VehicleTemplate(-188528, 245920, 2465), new VehicleTemplate(-189936, 245232, 1682), new VehicleTemplate(-191200, 242960, 1523), new VehicleTemplate(-190416, 239088, 1706), new VehicleTemplate(-187488, 237104, 2768), new VehicleTemplate(-184688, 238432, 2802), new VehicleTemplate(-184524, 241119, 2816), new VehicleTemplate(-182129, 243385, 2733), new VehicleTemplate(-179440, 243648, 1337), new VehicleTemplate(-174544, 246176, 39), new VehicleTemplate(-172608, 247728, 398), new VehicleTemplate(-171824, 250048, 425), new VehicleTemplate(-169776, 254800, 282), new VehicleTemplate(-168080, 256624, 343), new VehicleTemplate(-157264, 255664, 221, 0, 64781)
	};

	private static final VehicleTemplate[] WARPGATE_TO_GLUDIO =
	{
	        new VehicleTemplate(-153424, 255376, 221), new VehicleTemplate(-149552, 258160, 221), new VehicleTemplate(-146896, 257088, 221), new VehicleTemplate(-146672, 254224, 221), new VehicleTemplate(-147856, 252704, 206), new VehicleTemplate(-149392, 252544, 198)
	};

	private final AirShipInstance _ship;
	private int _cycle = 0;

	private boolean _foundAtcGludio = false;
	private Npc _atcGludio = null;
	private boolean _foundAtcGracia = false;
	private Npc _atcGracia = null;

	public AirShipGludioGracia(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(CONTROLLERS);
		addFirstTalkId(CONTROLLERS);
		addTalkId(CONTROLLERS);

		_ship = AirShipManager.getInstance().getNewAirShip(-149378, 252552, 198, 33837);
		_ship.setOustLoc(OUST_GLUDIO);
		_ship.setInDock(GLUDIO_DOCK_ID);
		_ship.registerEngine(this);
		_ship.runEngine(60000);
	}

	private final void broadcastInGludio(NpcStringId npcString)
	{
		if (!_foundAtcGludio)
		{
			_foundAtcGludio = true;
			_atcGludio = findController();
		}
		if (_atcGludio != null)
		{
			_atcGludio.broadcastPacket(new NpcSay(_atcGludio.getObjectId(), Say2.NPC_SHOUT, _atcGludio.getId(), npcString));
		}
	}

	private final void broadcastInGracia(NpcStringId npcStringId)
	{
		if (!_foundAtcGracia)
		{
			_foundAtcGracia = true;
			_atcGracia = findController();
		}
		if (_atcGracia != null)
		{
			_atcGracia.broadcastPacket(new NpcSay(_atcGracia.getObjectId(), Say2.NPC_SHOUT, _atcGracia.getId(), npcStringId));
		}
	}

	private final Npc findController()
	{
		for (final Npc obj : World.getInstance().getAroundNpc(_ship, 600, 400))
		{
			for (final int id : CONTROLLERS)
			{
				if (obj.getId() == id)
				{
					return obj;
				}
			}
		}
		return null;
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		if (player.isTransformed())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_TRANSFORMED);
			return null;
		}
		else if (player.isParalyzed())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_PETRIFIED);
			return null;
		}
		else if (player.isDead() || player.isFakeDeath())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_DEAD);
			return null;
		}
		else if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_FISHING);
			return null;
		}
		else if (player.isInCombat())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_IN_BATTLE);
			return null;
		}
		else if (player.isInDuel())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_IN_A_DUEL);
			return null;
		}
		else if (player.isSitting())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_SITTING);
			return null;
		}
		else if (player.isCastingNow())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_CASTING);
			return null;
		}
		else if (player.isCursedWeaponEquipped())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_A_CURSED_WEAPON_IS_EQUIPPED);
			return null;
		}
		else if (player.isCombatFlagEquipped())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_HOLDING_A_FLAG);
			return null;
		}
		else if (player.hasSummon() || player.isMounted())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_A_PET_OR_A_SERVITOR_IS_SUMMONED);
			return null;
		}
		else if (_ship.isInDock() && _ship.isInsideRadius(player, 600, true, false))
		{
			_ship.addPassenger(player);
		}
		return null;
	}

	@Override
	public final String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}

	@Override
	public void run()
	{
		try
		{
			switch (_cycle)
			{
				case 0:
					broadcastInGludio(NpcStringId.THE_REGULARLY_SCHEDULED_AIRSHIP_THAT_FLIES_TO_THE_GRACIA_CONTINENT_HAS_DEPARTED);
					_ship.setInDock(0);
					_ship.executePath(GLUDIO_TO_WARPGATE);
					break;
				case 1:
					_ship.setOustLoc(OUST_GRACIA);
					ThreadPoolManager.getInstance().schedule(this, 5000);
					break;
				case 2:
					_ship.executePath(WARPGATE_TO_GRACIA);
					break;
				case 3:
					broadcastInGracia(NpcStringId.THE_REGULARLY_SCHEDULED_AIRSHIP_HAS_ARRIVED_IT_WILL_DEPART_FOR_THE_ADEN_CONTINENT_IN_1_MINUTE);
					_ship.setInDock(GRACIA_DOCK_ID);
					_ship.oustPlayers();
					ThreadPoolManager.getInstance().schedule(this, 60000);
					break;
				case 4:
					broadcastInGracia(NpcStringId.THE_REGULARLY_SCHEDULED_AIRSHIP_THAT_FLIES_TO_THE_ADEN_CONTINENT_HAS_DEPARTED);
					_ship.setInDock(0);
					_ship.executePath(GRACIA_TO_WARPGATE);
					break;
				case 5:
					_ship.setOustLoc(OUST_GLUDIO);
					ThreadPoolManager.getInstance().schedule(this, 5000);
					break;
				case 6:
					_ship.executePath(WARPGATE_TO_GLUDIO);
					break;
				case 7:
					broadcastInGludio(NpcStringId.THE_REGULARLY_SCHEDULED_AIRSHIP_HAS_ARRIVED_IT_WILL_DEPART_FOR_THE_GRACIA_CONTINENT_IN_1_MINUTE);
					_ship.setInDock(GLUDIO_DOCK_ID);
					_ship.oustPlayers();
					ThreadPoolManager.getInstance().schedule(this, 60000);
					break;
			}
			_cycle++;
			if (_cycle > 7)
			{
				_cycle = 0;
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean unload(boolean removeFromList)
	{
		if (_ship != null)
		{
			_ship.oustPlayers();
			_ship.deleteMe();
		}
		return super.unload(removeFromList);
	}

	public static void main(String[] args)
	{
		new AirShipGludioGracia(-1, AirShipGludioGracia.class.getSimpleName(), "vehicles");
	}
}
