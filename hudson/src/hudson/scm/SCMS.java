package hudson.scm;

import hudson.model.Descriptor;

/**
 * @author Kohsuke Kawaguchi
 */
public class SCMS {
    public static final Descriptor<SCM>[] SCMS =
        Descriptor.toArray(NullSCM.DESCRIPTOR,CVSSCM.DESCRIPTOR,SubversionSCM.DESCRIPTOR);
}
