package hudson.tasks;

import hudson.Launcher;
import hudson.Functions;
import hudson.maven.AbstractMavenProject;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.User;
import hudson.model.UserPropertyDescriptor;
import hudson.util.FormFieldValidator;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

/**
 * {@link Publisher} that sends the build result in e-mail.
 *
 * @author Kohsuke Kawaguchi
 */
public class Mailer extends Publisher {
    protected static final Logger LOGGER = Logger.getLogger(Mailer.class.getName());

    /**
     * Whitespace-separated list of e-mail addresses that represent recipients.
     */
    public String recipients;

    /**
     * If true, only the first unstable build will be reported.
     */
    public boolean dontNotifyEveryUnstableBuild;

    /**
     * If true, individuals will receive e-mails regarding who broke the build.
     */
    public boolean sendToIndividuals;

    // TODO: left so that XStream won't get angry. figure out how to set the error handling behavior
    // in XStream.
    private transient String from;
    private transient String subject;
    private transient boolean failureOnly;

    public boolean perform(Build build, Launcher launcher, BuildListener listener) throws InterruptedException {
        return _perform(build,launcher,listener);
    }

    public <P extends Project<P,B>,B extends Build<P,B>> boolean _perform(B build, Launcher launcher, BuildListener listener) throws InterruptedException {
        if(debug)
            listener.getLogger().println("Running mailer");
        return new MailSender<P,B>(recipients,dontNotifyEveryUnstableBuild,sendToIndividuals) {
            /** Check whether a path (/-separated) will be archived. */
            @Override
            public boolean artifactMatches(String path, B build) {
                ArtifactArchiver aa = (ArtifactArchiver) build.getProject().getPublishers().get(ArtifactArchiver.DESCRIPTOR);
                if (aa == null) {
                    LOGGER.finer("No ArtifactArchiver found");
                    return false;
                }
                String artifacts = aa.getArtifacts();
                for (String include : artifacts.split("[, ]+")) {
                    String pattern = include.replace(File.separatorChar, '/');
                    if (pattern.endsWith("/")) {
                        pattern += "**";
                    }
                    if (SelectorUtils.matchPath(pattern, path)) {
                        LOGGER.log(Level.FINER, "DescriptorImpl.artifactMatches true for {0} against {1}", new Object[] {path, pattern});
                        return true;
                    }
                }
                LOGGER.log(Level.FINER, "DescriptorImpl.artifactMatches for {0} matched none of {1}", new Object[] {path, artifacts});
                return false;
            }
        }.execute(build,listener);
    }

    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * The default e-mail address suffix appended to the user name found from changelog,
         * to send e-mails. Null if not configured.
         */
        private String defaultSuffix;

        /**
         * Hudson's own URL, to put into the e-mail.
         */
        private String hudsonUrl;

        /**
         * If non-null, use SMTP-AUTH with these information.
         */
        private String smtpAuthPassword,smtpAuthUsername;

        /**
         * The e-mail address that Hudson puts to "From:" field in outgoing e-mails.
         * Null if not configured.
         */
        private String adminAddress;

        /**
         * The SMTP server to use for sending e-mail. Null for default to the environment,
         * which is usually <tt>localhost</tt>.
         */
        private String smtpHost;
        
        /**
         * If true use SSL on port 465 (standard SMTPS) unless <code>smtpPort</code> is set.
         */
        private boolean useSsl;

        /**
         * The SMTP port to use for sending e-mail. Null for default to the environment,
         * which is usually <tt>25</tt>.
         */
        private String smtpPort;

        /**
         * Used to keep track of number test e-mails.
         */
        private static transient int testEmailCount = 0;
        

        public DescriptorImpl() {
            super(Mailer.class);
            load();
        }

        /**
         * For backward compatibility.
         */
        protected void convert(Map<String, Object> oldPropertyBag) {
            defaultSuffix = (String)oldPropertyBag.get("mail.default.suffix");
            hudsonUrl = (String)oldPropertyBag.get("mail.hudson.url");
            smtpAuthUsername = (String)oldPropertyBag.get("mail.hudson.smtpauth.username");
            smtpAuthPassword = (String)oldPropertyBag.get("mail.hudson.smtpauth.password");
            adminAddress = (String)oldPropertyBag.get("mail.admin.address");
            smtpHost = (String)oldPropertyBag.get("mail.smtp.host");
        }

        public String getDisplayName() {
            return Messages.Mailer_DisplayName();
        }

        public String getHelpFile() {
            return "/help/project-config/mailer.html";
        }

        public String getDefaultSuffix() {
            return defaultSuffix;
        }

        /** JavaMail session. */
        public Session createSession() {
            Properties props = new Properties(System.getProperties());
            if(smtpHost!=null)
                props.put("mail.smtp.host",smtpHost);
            if (smtpPort!=null) {
                props.put("mail.smtp.port", smtpPort);
            }
            if (useSsl) {
            	/* This allows the user to override settings by setting system properties but
            	 * also allows us to use the default SMTPs port of 465 if no port is already set.
            	 * It would be cleaner to use smtps, but that's done by calling session.getTransport()...
            	 * and thats done in mail sender, and it would be a bit of a hack to get it all to
            	 * coordinate, and we can make it work through setting mail.smtp properties.
            	 */
            	if (props.getProperty("mail.smtp.socketFactory.port") == null) {
                    String port = smtpPort==null?"465":smtpPort;
                    props.put("mail.smtp.port", port);
                    props.put("mail.smtp.socketFactory.port", port);
            	}
            	if (props.getProperty("mail.smtp.socketFactory.class") == null) {
            		props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
            	}
				props.put("mail.smtp.socketFactory.fallback", "false");
			}
            if(getSmtpAuthUserName()!=null)
                props.put("mail.smtp.auth","true");
            return Session.getInstance(props,getAuthenticator());
        }

        private Authenticator getAuthenticator() {
            final String un = getSmtpAuthUserName();
            if(un==null)    return null;
            return new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(getSmtpAuthUserName(),getSmtpAuthPassword());
                }
            };
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            // this code is brain dead
            smtpHost = nullify(req.getParameter("mailer_smtp_server"));
            setAdminAddress(req.getParameter("mailer_admin_address"));

            defaultSuffix = nullify(req.getParameter("mailer_default_suffix"));
            String url = nullify(json.getString("url"));
            if(url!=null && !url.endsWith("/"))
                url += '/';
            hudsonUrl = url;

            if(req.getParameter("mailer.useSMTPAuth")!=null) {
                smtpAuthUsername = nullify(req.getParameter("mailer.SMTPAuth.userName"));
                smtpAuthPassword = nullify(req.getParameter("mailer.SMTPAuth.password"));
            } else {
                smtpAuthUsername = smtpAuthPassword = null;
            }
            smtpPort = nullify(req.getParameter("mailer_smtp_port"));
            useSsl = req.getParameter("mailer_smtp_use_ssl")!=null;
            save();
            return true;
        }

        private String nullify(String v) {
            if(v!=null && v.length()==0)    v=null;
            return v;
        }

        public String getSmtpServer() {
            return smtpHost;
        }

        public String getAdminAddress() {
            String v = adminAddress;
            if(v==null)     v = "address not configured yet <nobody@nowhere>";
            return v;
        }

        public String getUrl() {
            return hudsonUrl;
        }

        public String getSmtpAuthUserName() {
            return smtpAuthUsername;
        }

        public String getSmtpAuthPassword() {
            return smtpAuthPassword;
        }
        
        public boolean getUseSsl() {
        	return useSsl;
        }

        public String getSmtpPort() {
        	return smtpPort;
        }

        public void setDefaultSuffix(String defaultSuffix) {
            this.defaultSuffix = defaultSuffix;
        }

        public void setHudsonUrl(String hudsonUrl) {
            this.hudsonUrl = hudsonUrl;
        }

        public void setAdminAddress(String adminAddress) {
            if(adminAddress.startsWith("\"") && adminAddress.endsWith("\"")) {
                // some users apparently quote the whole thing. Don't konw why
                // anyone does this, but it's a machine's job to forgive human mistake
                adminAddress = adminAddress.substring(1,adminAddress.length()-1);
            }
            this.adminAddress = adminAddress;
        }

        public void setSmtpHost(String smtpHost) {
            this.smtpHost = smtpHost;
        }

        public void setUseSsl(boolean useSsl) {
            this.useSsl = useSsl;
        }

        public void setSmtpPort(String smtpPort) {
            this.smtpPort = smtpPort;
        }

        public Publisher newInstance(StaplerRequest req) {
            Mailer m = new Mailer();
            req.bindParameters(m,"mailer_");
            m.dontNotifyEveryUnstableBuild = req.getParameter("mailer_notifyEveryUnstableBuild")==null;

            if(hudsonUrl==null) {
                // if Hudson URL is not configured yet, infer some default
                hudsonUrl = Functions.inferHudsonURL(req);
                save();
            }

            return m;
        }

        /**
         * Checks the URL in <tt>global.jelly</tt>
         */
        public void doCheckUrl(StaplerRequest req, StaplerResponse rsp,
                                   @QueryParameter("value") final String value) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,false) {
                protected void check() throws IOException, ServletException {
                    if(value.startsWith("http://localhost"))
                        warning("Please set a valid host name, instead of localhost");
                    else
                        ok();
                }
            }.process();
        }

        public void doAddressCheck(StaplerRequest req, StaplerResponse rsp,
                                   @QueryParameter("value") final String value) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,false) {
                protected void check() throws IOException, ServletException {
                    try {
                        new InternetAddress(value);
                        ok();
                    } catch (AddressException e) {
                        error(e.getMessage());
                    }
                }
            }.process();
        }
        
        /**
         * Send an email to the admin address
         * @param req ignored request
         * @param rsp used to write the result of the sending
         * @throws IOException
         * @throws ServletException
         * @throws InterruptedException
         */
        public void doSendTestMail(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
            rsp.setContentType("text/plain");
            PrintStream writer = new PrintStream(rsp.getOutputStream());            
            try {
                writer.println("Sending email to " + getAdminAddress());
                writer.println();
                writer.println("Email content ---------------------------------------------------------");
                writer.flush();
                
                MimeMessage msg = new MimeMessage(createSession());
                msg.setSubject("Test email #" + ++testEmailCount);
                msg.setContent("This is test email #" + testEmailCount + " sent from Hudson Continuous Integration server.", "text/plain");
                msg.setFrom(new InternetAddress(getAdminAddress()));
                msg.setSentDate(new Date());
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress(getAdminAddress()));                
                msg.writeTo(writer);
                writer.println();                
                writer.println("-----------------------------------------------------------------------");
                writer.println();
                writer.flush();
                
                Transport.send(msg);
                
                writer.println("Email was successfully sent");
            } catch (MessagingException e) {
                e.printStackTrace(writer);
            }
            writer.flush();
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            // for historical reasons, Maven uses MavenMailer
            return !AbstractMavenProject.class.isAssignableFrom(jobType);
        }
    }

    /**
     * Per user property that is e-mail address.
     */
    public static class UserProperty extends hudson.model.UserProperty {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        /**
         * The user's e-mail address.
         * Null to leave it to default.
         */
        private final String emailAddress;

        public UserProperty(String emailAddress) {
            this.emailAddress = emailAddress;
        }

        @Exported
        public String getAddress() {
            if(emailAddress!=null)
                return emailAddress;

            // try the inference logic
            return MailAddressResolver.resolve(user);
        }

        public DescriptorImpl getDescriptor() {
            return DESCRIPTOR;
        }

        public static final class DescriptorImpl extends UserPropertyDescriptor {
            public DescriptorImpl() {
                super(UserProperty.class);
            }

            public String getDisplayName() {
                return Messages.Mailer_UserProperty_DisplayName();
            }

            public UserProperty newInstance(User user) {
                
                return new UserProperty(null);
            }

            public UserProperty newInstance(StaplerRequest req) throws FormException {
                return new UserProperty(req.getParameter("email.address"));
            }
        }
    }

    /**
     * Debug probe point to be activated by the scripting console.
     */
    public static boolean debug = false;
}
