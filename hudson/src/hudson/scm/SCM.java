package hudson.scm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Describable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Captures the configuration information in it.
 *
 * @author Kohsuke Kawaguchi
 */
public interface SCM extends Describable<SCM> {

    ///**
    // * Calculate changelog for the given build and write it to the specified file.
    // *
    // * @return
    // *      false if the operation fails. The error should be reported to the listener.
    // */
    //boolean calcChangeLog(Build build, File changelogFile, Launcher launcher, BuildListener listener) throws IOException;

    /**
     * Obtains a fresh workspace of the module(s) into the specified directory
     * of the specified machine.
     *
     * <p>
     * The "update" operation can be performed instead of a fresh checkout if
     * feasible.
     *
     * <p>
     * This operation should also capture the information necessary to tag the workspace later.
     *
     * @param launcher
     *      Abstracts away the machine that the files will be checked out.
     * @param dir
     *      a directory to check out the source code. May contain left-over
     *      from the previous build.
     * @param changelogFile
     *      Upon a successful return, this file should capture the changelog.
     * @return
     *      null if the operation fails. The error should be reported to the listener.
     *      Otherwise return the changes included in this update (if this was an update.)
     */
    boolean checkout(Build build, Launcher launcher, FilePath dir, BuildListener listener, File changelogFile) throws IOException;

    /**
     * Adds environmental variables for the builds to the given map.
     */
    void buildEnvVars(Map env);

    /**
     * Gets the top directory of the checked out module.
     * @param workspace
     */
    FilePath getModuleRoot(FilePath workspace);

    /**
     * The returned object will be used to parse <tt>changelog.xml</tt>.
     */
    ChangeLogParser createChangeLogParser();
}
