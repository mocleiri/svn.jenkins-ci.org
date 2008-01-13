package hudson.maven.reporters;

import hudson.FilePath;
import hudson.maven.MavenBuild;
import hudson.maven.MavenBuildProxy;
import hudson.maven.MavenBuildProxy.BuildCallable;
import hudson.maven.MavenModule;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.maven.MojoInfo;
import hudson.model.BuildListener;
import hudson.model.FingerprintMap;
import hudson.model.Hudson;
import hudson.tasks.Fingerprinter.FingerprintAction;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Records fingerprints of the builds to keep track of dependencies.
 *
 * @author Kohsuke Kawaguchi
 */
public class MavenFingerprinter extends MavenReporter {

    /**
     * Files whose fingerprints were already recorded.
     */
    private transient Set<File> files;
    /**
     * Fingerprints for files that were used.
     */
    private transient Map<String,String> used;
    /**
     * Fingerprints for files that were produced.
     */
    private transient Map<String,String> produced;

    public boolean preBuild(MavenBuildProxy build, MavenProject pom, BuildListener listener) throws InterruptedException, IOException {
        files = new HashSet<File>();
        used = new HashMap<String,String>();
        produced = new HashMap<String,String>();
        return true;
    }

    /**
     * Mojos perform different dependency resolution, so we need to check this for each mojo.
     */
    public boolean postExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener, Throwable error) throws InterruptedException, IOException {
        record(pom.getArtifacts(),used);
        record(pom.getArtifact(),produced);
        record(pom.getAttachedArtifacts(),produced);

        return true;
    }

    /**
     * Sends the collected fingerprints over to the master and record them.
     */
    public boolean postBuild(MavenBuildProxy build, MavenProject pom, BuildListener listener) throws InterruptedException, IOException {
        build.execute(new BuildCallable<Void,IOException>() {
            // record is transient, so needs to make a copy first
            private final Map<String,String> u = used;
            private final Map<String,String> p = produced;

            public Void call(MavenBuild build) throws IOException, InterruptedException {
                FingerprintMap map = Hudson.getInstance().getFingerprintMap();

                for (Entry<String, String> e : p.entrySet())
                    map.getOrCreate(build, e.getKey(), e.getValue()).add(build);
                for (Entry<String, String> e : u.entrySet())
                    map.getOrCreate(null, e.getKey(), e.getValue()).add(build);

                Map<String,String> all = new HashMap<String, String>(u);
                all.putAll(p);

                // add action
                build.getActions().add(new FingerprintAction(build,all));
                return null;
            }
        });
        return true;
    }

    private void record(Collection<Artifact> artifacts, Map<String,String> record) throws IOException, InterruptedException {
        for (Artifact a : artifacts)
            record(a,record);
    }

    /**
     * Records the fingerprint of the given {@link Artifact}.
     *
     * <p>
     * This method contains the logic to avoid doubly recording the fingerprint
     * of the same file.
     */
    private void record(Artifact a, Map<String,String> record) throws IOException, InterruptedException {
        File f = a.getFile();
        if(files==null)
            throw new InternalError();
        if(f==null || !f.exists() || f.isDirectory() || !files.add(f))
            return;

        // new file
        String name = a.getGroupId()+':'+f.getName();
        String digest = new FilePath(f).digest();
        record.put(name,digest);
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }

    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        private DescriptorImpl() {
            super(MavenFingerprinter.class);
        }

        public String getDisplayName() {
            return "Record fingerprints";
        }

        public MavenReporter newAutoInstance(MavenModule module) {
            return new MavenFingerprinter();
        }
    }

    private static final long serialVersionUID = 1L;
}
