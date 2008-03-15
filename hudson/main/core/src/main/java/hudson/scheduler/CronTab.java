package hudson.scheduler;

import antlr.ANTLRException;

import java.io.StringReader;
import java.util.Calendar;

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
     * Textual representation.
     */
    private String spec;

    public CronTab(String format) throws ANTLRException {
        this(format,1);
    }

    public CronTab(String format, int line) throws ANTLRException {
        set(format, line);
    }

    private void set(String format, int line) throws ANTLRException {
        CrontabLexer lexer = new CrontabLexer(new StringReader(format));
        lexer.setLine(line);
        CrontabParser parser = new CrontabParser(lexer);
        spec = format;

        parser.startRule(this);
        if((dayOfWeek&(1<<7))!=0)
            dayOfWeek |= 1; // copy bit 7 over to bit 0
    }


    /**
     * Returns true if the given calendar matches
     */
    boolean check(Calendar cal) {
        if(!checkBits(bits[0],cal.get(Calendar.MINUTE)))
            return false;
        if(!checkBits(bits[1],cal.get(Calendar.HOUR_OF_DAY)))
            return false;
        if(!checkBits(bits[2],cal.get(Calendar.DAY_OF_MONTH)))
            return false;
        if(!checkBits(bits[3],cal.get(Calendar.MONTH)+1))
            return false;
        if(!checkBits(dayOfWeek,cal.get(Calendar.DAY_OF_WEEK)-1))
            return false;

        return true;
    }

    void set(String format) throws ANTLRException {
        set(format,1);
    }

    /**
     * Returns true if n-th bit is on.
     */
    private boolean checkBits(long bitMask, int n) {
        return (bitMask|(1L<<n))==bitMask;
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

    /**
     * Checks if this crontab entry looks reasonable,
     * and if not, return an warning message.
     *
     * <p>
     * The point of this method is to catch syntactically correct
     * but semantically suspicious combinations, like
     * "* 0 * * *"
     */
    public String checkSanity() {
        for( int i=0; i<5; i++ ) {
            for( int j=LOWER_BOUNDS[i]; j<=UPPER_BOUNDS[i]; j++ ) {
                if(!checkBits(bits[i],j)) {
                    // this rank has a sparse entry.
                    // if we have a sparse rank, one of them better be the left-most.
                    if(i>0)
                        return "Do you really mean \"every minute\" when you say \""+spec+"\"? "+
                                "Perhaps you meant \"0 "+spec.substring(spec.indexOf(' ')+1)+"\"";
                    // once we find a sparse rank, upper ranks don't matter
                    return null;
                }
            }
        }

        return null;
    }

    // lower/uppser bounds of fields
    private static final int[] LOWER_BOUNDS = new int[] {0,0,1,0,0};
    private static final int[] UPPER_BOUNDS = new int[] {59,23,31,12,7};
}
