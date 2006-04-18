package hudson.model;

import hudson.XmlFile;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.io.IOException;
import java.io.File;

import com.thoughtworks.xstream.XStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class Fingerprint {
    /**
     * Pointer to a {@link Build}.
     */
    private static class BuildPtr {
        final String name;
        final int number;

        public BuildPtr(String name, int number) {
            this.name = name;
            this.number = number;
        }

        public BuildPtr(Run run) {
            this( run.getParent().getName(), run.getNumber() );
        }

        /**
         * Gets the name of the job.
         * <p>
         * Such job could be since then removed,
         * so there might not be a corresponding
         * {@link Job}.
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the {@link Job} that this pointer points to,
         * or null if such a job no longer exists.
         */
        public Job getJob() {
            return Hudson.getInstance().getJob(name);
        }

        /**
         * Gets the project build number.
         * <p>
         * Such {@link Run} could be since then
         * discarded.
         */
        public int getNumber() {
            return number;
        }

        /**
         * Gets the {@link Job} that this pointer points to,
         * or null if such a job no longer exists.
         */
        public Run getRun() {
            Job j = getJob();
            if(j==null)     return null;
            return j.getBuildByNumber(number);
        }
    }

    /**
     * Range of build numbers [start,end). Immutable.
     */
    public static final class Range {
        final int start;
        final int end;

        public Range(int start, int end) {
            assert start<end;
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public boolean isBiggerThan(int i) {
            return i<start;
        }

        public boolean includes(int i) {
            return start<=i && i<end;
        }

        public Range expandRight() {
            return new Range(start,end+1);
        }

        public Range expandLeft() {
            return new Range(start-1,end);
        }

        public boolean isAdjacentTo(Range that) {
            return this.end==that.start;
        }

        public String toString() {
            return "["+start+","+end+")";
        }
    }

    /**
     * Set of {@link Range}s.
     */
    public static final class RangeSet {
        // sorted
        private final List<Range> ranges = new ArrayList<Range>();

        /**
         * Gets all the ranges.
         */
        public synchronized List<Range> getRanges() {
            return new ArrayList<Range>(ranges);
        }

        /**
         * Expands the range set to include the given value.
         * If the set already includes this number, this will be a no-op.
         */
        public synchronized void add(int n) {
            for( int i=0; i<ranges.size(); i++ ) {
                Range r = ranges.get(i);
                if(r.includes(n))   return; // already included
                if(r.end==n) {
                    ranges.set(i,r.expandRight());
                    checkCollapse(i);
                    return;
                }
                if(r.start==n+1) {
                    ranges.set(i,r.expandLeft());
                    checkCollapse(i-1);
                    return;
                }
                if(r.isBiggerThan(n)) {
                    // needs to insert a single-value Range
                    ranges.add(i,new Range(n,n+1));
                    return;
                }
            }

            ranges.add(new Range(n,n+1));
        }

        private void checkCollapse(int i) {
            if(i<0 || i==ranges.size()-1)     return;
            Range lhs = ranges.get(i);
            Range rhs = ranges.get(i+1);
            if(lhs.isAdjacentTo(rhs)) {
                // collapsed
                Range r = new Range(lhs.start,rhs.end);
                ranges.set(i,r);
                ranges.remove(i+1);
            }
        }

        public synchronized boolean includes(int i) {
            for (Range r : ranges) {
                if(r.includes(i))
                    return true;
            }
            return false;
        }

        public synchronized String toString() {
            StringBuffer buf = new StringBuffer();
            for (Range r : ranges) {
                if(buf.length()>0)  buf.append(',');
                buf.append(r);
            }
            return buf.toString();
        }
    }

    private final BuildPtr original;

    private final byte[] md5sum;

    /**
     * Range of builds that use this file keyed by a job name.
     */
    private final Hashtable<String,RangeSet> ranges = new Hashtable<String,RangeSet>();

    public Fingerprint(Run build, byte[] md5sum) {
        this.original = new BuildPtr(build);
        this.md5sum = md5sum;
    }

    /**
     * The first build in which this file showed up.
     * <p>
     * This is considered as the "source" of this file,
     * or the owner, in the sense that this project "owns"
     * this file.
     */
    public BuildPtr getOriginal() {
        return original;
    }

    /**
     * Gets the build range set for the given job name.
     *
     * <p>
     * These builds of this job has used this file.
     */
    public RangeSet getRangeSet(String jobName) {
        RangeSet r = ranges.get(jobName);
        if(r==null) r = new RangeSet();
        return r;
    }

    /**
     * Records that a build of a job has used this file.
     */
    public synchronized void add(String jobName, int n) throws IOException {
        synchronized(ranges) {
            RangeSet r = ranges.get(jobName);
            if(r==null) {
                r = new RangeSet();
                ranges.put(jobName,r);
            }
            r.add(n);
        }
        save();
    }

    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        getConfigFile(md5sum).write(this);
    }

    /**
     * The file we save our configuration.
     */
    public static XmlFile getConfigFile(byte[] md5sum) {
        assert md5sum.length==16;
        XStream xs = new XStream();
        xs.alias("fingerprint",Fingerprint.class);
        return new XmlFile(xs,
            new File( Hudson.getInstance().getRootDir(),
                "fingerprints/"+toHexString(md5sum,0,2)+'/'+toHexString(md5sum,2,2)+'/'+toHexString(md5sum,4,md5sum.length-4)));
    }

    private static String toHexString(byte[] data, int start, int len) {
        StringBuffer buf = new StringBuffer();
        for( int i=0; i<len; i++ ) {
            byte b = data[start+i];
            if(b<16)    buf.append('0');
            buf.append(Integer.toHexString(b));
        }
        return buf.toString();
    }
}
