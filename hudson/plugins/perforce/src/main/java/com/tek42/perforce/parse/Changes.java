package com.tek42.perforce.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.tek42.perforce.Depot;
import com.tek42.perforce.PerforceException;
import com.tek42.perforce.model.Changelist;

/**
 * Base API object for interacting with changelists
 * 
 * @author Mike Wille
 * 
 * Change history: 11/3/07, Brian Westrich: Add workspace support to
 * getChangeNumbersTo.
 * 
 */
public class Changes extends AbstractPerforceTemplate {

	public Changes(Depot depot) {
		super(depot);
	}

	/**
	 * Returns a single changelist specified by its number.
	 * 
	 * @param number
	 * @return
	 * @throws PerforceException
	 */
	public Changelist getChangelist(int number) throws PerforceException {
		String id = new Integer(number).toString();
		ChangelistBuilder builder = new ChangelistBuilder();
		Changelist change = builder.build(getPerforceResponse(builder
				.getBuildCmd(id)));
		if (change == null)
			throw new PerforceException("Failed to retrieve changelist "
					+ number);
		return change;
	}

	/**
	 * Returns a list of changelists that match the parameters
	 * 
	 * @param path
	 *            What point in the depot to show changes for?
	 * @param lastChange
	 *            The last changelist number to start from
	 * @param limit
	 *            The maximum changes to return if less than 1, will return
	 *            everything
	 * @return
	 * @throws PerforceException
	 */
	public List<Changelist> getChangelists(String path, int lastChange,
			int limit) throws PerforceException {
		path = normalizePath(path);
		if (lastChange > 0)
			path += "@" + lastChange;

		String cmd[];

		if (limit > 0)
			cmd = new String[] { "p4", "changes", "-m",
					new Integer(limit).toString(), path };
		else
			cmd = new String[] { "p4", "changes", path };

		StringBuilder response = getPerforceResponse(cmd);
		List<String> ids = parseList(response, 1);

		List<Changelist> changes = new ArrayList<Changelist>();
		for (String id : ids) {
			changes.add(getChangelist(new Integer(id)));
		}
		return changes;
	}

	/**
	 * A lightweight call to return changelist numbers for a given path.
	 * <p>
	 * To get the latest change in the depot for the project, you can use:
	 * 
	 * <pre>
	 * depot.getChangeNumbers(&quot;//project/...&quot;, -1, 1)
	 * </pre>
	 * 
	 * <p>
	 * Note: this method follows perforce in that it starts at the highest
	 * number and works backwards. So this might not be what you want. (It
	 * certainly isn't for Hudson)
	 * 
	 * @param path
	 *            Path to filter on
	 * @param start
	 *            The number of the change to start from
	 * @param limit
	 *            The number of changes to return
	 * @return
	 * @throws PerforceException
	 */
	public List<Integer> getChangeNumbers(String path, int start, int limit)
			throws PerforceException {
		path = normalizePath(path);
		if (start > 0)
			path += "@" + start;

		String cmd[];

		if (limit > 0)
			cmd = new String[] { "p4", "changes", "-m",
					new Integer(limit).toString(), path };
		else
			cmd = new String[] { "p4", "changes", path };

		StringBuilder response = getPerforceResponse(cmd);
		List<String> ids = parseList(response, 1);
		List<Integer> numbers = new ArrayList<Integer>(ids.size());
		for (String id : ids) {
			numbers.add(new Integer(id));
		}
		return numbers;
	}

	/**
	 * Returns a list of changenumbers that start with the most recent change
	 * and work back to the specified change.
	 * 
	 * @param path
	 * @param untilChange
	 * @return
	 */
	public List<Integer> getChangeNumbersTo(String path, int untilChange)
			throws PerforceException {

		return getChangeNumbersTo(null, path, untilChange);

	}

	/**
	 * Returns a list of changenumbers that start with the most recent change
	 * and work back to the specified change.
	 * 
	 * @param workspace
	 * @param path
	 *            one or more paths, e.g. "//testproject/... //testfw/...".
	 *            Paths are assumed to be delimited by a single space.
	 * @param untilChange
	 * @return
	 */
	public List<Integer> getChangeNumbersTo(String workspace, String path,
			int untilChange) throws PerforceException {
		String DELIM = " ";

		// maximum number of paths per command supported by perforce
		// note that command line perforce supports up to three, but p4java only
		// supports one.
		int MAX_PATHS_SUPPORTED_PER_COMMAND = 1;

		StringTokenizer allPaths = new StringTokenizer(path, DELIM);
		List<String> supportedPaths = new ArrayList<String>();
		StringBuilder currentPaths = new StringBuilder("");
		int numberOfPathsInCurrentPaths = 0;
		while (true) {
			if (!allPaths.hasMoreTokens()) {
				if (currentPaths.length() > 0) {
					supportedPaths.add(currentPaths.toString().trim());
				}
				break;
			}
			String nextPath = allPaths.nextToken();
			currentPaths.append(nextPath + " ");
			numberOfPathsInCurrentPaths++;
			if (numberOfPathsInCurrentPaths == MAX_PATHS_SUPPORTED_PER_COMMAND) {
				supportedPaths.add(currentPaths.toString().trim());
				currentPaths.setLength(0);
				numberOfPathsInCurrentPaths = 0;
			}
		}
		Set<Integer> uniqueIds = new HashSet<Integer>();
		for (String pathToUse : supportedPaths) {
			List<Integer> ids = getChangeNumbersToForSinglePath(workspace,
					pathToUse, untilChange);
			uniqueIds.addAll(ids);
		}
		List<Integer> sortedIds = new ArrayList<Integer>(uniqueIds);
		Collections.sort(sortedIds, Collections.reverseOrder());
		return sortedIds;
	}

	/**
	 * Returns a list of changenumbers that start with the most recent change
	 * and work back to the specified change.
	 * 
	 * @param workspace
	 * @param path
	 *            a single path, e.g. //testproject/...
	 * @param untilChange
	 * @return
	 */
	private List<Integer> getChangeNumbersToForSinglePath(String workspace,
			String path, int untilChange) throws PerforceException {
		path = normalizePath(path);

		List<String> cmdList = new ArrayList<String>();

		addCommand(cmdList, "p4", "changes", "-m", "25");
		addCommandWorkspace(cmdList, workspace);
		addCommand(cmdList, path);

		List<Integer> ids = new ArrayList<Integer>();
		int counter = 0;
		String lastChange;
		OUTER: while (true) {
			// System.out.println("Looping: " + counter++);
			StringBuilder response;
			try {
				// getPerforceResponse will throw an exception if a command it
				// executes
				// returns nothing from perforce. If we are moving back through
				// a list and have
				// less change lists in the history then what was specified, we
				// will hit this
				// exception
				response = getPerforceResponse(cmdList
						.toArray(new String[cmdList.size()]));
			} catch (PerforceException e) {
				if (e.getMessage().startsWith("No output for"))
					break OUTER;
				throw e;
			}
			List<String> temp = parseList(response, 1);
			if (temp.size() == 0)
				break;
			for (String num : temp) {
				if (new Integer(num) >= untilChange)
					ids.add(new Integer(num));
				else
					break OUTER;
			}
			lastChange = temp.get(temp.size() - 1);
			int next = new Integer(lastChange) - 1;
			cmdList.clear();
			addCommand(cmdList, "p4", "changes", "-m", "25");
			addCommandWorkspace(cmdList, workspace);
			addCommand(cmdList, path + "@" + next);
		}
		return ids;
	}

	/**
	 * Add workspace to the command.
	 * 
	 * @param cmdList
	 * @param workspace
	 */
	private void addCommandWorkspace(List<String> cmdList, String workspace) {
		if (workspace != null) {
			addCommand(cmdList, "-c", workspace);
		}
	}

	/**
	 * translate the path into a p4 acceptable format.
	 * 
	 * @param path
	 *            the path
	 * @return the normalized path
	 */
	private String normalizePath(String path) {
		if (path == null || path.equals(""))
			path = "//...";
		return path;
	}

	/**
	 * add one or more parameters to a command
	 * 
	 * @param list
	 *            the command
	 * @param args
	 *            the parameters to add
	 */
	private void addCommand(List<String> list, String... args) {
		for (String command : args) {
			list.add(command);
		}
	}

	/**
	 * Converts a list of numbers to a list of changes.
	 * 
	 * @param numbers
	 * @return
	 * @throws PerforceException
	 */
	public List<Changelist> getChangelistsFromNumbers(List<Integer> numbers)
			throws PerforceException {
		List<Changelist> changes = new ArrayList<Changelist>();
		for (Integer id : numbers) {
			changes.add(getChangelist(id));
		}
		return changes;
	}

//	/**
//	 * Patched version of same method in AbstractPerforceTemp. Patched since
//	 * original version was not always returning results, e.g. for the command
//	 * p4 changes -m 25 -c Brian-mcwest5 //testfw/... no results were returned.
//	 * 
//	 * @param cmd
//	 * @return
//	 * @throws PerforceException
//	 */
//	protected StringBuilder getPerforceResponse(String cmd[])
//			throws PerforceException {
//		String[] mesg = { "Connect to server failed; check $P4PORT",
//				"Perforce password (P4PASSWD) invalid or unset.",
//				"Can't create a new user - over license quota.",
//				"Access for user '" };
//		boolean loop = false;
//		int loopCounter = 0;
//		StringBuilder response = new StringBuilder();
//		do {
//			int mesgIndex = -1, i, count = 0;
//			Executor p4 = depot.getExecFactory().newExecutor();
//			String debugcmd = "";
//			try {
//				for (String cm : cmd) {
//					debugcmd += cm + " ";
//				}
//				p4.exec(cmd);
//
//				BufferedReader reader = p4.getReader();
//				String line;
//				response = new StringBuilder();
//				while ((line = reader.readLine()) != null) {
//					count++;
//					for (i = 0; i < mesg.length; i++) {
//						if (line.indexOf(mesg[i]) != -1)
//							mesgIndex = i;
//					}
//					response.append(line + "\n");
//				}
//				if (loopCounter == 0 && mesgIndex == 1) {
//					// password is unset means that perforce isn't using the
//					// environment var P4PASSWD
//					// Instead it is using tickets. We must attempt to login via
//					// p4 login, then
//					// retry this cmd.
//					p4.close();
//					login();
//					loop = true;
//					loopCounter++;
//					continue;
//				}
//
//				// the way the warnings are currently setup doesn't work for
//				// some new messages...
//				if (mesgIndex == 3)
//					throw new PerforceException("Access for user '"
//							+ depot.getUser()
//							+ "' has not been enabled by 'p4 protect'");
//				if (mesgIndex != -1)
//					throw new PerforceException(mesg[mesgIndex]);
//				if (count == 0)
//					throw new PerforceException("No output for: " + debugcmd);
//
//			} catch (IOException e) {
//				throw new PerforceException("Failed to communicate with p4", e);
//			} finally {
//				p4.close();
//			}
//		} while (loop);
//		return response;
//	}

}
