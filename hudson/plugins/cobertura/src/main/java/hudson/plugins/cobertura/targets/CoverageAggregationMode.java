package hudson.plugins.cobertura.targets;

import hudson.plugins.cobertura.Ratio;

/**
 * Different ways of aggregating data series {x_1,x_2,x_3,...}, which can be represented as
 * <tt>f(...f(f(ZERO,x_1),x_2)...,x_n)</tt>
 *
 * @author Stephen Connolly
 * @since 22-Aug-2007 18:07:35
 */
public enum CoverageAggregationMode {
    /**
     * Aggregation that doesn't produce any value at all.
     */
    NONE(null) {
        public Ratio aggregate(Ratio a, Ratio b) {
            return null;
        }
    },

    /**
     * Adds up numerator and denominator separately.
     *
     * Say if you want to count the ratio of male among the population in a state from
     * a series of those ratios per county, this is how you add them up. 
     */
    SUM(Ratio.create(0,0)) {
        public Ratio aggregate(Ratio a, Ratio b) {
            return Ratio.create(a.numerator + b.numerator, a.denominator + b.denominator);
        }
    },

    /**
     * x_1 * x_2 * x_3 + ...
     */
    PRODUCT(Ratio.create(1,1)) {
        public Ratio aggregate(Ratio a, Ratio b) {
            return Ratio.create(a.numerator * b.numerator, a.denominator * b.denominator);
        }
    },
    /**
     * Count number of data that is non-zero.
     */
    COUNT_NON_ZERO(Ratio.create(0,0)) {
        public Ratio aggregate(Ratio a, Ratio b) {
            if (Math.abs(b.denominator) < 1e-7)
                return a;       // 0/0 doesn't contribute to the math in this method.
            return Ratio.create(a.numerator + (Math.abs(b.numerator) > 1e-7 ? 1:0),    a.denominator + 1);
        }};

    /**
     * Initial value of this aggregation mode, which is the output of the aggregation when
     * the data series is empty.
     */
    public final Ratio ZERO;

    CoverageAggregationMode(Ratio ZERO) {
        this.ZERO = ZERO;
    }

    /**
     * Combinator function. Note that this function is defined to be left-associative and f(x,y) isn't necessarily
     * the same as f(y,x)
     */
    public abstract Ratio aggregate(Ratio a, Ratio b);
}
