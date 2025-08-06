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
package l2e.scripts.quests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SoulCrystalParser;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.SoulCrystalTemplate;
import l2e.gameserver.model.actor.templates.npc.AbsorbInfo;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;

public class _350_EnhanceYourWeapon extends Quest
{
	protected static class PlayerResult
	{
		private final Player _player;
		private SystemMessageId _message;
		
		public PlayerResult(Player player)
		{
			_player = player;
		}
		
		public Player getPlayer()
		{
			return _player;
		}
		
		public SystemMessageId getMessage()
		{
			return _message;
		}
		
		public void setMessage(SystemMessageId message)
		{
			_message = message;
		}
		
		public void send()
		{
			if (_message != null)
			{
				_player.sendPacket(_message);
				_message = null;
			}
		}
	}
	
	private static final int[] STARTING_NPCS =
	{
	        30115, 30856, 30194
	};
	private static final int RED_SOUL_CRYSTAL0_ID = 4629;
	private static final int GREEN_SOUL_CRYSTAL0_ID = 4640;
	private static final int BLUE_SOUL_CRYSTAL0_ID = 4651;
	
	private boolean check(QuestState st)
	{
		for (int i = 4629; i < 4665; i++)
		{
			if (st.hasQuestItems(i))
			{
				return true;
			}
		}
		return false;
	}
	
	public _350_EnhanceYourWeapon(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		for (final int npcId : STARTING_NPCS)
		{
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		
		for (final var template : NpcsParser.getInstance().getAllNpcs())
		{
			if ((template != null) && !template.getAbsorbInfo().isEmpty())
			{
				addSkillSeeId(template.getId());
				addKillId(template.getId());
			}
		}
	}
	
	@Override
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		super.onSkillSee(npc, caster, skill, targets, isSummon);
		
		if ((skill == null) || (skill.getId() != 2096))
		{
			return null;
		}
		else if ((caster == null) || caster.isDead())
		{
			return null;
		}
		if (!(npc instanceof Attackable) || npc.isDead() || npc.getTemplate().getAbsorbInfo().isEmpty())
		{
			return null;
		}
		
		try
		{
			((Attackable) npc).addAbsorber(caster);
		}
		catch (final Exception e)
		{
			_log.error("", e);
		}
		return null;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var st = player.getQuestState(getName());
		if (st != null)
		{
			List<PlayerResult> list;
			final var party = player.getParty();
			if (party == null || party.getMemberCount() < 2)
			{
				list = new ArrayList<>(1);
				list.add(new PlayerResult(player));
			}
			else
			{
				list = new ArrayList<>(party.getMembers().size());
				for (final var m : party.getMembers())
				{
					if (m.isInRange(npc.getLocation(), Config.ALT_PARTY_RANGE2))
					{
						list.add(new PlayerResult(m));
					}
				}
			}
			
			if (list != null && !list.isEmpty())
			{
				final List<Integer> blockList = new ArrayList<>();
				for (final var info : npc.getTemplate().getAbsorbInfo())
				{
					calcAbsorb(list, blockList, (MonsterInstance) npc, info);
				}
				
				for (final var r : list)
				{
					r.send();
				}
				blockList.clear();
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final var st = player.getQuestState(getName());
		if (event.endsWith("-04.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.endsWith("-09.htm"))
		{
			st.giveItems(RED_SOUL_CRYSTAL0_ID, 1);
		}
		else if (event.endsWith("-10.htm"))
		{
			st.giveItems(GREEN_SOUL_CRYSTAL0_ID, 1);
		}
		else if (event.endsWith("-11.htm"))
		{
			st.giveItems(BLUE_SOUL_CRYSTAL0_ID, 1);
		}
		else if (event.equalsIgnoreCase("exit.htm"))
		{
			st.exitQuest(true);
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (st.getState() == State.CREATED)
		{
			st.set("cond", "0");
		}
		if (st.getInt("cond") == 0)
		{
			htmltext = npc.getId() + "-01.htm";
		}
		else if (check(st))
		{
			htmltext = npc.getId() + "-03.htm";
		}
		else if (!st.hasQuestItems(RED_SOUL_CRYSTAL0_ID) && !st.hasQuestItems(GREEN_SOUL_CRYSTAL0_ID) && !st.hasQuestItems(BLUE_SOUL_CRYSTAL0_ID))
		{
			htmltext = npc.getId() + "-21.htm";
		}
		return htmltext;
	}
	
	private void calcAbsorb(List<PlayerResult> players, List<Integer> blockList, MonsterInstance npc, AbsorbInfo info)
	{
		int memberSize = 0;
		List<PlayerResult> targets;
		switch (info.getAbsorbType())
		{
			case LAST_HIT :
				targets = Collections.singletonList(players.get(0));
				break;
			case PARTY_ALL :
				targets = players;
				break;
			case PARTY_RANDOM :
				memberSize = players.size();
				if (memberSize == 1)
				{
					targets = Collections.singletonList(players.get(0));
				}
				else
				{
					final int size = Rnd.get(memberSize);
					targets = new ArrayList<>(size);
					final List<PlayerResult> temp = new ArrayList<>(players);
					Collections.shuffle(temp);
					for (int i = 0; i < size; i++)
					{
						targets.add(temp.get(i));
					}
				}
				break;
			case PARTY_ONE :
				memberSize = players.size();
				if (memberSize == 1)
				{
					targets = Collections.singletonList(players.get(0));
				}
				else
				{
					final int rnd = Rnd.get(memberSize);
					targets = Collections.singletonList(players.get(rnd));
				}
				break;
			default :
				return;
		}
		
		for (final var target : targets)
		{
			if ((target == null) || !((target.getMessage() == null) || (target.getMessage() == SystemMessageId.SOUL_CRYSTAL_ABSORBING_REFUSED)))
			{
				continue;
			}
			final var targetPlayer = target.getPlayer();
			if (info.isSkill() && !npc.isAbsorbed(targetPlayer))
			{
				continue;
			}
			if (targetPlayer.getQuestState(getName()) == null || blockList.contains(targetPlayer.getObjectId()))
			{
				continue;
			}
			
			boolean resonation = false;
			SoulCrystalTemplate soulCrystal = null;
			final var items = targetPlayer.getInventory().getItems();
			for (final var item : items)
			{
				final var crystal = SoulCrystalParser.getInstance().getCrystal(item.getId());
				if (crystal == null)
				{
					continue;
				}
				
				if (soulCrystal != null)
				{
					target.setMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_FAILED_RESONATION);
					resonation = true;
					break;
				}
				soulCrystal = crystal;
			}
			
			if (resonation)
			{
				continue;
			}
			
			if (soulCrystal == null)
			{
				continue;
			}
			
			if (!info.canAbsorb(soulCrystal.getLvl() + 1))
			{
				target.setMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_REFUSED);
				continue;
			}
			
			int nextItemId = 0;
			if ((info.getCursedChance() > 0) && (soulCrystal.getCursedNextId() > 0))
			{
				nextItemId = Rnd.chance(info.getCursedChance()) ? soulCrystal.getCursedNextId() : 0;
			}
			
			if (nextItemId == 0)
			{
				nextItemId = Rnd.chance(info.getChance()) ? soulCrystal.getNextId() : 0;
			}
			
			if (nextItemId == 0)
			{
				target.setMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_FAILED);
				continue;
			}
			
			if (targetPlayer.destroyItemByItemId("SoulCrystal", soulCrystal.getId(), 1, targetPlayer, false))
			{
				blockList.add(targetPlayer.getObjectId());
				targetPlayer.addItem("SoulCrystal", nextItemId, 1, targetPlayer, true);
				targetPlayer.sendPacket(SystemMessageId.SOUL_CRYSTAL_ABSORBING_SUCCEEDED);
			}
			else
			{
				target.setMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_FAILED);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new _350_EnhanceYourWeapon(350, _350_EnhanceYourWeapon.class.getSimpleName(), "");
	}
}
