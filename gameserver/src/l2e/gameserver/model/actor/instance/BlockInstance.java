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


import l2e.commons.util.Rnd;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.ArenaParticipantsHolder;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.BlockCheckerEngine;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.serverpackets.ExBlockUpSetState;
import l2e.gameserver.network.serverpackets.NpcInfo;

public class BlockInstance extends MonsterInstance
{
	private int _colorEffect;

	public BlockInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	public void changeColor(Player attacker, ArenaParticipantsHolder holder, int team)
	{
		synchronized (this)
		{
			final BlockCheckerEngine event = holder.getEvent();
			if (_colorEffect == 0x53)
			{
				_colorEffect = 0x00;
				this.broadcastPacket(new NpcInfo.Info(this, attacker));
				increaseTeamPointsAndSend(attacker, team, event);
			}
			else
			{
				_colorEffect = 0x53;
				this.broadcastPacket(new NpcInfo.Info(this, attacker));
				increaseTeamPointsAndSend(attacker, team, event);
			}
			final int random = Rnd.get(100);

			if ((random > 69) && (random <= 84))
			{
				dropItem(13787, event, attacker);
			}
			else if (random > 84)
			{
				dropItem(13788, event, attacker);
			}
		}
	}

	public void setRed(boolean isRed)
	{
		_colorEffect = isRed ? 0x53 : 0x00;
	}

	@Override
	public int getColorEffect()
	{
		return _colorEffect;
	}

	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		if (attacker.isPlayer())
		{
			return (attacker.getActingPlayer() != null) && (attacker.getActingPlayer().getBlockCheckerArena() > -1);
		}
		return true;
	}

	@Override
	protected void onDeath(Creature killer)
	{
	}

	@Override
	public void onAction(Player player, boolean interact, boolean shift)
	{
		if (!canTarget(player))
		{
			return;
		}

		player.setLastFolkNPC(this);

		if (player.getTarget() != this)
		{
			player.setTarget(this);
			getAI();
		}
		else if (interact)
		{
			player.sendActionFailed();
		}
	}

	private void increaseTeamPointsAndSend(Player player, int team, BlockCheckerEngine eng)
	{
		eng.increasePlayerPoints(player, team);

		final int timeLeft = (int) ((eng.getStarterTime() - System.currentTimeMillis()) / 1000);
		final boolean isRed = eng.getHolder().getRedPlayers().contains(player);

		final ExBlockUpSetState changePoints = new ExBlockUpSetState(timeLeft, eng.getBluePoints(), eng.getRedPoints());
		final ExBlockUpSetState secretPoints = new ExBlockUpSetState(timeLeft, eng.getBluePoints(), eng.getRedPoints(), isRed, player, eng.getPlayerPoints(player, isRed));
		
		eng.getHolder().broadCastPacketToTeam(changePoints);
		eng.getHolder().broadCastPacketToTeam(secretPoints);
	}

	private void dropItem(int id, BlockCheckerEngine eng, Player player)
	{
		final ItemInstance drop = ItemsParser.getInstance().createItem("Loot", id, 1, player, this);
		final int x = getX() + Rnd.get(50);
		final int y = getY() + Rnd.get(50);
		final int z = getZ();

		drop.dropMe(this, x, y, z, true);

		eng.addNewDrop(drop);
	}
}