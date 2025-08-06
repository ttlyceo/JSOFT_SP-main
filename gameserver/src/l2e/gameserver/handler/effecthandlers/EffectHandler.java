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
package l2e.gameserver.handler.effecthandlers;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.handler.effecthandlers.impl.*;
import l2e.gameserver.listener.ScriptListenerLoader;
import l2e.gameserver.model.skills.effects.Effect;

public final class EffectHandler extends LoggerObject
{
	private final Map<Integer, Class<? extends Effect>> _handlers;
	private final Path EFFECTS_FOLDER = new File(Config.DATAPACK_ROOT.getAbsolutePath(), "data/scripts/effecthandlers").toPath();
	
	private static final Class<?> _loadInstances = EffectHandler.class;

	private static final Class<?>[] _effects =
	{
	        AbortCast.class, Betray.class, BigHead.class, BlockAction.class, BlockBuffSlot.class, BlockChat.class, BlockParty.class, BlockResurrection.class, Bluff.class, Buff.class, CallParty.class, CallClan.class, CallPc.class, CallSkills.class, Cancel.class, CancelBySlot.class, CancelProbability.class, CancelAll.class, ChameleonRest.class, ChanceSkillTrigger.class, ChangeFace.class, ChangeHairColor.class, ChangeHairStyle.class, CharmOfCourage.class, CharmOfLuck.class, ClanGate.class, Confusion.class, ConsumeBody.class, CpHeal.class, CpHealOverTime.class, CpHealPercent.class, CpDamPercent.class, CubicMastery.class, DamOverTime.class, DamOverTimePercent.class, Debuff.class, Disarm.class, EnergyDamOverTime.class, EnlargeAbnormalSlot.class, EquipmentSet.class, Escape.class, FakeDeath.class, Fear.class, Flag.class, FocusEnergy.class, FocusMaxEnergy.class, FocusSouls.class, Fusion.class, GiveRecommendation.class, GiveSp.class, Grow.class, Harvesting.class, HealOverTime.class, HealPercent.class, Heal.class, HealDamPercent.class, Hide.class, HolythingPossess.class, HpByLevel.class, IgnoreSkills.class, ImmobileBuff.class, ImmobilePetBuff.class, Invincible.class, Lucky.class, ManaDamOverTime.class, ManaHeal.class, ManaDamPercent.class, ManaHealByLevel.class, ManaHealOverTime.class, ManaHealPercent.class, MpByLevel.class, MpConsumePerLevel.class, Mute.class, NoblesseBless.class, OpenCommonRecipeBook.class, OpenDwarfRecipeBook.class, OutpostDestroy.class, Paralyze.class, Petrification.class, PhoenixBless.class, PhysicalAttackMute.class, PhysicalMute.class, PcBangPointUp.class, ProtectionBlessing.class, RebalanceHP.class, RecoBonus.class, PetShare.class, ResetReflectionEntry.class, RandomizeHate.class, Recovery.class, RefuelAirship.class, Relax.class, Restoration.class, RestorationRandom.class, Root.class, ServitorShare.class, SetSkill.class, Signet.class, SignetMDam.class, SilentMove.class, SingleTarget.class, Sleep.class, SoulEating.class, Spoil.class, StealBuffs.class, Stun.class, SummonAgathion.class, SummonCubic.class, SummonNpc.class, SummonPet.class, SummonRide.class, SummonTrap.class, Sweeper.class, TargetCancel.class, TargetMe.class, Teleport.class, TransferDamage.class, TransferHate.class, Transformation.class, TransformationDispel.class, UnAggro.class, UnsummonAgathion.class, UnsummonServitor.class, VitalityPointUp.class, VisualSkin.class,
	};

	protected EffectHandler()
	{
		_handlers = new HashMap<>();
	}

	public void registerHandler(String name, Class<? extends Effect> func)
	{
		_handlers.put(name.hashCode(), func);
	}

	public final Class<? extends Effect> getHandler(String name)
	{
		return _handlers.get(name.hashCode());
	}

	public int size()
	{
		return _handlers.size();
	}

	public void executeScript() throws Exception
	{
		Object loadInstance = null;
		Method method = null;
		
		try
		{
			method = _loadInstances.getMethod("getInstance");
			loadInstance = method.invoke(_loadInstances);
		}
		catch (final Exception e)
		{
			warn("Failed invoking getInstance method for handler: " + _loadInstances.getSimpleName(), e);
			return;
		}
		
		method = null;
		
		for (final Class<?> c : _effects)
		{
			try
			{
				if (c == null)
				{
					continue;
				}
				
				if (method == null)
				{
					method = loadInstance.getClass().getMethod("registerHandler", String.class, Class.class);
				}
				method.invoke(loadInstance, c.getSimpleName(), c);
			}
			catch (final Exception e)
			{
				warn("Failed loading effect handler: " + c.getSimpleName(), e);
				continue;
			}
		}
		
		EFFECTS_FOLDER.toFile().mkdir();
		for (final File file : EFFECTS_FOLDER.toFile().listFiles())
		{
			if (file.isFile())
			{
				final String filePath = file.toURI().getPath();
				if (filePath.endsWith(".java"))
				{
					ScriptListenerLoader.getInstance().executeScript(file.toPath());
				}
			}
		}
		
		try
		{
			method = loadInstance.getClass().getMethod("size");
			final Object returnVal = method.invoke(loadInstance);
			if (Config.DEBUG)
			{
				info("Loaded " + returnVal + " effect templates.");
			}
		}
		catch (final Exception e)
		{
			warn("Failed invoking size method for handler: " + loadInstance.getClass().getSimpleName(), e);
		}
	}
	
	private static final class SingletonHolder
	{
		protected static final EffectHandler _instance = new EffectHandler();
	}

	public static EffectHandler getInstance()
	{
		return SingletonHolder._instance;
	}
}