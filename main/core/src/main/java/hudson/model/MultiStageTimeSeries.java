package hudson.model;

import hudson.util.TimeUnit2;

/**
 * Maintains several {@link TimeSeries} with different update frequencies to satisfy three goals;
 * (1) retain data over long timespan, (2) save memory, and (3) retain accurate data for the recent past.
 *
 * All in all, one instance uses about 8KB space.
 *
 * @author Kohsuke Kawaguchi
 */
public class MultiStageTimeSeries {
    /**
     * Updated every 10 seconds. Keep data up to 1 hour.
     */
    public final TimeSeries sec10;
    /**
     * Updated every 1 min. Keep data up to 1 day.
     */
    public final TimeSeries min;
    /**
     * Updated every 1 hour. Keep data up to 4 weeks.
     */
    public final TimeSeries hour;

    private int counter;

    public MultiStageTimeSeries(float initialValue, float decay) {
        this.sec10 = new TimeSeries(initialValue,decay,6*60);
        this.min = new TimeSeries(initialValue,decay,60*24);
        this.hour = new TimeSeries(initialValue,decay,28*24);
    }

    /**
     * Call this method every 10 sec and supply a new data point.
     */
    public void update(float f) {
        counter = (counter+1)%360;   // 1hour/10sec = 60mins/10sec=3600secs/10sec = 360
        sec10.update(f);
        if(counter%6==0)    min.update(f);
        if(counter==0)      hour.update(f);
    }

    /**
     * Selects a {@link TimeSeries}.
     */
    public TimeSeries pick(TimeScale timeScale) {
        switch (timeScale) {
        case HOUR:  return hour;
        case MIN:   return min;
        case SEC10: return sec10;
        default:    throw new AssertionError();
        }
    }

    /**
     * Gets the most up-to-date data point value.
     */
    public float getLatest(TimeScale timeScale) {
        return pick(timeScale).getLatest();
    }

    /**
     * Choose which datapoint to use.
     */
    public enum TimeScale {
        SEC10(TimeUnit2.SECONDS.toMillis(10)),
        MIN(TimeUnit2.MINUTES.toMillis(1)),
        HOUR(TimeUnit2.HOURS.toMillis(1));

        /**
         * Number of milliseconds (10 secs, 1 min, and 1 hour)
         * that this constant represents.
         */
        public final long tick;

        TimeScale(long tick) {
            this.tick = tick;
        }
    }
}
