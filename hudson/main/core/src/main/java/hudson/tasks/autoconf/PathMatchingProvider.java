package hudson.tasks.autoconf;

import hudson.model.AutomaticConfiguration;

import java.io.Serializable;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 *
 * @author connollys
 * @since 28-May-2007 12:54:02
 */
public interface PathMatchingProvider extends Serializable {
    /**
     * This should perform a quick check that the path tree is one that can be inspected.
     * We use an array of File to allow some directory naming checks to be optimized.
     * @param pathStack The stack of directories that have been navigated.
     * @param subdirs The collection of immediate subdirectories
     * @param files The immediate files
     * @return true if and only if inspect should be called.
     */
    boolean isIterested(File[] pathStack, File[] subdirs, File[] files);

    /**
     * The maximum path depth that this provider considers useful.
     * @return the path depth.
     */
    int getMaxUsefulDepth();

    /**
     * This should perform a thourough check that the path tree contains the require files for this configuration.
     * @param path
     * @return
     */
    AutomaticConfiguration inspect(File path);
}
