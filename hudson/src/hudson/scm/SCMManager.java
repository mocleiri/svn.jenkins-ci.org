package hudson.scm;

import hudson.model.Descriptor;


/**
 * @author Kohsuke Kawaguchi
 */
public final class SCMManager {
    public static Descriptor<SCM>[] getSupportedSCMs() {
        return Descriptor.toArray(NullSCM.DESCRIPTOR,CVSSCM.DESCRIPTOR,SubversionSCM.DESCRIPTOR);
    }
}
