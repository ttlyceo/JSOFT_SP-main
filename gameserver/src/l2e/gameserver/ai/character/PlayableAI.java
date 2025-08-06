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
package l2e.gameserver.ai.character;

import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;

public abstract class PlayableAI extends CharacterAI
{
	public PlayableAI(Playable playable)
	{
		super(playable);
	}

	@Override
	protected void onIntentionAttack(Creature target, boolean shift)
	{
		if (target instanceof Playable)
		{
			if (_actor.isInFightEvent() && target.isInFightEvent())
			{
				for (final var e : _actor.getFightEvents())
				{
					if (e != null && !e.canAttack(target, _actor))
					{
						clientActionFailed();
						return;
					}
				}
			}

			if (_actor.isInPartyTournament() && target.isInPartyTournament())
			{
				var e = _actor.getPartyTournament();
				if (e != null && !e.canAttack(target, _actor))
				{
					clientActionFailed();
					return;
				}
			}
			
			if (target.getActingPlayer().isProtectionBlessingAffected() && ((_actor.getActingPlayer().getLevel() - target.getActingPlayer().getLevel()) >= 10) && (_actor.getActingPlayer().getKarma() > 0) && !(target.isInsideZone(ZoneId.PVP)))
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}

			if (_actor.getActingPlayer().isProtectionBlessingAffected() && ((target.getActingPlayer().getLevel() - _actor.getActingPlayer().getLevel()) >= 10) && (target.getActingPlayer().getKarma() > 0) && !(target.isInsideZone(ZoneId.PVP)))
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}

			if (target.getActingPlayer().isCursedWeaponEquipped() && (_actor.getActingPlayer().getLevel() <= 20))
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}

			if (_actor.getActingPlayer().isCursedWeaponEquipped() && (target.getActingPlayer().getLevel() <= 20))
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}
		}
		super.onIntentionAttack(target, shift);
	}

	@Override
	protected void onIntentionCast(Skill skill, GameObject target)
	{
		if ((target instanceof Playable) && skill.isOffensive())
		{
			if (_actor.isInFightEvent() && target.getActingPlayer() != null && target.getActingPlayer().isInFightEvent())
			{
				for (final var e : _actor.getFightEvents())
				{
					if (e != null && !e.canUseMagic(target.getActingPlayer(), _actor, skill))
					{
						clientActionFailed();
						_actor.setIsCastingNow(false);
						return;
					}
				}
			}

			if (_actor.isInPartyTournament() && target.getActingPlayer() != null && target.getActingPlayer().isInPartyTournament())
			{
				var e = _actor.getPartyTournament();
				if (e != null && !e.canUseMagic(target.getActingPlayer(), _actor, skill))
				{
					clientActionFailed();
					_actor.setIsCastingNow(false);
					return;
				}
			}

			if (target.getActingPlayer().isProtectionBlessingAffected() && ((_actor.getActingPlayer().getLevel() - target.getActingPlayer().getLevel()) >= 10) && (_actor.getActingPlayer().getKarma() > 0) && !target.isInsideZone(ZoneId.PVP))
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}

			if (_actor.getActingPlayer().isProtectionBlessingAffected() && ((target.getActingPlayer().getLevel() - _actor.getActingPlayer().getLevel()) >= 10) && (target.getActingPlayer().getKarma() > 0) && !target.isInsideZone(ZoneId.PVP))
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}

			if (target.getActingPlayer().isCursedWeaponEquipped() && (_actor.getActingPlayer().getLevel() <= 20))
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}

			if (_actor.getActingPlayer().isCursedWeaponEquipped() && (target.getActingPlayer().getLevel() <= 20))
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}
		}
		super.onIntentionCast(skill, target);
	}
}