package l2e.gameserver.model.entity.events.tournaments;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.JSOFT;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.admincommandhandlers.AdminCommandHandler;
import l2e.gameserver.handler.bypasshandlers.BypassHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.listener.player.OnPlayerEnterListener;
import l2e.gameserver.listener.player.OnPlayerExitListener;
import l2e.gameserver.listener.player.OnPlayerPartyLeaveListener;
import l2e.gameserver.listener.player.PlayerListenerList;
import l2e.gameserver.listener.player.impl.AskToJoinTeamAnswerListner;
import l2e.gameserver.listener.player.impl.AskTournamentAnswerListner;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.tournaments.Commands.AdminTournament;
import l2e.gameserver.model.entity.events.tournaments.Commands.TournamentBypasses;
import l2e.gameserver.model.entity.events.tournaments.Commands.VoiceTournament;
import l2e.gameserver.model.entity.events.tournaments.DAO.TournamentsDAO;
import l2e.gameserver.model.entity.events.tournaments.data.template.TournamentsEventsTemplate;
import l2e.gameserver.model.entity.events.tournaments.data.template.TournamentsMainTemplate;
import l2e.gameserver.model.entity.events.tournaments.enums.LimitClassType;
import l2e.gameserver.model.entity.events.tournaments.enums.RegisterType;
import l2e.gameserver.model.entity.events.tournaments.enums.TeamType;
import l2e.gameserver.model.entity.events.tournaments.enums.TournamentStatsType;
import l2e.gameserver.model.entity.events.tournaments.model.TournamentStats;
import l2e.gameserver.model.entity.events.tournaments.model.TournamentTeam;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.model.entity.events.tournaments.wrappers.AbstractRangeSchedule;
import l2e.gameserver.model.entity.events.tournaments.wrappers.Countdown;
import l2e.gameserver.model.olympiad.OlympiadManager;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author psygrammator
 */
public class TournamentData extends AbstractRangeSchedule implements OnPlayerEnterListener, OnPlayerExitListener, OnPlayerPartyLeaveListener
{
    private static final int RESULT_PER_PAGE = 10;
    private static final List<TournamentTeam> LOBBY = new CopyOnWriteArrayList<>();
    private static final Map<Player, Integer> LOBBY_SOLO = new ConcurrentHashMap<>();
    private static final List<Tournament> TOURNAMENT = new CopyOnWriteArrayList<>();
    private boolean tournamentTeleporting;
    private static final AtomicInteger allTimeFights = new AtomicInteger(0);
    private AnnounceCountdown announceCountdown = null;

    public TournamentData()
    {
        super(TournamentUtil.TOURNAMENT_MAIN.getSchedule());

        if (TournamentUtil.TOURNAMENT_MAIN.isEnable())
        {
            ThreadPoolManager.getInstance().scheduleAtFixedRate(this::checkLobby, TimeUnit.SECONDS.toMillis(TournamentUtil.TOURNAMENT_MAIN.getMatchFindTime()), TimeUnit.SECONDS.toMillis(TournamentUtil.TOURNAMENT_MAIN.getMatchFindTime()));

            if (TournamentUtil.TOURNAMENT_MAIN.isShedule())
            {
                start();
            }

            PlayerListenerList.addGlobal(this);
            VoicedCommandHandler.getInstance().registerHandler(new VoiceTournament());
            AdminCommandHandler.getInstance().registerHandler(new AdminTournament());
            BypassHandler.getInstance().registerHandler(new TournamentBypasses());
        }
    }

    public void onPlayerObserverLeave(final Player player)
    {
        getTournaments().stream().filter(s -> s.getObservers().contains(player)).findFirst().ifPresent(s -> s.getObservers().remove(player));
    }

    public void onPlayerPartyLeft(final Player player)
    {
        if(player.getTournamentTeam() != null)
            removeFromLobby(player.getTournamentTeam());

        removeFromLobbySolo(player);
    }

    public void onPlayerLogout(final Player player)
    {
        Optional.ofNullable(player.getPartyTournament()).ifPresent(s -> s.removePlayer(player));

        getTournaments().stream().filter(s -> s.getObservers().contains(player)).findFirst().ifPresent(s -> s.getObservers().remove(player));

        if(player.isInTournamentTeam())
        {
            removeFromLobby(player.getTournamentTeam());

            TournamentTeam team = player.getTournamentTeam();
            team.getMembers().remove(player);
            TournamentUtil.toTeam(team, Say2.CLAN, player.getName(null) + " left the Tournament Team.");
            if (team.getMembers().size() <= 1)
            {
                team.disbandTeam();
                return;
            }
            if (team.isLeader(player))
            {
                Player newLeader = team.getMembers().get(Rnd.get(team.getMembers().size()));
                if (newLeader != null)
                {
                    team.setLeader(newLeader);
                    TournamentUtil.toPlayer(newLeader, Say2.CLAN, "You has became the new Tournament Team Leader");
                }
                TournamentUtil.toTeam(team, Say2.CLAN,newLeader + " has became the new Team Leader");
            }
        }

        removeFromLobbySolo(player);
    }

    public boolean isInLobby(final TournamentTeam team)
    {
        return LOBBY.stream().anyMatch(e -> e == team);
    }
    public boolean isInLobbySolo(final Creature player)
    {
        if(!player.isPlayer())
            return false;

        return LOBBY_SOLO.keySet().stream().anyMatch(e -> e.getObjectId() == player.getObjectId());
    }

    public long sizeInLobbyByType(int type)
    {
        return TournamentUtil.TOURNAMENT_EVENTS.get(type).getRegisterType() == RegisterType.TEAM ? LOBBY.stream().filter(p -> p.size() == type).count() : TournamentUtil.TOURNAMENT_EVENTS.get(type).getRegisterType() == RegisterType.SOLO ? LOBBY_SOLO.entrySet().stream().filter(e -> e.getValue() == type).count() : 0;
    }

    public boolean checkClassRestriction(final TournamentTeam team, final TournamentsEventsTemplate eventsTemplate)
    {
        final int size = team.size();

        final List<Integer> disabledClasses = eventsTemplate.getRestrictedClass();

        if (!disabledClasses.isEmpty())
        {
            for (final Player player : team.getMembers())
            {
                if (disabledClasses.contains(player.getClassId().getId()))
                {
                    TournamentUtil.toTeam(team, new ExShowScreenMessage(player.getName(null) + " class is not allowed in " + (size + "vs" + size), 3000));
                    TournamentUtil.toTeam(team, Say2.CLAN, player.getName(null) + " class is not allowed in " + (size + "vs" + size));
                    return false;
                }
            }
        }

        return true;
    }
    public boolean checkClassLimitByType(final TournamentTeam team, final TournamentsEventsTemplate eventsTemplate)
    {
        final int size = team.size();

        final Map<LimitClassType, Integer> limitClassesByType = eventsTemplate.getLimitClassesByType();

        if (!limitClassesByType.isEmpty())
        {
            for(Map.Entry<LimitClassType, Integer> limitClassTypeIntegerEntry : limitClassesByType.entrySet())
            {
                LimitClassType limitClassType = limitClassTypeIntegerEntry.getKey();
                int limitCount = limitClassTypeIntegerEntry.getValue();
                if (limitCount != -1 && team.getClassTypeCount(limitClassType) > limitCount)
                {
                    TournamentUtil.toTeam(team, new ExShowScreenMessage(limitClassType.getName() + " limit " + limitCount + " in " + (size + "vs" + size), 3000));
                    TournamentUtil.toTeam(team, Say2.CLAN, limitClassType.getName() + " limit " + limitCount + " in " + (size + "vs" + size));
                    return false;
                }
            }
        }

        return true;
    }
    public boolean checkClassLimitById(final TournamentTeam team, final TournamentsEventsTemplate eventsTemplate)
    {
        final int size = team.size();

        final Map<Integer, Integer> limitClassesByType = eventsTemplate.getLimitClassesById();

        if (!limitClassesByType.isEmpty())
        {
            for(Map.Entry<Integer, Integer> limitClassTypeIntegerEntry : limitClassesByType.entrySet())
            {
                int classId = limitClassTypeIntegerEntry.getKey();
                int limitCount = limitClassTypeIntegerEntry.getValue();
                if (limitCount != -1 && team.getCountOfClass(classId) > limitCount)
                {
                    TournamentUtil.toTeam(team, new ExShowScreenMessage(TournamentUtil.getClassName(null, classId) + " limit " + limitCount + " in " + (size + "vs" + size), 3000));
                    TournamentUtil.toTeam(team, Say2.CLAN, TournamentUtil.getClassName(null, classId) + " limit " + limitCount + " in " + (size + "vs" + size));
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Add {@code Player} to lobby of {@code TournamentData}
     *
     * @param player
     * @param team
     */
    public boolean addToLobby(final Player player, final TournamentTeam team, final int type, final RegisterType registerType)
    {
        if (!TournamentUtil.TOURNAMENT_MAIN.isEnable())
        {
            TournamentUtil.toPlayer(player, new ExShowScreenMessage("Tournament participation is disabled.", 3000));
            TournamentUtil.toPlayer(player, Say2.CLAN, "Tournament participation is disabled.");
            return false;
        }

        if (TournamentUtil.TOURNAMENT_MAIN.isShedule() && !isEnabled())
        {
            TournamentUtil.toPlayer(player, new ExShowScreenMessage("Tournament participation open in " + TournamentUtil.formatMillisToTime(getReference().get().getFromInMillis(true) - System.currentTimeMillis()), 3000));
            TournamentUtil.toPlayer(player, Say2.CLAN, "Tournament participation open in " + TournamentUtil.formatMillisToTime(getReference().get().getFromInMillis(true) - System.currentTimeMillis()));
            return false;
        }

        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(type);
        if(eventsTemplate == null)
        {
            TournamentUtil.toPlayer(player, new ExShowScreenMessage("Tournament participation is disabled.", 3000));
            TournamentUtil.toPlayer(player, Say2.CLAN, "Tournament participation is disabled.");
            error("TournamentsEventsTemplate: " + type);
            return false;
        }

        if(registerType == RegisterType.TEAM)
        {
            if (team.getLeader() != player)
            {
                TournamentUtil.toPlayer(player, new ExShowScreenMessage("Only party leader can register party.", 3000));
                TournamentUtil.toPlayer(player, Say2.CLAN, "Only party leader can register party.");
                return false;
            }

            if (team.size() < type)
            {
                int needTo = type - team.size();
                TournamentUtil.toPlayer(player, new ExShowScreenMessage("You need to invite " + needTo + " players to register this mode.", 3000));
                TournamentUtil.toPlayer(player, Say2.CLAN, "You need to invite " + needTo + " players to register this mode.");
                return false;
            }

            if (isInLobby(team) || isInLobbySolo(player))
            {
                TournamentUtil.toTeam(team, new ExShowScreenMessage(team.getName() + " is already participant in tournament.", 3000));
                TournamentUtil.toTeam(team, Say2.CLAN, team.getName() + " is already participant in tournament.");
                return false;
            }

            if (!checkClassRestriction(team, eventsTemplate))
            {
                return false;
            }

            if (!checkClassLimitByType(team, eventsTemplate))
            {
                return false;
            }

            if (!checkClassLimitById(team, eventsTemplate))
            {
                return false;
            }

            for (Player activeChar : team.getMembers())
            {
                if (!player.isInsideRadius(activeChar, 2400, false, false))
                {
                    TournamentUtil.toTeam(team, new ExShowScreenMessage(activeChar.getName(null) + " is standing too far from you.", 3000));
                    TournamentUtil.toTeam(team, Say2.CLAN, activeChar.getName(null) + " is standing too far from you.");
                    return false;
                }

                if (activeChar.isInPartyTournament())
                {
                    TournamentUtil.toTeam(team, new ExShowScreenMessage(activeChar.getName(null) + " is already participant in tournament.", 3000));
                    TournamentUtil.toTeam(team, Say2.CLAN, activeChar.getName(null) + " is already participant in tournament.");
                    return false;
                }

                if (eventsTemplate.getRestrictedClass().contains(activeChar.getClassId().getId()))
                {
                    TournamentUtil.toTeam(team, new ExShowScreenMessage(activeChar.getName(null) + " class is restricted in tournament.", 3000));
                    TournamentUtil.toTeam(team, Say2.CLAN, activeChar.getName(null) + " class is restricted in tournament.");
                    return false;
                }

                if (OlympiadManager.getInstance().isRegistered(activeChar))
                {
                    TournamentUtil.toTeam(team, new ExShowScreenMessage(activeChar.getName(null) + " is already participant in Olympiad.", 3000));
                    TournamentUtil.toTeam(team, Say2.CLAN, activeChar.getName(null) + " is already participant in Olympiad.");
                    return false;
                }

                if (activeChar.isRegisteredInFightEvent())
                {
                    TournamentUtil.toTeam(team, new ExShowScreenMessage(activeChar.getName(null) + " is already participant in another Fight Event.", 3000));
                    TournamentUtil.toTeam(team, Say2.CLAN, activeChar.getName(null) + " is already participant in another Fight Event.");
                    return false;
                }
                TournamentsMainTemplate tournamentMain = TournamentUtil.TOURNAMENT_MAIN;
                if (tournamentMain.getMinLvl() != -1 && activeChar.getLevel() < tournamentMain.getMinLvl())
                {
                    TournamentUtil.toTeam(team, new ExShowScreenMessage(tournamentMain.getMinLvl() + " is the minimum level to participate.", 3000));
                    TournamentUtil.toTeam(team, Say2.CLAN, tournamentMain.getMinLvl() + " is the minimum level to participate.");
                    return false;
                }

                if (tournamentMain.getMaxLvl() != -1 && activeChar.getLevel() > tournamentMain.getMaxLvl())
                {
                    TournamentUtil.toTeam(team, new ExShowScreenMessage(tournamentMain.getMaxLvl() + " is the maximum level to participate.", 3000));
                    TournamentUtil.toTeam(team, Say2.CLAN, tournamentMain.getMaxLvl() + " is the maximum level to participate.");
                    return false;
                }

                if (activeChar.isInOlympiadMode() || activeChar.isInSiege())
                {
                    TournamentUtil.toTeam(team, new ExShowScreenMessage("Cannot participate while in olympiad, siege or event.", 3000));
                    TournamentUtil.toTeam(team, Say2.CLAN, "Cannot participate while in olympiad, siege or event.");
                    return false;
                }

                if (tournamentMain.getLimitByIp() > 0)
                {
                    final Map<String, AtomicInteger> ipAddressMap = new HashMap<>();

                    for (final Player lobbyMember : Stream.of(LOBBY.stream().map(TournamentTeam::getMembers).flatMap(Collection::stream).collect(Collectors.toList()), TOURNAMENT.stream().map(Tournament::getPlayers).flatMap(List::stream).collect(Collectors.toList()), team.getMembers()).flatMap(List::stream).toList())
                    {
                        if (ipAddressMap.computeIfAbsent(lobbyMember.getIPAddress(), k -> new AtomicInteger()).incrementAndGet() > tournamentMain.getLimitByIp())
                        {
                            TournamentUtil.toTeam(team, new ExShowScreenMessage("Only up to " + tournamentMain.getLimitByIp() + " players per IP are allowed to registered.", 3000));
                            TournamentUtil.toTeam(team, Say2.CLAN, "Only up to " + tournamentMain.getLimitByIp() + " players per IP are allowed to registered.");
                            return false;
                        }
                    }
                }

                if (tournamentMain.getLimitByHwid() > 0)
                {
                    final Map<String, AtomicInteger> hwidAddressMap = new HashMap<>();

                    for (final Player lobbyMember : Stream.of(LOBBY.stream().map(TournamentTeam::getMembers).flatMap(Collection::stream).collect(Collectors.toList()), TOURNAMENT.stream().map(Tournament::getPlayers).flatMap(List::stream).collect(Collectors.toList()), team.getMembers()).flatMap(List::stream).toList())
                    {
                        if (hwidAddressMap.computeIfAbsent(lobbyMember.getHWID(), k -> new AtomicInteger()).incrementAndGet() > tournamentMain.getLimitByHwid())
                        {
                            TournamentUtil.toTeam(team, new ExShowScreenMessage("Only up to " + tournamentMain.getLimitByHwid() + " players per HWID are allowed to registered.", 3000));
                            TournamentUtil.toTeam(team, Say2.CLAN, "Only up to " + tournamentMain.getLimitByHwid() + " players per HWID are allowed to registered.");
                            return false;
                        }
                    }
                }
            }

            LOBBY.add(team);

            TournamentUtil.toTeam(team, new ExShowScreenMessage("Successfuly added in tournament. Please wait until match is found.", 3000));
            TournamentUtil.toTeam(team, Say2.CLAN, "You have been registered in " + team.size() + " vs " + team.size() + ".");
        }
        else if(registerType == RegisterType.SOLO)
        {
            if (player.isInTournamentTeam())
            {
                TournamentUtil.toPlayer(player, new ExShowScreenMessage("You you can't register in tournament. Disband tournament team!", 3000));
                TournamentUtil.toPlayer(player, Say2.CLAN, "You you can't register in tournament. Disband tournament team!");
                return false;
            }

            if (player.isInPartyTournament() || player.isRegisteredTournament())
            {
                TournamentUtil.toPlayer(player, new ExShowScreenMessage("You is already participant in tournament.", 3000));
                TournamentUtil.toPlayer(player, Say2.CLAN, "You is already participant in tournament.");
                return false;
            }

            if (isInLobbySolo(player))
            {
                TournamentUtil.toPlayer(player, new ExShowScreenMessage("You is already participant in tournament.", 3000));
                TournamentUtil.toPlayer(player, Say2.CLAN, "You is already participant in tournament.");
                return false;
            }

            if (eventsTemplate.getRestrictedClass().contains(player.getClassId().getId()))
            {
                TournamentUtil.toPlayer(player, new ExShowScreenMessage("Your class is restricted in tournament.", 3000));
                TournamentUtil.toPlayer(player, Say2.CLAN, "Your class is restricted in tournament.");
                return false;
            }

            if (OlympiadManager.getInstance().isRegistered(player))
            {
                TournamentUtil.toPlayer(player, new ExShowScreenMessage("You is already participant in Olympiad.", 3000));
                TournamentUtil.toPlayer(player, Say2.CLAN, "You is already participant in Olympiad.");
                return false;
            }

            if (player.isRegisteredInFightEvent())
            {
                TournamentUtil.toPlayer(player, new ExShowScreenMessage("You is already participant in another Fight Event.", 3000));
                TournamentUtil.toPlayer(player, Say2.CLAN, "You is already participant in another Fight Event.");
                return false;
            }
            TournamentsMainTemplate tournamentMain = TournamentUtil.TOURNAMENT_MAIN;
            if (tournamentMain.getMinLvl() != -1 && player.getLevel() < tournamentMain.getMinLvl())
            {
                TournamentUtil.toPlayer(player, new ExShowScreenMessage(tournamentMain.getMinLvl() + " is the minimum level to participate.", 3000));
                TournamentUtil.toPlayer(player, Say2.CLAN, tournamentMain.getMinLvl() + " is the minimum level to participate.");
                return false;
            }

            if (tournamentMain.getMaxLvl() != -1 && player.getLevel() > tournamentMain.getMaxLvl())
            {
                TournamentUtil.toPlayer(player, new ExShowScreenMessage(tournamentMain.getMaxLvl() + " is the maximum level to participate.", 3000));
                TournamentUtil.toPlayer(player, Say2.CLAN, tournamentMain.getMaxLvl() + " is the maximum level to participate.");
                return false;
            }

            if (player.isInOlympiadMode() || player.isInSiege())
            {
                TournamentUtil.toPlayer(player, new ExShowScreenMessage("Cannot participate while in olympiad, siege or event.", 3000));
                TournamentUtil.toPlayer(player, Say2.CLAN, "Cannot participate while in olympiad, siege or event.");
                return false;
            }

            if (tournamentMain.getLimitByIp() > 0)
            {
                final Map<String, AtomicInteger> ipAddressMap = new HashMap<>();

                for (final Player lobbyMember : Stream.of(new ArrayList<>(LOBBY_SOLO.keySet()), TOURNAMENT.stream().map(Tournament::getPlayers).flatMap(List::stream).collect(Collectors.toList())).flatMap(List::stream).toList())
                {
                    if (ipAddressMap.computeIfAbsent(lobbyMember.getIPAddress(), k -> new AtomicInteger()).incrementAndGet() > tournamentMain.getLimitByIp())
                    {
                        TournamentUtil.toPlayer(player, new ExShowScreenMessage("Only up to " + tournamentMain.getLimitByIp() + " players per IP are allowed to registered.", 3000));
                        TournamentUtil.toPlayer(player, Say2.CLAN, "Only up to " + tournamentMain.getLimitByIp() + " players per IP are allowed to registered.");
                        return false;
                    }
                }
            }

            if (tournamentMain.getLimitByHwid() > 0)
            {
                final Map<String, AtomicInteger> hwidAddressMap = new HashMap<>();

                for (final Player lobbyMember : Stream.of(new ArrayList<>(LOBBY_SOLO.keySet()), TOURNAMENT.stream().map(Tournament::getPlayers).flatMap(List::stream).collect(Collectors.toList())).flatMap(List::stream).toList())
                {
                    if (hwidAddressMap.computeIfAbsent(lobbyMember.getHWID(), k -> new AtomicInteger()).incrementAndGet() > tournamentMain.getLimitByHwid())
                    {
                        TournamentUtil.toPlayer(player, new ExShowScreenMessage("Only up to " + tournamentMain.getLimitByHwid() + " players per HWID are allowed to registered.", 3000));
                        TournamentUtil.toPlayer(player, Say2.CLAN, "Only up to " + tournamentMain.getLimitByHwid() + " players per HWID are allowed to registered.");
                        return false;
                    }
                }
            }

            LOBBY_SOLO.put(player, type);

            TournamentUtil.toPlayer(player, new ExShowScreenMessage("Successfuly added in tournament. Please wait until match is found.", 3000));
            TournamentUtil.toPlayer(player, Say2.CLAN, "You have been registered in " + type + " vs " + type + ".");
        }

        return true;
    }

    /**
     * Remove {@code TournamentTeam} from lobby of {@code TournamentData}
     *
     * @param team
     */
    public void removeFromLobby(final TournamentTeam team)
    {
        if (Objects.nonNull(team))
        {
            LOBBY.remove(team);

            TournamentUtil.toTeam(team, new ExShowScreenMessage("Participation to lobby is removed because " + team.getName() + " has left.", 3000));
        }
    }

    /**
     * Remove {@code Player} from lobby of {@code TournamentData}
     *
     * @param player
     */
    public void removeFromLobbySolo(final Player player)
    {
        if (Objects.nonNull(player) && LOBBY_SOLO.keySet().stream().anyMatch(p -> p.getObjectId() == player.getObjectId()))
        {
            LOBBY_SOLO.remove(player);

            TournamentUtil.toPlayer(player, new ExShowScreenMessage("Participation to lobby is removed because " + player.getName(null) + " has left.", 3000));
        }
    }

    /**
     * Add {@code Tournament} to {@code TournamentData}
     *
     * @param tournament
     */
    public void addTournament(final Tournament tournament)
    {
        TOURNAMENT.add(tournament);
    }

    /**
     * Remove {@code Tournament} from {@code TournamentData}
     *
     * @param tournament
     */
    public void removeTournament(final Tournament tournament)
    {
        TOURNAMENT.remove(tournament);
    }

    /**
     * @param id
     * @return {@code Tournament}
     */
    public Tournament getTournament(final int id)
    {
        return TOURNAMENT.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
    }

    /**
     * @param type
     * @return {@code Tournament}
     */
    public long sizeTournamentsByType(final int type)
    {
        return TOURNAMENT.stream().filter(p -> p.getType() == type).count();
    }

    /**
     * @param type
     * @return {@code Tournament}
     */
    public int sizeObserversByType(final int type)
    {
        return TOURNAMENT.stream().filter(p -> p.getType() == type).mapToInt(p -> p.getObservers().size()).sum();
    }

    /**
     * @return list of {@code Tournament}
     */
    public List<Tournament> getTournaments()
    {
        return TOURNAMENT;
    }

    /**
     * @return list of all waiting {@code TournamentTeam} in lobby
     */
    public List<TournamentTeam> getLobbies()
    {
        return LOBBY;
    }

    public void checkLobby()
    {
        if(!isEnabled())
            return;

        if (LOBBY.size() > 1)
        {
            Collections.shuffle(LOBBY);

            TournamentUtil.TOURNAMENT_TYPES.forEach(i -> TournamentUtil.doubleIterator(LOBBY.
                    stream().filter(s -> s.size() == i).
                    collect(Collectors.toList()), (obj1, obj2) ->
            {
                addTournament(new Tournament(obj1, obj2));

                //System.out.println(LOBBY.contains(obj1));

                LOBBY.remove(obj1);
                LOBBY.remove(obj2);

                //System.out.println(LOBBY.contains(obj1));
            }));
        }

        if(LOBBY_SOLO.size() > 1)
        {
            //_log.warn("LOBBY SIZE: " + LOBBY_SOLO.size());
            Map<Integer, List<Player>> listMap = new ConcurrentHashMap<>();

            TournamentUtil.TOURNAMENT_TYPES.forEach(i -> listMap.put(i, LOBBY_SOLO.entrySet().stream().filter(s -> s.getValue() == i.intValue()).map(Map.Entry::getKey).limit(i * 2).collect(Collectors.toList())));

            //_log.warn("listMap: " + listMap.toString());

            AtomicInteger team = new AtomicInteger(1);
            TournamentUtil.TOURNAMENT_TYPES.forEach(i -> listMap.entrySet().
                    stream().filter(s -> s.getKey().intValue() == i.intValue()).filter(s -> s.getValue().size() > i).map(Map.Entry::getValue).limit(i * 2).
                    toList().stream().filter(p -> p.size() == (i * 2)).forEach(playerList ->
            {

                //_log.warn("playerList: " + playerList.toString());

                TournamentTeam team1 = null;
                TournamentTeam team2 = null;
                for(Player player : playerList)
                {
                    if(team.get() == 1)
                    {
                        if(team1 == null)
                            player.setTournamentTeam(team1 = new TournamentTeam(player, null));
                        else
                            team1.addMember(player);

                        //_log.warn("team1: " + player.getName(null));
                        team.set(2);
                    }
                    else
                    {
                        if(team2 == null)
                            player.setTournamentTeam(team2 = new TournamentTeam(player, null));
                        else
                            team2.addMember(player);

                        //_log.warn("team2: " + player.getName(null));
                        team.set(1);
                    }

                    LOBBY_SOLO.remove(player);
                }

                TOURNAMENT.add(new Tournament(team1, team2));
            }));
        }
    }

    public static TournamentData getInstance()
    {
        return SingletonHolder._instance;
    }

    private final List<Npc> _spawns = new ArrayList<>();

    @Override
    public void onStart()
    {
        info("Event Started.");

        TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(),  new ExShowScreenMessage("Tournament registration of party selected teams has opened!", 5000));

        TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(),"Tournament: Event Started.", true);
        StringBuilder stringBuilder = new StringBuilder();
        TournamentUtil.TOURNAMENT_TYPES.forEach(id -> stringBuilder.append(id).append("x").append(id).append(" / "));
        stringBuilder.replace(stringBuilder.lastIndexOf("/"), stringBuilder.lastIndexOf("/") + 1, "");
        TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(),"- > Shedule " + getNextSchedule().toString(), true);
        TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(),"- > Mods " + stringBuilder, true);
        TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(),"- > Duration " + TimeUnit.SECONDS.toMinutes(TournamentUtil.TOURNAMENT_MAIN.getBattleDuration()) + " minutes.", true);
        TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(),"- > Required level to participate: " + TournamentUtil.TOURNAMENT_MAIN.getMinLvl() + " - " + TournamentUtil.TOURNAMENT_MAIN.getMaxLvl(), true);
        TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(),"- > Commands .ts / .tournamentinvite", true);

        _spawns.forEach(Npc::deleteMe);
        _spawns.clear();

        for (final Location location : TournamentUtil.TOURNAMENT_MAIN.getNpcSpawnLocs())
        {
            try
            {
                final Spawner spawn = new Spawner(NpcsParser.getInstance().getTemplate(TournamentUtil.TOURNAMENT_MAIN.getNpcId()));
                spawn.setLocation(new Location(location.getX(), location.getY(), location.getZ(), location.getHeading()));
                _spawns.add(spawn.doSpawn());
            }
            catch(final Exception e)
            {
                error(getClass().getSimpleName() + ": onStart -> ", e);
            }
        }
        GameObjectsStorage.getPlayers().stream().filter(Player::isOnline).forEach(this::askTeleport);

        if(TournamentUtil.TOURNAMENT_MAIN.getAnnounceCountdown().length > 0)
        {
            announceCountdown = new AnnounceCountdown(getNextSchedule().getTo().toSecondOfDay() - getNextSchedule().getFrom().toSecondOfDay());
            announceCountdown.start();
        }
    }

    @Override
    public void onFinish()
    {
        if(TournamentUtil.TOURNAMENT_MAIN.getAnnounceCountdown().length > 0 && announceCountdown != null)
        {
            announceCountdown.stop();
            announceCountdown = null;
        }

        TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(),  new ExShowScreenMessage("Tournament registration has finished!", 5000));

        _spawns.forEach(Npc::deleteMe);
        _spawns.clear();

        getTournaments().forEach(Tournament::stop);
        GameObjectsStorage.getPlayers().stream().filter(Creature::isInTournamentTeam).forEach(p -> p.getTournamentTeam().disbandTeam());

        TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(),"Tournament: Event Finished.", true);
        TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(),"- > All fights have been stored.", true);
        TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(),"- > Next event: " + getNextSchedule().toString(), true);

        info("Event Finished.");
        info("All fights have been stored");
        info("Next event: " + getNextSchedule().toString());

        GameObjectsStorage.getPlayers().stream().filter(Player::isOnline).forEach(p -> TournamentUtil.toPlayer(p, Say2.PARTY, "[Tournament]", "Next randomly selected teams event: " + getNextTime()));
    }

    // Npc html part
    public void showHtml(Player player, String page, int type)
    {
        showHtml(player, page, type, 0);
    }
    public void showHtml(Player player, String page, int type, int index)
    {
        NpcHtmlMessage htm = new NpcHtmlMessage(0);
        htm.setFile(player, player.getLang(),"data/html/mods/tournament/" + page + ".htm");

        final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        htm.replace("%time%", format.format(new Date(getNextSchedule().getToInMillis())));
        htm.replace("%missingMembers%", getMembersMessageForFightType(player, type));
        htm.replace("%memberslist%", generateMemberList(player.getTournamentTeam()));
        htm.replace("%inviteBoxRegButton%", getInviteBoxOrRegisterButton(player, type));
        htm.replace("%fightType%", type == 0 ? "" : type + "x" + type);

        // Fight Data
        for(int id : TournamentUtil.TOURNAMENT_TYPES)
        {
            htm.replace("%victories" + TournamentUtil.getStringNameType(id) + "%", player.getTournamentStats().getTournamentStatsByType(id).get(TournamentStatsType.VICTORIES));
            htm.replace("%defeats" + TournamentUtil.getStringNameType(id) + "%", player.getTournamentStats().getTournamentStatsByType(id).get(TournamentStatsType.DEFEATS));
            htm.replace("%ties" + TournamentUtil.getStringNameType(id) + "%", player.getTournamentStats().getTournamentStatsByType(id).get(TournamentStatsType.TIES));
            htm.replace("%kills" + TournamentUtil.getStringNameType(id) + "%", player.getTournamentStats().getTournamentStatsByType(id).get(TournamentStatsType.KILLS));
            htm.replace("%damage" + TournamentUtil.getStringNameType(id) + "%", player.getTournamentStats().getTournamentStatsByType(id).get(TournamentStatsType.DAMAGE));
            htm.replace("%dpf" + TournamentUtil.getStringNameType(id) + "%", getDamagePerFight(player.getTournamentStats(), id));
            htm.replace("%fightsDone" + TournamentUtil.getStringNameType(id) + "%", player.getTournamentStats().getTournamentFightsDone(id));
            htm.replace("%teamsReg" + TournamentUtil.getStringNameType(id) + "%", sizeInLobbyByType(id));
            htm.replace("%activeFights" + TournamentUtil.getStringNameType(id) + "%", sizeTournamentsByType(id));
            htm.replace("%activeObservers" + TournamentUtil.getStringNameType(id) + "%", sizeObserversByType(id));
        }

        if(type > 0)
        {
            htm.replace("%observerFights%", generateObserverList(type, index, page));
        }

        htm.replace("%allTimeFights%", getAllTimeFights().get());
        htm.replace("%tourPoints%", player.getTournamentStats().getTournamentPoints());
        htm.replace("%killstotal%", player.getTournamentStats().getTotalTournamentKills());
        htm.replace("%totalDmg%", player.getTournamentStats().getTournamentTotalDamage());
        htm.replace("%playerName%", player.getName(null));
        htm.replace("%dpfTotal%", getDamagePerFight(player.getTournamentStats()));
        htm.replace("%wdt%", getWinDefeatTie(player.getTournamentStats()));
        htm.replace("%totalFights%", player.getTournamentStats().getTotalTournamentFightsDone());

        player.getTournamentStats().setLastPage(page);

        player.sendPacket(htm);
    }

    public String getMembersMessageForFightType(Player player, int type)
    {
        if (!player.isInTournamentTeam())
        {
            return "<font color=ff0000>You haven't a Tournament Team</font>";
        }
        if (type != 0)
        {
            return "You need to invite <font color=LEVEL>" + getMissingMembersForFightType(player, type) + "</font> to register " + (type + "x" + type) + " fights.";
        }
        return "";
    }

    public int getMissingMembersForFightType(Player player, int type)
    {
        int membersCount = 0;
        if (!player.isInTournamentTeam())
        {
            return -1;
        }
        membersCount = player.getTournamentTeam().getMembers().size();
        if(type == 1)
            return 0;
        else if(type > 1)
            return type - membersCount;
        else
            return -1;
    }

    public String generateMemberList(TournamentTeam team)
    {
        StringBuilder sb = new StringBuilder();
        if(team == null)
        {
            sb.append("<img src=L2UI.SquareGray width=300 height=1>");
            sb.append("<table width=300 bgcolor=000000>");
            sb.append("<tr>");
            sb.append("<td align=center><font color=LEVEL>").append("You haven't a Tournament Team").append("</font></td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("<img src=L2UI.SquareGray width=300 height=1>");
        }
        else
        {
            int i = 1;
            for (Player member : team.getMembers())
            {
                sb.append("<img src=L2UI.SquareGray width=300 height=1>");
                sb.append("<table width=300 bgcolor=000000>");
                sb.append("<tr>");
                sb.append("<td width=250 align=center>").append(team.size() > 1 ? i+". " : "").append("<font color=LEVEL>").append(member.getName(null)).append("</font></td>");
                sb.append("</tr>");
                sb.append("</table>");
                sb.append("<img src=L2UI.SquareGray width=300 height=1>");
                i++;
            }
        }
        return sb.toString();
    }
    public String generateObserverList(int type, int index, String page)
    {
        List<Tournament> list = TournamentData.TOURNAMENT.stream().filter(t -> t.getType() == type).toList();
        final int starting = index * RESULT_PER_PAGE;
        final int ending = Math.min(list.size(), (index + 1) * RESULT_PER_PAGE);

        final StringBuilder sb = new StringBuilder();

        if(!list.isEmpty()) {
            for (int i = starting; i < ending; i++) {
                final TournamentTeam teamBlue = list.get(i).getTeamOfType(TeamType.BLUE);
                final TournamentTeam teamRed = list.get(i).getTeamOfType(TeamType.RED);
                sb.append("<img src=L2UI.SquareGray width=300 height=1>");
                sb.append("<table width=300 bgcolor=000000>");
                sb.append("<tr>");
                sb.append("<td align=center><a action=\"bypass -h bp_observerTournament " + list.get(i).getId() + "\">" + teamBlue.getName() + " vs " + teamRed.getName() + "</a></td>");
                sb.append("</tr>");
                sb.append("</table>");
                sb.append("<img src=L2UI.SquareGray width=300 height=1>");
            }
        }
        else
        {
            sb.append("<img src=L2UI.SquareGray width=300 height=1>");
            sb.append("<table width=300 bgcolor=000000>");
            sb.append("<tr>");
            sb.append("<td align=center><font color=LEVEL>No active Tournament Fights</font></td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("<img src=L2UI.SquareGray width=300 height=1>");
        }

        sb.append("<br>");
        sb.append("<table width=90>");
        sb.append("<tr>");

        final int maxPages = (list.size() - 1) / RESULT_PER_PAGE;

        if (index > 0) {
            sb.append("<td><button action=\"bypass -h bp_showTournamentPage " + page + " " + Math.max(0, index - 1) + "\" width=15 height=15 back=L2UI_CH3.Button.prev1_down fore=L2UI_CH3.Button.prev1></td>");
        }

        sb.append("<td align=center>Page " + (index + 1) + "</td>");

        if (maxPages > index) {
            sb.append("<td><button action=\"bypass -h bp_showTournamentPage " + page + " " + Math.min(maxPages, index + 1) + "\" width=15 height=15 back=L2UI_CH3.Button.next1_down fore=L2UI_CH3.Button.next1></td>");
        }

        sb.append("</tr>");
        sb.append("</table>");

        return sb.toString();
    }

    public String getInviteBoxOrRegisterButton(Player player, int type)
    {
        StringBuilder sb = new StringBuilder();
        if (getMissingMembersForFightType(player, type) == 0)
        {
            sb.append("<img src=L2UI.SquareGray width=300 height=1>");
            sb.append("<table width=300 bgcolor=000000>");
            sb.append("<tr>");
            sb.append("<td width=265><font color=994992>Your team is ready!</font></td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("<img src=L2UI.SquareGray width=300 height=1>");
        }
        else
        {
            sb.append("<img src=L2UI.SquareGray width=300 height=1>");
            sb.append("<table width=300 bgcolor=000000>");
            sb.append("<tr>");
            sb.append("<td width=265><font color=994992>Type the name of your partner or use command: </font></td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("<img src=L2UI.SquareGray width=300 height=1>");
            sb.append("<table width=300 bgcolor=000000>");
            sb.append("<tr>");
            sb.append("<td width=265><font color=994992>\".tournamentinvite playername\"</font></td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("<img src=L2UI.SquareGray width=300 height=1>");
            sb.append("<table width=300 bgcolor=000000>");
            sb.append("<tr>");
            sb.append("<td width=\"80\"><font color=LEVEL>Player Name: </font></td>");
            sb.append("<td width=\"120\"><edit var=\"playerName\" width=120 height=10></td>");
            sb.append("<td width=\"65\"><button value=\"Invite\" action=\"bypass -h bp_inviteTournamentMember $playerName\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("<img src=L2UI.SquareGray width=300 height=1>");
        }

        return sb.toString();
    }

    public String getDamagePerFight(TournamentStats tournamentStats, int type)
    {
        DecimalFormat format = new DecimalFormat("0.#");
        double dpf = 0;
        int totalDamage = tournamentStats.getTournamentTotalDamage();
        int totalFightsDone = tournamentStats.getTournamentFightsDone(type);
        if (totalFightsDone == 0)
        {
            return format.format(0);
        }
        dpf = ((double) totalDamage / totalFightsDone);
        return format.format(dpf);
    }

    public String getDamagePerFight(TournamentStats tournamentStats)
    {
        DecimalFormat format = new DecimalFormat("0.#");
        double dpf = 0;
        int totalDamage = tournamentStats.getTournamentTotalDamage();
        int totalFightsDone = tournamentStats.getTotalTournamentFightsDone();
        if (totalFightsDone == 0)
        {
            return format.format(0);
        }
        dpf = ((double) totalDamage / totalFightsDone);
        return format.format(dpf);
    }

    public String getWinDefeatTie(TournamentStats tournamentStats)
    {
        DecimalFormat decimalFormat = new DecimalFormat("0.##");
        int ratioByFight = 1;
        double playerWDT = 0;
        int totalFightsDone = tournamentStats.getTotalTournamentFightsDone();
        int totalVictories = tournamentStats.getTotalVictories();
        int totalDefeats = tournamentStats.getTotalDefeats();
        int totalTies = tournamentStats.getTotalTies();
        if (totalFightsDone == 0)
        {
            return decimalFormat.format(0);
        }
        playerWDT = (double) (ratioByFight * (((3) * totalVictories) + ((-3) * totalDefeats) + (totalTies))) / totalFightsDone;
        return decimalFormat.format(playerWDT);
    }

    public String getWinDefeatTie(Player player, int type)
    {
        DecimalFormat decimalFormat = new DecimalFormat("0.##");
        int ratioByFight = 1;
        double playerWDT = 0;
        int totalFightsDone = player.getTournamentStats().getTournamentFightsDone(type);
        int totalVictories = player.getTournamentStats().getTournamentStats(type, TournamentStatsType.VICTORIES);
        int totalDefeats = player.getTournamentStats().getTournamentStats(type, TournamentStatsType.DEFEATS);
        int totalTies = player.getTournamentStats().getTournamentStats(type, TournamentStatsType.TIES);
        if (totalFightsDone == 0)
        {
            return decimalFormat.format(0);
        }
        playerWDT = (double) (ratioByFight * (((3) * totalVictories) + ((-3) * totalDefeats) + (totalTies))) / totalFightsDone;
        return decimalFormat.format(playerWDT);
    }

    public void showPlayerRankingData(Player player, int targetObjectId, int type)
    {
        TournamentStats tournamentStats = TournamentsDAO.getInstance().loadTournamentStatsFromTarget(targetObjectId);
        if(tournamentStats != null)
            showPlayerTournamentData(player, targetObjectId, type, tournamentStats);
        else
        {
            TournamentUtil.toPlayer(player, Say2.CLAN, "Error load stats try later.");
            showHtml(player, "main", 0);
        }
    }

    public void showPlayerTournamentData(Player player, int targetObjectId, int type, TournamentStats tournamentStats)
    {
        NpcHtmlMessage htm = new NpcHtmlMessage(0);
        htm.setFile(player, player.getLang(),"data/html/mods/tournament/ranking/info/playerInfo" + TournamentUtil.getStringNameType(type) + ".htm");

        // Fight Data
        for(int id : TournamentUtil.TOURNAMENT_TYPES)
        {
            htm.replace("%victories" + TournamentUtil.getStringNameType(id) + "%", tournamentStats.getTournamentStatsByType(id).get(TournamentStatsType.VICTORIES));
            htm.replace("%defeats" + TournamentUtil.getStringNameType(id) + "%", tournamentStats.getTournamentStatsByType(id).get(TournamentStatsType.DEFEATS));
            htm.replace("%ties" + TournamentUtil.getStringNameType(id) + "%", tournamentStats.getTournamentStatsByType(id).get(TournamentStatsType.TIES));
            htm.replace("%kills" + TournamentUtil.getStringNameType(id) + "%", tournamentStats.getTournamentStatsByType(id).get(TournamentStatsType.KILLS));
            htm.replace("%damage" + TournamentUtil.getStringNameType(id) + "%", tournamentStats.getTournamentStatsByType(id).get(TournamentStatsType.DAMAGE));
            htm.replace("%dpf" + TournamentUtil.getStringNameType(id) + "%", getDamagePerFight(tournamentStats, id));
            htm.replace("%fightsDone" + TournamentUtil.getStringNameType(id) + "%", tournamentStats.getTournamentFightsDone(id));
        }

        htm.replace("%tourPoints%", tournamentStats.getTournamentPoints());
        htm.replace("%killstotal%", tournamentStats.getTotalTournamentKills());
        htm.replace("%totalDmg%", tournamentStats.getTournamentTotalDamage());
        String nameById = CharNameHolder.getInstance().getNameById(targetObjectId);
        htm.replace("%playerName%", (nameById != null ? nameById : "Unknown"));
        htm.replace("%dpfTotal%", getDamagePerFight(tournamentStats));
        htm.replace("%wdt%", getWinDefeatTie(tournamentStats));
        htm.replace("%totalFights%", tournamentStats.getTotalTournamentFightsDone());

        player.sendPacket(htm);
    }

    // RANKING

    public String generateRankingRecords(Player player, int type, LinkedList<TourRankRecord> records, String rankType)
    {
        StringBuilder sb = new StringBuilder();
        int bgColor = 1;
        for (TourRankRecord record : records)
        {
            if (record == null)
                continue;
            if (bgColor % 2 == 0)
                sb.append("<table width=300 bgcolor=000000>");
            else
                sb.append("<table width=300>");
            sb.append("<tr>");
            sb.append("<td align=center fixwidth=20>");
            sb.append(record.pos);
            sb.append("</td>");
            sb.append("<td fixwidth=5></td>");
            sb.append("<td align=center fixwidth=75>");
            sb.append(record.playerName);
            sb.append("</td>");
            sb.append("<td align=center fixwidth=50>");
            sb.append(record.recordVal);
            sb.append("</td>");
            sb.append("<td align=center fixwidth=50>");
            sb.append("<a action=\"bypass bp_checkTournamentPlayer " + record.playerName + " " + TournamentUtil.getStringNameType(type) + "\"><font color=LEVEL>Check</font></a>");
            sb.append("</td>");
            sb.append("</tr>");
            sb.append("</table>");

        }
        return sb.toString();
    }

    public void showRanking(Player player, int fightType, String rankType)
    {
        NpcHtmlMessage htm = new NpcHtmlMessage(0);
        htm.setFile(player, player.getLang(),"data/html/mods/tournament/ranking/" + rankType + "/" + TournamentUtil.getStringNameType(fightType) + ".htm");

        //FIXME: ѕереписать закешировать.
        LinkedList<TourRankRecord> records = new LinkedList<>();
        int pos = 0;
        Connection con = null;
        PreparedStatement offline = null;
        try
        {
            con = DatabaseFactory.getInstance().getConnection();
            offline = con.prepareStatement("SELECT * FROM tournament_player_data WHERE fight_type=? ORDER BY " + rankType + " DESC LIMIT 10");
            offline.setString(1, TournamentUtil.getStringNameType(fightType));
            ResultSet rs = offline.executeQuery();
            while (rs.next())
            {

                records.add(new TourRankRecord(pos, CharNameHolder.getInstance().getNameById(rs.getInt("obj_id")), (rankType.equalsIgnoreCase("wdt") || rankType.equalsIgnoreCase("dpf")) ? rs.getString(rankType) : String.valueOf(rs.getInt(rankType))));
                pos++;

            }
        }
        catch (Exception e)
        {
            error("", e);
        }
        finally
        {
            DbUtils.closeQuietly(con, offline);
        }

        TournamentUtil.TOURNAMENT_TYPES.forEach(id -> htm.replace("%ranking-" + rankType + TournamentUtil.getStringNameType(id) + "%", generateRankingRecords(player, fightType, records, rankType)));
        player.sendPacket(htm);
    }

    class TourRankRecord
    {
        int pos;
        String playerName;
        String recordVal;

        public TourRankRecord(int pos, String playerName, String recordVal)
        {
            this.pos = pos + 1;
            this.playerName = playerName;
            this.recordVal = recordVal;
        }
    }

    public AtomicInteger getAllTimeFights()
    {
        return allTimeFights;
    }

    public void incrementAllTimeFights()
    {
        allTimeFights.getAndIncrement();
    }

    public boolean isTournamentTeleporting()
    {
        return tournamentTeleporting;
    }
    public void setTournamentTeleporting(boolean tournamentTeleporting)
    {
        this.tournamentTeleporting = tournamentTeleporting;
    }

    public void askJoinTeam(Player leader, Player target)
    {
        target.setTournamentTeamRequesterId(leader.getObjectId());
        target.setTournamentTeamBeingInvited(true);
        target.sendConfirmDlg(new AskToJoinTeamAnswerListner(target), 30000, "Do you wish to join " + leader.getName(null) + "'s Tournament Team?");
        TournamentUtil.toPlayer(leader, Say2.CLAN, target.getName(null) + " was invited to your team.");
    }

    public void askTeleport(Player player)
    {
        if (JSOFT.checkCondition(player))
        {
            setTournamentTeleporting(true);
            ThreadPoolManager.getInstance().schedule(() -> setTournamentTeleporting(false), TournamentUtil.TOURNAMENT_MAIN.getSummonToNpc() * 1000L);
            player.sendConfirmDlg(new AskTournamentAnswerListner(player), TournamentUtil.TOURNAMENT_MAIN.getSummonToNpc() * 1000, "Do you wish to teleport to Tournament Zone?");
        }
    }

    public final void reload()
    {
        if(TournamentUtil.TOURNAMENT_MAIN.isEnable())
        {
            reloadSchedule(TournamentUtil.TOURNAMENT_MAIN.getSchedule());
            if(isEnabled())
            {
                setEnabled(false);
                onFinish();
                stop();
            }

            LOBBY.clear();
            LOBBY_SOLO.clear();

            if(TournamentUtil.TOURNAMENT_MAIN.isShedule())
            {
                start();
            }
        }
    }

    private class AnnounceCountdown extends Countdown {
        public AnnounceCountdown(int time) {
            super(time);
        }

        @Override
        public void onTick() {
            if(TournamentUtil.contains(TournamentUtil.TOURNAMENT_MAIN.getAnnounceCountdown(), getTime()))
                announceEventEnd(getTime());
        }

        @Override
        public void onZero() {
            if(announceCountdown != null)
            {
                announceCountdown.stop();
                announceCountdown = null;
            }
        }

        private void announceEventEnd(int seconds) {
            int mins = seconds / 60;
            int secs = seconds % 60;

            TournamentUtil.toPlayers(GameObjectsStorage.getPlayers(), (mins == 0 ? "" : "Tournament: " + mins + " minute(s)") + (mins > 0 && secs > 0 ? " and " : "") + (secs == 0 ? "" : "Tournament: " + secs + " second(s)") + " remaining until the event ends.", true);
        }
    }

    @Override
    public void onPlayerEnter(Player player) {
        TournamentsDAO.getInstance().loadTournamentData(player);

        if(isEnabled())
        {
            TournamentUtil.toPlayer(player,"Tournament: Event Started.", true);
            StringBuilder stringBuilder = new StringBuilder();
            TournamentUtil.TOURNAMENT_TYPES.forEach(id -> stringBuilder.append(id).append("x").append(id).append(" / "));
            stringBuilder.replace(stringBuilder.lastIndexOf("/"), stringBuilder.lastIndexOf("/") + 1, "");
            TournamentUtil.toPlayer(player,"- > Shedule " + getNextSchedule().toString(), true);
            TournamentUtil.toPlayer(player,"- > Mods " + stringBuilder, true);
            TournamentUtil.toPlayer(player,"- > Duration " + TimeUnit.SECONDS.toMinutes(TournamentUtil.TOURNAMENT_MAIN.getBattleDuration()) + " minutes.", true);
            TournamentUtil.toPlayer(player,"- > Required level to participate: " + TournamentUtil.TOURNAMENT_MAIN.getMinLvl() + " - " + TournamentUtil.TOURNAMENT_MAIN.getMaxLvl(), true);
            TournamentUtil.toPlayer(player,"- > Commands .ts / .tournamentinvite", true);
        }

    }

    @Override
    public void onPlayerExit(Player player) {
        TournamentUtil.TOURNAMENT_TYPES.forEach(id -> TournamentsDAO.getInstance().storeData(player, id));

        TournamentData.getInstance().onPlayerLogout(player);
    }

    @Override
    public void onPartyLeave(Player player) {
        TournamentData.getInstance().onPlayerPartyLeft(player);
    }

    private static class SingletonHolder
    {
        protected static final TournamentData _instance = new TournamentData();
    }
}
