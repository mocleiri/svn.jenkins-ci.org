package hudson.tasks;

import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Result;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.net.InetAddress;

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

    /**
     * If true, the e-mail is sent only when a build fails.
     */
    private boolean failureOnly;

    private String subject;

    private String from;

    public Mailer(String recipients, boolean failureOnly, String subject, String from) {
        this.recipients = recipients;
        this.failureOnly = failureOnly;
        this.subject = subject;
        this.from = from;
    }

    public String getRecipients() {
        return recipients;
    }

    public boolean isFailureOnly() {
        return failureOnly;
    }

    public String getSubject() {
        return subject;
    }

    public String getFrom() {
        return from;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, BuildListener listener) {
        if(build.getResult()!=Result.FAILURE && failureOnly)
            return true;    // nothing to report

        listener.getLogger().println("Sending e-mails to "+recipients);

        try {
            MimeMessage msg = new MimeMessage(DESCRIPTOR.createSession());
            // TODO: I'd like to put the URL to the page in here,
            // but how do I obtain that?
            msg.setContent("","text/plain");
            msg.setSubject(expandSubjectMacro(subject,build));
            msg.setFrom(new InternetAddress(from));

            List<InternetAddress> rcp = new ArrayList<InternetAddress>();
            StringTokenizer tokens = new StringTokenizer(recipients);
            while(tokens.hasMoreTokens())
                rcp.add(new InternetAddress(tokens.nextToken()));
            msg.setRecipients(Message.RecipientType.TO, rcp.toArray(new InternetAddress[rcp.size()]));

            Transport.send(msg);
        } catch (MessagingException e) {
            e.printStackTrace( listener.error(e.getMessage()) );
        }

        return true;
    }

    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    private String expandSubjectMacro( String s, Build build ) {
        StringBuffer buf = new StringBuffer(s);
        replace(buf,"${buildId}",build.getId());
        replace(buf,"${buildNumber}",Integer.toString(build.getNumber()));
        replace(buf,"${result}",build.getResult().toString());
        return buf.toString();
    }

    private void replace(StringBuffer buf, String src, String tgt) {
        int idx;
        while((idx=buf.indexOf(src))!=-1) {
            buf.replace(idx,idx+src.length(),tgt);
        }
    }


    public static final Descriptor DESCRIPTOR = new Descriptor();

    public static final class Descriptor extends BuildStepDescriptor {

        public Descriptor() {
            super(Mailer.class);
        }

        public String getDisplayName() {
            return "E-mail Notification";
        }

        /** JavaMail session. */
        public Session createSession() {
            Properties props = new Properties(System.getProperties());
            props.putAll(getProperties());
            return Session.getInstance(props);
        }

        public boolean configure(HttpServletRequest req) {
            String v = req.getParameter("mailer_smtpServer");
            if(v!=null && v.length()==0)    v=null;
            getProperties().put("mail.smtp.host",v);
            save();
            return super.configure(req);
        }

        public String getSmtpServer() {
            return (String)getProperties().get("mail.smtp.host");
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new Mailer(
                req.getParameter("mailer_recipients"),
                req.getParameter("mailer_failureOnly")!=null,
                req.getParameter("mailer_subject"),
                req.getParameter("mailer_from")
            );
        }
    };
}
