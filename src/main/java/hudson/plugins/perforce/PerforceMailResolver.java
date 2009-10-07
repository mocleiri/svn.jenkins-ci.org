package hudson.plugins.perforce;

import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.tasks.MailAddressResolver;
import hudson.Extension;

import com.perforce.p4java.server.P4JUser;

/**
 * Implementation of {@link MailAddressResolver} for looking up the email address of a user in the Perforce repository.
 *
 * @author Mike
 *         Date: Apr 22, 2008 2:01:37 PM
 */
@Extension
public class PerforceMailResolver extends MailAddressResolver {

    public String findMailAddressFor(User u) {
        for (AbstractProject p : u.getProjects()) {
            if (p.getScm() instanceof PerforceSCM) {
                PerforceSCM pscm = (PerforceSCM) p.getScm();
                try {
                    // couldn't resist the name pu...
                    P4JUser pu = P4jUtil.getUser(pscm.getServer(), u.getId());
                    if (pu.getEmail() != null && !pu.getEmail().equals(""))
                        return pu.getEmail();
                } catch (Exception e) {
                    // where are we supposed to log this error?
                }
            }
        }
        return null;
    }

}
