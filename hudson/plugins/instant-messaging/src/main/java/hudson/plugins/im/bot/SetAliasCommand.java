package hudson.plugins.im.bot;

import hudson.plugins.im.IMChat;
import hudson.plugins.im.IMException;
import hudson.plugins.im.IMMessage;
import hudson.plugins.im.tools.MessageHelper;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

/**
 * {@link BotCommand} to create a command alias.
 * 
 * @author kutzi
 */
public class SetAliasCommand extends AbstractTextSendingCommand {

	private final Bot bot;

	public SetAliasCommand(Bot bot) {
		this.bot = bot;
	}
	
	@Override
	protected String getReply(String sender, String[] args) {
		if (args.length < 1) {
			throw new IllegalArgumentException();
		} else if (args.length == 1) {
			Map<String, AliasCommand> aliases = this.bot.getAliases();
			if (aliases.isEmpty()) {
				return "Defined aliases: none";
			} else {
				StringBuilder msg = new StringBuilder("Defined aliases:");
				for (Map.Entry<String, AliasCommand> entry : aliases.entrySet()) {
					msg.append("\n\t")
						.append(entry.getKey())
						.append(entry.getValue().getHelp());
				}
				return msg.toString();
			}
		} else if (args.length < 3) {
			String alias = args[1];
			AliasCommand aliasCmd = this.bot.removeAlias(alias);
			if (aliasCmd != null) {
				return "deleted alias: " + alias + aliasCmd.getHelp();
			} else {
				return sender + ": don't know an alias called '" + alias + "'";
			}
		} else {
			String alias = args[1];
			String cmdName = args[2];
			BotCommand cmd = this.bot.getCommand(cmdName);
			if (cmd == null) {
				return sender + ": sorry don't know a command or alias called '" + cmdName + "'";
			}
			String[] cmdArguments = ArrayUtils.EMPTY_STRING_ARRAY;
			if (args.length > 3) {
				cmdArguments = MessageHelper.copyOfRange(args, 3, args.length);
			}
			
			AliasCommand aliasCmd = new AliasCommand(cmd, cmdName, cmdArguments);
			try {
				this.bot.addAlias(alias, aliasCmd);
			} catch (IllegalArgumentException e) {
				return sender + ": " + e.getMessage();
			}
			return "created alias: " + alias + aliasCmd.getHelp();
		}
	}

	public String getHelp() {
		return " [<alias> [<command>]] - defines a new alias, deletes one or lists all existing aliases";
	}

	/**
	 * An alias.
	 */
	public static class AliasCommand implements BotCommand {

		private final BotCommand command;
		private final String commandName;
		private final String[] arguments;

		public AliasCommand(BotCommand cmd, String commandName, String[] arguments) {
			this.command = cmd;
			this.commandName = commandName;
			this.arguments = arguments;
		}
		
		public void executeCommand(IMChat chat, IMMessage message,
				String sender, String[] args) throws IMException {
			String[] dynamicArgs = MessageHelper.copyOfRange(args, 1, args.length);
			
			String[] allArgs = MessageHelper.concat(new String[] {this.commandName}, this.arguments, dynamicArgs);
			System.out.println("Args: " + Arrays.toString(allArgs));
			
			this.command.executeCommand(chat, message, sender, allArgs);
		}

		public String getHelp() {
			String help = " - alias for: '" + this.commandName;
			if (this.arguments.length > 0) {
				help += " " + MessageHelper.join(this.arguments, 0);
			}
			help += "'";
			return help;
		}
	}
}
