package hudson.scheduler;

import antlr.ANTLRException;
import antlr.RecognitionException;

import java.io.StringReader;
import java.text.ParseException;
import java.util.Calendar;
import java.util.StringTokenizer;

/**
 * Table for driving scheduled tasks.
 *
 * @author Kohsuke Kawaguchi
 */
public final class CronTab {
    /**
     * bits[0]: minutes
     * bits[1]: hours
     * bits[2]: days
     * bits[3]: months
     *
     * false:not scheduled &lt;-> true scheduled
     */
    final long[] bits = new long[4];

    int dayOfWeek;

    /**
     * Job to be executed when the time comes.
     */
    private final Runnable task;

    public CronTab(Runnable task, String format) throws ParseException {
        this.task = task;

        CrontabParser parser = new CrontabParser(new CrontabLexer(new StringReader(format)));
        try {
            parser.startRule(this);
        } catch (RecognitionException e) {
            ParseException pe = new ParseException(e.getMessage(), e.getColumn());
            pe.initCause(e);
            throw pe;
        } catch (ANTLRException e) {
            ParseException pe = new ParseException(e.getMessage(), -1);
            pe.initCause(e);
            throw pe;
        }
    }


    /**
     * Checks for the run and run it.
     */
    public void check(Calendar cal) {
        if(!checkBits(bits[0],cal.get(Calendar.MINUTE)))
            return;
        if(!checkBits(bits[1],cal.get(Calendar.HOUR)))
            return;
        if(!checkBits(bits[2],cal.get(Calendar.DAY_OF_MONTH)))
            return;
        if(!checkBits(bits[3],cal.get(Calendar.MONTH)+1))
            return;
        if(!checkBits(dayOfWeek,cal.get(Calendar.DAY_OF_WEEK)-1))
            return;

        // execute
        task.run();
    }

    /**
     * Returns true if n-th bit is on.
     */
    private boolean checkBits(long bitMask, int n) {
        return (bitMask|(1<<n))!=bitMask;
    }

    public String toString() {
        return super.toString()+"["+
            toString("minute",bits[0])+','+
            toString("hour",bits[1])+','+
            toString("dayOfMonth",bits[2])+','+
            toString("month",bits[3])+','+
            toString("dayOfWeek",dayOfWeek)+']';
    }

    private String toString(String key, long bit) {
        return key+'='+Long.toHexString(bit);
    }
}
