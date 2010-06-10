package org.jvnet.hudson.tools;

import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.JNNewsItems;
import org.kohsuke.jnt.JNNewsItem;

import javax.mail.internet.MimeMessage;
import javax.mail.Session;

public class App {
    public static void main(String[] args) throws Exception {
        if(args.length==0) {
            MimeMessage msg = new MimeMessage(Session.getInstance(System.getProperties()),System.in);
            String s = msg.getSubject();
            if(s==null || !s.startsWith("Suggested announcement for project hudson:"))
                return; // unrelated e-mail
        }

        // approve anything as long as it doesn't contain SNAPSHOT
        System.out.println("Scanning the news items");
        JNNewsItems news = JavaNet.connect().getProject("hudson").getNewsItems();
        for( JNNewsItem n : news.getPendingApprovals() ) {
            String headline = n.getHeadline();
            if(headline.endsWith(" released")) {
                if(headline.contains("SNAPSHOT")) {
                    System.out.println("Rejecting "+headline);
                    n.disapprove();
                    System.out.println("disapproved "+headline);
                } else {
                    System.out.println("Approving "+headline);
                    n.approve();
                    System.out.println("approved "+headline);
                }
            }
        }
    }
}
