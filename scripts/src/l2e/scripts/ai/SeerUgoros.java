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
package l2e.scripts.ai;

import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.Util;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.scripts.quests._288_HandleWithCare;
import l2e.scripts.quests._423_TakeYourBestShot;

public class SeerUgoros extends AbstractNpcAI
{
	private ScheduledFuture<?> _thinkTask = null;
	
	private Attackable _ugoros = null;
	private Npc _weed = null;
	private boolean _weed_attack = false;
	
	private boolean _weed_killed_by_player = false;
	private boolean _killed_one_weed = false;
	
	private Player _player = null;
	
	private final byte ALIVE = 0;
	private final byte FIGHTING = 1;
	private final byte DEAD = 2;
	private byte STATE = DEAD;
	
	private final Skill _ugoros_skill = SkillsParser.getInstance().getInfo(6426, 1);
	
	public SeerUgoros(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(32740);
		addTalkId(32740);
		
		addAttackId(18867);
		
		addKillId(18863);
		
		startQuestTimer("ugoros_respawn", 60000, null, null);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("ugoros_respawn") && (_ugoros == null))
		{
			_ugoros = (Attackable) addSpawn(18863, 96804, 85604, -3720, 34360, false, 0);
			broadcastInRegion(_ugoros, NpcStringId.LISTEN_OH_TANTAS_I_HAVE_RETURNED_THE_PROPHET_YUGOROS_OF_THE_BLACK_ABYSS_IS_WITH_ME_SO_DO_NOT_BE_AFRAID);
			STATE = ALIVE;
			startQuestTimer("ugoros_shout", 120000, null, null);
		}
		else if (event.equalsIgnoreCase("ugoros_shout"))
		{
			if (STATE == FIGHTING)
			{
				final var _zone = ZoneManager.getInstance().getZoneById(20706);
				if (_player == null)
				{
					STATE = ALIVE;
				}
				else if (_zone != null && !_zone.isCharacterInZone(_player))
				{
					STATE = ALIVE;
					_player = null;
				}
			}
			else if (STATE == ALIVE)
			{
				broadcastInRegion(_ugoros, NpcStringId.LISTEN_OH_TANTAS_THE_BLACK_ABYSS_IS_FAMISHED_FIND_SOME_FRESH_OFFERINGS);
			}
			startQuestTimer("ugoros_shout", 120000, null, null);
		}
		else if (event.equalsIgnoreCase("ugoros_attack"))
		{
			if (_player != null)
			{
				changeAttackTarget(_player);
				final var packet = new NpcSay(_ugoros.getObjectId(), Say2.NPC_ALL, _ugoros.getId(), NpcStringId.WELCOME_S1_LET_US_SEE_IF_YOU_HAVE_BROUGHT_A_WORTHY_OFFERING_FOR_THE_BLACK_ABYSS);
				packet.addStringParameter(_player.getName(null).toString());
				_ugoros.broadcastPacketToOthers(2000, packet);
				if (_thinkTask != null)
				{
					_thinkTask.cancel(true);
				}
				_thinkTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new ThinkTask(), 1000, 3000);
			}
		}
		else if (event.equalsIgnoreCase("weed_check"))
		{
			if ((_weed_attack == true) && (_ugoros != null) && (_weed != null))
			{
				if (_weed.isDead() && !_weed_killed_by_player)
				{
					_killed_one_weed = true;
					_weed = null;
					_weed_attack = false;
					_ugoros.getStatus().setCurrentHp(_ugoros.getStatus().getCurrentHp() + (_ugoros.getMaxHp() * 0.2));
					_ugoros.broadcastPacketToOthers(2000, new NpcSay(_ugoros.getObjectId(), 0, _ugoros.getId(), NpcStringId.WHAT_A_FORMIDABLE_FOE_BUT_I_HAVE_THE_ABYSS_WEED_GIVEN_TO_ME_BY_THE_BLACK_ABYSS_LET_ME_SEE));
				}
				else
				{
					startQuestTimer("weed_check", 2000, null, null);
				}
			}
			else
			{
				_weed = null;
				_weed_attack = false;
			}
		}
		else if (event.equalsIgnoreCase("ugoros_expel"))
		{
			if (_player != null)
			{
				_player.teleToLocation(94701, 83053, -3580, true, _player.getReflection());
				_player = null;
			}
		}
		else if (event.equalsIgnoreCase("teleportInside"))
		{
			if (STATE == ALIVE)
			{
				if (player.getInventory().getItemByItemId(15496) != null)
				{
					STATE = FIGHTING;
					_player = player;
					_killed_one_weed = false;
					player.teleToLocation(95984, 85692, -3720, true, player.getReflection());
					player.destroyItemByItemId("SeerUgoros", 15496, 1, npc, true);
					startQuestTimer("ugoros_attack", 2000, null, null);
					final var st = player.getQuestState(_288_HandleWithCare.class.getSimpleName());
					if (st != null)
					{
						st.set("drop", "1");
					}
				}
				else
				{
					final var st = player.getQuestState(_423_TakeYourBestShot.class.getSimpleName());
					if (st == null)
					{
						return "<html><body>" + ServerStorage.getInstance().getString(player.getLang(), "288quest.QUEST_NULL") + "</body></html>";
					}
					
					return "<html><body>" + ServerStorage.getInstance().getString(player.getLang(), "288quest.NO_ITEM") + "</body></html>";
				}
			}
			else
			{
				return "<html><body>" + ServerStorage.getInstance().getString(player.getLang(), "288quest.NO_ITEM") + "</body></html>";
			}
		}
		else if (event.equalsIgnoreCase("teleport_back"))
		{
			if (player != null)
			{
				player.teleToLocation(94792, 83542, -3424, true, player.getReflection());
				_player = null;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isDead())
		{
			return null;
		}
		
		if (npc.getId() == 18867)
		{
			if ((_ugoros != null) && (_weed != null) && npc.equals(_weed))
			{
				_weed = null;
				_weed_attack = false;
				_weed_killed_by_player = true;
				_ugoros.broadcastPacketToOthers(2000, new NpcSay(_ugoros.getObjectId(), 0, _ugoros.getId(), NpcStringId.NO_HOW_DARE_YOU_STOP_ME_FROM_USING_THE_ABYSS_WEED_DO_YOU_KNOW_WHAT_YOU_HAVE_DONE));
				if (_thinkTask != null)
				{
					_thinkTask.cancel(true);
				}
				_thinkTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new ThinkTask(), 500, 3000);
			}
			npc.doDie(attacker);
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		if (npc.getId() == 18863)
		{
			if (_thinkTask != null)
			{
				_thinkTask.cancel(true);
				_thinkTask = null;
			}
			STATE = DEAD;
			broadcastInRegion(_ugoros, NpcStringId.AH_HOW_COULD_I_LOSE_OH_BLACK_ABYSS_RECEIVE_ME);
			_ugoros = null;
			addSpawn(32740, 96782, 85918, -3720, 34360, false, 50000);
			startQuestTimer("ugoros_expel", 50000, null, null);
			startQuestTimer("ugoros_respawn", 60000, null, null);
			final var st = player.getQuestState(_288_HandleWithCare.class.getSimpleName());
			if ((st != null) && (st.isCond(1)) && (st.getInt("drop") == 1))
			{
				if (_killed_one_weed)
				{
					player.addItem("SeerUgoros", 15498, 1, npc, true);
					st.setCond(2, true);
				}
				else
				{
					player.addItem("SeerUgoros", 15497, 1, npc, true);
					st.setCond(3, true);
				}
				st.unset("drop");
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	private void broadcastInRegion(Npc npc, NpcStringId npcString)
	{
		if (npc == null)
		{
			return;
		}
		
		final var cs = new NpcSay(npc.getObjectId(), 1, npc.getId(), npcString);
		final var region = MapRegionManager.getInstance().getMapRegionLocId(npc.getX(), npc.getY());
		for (final var player : GameObjectsStorage.getPlayers())
		{
			if (region == MapRegionManager.getInstance().getMapRegionLocId(player.getX(), player.getY()))
			{
				if (Util.checkIfInRange(6000, npc, player, false))
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	
	private class ThinkTask implements Runnable
	{
		@Override
		public void run()
		{
			final var _zone = ZoneManager.getInstance().getZoneById(20706);
			if ((STATE == FIGHTING) && _player != null && _zone != null && _zone.isCharacterInZone(_player) && !_player.isDead())
			{
				if (_weed_attack && (_weed != null))
				{}
				else if (getRandom(10) < 6)
				{
					_weed = null;
					for (final var _char : World.getInstance().getAroundNpc(_ugoros, 2000, 200))
					{
						if (_char != null && _char instanceof Attackable && !_char.isDead() && (_char.getId() == 18867))
						{
							_weed_attack = true;
							_weed = _char;
							changeAttackTarget(_weed);
							startQuestTimer("weed_check", 1000, null, null);
							break;
						}
					}
					if (_weed == null)
					{
						changeAttackTarget(_player);
					}
				}
				else
				{
					changeAttackTarget(_player);
				}
			}
			else
			{
				STATE = ALIVE;
				_player = null;
				if (_thinkTask != null)
				{
					_thinkTask.cancel(true);
					_thinkTask = null;
				}
			}
		}
	}
	
	private void changeAttackTarget(Creature _attack)
	{
		if (_ugoros == null)
		{
			return;
		}
		
		_ugoros.getAI().setIntention(CtrlIntention.IDLE);
		_ugoros.clearAggroList(true);
		_ugoros.setTarget(_attack);
		if (_attack instanceof Attackable)
		{
			_weed_killed_by_player = false;
			_ugoros.disableSkill(_ugoros_skill, 100000);
			_ugoros.setIsRunning(true);
			_ugoros.addDamageHate(_attack, 0, Integer.MAX_VALUE);
		}
		else
		{
			_ugoros.enableSkill(_ugoros_skill);
			_ugoros.addDamageHate(_attack, 0, 99);
			_ugoros.setIsRunning(false);
		}
		_ugoros.getAI().setIntention(CtrlIntention.ATTACK, _attack);
	}
	
	public static void main(String[] args)
	{
		new SeerUgoros(SeerUgoros.class.getSimpleName(), "ai");
	}
}
