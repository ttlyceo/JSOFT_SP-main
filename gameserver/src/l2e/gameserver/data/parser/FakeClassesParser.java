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
package l2e.gameserver.data.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.fake.ai.AbyssWalkerAI;
import l2e.fake.ai.AdventurerAI;
import l2e.fake.ai.ArbalesterAI;
import l2e.fake.ai.ArchmageAI;
import l2e.fake.ai.ArtisanAI;
import l2e.fake.ai.AssassinAI;
import l2e.fake.ai.BerserkerAI;
import l2e.fake.ai.BladedancerAI;
import l2e.fake.ai.BountyHunterAI;
import l2e.fake.ai.DarkAvengerAI;
import l2e.fake.ai.DarkElvenFighterAI;
import l2e.fake.ai.DarkElvenMysticAI;
import l2e.fake.ai.DarkWizardAI;
import l2e.fake.ai.DestroyerAI;
import l2e.fake.ai.DominatorAI;
import l2e.fake.ai.DoombringerAI;
import l2e.fake.ai.DreadnoughtAI;
import l2e.fake.ai.DuelistAI;
import l2e.fake.ai.DwarvenFighterAI;
import l2e.fake.ai.ElvenFighterAI;
import l2e.fake.ai.ElvenKnightAI;
import l2e.fake.ai.ElvenMysticAI;
import l2e.fake.ai.ElvenScoutAI;
import l2e.fake.ai.ElvenWizardAI;
import l2e.fake.ai.FakePlayerAI;
import l2e.fake.ai.FemaleSoldierAI;
import l2e.fake.ai.FemaleSoulbreakerAI;
import l2e.fake.ai.FemaleSoulhoundAI;
import l2e.fake.ai.FortuneSeekerAI;
import l2e.fake.ai.GhostHunterAI;
import l2e.fake.ai.GhostSentinelAI;
import l2e.fake.ai.GladiatorAI;
import l2e.fake.ai.GrandKhavatariAI;
import l2e.fake.ai.HawkeyeAI;
import l2e.fake.ai.HellKnightAI;
import l2e.fake.ai.HumanFighterAI;
import l2e.fake.ai.HumanMysticAI;
import l2e.fake.ai.KnightAI;
import l2e.fake.ai.MaestroAI;
import l2e.fake.ai.MaleSoldierAI;
import l2e.fake.ai.MaleSoulbreakerAI;
import l2e.fake.ai.MaleSoulhoundAI;
import l2e.fake.ai.MoonlightSentinelAI;
import l2e.fake.ai.MysticMuseAI;
import l2e.fake.ai.NecromancerAI;
import l2e.fake.ai.OrcFighterAI;
import l2e.fake.ai.OrcMonkAI;
import l2e.fake.ai.OrcMysticAI;
import l2e.fake.ai.OrcRaiderAI;
import l2e.fake.ai.OrcShamanAI;
import l2e.fake.ai.OverlordAI;
import l2e.fake.ai.PaladinAI;
import l2e.fake.ai.PalusKnightAI;
import l2e.fake.ai.PhantomRangerAI;
import l2e.fake.ai.PlainsWalkerAI;
import l2e.fake.ai.RogueAI;
import l2e.fake.ai.SaggitariusAI;
import l2e.fake.ai.ScavengerAI;
import l2e.fake.ai.ShillienKnightAI;
import l2e.fake.ai.ShillienTemplarAI;
import l2e.fake.ai.SilverRangerAI;
import l2e.fake.ai.SorcerorAI;
import l2e.fake.ai.SoultakerAI;
import l2e.fake.ai.SpellhowlerAI;
import l2e.fake.ai.SpellsingerAI;
import l2e.fake.ai.StormScreamerAI;
import l2e.fake.ai.SwordSingerAI;
import l2e.fake.ai.TempleKnightAI;
import l2e.fake.ai.TitanAI;
import l2e.fake.ai.TreasureHunterAI;
import l2e.fake.ai.TricksterAI;
import l2e.fake.ai.TrooperAI;
import l2e.fake.ai.TyrantAI;
import l2e.fake.ai.WarcryerAI;
import l2e.fake.ai.WarderAI;
import l2e.fake.ai.WarlordAI;
import l2e.fake.ai.WarriorAI;
import l2e.fake.ai.WarsmithAI;
import l2e.fake.ai.WindRiderAI;
import l2e.fake.ai.WizardAI;
import l2e.gameserver.model.base.ClassId;

public class FakeClassesParser extends LoggerObject
{
	private static List<ClassId> _baseClasses = new ArrayList<>();
	private static List<ClassId> _firstClasses = new ArrayList<>();
	private static List<ClassId> _secondClasses = new ArrayList<>();
	private static List<ClassId> _thirdClasses = new ArrayList<>();
	
	protected FakeClassesParser()
	{
		load();
	}
	
	public void load()
	{
		_baseClasses.clear();
		_firstClasses.clear();
		_secondClasses.clear();
		_thirdClasses.clear();
		lordClasses();
		info("Loaded " + (_baseClasses.size() + _firstClasses.size() + _secondClasses.size() + _thirdClasses.size()) + " fake classes.");
	}
	
	public void lordClasses()
	{
		_baseClasses.add(ClassId.fighter);
		_baseClasses.add(ClassId.mage);
		_baseClasses.add(ClassId.elvenFighter);
		_baseClasses.add(ClassId.elvenMage);
		_baseClasses.add(ClassId.darkFighter);
		_baseClasses.add(ClassId.darkMage);
		_baseClasses.add(ClassId.orcFighter);
		_baseClasses.add(ClassId.orcMage);
		_baseClasses.add(ClassId.dwarvenFighter);
		_baseClasses.add(ClassId.maleSoldier);
		_baseClasses.add(ClassId.femaleSoldier);
		_firstClasses.add(ClassId.warrior);
		_firstClasses.add(ClassId.knight);
		_firstClasses.add(ClassId.rogue);
		_firstClasses.add(ClassId.wizard);
		_firstClasses.add(ClassId.elvenKnight);
		_firstClasses.add(ClassId.elvenScout);
		_firstClasses.add(ClassId.elvenWizard);
		_firstClasses.add(ClassId.palusKnight);
		_firstClasses.add(ClassId.assassin);
		_firstClasses.add(ClassId.darkWizard);
		_firstClasses.add(ClassId.orcRaider);
		_firstClasses.add(ClassId.orcMonk);
		_firstClasses.add(ClassId.orcShaman);
		_firstClasses.add(ClassId.scavenger);
		_firstClasses.add(ClassId.artisan);
		_firstClasses.add(ClassId.trooper);
		_firstClasses.add(ClassId.warder);
		_secondClasses.add(ClassId.gladiator);
		_secondClasses.add(ClassId.warlord);
		_secondClasses.add(ClassId.paladin);
		_secondClasses.add(ClassId.darkAvenger);
		_secondClasses.add(ClassId.treasureHunter);
		_secondClasses.add(ClassId.hawkeye);
		_secondClasses.add(ClassId.sorceror);
		_secondClasses.add(ClassId.necromancer);
		_secondClasses.add(ClassId.templeKnight);
		_secondClasses.add(ClassId.swordSinger);
		_secondClasses.add(ClassId.plainsWalker);
		_secondClasses.add(ClassId.silverRanger);
		_secondClasses.add(ClassId.spellsinger);
		_secondClasses.add(ClassId.shillienKnight);
		_secondClasses.add(ClassId.bladedancer);
		_secondClasses.add(ClassId.abyssWalker);
		_secondClasses.add(ClassId.phantomRanger);
		_secondClasses.add(ClassId.spellhowler);
		_secondClasses.add(ClassId.destroyer);
		_secondClasses.add(ClassId.tyrant);
		_secondClasses.add(ClassId.overlord);
		_secondClasses.add(ClassId.warcryer);
		_secondClasses.add(ClassId.bountyHunter);
		_secondClasses.add(ClassId.warsmith);
		_secondClasses.add(ClassId.berserker);
		_secondClasses.add(ClassId.maleSoulbreaker);
		_secondClasses.add(ClassId.femaleSoulbreaker);
		_secondClasses.add(ClassId.arbalester);
		_thirdClasses.add(ClassId.stormScreamer);
		_thirdClasses.add(ClassId.ghostSentinel);
		_thirdClasses.add(ClassId.ghostHunter);
		_thirdClasses.add(ClassId.shillienTemplar);
		_thirdClasses.add(ClassId.dominator);
		_thirdClasses.add(ClassId.titan);
		_thirdClasses.add(ClassId.grandKhavatari);
		_thirdClasses.add(ClassId.maestro);
		_thirdClasses.add(ClassId.fortuneSeeker);
		_thirdClasses.add(ClassId.sagittarius);
		_thirdClasses.add(ClassId.archmage);
		_thirdClasses.add(ClassId.soultaker);
		_thirdClasses.add(ClassId.mysticMuse);
		_thirdClasses.add(ClassId.moonlightSentinel);
		_thirdClasses.add(ClassId.adventurer);
		_thirdClasses.add(ClassId.windRider);
		_thirdClasses.add(ClassId.duelist);
		_thirdClasses.add(ClassId.dreadnought);
		_thirdClasses.add(ClassId.hellKnight);
		_thirdClasses.add(ClassId.maestro);
		_thirdClasses.add(ClassId.fortuneSeeker);
		_thirdClasses.add(ClassId.doombringer);
		_thirdClasses.add(ClassId.maleSoulhound);
		_thirdClasses.add(ClassId.femaleSoulhound);
		_thirdClasses.add(ClassId.trickster);
	}
	
	public List<ClassId> getBaseClasses()
	{
		return _baseClasses;
	}
	
	public List<ClassId> getFirstClasses()
	{
		return _firstClasses;
	}
	
	public List<ClassId> getSecondClasses()
	{
		return _secondClasses;
	}
	
	public List<ClassId> getThirdClasses()
	{
		return _thirdClasses;
	}
	
	public Map<ClassId, Class<? extends FakePlayerAI>> getAllAIs()
	{
		final Map<ClassId, Class<? extends FakePlayerAI>> ais = new HashMap<>();
		// Base classes
		ais.put(ClassId.fighter, HumanFighterAI.class);
		ais.put(ClassId.mage, HumanMysticAI.class);
		ais.put(ClassId.elvenFighter, ElvenFighterAI.class);
		ais.put(ClassId.elvenMage, ElvenMysticAI.class);
		ais.put(ClassId.darkFighter, DarkElvenFighterAI.class);
		ais.put(ClassId.darkMage, DarkElvenMysticAI.class);
		ais.put(ClassId.orcFighter, OrcFighterAI.class);
		ais.put(ClassId.orcMage, OrcMysticAI.class);
		ais.put(ClassId.dwarvenFighter, DwarvenFighterAI.class);
		ais.put(ClassId.maleSoldier, MaleSoldierAI.class);
		ais.put(ClassId.femaleSoldier, FemaleSoldierAI.class);
		// First profession classes
		ais.put(ClassId.warrior, WarriorAI.class);
		ais.put(ClassId.knight, KnightAI.class);
		ais.put(ClassId.rogue, RogueAI.class);
		ais.put(ClassId.wizard, WizardAI.class);
		ais.put(ClassId.elvenKnight, ElvenKnightAI.class);
		ais.put(ClassId.elvenScout, ElvenScoutAI.class);
		ais.put(ClassId.elvenWizard, ElvenWizardAI.class);
		ais.put(ClassId.palusKnight, PalusKnightAI.class);
		ais.put(ClassId.assassin, AssassinAI.class);
		ais.put(ClassId.darkWizard, DarkWizardAI.class);
		ais.put(ClassId.orcRaider, OrcRaiderAI.class);
		ais.put(ClassId.orcMonk, OrcMonkAI.class);
		ais.put(ClassId.orcShaman, OrcShamanAI.class);
		ais.put(ClassId.scavenger, ScavengerAI.class);
		ais.put(ClassId.artisan, ArtisanAI.class);
		ais.put(ClassId.trooper, TrooperAI.class);
		ais.put(ClassId.warder, WarderAI.class);
		// Second profession classes
		ais.put(ClassId.gladiator, GladiatorAI.class);
		ais.put(ClassId.warlord, WarlordAI.class);
		ais.put(ClassId.paladin, PaladinAI.class);
		ais.put(ClassId.darkAvenger, DarkAvengerAI.class);
		ais.put(ClassId.treasureHunter, TreasureHunterAI.class);
		ais.put(ClassId.hawkeye, HawkeyeAI.class);
		ais.put(ClassId.sorceror, SorcerorAI.class);
		ais.put(ClassId.necromancer, NecromancerAI.class);
		ais.put(ClassId.templeKnight, TempleKnightAI.class);
		ais.put(ClassId.swordSinger, SwordSingerAI.class);
		ais.put(ClassId.plainsWalker, PlainsWalkerAI.class);
		ais.put(ClassId.silverRanger, SilverRangerAI.class);
		ais.put(ClassId.spellsinger, SpellsingerAI.class);
		ais.put(ClassId.shillienKnight, ShillienKnightAI.class);
		ais.put(ClassId.bladedancer, BladedancerAI.class);
		ais.put(ClassId.abyssWalker, AbyssWalkerAI.class);
		ais.put(ClassId.phantomRanger, PhantomRangerAI.class);
		ais.put(ClassId.spellhowler, SpellhowlerAI.class);
		ais.put(ClassId.destroyer, DestroyerAI.class);
		ais.put(ClassId.tyrant, TyrantAI.class);
		ais.put(ClassId.overlord, OverlordAI.class);
		ais.put(ClassId.warcryer, WarcryerAI.class);
		ais.put(ClassId.bountyHunter, BountyHunterAI.class);
		ais.put(ClassId.warsmith, WarsmithAI.class);
		ais.put(ClassId.berserker, BerserkerAI.class);
		ais.put(ClassId.maleSoulbreaker, MaleSoulbreakerAI.class);
		ais.put(ClassId.femaleSoulbreaker, FemaleSoulbreakerAI.class);
		ais.put(ClassId.arbalester, ArbalesterAI.class);
		// Third profession classes
		ais.put(ClassId.stormScreamer, StormScreamerAI.class);
		ais.put(ClassId.mysticMuse, MysticMuseAI.class);
		ais.put(ClassId.archmage, ArchmageAI.class);
		ais.put(ClassId.soultaker, SoultakerAI.class);
		ais.put(ClassId.sagittarius, SaggitariusAI.class);
		ais.put(ClassId.moonlightSentinel, MoonlightSentinelAI.class);
		ais.put(ClassId.ghostSentinel, GhostSentinelAI.class);
		ais.put(ClassId.adventurer, AdventurerAI.class);
		ais.put(ClassId.windRider, WindRiderAI.class);
		ais.put(ClassId.ghostHunter, GhostHunterAI.class);
		ais.put(ClassId.dominator, DominatorAI.class);
		ais.put(ClassId.titan, TitanAI.class);
		ais.put(ClassId.shillienTemplar, ShillienTemplarAI.class);
		ais.put(ClassId.duelist, DuelistAI.class);
		ais.put(ClassId.hellKnight, HellKnightAI.class);
		ais.put(ClassId.grandKhavatari, GrandKhavatariAI.class);
		ais.put(ClassId.dreadnought, DreadnoughtAI.class);
		ais.put(ClassId.maestro, MaestroAI.class);
		ais.put(ClassId.fortuneSeeker, FortuneSeekerAI.class);
		ais.put(ClassId.doombringer, DoombringerAI.class);
		ais.put(ClassId.maleSoulhound, MaleSoulhoundAI.class);
		ais.put(ClassId.femaleSoulhound, FemaleSoulhoundAI.class);
		ais.put(ClassId.trickster, TricksterAI.class);
		return ais;
	}
	
	public static FakeClassesParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FakeClassesParser _instance = new FakeClassesParser();
	}
}