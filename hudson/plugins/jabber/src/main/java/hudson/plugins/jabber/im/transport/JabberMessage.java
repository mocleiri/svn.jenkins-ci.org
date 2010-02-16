package hudson.plugins.jabber.im.transport;

import hudson.plugins.im.IMMessage;

import org.jivesoftware.smack.packet.Message;

public class JabberMessage extends IMMessage {

    public JabberMessage(Message msg) {
        super(msg.getFrom(), msg.getTo(), msg.getBody());
    }
}
