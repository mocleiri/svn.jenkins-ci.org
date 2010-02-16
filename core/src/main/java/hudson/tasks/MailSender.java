/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Bruce Chapman, Daniel Dyer, Jean-Baptiste Quenot, Seiji Sogabe
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.tasks;

import hudson.FilePath;
import hudson.Util;
import hudson.Functions;
import hudson.model.*;
import hudson.scm.ChangeLogSet;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.AddressException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core logic of sending out notification e-mail.
 *
 * @author Jesse Glick
 * @author Kohsuke Kawaguchi
 */
public class MailSender {
    /**
     * Whitespace-separated list of e-mail addresses that represent recipients.
     */
    private String recipients;

    /**
     * If true, only the first unstable build will be reported.
     */
    private boolean dontNotifyEveryUnstableBuild;

    /**
     * If true, individuals will receive e-mails regarding who broke the build.
     */
    private boolean sendToIndividuals;


    public MailSender(String recipients, boolean dontNotifyEveryUnstableBuild, boolean sendToIndividuals) {
        this.recipients = recipients;
        this.dontNotifyEveryUnstableBuild = dontNotifyEveryUnstableBuild;
        this.sendToIndividuals = sendToIndividuals;
    }

    public boolean execute(AbstractBuild<?, ?> build, BuildListener listener) throws InterruptedException {
        try {
            Map<String, String> messageIds = new HashMap<String, String>();
            Set<InternetAddress> rcps = getRecipients(build, listener);
            for (InternetAddress rcp : rcps) {
                try {
                    MimeMessage mail = getMail(build, rcp, listener);
                    if (mail == null)
                        continue;
                    // if the previous e-mail was sent for a success, this new e-mail
                    // is not a follow up
                    AbstractBuild<?, ?> pb = build.getPreviousBuild();
                    if (pb != null && pb.getResult() == Result.SUCCESS) {
                        mail.removeHeader("In-Reply-To");
                        mail.removeHeader("References");
                    }
                    listener.getLogger().println("Sending e-mail to:" + rcp.getAddress());
                    Transport.send(mail);
                    messageIds.put(rcp.getAddress(), mail.getMessageID());
                } catch (MessagingException e) {
                    e.printStackTrace(listener.error(e.getMessage()));
                }
            }
            if (rcps.isEmpty())
                listener.getLogger().println(Messages.MailSender_ListEmpty());
            build.addAction(new MailMessageIdAction(messageIds));
        } catch (MessagingException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        } finally {
            CHECKPOINT.report();
        }

        return true;
    }

    /**
     * To correctly compute the state change from the previous build to this build,
     * we need to ignore aborted builds.
     * See http://www.nabble.com/Losing-build-state-after-aborts--td24335949.html
     *
     * <p>
     * And since we are consulting the earlier result, we need to wait for the previous build
     * to pass the check point.
     */
    private Result findPreviousBuildResult(AbstractBuild<?,?> b) throws InterruptedException {
        CHECKPOINT.block();
        do {
            b=b.getPreviousBuild();
            if(b==null) return null;
        } while(b.getResult()==Result.ABORTED);
        return b.getResult();
    }

    protected MimeMessage getMail(AbstractBuild<?, ?> build, InternetAddress addr, BuildListener listener) throws MessagingException, InterruptedException {
        Locale locale = getUserLocale(addr);

        if (build.getResult() == Result.FAILURE) {
            return createFailureMail(build, addr, locale, listener);
        }

        if (build.getResult() == Result.UNSTABLE) {
            if (!dontNotifyEveryUnstableBuild)
                return createUnstableMail(build, addr, locale, listener);
            Result prev = findPreviousBuildResult(build);
            if (prev == Result.SUCCESS)
                return createUnstableMail(build, addr, locale, listener);
        }

        if (build.getResult() == Result.SUCCESS) {
            Result prev = findPreviousBuildResult(build);
            if (prev == Result.FAILURE)
                return createBackToNormalMail(build, addr, locale, Messages._MailSender_BackToNormal_Normal().toString(locale), listener);
            if (prev == Result.UNSTABLE)
                return createBackToNormalMail(build, addr, locale, Messages._MailSender_BackToNormal_Stable().toString(locale), listener);
        }

        return null;
    }

    private Locale getUserLocale(InternetAddress iaddr) {
        String addr = iaddr.getAddress();
        Collection<User> users = User.getAll();
        for (User user : users) {
            Mailer.UserProperty mup = user.getProperty(Mailer.UserProperty.class);
            if (mup == null || !addr.equals(mup.getAddress()))
                continue;
            UserLocaleProperty ulp = user.getProperty(UserLocaleProperty.class);
            if (ulp == null)
                continue;
            Locale locale = ulp.getLocale();
            return (locale != null) ? locale : Locale.getDefault();
        }
        return Locale.getDefault();
    }

    private MimeMessage createBackToNormalMail(AbstractBuild<?, ?> build, InternetAddress addr, Locale locale, String subject, BuildListener listener) throws MessagingException {
        MimeMessage msg = createEmptyMail(build, addr, locale, listener);

        msg.setSubject(getSubject(build, Messages._MailSender_BackToNormalMail_Subject(subject).toString(locale)),MAIL_CHARSET);
        StringBuilder buf = new StringBuilder();
        appendBuildUrl(build, locale, buf);
        msg.setText(buf.toString(),MAIL_CHARSET);

        return msg;
    }

    private MimeMessage createUnstableMail(AbstractBuild<?, ?> build, InternetAddress addr, Locale locale, BuildListener listener) throws MessagingException {
        MimeMessage msg = createEmptyMail(build, addr, locale, listener);

        String subject = Messages._MailSender_UnstableMail_Subject().toString(locale);

        AbstractBuild<?, ?> prev = build.getPreviousBuild();
        boolean still = false;
        if(prev!=null) {
            if(prev.getResult()==Result.SUCCESS)
                subject =Messages._MailSender_UnstableMail_ToUnStable_Subject().toString(locale);
            else if(prev.getResult()==Result.UNSTABLE) {
                subject = Messages._MailSender_UnstableMail_StillUnstable_Subject().toString(locale);
                still = true;
            }
        }

        msg.setSubject(getSubject(build, subject),MAIL_CHARSET);
        StringBuilder buf = new StringBuilder();
        // Link to project changes summary for "still unstable" if this or last build has changes
        if (still && !(build.getChangeSet().isEmptySet() && prev.getChangeSet().isEmptySet()))
            appendUrl(Util.encode(build.getProject().getUrl()) + "changes", locale, buf);
        else
            appendBuildUrl(build, locale, buf);
        msg.setText(buf.toString(), MAIL_CHARSET);

        return msg;
    }

    private void appendBuildUrl(AbstractBuild<?, ?> build, Locale locale, StringBuilder buf) {
        appendUrl(Util.encode(build.getUrl())
                  + (build.getChangeSet().isEmptySet() ? "" : "changes"), locale, buf);
    }

    private void appendUrl(String url, Locale locale, StringBuilder buf) {
        String baseUrl = Mailer.descriptor().getUrl();
        if (baseUrl != null)
            buf.append(Messages._MailSender_Link(baseUrl, url).toString(locale)).append("\n\n");
    }

    private MimeMessage createFailureMail(AbstractBuild<?, ?> build, InternetAddress addr, Locale locale, BuildListener listener) throws MessagingException, InterruptedException {
        MimeMessage msg = createEmptyMail(build, addr, locale, listener);

        msg.setSubject(getSubject(build, Messages._MailSender_FailureMail_Subject().toString(locale)),MAIL_CHARSET);

        StringBuilder buf = new StringBuilder();
        appendBuildUrl(build, locale, buf);

        boolean firstChange = true;
        for (ChangeLogSet.Entry entry : build.getChangeSet()) {
            if (firstChange) {
                firstChange = false;
                buf.append(Messages._MailSender_FailureMail_Changes().toString(locale)).append("\n\n");
            }
            buf.append('[');
            buf.append(entry.getAuthor().getFullName());
            buf.append("] ");
            String m = entry.getMsg();
            buf.append(m);
            if (!m.endsWith("\n")) {
                buf.append('\n');
            }
            buf.append('\n');
        }

        buf.append("------------------------------------------\n");

        try {
            // Restrict max log size to avoid sending enormous logs over email.
            // Interested users can always look at the log on the web server.
            List<String> lines = build.getLog(MAX_LOG_LINES);

            String workspaceUrl = null, artifactUrl = null;
            Pattern wsPattern = null;
            String baseUrl = Mailer.descriptor().getUrl();
            if (baseUrl != null) {
                // Hyperlink local file paths to the repository workspace or build artifacts.
                // Note that it is possible for a failure mail to refer to a file using a workspace
                // URL which has already been corrected in a subsequent build. To fix, archive.
                workspaceUrl = baseUrl + Util.encode(build.getProject().getUrl()) + "ws/";
                artifactUrl = baseUrl + Util.encode(build.getUrl()) + "artifact/";
                FilePath ws = build.getWorkspace();
                // Match either file or URL patterns, i.e. either
                // c:\hudson\workdir\jobs\foo\workspace\src\Foo.java
                // file:/c:/hudson/workdir/jobs/foo/workspace/src/Foo.java
                // will be mapped to one of:
                // http://host/hudson/job/foo/ws/src/Foo.java
                // http://host/hudson/job/foo/123/artifact/src/Foo.java
                // Careful with path separator between $1 and $2:
                // workspaceDir will not normally end with one;
                // workspaceDir.toURI() will end with '/' if and only if workspaceDir.exists() at time of call
                wsPattern = Pattern.compile("(" +
                    Pattern.quote(ws.getRemote()) + "|" + Pattern.quote(ws.toURI().toString()) + ")[/\\\\]?([^:#\\s]*)");
            }
            for (String line : lines) {
                line = line.replace('\0',' '); // shall we replace other control code? This one is motivated by http://www.nabble.com/Problems-with-NULL-characters-in-generated-output-td25005177.html
                if (wsPattern != null) {
                    // Perl: $line =~ s{$rx}{$path = $2; $path =~ s!\\\\!/!g; $workspaceUrl . $path}eg;
                    Matcher m = wsPattern.matcher(line);
                    int pos = 0;
                    while (m.find(pos)) {
                        String path = m.group(2).replace(File.separatorChar, '/');
                        String linkUrl = artifactMatches(path, build) ? artifactUrl : workspaceUrl;
                        String prefix = line.substring(0, m.start()) + '<' + linkUrl + Util.encode(path) + '>';
                        pos = prefix.length();
                        line = prefix + line.substring(m.end());
                        // XXX better style to reuse Matcher and fix offsets, but more work
                        m = wsPattern.matcher(line);
                    }
                }
                buf.append(line);
                buf.append('\n');
            }
        } catch (IOException e) {
            // somehow failed to read the contents of the log
            buf.append(Messages._MailSender_FailureMail_FailedToAccessBuildLog().toString(locale)).append("\n\n").append(Functions.printThrowable(e));
        }

        msg.setText(buf.toString(),MAIL_CHARSET);

        return msg;
    }

    private MimeMessage createEmptyMail(AbstractBuild<?, ?> build, InternetAddress addr, Locale locale, BuildListener listener) throws MessagingException {
        MimeMessage msg = new MimeMessage(Mailer.descriptor().createSession());
        // TODO: I'd like to put the URL to the page in here,
        // but how do I obtain that?
        msg.setContent("", "text/plain");
        msg.setFrom(new InternetAddress(Mailer.descriptor().getAdminAddress()));
        msg.setSentDate(new Date());

        msg.setRecipient(Message.RecipientType.TO, addr);

        AbstractBuild<?, ?> pb = build.getPreviousBuild();
        if(pb!=null) {
            MailMessageIdAction b = pb.getAction(MailMessageIdAction.class);
            if(b!=null) {
                String messageId = null;
                if (b.messageId != null)
                    // backward compatibility
                    messageId = b.messageId;
                else if (b.messageIds != null) 
                    messageId = b.messageIds.get(addr.getAddress());
                if (messageId!=null) {
                    msg.setHeader("In-Reply-To",b.messageId);
                    msg.setHeader("References",b.messageId);
                }
            }
        }

        return msg;
    }

    private Set<InternetAddress> getRecipients(AbstractBuild<?, ?> build, BuildListener listener) throws MessagingException {
        Set<InternetAddress> rcp = new LinkedHashSet<InternetAddress>();
        StringTokenizer tokens = new StringTokenizer(recipients);
        while (tokens.hasMoreTokens()) {
            String address = tokens.nextToken();
            if(address.startsWith("upstream-individuals:")) {
                // people who made a change in the upstream
                String projectName = address.substring("upstream-individuals:".length());
                AbstractProject<?,?> up = Hudson.getInstance().getItemByFullName(projectName,AbstractProject.class);
                if(up==null) {
                    listener.getLogger().println("No such project exist: "+projectName);
                    continue;
                }
                AbstractBuild<?,?> pb = build.getPreviousBuild();
                AbstractBuild<?,?> ub = build.getUpstreamRelationshipBuild(up);
                AbstractBuild<?,?> upb = pb!=null ? pb.getUpstreamRelationshipBuild(up) : null;
                if(pb==null && ub==null && upb==null) {
                    listener.getLogger().println("Unable to compute the changesets in "+up+". Is the fingerprint configured?");
                    continue;
                }
                if(pb==null || ub==null || upb==null) {
                    listener.getLogger().println("Unable to compute the changesets in "+up);
                    continue;
                }
                for( AbstractBuild<?,?> b=upb; b!=ub && b!=null; b=b.getNextBuild())
                    rcp.addAll(buildCulpritList(listener,b.getCulprits()));
            } else {
                // ordinary address
                try {
                    rcp.add(new InternetAddress(address));
                } catch (AddressException e) {
                    // report bad address, but try to send to other addresses
                    e.printStackTrace(listener.error(e.getMessage()));
                }
            }
        }

        if (sendToIndividuals) {
            Set<User> culprits = build.getCulprits();

            if(debug)
                listener.getLogger().println("Trying to send e-mails to individuals who broke the build. sizeof(culprits)=="+culprits.size());

            rcp.addAll(buildCulpritList(listener,culprits));
        }
        return rcp;
    }

    private Set<InternetAddress> buildCulpritList(BuildListener listener, Set<User> culprits) throws AddressException {
        Set<InternetAddress> r = new HashSet<InternetAddress>();
        for (User a : culprits) {
            String adrs = Util.fixEmpty(a.getProperty(Mailer.UserProperty.class).getAddress());
            if(debug)
                listener.getLogger().println("  User "+a.getId()+" -> "+adrs);
            if (adrs != null)
                r.add(new InternetAddress(adrs));
            else {
                listener.getLogger().println(Messages.MailSender_NoAddress(a.getFullName()));
            }
        }
        return r;
    }

    private String getSubject(AbstractBuild<?, ?> build, String caption) {
        return caption + ' ' + build.getProject().getFullDisplayName() + " #" + build.getNumber();
    }

    /**
     * Check whether a path (/-separated) will be archived.
     */
    protected boolean artifactMatches(String path, AbstractBuild<?, ?> build) {
        return false;
    }

    public static boolean debug = false;

    private static final int MAX_LOG_LINES = Integer.getInteger(MailSender.class.getName()+".maxLogLines",250);

    /**
     * The charset to use for the text and subject.
     */
    private static final String MAIL_CHARSET = "UTF-8";

    /**
     * Sometimes the outcome of the previous build affects the e-mail we send, hence this checkpoint.
     */
    private static final CheckPoint CHECKPOINT = new CheckPoint("mail sent");
}
