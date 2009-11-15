package hudson.plugins.jabber.im.transport;

import java.util.List;

import hudson.Extension;
import hudson.model.User;
import hudson.plugins.im.DefaultIMMessageTarget;
import hudson.plugins.im.DefaultIMMessageTargetConverter;
import hudson.plugins.im.GroupChatIMMessageTarget;
import hudson.plugins.im.IMConnection;
import hudson.plugins.im.IMException;
import hudson.plugins.im.IMMessageTarget;
import hudson.plugins.im.IMMessageTargetConversionException;
import hudson.plugins.im.IMMessageTargetConverter;
import hudson.plugins.im.IMPublisher;
import hudson.plugins.jabber.user.JabberUserProperty;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;

/**
 * Jabber-specific implementation of the Publisher.
 * @author Uwe Schaefer
 */
public class JabberPublisher extends IMPublisher
{
    private static class JabberIMMessageTargetConverter extends DefaultIMMessageTargetConverter
    {
        private void checkValidity(final String f) throws IMMessageTargetConversionException
        {
        	// See: http://xmpp.org/rfcs/rfc3920.html#addressing
        	// obviously, there is no easy regexp to validate this.
        	// Additionally, we require the part before the @.
            // So, just some very simple validation:
            final int i = f.indexOf('@');
            if (i == -1) {
            	throw new IMMessageTargetConversionException("Invalid input for target: '" + f + "'." +
            			"\nDoesn't contain a @.");
            } else if (f.indexOf('@', i + 1) != -1)
            {
                throw new IMMessageTargetConversionException("Invalid input for target: '" + f + "'." +
                		"\nContains more than on @.");
            }
        }

        @Override
        public IMMessageTarget fromString(final String targetAsString) throws IMMessageTargetConversionException
        {
            String f = targetAsString.trim();
            if (f.length() > 0)
            {
            	IMMessageTarget target;
            	if (f.startsWith("*")) {
            		f = f.substring(1);
            		// group chat
            		if (! f.contains("@")) {
            			f += "@conference." + JabberPublisher.DESCRIPTOR.getHostname();
            		}
            		target = new GroupChatIMMessageTarget(f);
            	} else if (f.contains("@conference.")) {
            		target = new GroupChatIMMessageTarget(f);
            	} else {
	                if (!f.contains("@")) {
	                    f += "@" + JabberPublisher.DESCRIPTOR.getHostname();
	                }
	                target = new DefaultIMMessageTarget(f);
            	}
                checkValidity(f);
                return target;
            }
            else
            {
                return null;
            }
        }
    }
    @Extension
    public static final JabberPublisherDescriptor DESCRIPTOR = new JabberPublisherDescriptor();

    private static final IMMessageTargetConverter CONVERTER = new JabberIMMessageTargetConverter();

    public JabberPublisher(final String targetsAsString, final String notificationStrategy,
    		final boolean notifyGroupChatsOnBuildStart,
    		final boolean notifySuspects,
    		final boolean notifyCulprits,
    		final boolean notifyFixers) throws IMMessageTargetConversionException
    {
        super(targetsAsString, notificationStrategy, notifyGroupChatsOnBuildStart,
        		notifySuspects, notifyCulprits, notifyFixers);
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return JabberPublisher.DESCRIPTOR;
    }

    @Override
    protected IMConnection getIMConnection() throws IMException {
        return JabberIMConnectionProvider.getInstance().currentConnection();
    }

    @Override
    protected IMMessageTargetConverter getIMMessageTargetConverter() {
        return JabberPublisher.CONVERTER;
    }

	@Override
	protected String getPluginName() {
		return "Jabber notifier plugin";
	}

	@Override
	protected String getConfiguredIMId(User user) {
		JabberUserProperty jabberUserProperty = (JabberUserProperty) user.getProperties().get(JabberUserProperty.DESCRIPTOR);
		if (jabberUserProperty != null) {
			return jabberUserProperty.getJid();
		}
		return null;
	}
    
	@Override
	public String getTargets() {
		List<IMMessageTarget> notificationTargets = getNotificationTargets();
		
		StringBuilder sb = new StringBuilder();
		for (IMMessageTarget target : notificationTargets) {
			if ((target instanceof GroupChatIMMessageTarget) && (! target.toString().contains("@conference."))) {
        		sb.append("*");
        	}
            sb.append(getIMMessageTargetConverter().toString(target));
            sb.append(" ");
        }
        return sb.toString().trim();
	}
    

    // since Hudson 1.319:
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}
}
