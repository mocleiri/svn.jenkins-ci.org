package hudson.plugins.japex;

import hudson.model.Action;
import hudson.model.Build;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * {@link Action} contributed to a {@link Build} to display
 * the regression information.
 *
 * @author Kohsuke Kawaguchi
 */
public class JapexReportBuildAction implements Action {
    public final Build owner;

    public JapexReportBuildAction(Build owner) {
        this.owner = owner;
    }

    public Build getOwner() {
        return owner;
    }

    public String getDisplayName() {
        return "Japex Regression Report";
    }

    public String getIconFileName() {
        return "graph.gif";
    }

    public String getUrlName() {
        return "japex";
    }

    /**
     * Gets the list of all regression reports.
     * @return can be empty but never null. 
     */
    public List<URL> getRegressionReports() throws MalformedURLException {
        File dir = JapexPublisher.getJapexReport(owner);
        File[] reports = dir.listFiles(REGRESSION_FILTER);
        if(reports==null)
            return Collections.EMPTY_LIST;

        List<URL> urls = new ArrayList<URL>();
        for (File f : reports) {
            urls.add(f.toURL());
        }
        
        return urls;
    }

    private static final FileFilter REGRESSION_FILTER = new FileFilter() {
        public boolean accept(File f) {
            return f.getName().endsWith(".regression");
        }
    };
}
