package hudson.plugins.analysis.core;

import static hudson.plugins.analysis.util.ThresholdValidator.*;

import org.apache.commons.lang.StringUtils;

/**
 * Data object that simply stores the thresholds.
 *
 * @author Ulli Hafner
 */
// CHECKSTYLE:OFF
@edu.umd.cs.findbugs.annotations.SuppressWarnings("")
@SuppressWarnings("all")
public class Thresholds {
    public String unstableTotalAll = StringUtils.EMPTY;
    public String unstableTotalHigh = StringUtils.EMPTY;
    public String unstableTotalNormal = StringUtils.EMPTY;
    public String unstableTotalLow = StringUtils.EMPTY;
    public String unstableNewAll = StringUtils.EMPTY;
    public String unstableNewHigh = StringUtils.EMPTY;
    public String unstableNewNormal = StringUtils.EMPTY;
    public String unstableNewLow = StringUtils.EMPTY;
    public String failedTotalAll = StringUtils.EMPTY;
    public String failedTotalHigh = StringUtils.EMPTY;
    public String failedTotalNormal = StringUtils.EMPTY;
    public String failedTotalLow = StringUtils.EMPTY;
    public String failedNewAll = StringUtils.EMPTY;
    public String failedNewHigh = StringUtils.EMPTY;
    public String failedNewNormal = StringUtils.EMPTY;
    public String failedNewLow = StringUtils.EMPTY;

    /**
     * Returns whether at least one of the thresholds is set.
     *
     * @return <code>true</code> if at least one of the thresholds is set,
     *         <code>false</code> if no threshold is set
     */
    public boolean isValid() {
        return isValid(unstableTotalAll)
        || isValid(unstableTotalHigh)
        || isValid(unstableTotalNormal)
        || isValid(unstableTotalLow)
        || isValid(unstableNewAll)
        || isValid(unstableNewHigh)
        || isValid(unstableNewNormal)
        || isValid(unstableNewLow)
        || isValid(failedTotalAll)
        || isValid(failedTotalHigh)
        || isValid(failedTotalNormal)
        || isValid(failedTotalLow)
        || isValid(failedNewAll)
        || isValid(failedNewHigh)
        || isValid(failedNewNormal)
        || isValid(failedNewLow);
    }

    /**
     * Returns whether the provided threshold string parameter is a valid
     * threshold number, i.e. an integer value greater or equal zero.
     *
     * @param threshold
     *        string representation of the threshold value
     * @return <code>true</code> if the provided threshold string parameter is a
     *         valid number >= 0
     */
    public static boolean isValid(final String threshold) {
        if (StringUtils.isNotBlank(threshold)) {
            try {
                return Integer.valueOf(threshold) >= 0;
            }
            catch (NumberFormatException exception) {
                // not valid
            }
        }
        return false;
    }

    /**
     * Returns a lower bound of warnings that will guarantee that a build
     * neither is unstable or failed.
     *
     * @return the number of warnings
     */
    public int getLowerBound() {
        if (isValid(unstableTotalAll)) {
            return convert(unstableTotalAll);
        }
        if (isValid(failedTotalAll)) {
            return convert(failedTotalAll);
        }
        return 0;
    }
}

