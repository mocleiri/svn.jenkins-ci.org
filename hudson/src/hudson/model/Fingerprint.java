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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.Collections;
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

        public synchronized String toString() {
            StringBuffer buf = new StringBuffer();
            for (Range r : ranges) {
                if(buf.length()>0)  buf.append(',');
                buf.append(r);
            }
            return buf.toString();
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

    private final BuildPtr original;

    private final byte[] md5sum;

    private final String fileName;

    /**
     * Range of builds that use this file keyed by a job name.
     */
    private final Hashtable<String,RangeSet> usages = new Hashtable<String,RangeSet>();

    public Fingerprint(Run build, String fileName, byte[] md5sum) throws IOException {
        this.original = new BuildPtr(build);
        this.md5sum = md5sum;
        this.fileName = fileName;
        this.timestamp = new Date();
        save();
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
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        XmlFile f = getConfigFile(md5sum);
        f.mkdirs();
        f.write(this);
    }

    /**
     * The file we save our configuration.
     */
    private static XmlFile getConfigFile(byte[] md5sum) {
        assert md5sum.length==16;
        return new XmlFile(XSTREAM,
            new File( Hudson.getInstance().getRootDir(),
                "fingerprints/"+ Util.toHexString(md5sum,0,1)+'/'+Util.toHexString(md5sum,1,1)+'/'+Util.toHexString(md5sum,2,md5sum.length-2)+".xml"));
    }

    /**
     * Loads a {@link Fingerprint} from a file in the image.
     */
    /*package*/ static Fingerprint load(byte[] md5sum) throws IOException {
        XmlFile configFile = getConfigFile(md5sum);
        if(!configFile.exists())
            return null;
        try {
            return (Fingerprint)configFile.read();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load "+configFile,e);
            throw e;
        }
    }

    private static final XStream XSTREAM = new XStream();
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
