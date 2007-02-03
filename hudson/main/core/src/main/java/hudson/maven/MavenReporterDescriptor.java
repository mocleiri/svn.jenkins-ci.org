package hudson.maven;

import hudson.maven.reporters.MavenArtifactArchiver;
import hudson.model.Descriptor;
import org.apache.commons.jelly.JellyException;
import org.kohsuke.stapler.MetaClass;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.jelly.JellyClassTearOff;

/**
 * {@link Descriptor} for {@link MavenReporter}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class MavenReporterDescriptor extends Descriptor<MavenReporter> {
    protected MavenReporterDescriptor(Class<? extends MavenReporter> clazz) {
        super(clazz);
    }

    /**
     * Returns an instance used for automatic {@link MavenReporter} activation.
     *
     * <p>
     * Some {@link MavenReporter}s, such as {@link MavenArtifactArchiver},
     * can work just with the configuration in POM and don't need any additional
     * Hudson configuration. They also don't need any explicit enabling/disabling
     * as they can activate themselves by listening to the callback from the build
     * (for example javadoc archiver can do the work in response to the execution
     * of the javadoc target.)
     *
     * <p>
     * Those {@link MavenReporter}s should return a valid instance
     * from this method. Such instance will then participate into the build
     * and receive event callbacks.
     */
    public MavenReporter newAutoInstance(MavenModule module) {
        return null;
    }

    /**
     * If {@link #hasConfigScreen() the reporter has no configuration screen},
     * this method can safely return null, which is the default implementation.
     */
    public MavenReporter newInstance(StaplerRequest req) throws FormException {
        return null;
    }

    /**
     * Returns true if this descriptor has <tt>config.jelly</tt>.
     */
    public final boolean hasConfigScreen() {
        MetaClass c = MetaClass.get(getClass());
        try {
            JellyClassTearOff tearOff = c.loadTearOff(JellyClassTearOff.class);
            return tearOff.findScript(getConfigPage())!=null;
        } catch(JellyException e) {
            return false;
        }
    }
}
