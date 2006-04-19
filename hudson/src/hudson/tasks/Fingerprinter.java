package hudson.tasks;

import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Project;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Fingerprint;
import hudson.Launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.DirectoryScanner;

import javax.servlet.http.HttpServletRequest;

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


        byte[] buf = new byte[8192];

        DirectoryScanner ds = src.getDirectoryScanner(new org.apache.tools.ant.Project());
        for( String f : ds.getIncludedFiles() ) {
            File file = new File(baseDir,f);

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
                    build, file.getName(), md5.digest());
                if(fp==null) {
                    listener.error("failed to record fingerprint for "+file);
                    continue;
                }
                fp.add(build);
            } catch (IOException e) {
                e.printStackTrace(listener.error("Failed to compute digest for "+file));
            }
        }

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
}
