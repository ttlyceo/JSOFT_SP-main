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
package l2e.scripts.custom;

import l2e.commons.util.Util;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.listener.actor.OnMagicUseListener;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.scripts.ai.AbstractNpcAI;

public class Sumiel extends AbstractNpcAI implements OnMagicUseListener
{
	private static final int SUMIEL = 32758;
	private static final int BURNER = 18913;
	private static final int TREASURE_BOX = 18911;

	private static final int UNLIT_TORCHLIGHT = 15540;
	private static final int TORCHLIGHT = 15485;

	private static final int SKILL_TORCH_LIGHT = 9059;
	private static final SkillHolder TRIGGER_MIRAGE = new SkillHolder(5144, 1);

	private static final Location TELEPORT1 = new Location(113187, -85388, -3424, 0);
	private static final Location TELEPORT2 = new Location(118833, -80589, -2688, 0);

	private static final int TIMER_INTERVAL = 3;
	private static final int MAX_ATTEMPTS = 3;

	private final MinigameRoom _rooms[] = new MinigameRoom[2];

	public Sumiel(String name, String descr)
	{
		super(name, descr);
		addStartNpc(SUMIEL);
		addFirstTalkId(SUMIEL);
		addTalkId(SUMIEL);
		addSpawnId(TREASURE_BOX);

		int i = 0;
		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn.getId() == SUMIEL)
			{
				_rooms[i++] = initRoom(spawn.getLastSpawn());
			}
		}
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final MinigameRoom room = getRoomByManager(npc);
		switch (event)
		{
			case "restart" :
			{
				final boolean miniGameStarted = room.getStarted();
				if (!miniGameStarted && !hasQuestItems(player, UNLIT_TORCHLIGHT))
				{
					return "32758-05.htm";
				}
				else if ((npc.getTarget() != null) && (npc.getTarget() != player))
				{
					return "32758-04.htm";
				}

				takeItems(player, UNLIT_TORCHLIGHT, 1);
				giveItems(player, TORCHLIGHT, 1);
				broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.THE_FURNACE_WILL_GO_OUT_WATCH_AND_SEE);

				room.getManager().setTarget(player);
				room.setParticipant(player);
				room.setStarted(true);
				for (int i = 0; i < 9; i++)
				{
					room.getOrder()[i] = getRandom(8);
				}
				cancelQuestTimer("hurry_up", npc, null);
				cancelQuestTimer("hurry_up2", npc, null);
				cancelQuestTimer("expire", npc, null);

				startQuestTimer("hurry_up", 120000, npc, null);
				startQuestTimer("expire", 190000, npc, null);
				startQuestTimer("start", 1000, npc, null);
				return null;
			}
			case "off" :
			{
				if (npc.getId() == BURNER)
				{
					npc.setDisplayEffect(2);
					npc.setIsRunning(false);
				}
				else
				{
					for (final Npc burner : room.getBurners())
					{
						burner.setDisplayEffect(2);
						burner.setIsRunning(false);
					}
				}
				break;
			}
			case "teleport1" :
			{
				player.teleToLocation(TELEPORT1, 0, true, player.getReflection());
				break;
			}
			case "teleport2" :
			{
				player.teleToLocation(TELEPORT2, 0, true, player.getReflection());
				break;
			}
			case "start" :
			{
				room.burnThemAll();
				startQuestTimer("off", 2000, npc, null);
				startQuestTimer("timer", 4000, npc, null);
				break;
			}
			case "timer" :
			{
				if (room.getCurrentPot() < 9)
				{
					final Npc b = room.getBurners()[room.getOrder()[room.getCurrentPot()]];
					b.setDisplayEffect(1);
					b.setIsRunning(false);
					startQuestTimer("off", 2000, b, null);
					startQuestTimer("timer", TIMER_INTERVAL * 1000, npc, null);
					room.setCurrentPot(room.getCurrentPot() + 1);
				}
				else
				{
					broadcastNpcSay(room.getManager(), Say2.NPC_ALL, NpcStringId.NOW_LIGHT_THE_FURNACES_FIRE);
					room.burnThemAll();
					startQuestTimer("off", 2000, npc, null);
					room.getParticipant().getListeners().add(this);
					room.setCurrentPot(0);
				}
				break;
			}
			case "hurry_up" :
			{
				broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.THERES_ABOUT_1_MINUTE_LEFT);
				startQuestTimer("hurry_up2", 60000, npc, null);
				break;
			}
			case "hurry_up2" :
			{
				broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.THERES_JUST_10_SECONDS_LEFT);
				startQuestTimer("expire", 10000, npc, null);
				break;
			}
			case "expire" :
			{
				broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.TIME_IS_UP_AND_YOU_HAVE_FAILED_ANY_MORE_WILL_BE_DIFFICULT);
			}
			case "end" :
			{
				cancelQuestTimer("expire", npc, null);
				cancelQuestTimer("hurry_up", npc, null);
				cancelQuestTimer("hurry_up2", npc, null);
				room.getManager().setTarget(null);
				room.setParticipant(null);
				room.setStarted(false);
				room.setAttemptNumber(1);
				room.setCurrentPot(0);
				break;
			}
			case "afterthat" :
			{
				npc.deleteMe();
				break;
			}
		}
		return event;
	}

	@Override
	public String onFirstTalk(Npc npc, Player talker)
	{
		String htmltext = null;
		final MinigameRoom room = getRoomByManager(npc);
		final boolean miniGameStarted = room.getStarted();

		if (npc.getTarget() == null)
		{
			htmltext = (miniGameStarted ? "32758-08.htm" : "32758.htm");
		}
		else if (npc.getTarget() == talker)
		{
			if (miniGameStarted)
			{
				htmltext = "32758-07.htm";
			}
			else
			{
				final int attemptNumber = room.getAttemptNumber();

				if (attemptNumber == 2)
				{
					htmltext = "32758-02.htm";
				}
				else if (attemptNumber == 3)
				{
					htmltext = "32758-03.htm";
				}
			}
		}
		else
		{
			htmltext = "32758-04.htm";
		}

		return htmltext;
	}

	@Override
	public String onSpawn(Npc npc)
	{
		npc.disableCoreAI(true);
		startQuestTimer("afterthat", 180000, npc, null);
		return super.onSpawn(npc);
	}

	@Override
	public void onMagicUse(Creature actor, Skill skill, GameObject[] targets, boolean alt)
	{
		if (!actor.isPlayer())
		{
			return;
		}
		
		final MinigameRoom room = getRoomByParticipant((Player) actor);
		final boolean miniGameStarted = room.getStarted();
		if (miniGameStarted && (skill.getId() == SKILL_TORCH_LIGHT))
		{
			for (final GameObject obj : targets)
			{
				if ((obj != null) && obj.isNpc())
				{
					final Npc npc = (Npc) obj;
					if (npc.getId() == BURNER)
					{
						npc.doCast(TRIGGER_MIRAGE.getSkill());
						final int pos = room.getBurnerPos(npc);
						if (pos == room.getOrder()[room.getCurrentPot()])
						{
							if (room.getCurrentPot() < 8)
							{
								npc.setDisplayEffect(1);
								npc.setIsRunning(false);
								startQuestTimer("off", 2000, npc, null);
								room.setCurrentPot(room.getCurrentPot() + 1);
							}
							else
							{
								addSpawn(TREASURE_BOX, room.getParticipant().getLocation(), true, 0);
								broadcastNpcSay(room.getManager(), Say2.NPC_ALL, NpcStringId.OH_YOUVE_SUCCEEDED);
								room.setCurrentPot(0);
								room.burnThemAll();
								startQuestTimer("off", 2000, room.getManager(), null);
								startQuestTimer("end", 4000, room.getManager(), null);
							}
						}
						else
						{
							if (room.getAttemptNumber() == MAX_ATTEMPTS)
							{
								broadcastNpcSay(room.getManager(), Say2.NPC_ALL, NpcStringId.AH_IVE_FAILED_GOING_FURTHER_WILL_BE_DIFFICULT);
								room.burnThemAll();
								startQuestTimer("off", 2000, room.getManager(), null);
								room.getParticipant().getListeners().remove(this);
								startQuestTimer("end", 4000, room.getManager(), null);
							}
							else if (room.getAttemptNumber() < MAX_ATTEMPTS)
							{
								broadcastNpcSay(room.getManager(), Say2.NPC_ALL, NpcStringId.AH_IS_THIS_FAILURE_BUT_IT_LOOKS_LIKE_I_CAN_KEEP_GOING);
								room.burnThemAll();
								startQuestTimer("off", 2000, room.getManager(), null);
								room.setAttemptNumber(room.getAttemptNumber() + 1);
							}
						}
						break;
					}
				}
			}
		}
	}

	private MinigameRoom initRoom(Npc manager)
	{
		final Npc[] burners = new Npc[9];
		Npc lastSpawn;
		int potNumber = 0;

		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn.getId() == BURNER)
			{
				lastSpawn = spawn.getLastSpawn();
				if ((potNumber <= 8) && Util.checkIfInRange(1000, manager, lastSpawn, false))
				{
					lastSpawn.setAutoAttackable(true);
					burners[potNumber++] = lastSpawn;
				}
			}
		}
		return new MinigameRoom(burners, manager);
	}

	private MinigameRoom getRoomByManager(Npc manager)
	{
		return (_rooms[0].getManager() == manager) ? _rooms[0] : _rooms[1];
	}

	private MinigameRoom getRoomByParticipant(Player participant)
	{
		return (_rooms[0].getParticipant() == participant) ? _rooms[0] : _rooms[1];
	}

	private class MinigameRoom
	{
		private final Npc[] _burners;
		private final Npc _manager;
		private Player _participant;
		private boolean _started;
		private int _attemptNumber;
		private int _currentPot;
		private final int _order[];

		public MinigameRoom(Npc[] burners, Npc manager)
		{
			_burners = burners;
			_manager = manager;
			_participant = null;
			_started = false;
			_attemptNumber = 1;
			_currentPot = 0;
			_order = new int[9];
		}

		public int getBurnerPos(Npc npc)
		{
			for (int i = 0; i < 9; i++)
			{
				if (npc.equals(_burners[i]))
				{
					return i;
				}
			}
			return 0;
		}

		public void burnThemAll()
		{
			for (final Npc burner : _burners)
			{
				burner.setDisplayEffect(1);
				burner.setIsRunning(false);
			}
		}

		public Npc[] getBurners()
		{
			return _burners;
		}

		public Npc getManager()
		{
			return _manager;
		}

		public Player getParticipant()
		{
			return _participant;
		}

		public void setParticipant(Player participant)
		{
			_participant = participant;
		}

		public boolean getStarted()
		{
			return _started;
		}

		public void setStarted(boolean started)
		{
			_started = started;
		}

		public int getCurrentPot()
		{
			return _currentPot;
		}

		public void setCurrentPot(int pot)
		{
			_currentPot = pot;
		}

		public int getAttemptNumber()
		{
			return _attemptNumber;
		}

		public void setAttemptNumber(int attempt)
		{
			_attemptNumber = attempt;
		}

		public int[] getOrder()
		{
			return _order;
		}
	}

	public static void main(String[] args)
	{
		new Sumiel(Sumiel.class.getSimpleName(), "custom");
	}
}
