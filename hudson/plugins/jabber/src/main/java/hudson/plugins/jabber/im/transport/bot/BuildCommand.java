/*
 * Created on Apr 22, 2007
 */
package hudson.plugins.jabber.im.transport.bot;

import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Hudson;
import hudson.model.Queue;
import hudson.plugins.jabber.im.transport.JabberChat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 * Build command for the Jabber bot.
 * @author Pascal Bleser
 */
public class BuildCommand implements BotCommand {
	
	private static final Pattern NUMERIC_EXTRACTION_REGEX = Pattern.compile("^(\\d+)");
	private static final String HELP = " <job> [now|<delay[s|m|h]>] - schedule a job build, with standard, custom or no quiet period";
	
	private final String buildNowCommand;
	
	public BuildCommand() {
		this.buildNowCommand = "build";
	}
	
	public BuildCommand(final String buildNowCommand) {
		this.buildNowCommand = buildNowCommand;
	}
	
	private boolean scheduleBuild(final AbstractProject<?, ?> project, final int delaySeconds) {
		Queue queue = Hudson.getInstance().getQueue();
        return queue.schedule(project, delaySeconds) != null;
	}
	
	public void executeCommand(final JabberChat groupChat, final Message message, String sender,
			final String[] args) throws XMPPException {
		if (args.length >= 2) {
			String jobName = args[1];
			jobName = jobName.replaceAll("\"", "");
			
            		AbstractProject<?, ?> project = Hudson.getInstance().getItemByFullName(jobName, AbstractProject.class);
			if (project != null) {
    				if (project.isInQueue()) {
    					Queue.Item queueItem = project.getQueueItem();
						groupChat.sendMessage(new StringBuffer(sender).append(": job ")
							.append(jobName).append(" is already in the build queue (")
							.append(queueItem.getWhy()).append(")")
							.toString());
            			} else {
            				if (project.isDisabled()) {
            					groupChat.sendMessage(new StringBuffer(sender).append(": job ")
            						.append(jobName).append(" is disabled")
            						.toString());
            				} else {
            					//project.scheduleBuild();
            					if ((args.length <= 2) && (args[0].equals(buildNowCommand)) || "now".equalsIgnoreCase(args[2])) {
            						if (scheduleBuild(project, 1)) {
	                					groupChat.sendMessage(new StringBuffer(sender).append(": job ")
		                					.append(jobName).append(" build scheduled now")
		                					.toString());
      	     						} else {
		            					groupChat.sendMessage(new StringBuffer(sender).append(": job ")
	                					.append(jobName).append(" scheduling failed or already in build queue")
	                					.toString());
            						}
            					} else if (args.length >= 3) {
		            				final String delay = args[2].trim();
		            				int factor = 1;
		            				if (delay.endsWith("m") || delay.endsWith("min")) {
		            					factor = 60;
		            				} else if (delay.endsWith("h")) {
		            					factor = 3600;
		            				}
		            				Matcher matcher = NUMERIC_EXTRACTION_REGEX.matcher(delay);
		            				if (matcher.find()) {
		            					int value = Integer.parseInt(matcher.group());
		                				if (scheduleBuild(project, value * factor)) {
		    	                			groupChat.sendMessage(new StringBuffer(sender).append(": job ")
		    	                					.append(jobName).append(" build scheduled with a quiet period of ")
		    	                					.append(value * factor).append(" seconds")
		    	                					.toString());
		                				} else {
		                					groupChat.sendMessage(new StringBuffer(sender).append(": job ")
		    	                					.append(jobName).append(" already scheduled in build queue")
		    	                					.toString());
		                				}
		            				}
            				
            					} else {
            						// TODO: replace this with a remoteCause or with a UserCause for the user issueing the bot command?
		            				if (project.scheduleBuild(new Cause.LegacyCodeCause())) {
			                			groupChat.sendMessage(new StringBuffer(sender).append(": job ")
			                					.append(jobName).append(" build scheduled (quiet period: ")
			                					.append(project.getQuietPeriod()).append(" seconds)")
			                					.toString());
		            				} else {
		            					groupChat.sendMessage(new StringBuffer(sender).append(": job ")
			                					.append(jobName).append(" already scheduled in build queue")
			                					.toString());
		            				}
            					}
            				}
            			}
            		} else {
            			groupChat.sendMessage(new StringBuffer(sender).append(": unknown job ")
            				.append(jobName).toString());
            		}
		}
	}

	public String getHelp() {
		return HELP;
	}

}
