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
package l2e.scripts.instances;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.scripts.quests._196_SevenSignSealOfTheEmperor;

/**
 * Rework by LordWinter 16.11.2020
 */
public class SealOfTheEmperor extends AbstractReflection
{
	private class SIGNSWorld extends ReflectionWorld
	{
		public List<Npc> _npcList = new ArrayList<>();
		public Attackable _lilith = null;
		public Attackable _lilith_guard0 = null;
		public Attackable _lilith_guard1 = null;
		public Attackable _anakim = null;
		public Attackable _anakim_guard0 = null;
		public Attackable _anakim_guard1 = null;
		public Attackable _anakim_guard2 = null;

		public SIGNSWorld()
		{
		}
	}

	private static final NpcStringId[] ANAKIM_TEXT =
	{
	        NpcStringId.FOR_THE_ETERNITY_OF_EINHASAD, NpcStringId.DEAR_SHILLIENS_OFFSPRINGS_YOU_ARE_NOT_CAPABLE_OF_CONFRONTING_US, NpcStringId.ILL_SHOW_YOU_THE_REAL_POWER_OF_EINHASAD, NpcStringId.DEAR_MILITARY_FORCE_OF_LIGHT_GO_DESTROY_THE_OFFSPRINGS_OF_SHILLIEN
	};

	private static final NpcStringId[] LILITH_TEXT =
	{
	        NpcStringId.YOU_SUCH_A_FOOL_THE_VICTORY_OVER_THIS_WAR_BELONGS_TO_SHILIEN, NpcStringId.HOW_DARE_YOU_TRY_TO_CONTEND_AGAINST_ME_IN_STRENGTH_RIDICULOUS, NpcStringId.ANAKIM_IN_THE_NAME_OF_GREAT_SHILIEN_I_WILL_CUT_YOUR_THROAT, NpcStringId.YOU_CANNOT_BE_THE_MATCH_OF_LILITH_ILL_TEACH_YOU_A_LESSON
	};

	public SealOfTheEmperor()
	{
		super(SealOfTheEmperor.class.getSimpleName(), "instances");

		addStartNpc(32585, 32657);
		addTalkId(32585, 32657);
		addSkillSeeId(27384);
		addSpawnId(32718, 32715, 27384);
		addAggroRangeEnterId(27371, 27372, 27373, 27377, 27378, 27379);
		addAttackId(27384, 32715, 32716, 32717, 32718, 32719, 32720, 32721);
		addKillId(27371, 27372, 27373, 27374, 27375, 27377, 27378, 27379, 27384);
	}

	@Override
	public String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(player.getReflectionId());
		if (tmpworld != null && tmpworld instanceof SIGNSWorld)
		{
				((Attackable) npc).abortAttack();
				npc.setTarget(player);
				npc.setIsRunning(true);
				npc.getAI().setIntention(CtrlIntention.ATTACK, player);
		}
		return super.onAggroRangeEnter(npc, player, isSummon);
	}

	protected void runStartRoom(SIGNSWorld world)
	{
		world.setStatus(0);
		addSpawn(32586, -89456, 216184, -7504, 40960, false, 0, false, world.getReflection());
		addSpawn(32587, -89400, 216125, -7504, 40960, false, 0, false, world.getReflection());
		addSpawn(32657, -84385, 216117, -7497, 0, false, 0, false, world.getReflection());
		addSpawn(32598, -84945, 220643, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(32598, -89563, 220647, -7491, 0, false, 0, false, world.getReflection());
	}

	protected void runFirstRoom(SIGNSWorld world)
	{
		world._npcList.add(addSpawn(27371, -89049, 217979, -7495, 0, false, 0, false, world.getReflection()));
		addSpawn(27372, -89049, 217979, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27373, -89049, 217979, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27374, -89049, 217979, -7495, 0, false, 0, false, world.getReflection());
		world.setStatus(1);
	}

	protected void runSecondRoom(SIGNSWorld world)
	{
		world._npcList.clear();
		world._npcList.add(addSpawn(27371, -88599, 220071, -7495, 0, false, 0, false, world.getReflection()));
		addSpawn(27371, -88599, 220071, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27372, -88599, 220071, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27373, -88599, 220071, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27373, -88599, 220071, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27374, -88599, 220071, -7495, 0, false, 0, false, world.getReflection());
		world.setStatus(2);
	}

	protected void runThirdRoom(SIGNSWorld world)
	{
		world._npcList.clear();
		world._npcList.add(addSpawn(27371, -86846, 220639, -7495, 0, false, 0, false, world.getReflection()));
		addSpawn(27371, -86846, 220639, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27372, -86846, 220639, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27372, -86846, 220639, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27373, -86846, 220639, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27373, -86846, 220639, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27374, -86846, 220639, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27374, -86846, 220639, -7495, 0, false, 0, false, world.getReflection());
		world.setStatus(3);
	}

	protected void runForthRoom(SIGNSWorld world)
	{
		world._npcList.clear();
		world._npcList.add(addSpawn(27371, -85463, 219227, -7495, 0, false, 0, false, world.getReflection()));
		addSpawn(27372, -85463, 219227, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27373, -85463, 219227, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27374, -85463, 219227, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27375, -85463, 219227, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27377, -85463, 219227, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27378, -85463, 219227, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27379, -85463, 219227, -7495, 0, false, 0, false, world.getReflection());
		world.setStatus(4);
	}

	protected void runFifthRoom(SIGNSWorld world)
	{
		world._npcList.clear();
		world._npcList.add(addSpawn(27371, -87441, 217623, -7495, 0, false, 0, false, world.getReflection()));
		addSpawn(27372, -87441, 217623, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27373, -87441, 217623, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27374, -87441, 217623, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27375, -87441, 217623, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27375, -87441, 217623, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27377, -87441, 217623, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27377, -87441, 217623, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27378, -87441, 217623, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27378, -87441, 217623, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27379, -87441, 217623, -7495, 0, false, 0, false, world.getReflection());
		addSpawn(27379, -87441, 217623, -7495, 0, false, 0, false, world.getReflection());
		world.setStatus(5);
	}

	protected void runBossRoom(SIGNSWorld world)
	{
		world._lilith = (Attackable) addSpawn(32715, -83175, 217021, -7504, 49151, false, 0, false, world.getReflection());
		world._lilith_guard0 = (Attackable) addSpawn(32716, -83222, 217055, -7504, 49151, false, 0, false, world.getReflection());
		world._lilith_guard1 = (Attackable) addSpawn(32717, -83127, 217056, -7504, 49151, false, 0, false, world.getReflection());
		world._anakim = (Attackable) addSpawn(32718, -83179, 216479, -7504, 16384, false, 0, false, world.getReflection());
		world._anakim_guard0 = (Attackable) addSpawn(32719, -83227, 216443, -7504, 16384, false, 0, false, world.getReflection());
		world._anakim_guard1 = (Attackable) addSpawn(32720, -83179, 216432, -7504, 16384, false, 0, false, world.getReflection());
		world._anakim_guard2 = (Attackable) addSpawn(32721, -83134, 216443, -7504, 16384, false, 0, false, world.getReflection());

		world._lilith_guard0.setIsImmobilized(true);
		world._lilith_guard1.setIsImmobilized(true);
		world._anakim_guard0.setIsImmobilized(true);
		world._anakim_guard1.setIsImmobilized(true);
		world._anakim_guard2.setIsImmobilized(true);

		addSpawn(27384, -83177, 217353, -7520, 32768, false, 0, false, world.getReflection());
		addSpawn(27384, -83177, 216137, -7520, 32768, false, 0, false, world.getReflection());
		addSpawn(27384, -82588, 216754, -7520, 32768, false, 0, false, world.getReflection());
		addSpawn(27384, -83804, 216754, -7520, 32768, false, 0, false, world.getReflection());
		addSpawn(32592, -83176, 216753, -7497, 0, false, 0, false, world.getReflection());
		world.setStatus(6);
	}

	protected void runSDRoom(SIGNSWorld world)
	{
		final Npc npc1 = addSpawn(27384, -83177, 217353, -7520, 32768, false, 0, false, world.getReflection());
		npc1.setIsNoRndWalk(true);
		npc1.setRHandId(15281);
		final Npc npc2 = addSpawn(27384, -83177, 216137, -7520, 32768, false, 0, false, world.getReflection());
		npc2.setIsNoRndWalk(true);
		npc2.setRHandId(15281);
		final Npc npc3 = addSpawn(27384, -82588, 216754, -7520, 32768, false, 0, false, world.getReflection());
		npc3.setIsNoRndWalk(true);
		npc3.setRHandId(15281);
		final Npc npc4 = addSpawn(27384, -83804, 216754, -7520, 32768, false, 0, false, world.getReflection());
		npc4.setIsNoRndWalk(true);
		npc4.setRHandId(15281);
	}

	protected boolean checkKillProgress(SIGNSWorld world, Npc npc)
	{
		return world._npcList.contains(npc);
	}

	@Override
	protected void onTeleportEnter(Player player, ReflectionTemplate template, ReflectionWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
		else
		{
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
	}
	
	public final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new SIGNSWorld(), 112))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			runStartRoom((SIGNSWorld) world);
			runFirstRoom((SIGNSWorld) world);
		}
	}
	
	protected void exitInstance(Player player)
	{
		player.teleToLocation(171782, -17612, -4901, true, ReflectionManager.DEFAULT);

		final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
		if (world != null)
		{
			final Reflection inst = world.getReflection();
			inst.setDuration(5 * 60000);
			inst.setEmptyDestroyTime(0);
		}
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final int npcId = npc.getId();
		if ((npcId == 32715) || (npcId == 32716) || (npcId == 32717))
		{
			npc.setCurrentHp(npc.getCurrentHp() + damage);
			((Attackable) npc).getAggroList().stopHating(attacker);
		}

		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof SIGNSWorld)
		{
			final SIGNSWorld world = (SIGNSWorld) tmpworld;

			if (world.isStatus(6) && (npc.getId() == 27384))
			{
				npc.doCast(SkillsParser.getInstance().getInfo(5980, 3));
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon, skill);
	}

	@Override
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof SIGNSWorld)
		{
			final SIGNSWorld world = (SIGNSWorld) tmpworld;
			if ((skill.getId() == 8357) && (world.isStatus(6)) && (npc.getId() == 27384))
			{
				npc.doCast(SkillsParser.getInstance().getInfo(5980, 3));
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(player.getReflectionId());
		if (tmpworld != null && tmpworld instanceof SIGNSWorld)
		{
			final SIGNSWorld world = (SIGNSWorld) tmpworld;
			if (event.equalsIgnoreCase("DOORS"))
			{
				world.getReflection().openDoor(17240111);
				for (final int objId : world.getAllowed())
				{
					final Player pl = GameObjectsStorage.getPlayer(objId);
					if (pl != null)
					{
						pl.showQuestMovie(12);
					}
					ThreadPoolManager.getInstance().schedule(new SpawnLilithRoom(world), 22000);
					startQuestTimer("lilith_text", 26000, npc, player);
					startQuestTimer("anakim_text", 26000, npc, player);
					startQuestTimer("go_fight", 25000, npc, player);
				}
				return null;
			}
			else if (event.equalsIgnoreCase("anakim_text"))
			{
				cancelQuestTimer("anakim_text", npc, player);
				if (world._anakim != null)
				{
					final NpcSay ns = new NpcSay(world._anakim.getObjectId(), 0, world._anakim.getId(), ANAKIM_TEXT[getRandom(ANAKIM_TEXT.length)]);
					player.sendPacket(ns);
					startQuestTimer("anakim_text", 20000, npc, player);
				}
				return null;
			}
			else if (event.equalsIgnoreCase("lilith_text"))
			{
				cancelQuestTimer("lilith_text", npc, player);
				if (world._lilith != null)
				{
					final NpcSay ns = new NpcSay(world._lilith.getObjectId(), 0, world._lilith.getId(), LILITH_TEXT[getRandom(LILITH_TEXT.length)]);
					player.sendPacket(ns);
					startQuestTimer("lilith_text", 22000, npc, player);
				}
				return null;
			}
			else if (event.equalsIgnoreCase("go_fight"))
			{
				world._lilith_guard0.setIsImmobilized(false);
				world._lilith_guard1.setIsImmobilized(false);
				world._anakim_guard0.setIsImmobilized(false);
				world._anakim_guard1.setIsImmobilized(false);
				world._anakim_guard2.setIsImmobilized(false);
				return null;
			}
			else if (event.equalsIgnoreCase("Delete"))
			{
				world._lilith.deleteMe();
				world._lilith = null;
				world._anakim.deleteMe();
				world._anakim = null;
				world._lilith_guard0.deleteMe();
				world._lilith_guard0 = null;
				world._lilith_guard1.deleteMe();
				world._lilith_guard1 = null;
				world._anakim_guard0.deleteMe();
				world._anakim_guard0 = null;
				world._anakim_guard1.deleteMe();
				world._anakim_guard1 = null;
				world._anakim_guard2.deleteMe();
				world._anakim_guard2 = null;
				return null;
			}
			else if (event.equalsIgnoreCase("Tele"))
			{
				player.teleToLocation(-89528, 216056, -7516, true, player.getReflection());
				return null;
			}
		}
		return null;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		final String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(_196_SevenSignSealOfTheEmperor.class.getSimpleName());
		if (st == null)
		{
			return htmltext;
		}
		
		switch (npc.getId())
		{
			case 32585 :
				if (st.isCond(3) || st.isCond(4))
				{
					enterInstance(player, npc);
					return null;
				}
				break;
		}
		return htmltext;
	}

	@Override
	public final String onSpawn(Npc npc)
	{
		if (npc.getId() == 32718 || npc.getId() == 32715 || npc.getId() == 27384)
		{
			npc.setIsNoRndWalk(true);
			npc.setIsImmobilized(true);
		}
		return super.onSpawn(npc);
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(_196_SevenSignSealOfTheEmperor.class.getSimpleName());
		if (st == null)
		{
			return null;
		}
		
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof SIGNSWorld)
		{
			final SIGNSWorld world = (SIGNSWorld) tmpworld;
			if (world.isStatus(1))
			{
				if (checkKillProgress(world, npc))
				{
					runSecondRoom(world);
					world.getReflection().openDoor(17240102);
				}
			}
			else if (world.isStatus(2))
			{
				if (checkKillProgress(world, npc))
				{
					runThirdRoom(world);
					world.getReflection().openDoor(17240104);
				}
			}
			else if (world.isStatus(3))
			{
				if (checkKillProgress(world, npc))
				{
					runForthRoom(world);
					world.getReflection().openDoor(17240106);
				}
			}
			else if (world.isStatus(4))
			{
				if (checkKillProgress(world, npc))
				{
					runFifthRoom(world);
					world.getReflection().openDoor(17240108);
				}
			}
			else if (world.isStatus(5))
			{
				if (checkKillProgress(world, npc))
				{
					world.getReflection().openDoor(17240110);
				}
			}
			else if (world.isStatus(6))
			{
				if (npc.getId() == 27384)
				{
					if (st.getQuestItemsCount(13846) < 3)
					{
						npc.setRHandId(15281);
						st.playSound("ItemSound.quest_itemget");
						st.giveItems(13846, 1);
					}
					else
					{
						npc.setRHandId(15281);
						giveItems(player, 13846, 1);
						st.playSound("ItemSound.quest_middle");
						runSDRoom(world);
						player.showQuestMovie(13);
						startQuestTimer("Tele", 26000, null, player);
						startQuestTimer("Delete", 26000, null, player);
					}
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	private class SpawnLilithRoom implements Runnable
	{
		private final SIGNSWorld _world;

		public SpawnLilithRoom(SIGNSWorld world)
		{
			_world = world;
		}

		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null)
			{
				runBossRoom(_world);
			}
		}
	}

	public static void main(String[] args)
	{
		new SealOfTheEmperor();
	}
}
