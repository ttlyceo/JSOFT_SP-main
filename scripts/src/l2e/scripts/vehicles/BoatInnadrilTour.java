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

public class BoatInnadrilTour extends Quest implements Runnable
{
	private static final VehicleTemplate[] TOUR =
	{
	                new VehicleTemplate(105129, 226240, -3610, 150, 800),
	                new VehicleTemplate(90604, 238797, -3610, 150, 800),
	                new VehicleTemplate(74853, 237943, -3610, 150, 800),
	                new VehicleTemplate(68207, 235399, -3610, 150, 800),
	                new VehicleTemplate(63226, 230487, -3610, 150, 800),
	                new VehicleTemplate(61843, 224797, -3610, 150, 800),
	                new VehicleTemplate(61822, 203066, -3610, 150, 800),
	                new VehicleTemplate(59051, 197685, -3610, 150, 800),
	                new VehicleTemplate(54048, 195298, -3610, 150, 800),
	                new VehicleTemplate(41609, 195687, -3610, 150, 800),
	                new VehicleTemplate(35821, 200284, -3610, 150, 800),
	                new VehicleTemplate(35567, 205265, -3610, 150, 800),
	                new VehicleTemplate(35617, 222471, -3610, 150, 800),
	                new VehicleTemplate(37932, 226588, -3610, 150, 800),
	                new VehicleTemplate(42932, 229394, -3610, 150, 800),
	                new VehicleTemplate(74324, 245231, -3610, 150, 800),
	                new VehicleTemplate(81872, 250314, -3610, 150, 800),
	                new VehicleTemplate(101692, 249882, -3610, 150, 800),
	                new VehicleTemplate(107907, 256073, -3610, 150, 800),
	                new VehicleTemplate(112317, 257133, -3610, 150, 800),
	                new VehicleTemplate(126273, 255313, -3610, 150, 800),
	                new VehicleTemplate(128067, 250961, -3610, 150, 800),
	                new VehicleTemplate(128520, 238249, -3610, 150, 800),
	                new VehicleTemplate(126428, 235072, -3610, 150, 800),
	                new VehicleTemplate(121843, 234656, -3610, 150, 800),
	                new VehicleTemplate(120096, 234268, -3610, 150, 800),
	                new VehicleTemplate(118572, 233046, -3610, 150, 800),
	                new VehicleTemplate(117671, 228951, -3610, 150, 800),
	                new VehicleTemplate(115936, 226540, -3610, 150, 800),
	                new VehicleTemplate(113628, 226240, -3610, 150, 800),
	                new VehicleTemplate(111300, 226240, -3610, 150, 800),
	                new VehicleTemplate(111264, 226240, -3610, 150, 800)
	};

	private static final VehicleTemplate DOCK = TOUR[TOUR.length - 1];

	private final BoatInstance _boat;
	private int _cycle = 0;

	private final CreatureSay ARRIVED_AT_INNADRIL;
	private final CreatureSay LEAVE_INNADRIL5;
	private final CreatureSay LEAVE_INNADRIL1;
	private final CreatureSay LEAVE_INNADRIL0;
	private final CreatureSay LEAVING_INNADRIL;

	private final CreatureSay ARRIVAL20;
	private final CreatureSay ARRIVAL15;
	private final CreatureSay ARRIVAL10;
	private final CreatureSay ARRIVAL5;
	private final CreatureSay ARRIVAL1;

	private final PlaySound INNADRIL_SOUND;

	public BoatInnadrilTour(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		_boat = BoatManager.getInstance().getNewBoat(4, 111264, 226240, -3610, 32768);
		_boat.registerEngine(this);
		_boat.runEngine(180000);
		
		ARRIVED_AT_INNADRIL = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.INNADRIL_BOAT_ANCHOR_10_MINUTES);
		LEAVE_INNADRIL5 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.INNADRIL_BOAT_LEAVE_IN_5_MINUTES);
		LEAVE_INNADRIL1 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.INNADRIL_BOAT_LEAVE_IN_1_MINUTE);
		LEAVE_INNADRIL0 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.INNADRIL_BOAT_LEAVE_SOON);
		LEAVING_INNADRIL = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.INNADRIL_BOAT_LEAVING);

		ARRIVAL20 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.INNADRIL_BOAT_ARRIVE_20_MINUTES);
		ARRIVAL15 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.INNADRIL_BOAT_ARRIVE_15_MINUTES);
		ARRIVAL10 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.INNADRIL_BOAT_ARRIVE_10_MINUTES);
		ARRIVAL5 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.INNADRIL_BOAT_ARRIVE_5_MINUTES);
		ARRIVAL1 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.INNADRIL_BOAT_ARRIVE_1_MINUTE);

		INNADRIL_SOUND = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), DOCK.getX(), DOCK.getY(), DOCK.getZ());
	}

	@Override
	public void run()
	{
		try
		{
			switch (_cycle)
			{
				case 0:
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, LEAVE_INNADRIL5);
					ThreadPoolManager.getInstance().schedule(this, 240000);
					break;
				case 1:
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, LEAVE_INNADRIL1);
					ThreadPoolManager.getInstance().schedule(this, 40000);
					break;
				case 2:
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, LEAVE_INNADRIL0);
					ThreadPoolManager.getInstance().schedule(this, 20000);
					break;
				case 3:
					BoatManager.getInstance().broadcastPackets(DOCK, DOCK, LEAVING_INNADRIL, INNADRIL_SOUND);
					_boat.payForRide(0, 1, 107092, 219098, -3952);
					_boat.executePath(TOUR);
					ThreadPoolManager.getInstance().schedule(this, 650000);
					break;
				case 4:
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, ARRIVAL20);
					ThreadPoolManager.getInstance().schedule(this, 300000);
					break;
				case 5:
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, ARRIVAL15);
					ThreadPoolManager.getInstance().schedule(this, 300000);
					break;
				case 6:
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, ARRIVAL10);
					ThreadPoolManager.getInstance().schedule(this, 300000);
					break;
				case 7:
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, ARRIVAL5);
					ThreadPoolManager.getInstance().schedule(this, 240000);
					break;
				case 8:
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, ARRIVAL1);
					break;
				case 9:
					BoatManager.getInstance().broadcastPackets(DOCK, DOCK, ARRIVED_AT_INNADRIL, INNADRIL_SOUND);
					ThreadPoolManager.getInstance().schedule(this, 300000);
					break;
			}
			_cycle++;
			if (_cycle > 9)
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
			new BoatInnadrilTour(-1, BoatInnadrilTour.class.getSimpleName(), "vehicles");
	}
}
