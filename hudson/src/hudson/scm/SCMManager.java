package hudson.scm;

import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public final class SCMManager {
    public static SCMDescriptor[] getSupportedSCMs() {
        return new SCMDescriptor[]{NullSCM.DESCRIPTOR,CVSSCM.DESCRIPTOR};
    }
}
