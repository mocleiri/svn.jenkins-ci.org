package hudson.scheduler;

import java.util.Date;

/**
 * Executes every day at the same time.
 * @author Kohsuke Kawaguchi
 */
public class DailyScheduler implements Scheduler {

    // to make the persisted XML more readable,
    // store values in hour and minute separately.

    private int hour;
    private int minute;

    public Date next() {
        Date now = new Date();
        long DAY_LENGTH = 1000*60*60*24;    // # of ms in a day

        long dayStart = (now.getTime()/DAY_LENGTH)*DAY_LENGTH;
        dayStart += (hour*60+minute)*60*1000;
        if(dayStart < now.getTime() ) {
            dayStart += DAY_LENGTH;
        }

        assert dayStart >= now.getTime();

        return new Date(dayStart);
    }
}
