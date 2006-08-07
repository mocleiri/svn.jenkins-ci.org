package hudson.model;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import hudson.Util;
import hudson.XmlFile;
import hudson.util.HexBinaryConverter;
import hudson.util.XStream2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A file being tracked by Hudson.
 *
 * @author Kohsuke Kawaguchi
 */
public class Fingerprint implements ModelObject {
    /**
     * Pointer to a {@link Build}.
     */
    public static class BuildPtr {
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

        private boolean isAlive() {
            return getRun()!=null;
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

        public boolean isSmallerThan(int i) {
            return end<=i;
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

        /**
         * Returns true if two {@link Range}s can't be combined into a single range.
         */
        public boolean isIndependent(Range that) {
            return this.end<that.start ||that.end<this.start;
        }

        /**
         * Returns the {@link Range} that combines two ranges.
         */
        public Range combine(Range that) {
            assert !isIndependent(that);
            return new Range(
                Math.min(this.start,that.start),
                Math.max(this.end  ,that.end  ));
        }
    }

    /**
     * Set of {@link Range}s.
     */
    public static final class RangeSet {
        // sorted
        private final List<Range> ranges;

        public RangeSet() {
            this(new ArrayList<Range>());
        }

        private RangeSet(List<Range> data) {
            this.ranges = data;
        }

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

        public synchronized void add(RangeSet that) {
            int lhs=0,rhs=0;
            while(lhs<this.ranges.size() && rhs<that.ranges.size()) {
                Range lr = this.ranges.get(lhs);
                Range rr = that.ranges.get(rhs);

                // no overlap
                if(lr.end<rr.start) {
                    lhs++;
                    continue;
                }
                if(rr.end<lr.start) {
                    ranges.add(lhs,rr);
                    lhs++;
                    rhs++;
                    continue;
                }

                // overlap. merge two
                Range m = lr.combine(rr);
                rhs++;

                // since ranges[lhs] is explanded, it might overlap with others in this.ranges
                while(lhs+1<this.ranges.size() && !m.isIndependent(this.ranges.get(lhs+1))) {
                    m = m.combine(this.ranges.get(lhs+1));
                    this.ranges.remove(lhs+1);
                }

                this.ranges.set(lhs,m);
            }

            // if anything is left in that.ranges, add them all
            this.ranges.addAll(that.ranges.subList(rhs,that.ranges.size()));
        }

        public synchronized String toString() {
            StringBuffer buf = new StringBuffer();
            for (Range r : ranges) {
                if(buf.length()>0)  buf.append(',');
                buf.append(r);
            }
            return buf.toString();
        }

        public synchronized boolean isEmpty() {
            return ranges.isEmpty();
        }

        /**
         * Returns true if all the integers logically in this {@link RangeSet}
         * is smaller than the given integer. For example, {[1,3)} is smaller than 3,
         * but {[1,3),[100,105)} is not smaller than anything less than 105.
         *
         * Note that {} is smaller than any n.
         */
        public synchronized boolean isSmallerThan(int n) {
            if(ranges.isEmpty())    return true;

            return ranges.get(ranges.size() - 1).isSmallerThan(n);
        }

        static final class ConverterImpl implements Converter {
            private final Converter collectionConv; // used to convert ArrayList in it

            public ConverterImpl(Converter collectionConv) {
                this.collectionConv = collectionConv;
            }

            public boolean canConvert(Class type) {
                return type==RangeSet.class;
            }

            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                collectionConv.marshal( ((RangeSet)source).getRanges(), writer, context );
            }

            public Object unmarshal(HierarchicalStreamReader reader, final UnmarshallingContext context) {
                return new RangeSet((List<Range>)(collectionConv.unmarshal(reader,context)));
            }
        }
    }

    private final Date timestamp;

    /**
     * Null if this fingerprint is for a file that's
     * apparently produced outside.
     */
    private final BuildPtr original;

    private final byte[] md5sum;

    private final String fileName;

    /**
     * Range of builds that use this file keyed by a job name.
     */
    private final Hashtable<String,RangeSet> usages = new Hashtable<String,RangeSet>();

    public Fingerprint(Run build, String fileName, byte[] md5sum) throws IOException {
        this.original = build==null ? null : new BuildPtr(build);
        this.md5sum = md5sum;
        this.fileName = fileName;
        this.timestamp = new Date();
        save();
    }

    /**
     * The first build in which this file showed up,
     * if the file looked like it's created there.
     * <p>
     * This is considered as the "source" of this file,
     * or the owner, in the sense that this project "owns"
     * this file.
     */
    public BuildPtr getOriginal() {
        return original;
    }

    public String getDisplayName() {
        return fileName;
    }

    /**
     * The file name (like "foo.jar" without path).
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets the MD5 hash string.
     */
    public String getHashString() {
        return Util.toHexString(md5sum);
    }

    /**
     * Gets the timestamp when this record is created.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the string that says how long since this build has scheduled.
     *
     * @return
     *      string like "3 minutes" "1 day" etc.
     */
    public String getTimestampString() {
        long duration = System.currentTimeMillis()-timestamp.getTime();
        return Util.getTimeSpanString(duration);

    }

    /**
     * Gets the build range set for the given job name.
     *
     * <p>
     * These builds of this job has used this file.
     */
    public RangeSet getRangeSet(String jobName) {
        RangeSet r = usages.get(jobName);
        if(r==null) r = new RangeSet();
        return r;
    }

    public RangeSet getRangeSet(Job job) {
        return getRangeSet(job.getName());
    }

    /**
     * Gets the sorted list of job names where this jar is used.
     */
    public List<String> getJobs() {
        List<String> r = new ArrayList<String>();
        r.addAll(usages.keySet());
        Collections.sort(r);
        return r;
    }

    public Hashtable<String,RangeSet> getUsages() {
        return usages;
    }

    public synchronized void add(Build b) throws IOException {
        add(b.getParent().getName(),b.getNumber());
    }

    /**
     * Records that a build of a job has used this file.
     */
    public synchronized void add(String jobName, int n) throws IOException {
        synchronized(usages) {
            RangeSet r = usages.get(jobName);
            if(r==null) {
                r = new RangeSet();
                usages.put(jobName,r);
            }
            r.add(n);
        }
        save();
    }

    /**
     * Returns true if any of the builds recorded in this fingerprint
     * is still retained.
     *
     * <p>
     * This is used to find out old fingerprint records that can be removed
     * without losing too much information.
     */
    public synchronized boolean isAlive() {
        if(original.isAlive())
            return true;

        for (Entry<String,RangeSet> e : usages.entrySet()) {
            Job j = Hudson.getInstance().getJob(e.getKey());
            if(j==null)
                continue;

            int oldest = j.getFirstBuild().getNumber();
            if(!e.getValue().isSmallerThan(oldest))
                return true;
        }
        return false;
    }

    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        XmlFile f = getConfigFile(getFingerprintFile(md5sum));
        f.mkdirs();
        f.write(this);
    }

    /**
     * The file we save our configuration.
     */
    private static XmlFile getConfigFile(File file) {
        return new XmlFile(XSTREAM,file);
    }

    /**
     * Determines the file name from md5sum.
     */
    private static File getFingerprintFile(byte[] md5sum) {
        assert md5sum.length==16;
        return new File( Hudson.getInstance().getRootDir(),
            "fingerprints/"+ Util.toHexString(md5sum,0,1)+'/'+Util.toHexString(md5sum,1,1)+'/'+Util.toHexString(md5sum,2,md5sum.length-2)+".xml");
    }

    /**
     * Loads a {@link Fingerprint} from a file in the image.
     */
    /*package*/ static Fingerprint load(byte[] md5sum) throws IOException {
        return load(getFingerprintFile(md5sum));
    }
    /*package*/ static Fingerprint load(File file) throws IOException {
        XmlFile configFile = getConfigFile(file);
        if(!configFile.exists())
            return null;
        try {
            return (Fingerprint)configFile.read();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load "+configFile,e);
            throw e;
        }
    }

    private static final XStream XSTREAM = new XStream2();
    static {
        XSTREAM.alias("fingerprint",Fingerprint.class);
        XSTREAM.alias("range",Range.class);
        XSTREAM.alias("ranges",RangeSet.class);
        XSTREAM.registerConverter(new HexBinaryConverter(),10);
        XSTREAM.registerConverter(new RangeSet.ConverterImpl(
            new CollectionConverter(XSTREAM.getClassMapper()) {
                protected Object createCollection(Class type) {
                    return new ArrayList();
                }
            }
        ),10);
    }

    private static final Logger logger = Logger.getLogger(Fingerprint.class.getName());
}
