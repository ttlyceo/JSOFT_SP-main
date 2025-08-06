package top.jsoft.jguard.model;

/**
 * @author Breeze
 * @date 05.02.2015
 */
public class TimedObject<T> {
    public final T value;
    public final long time;

    public TimedObject(T value, long time) {
        this.value = value;
        this.time = time;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimedObject<T> that = (TimedObject<T>) o;

        if (time != that.time) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }
}
