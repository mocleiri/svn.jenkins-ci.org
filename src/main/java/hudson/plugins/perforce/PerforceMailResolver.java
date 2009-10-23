package hudson.plugins.perforce;

import com.perforce.p4java.exception.P4JException;
import com.perforce.p4java.server.P4JServer;
import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.tasks.MailAddressResolver;
import hudson.Extension;

import com.perforce.p4java.server.P4JUser;
import java.util.logging.Level;
import java.util.logging.Logger;

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
                P4JServer server = null;
                try {
                    // couldn't resist the name pu...
                    server = pscm.getServer();
                    P4JUser pu = P4jUtil.getUser(server, u.getId());
                    if (pu.getEmail() != null && !pu.getEmail().equals(""))
                        return pu.getEmail();
                } catch (Exception e) {
                    // where are we supposed to log this error?
                } finally {
                    try {
                        pscm.disconnectServer();
                    } catch (P4JException ex) {
                        // where are we supposed to log this error?
                    }
                }
            }
        }
        return null;
    }

}
