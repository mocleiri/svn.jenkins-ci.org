package hudson.scm;

import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.Launcher;
import hudson.FilePath;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Captures the configuration information in it.
 *
 * @author Kohsuke Kawaguchi
 */
public interface SCM {

    /**
     * Calculate changelog for the given build and write it to the specified file.
     *
     * @return
     *      false if the operation fails. The error should be reported to the listener.
     */
    boolean calcChangeLog(Build build, File changelogFile, Launcher launcher, BuildListener listener) throws IOException;

    /**
     * Obtains a fresh workspace of the module(s) into the specified directory
     * of the specified machine.
     *
     * <p>
     * This operation should also capture the information necessary to tag the workspace later.
     *
     * @param launcher
     *      Abstracts away the machine that the files will be checked out.
     * @param dir
     *      a directory to check out the source code. May contain left-over
     *      from the previous build.
     * @return
     *      false if the operation fails. The error should be reported to the listener.
     */
    boolean checkout(Build build, Launcher launcher, FilePath dir, BuildListener listener) throws IOException;

    /**
     * Gets the descriptor for this instance.
     */
    SCMDescriptor getDescriptor();

    /**
     * Adds environmental variables for the builds to the given map.
     */
    void buildEnvVars(Map env);

    /**
     * Gets the name of the module, or the empty string.
     */
    String getModule();
}
