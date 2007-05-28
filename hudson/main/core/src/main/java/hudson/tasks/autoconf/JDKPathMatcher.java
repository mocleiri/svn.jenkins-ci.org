package hudson.tasks.autoconf;

import hudson.Functions;
import hudson.model.AbstractAutomaticConfiguration;
import hudson.model.AutomaticConfiguration;
import hudson.util.StreamCopyThread;
import org.codehaus.plexus.util.StringOutputStream;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 *
 * @author connollys
 * @since 28-May-2007 13:06:35
 */
public class JDKPathMatcher implements PathMatchingProvider {
    public static final JDKPathMatcher INSTANCE = new JDKPathMatcher();

    /**
     * This should perform a quick check that the path tree is one that can be inspected. We use an array of File to
     * allow some directory naming checks to be optimized.
     *
     * @param pathStack The stack of directories that have been navigated.
     * @param subdirs   The collection of immediate subdirectories
     * @param files     The immediate files
     * @return true if and only if inspect should be called.
     */
    public boolean isIterested(File[] pathStack, File[] subdirs, File[] files) {
        boolean hasJRE = false;
        boolean hasBin = false;
        for (File subdir : subdirs) {
            if ("jre".equalsIgnoreCase(subdir.getName())) {
                hasJRE = true;
            }
            if ("bin".equalsIgnoreCase(subdir.getName())) {
                hasBin = true;
            }
        }
        return hasBin && hasJRE;
    }

    /**
     * The maximum path depth that this provider considers useful.
     *
     * @return the path depth.
     */
    public int getMaxUsefulDepth() {
        return 4;
    }

    /**
     * This should perform a thourough check that the path tree contains the require files for this configuration.
     *
     * @param pathStack
     * @return
     */
    public AutomaticConfiguration inspect(File path) {
        String postExecutable = Functions.isWindows() ? ".exe" : "";
        File binDir = new File(path, "bin");
        File jdkJava = new File(binDir, "java" + postExecutable);
        File jdkJavaC = new File(binDir, "javac" + postExecutable);
        File jreDir = new File(path, "jre");
        File jreBinDir = new File(jreDir, "bin");
        File jreJava = new File(jreBinDir, "java" + postExecutable);
        if (jdkJava.exists() && jdkJavaC.exists() && jreJava.exists()) {
            // we have found a jdk
            String[] cmd = { jdkJava.getAbsolutePath(), "-version"};
            try {
                StringOutputStream os = new StringOutputStream();
                StringOutputStream er = new StringOutputStream();
                Process proc = Runtime.getRuntime().exec(cmd);
                new StreamCopyThread("java-version-finder-stdout", proc.getInputStream(), os).start();
                new StreamCopyThread("java-version-finder-stderr", proc.getErrorStream(), er).start();
                proc.waitFor();
                String output = er.toString();
                Pattern pattern = Pattern.compile("java version \"([^\"]+)\".*");
                Matcher matcher = pattern.matcher(output);
                if (matcher.find()) {
                    // bingo;
                    String version = matcher.group(1).replace('_', '.');
                    return new JDKSystemConfiguration("jdk_" + version, path.getAbsolutePath(), version);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public static class JDKSystemConfiguration extends AbstractAutomaticConfiguration {
        private final String jdkHome;
        private final String version;

        public JDKSystemConfiguration(String name, String jdkHome, String version) {
            super(name);
            if (jdkHome.endsWith(File.separator)) {
                jdkHome = jdkHome.substring(0, jdkHome.length() - File.separator.length());
            }
            this.jdkHome = jdkHome;
            this.version = version;
        }

        public String getVersion() {
            return version;
        }

        public String getHome() {
            return jdkHome;
        }

        /**
         * Configure the environment for this configuration.
         *
         * @param environment The environment to configure.
         * @return An unmodifiable map containing the configured environment.
         */
        public Map<String, String> configureEnvironment(Map<String, String> environment) {
            Map<String, String> env = new HashMap<String, String>(super.configureEnvironment(environment));
            env.put("JAVA_HOME", jdkHome);
            String path = env.get("PATH");
            env.put("PATH", jdkHome + File.separator + "bin" + File.pathSeparator + path);
            return Collections.unmodifiableMap(environment);
        }
    }

}
