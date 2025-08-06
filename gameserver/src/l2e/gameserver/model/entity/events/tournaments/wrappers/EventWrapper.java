package l2e.gameserver.model.entity.events.tournaments.wrappers;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.entity.events.tournaments.enums.WrapperType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * @author psygrammator
 */
public class EventWrapper
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(EventWrapper.class.getName());

    private final Map<WrapperType, EventWrapperBuilder> _wrappers = new ConcurrentHashMap<>();
    private final Map<String, List<Future<?>>> _async = new ConcurrentHashMap<>();

    private WrapperType _wrapperType = WrapperType.PREPARE;

    protected final void addWrapper(final WrapperType type, final EventWrapperBuilder builder)
    {
        _wrappers.put(type, builder);
    }

    protected final void removeWrapper(final WrapperType type)
    {
        _wrappers.remove(type);
    }

    protected final WrapperType getWrapperType()
    {
        return _wrapperType;
    }

    protected final void setWrapperType(final WrapperType type)
    {
        _wrapperType = Objects.requireNonNullElse(type, WrapperType.PREPARE);
    }

    /**
     * @param type
     * @return {@code EventWrapperBuilder}
     */
    protected final EventWrapperBuilder getWrapper(final WrapperType type)
    {
        return _wrappers.get(type);
    }

    protected void executeSyncWrapper(final WrapperType type)
    {
        try
        {
            if (_wrappers.containsKey(type))
            {
                _wrappers.get(type).GET().forEach(s -> _async.computeIfAbsent(type.name(), k -> new ArrayList<>()).add(s));
            }
        }
        catch (final Exception e)
        {
            LOGGER.warn(getClass().getSimpleName() + ": Failed to execute. " + e);
            e.printStackTrace();
            onExecuteFailure();
        }

        setWrapperType(type);
    }

    protected void onExecuteFailure()
    {

    }

    /**
     * Schedules an a-sync task to be executed after the given {@code Duration}.<BR>
     * In case {@code Duration} is zero it will execute the task instantly.
     *
     * @param name
     * @param task
     * @param duration
     */
    protected final void scheduleAsync(final String name, final Runnable task, final Duration duration)
    {
        scheduleAsync(name, task, duration, true);
    }

    /**
     * Schedules an a-sync task to be executed after the given {@code Duration}.<BR>
     * In case {@code Duration} is zero it will execute the task instantly.
     *
     * @param name
     * @param task
     * @param duration
     * @param cancelPreviousAsync
     */
    protected final void scheduleAsync(final String name, final Runnable task, final Duration duration, final boolean cancelPreviousAsync)
    {
        if (cancelPreviousAsync)
        {
            cancelAsync(name);
        }

        if (!duration.isZero())
        {
            _async.computeIfAbsent(name, k -> new ArrayList<>()).add(ThreadPoolManager.getInstance().schedule(task, duration.toMillis()));
        }
        else
        {
            ThreadPoolManager.getInstance().execute(task);
        }
    }

    /**
     * Schedules an a-sync task to be executed after the given {@code Duration}.<BR>
     * Repeat the task every {@code Duration}
     *
     * @param name
     * @param task
     * @param duration
     */
    protected final void scheduleAsyncAtFixedRate(final String name, final Runnable task, final Duration duration)
    {
        _async.computeIfAbsent(name, k -> new ArrayList<>()).add(ThreadPoolManager.getInstance().scheduleAtFixedRate(task, duration.toMillis(), duration.toMillis()));
    }

    /**
     * Cancel all given scheduled tasks associated with given {@code name}
     *
     * @param name
     */
    protected final void cancelAsync(final String name)
    {
        _async.getOrDefault(name, Collections.emptyList()).stream().filter(Objects::nonNull).forEach(s -> s.cancel(true));

        _async.remove(name);
    }

    protected final void cancelAllAsync()
    {
        _async.keySet().forEach(this::cancelAsync);
    }
}
