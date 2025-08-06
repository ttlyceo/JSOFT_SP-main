package l2e.gameserver.model.entity.events.tournaments.util;

import l2e.commons.util.Rnd;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.data.template.TournamentsEventsTemplate;
import l2e.gameserver.model.entity.events.tournaments.data.template.TournamentsMainTemplate;
import l2e.gameserver.model.entity.events.tournaments.enums.LimitClassType;
import l2e.gameserver.model.entity.events.tournaments.model.TournamentTeam;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.GameServerPacket;

import java.text.DateFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.*;
import java.util.function.*;

/**
 * @author psygrammator
 */
public class TournamentUtil {
    public static TournamentsMainTemplate TOURNAMENT_MAIN;
    public static List<Integer> TOURNAMENT_TYPES;
    public static Map<Integer, TournamentsEventsTemplate> TOURNAMENT_EVENTS;
    private static final ClassId[] FIGHTERS = { ClassId.fighter, ClassId.warrior, ClassId.gladiator, ClassId.warlord, ClassId.knight, ClassId.rogue, ClassId.elvenFighter, ClassId.elvenKnight, ClassId.elvenScout, ClassId.darkFighter, ClassId.palusKnight, ClassId.assassin, ClassId.orcFighter, ClassId.orcRaider, ClassId.destroyer, ClassId.orcMonk, ClassId.tyrant, ClassId.dwarvenFighter, ClassId.scavenger, ClassId.bountyHunter, ClassId.artisan, ClassId.warsmith, ClassId.duelist, ClassId.dreadnought, ClassId.titan, ClassId.grandKhavatari, ClassId.maestro };
    private static final ClassId[] TANKS = { ClassId.paladin, ClassId.darkAvenger, ClassId.templeKnight, ClassId.shillienKnight, ClassId.phoenixKnight, ClassId.hellKnight, ClassId.evaTemplar, ClassId.shillienTemplar };
    private static final ClassId[] ARCHERS = { ClassId.hawkeye, ClassId.silverRanger, ClassId.phantomRanger, ClassId.sagittarius, ClassId.moonlightSentinel, ClassId.ghostSentinel, ClassId.fortuneSeeker };
    private static final ClassId[] DAGGERS = { ClassId.treasureHunter, ClassId.plainsWalker, ClassId.abyssWalker, ClassId.adventurer, ClassId.windRider, ClassId.ghostHunter };
    private static final ClassId[] MAGES = { ClassId.mage, ClassId.wizard, ClassId.sorceror, ClassId.necromancer, ClassId.elvenMage, ClassId.elvenWizard, ClassId.spellsinger, ClassId.darkMage, ClassId.darkWizard, ClassId.spellhowler, ClassId.orcMage, ClassId.orcShaman, ClassId.archmage, ClassId.soultaker, ClassId.mysticMuse, ClassId.stormScreamer };
    private static final ClassId[] SUMMONERS = { ClassId.warlock, ClassId.elementalSummoner, ClassId.phantomSummoner, ClassId.arcanaLord, ClassId.elementalMaster, ClassId.spectralMaster };
    private static final ClassId[] HEALERS = { ClassId.bishop, ClassId.elder, ClassId.shillenElder, ClassId.cardinal, ClassId.evaSaint, ClassId.shillienSaint, ClassId.dominator };
    private static final ClassId[] SUPPORTS = { ClassId.cleric, ClassId.prophet, ClassId.swordSinger, ClassId.oracle, ClassId.bladedancer, ClassId.shillienOracle, ClassId.overlord, ClassId.warcryer, ClassId.hierophant, ClassId.swordMuse, ClassId.spectralDancer, ClassId.doomcryer };
    private static final ClassId[] KAMAELS = { ClassId.maleSoldier, ClassId.femaleSoldier, ClassId.trooper, ClassId.warder, ClassId.berserker, ClassId.maleSoulbreaker, ClassId.femaleSoulbreaker, ClassId.inspector, ClassId.doombringer, ClassId.maleSoulhound, ClassId.femaleSoulhound, ClassId.trickster, ClassId.arbalester, ClassId.judicator };

    public static boolean isLimitClassByType(Player player, LimitClassType limitClassType)
    {
        ClassId classId = player.getClassId();
        switch (limitClassType)
        {
            case FIGHTERS -> {
                return contains(FIGHTERS, classId);
            }
            case TANKS -> {
                return contains(TANKS, classId);
            }
            case ARCHERS -> {
                return contains(ARCHERS, classId);
            }
            case DAGGERS -> {
                return contains(DAGGERS, classId);
            }
            case MAGES -> {
                return contains(MAGES, classId);
            }
            case SUMMONERS -> {
                return contains(SUMMONERS, classId);
            }
            case HEALERS -> {
                return contains(HEALERS, classId);
            }
            case SUPPORTS -> {
                return contains(SUPPORTS, classId);
            }
            case KAMAELS -> {
                return contains(KAMAELS, classId);
            }
            default -> {
                return false;
            }
        }
    }

    public static void reviveAndRestore(final Creature character)
    {
        if (Objects.isNull(character))
        {
            return;
        }

        if (character.isDead())
        {
            character.doRevive();
        }

        character.setCurrentCp(character.getMaxCp());
        character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());

        if (Objects.nonNull(character.getSummon()))
        {
            if (character.getSummon().isDead())
            {
                character.getSummon().doRevive();
            }

            character.getSummon().setCurrentHpMp(character.getSummon().getMaxHp(), character.getSummon().getMaxMp());
        }
    }

    /**
     * @param location
     * @param range
     * @param includeZ
     * @return {@code Location}
     */
    public static Location randomizeILocational(final Location location, final ValueRange range, final boolean includeZ)
    {
        return new Location(location.getX() + (int) Rnd.get(range.getMinimum(), range.getMaximum()), location.getY() + (int) Rnd.get(range.getMinimum(), range.getMaximum()), includeZ ? location.getZ() + (int) Rnd.get(range.getMinimum(), range.getMaximum()) : location.getZ());
    }

    /**
     * @param ms
     * @return formatted time
     */
    public static String formatMillisToTime(final long ms)
    {
        final int seconds = (int) (ms / 1000) % 60;
        final int minutes = (int) (ms / (1000 * 60) % 60);
        final int hours = (int) (ms / (1000 * 60 * 60) % 24);

        return hours + "h " + minutes + "m " + seconds + "s(s)";
    }

    /**
     * @param <T>
     * @param iterable
     * @param consumer
     */
    public static <T> void doubleIterator(final Iterable<T> iterable, final BiConsumer<T, T> consumer)
    {
        final Iterator<T> it = iterable.iterator();

        if (it.hasNext())
        {
            while (it.hasNext())
            {
                final T next = it.next();

                if (it.hasNext())
                {
                    consumer.accept(next, it.next());
                }
            }
        }
    }

    /**
     * @param <T>
     * @param element
     * @param list
     * @return {@code T}
     */
    public static <T> T addToList(final T element, final List<T> list)
    {
        Objects.requireNonNull(list);

        if (Objects.nonNull(element))
        {
            list.add(element);
        }

        return element;
    }

    /**
     * @param <T>
     * @param element
     * @param list
     * @param consumer
     * @return {@code T}
     */
    public static <T> T addToList(final T element, final List<T> list, final Consumer<T> consumer)
    {
        if (Objects.nonNull(consumer))
        {
            consumer.accept(element);
        }

        return addToList(element, list);
    }

    private static final DateTimeFormatter WEEKLY_FORMATER = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("EEE H:mm").toFormatter(Locale.ROOT);
    private static final DateTimeFormatter DAILY_FORMATER = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("H:mm").toFormatter(Locale.ROOT);

    public static long parseWeeklyDate(final String str, final boolean compare)
    {
        final Calendar calendar = Calendar.getInstance();

        try
        {
            final TemporalAccessor temporal;

            if (Arrays.asList(DateFormatSymbols.getInstance().getShortWeekdays()).stream().filter(s -> !s.isEmpty() && str.trim().toLowerCase().contains(s.toLowerCase())).findFirst().isPresent())
            {
                temporal = WEEKLY_FORMATER.parse(str.trim());
                calendar.set(Calendar.DAY_OF_WEEK, DayOfWeek.from(temporal).getValue() + 1);
            }
            else
            {
                temporal = DAILY_FORMATER.parse(str.trim());
            }

            final LocalTime time = LocalTime.from(temporal);

            calendar.set(Calendar.HOUR_OF_DAY, time.getHour());
            calendar.set(Calendar.MINUTE, time.getMinute());
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (compare && calendar.getTimeInMillis() < System.currentTimeMillis())
            {
                calendar.add(Calendar.DAY_OF_MONTH, temporal.isSupported(ChronoField.DAY_OF_WEEK) ? 7 : 1);
            }
        }
        catch (final Exception e)
        {

        }

        return calendar.getTimeInMillis();
    }

    /**
     * @param <T>
     * @param element
     * @param predicate
     * @param operator
     * @return T
     */
    public static <T> T ifThenApply(final T element, final Predicate<T> predicate, final UnaryOperator<T> operator)
    {
        return Objects.nonNull(element) && predicate.test(element) ? operator.apply(element) : element;
    }

    /**
     * @param <T>
     * @param list
     * @param element
     * @param predicate
     * @return {@code List<T>}
     */
    public static <T> List<T> addIf(final List<T> list, final T element, final Predicate<T> predicate)
    {
        if (Objects.nonNull(list) && Objects.nonNull(element) && predicate.test(element))
        {
            list.add(element);
        }

        return list;
    }

    /**
     * @param <T>
     * @param supplier
     * @param defaultValue
     * @return result of {@code Supplier} or default value
     */
    public static <T> T tryWithNoException(final Supplier<? extends T> supplier, final T defaultValue)
    {
        try
        {
            return supplier.get();
        }
        catch (final Exception e)
        {
            return defaultValue;
        }
    }

    /**
     * @param <T> : The Object type.
     * @param array : the array to look into.
     * @return {@code true} if the array is empty or null.
     */
    public static <T> boolean isEmpty(T[] array)
    {
        return array == null || array.length == 0;
    }

    /**
     * @param <T> : The Object type.
     * @param array : the array to look into.
     * @param obj : the object to search for.
     * @return {@code true} if the array contains the object, {@code false} otherwise.
     */
    public static <T> boolean contains(T[] array, T obj)
    {
        if (array == null || array.length == 0)
            return false;

        for (T element : array)
            if (element.equals(obj))
                return true;

        return false;
    }

    /**
     * @param <T> : The Object type.
     * @param array1 : the array to look into.
     * @param array2 : the array to search for.
     * @return {@code true} if both arrays contains a similar value.
     */
    public static <T> boolean contains(T[] array1, T[] array2)
    {
        if (array1 == null || array1.length == 0)
            return false;

        if (array2 == null || array2.length == 0)
            return false;

        for (T element1 : array1)
        {
            for (T element2 : array2)
                if (element2.equals(element1))
                    return true;
        }
        return false;
    }

    /**
     * @param array : the array to look into.
     * @param obj : the integer to search for.
     * @return {@code true} if the array contains the integer, {@code false} otherwise.
     */
    public static boolean contains(int[] array, int obj)
    {
        if (array == null || array.length == 0)
            return false;

        for (int element : array)
            if (element == obj)
                return true;

        return false;
    }

    /**
     * @param array : the array to look into.
     * @param obj : the integer to search for.
     * @return {@code true} if the array contains the integer, {@code false} otherwise.
     */
    public static boolean contains(String[] array, int obj)
    {
        if (array == null || array.length == 0)
            return false;

        for (String element : array) {
            if (Integer.parseInt(element) == obj)
                return true;
        }
        return false;
    }

    /**
     * Concat two arrays of the same type into a single array.
     * @param <T> : The Object type.
     * @param first : The initial array used as recipient.
     * @param second : The second array to merge.
     * @return an array of the given type, holding informations of passed arrays.
     */
    public static <T> T[] concat(T[] first, T[] second)
    {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * Concat multiple arrays of the same type into a single array.
     * @param <T> : The Object type.
     * @param first : The initial array used as recipient.
     * @param rest : An array vararg.
     * @return an array of the given type, holding informations of passed arrays.
     */
    @SafeVarargs
    public static <T> T[] concatAll(T[] first, T[]... rest)
    {
        int totalLength = first.length;
        for (T[] array : rest)
            totalLength += array.length;

        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest)
        {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    public static void toPlayers(final Collection<Player> collection, final String text)
    {
        toPlayers(collection, text, false);
    }

    public static void toPlayers(final Collection<Player> collection, final String text, final boolean isCritical)
    {
        toPlayers(collection, new CreatureSay(0, isCritical ? Say2.CRITICAL_ANNOUNCE : Say2.ANNOUNCEMENT, "", text));
    }

    public static void toPlayers(final Collection<Player> collection, final GameServerPacket packet)
    {
        collection.stream().filter(Player::isOnline).forEach(p -> p.sendPacket(packet));
    }

    public static void toTeams(List<TournamentTeam> collection, String text)
    {
        toTeams(collection, text, false);
    }

    public static void toTeams(List<TournamentTeam> collection, String text, boolean isCritical)
    {
        toTeams(collection, new CreatureSay(0, isCritical ? Say2.CRITICAL_ANNOUNCE : Say2.ANNOUNCEMENT, "", text));
    }
    public static void toTeams(List<TournamentTeam> collection, GameServerPacket packet)
    {
        collection.stream().flatMap(team -> team.getMembers().stream()).filter(Player::isOnline).forEach(p -> p.sendPacket(packet));
    }
    public static void toTeam(TournamentTeam team, GameServerPacket packet)
    {
        team.getMembers().stream().filter(Player::isOnline).forEach(p -> p.sendPacket(packet));
    }
    public static void toPlayer(Player player, GameServerPacket packet)
    {
        player.sendPacket(packet);
    }

    public static void toPlayer(Player player, final String text, final boolean isCritical)
    {
        toPlayer(player, new CreatureSay(0, isCritical ? Say2.CRITICAL_ANNOUNCE : Say2.ANNOUNCEMENT, "", text));
    }

    public static String getStringNameType(int type)
    {
        return type > 0 ? "F" + type + "X" + type : "NONE";
    }

    public static int getIntNameType(String name)
    {
        if(name.equalsIgnoreCase("none"))
            return 0;

        String subName = name.substring(name.indexOf("X") + 1);
        if(subName.isEmpty() || !name.contains("X"))
            return 0;
        return Integer.parseInt(subName);
    }

    public static void toPlayer(Player player, int type, String charName, String text)
    {
        player.sendPacket(new CreatureSay(0, type, charName, text));
    }

    public static void toPlayer(Player player, int type, String text)
    {
        toPlayer(player, type, "[Tournament]", text);
    }

    public static void toPlayers(Collection<Player> players, int type, String charName, String text)
    {
        players.stream().filter(p -> Objects.nonNull(p) && p.isOnline()).forEach(p -> p.sendPacket(new CreatureSay(p.getObjectId(), type, charName, text)));
    }
    public static void toPlayers(Collection<Player> players, int type, String text)
    {
        toPlayers(players, type, "[Tournament]", text);
    }

    public static void toTeam(TournamentTeam team, int type, String text)
    {
        team.sendPacket(new CreatureSay(0, type, "[Tournament]", text));
    }

    public static String getClassName(String lang, int classId)
    {
        return ServerStorage.getInstance().getString(lang, "ClassName." + classId);
    }
}
