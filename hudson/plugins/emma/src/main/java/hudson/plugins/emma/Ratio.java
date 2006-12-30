package hudson.plugins.emma;

import java.io.IOException;
import java.io.Serializable;

/**
 * Represents <tt>x/y</tt> where x={@link #numerator} and y={@link #denominator}.
 * 
 * @author Kohsuke Kawaguchi
 */
final class Ratio implements Serializable {
    public final float numerator;
    public final float denominator;

    public Ratio(float numerator, float denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Gets "x/y" representation.
     */
    public String toString() {
        return numerator+"/"+denominator;
    }

    /**
     * Gets the percentage in integer.
     */
    public int getPercentage() {
        return Math.round(100*numerator/denominator);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ratio ratio = (Ratio) o;

        return Float.compare(ratio.denominator, denominator)==0
            && Float.compare(ratio.numerator, numerator)==0;

    }

    public int hashCode() {
        int result;
        result = numerator != +0.0f ? Float.floatToIntBits(numerator) : 0;
        result = 31 * result + denominator != +0.0f ? Float.floatToIntBits(denominator) : 0;
        return result;
    }

    /**
     * Parses the value attribute format of EMMA "52% (52/100)".
     */
    static Ratio parseValue(String v) throws IOException {
        // if only I could use java.util.Scanner...

        // only leave "a/b" in "N% (a/b)"
        int idx = v.indexOf('(');
        v = v.substring(idx+1,v.length()-1);

        idx = v.indexOf('/');

        return new Ratio(
           Float.parseFloat(v.substring(0,idx)),
           Float.parseFloat(v.substring(idx+1)));
    }

    private static final long serialVersionUID = 1L;
}
