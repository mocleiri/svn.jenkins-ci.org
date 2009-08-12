package hudson.plugins.perforce;

import hudson.FilePath;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * Perforce path string manipulation utility methods.
 *
 * @author Brian Westrich
 */
public final class PerforceSCMHelper {

    private static final String DEPOT_ROOT = "//";
    private static final String EXCLUSION_VIEW_PREFIX = "-";

    private PerforceSCMHelper() {
        // static methods, do not instantiate
    }

    /**
     * Generate a path for the changes command based on a workspaces views.
     *
     * @param views
     * @return
     */
    static String computePathFromViews(Collection<String> views) {

        StringBuilder path = new StringBuilder("");

        for (String view : views) {
            StringTokenizer columns = new StringTokenizer(view, " ");
            String leftColumn = columns.nextToken().trim();
            if (leftColumn.indexOf(EXCLUSION_VIEW_PREFIX + DEPOT_ROOT) != -1) {
                continue;
            }
            leftColumn = leftColumn.substring(leftColumn.indexOf(DEPOT_ROOT));
            path.append(leftColumn + " ");
        }

        return path.toString();
    }

    /**
     * Assuming there are multiple views, see whether the project path is valid.
     *
     * @param projectPath the project path specified by the user.
     * @return true if valid, false if invalid
     */
    static boolean projectPathIsValidForMultiviews(String projectPath) {
        return projectPath.equals("//...") // root of depot ok
                || projectPath.indexOf('@') > -1; // labels ok {
    }

    /**
     * Perform some manipulation on the workspace URI to get a valid local path
     * <p>
     * Is there an issue doing this?  What about remote workspaces?  does that happen?
     *
     * @param path
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    static String getLocalPathName(FilePath path, boolean isUnix) throws IOException, InterruptedException {
        String uriString = path.toURI().toString();
        // Get rid of URI prefix
        // NOTE: this won't handle remote files, is that a problem?
        uriString = uriString.replaceAll("file:/", "");
        // It seems there is a /./ to denote the root in the path on my test instance.
        // I don't know if this is in production, or how it works on other platforms (non win32)
        // but I am removing it here because perforce doesn't like it.
        uriString = uriString.replaceAll("/./", "/");
        // The URL is also escaped.  We need to unescape it because %20 in path names isn't cool for perforce.
        uriString = URLDecoder.decode(uriString, "UTF-8");

        // Last but not least, we need to convert this to local path separators.
        if (isUnix) {
            // on unixen we need to prepend with /
            uriString = "/" + uriString;
        } else {
            // just replace with sep doesn't work because Java's regexp replaceAll
            uriString = uriString.replaceAll("/", "\\\\");
        }

        return uriString;
    }

}
