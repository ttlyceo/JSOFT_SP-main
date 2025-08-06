package l2e.gameserver.model.entity.events.tournaments.wrappers;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author psygrammator
 */
public abstract class AbstractRangeSchedule extends LoggerObject
{
    private final List<RangeSchedule> _schedule = new ArrayList<>();

    private final AtomicBoolean _isEnabled = new AtomicBoolean();

    private final AtomicReference<RangeSchedule> _reference = new AtomicReference<>();

    private Future<?> _task = null;
    private final SimpleDateFormat format = new SimpleDateFormat("E d/M HH:mm");

    public AbstractRangeSchedule(final String str)
    {
        Arrays.asList(str.replaceAll("\\s", "").split(";")).forEach(s -> TournamentUtil.addIf(_schedule, TournamentUtil.tryWithNoException(() -> RangeSchedule.of(LocalTime.of(Integer.parseInt(s.split("-")[0].split(":")[0]), Integer.parseInt(s.split("-")[0].split(":")[1])), LocalTime.of(Integer.parseInt(s.split("-")[1].split(":")[0]), Integer.parseInt(s.split("-")[1].split(":")[1]))), null), Objects::nonNull));
    }

    public String getNextTime()
    {
        if (getNextSchedule() != null) {
            return format.format(new Date(getNextSchedule().getFromInMillis(true)));
        }
        return "Error";
    }

    public String getNextTimeEnd()
    {
        if (getNextSchedule() != null) {
            return format.format(new Date(getNextSchedule().getToInMillis()));
        }
        return "Error";
    }

    public final void start()
    {
        stop();

        getReference().set(getNextSchedule());

        if (Objects.nonNull(getReference().get()))
        {
            if (getReference().get().isNow())
            {
                onStart();

                setEnabled(true);

                _task = ThreadPoolManager.getInstance().schedule(this::onAction, getReference().get().getToInMillis() - System.currentTimeMillis());
            }
            else
            {
                _task = ThreadPoolManager.getInstance().schedule(this::onAction, getReference().get().getFromInMillis(true) - System.currentTimeMillis());
            }

        }
    }

    public final void stop()
    {
        if (Objects.nonNull(_task))
        {
            _task.cancel(true);
            _task = null;
        }
    }

    public final void onAction()
    {
        if (isEnabled())
        {
            setEnabled(false);

            onFinish();

            start();
        }
        else
        {
            setEnabled(true);

            onStart();

            _task = ThreadPoolManager.getInstance().schedule(this::onAction, getReference().get().getToInMillis() - System.currentTimeMillis());
        }
    }

    /**
     * @return {@code RangeSchedule}
     */
    public RangeSchedule getNextSchedule()
    {
        RangeSchedule closest = null;

        for (final RangeSchedule schedule : getSchedules())
        {
            if (schedule.isNow())
            {
                return schedule;
            }
            else if (Objects.isNull(closest))
            {
                closest = schedule;
            }
            else if (schedule.isCloserThan(closest))
            {
                closest = schedule;
            }
        }

        return closest;
    }

    /**
     * @return {@code AtomicReference<RangeSchedule>}
     */
    public AtomicReference<RangeSchedule> getReference()
    {
        return _reference;
    }

    public boolean isEnabled()
    {
        return _isEnabled.get();
    }

    /**
     * @param val
     */
    public void setEnabled(final boolean val)
    {
        _isEnabled.set(val);
    }

    /**
     * @return collection of {@code RangeSchedule} of {@code AbstractRangeSchedule}
     */
    public Collection<RangeSchedule> getSchedules()
    {
        return _schedule;
    }

    public void reloadSchedule(String str)
    {
        _schedule.clear();
        Arrays.asList(str.replaceAll("\\s", "").split(";")).forEach(s -> TournamentUtil.addIf(_schedule, TournamentUtil.tryWithNoException(() -> RangeSchedule.of(LocalTime.of(Integer.parseInt(s.split("-")[0].split(":")[0]), Integer.parseInt(s.split("-")[0].split(":")[1])), LocalTime.of(Integer.parseInt(s.split("-")[1].split(":")[0]), Integer.parseInt(s.split("-")[1].split(":")[1]))), null), Objects::nonNull));
    }

    public abstract void onStart();

    public abstract void onFinish();

    //@formatter:off
    public static final class RangeSchedule
    {
        private final LocalTime getFrom;
        private final LocalTime getTo;

        public RangeSchedule(LocalTime getFrom, LocalTime getTo)
        {
            this.getFrom = getFrom;
            this.getTo = getTo;
        }

        public LocalTime getFrom()
        {
            return getFrom;
        }

        public LocalTime getTo()
        {
            return getTo;
        }

        private static final Duration DURATION = Duration.ofHours(24);

        public boolean isNow()
        {
            final long from = TournamentUtil.parseWeeklyDate(getFrom().toString(), false);
            final long to = TournamentUtil.ifThenApply(TournamentUtil.parseWeeklyDate(getTo().toString(), false), s -> s.longValue() < from, s -> s.longValue() + DURATION.toMillis());

            return from < System.currentTimeMillis() && to > System.currentTimeMillis();
        }

        public boolean isCloserThan(final RangeSchedule schedule)
        {
            return Math.abs(TournamentUtil.ifThenApply(getFromInMillis(false), s -> s.longValue() < System.currentTimeMillis(), s -> s.longValue() + DURATION.toMillis()) - System.currentTimeMillis()) < Math.abs(TournamentUtil.ifThenApply(schedule.getFromInMillis(false), s -> s.longValue() < System.currentTimeMillis(), s -> s.longValue() + DURATION.toMillis()) - System.currentTimeMillis());
        }

        public long getFromInMillis(final boolean compare)
        {
            return TournamentUtil.parseWeeklyDate(getFrom().toString(), compare);
        }

        public long getToInMillis()
        {
            return TournamentUtil.ifThenApply(TournamentUtil.parseWeeklyDate(getTo().toString(), true), s -> s.longValue() < getFromInMillis(false), s -> s.longValue() + DURATION.toMillis());
        }

        @Override
        public String toString()
        {
            return getFrom() + " - " + getTo();
        }

        public static RangeSchedule of(final LocalTime from, final LocalTime to)
        {
            return new RangeSchedule(from, to);
        }
    }
    //@formatter:on
}
