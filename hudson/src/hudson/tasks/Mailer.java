package hudson.tasks;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Descriptor;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Sends the build result in e-mail.
 *
 * @author Kohsuke Kawaguchi
 */
public class Mailer implements BuildStep {

    /**
     * Whitespace-separated list of e-mail addresses.
     */
    private String recipients;

    private boolean dontNotifyEveryUnstableBuild;

    // TODO: left so that XStream won't get angry. figure out how to set the error handling behavior
    // in XStream.
    private transient String from;
    private transient String subject;
    private transient boolean failureOnly;

    public Mailer(String recipients, boolean dontNotifyEveryUnstableBuild) {
        this.recipients = recipients;
        this.dontNotifyEveryUnstableBuild = dontNotifyEveryUnstableBuild;
    }

    public String getRecipients() {
        return recipients;
    }

    public boolean isDontNotifyEveryUnstableBuild() {
        return dontNotifyEveryUnstableBuild;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        try {
            MimeMessage mail = getMail(build);
            if(mail!=null) {
                listener.getLogger().println("Sending e-mails to "+recipients);
                Transport.send(mail);
            }
        } catch (MessagingException e) {
            e.printStackTrace( listener.error(e.getMessage()) );
        }

        return true;
    }

    private MimeMessage getMail(Build build) throws MessagingException {
        if(build.getResult()==Result.FAILURE) {
            return createFailureMail(build);
        }

        if(build.getResult()==Result.UNSTABLE) {
            Build prev = build.getPreviousBuild();
            if(!dontNotifyEveryUnstableBuild)
                return createUnstableMail(build);
            if(prev!=null) {
                if(prev.getResult()==Result.SUCCESS)
                    return createUnstableMail(build);
            }
        }

        if(build.getResult()==Result.SUCCESS) {
            Build prev = build.getPreviousBuild();
            if(prev!=null) {
                if(prev.getResult()==Result.FAILURE)
                    return createBackToNormalMail(build, "normal");
                if(prev.getResult()==Result.UNSTABLE)
                    return createBackToNormalMail(build, "stable");
            }
        }

        return null;
    }

    private MimeMessage createBackToNormalMail(Build build, String subject) throws MessagingException {
        MimeMessage msg = createEmptyMail();

        msg.setSubject(getSubject(build,"Hudson build is back to "+subject +": "));
        StringBuffer buf = new StringBuffer();
        appendBuildUrl(build,buf);
        msg.setText(buf.toString());

        return msg;
    }

    private MimeMessage createUnstableMail(Build build) throws MessagingException {
        MimeMessage msg = createEmptyMail();

        msg.setSubject(getSubject(build,"Hudson build became unstable: "));
        StringBuffer buf = new StringBuffer();
        appendBuildUrl(build,buf);
        msg.setText(buf.toString());

        return msg;
    }

    private void appendBuildUrl(Build build, StringBuffer buf) {
        String baseUrl = DESCRIPTOR.getUrl();
        if(baseUrl!=null) {
            buf.append("See ").append(baseUrl).append(build.getUrl()).append("\n\n");
        }
    }

    private MimeMessage createFailureMail(Build build) throws MessagingException {
        MimeMessage msg = createEmptyMail();

        msg.setSubject(getSubject(build, "Build failed in Hudson: "));

        StringBuffer buf = new StringBuffer();
        appendBuildUrl(build,buf);

        buf.append("---------\n");

        try {
            buf.append(build.getLog());
        } catch (IOException e) {
            // somehow failed to read the contents of the log
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            buf.append("Failed to access build log\n\n").append(sw);
        }

        msg.setText(buf.toString());

        return msg;
    }

    private MimeMessage createEmptyMail() throws MessagingException {
        MimeMessage msg = new MimeMessage(DESCRIPTOR.createSession());
        // TODO: I'd like to put the URL to the page in here,
        // but how do I obtain that?
        msg.setContent("","text/plain");
        msg.setFrom(new InternetAddress(DESCRIPTOR.getAdminAddress()));

        List<InternetAddress> rcp = new ArrayList<InternetAddress>();
        StringTokenizer tokens = new StringTokenizer(recipients);
        while(tokens.hasMoreTokens())
            rcp.add(new InternetAddress(tokens.nextToken()));
        msg.setRecipients(Message.RecipientType.TO, rcp.toArray(new InternetAddress[rcp.size()]));
        return msg;
    }

    public hudson.model.Descriptor<BuildStep> getDescriptor() {
        return DESCRIPTOR;
    }

    private String getSubject(Build build, String caption) {
        return caption +build.getProject().getName()+" #"+build.getNumber();
    }


    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<BuildStep> {

        public DescriptorImpl() {
            super(Mailer.class);
        }

        public String getDisplayName() {
            return "E-mail Notification";
        }

        public String getHelpFile() {
            return "/help/project-config/mailer.html";
        }

        /** JavaMail session. */
        public Session createSession() {
            Properties props = new Properties(System.getProperties());
            // can't use putAll
            for (Map.Entry o : ((Map<?,?>)getProperties()).entrySet()) {
                if(o.getValue()!=null)
                    props.put(o.getKey(),o.getValue());
            }
            return Session.getInstance(props);
        }

        public boolean configure(HttpServletRequest req) {
            getProperties().put("mail.smtp.host",nullify(req.getParameter("mailer_smtp_server")));
            getProperties().put("mail.admin.address",req.getParameter("mailer_admin_address"));
            getProperties().put("mail.hudson.url",nullify(req.getParameter("mailer_hudson_url")));

            save();
            return super.configure(req);
        }

        private String nullify(String v) {
            if(v!=null && v.length()==0)    v=null;
            return v;
        }

        public String getSmtpServer() {
            return (String)getProperties().get("mail.smtp.host");
        }

        public String getAdminAddress() {
            String v = (String)getProperties().get("mail.admin.address");
            if(v==null)     v = "address not configured yet <nobody>";
            return v;
        }

        public String getUrl() {
            return (String)getProperties().get("mail.hudson.url");
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new Mailer(
                req.getParameter("mailer_recipients"),
                req.getParameter("mailer_not_every_unstable")!=null
            );
        }
    };
}
