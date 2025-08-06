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
package l2e.gameserver.model.entity.events.model.impl;

import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Created by LordWinter
 */
public class TeamVSTeamEvent extends AbstractFightEvent
{
	public TeamVSTeamEvent(MultiValueSet<String> set)
	{
		super(set);
	}

	@Override
	public void onKilled(Creature actor, Creature victim)
	{
		if (actor != null && actor.isPlayer())
		{
			final FightEventPlayer realActor = getFightEventPlayer(actor.getActingPlayer());
			if (victim.isPlayer() && realActor != null)
			{
				realActor.getTeam().incScore(1);
				increaseKills(realActor);
				updateScreenScores();
				final ServerMessage msg = new ServerMessage("FightEvents.YOU_HAVE_KILL", realActor.getPlayer().getLang());
				msg.add(victim.getName(null));
				sendMessageToPlayer(realActor.getPlayer(), MESSAGE_TYPES.GM, msg);
			}
		}

		if (victim.isPlayer())
		{
			final FightEventPlayer realVictim = getFightEventPlayer(victim);
			if (realVictim != null)
			{
				realVictim.increaseDeaths();
				if (actor != null)
				{
					final ServerMessage msg = new ServerMessage("FightEvents.YOU_KILLED", realVictim.getPlayer().getLang());
					msg.add(actor.getName(null));
					sendMessageToPlayer(realVictim.getPlayer(), MESSAGE_TYPES.GM, msg);
				}
				victim.getActingPlayer().broadcastCharInfo();
			}
		}
		super.onKilled(actor, victim);
	}

	@Override
	public String getVisibleTitle(Player player, Player viewer, String currentTitle, boolean toMe)
	{
		final FightEventPlayer fPlayer = getFightEventPlayer(player);

		if (fPlayer == null)
		{
			return currentTitle;
		}
		final ServerMessage msg = new ServerMessage("FightEvents.TITLE_INFO", viewer.getLang());
		msg.add(fPlayer.getKills());
		msg.add(fPlayer.getDeaths());
		return msg.toString();
	}
}
