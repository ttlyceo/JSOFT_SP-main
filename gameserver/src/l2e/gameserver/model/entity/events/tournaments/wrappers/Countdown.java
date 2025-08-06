package l2e.gameserver.model.entity.events.tournaments.wrappers;

import l2e.gameserver.ThreadPoolManager;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author psygrammator
 */
public class Countdown implements Runnable
{
    private final AtomicInteger _countdown;

    private Future<?> _task;

    public Countdown(final int time)
    {
        _countdown = new AtomicInteger(time);
    }

    @Override
    public void run()
    {
        onTick();

        if (getTime() < 1)
        {
            stop();
            onZero();
        }
        else
        {
            _countdown.decrementAndGet();
        }
    }

    /**
     * Starts the ticking of {@code Countdown}
     */
    public final void start()
    {
        if (Objects.isNull(_task))
        {
            onStart();

            _task = ThreadPoolManager.getInstance().scheduleAtFixedRate(this, 1, 1000);
        }
    }

    /**
     * Stops the ticking of {@code Countdown}
     */
    public final void stop()
    {
        if (_task != null)
        {
            _task.cancel(true);
            _task = null;
        }
    }

    /**
     * Adds t to {@code Countdown}
     * @param time - Value in second
     */
    public void addTime(final int time)
    {
        _countdown.addAndGet(time);
    }

    /**
     * Set the second of {@code Countdown} to given time
     * @param time - Value in second
     */
    public void setTime(final int time)
    {
        _countdown.set(time);
    }

    /**
     * @return remaining second of {@code Countdown}
     */
    public int getTime()
    {
        return _countdown.get();
    }

    public boolean isActive()
    {
        return _task != null;
    }

    /**
     * @return remaining second of {@code Countdown} in {@code String} format
     */
    public String getTimeInString()
    {
        final String min = "" + getTime() / 60;
        final String second = getTime() % 60 < 10 ? "0" + getTime() % 60 : "" + getTime() % 60;

        return min + ":" + second;
    }

    public void onStart()
    {

    }

    public void onTick()
    {

    }

    public void onZero()
    {

    }

    @Override
    public String toString()
    {
        return "Remaining: " + getTimeInString();
    }
}
