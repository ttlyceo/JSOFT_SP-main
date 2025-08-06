package l2e.gameserver.model.entity.events.tournaments;

import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.impl.CommunityBuffer;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.entity.events.tournaments.DAO.TournamentsDAO;
import l2e.gameserver.model.entity.events.tournaments.data.template.TournamentsEventsTemplate;
import l2e.gameserver.model.entity.events.tournaments.enums.RegisterType;
import l2e.gameserver.model.entity.events.tournaments.enums.TeamType;
import l2e.gameserver.model.entity.events.tournaments.enums.WrapperType;
import l2e.gameserver.model.entity.events.tournaments.model.TournamentTeam;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.model.entity.events.tournaments.wrappers.Countdown;
import l2e.gameserver.model.entity.events.tournaments.wrappers.EventWrapper;
import l2e.gameserver.model.entity.events.tournaments.wrappers.EventWrapperBuilder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.olympiad.OlympiadInfo;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static l2e.gameserver.network.serverpackets.ExShowScreenMessage.BOTTOM_RIGHT;

/**
 * @author psygrammator
 */
public class Tournament extends EventWrapper {
    private final Map<TeamType, TournamentTeam> _players = new ConcurrentHashMap<>();

    private final List<Player> _observers = new CopyOnWriteArrayList<>();

    private final int _id;
    private final int _teamSize;
    private final Reflection _instanceId;

    private final ClockTask _clock = new ClockTask();
    private final MatchCountdown _countdown = new MatchCountdown();
    private final MatchCountdownEnd _countdownEnd = new MatchCountdownEnd();

    private boolean _canAttack = true;

    public Tournament(final TournamentTeam blueTeam, final TournamentTeam redTeam) {
        _players.put(TeamType.BLUE, blueTeam);
        _players.put(TeamType.RED, redTeam);

        _id = IdFactory.getInstance().getNextId();
        _teamSize = blueTeam.size();
        _instanceId = ReflectionManager.getInstance().createRef();

        getPlayers().forEach(s -> s.setPartyTournament(this));
        getPlayers().forEach(s -> s.setTournamentGameId(_id));

        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(blueTeam.size());
        if(eventsTemplate != null && eventsTemplate.getDoors() != null && !eventsTemplate.getDoors().isEmpty())
        {
            _instanceId.setPvPInstance(true);
            eventsTemplate.getDoors().forEach(doorId -> _instanceId.addDoor(doorId, new StatsSet()));
        }

        TournamentData.getInstance().incrementAllTimeFights();

        //@formatter:off
        addWrapper(WrapperType.PREPARE, EventWrapperBuilder.of().
                addSync(this::preparePlayers).
                addSync(() -> scheduleAsync("COUNTDOWN", _countdown::start, Duration.ofSeconds(5))));

        addWrapper(WrapperType.START, EventWrapperBuilder.of().addSync(this::start));

        addWrapper(WrapperType.FINISH, EventWrapperBuilder.of().addSync(this::finish));

        addWrapper(WrapperType.BACK, EventWrapperBuilder.of().addSync(this::back));
        //@formatter:on

        executeSyncWrapper(WrapperType.PREPARE);
    }

    public int getId() {
        return _id;
    }

    public void checkWinner() {
        if (!getAliveOfTeam(TeamType.BLUE).isEmpty() && !getAliveOfTeam(TeamType.RED).isEmpty()) {
            return;
        }

        executeSyncWrapper(WrapperType.FINISH);
    }

    public void onKill(Creature killer, Player killed)
    {
        if(killer.isInPartyTournament() && killed.isInPartyTournament())
            killer.getTournamentStats().addTournamentKill(killer.getPartyTournament().getType());

        checkWinner();
    }

    /**
     * @return winner {@code Team} base on alive {@code Player}
     */
    public TeamType getWinner() {
        switch (Integer.compare(getAliveOfTeam(TeamType.BLUE).size(), getAliveOfTeam(TeamType.RED).size())) {
            case 1: {
                return TeamType.BLUE;
            }
            case -1: {
                return TeamType.RED;
            }
            default: {
                return TeamType.NONE;
            }
        }
    }

    /**
     * @return looser {@code Team} base on alive {@code Player}
     */
    public TeamType getLooser() {
        if(getWinner() == TeamType.NONE)
            return TeamType.NONE;

        return getWinner() == TeamType.BLUE ? TeamType.RED : TeamType.BLUE;
    }

    /**
     * l2e.gameserver.Announcements the status to {@code Tournament} for {@code Player}
     *
     * @param activeChar
     */
    public void broadcastStatusUpdate2(final Player activeChar) {
        TournamentUtil.toPlayers(getPlayers(), new ExOlympiadUserInfo(activeChar));
        TournamentUtil.toPlayers(getObservers(), new ExOlympiadUserInfo(activeChar));
    }

    /**
     * @param team
     * @return list of alive {@code Player} of {@code Team}
     */
    public List<Player> getAliveOfTeam(final TeamType team) {
        return getPlayersOfTeam(team).stream().filter(s -> !s.isDead()).collect(Collectors.toList());
    }

    /**
     * @param team
     * @return list of all {@code Player} of {@code Team}
     */
    public List<Player> getPlayersOfTeam(final TeamType team) {
        return _players.get(team).getMembers();
    }

    /**
     * @param team
     * @return list of all {@code TournamentTeam} of {@code Team}
     */
    public TournamentTeam getTeamOfType(final TeamType team) {
        return _players.get(team);
    }

    /**
     * @param activeChar
     */
    public void removePlayer(final Player activeChar) {

        activeChar.setPartyTournament(null);
        activeChar.setTournamentGameId(-1);

        _players.forEach((k, v) -> v.removeMember(activeChar));

        switch (getWrapperType()) {
            case PREPARE: {
                teleAllBack();

                TournamentUtil.toPlayers(getPlayers(), new ExShowScreenMessage("Match has been aborted.", 3000));
                TournamentUtil.toPlayers(getPlayers(), Say2.CLAN, "Match has been aborted.");
                break;
            }
            case START: {
                checkWinner();

                activeChar.teleToLocation(TeleportWhereType.TOWN, true, ReflectionManager.DEFAULT);
                break;
            }
        }
    }

    /**
     * @return list of all {@code Player} in this {@code Tournament}
     */
    public List<Player> getPlayers() {
        return _players.values().stream().flatMap(p -> p.getMembers().stream()).collect(Collectors.toList());
    }

    public List<TournamentTeam> getTeams() {
        return _players.values().stream().toList();
    }

    private void teleTeamToArray(final TeamType type)
    {
        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(_teamSize);
        List<Location> locationList = type == TeamType.BLUE ? eventsTemplate.getTeamBlueLoc() : eventsTemplate.getTeamRedLoc();
        final List<Player> players = getPlayersOfTeam(type);

        int locIndex = 0;
        for (Player player : players)
        {
            if (!player.isOnline())
                continue;

            Location loc = locationList.get(locIndex);
            if (loc != null)
            {
                player.getLastLocation().set(player.getX(), player.getY(), player.getZ());

                player.setReflection(getInstanceId());
                player.teleToLocation(loc, 50,true, getInstanceId());
                locIndex++;
            }
            else
            {
                LOGGER.error("Tournament: Something goes wrong with locations of team " + type);
            }
        }
    }

    /**
     * @return list of all {@code Player} observers
     */
    public List<Player> getObservers() {
        return _observers;
    }

    private void preparePlayers() {
        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(getType());
        getPlayersOfTeam(TeamType.BLUE).forEach(s -> s.setTeam(TeamType.BLUE.getId()));
        getPlayersOfTeam(TeamType.RED).forEach(s -> s.setTeam(TeamType.RED.getId()));

        getTeamOfType(TeamType.BLUE).createParty();
        getTeamOfType(TeamType.RED).createParty();

        getTeamOfType(TeamType.BLUE).callBuffer();
        getTeamOfType(TeamType.RED).callBuffer();

        for (final Player player : getPlayers()) {
            if(eventsTemplate.isRemoveBuffEnter())
                player.stopAllEffects();
            if(eventsTemplate.isResetReuseSkillsEnter())
            {
                player.resetDisabledSkills();
                player.resetReuse();
                player.sendSkillList(true);
            }
            TournamentUtil.reviveAndRestore(player);

            if(!eventsTemplate.isSummons())
                Optional.ofNullable(player.getSummon()).ifPresent(s -> s.unSummon(player));

            if (player.isMounted()) {
                player.dismount();
            }

            player.setIsParalyzed(true);
            player.getTournamentStats().setTournamentMatchDamage(0);
        }

        teleTeamToArray(TeamType.BLUE);
        teleTeamToArray(TeamType.RED);

        _canAttack = false;

        TournamentUtil.toPlayers(getPlayers(), new ExShowScreenMessage("You are teleported in tournament zone.", 3000));
        TournamentUtil.toPlayers(getPlayers(), Say2.CLAN, "You are teleported in tournament zone.");
    }

    private void start() {
        _clock.start();

        _instanceId.getDoors().forEach(DoorInstance::closeMe);

        for(Player p : getPlayers())
        {
            TournamentUtil.reviveAndRestore(p);
            p.sendPacket(ShowBoard.STATIC);

            p.setIsParalyzed(false);
            p.setCanRevive(false);

            broadcastStatusUpdate(p);
            p.updateEffectIcons();
        }

        _canAttack = true;
    }

    public void stop()
    {
        _clock.stop();
        _countdown.stop();
        _countdownEnd.stop();

        scheduleAsync("TELEPORT", this::teleAllBack, Duration.ofMillis(100));
    }

    private void back() {
        _countdownEnd.stop();

        TournamentUtil.toPlayers(getPlayers(), new ExShowScreenMessage("Match Ended!", 1500));
        TournamentUtil.toPlayers(getPlayers(), ExOlympiadMatchEnd.STATIC_PACKET);
        TournamentUtil.toPlayers(getObservers(), ExOlympiadMatchEnd.STATIC_PACKET);

        scheduleAsync("TELEPORT", this::teleAllBack, Duration.ofSeconds(2));
    }

    private void finish() {
        _clock.stop();

        final TeamType teamWinner = getWinner();
        final TeamType teamLooser = getLooser();

        reward(teamWinner, teamLooser);

        getTeams().forEach(s -> s.addTotalDamageToPlayers(_teamSize));
        getTeams().forEach(TournamentTeam::resetTeamMatchDamage);

        getPlayers().forEach(s -> TournamentsDAO.getInstance().storeData(s, _teamSize));

        _countdownEnd.start();

        _canAttack = false;
    }

    private void reward(TeamType teamWinner, TeamType teamLooser)
    {
        final List<OlympiadInfo> list1 = new ArrayList<>(getType());
        final List<OlympiadInfo> list2 = new ArrayList<>(getType());

        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(_teamSize);
        if(teamWinner != TeamType.NONE && teamLooser != TeamType.NONE)
        {
            TournamentUtil.toPlayers(getPlayers(), new ExShowScreenMessage(teamWinner.toString() + " won the match. Congratulations!", 3000));
            TournamentUtil.toPlayers(getPlayers(), Say2.CLAN, teamWinner.name() + " won the match. Congratulations!");
            TournamentUtil.toPlayers(getObservers(), new ExShowScreenMessage(teamWinner.toString() + " won the match. Congratulations!", 3000));
            TournamentUtil.toPlayers(getObservers(), Say2.CLAN, teamWinner.name() + " won the match. Congratulations!");

            final List<Player> winners = getPlayersOfTeam(teamWinner);
            winners.forEach(s -> eventsTemplate.getWinner().forEach(i -> s.addItem(getClass().getSimpleName(), i, s, true)));
            winners.forEach(s -> s.getTournamentStats().addTournamentVictory(_teamSize));
            winners.forEach(s -> list1.add(new OlympiadInfo(s.getName(null), s.getClan() != null ? s.getClan().getName() : "", s.getClanId(), s.getActiveClass(), s.getTournamentStats().getTournamentMatchDamage(), s.getTournamentStats().getTournamentPoints(), 1)));

            final List<Player> looser = getPlayersOfTeam(teamLooser);
            looser.forEach(s -> eventsTemplate.getLooser().forEach(i -> s.addItem(getClass().getSimpleName(), i, s, true)));
            looser.forEach(s -> s.getTournamentStats().addTournamentDefeat(_teamSize));
            looser.forEach(s -> list2.add(new OlympiadInfo(s.getName(null), s.getClan() != null ? s.getClan().getName() : "", s.getClanId(), s.getActiveClass(), s.getTournamentStats().getTournamentMatchDamage(), s.getTournamentStats().getTournamentPoints(), -1)));

            getPlayers().forEach(p -> p.sendPacket(new ExReceiveOlympiadForTournamentList.OlympiadResult(false, teamWinner.getId(), list1, list2)));
        }
        else
        {
            TournamentUtil.toPlayers(getPlayers(), new ExShowScreenMessage("Match has ended in a tie.", 3000));
            TournamentUtil.toPlayers(getPlayers(), Say2.CLAN, "Match has ended in a tie.");
            TournamentUtil.toPlayers(getObservers(), new ExShowScreenMessage("Match has ended in a tie.", 3000));
            TournamentUtil.toPlayers(getObservers(), Say2.CLAN, "Match has ended in a tie.");

            final List<Player> ties = getPlayers();
            ties.forEach(s -> eventsTemplate.getTie().forEach(i -> s.addItem(getClass().getSimpleName(), i, s, true)));
            ties.forEach(s -> s.getTournamentStats().addTournamentTie(_teamSize));

            getPlayersOfTeam(TeamType.BLUE).forEach(s -> list1.add(new OlympiadInfo(s.getName(null), s.getClan() != null ? s.getClan().getName() : "", s.getClanId(), s.getActiveClass(), s.getTournamentStats().getTournamentMatchDamage(), s.getTournamentStats().getTournamentPoints(), 0)));
            getPlayersOfTeam(TeamType.RED).forEach(s -> list2.add(new OlympiadInfo(s.getName(null), s.getClan() != null ? s.getClan().getName() : "", s.getClanId(), s.getActiveClass(), s.getTournamentStats().getTournamentMatchDamage(), s.getTournamentStats().getTournamentPoints(), 0)));
            getPlayers().forEach(p -> p.sendPacket(new ExReceiveOlympiadForTournamentList.OlympiadResult(true, 0, list1, list2)));
        }


    }

    private void teleAllBack() {
        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(getType());
        _countdown.stop();

        getObservers().forEach(Player::leaveTournamentObserverMode);


        if(eventsTemplate.getRegisterType() == RegisterType.SOLO)
        {
            getTeamOfType(TeamType.BLUE).disbandTeam();
            getTeamOfType(TeamType.RED).disbandTeam();
        }

        for (final Player player : getPlayers())
        {
            if(eventsTemplate.isRemoveBuffExit())
                player.stopAllEffects();
            if(eventsTemplate.isResetReuseSkillsExit())
            {
                player.resetDisabledSkills();
                player.resetReuse();
                player.sendSkillList(true);
            }

            player.setPartyTournament(null);
            player.setTeam(TeamType.NONE.getId());
            player.broadcastUserInfo(true);

            if (player.getReflection().getId() == getInstanceId().getId())
            {
                TournamentUtil.reviveAndRestore(player);

                player.teleToLocation(player.getLastLocation(), true, player.getReflection());
                player.setReflection(ReflectionManager.DEFAULT);
                player.setIsParalyzed(false);
                player.setCanRevive(true);
                player.setTournamentGameId(-1);
                player.sendActionFailed();
            }
        }

        getObservers().clear();
        getPlayers().clear();

        ReflectionManager.getInstance().destroyRef(_instanceId.getId());
        IdFactory.getInstance().releaseId(getId());

        TournamentData.getInstance().removeTournament(this);
    }

    private class MatchCountdown extends Countdown {
        public MatchCountdown() {
            super(TournamentUtil.TOURNAMENT_MAIN.getTimerStart());
        }

        @Override
        public void onTick() {
            TournamentUtil.toPlayers(getPlayers(), SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_WILL_START_IN_S1_SECOND_S).addNumber(getTime()));
        }

        @Override
        public void onZero() {
            executeSyncWrapper(WrapperType.START);
        }
    }

    private class MatchCountdownEnd extends Countdown {
        public MatchCountdownEnd() {
            super(TournamentUtil.TOURNAMENT_MAIN.getTimerEnd());
        }

        @Override
        public void onTick() {
            TournamentUtil.toPlayers(getPlayers(), SystemMessage.getSystemMessage(SystemMessageId.GAME_WILL_END_IN_S1_SECONDS).addNumber(getTime()));
            TournamentUtil.toPlayers(getObservers(), SystemMessage.getSystemMessage(SystemMessageId.GAME_WILL_END_IN_S1_SECONDS).addNumber(getTime()));
        }

        @Override
        public void onZero() {
            executeSyncWrapper(WrapperType.BACK);
        }
    }

    private class ClockTask extends Countdown {
        public ClockTask() {
            super(TournamentUtil.TOURNAMENT_MAIN.getBattleDuration());
        }

        @Override
        public void onTick() {
            TournamentUtil.toPlayers(getPlayers(), new ExShowScreenMessage("Time: " + getTimeInString(), 1250, BOTTOM_RIGHT, false));
            TournamentUtil.toPlayers(getObservers(), new ExShowScreenMessage("Time: " + getTimeInString(), 1250, BOTTOM_RIGHT, false));
        }

        @Override
        public void onZero() {
            executeSyncWrapper(WrapperType.FINISH);
        }
    }

    public boolean canAttack()
    {
        return _canAttack;
    }

    public Reflection getInstanceId() {
        return _instanceId;
    }

    public int getType()
    {
        return _teamSize;
    }

    public boolean isFinish() {
        return getWrapperType() == WrapperType.FINISH;
    }
    public boolean isPrepare() {
        return getWrapperType() == WrapperType.PREPARE;
    }

    public boolean isRunning() {
        return getWrapperType() == WrapperType.START;
    }

    public boolean canAttack(Creature target, Creature attacker)
    {
        if (getWrapperType() != WrapperType.START)
        {
            return false;
        }
        final Player player = attacker.getActingPlayer();
        if (player == null)
        {
            return true;
        }

        if (player != null && player.isRespawnProtected())
        {
            return false;
        }

        if (target != null && target.isPlayer() && target.getActingPlayer().isRespawnProtected())
        {
            return false;
        }

        if(player.isOnSameSidePartyTournamentWith(target))
        {
            return false;
        }

        if (!canAttack())
        {
            return false;
        }
        return true;
    }

    public boolean canAction(Creature target, Creature attacker)
    {
        if (getWrapperType() != WrapperType.START)
        {
            return false;
        }
        final Player player = attacker.getActingPlayer();
        if (player == null)
        {
            return true;
        }

        if (attacker != null && target != null)
        {
            if (attacker.getObjectId() == target.getObjectId())
            {
                return true;
            }
        }
        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(_teamSize);
        if(eventsTemplate != null && player.isOnSameSidePartyTournamentWith(target) && !eventsTemplate.isCanTeamTarget())
        {
            return false;
        }

        return true;
    }

    public boolean canUseMagic(Creature target, Creature attacker, Skill skill)
    {
        if (getWrapperType() != WrapperType.START)
        {
            return false;
        }

        if (attacker.isSummon() && skill.isAura())
        {
            return true;
        }

        if (attacker != null && target != null)
        {
            if (!canUseSkill(attacker, target, skill))
            {
                return false;
            }

            if (attacker.getObjectId() == target.getObjectId())
            {
                return true;
            }
        }

        if (attacker != null && attacker.isPlayer() && attacker.getActingPlayer().isRespawnProtected())
        {
            return false;
        }

        if (target != null && target.isPlayer() && target.getActingPlayer().isRespawnProtected())
        {
            return false;
        }

        if(attacker.isOnSameSidePartyTournamentWith(target) && skill.isOffensive())
        {
            return false;
        }

        if (!canAttack())
        {
            return false;
        }
        return true;
    }

    public boolean canUseItem(ItemInstance item)
    {
        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(_teamSize);
        if (eventsTemplate != null && eventsTemplate.getRestrictedItems() != null)
        {
            for (final int id : eventsTemplate.getRestrictedItems())
            {
                if (item.getId() == id)
                {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canUseSkill(Creature caster, Creature target, Skill skill)
    {
        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(_teamSize);
        if (eventsTemplate != null && eventsTemplate.getRestrictedSkills() != null)
        {
            for (final int id : eventsTemplate.getRestrictedSkills())
            {
                if (skill.getId() == id)
                {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canUseScroll(Creature caster)
    {
        if (getWrapperType() != WrapperType.START)
        {
            return false;
        }

        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(_teamSize);
        if (eventsTemplate != null && caster != null && !eventsTemplate.isScroll())
        {
            return false;
        }
        return true;
    }

    public boolean canUsePotion(Creature caster)
    {
        if (getWrapperType() != WrapperType.START)
        {
            return false;
        }

        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(_teamSize);
        if (eventsTemplate != null && caster != null && !eventsTemplate.isPotion())
        {
            return false;
        }
        return true;
    }

    public boolean canUseEscape(Creature caster)
    {
        if (getWrapperType() != WrapperType.START)
        {
            return false;
        }

        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(_teamSize);
        if (eventsTemplate != null && caster != null && !eventsTemplate.isEscape())
        {
            return false;
        }
        return true;
    }

    public boolean canUseItemSummon(Creature caster)
    {
        if (getWrapperType() != WrapperType.START)
        {
            return false;
        }

        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(_teamSize);
        if (eventsTemplate != null && caster != null && !eventsTemplate.isItemSummon())
        {
            return false;
        }
        return true;
    }

    public boolean canRessurect(Player player, Creature creature)
    {
        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(_teamSize);
        return eventsTemplate != null && eventsTemplate.isResurrection();
    }

    public boolean canUseCommunity()
    {
        TournamentsEventsTemplate eventsTemplate = TournamentUtil.TOURNAMENT_EVENTS.get(_teamSize);
        return eventsTemplate != null && eventsTemplate.isCommunityBuffer();
    }

    public final void broadcastPacketToObservers(GameServerPacket packet)
    {
        getObservers().stream().filter(Player::inObserverMode).forEach(p -> p.sendPacket(packet));
    }

    public final void broadcastUserInfoToObserver(Player player)
    {
        getPlayers().forEach(p -> player.sendPacket(new ExOlympiadUserInfo(p)));
    }

    public final void broadcastStatusUpdate(Player player)
    {
        for (final Player target : getPlayers())
        {
            final ExOlympiadUserInfo packet = new ExOlympiadUserInfo(player);
            if (target != null)
            {
                target.sendPacket(packet);
            }
        }
        for (final Player target : getObservers())
        {
            final ExOlympiadUserInfo packet = new ExOlympiadUserInfo(player);
            if (target != null && target.inObserverMode())
            {
                target.sendPacket(packet);
            }
        }
    }
}