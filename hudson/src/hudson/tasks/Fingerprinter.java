package hudson.tasks;

import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Fingerprint;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.Result;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Records fingerprints of the specified files.
 *
 * @author Kohsuke Kawaguchi
 */
public class Fingerprinter implements BuildStep {

    /**
     * Comma-separated list of files/directories to be fingerprinted.
     */
    private final String targets;

    public Fingerprinter(String targets) {
        this.targets = targets;
    }

    public String getTargets() {
        return targets;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println("Recording fingerprints");

        Project p = build.getProject();

        FileSet src = new FileSet();
        File baseDir = p.getWorkspace().getLocal();
        src.setDir(baseDir);
        src.setIncludes(targets);

        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // I don't think this is possible, but check anyway
            e.printStackTrace(listener.error("MD5 not installed"));
            build.setResult(Result.FAILURE);
            return true;
        }

        Map<String,String> record = new HashMap<String,String>();

        byte[] buf = new byte[8192];

        DirectoryScanner ds = src.getDirectoryScanner(new org.apache.tools.ant.Project());
        for( String f : ds.getIncludedFiles() ) {
            File file = new File(baseDir,f);

            // consider the file to be produced by this build only if the timestamp
            // is newer than when the build has started.
            boolean produced = build.getTimestamp().getTimeInMillis() <= file.lastModified();

            try {
                md5.reset();    // technically not necessary, but hey, just to be safe
                DigestInputStream in =new DigestInputStream(new FileInputStream(file),md5);
                try {
                    while(in.read(buf)>0)
                        ; // simply discard the input
                } finally {
                    in.close();
                }

                Fingerprint fp = Hudson.getInstance().getFingerprintMap().getOrCreate(
                    produced?build:null, file.getName(), md5.digest());
                if(fp==null) {
                    listener.error("failed to record fingerprint for "+file);
                    continue;
                }
                fp.add(build);
                record.put(f,fp.getHashString());
            } catch (IOException e) {
                e.printStackTrace(listener.error("Failed to compute digest for "+file));
            }
        }

        build.getActions().add(new FingerprintAction(build,record));

        return true;
    }

    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }


    public static final BuildStepDescriptor DESCRIPTOR = new BuildStepDescriptor(Fingerprinter.class) {
        public String getDisplayName() {
            return "Record fingerprints of files to track usage";
        }

        public String getHelpFile() {
            return "/help/project-config/fingerprint.html";
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new Fingerprinter(req.getParameter("fingerprint_targets"));
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

        public synchronized Map<String,Fingerprint> getFingerprints() {
            if(ref!=null) {
                Map<String,Fingerprint> m = ref.get();
                if(m!=null)
                    return m;
            }

            Hudson h = Hudson.getInstance();

            Map<String,Fingerprint> m = new HashMap<String,Fingerprint>();
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
    }

    private static final Logger logger = Logger.getLogger(Fingerprinter.class.getName());
}
