package hudson.tasks.autoconf;

import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.ConfigurationProvider;
import hudson.model.AutomaticConfiguration;

import java.util.*;
import java.io.File;
import java.io.FileFilter;

/**
 * Created by IntelliJ IDEA.
 *
 * @author connollys
 * @since 25-May-2007 15:25:03
 */
public class PathSearcher extends ConfigurationProvider {

    public static PathSearcher INSTANCE = new PathSearcher();
    public static final List<PathMatchingProvider> MATCHERS = new ArrayList<PathMatchingProvider>();
    static {
        MATCHERS.add(JDKPathMatcher.INSTANCE);
    }

    private PathSearcher() {
    }

    /**
     * Find the labels that the node supports.
     *
     * @return a set of labels.
     */
    public Set<AutomaticConfiguration> getAvailableConfigurations(VirtualChannel channel) {
        try {
            return channel.call(new JDKFinder(MATCHERS));
        } catch (Exception e) {
            return new HashSet<AutomaticConfiguration>();
        }
    }

    private static class JDKFinder implements Callable<Set<AutomaticConfiguration>, Exception> {
        private final List<PathMatchingProvider> matchers;
        private final int maxDepth;

        public JDKFinder(List<PathMatchingProvider> matchers) {
            this.matchers = matchers;
            int maxDepth = 0;
            for (PathMatchingProvider provider : matchers) {
                if (provider.getMaxUsefulDepth() > maxDepth) {
                    maxDepth = provider.getMaxUsefulDepth();
                }
            }
            this.maxDepth = maxDepth;
        }

        /** Performs computation and returns the result, or throws some exception. */
        public Set<AutomaticConfiguration> call() throws Exception {
            Set<AutomaticConfiguration> result = new HashSet<AutomaticConfiguration>();
            File root = new File(File.separator);
            File[] path = { root };
            doSearch(result, path);
            return result;
        }

        private void doSearch(Set<AutomaticConfiguration> result, File[] path) {
            if (path.length >= maxDepth) {
                return;
            }
            final FileFilter dirFilter = new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            };
            final FileFilter fileFilter = new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isFile();
                }
            };
            final File[] newPath = Arrays.copyOf(path, path.length + 1);
            final File curDir = path[path.length - 1];
            final File[] dirs = curDir.listFiles(dirFilter);
            for (File dir: dirs) {
                newPath[path.length] = dir;

                if (".svn".equalsIgnoreCase(dir.getName())) {
                    break;
                }
                if ("CVS".equalsIgnoreCase(dir.getName())) {
                    break;
                }
                // recurse
                doSearch(result, newPath);

                // now do the local checks
                final File[] subDirs = dir.listFiles(dirFilter);
                final File[] files = dir.listFiles(fileFilter);
                for (PathMatchingProvider matcher: matchers) {
                    if (matcher.isIterested(newPath, subDirs, files)) {
                        AutomaticConfiguration config = matcher.inspect(dir);
                        if (null != config) {
                            result.add(config);
                        }
                    }
                }
            }
        }
    }

}