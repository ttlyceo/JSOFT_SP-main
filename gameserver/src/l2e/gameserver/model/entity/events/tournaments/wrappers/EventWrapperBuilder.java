package l2e.gameserver.model.entity.events.tournaments.wrappers;

import l2e.gameserver.ThreadPoolManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author psygrammator
 */
public class EventWrapperBuilder
{
    private final List<WrapperHolder> _wrappers = new ArrayList<>();

    public EventWrapperBuilder addSync(final Runnable task)
    {
        _wrappers.add(new WrapperHolder(task, Duration.ZERO));
        return this;
    }

    public EventWrapperBuilder addAsync(final Runnable task, final Duration duration)
    {
        _wrappers.add(new WrapperHolder(task, duration));
        return this;
    }

    /**
     * @return {@code List} of all {@code Future}
     */
    public List<Future<?>> GET()
    {
        final List<Future<?>> list = new ArrayList<>();

        for (final WrapperHolder wrapper : getWrappers())
        {
            if (!wrapper.getDuration().isZero())
            {
                list.add(ThreadPoolManager.getInstance().schedule(wrapper.getTask(), wrapper.getDuration().toMillis()));
            }
            else
            {
                wrapper.getTask().run();
            }
        }

        return list;
    }

    /**
     * @param builder
     * @return {@code EventWrapperBuilder}
     */
    public EventWrapperBuilder COMBINE(final EventWrapperBuilder builder)
    {
        _wrappers.addAll(builder.getWrappers());
        return this;
    }

    /**
     * @return {@code List} of all {@code WrapperHolder}
     */
    public List<WrapperHolder> getWrappers()
    {
        return _wrappers;
    }

    //@formatter:off
    private static class WrapperHolder
    {
        private final Runnable getTask;
        private final Duration getDuration;

        public WrapperHolder(Runnable getTask, Duration getDuration)
        {
            this.getTask = getTask;
            this.getDuration = getDuration;
        }

        public Runnable getTask()
        {
            return this.getTask;
        }

        public Duration getDuration()
        {
            return this.getDuration;
        }
    }

    //@formatter:on

    public static EventWrapperBuilder of()
    {
        return new EventWrapperBuilder();
    }
}
