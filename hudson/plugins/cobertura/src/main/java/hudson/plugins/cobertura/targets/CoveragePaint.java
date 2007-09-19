package hudson.plugins.cobertura.targets;

import hudson.plugins.cobertura.Ratio;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

/**
 * TODO javadoc.
 *
 * @author Stephen Connolly
 * @since 29-Aug-2007 17:44:29
 */
public class CoveragePaint {
    private static int[] EMPTY_INT_ARRAY = {};
    private static Integer[] EMPTY_INTEGER_ARRAY = {};
    private static boolean[] EMPTY_BOOLEAN_ARRAY = {};
    private static int EXTRA_BUFFER_SIZE = 64;

    private final CoverageElement source;

    /**
     * has the line been covered.
     */
    private boolean[] painted = EMPTY_BOOLEAN_ARRAY;

    /**
     * is the line partially covered.
     */
    private int[] branchCount = EMPTY_INT_ARRAY;

    /**
     * how many times the line has been covered.
     */
    private int[] hitCount = EMPTY_INT_ARRAY;

    /**
     * the number of branches that are covered.
     */
    private int[] branchCoverage = EMPTY_INT_ARRAY;

    public CoveragePaint(CoverageElement source) {
        this.source = source;
    }

    private void ensureSize(int line) {
        if (painted.length <= line) {
            painted = Arrays.copyOf(painted, line + EXTRA_BUFFER_SIZE);
            hitCount = Arrays.copyOf(hitCount, line + EXTRA_BUFFER_SIZE);
            branchCount = Arrays.copyOf(branchCount, line + EXTRA_BUFFER_SIZE);
            branchCoverage = Arrays.copyOf(branchCoverage, line + EXTRA_BUFFER_SIZE);
        }
    }

    public void paint(int line, int hits) {
        ensureSize(line);
        if (painted[line]) {
            hitCount[line] += hits;
        } else {
            painted[line] = true;
            hitCount[line] = hits;
            branchCount[line] = 0;
            branchCoverage[line] = 0;
        }
    }

    public void paint(int line, int hits, int branchCover, int branchCount) {
        ensureSize(line);
        if (painted[line]) {
            hitCount[line] += hits;
            if (this.branchCount[line] == 0) {
                this.branchCount[line] = branchCount;
                branchCoverage[line] = branchCover;
            } else {
                // TODO find a better algorithm
                this.branchCount[line] = Math.max(this.branchCount[line], branchCount);
                branchCoverage[line] = Math.max(branchCoverage[line], branchCover);
            }
        } else {
            painted[line] = true;
            hitCount[line] = hits;
            this.branchCount[line] = branchCount;
            branchCoverage[line] = branchCover;
        }
    }

    public void add(CoveragePaint child) {
        ensureSize(child.painted.length);
        for (int i = 0; i < child.painted.length; i++) {
            if (child.painted[i]) {
                if (painted[i]) {
                    hitCount[i] += child.hitCount[i];
                    if ((branchCount[i] = Math.max(branchCount[i], child.branchCount[i])) != 0) {
                        branchCoverage[i] = Math.max(branchCoverage[i], child.branchCoverage[i]);
                    }
                } else {
                    painted[i] = child.painted[i];
                    hitCount[i] = child.hitCount[i];
                    branchCount[i] = child.branchCount[i];
                    branchCoverage[i] = child.branchCoverage[i];
                }
            }
        }
    }

    public Ratio getLineCoverage() {
        int painted = 0;
        int covered = 0;
        for (int i = 0; i < this.painted.length; i++) {
            if (this.painted[i]) {
                painted++;
                if (hitCount[i] > 0) {
                    covered++;
                }
            }
        }
        return Ratio.create(covered, painted);
    }

    public Ratio getConditionalCoverage() {
        long maxTotal = 0;
        long total = 0;
        for (int i = 0; i < this.branchCount.length; i++) {
            maxTotal += this.branchCount[i];
            total += this.branchCoverage[i];
        }
        return Ratio.create(total, maxTotal);
    }

    public Map<CoverageMetric, Ratio> getResults() {
        Map<CoverageMetric, Ratio> result = new HashMap<CoverageMetric, Ratio>();
        result.put(CoverageMetric.LINE, getLineCoverage());
        result.put(CoverageMetric.CONDITIONAL, getConditionalCoverage());
        return result;
    }

    public boolean isPainted(int line) {
        if (line > 0 && line < painted.length) {
            return painted[line];
        }
        return false;
    }

    public int getHits(int line) {
        if (line > 0 && line < painted.length) {
            return hitCount[line];
        }
        return 0;
    }

    public int getBranchTotal(int line) {
        if (line > 0 && line < painted.length) {
            return branchCount[line];
        }
        return 0;
    }

    public int getBranchCoverage(int line) {
        if (line > 0 && line < painted.length) {
            return branchCoverage[line];
        }
        return 0;
    }

}
