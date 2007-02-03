package hudson.maven;

import hudson.model.Descriptor;
import hudson.maven.reporters.MavenArtifactArchiver;
import hudson.maven.reporters.MavenFingerprinter;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Kohsuke Kawaguchi
 * @see MavenReporter
 */
public final class MavenReporters {
    /**
     * List of all installed {@link MavenReporter}s.
     */
    public static final List<MavenReporterDescriptor> LIST = Descriptor.toList(
        MavenArtifactArchiver.DescriptorImpl.DESCRIPTOR,
        MavenFingerprinter.DescriptorImpl.DESCRIPTOR
    );

    /**
     * Gets the subset of {@link #LIST} that has configuration screen.
     */
    public static List<MavenReporterDescriptor> getConfigurableList() {
        List<MavenReporterDescriptor> r = new ArrayList<MavenReporterDescriptor>();
        for (MavenReporterDescriptor d : LIST) {
            if(d.hasConfigScreen())
                r.add(d);
        }
        return r;
    }
}
