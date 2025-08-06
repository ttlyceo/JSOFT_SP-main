package top.jsoft.phaseevent;


import l2e.commons.util.Broadcast;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.handler.admincommandhandlers.AdminCommandHandler;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.jsoft.commons.time.Chronos;

import java.util.concurrent.ScheduledFuture;

/**
 * | Created by psygrammator
 * | @team jsoft
 */
public class PhaseManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PhaseManager.class.getName());

    private static final String VAR_PHASE = "CURRENT_PHASE";
    private static final String VAR_PHASE_END = "PHASE_END";

    private static final String PHASE_DAY = "Day";
    private static final String PHASE_NIGHT = "Night";
    public static final String PHASE_REDMOON = "RedMoon";

    private ScheduledFuture<?> _phaseTask;

    private static final PhaseManager INSTANCE = new PhaseManager();

    public static PhaseManager getInstance()
    {
        return INSTANCE;
    }

    private PhaseManager()
    {
        startPhaseCycle();
        AdminCommandHandler.getInstance().registerHandler(new AdminPhaseControl());
    }

    public void startPhaseCycle()
    {
        final String currentPhase = getCurrentPhase();
        final long timeLeft = getPhaseEndTime() - System.currentTimeMillis();

        _phaseTask = ThreadPoolManager.getInstance().schedule(this::nextPhase, timeLeft);
        PhaseMobSpawner.getInstance().spawnMobsForPhase(currentPhase);
        info("PhaseManager started. Current phase: " + currentPhase + ", ends in " + (timeLeft / 60000) + " minutes.");
    }

    public void nextPhase()
    {
        String nextPhase = "Day";
        final String current = getCurrentPhase();

        switch (current)
        {
            case PHASE_DAY:
                nextPhase = PHASE_NIGHT;
                setPhase(nextPhase, Config.PHASEEVENT_NIGHT_DURATION * Chronos.MILLIS_PER_MINUTE);
                break;

            case PHASE_NIGHT:
                nextPhase = PHASE_DAY;
                long nextDuration = isRedMoonTime() ? Config.PHASEEVENT_DAY_DURATION_BEFORE_REDMOON * Chronos.MILLIS_PER_MINUTE
                        : Config.PHASEEVENT_DAY_DURATION * Chronos.MILLIS_PER_MINUTE;
                setPhase(nextPhase, nextDuration);
                break;

            case PHASE_REDMOON:
                nextPhase = PHASE_DAY;
                setPhase(nextPhase, Config.PHASEEVENT_DAY_DURATION * Chronos.MILLIS_PER_MINUTE);
                break;
        }

        PhaseMobSpawner.getInstance().spawnMobsForPhase(nextPhase);
    }

    public void setPhase(String phase, long durationMillis)
    {
        ServerVariables.set(VAR_PHASE, phase);
        ServerVariables.set(VAR_PHASE_END, String.valueOf(System.currentTimeMillis() + durationMillis));

        Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "System", "Phase has changed to: " + phase + "!"));
        info("Phase changed to " + phase + ", for " + (durationMillis / 60000) + " minutes.");

        switch (phase)
        {
            case PHASE_DAY:
                Broadcast.toAllOnlinePlayers(new ExRedSky(0));
                Broadcast.toAllOnlinePlayers(SunSet.STATIC_PACKET);
                break;
            case PHASE_NIGHT:
                Broadcast.toAllOnlinePlayers(new ExRedSky(0));
                Broadcast.toAllOnlinePlayers(SunRise.STATIC_PACKET);
                break;
            case PHASE_REDMOON:
                GameObjectsStorage.getPlayers()
                        .stream()
                        .filter(player -> player != null && player.isOnline())
                        .forEach(player -> player.sendPacket(new EarthQuake(player.getX(), player.getY(), player.getZ(), 30, 3)));
                Broadcast.toAllOnlinePlayers(new ExRedSky(7200));
                break;
        }

        // Cancel any existing phase task
        if (_phaseTask != null)
        {
            _phaseTask.cancel(false);
            _phaseTask = null;
        }

        // Schedule the next phase change
        _phaseTask = ThreadPoolManager.getInstance().schedule(this::nextPhase, durationMillis);

        // Schedule pre-phase notification 5 minutes before the end
        ThreadPoolManager.getInstance().schedule(this::sendPrePhaseNotice, durationMillis - Chronos.MILLIS_PER_MINUTE * 5);
    }

    public void setPhaseImmediate(String phase, long durationMillis)
    {
        // Cancel any existing phase task
        if (_phaseTask != null)
        {
            _phaseTask.cancel(false);
            _phaseTask = null;
        }

        // Set the new phase and spawn mobs immediately
        setPhase(phase, durationMillis);
        PhaseMobSpawner.getInstance().spawnMobsForPhase(phase);
    }

    private void sendPrePhaseNotice()
    {
        String nextPhase = "unknown";
        String current = getCurrentPhase();
        switch (current)
        {
            case PHASE_DAY:
                nextPhase = PHASE_NIGHT;
                break;
            case PHASE_NIGHT:
                nextPhase = isRedMoonTime() ? PHASE_REDMOON : PHASE_DAY;
                break;
            case PHASE_REDMOON:
                nextPhase = PHASE_DAY;
                break;
        }
        Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "System", "Attention! Phase will switch to " + nextPhase + " in 5 minutes!"));
    }

    public String getCurrentPhase()
    {
        return ServerVariables.getString(VAR_PHASE, PHASE_DAY);
    }

    public long getPhaseEndTime()
    {
        return ServerVariables.getLong(VAR_PHASE_END, System.currentTimeMillis() + Config.PHASEEVENT_DAY_DURATION * Chronos.MILLIS_PER_MINUTE);
    }

    private boolean isRedMoonTime()
    {
        // проверяем попали ли мы в окно RedMoon
        final int hour = (int) ((System.currentTimeMillis() % Chronos.MILLIS_PER_DAY) / Chronos.MILLIS_PER_HOUR);
        return (hour >= Config.PHASEEVENT_REDMOON_START_HOUR && hour < Config.PHASEEVENT_REDMOON_END_HOUR);
    }

    private void info(String text)
    {
        LOGGER.info("PhaseManager: " + text);
    }
}