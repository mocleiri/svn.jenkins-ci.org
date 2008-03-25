package org.hudson.trayapp;

import java.awt.TrayIcon;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hudson.trayapp.actions.Action;
import org.hudson.trayapp.gui.MainFrame;
import org.hudson.trayapp.gui.Tray;
import org.hudson.trayapp.model.Job;
import org.hudson.trayapp.model.Model;
import org.hudson.trayapp.model.Preferences;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class HudsonTrayApp {

	public static void main(String[] args) {
		HudsonTrayApp.args = args;
		try {
			UIManager.setLookAndFeel(
			UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
		getHudsonTrayAppInstance();
	}
	
	private static String[] args = null;
	
	private static HudsonTrayApp hta = null;
	public static HudsonTrayApp getHudsonTrayAppInstance() {
		if (hta == null) {
			hta = new HudsonTrayApp();
		}
		return hta;
	}
	
	private static String PERSISTENCEFILE = System.getProperty("user.home") + System.getProperty("file.separator") + ".hudson" + System.getProperty("file.separator") + "trayapp" + System.getProperty("file.separator") + "preferences";

	private Model model = null;
	private Preferences prefs = null;
	private Tray tray = null;
	private Timer timer = null;

	public HudsonTrayApp() {
		model = new Model();
		prefs = new Preferences();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputSource inputSource = new InputSource(new FileInputStream(PERSISTENCEFILE));
			Document document = builder.parse(inputSource);
			process(document.getChildNodes());
			inputSource.getByteStream().close();
//				
		} catch (Exception e) {
			e.printStackTrace();
		}
		String serverStart = getStartupServer();
		if (serverStart != null && !model.containsServer(serverStart)) {
			model.addServer(serverStart, "My Server");
		}

		MainFrame mainFrame = MainFrame.getMainFrameInstance();
	    mainFrame.setModel(model);
	    mainFrame.setPreferences(prefs);

	    tray = new Tray();
	    scheduleTimer(prefs.getUpdateFrequency() * 60000, true);
	    
	    // Finally, we need to check that we have a server set up, if not, we'll ask the user to set one up
	    if (model.getServerModelSize() == 0) {
	    	mainFrame.setVisible(true);
	    	mainFrame.getMainTabbedPane().setSelectedIndex(1); // Switch to the Configuration Pane
	    }
	}

	public Model getModel() {
		return model;
	}
	
	public Tray getTray() {
		return tray;
	}
	
	public Preferences getPreferences() {
		return prefs;
	}
	
	public void scheduleTimer(int interval, boolean now) {
		if (timer != null) {
			timer.cancel();
		}
	    timer = new Timer("HudsonTrayUpdater", true);
	    timer.scheduleAtFixedRate(new UpdateTimerTask(), now ? 0l : (long) interval, interval);
	}
	
	public void update() {
		model.update();
		
		Integer colourCurrent = Job.convertColour(model.getWorstColour(false));
		MainFrame.getMainFrameInstance().updateResults();
		tray.setWorstCaseColour(Job.getColour(colourCurrent));
		
		// There is no point doing anything if we're on the first run.
		if (model.getWorstJobs(true).isEmpty()) {
			return;
		}
		Integer colourPrevious = Job.convertColour(model.getWorstColour(true));
		boolean bldChgd = model.getBuildChanged();
		Integer buildChanged = bldChgd ? Job.BUILD_CHANGED : Job.BUILD_UNCHANGED;
		
		if (colourPrevious.intValue() > colourCurrent.intValue()) {
			tray.showMessage("Build Worsening", "The overall build status has degraded from\n"+Job.getColour(colourPrevious) + " to " + Job.getColour(colourCurrent), TrayIcon.MessageType.ERROR);
		} else if (colourPrevious.intValue() < colourCurrent.intValue()) {
			tray.showMessage("Build Improving", "The overall build status has improved from\n"+Job.getColour(colourPrevious) + " to " + Job.getColour(colourCurrent), TrayIcon.MessageType.INFO);
		} else if (bldChgd && colourCurrent.intValue() < Job.BLUE.intValue()) {
			List<Job> lstLeftJobs = model.getJobsLeftWorstBuild();
			List<Job> lstJoinedJobs = model.getJobsJoinedWorstBuild();
			if (lstLeftJobs.isEmpty() && lstJoinedJobs.isEmpty()) {
				tray.showMessage("Build No Improvement", "The overall build has changed, but this has neither made an improvement or degredation.", TrayIcon.MessageType.INFO);
			} else if (lstLeftJobs.isEmpty()) {
				// We haven't had any jobs leave which means that some have joined our status.
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < lstJoinedJobs.size(); i++) {
					sb.append("\n"+lstJoinedJobs.get(i).getName());
				}
				tray.showMessage("Jobs have Joined Worst Colour", "The overall build worst colour hasn't changed, but the following projects have worsened, and joined this colour:" + sb.toString(), TrayIcon.MessageType.ERROR);
			} else if (lstJoinedJobs.isEmpty()) {
				// We haven't had any jobs join which means that some have left our status.
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < lstLeftJobs.size(); i++) {
					sb.append("\n"+lstLeftJobs.get(i).getName());
				}
				tray.showMessage("Jobs have left Worst Colour", "The overall build worst colour hasn't changed, but the following projects have improved away from this colour:" + sb.toString(), TrayIcon.MessageType.INFO);
			} else {
				// We had jobs join and leave our status.
				StringBuilder sbleft = new StringBuilder();
				for (int i = 0; i < lstLeftJobs.size(); i++) {
					sbleft.append("\n"+lstLeftJobs.get(i).getName());
				}
				StringBuilder sbjoin = new StringBuilder();
				for (int i = 0; i < lstJoinedJobs.size(); i++) {
					sbjoin.append("\n"+lstJoinedJobs.get(i).getName());
				}
				tray.showMessage("Jobs have joined and left Worst Colour", "The overall build worst colour hasn't changed, but the following projects have worsened, and joined this colour:" + sbjoin.toString() + "\nThe following projects have improved away from this colour:" + sbleft.toString(), TrayIcon.MessageType.INFO);
			}
		}
		// OK we've got the full status, let's fire the actions
		Action action = prefs.getAction(colourPrevious, colourCurrent, buildChanged);
		if (action != null) {
			action.fireAction(colourPrevious, colourCurrent, buildChanged);
		}
	}
	
	public void write() {
		try {
			File f = new File(PERSISTENCEFILE);
			f.delete();
			f.getParentFile().mkdirs();
			f.createNewFile();
			Writer w = new FileWriter(PERSISTENCEFILE);
			writeXML(w);
			w.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void writeXML(Writer w) throws IOException{
		w.write("<hudsontrayapp>");
		prefs.writeXML(w);
		model.writeXML(w);
		w.write("</hudsontrayapp>");
	}
	
	private void process(NodeList nodes) {
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			String name = node.getNodeName();
			if (name.equals("hudsontrayapp")) {
				process(node.getChildNodes());
			} else if (name.equals("preferences")) {
				prefs.process(node.getChildNodes());
			} else if (name.equals("model")) {
				model.process(node.getChildNodes());
			}
		}
	}
	
	private static String getStartupServer() {
		for (int i = 0; i < args.length; i++) {
			if (args[i].indexOf("server=") != -1) {
				return args[i].substring("server=".length());
			}
		}
		return null;
	}
	
	private class UpdateTimerTask extends TimerTask {
		public void run() {
			update();
		}
	}
}
