package hudson.tasks;

import hudson.Launcher;
import hudson.remoting.VirtualChannel;
import hudson.util.IOException2;
import hudson.FilePath.FileCallable;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Fingerprint;
import hudson.model.Fingerprint.BuildPtr;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.FingerprintMap;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Records fingerprints of the specified files.
 *
 * @author Kohsuke Kawaguchi
 */
public class Fingerprinter extends Publisher {

    /**
     * Comma-separated list of files/directories to be fingerprinted.
     */
    private final String targets;

    /**
     * Also record all the finger prints of the build artifacts.
     */
    private final boolean recordBuildArtifacts;

    public Fingerprinter(String targets, boolean recordBuildArtifacts) {
        this.targets = targets;
        this.recordBuildArtifacts = recordBuildArtifacts;
    }

    public String getTargets() {
        return targets;
    }

    public boolean getRecordBuildArtifacts() {
        return recordBuildArtifacts;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) throws InterruptedException {
        try {
            listener.getLogger().println("Recording fingerprints");

            Map<String,String> record = new HashMap<String,String>();


            if(targets.length()!=0)
            record(build, listener, record, targets);

            if(recordBuildArtifacts) {
                ArtifactArchiver aa = (ArtifactArchiver) build.getProject().getPublishers().get(ArtifactArchiver.DESCRIPTOR);
                if(aa==null) {
                    // configuration error
                    listener.error("Build artifacts are supposed to be fingerprinted, but build artifact archiving is not configured");
                    build.setResult(Result.FAILURE);
                    return true;
                }
                record(build, listener, record, aa.getArtifacts() );
            }

            build.getActions().add(new FingerprintAction(build,record));

        } catch (IOException e) {
            e.printStackTrace(listener.error("Failed to record fingerprints"));
            build.setResult(Result.FAILURE);
        }

        // failing to record fingerprints is an error but not fatal
        return true;
    }

    private void record(Build build, BuildListener listener, Map<String,String> record, final String targets) throws IOException, InterruptedException {
        final class Record implements Serializable {
            final boolean produced;
            final String relativePath;
            final String fileName;
            final byte[] md5sum;

            public Record(boolean produced, String relativePath, String fileName, byte[] md5sum) {
                this.produced = produced;
                this.relativePath = relativePath;
                this.fileName = fileName;
                this.md5sum = md5sum;
            }
            
            Fingerprint addRecord(Build build) throws IOException {
                FingerprintMap map = Hudson.getInstance().getFingerprintMap();
                return map.getOrCreate(produced?build:null, fileName, md5sum);
            }

            private static final long serialVersionUID = 1L;
        }

        Project p = build.getProject();
        final long buildTimestamp = build.getTimestamp().getTimeInMillis();

        List<Record> records = p.getWorkspace().act(new FileCallable<List<Record>>() {
            public List<Record> invoke(File baseDir, VirtualChannel channel) throws IOException {
                List<Record> results = new ArrayList<Record>();

                FileSet src = new FileSet();
                src.setDir(baseDir);
                src.setIncludes(targets);

                byte[] buf = new byte[8192];
                MessageDigest md5 = createMD5();

                DirectoryScanner ds = src.getDirectoryScanner(new org.apache.tools.ant.Project());
                for( String f : ds.getIncludedFiles() ) {
                    File file = new File(baseDir,f);

                    // consider the file to be produced by this build only if the timestamp
                    // is newer than when the build has started.
                    boolean produced = buildTimestamp <= file.lastModified();

                    try {
                        md5.reset();    // technically not necessary, but hey, just to be safe
                        DigestInputStream in =new DigestInputStream(new FileInputStream(file),md5);
                        try {
                            while(in.read(buf)>0)
                                ; // simply discard the input
                        } finally {
                            in.close();
                        }

                        results.add(new Record(produced,f,file.getName(),md5.digest()));
                    } catch (IOException e) {
                        throw new IOException2("Failed to compute digest for "+file,e);
                    }
                }

                return results;
            }
        });

        for (Record r : records) {
            Fingerprint fp = r.addRecord(build);
            if(fp==null) {
                listener.error("failed to record fingerprint for "+r.relativePath);
                continue;
            }
            fp.add(build);
            record.put(r.relativePath,fp.getHashString());
        }
    }

    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    private static MessageDigest createMD5() throws IOException2 {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // I don't think this is possible, but check anyway
            throw new IOException2("MD5 not installed",e);
        }
    }


    public static final Descriptor<Publisher> DESCRIPTOR = new Descriptor<Publisher>(Fingerprinter.class) {
        public String getDisplayName() {
            return "Record fingerprints of files to track usage";
        }

        public String getHelpFile() {
            return "/help/project-config/fingerprint.html";
        }

        public Publisher newInstance(StaplerRequest req) {
            return new Fingerprinter(
                req.getParameter("fingerprint_targets").trim(),
                req.getParameter("fingerprint_artifacts")!=null);
        }
    };


    /**
     * Action for displaying fingerprints.
     */
    public static final class FingerprintAction implements Action {
        private final Build build;

        private final Map<String,String> record;

        private transient WeakReference<Map<String,Fingerprint>> ref;

        public FingerprintAction(Build build, Map<String, String> record) {
            this.build = build;
            this.record = record;
        }

        public String getIconFileName() {
            return "fingerprint.gif";
        }

        public String getDisplayName() {
            return "See fingerprints";
        }

        public String getUrlName() {
            return "fingerprints";
        }

        public Build getBuild() {
            return build;
        }

        /**
         * Map from file names of the fingerprinted file to its fingerprint record.
         */
        public synchronized Map<String,Fingerprint> getFingerprints() {
            if(ref!=null) {
                Map<String,Fingerprint> m = ref.get();
                if(m!=null)
                    return m;
            }

            Hudson h = Hudson.getInstance();

            Map<String,Fingerprint> m = new TreeMap<String,Fingerprint>();
            for (Entry<String, String> r : record.entrySet()) {
                try {
                    m.put(r.getKey(), h._getFingerprint(r.getValue()) );
                } catch (IOException e) {
                    logger.log(Level.WARNING,e.getMessage(),e);
                }
            }

            m = Collections.unmodifiableMap(m);
            ref = new WeakReference<Map<String,Fingerprint>>(m);
            return m;
        }

        /**
         * Gets the dependency to other builds in a map.
         * Returns build numbers instead of {@link Build}, since log records may be gone.
         */
        public Map<Project,Integer> getDependencies() {
            Map<Project,Integer> r = new HashMap<Project,Integer>();

            for (Fingerprint fp : getFingerprints().values()) {
                BuildPtr bp = fp.getOriginal();
                if(bp==null)    continue;       // outside Hudson
                if(bp.is(build))    continue;   // we are the owner

                Integer existing = r.get(bp.getJob());
                if(existing!=null && existing>bp.getNumber())
                    continue;   // the record in the map is already up to date
                r.put((Project)bp.getJob(),bp.getNumber());
            }
            
            return r;
        }
    }

    private static final Logger logger = Logger.getLogger(Fingerprinter.class.getName());
}
