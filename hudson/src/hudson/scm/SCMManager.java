package hudson.scm;

import hudson.model.Descriptor;


/**
 * @author Kohsuke Kawaguchi
 */
public final class SCMManager {
    public static Descriptor<SCM>[] getSupportedSCMs() {
        return new Descriptor[]{NullSCM.DESCRIPTOR,CVSSCM.DESCRIPTOR,SubversionSCM.DESCRIPTOR};
    }
}
