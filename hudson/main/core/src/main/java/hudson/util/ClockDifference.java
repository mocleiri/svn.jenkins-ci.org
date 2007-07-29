package hudson.util;

import hudson.Util;
import hudson.model.Node;

import java.io.IOException;

/**
 * Represents a clock difference. Immutable.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ClockDifference {
    /**
     * The difference in milliseconds.
     *
     * Positive value means the slave is behind the master,
     * negative value means the slave is ahead of the master.
     */
    public final long diff;

    public ClockDifference(long value) {
        this.diff = value;
    }

    /**
     * Returns true if the difference is big enough to be considered dangerous.
     */
    public boolean isDangerous() {
        return Math.abs(diff)>5000;
    }

    /**
     * Gets the absolute value of {@link #diff}.
     */
    public long abs() {
        return Math.abs(diff);
    }

    /**
     * Gets the clock difference in HTML string.
     */
    public String toString() {
        if(-1000<diff && diff <1000)
            return "In sync";  // clock is in sync

        long abs = Math.abs(diff);

        String s = Util.getTimeSpanString(abs);
        if(diff<0)
            s += " ahead";
        else
            s += " behind";

        return s;
    }
    
    public String toHtml() {
        String s = toString();
        if(isDangerous())   s = "<span class=error>"+s+"</span>";
        return s;
    }

    public static String toHtml(Node d) {
        try {
            return d.getClockDifference().toHtml();
        } catch (IOException e) {
            return FAILED_HTML;
        } catch (InterruptedException e) {
            return FAILED_HTML;
        }
    }

    /**
     * Gets the clock difference in HTML string.
     * This version handles null {@link ClockDifference}.
     */
    public static String toHtml(ClockDifference d) {
        if(d==null)     return FAILED_HTML;
        return d.toHtml();
    }

    public static final ClockDifference ZERO = new ClockDifference(0);

    private static final String FAILED_HTML = "<span class='error'>Failed to heck</span>";
}
