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

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.instancemanager.BoatManager;
import l2e.gameserver.model.actor.instance.BoatInstance;
import l2e.gameserver.model.actor.templates.VehicleTemplate;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.PlaySound;

public class BoatRunePrimeval extends Quest implements Runnable
{
	// Time: 239s
	private static final VehicleTemplate[] RUNE_TO_PRIMEVAL =
	{
	                new VehicleTemplate(32750, -39300, -3610, 180, 800),
	                new VehicleTemplate(27440, -39328, -3610, 250, 1000),
	                new VehicleTemplate(19616, -39360, -3610, 270, 1000),
	                new VehicleTemplate(3840, -38528, -3610, 270, 1000),
	                new VehicleTemplate(1664, -37120, -3610, 270, 1000),
	                new VehicleTemplate(896, -34560, -3610, 180, 1800),
	                new VehicleTemplate(832, -31104, -3610, 180, 180),
	                new VehicleTemplate(2240, -29132, -3610, 150, 1800),
	                new VehicleTemplate(4160, -27828, -3610, 150, 1800),
	                new VehicleTemplate(5888, -27279, -3610, 150, 1800),
	                new VehicleTemplate(7000, -27279, -3610, 150, 1800),
	                new VehicleTemplate(10342, -27279, -3610, 150, 1800)
	};

	// Time: 221s
	private static final VehicleTemplate[] PRIMEVAL_TO_RUNE =
	{
	                new VehicleTemplate(15528, -27279, -3610, 180, 800),
	                new VehicleTemplate(22304, -29664, -3610, 290, 800),
	                new VehicleTemplate(33824, -26880, -3610, 290, 800),
	                new VehicleTemplate(38848, -21792, -3610, 240, 1200),
	                new VehicleTemplate(43424, -22080, -3610, 180, 1800),
	                new VehicleTemplate(44320, -25152, -3610, 180, 1800),
	                new VehicleTemplate(40576, -31616, -3610, 250, 800),
	                new VehicleTemplate(36819, -35315, -3610, 220, 800)
	};

	private static final VehicleTemplate[] RUNE_DOCK =
	{
	                new VehicleTemplate(34381, -37680, -3610, 220, 800)
	};

	private static final VehicleTemplate PRIMEVAL_DOCK = RUNE_TO_PRIMEVAL[RUNE_TO_PRIMEVAL.length - 1];

	private final BoatInstance _boat;
	private int _cycle = 0;
	private int _shoutCount = 0;

	private final CreatureSay ARRIVED_AT_RUNE;
	private final CreatureSay ARRIVED_AT_RUNE_2;
	private final CreatureSay LEAVING_RUNE;
	private final CreatureSay ARRIVED_AT_PRIMEVAL;
	private final CreatureSay ARRIVED_AT_PRIMEVAL_2;
	private final CreatureSay LEAVING_PRIMEVAL;
	private final CreatureSay BUSY_RUNE;

	private final PlaySound RUNE_SOUND;
	private final PlaySound PRIMEVAL_SOUND;

	public BoatRunePrimeval(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		_boat = BoatManager.getInstance().getNewBoat(5, 34381, -37680, -3610, 40785);
		_boat.registerEngine(this);
		_boat.runEngine(180000);
		BoatManager.getInstance().dockShip(BoatManager.RUNE_HARBOR, true);

		ARRIVED_AT_RUNE = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.ARRIVED_AT_RUNE);
		ARRIVED_AT_RUNE_2 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_LEAVING_FOR_PRIMEVAL_3_MINUTES);
		LEAVING_RUNE = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_LEAVING_RUNE_FOR_PRIMEVAL_NOW);
		ARRIVED_AT_PRIMEVAL = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_ARRIVED_AT_PRIMEVAL);
		ARRIVED_AT_PRIMEVAL_2 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_LEAVING_FOR_RUNE_3_MINUTES);
		LEAVING_PRIMEVAL = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_LEAVING_PRIMEVAL_FOR_RUNE_NOW);
		BUSY_RUNE = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_FROM_PRIMEVAL_TO_RUNE_DELAYED);

		RUNE_SOUND = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), RUNE_DOCK[0].getX(), RUNE_DOCK[0].getY(), RUNE_DOCK[0].getZ());
		PRIMEVAL_SOUND = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), PRIMEVAL_DOCK.getX(), PRIMEVAL_DOCK.getY(), PRIMEVAL_DOCK.getZ());
	}

	@Override
	public void run()
	{
		try
		{
			switch (_cycle)
			{
				case 0:
					BoatManager.getInstance().dockShip(BoatManager.RUNE_HARBOR, false);
					BoatManager.getInstance().broadcastPackets(RUNE_DOCK[0], PRIMEVAL_DOCK, LEAVING_RUNE, RUNE_SOUND);
					_boat.payForRide(8925, 1, 34513, -38009, -3640);
					_boat.executePath(RUNE_TO_PRIMEVAL);
					break;
				case 1:
					BoatManager.getInstance().broadcastPackets(PRIMEVAL_DOCK, RUNE_DOCK[0], ARRIVED_AT_PRIMEVAL, ARRIVED_AT_PRIMEVAL_2, PRIMEVAL_SOUND);
					ThreadPoolManager.getInstance().schedule(this, 180000);
					break;
				case 2:
					BoatManager.getInstance().broadcastPackets(PRIMEVAL_DOCK, RUNE_DOCK[0], LEAVING_PRIMEVAL, PRIMEVAL_SOUND);
					_boat.payForRide(8924, 1, 10447, -24982, -3664);
					_boat.executePath(PRIMEVAL_TO_RUNE);
					break;
				case 3:
					if (BoatManager.getInstance().dockBusy(BoatManager.RUNE_HARBOR))
					{
						if (_shoutCount == 0)
						{
							BoatManager.getInstance().broadcastPacket(RUNE_DOCK[0], PRIMEVAL_DOCK, BUSY_RUNE);
						}

						_shoutCount++;
						if (_shoutCount > 35)
						{
							_shoutCount = 0;
						}

						ThreadPoolManager.getInstance().schedule(this, 5000);
						return;
					}
					_boat.executePath(RUNE_DOCK);
					break;
				case 4:
					BoatManager.getInstance().dockShip(BoatManager.RUNE_HARBOR, true);
					BoatManager.getInstance().broadcastPackets(RUNE_DOCK[0], PRIMEVAL_DOCK, ARRIVED_AT_RUNE, ARRIVED_AT_RUNE_2, RUNE_SOUND);
					ThreadPoolManager.getInstance().schedule(this, 180000);
					break;
			}
			_shoutCount = 0;
			_cycle++;
			if (_cycle > 4)
			{
				_cycle = 0;
			}
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage());
		}
	}

	public static void main(String[] args)
	{
		if(Config.ALLOW_BOAT)
			new BoatRunePrimeval(-1, BoatRunePrimeval.class.getSimpleName(), "vehicles");
	}
}
