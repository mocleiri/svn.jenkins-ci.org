package hudson.plugins.jabber.im.transport.bot;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.plugins.jabber.im.transport.JabberChat;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 * Abort a running job
 * @author R. Tyler Ballance <tyler@slide.com>
 */
public class AbortCommand implements BotCommand {
	
	private static final String HELP = " <job> - specify which job to abort";

	public void executeCommand(JabberChat groupChat, Message message, String sender, String[] args) throws XMPPException {
		if (args.length >= 2) {
			String jobName = args[1];
			jobName = jobName.replaceAll("\"", "");

			AbstractProject<?, ?> project = Hudson.getInstance().getItemByFullName(jobName, AbstractProject.class);
			if (project == null) {
				// Invalid job name
				groupChat.sendMessage(sender + ": that doesn't look like a valid job");
				return;
			}
			if ( (project.isInQueue() == false) && (project.isBuilding() == false) ) {
				groupChat.sendMessage(String.format("%s: how do you intend a build that isn't building?", sender));
				return;
			}
			
			boolean aborted = false;
			if (project.isInQueue()) {
				aborted = Hudson.getInstance().getQueue().cancel(project);
			}
			
			if (!aborted) {
				// must be already building
				AbstractBuild<?, ?> build = project.getLastBuild();
				if (build == null) {
					// No builds? lolwut?
					groupChat.sendMessage(sender + ": it appears this job has never been built");
					return;
				}	
	
				Executor ex = build.getExecutor();
				if (ex == null) {
					aborted = false; // how the hell does this happen o_O
				} else {
					ex.interrupt();
				}
			}

			if (aborted) {
				groupChat.sendMessage(jobName + " aborted, I hope you're happy!");
			} else {
				groupChat.sendMessage(sender + ": " + " couldn't abort " + jobName + ". I don't know why this happened.");
			}
		} 
		else {
			// No job name specified
			groupChat.sendMessage(sender + ": you need to specify a job name");
			return; 
		}
	}

	public String getHelp() {
		return HELP;
	}

}
