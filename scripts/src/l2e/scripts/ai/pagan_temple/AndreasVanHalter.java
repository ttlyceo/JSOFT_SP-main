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
package l2e.scripts.ai.pagan_temple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.NpcUtils;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.zone.type.BossZone;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.network.serverpackets.SpecialCamera;

/**
 * Created by LordWinter 13.12.2020
 */
public class AndreasVanHalter extends Fighter
{
	private boolean _firstTimeMove = false;
	private ScheduledFuture<?> _checkTask = null;
	private ScheduledFuture<?> _movieTask = null;
	private final BossZone _zone;
	private final List<Npc> _guard = new ArrayList<>();
	private final List<Npc> _altarGuards = new ArrayList<>();
	private Npc _ritualOffering;
	
	public AndreasVanHalter(Attackable actor)
	{
		super(actor);
		_zone = (BossZone) ZoneManager.getInstance().getZoneById(12014);
	}

	@Override
	protected void onEvtSpawn()
	{
		_guard.clear();
		_altarGuards.clear();
		firstSpawn();
		_zone.setCanTeleport(false);
		super.onEvtSpawn();
	}

	@Override
	protected boolean thinkActive()
	{
		final var actor = getActiveChar();
		if (actor.isDead())
		{
			return true;
		}
		
		if (!checkAliveGuards() && _firstTimeMove)
		{
			_firstTimeMove = false;
			_altarGuards.clear();
			
			final var door1 = DoorParser.getInstance().getDoor(19160016);
			if (door1 != null)
			{
				door1.openMe();
			}
			
			final var door2 = DoorParser.getInstance().getDoor(19160017);
			if (door2 != null)
			{
				door2.openMe();
			}
			
			final var announce = GameObjectsStorage.getByNpcId(32051);
			if (announce != null)
			{
				announce.broadcastPacketToOthers(10000, new NpcSay(announce.getObjectId(), Say2.NPC_SHOUT, announce.getId(), NpcStringId.THE_DOOR_TO_THE_3RD_FLOOR_OF_THE_ALTAR_IS_NOW_OPEN));
			}
			_movieTask = ThreadPoolManager.getInstance().schedule(new Movie(1), 3000);

			actor.broadcastPacketToOthers(new PlaySound("BS04_A"));
			if (_checkTask != null)
			{
				_checkTask.cancel(false);
				_checkTask = null;
			}
			_checkTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new CheckAttack(), 3600000, 3600000);
		}
		return true;
	}

	private class CheckAttack implements Runnable
	{
		@Override
		public void run()
		{
			final var actor = getActiveChar();
			if (actor != null && !actor.isAttackingNow() && !actor.isInCombat())
			{
				cleanUp();
				ThreadPoolManager.getInstance().schedule(new NewSpawn(), 10000);
			}
		}
	}

	private class NewSpawn implements Runnable
	{
		@Override
		public void run()
		{
			firstSpawn();
		}
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		final var actor = getActiveChar();
		if (_checkTask != null)
		{
			_checkTask.cancel(false);
			_checkTask = null;
		}
		_zone.broadcastPacket(new PlaySound(1, "BS01_D", 1, actor.getObjectId(), actor.getX(), actor.getY(), actor.getZ()));
		_zone.setCanTeleport(true);
		cleanUp();
		super.onEvtDead(killer);
	}

	private void firstSpawn()
	{
		_guard.add(NpcUtils.spawnSingle(32058, new Location(-20117, -52683, -10974)));
		_guard.add(NpcUtils.spawnSingle(32059, new Location(-20137, -54371, -11170)));
		_guard.add(NpcUtils.spawnSingle(32060, new Location(-12710, -52677, -10974)));
		_guard.add(NpcUtils.spawnSingle(32061, new Location(-12660, -54379, -11170)));
		_guard.add(NpcUtils.spawnSingle(32062, new Location(-17826, -53426, -11624)));
		_guard.add(NpcUtils.spawnSingle(32063, new Location(-17068, -53440, -11624)));
		_guard.add(NpcUtils.spawnSingle(32064, new Location(-16353, -53549, -11624)));
		_guard.add(NpcUtils.spawnSingle(32065, new Location(-15655, -53869, -11624)));
		_guard.add(NpcUtils.spawnSingle(32066, new Location(-15005, -53132, -11624)));
		_guard.add(NpcUtils.spawnSingle(32067, new Location(-16316, -56842, -10900)));
		_guard.add(NpcUtils.spawnSingle(32068, new Location(-16395, -54055, -10439, 15992)));
		_ritualOffering = NpcUtils.spawnSingle(32038, new Location(-16386, -53368, -10448, 15992));
		if (_ritualOffering != null)
		{
			_ritualOffering.setIsImmobilized(true);
		}
		_guard.add(NpcUtils.spawnSingle(32051, new Location(-17248, -54832, -10424, 16384)));
		_guard.add(NpcUtils.spawnSingle(32051, new Location(-15547, -54835, -10424, 16384)));
		_guard.add(NpcUtils.spawnSingle(32051, new Location(-18116, -54831, -10579, 16384)));
		_guard.add(NpcUtils.spawnSingle(32051, new Location(-14645, -54836, -10577, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-18008, -53394, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17653, -53399, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17827, -53575, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-18008, -53749, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17653, -53754, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17827, -53930, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-18008, -54100, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17653, -54105, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17275, -52577, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-16917, -52577, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-16738, -52577, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17003, -52404, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17353, -52404, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17362, -52752, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17006, -52752, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17721, -52752, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17648, -52968, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-17292, -52968, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-16374, -52577, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-16648, -52404, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-16284, -52404, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-16013, -52577, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15658, -52577, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15306, -52577, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15923, -52404, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15568, -52404, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15216, -52404, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15745, -52752, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15394, -52752, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15475, -52969, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15119, -52969, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15149, -53411, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-14794, -53416, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-14968, -53592, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15149, -53766, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-14794, -53771, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-14968, -53947, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-15149, -54117, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22175, new Location(-14794, -54122, -10594, 16384)));
		_altarGuards.add(NpcUtils.spawnSingle(22188, new Location(-16392, -52124, -10592)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16380, -45796, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16290, -45796, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16471, -45796, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16380, -45514, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16290, -45514, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16471, -45514, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16380, -45243, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16290, -45243, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16471, -45243, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16380, -44973, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16290, -44973, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16471, -44973, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16380, -44703, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16290, -44703, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16471, -44703, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16471, -44443, -10726, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16382, -47685, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16292, -47685, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16474, -47685, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16382, -47404, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16292, -47404, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16474, -47404, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16382, -47133, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16292, -47133, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16474, -47133, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16382, -46862, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16292, -46862, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16474, -46862, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16382, -46593, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16292, -46593, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16474, -46593, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16382, -46333, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16292, -46333, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16474, -46333, -10822, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16381, -49743, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16291, -49743, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16473, -49743, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16381, -49461, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16291, -49461, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16473, -49461, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16381, -49191, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16291, -49191, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16473, -49191, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16381, -48920, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16291, -48920, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16473, -48920, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16381, -48650, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16291, -48650, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16473, -48650, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16381, -48391, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16291, -48391, -10918, 16384)));
		_guard.add(NpcUtils.spawnSingle(22176, new Location(-16473, -48391, -10918, 16384)));
		_firstTimeMove = true;
	}

	private void secondSpawn()
	{
		_guard.add(NpcUtils.spawnSingle(22189, new Location(-16199, -53591, -10449, 14881)));
		_guard.add(NpcUtils.spawnSingle(22189, new Location(-16331, -53260, -10449, 6134)));
		_guard.add(NpcUtils.spawnSingle(22189, new Location(-16479, -53528, -10449, 11646)));
		_guard.add(NpcUtils.spawnSingle(22190, new Location(-15847, -53336, -10449, 31957)));
		_guard.add(NpcUtils.spawnSingle(22190, new Location(-16297, -53086, -10449, 7806)));
		_guard.add(NpcUtils.spawnSingle(22190, new Location(-16712, -53438, -10449, 4083)));
		_guard.add(NpcUtils.spawnSingle(22191, new Location(-15690, -54030, -10439, 15992)));
		_guard.add(NpcUtils.spawnSingle(22191, new Location(-16385, -53268, -10439, 15992)));
		_guard.add(NpcUtils.spawnSingle(22191, new Location(-17150, -54046, -10439, 15992)));
		_guard.add(NpcUtils.spawnSingle(22192, new Location(-16385, -53268, -10439, 15992)));
		_guard.add(NpcUtils.spawnSingle(22192, new Location(-17150, -54046, -10439, 15992)));
		_guard.add(NpcUtils.spawnSingle(22192, new Location(-15690, -54030, -10439, 15992)));
		_guard.add(NpcUtils.spawnSingle(22193, new Location(-16385, -53268, -10439, 15992)));
		_guard.add(NpcUtils.spawnSingle(22193, new Location(-17150, -54046, -10439, 15992)));
		_guard.add(NpcUtils.spawnSingle(22193, new Location(-15690, -54030, -10439, 15992)));

	}

	private void cleanUp()
	{
		if (_movieTask != null)
		{
			_movieTask.cancel(false);
			_movieTask = null;
		}
		
		if (_checkTask != null)
		{
			_checkTask.cancel(false);
			_checkTask = null;
		}
		
		for (final Npc npc : _guard)
		{
			if(npc != null)
			{
				npc.deleteMe();
			}
		}
		_guard.clear();
		
		for (final var npc : _altarGuards)
		{
			if (npc != null)
			{
				npc.deleteMe();
			}
		}
		_altarGuards.clear();
		
		if (_ritualOffering != null)
		{
			_ritualOffering.deleteMe();
			_ritualOffering = null;
		}
		
		final var door1 = DoorParser.getInstance().getDoor(19160016);
		if (door1 != null && door1.isOpen())
		{
			door1.closeMe();
		}
		
		final var door2 = DoorParser.getInstance().getDoor(19160017);
		if (door2 != null && door2.isOpen())
		{
			door2.closeMe();
		}
	}
	
	private boolean checkAliveGuards()
	{
		for (final var guard : _altarGuards)
		{
			if (guard != null && !guard.isDead())
			{
				return true;
			}
		}
		return false;
	}
	
	private class Movie implements Runnable
	{
		private int _taskId;
		private List<Player> _players;
		
		public Movie(int taskId)
		{
			_taskId = taskId;
		}
		
		private void nextMovie(int taskId, long delay)
		{
			_taskId = taskId;
			_movieTask = ThreadPoolManager.getInstance().schedule(this, delay);
		}
		
		private void specialCamera(GameObject npc, int dist, int yaw, int pitch, int time, int duration, int turn, int rise, int widescreen, int unk)
		{
			final SpecialCamera packet = new SpecialCamera(npc.getObjectId(), dist, yaw, pitch, time, duration, turn, rise, widescreen, unk);
			for (final var pc : _players)
			{
				pc.sendPacket(packet);
			}
		}
		
		private void enterMovieMode(Player pc)
		{
			pc.setTarget(null);
			pc.stopMove(null);
			pc.setIsParalyzed(true);
			pc.setIsInvul(true);
			pc.setIsImmobilized(true);
		}
		
		public void leaveMovieMode(Player pc)
		{
			pc.setTarget(null);
			pc.stopMove(null);
			pc.setIsParalyzed(false);
			pc.setIsInvul(false);
			pc.setIsImmobilized(false);
		}
		
		@Override
		public void run()
		{
			_movieTask = null;
			final var actor = getActiveChar();
			
			switch (_taskId)
			{
				case 1 :
					_players = getPlayersInside();
					for (final var pc : _players)
					{
						enterMovieMode(pc);
					}
					actor.abortAttack();
					actor.abortCast();
					actor.setHeading(16384);
					actor.setTarget(_ritualOffering);
					specialCamera(actor, 1650, 90, 89, 0, 15000, 0, 89, 0, 0);
					nextMovie(2, 50);
					break;
				case 2 :
					specialCamera(actor, 1650, 90, 89, 0, 15000, 0, 89, 0, 0);
					nextMovie(3, 4000);
					break;
				case 3 :
					specialCamera(actor, 1450, 90, 80, 4000, 15000, 0, 80, 0, 0);
					nextMovie(4, 2000);
					break;
				case 4 :
					specialCamera(actor, 1250, 90, 70, 4000, 15000, 0, 70, 0, 0);
					nextMovie(5, 2000);
					break;
				case 5 :
					specialCamera(actor, 1050, 90, 60, 4000, 15000, 0, 60, 0, 0);
					nextMovie(6, 2000);
					break;
				case 6 :
					specialCamera(actor, 850, 90, 50, 4000, 15000, 0, 45, 0, 0);
					nextMovie(7, 2000);
					break;
				case 7 :
					specialCamera(actor, 650, 90, 40, 4000, 15000, 0, 30, 0, 0);
					nextMovie(8, 2000);
					break;
				case 8 :
					specialCamera(actor, 450, 90, 30, 4000, 15000, 0, 15, 0, 0);
					nextMovie(9, 2000);
					break;
				case 9 :
					specialCamera(actor, 250, 90, 20, 4000, 15000, 0, 1, 0, 0);
					nextMovie(12, 2000);
					break;
				case 12 :
					specialCamera(actor, 50, 90, 10, 4000, 15000, 0, 0, 0, 0);
					nextMovie(13, 3000);
					break;
				case 13 :
					final var skill = SkillsParser.getInstance().getInfo(1168, 7);
					if (skill != null)
					{
						actor.setTarget(_ritualOffering);
						actor.setIsImmobilized(false);
						actor.doCast(skill);
						actor.setIsImmobilized(true);
					}
					nextMovie(14, 4700);
					break;
				case 14 :
					_ritualOffering.broadcastPacketToOthers(new SocialAction(_ritualOffering, 1));
					nextMovie(15, 2500);
					break;
				case 15 :
					secondSpawn();
					if (_ritualOffering != null)
					{
						_ritualOffering.deleteMe();
						_ritualOffering = null;
					}
					specialCamera(actor, 100, 90, 15, 1500, 15000, 0, 0, 0, 0);
					nextMovie(16, 3000);
					break;
				case 16 :
					specialCamera(actor, 5200, 90, 10, 9500, 6000, 0, 20, 0, 0);
					nextMovie(17, 7000);
					break;
				case 17 :
					for (final var pc : _players)
					{
						leaveMovieMode(pc);
					}
					_players = null;
					actor.setIsImmobilized(false);
					actor.setIsInvul(false);
					break;
			}
		}
	}
	
	protected List<Player> getPlayersInside()
	{
		final List<Player> list = new ArrayList<>();
		for (final var pl : _zone.getPlayersInside())
		{
			list.add(pl);
		}
		return list;
	}
}