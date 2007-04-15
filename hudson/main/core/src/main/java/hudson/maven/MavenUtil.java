package hudson.maven;

import hudson.model.TaskListener;
import hudson.model.BuildListener;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class MavenUtil {
    /**
     * Creates a fresh {@link MavenEmbedder} instance.
     *
     * @param listener
     *      This is where the log messages from Maven will be recorded.
     */
    public static MavenEmbedder createEmbedder(TaskListener listener) throws MavenEmbedderException, IOException {
        MavenEmbedder maven = new MavenEmbedder();

        ClassLoader cl = MavenUtil.class.getClassLoader();
        maven.setClassLoader(new MaskingClassLoader(cl));
        maven.setLogger( new EmbedderLoggerImpl(listener) );

        // make sure ~/.m2 exists to avoid http://www.nabble.com/BUG-Report-tf3401736.html
        File m2Home = new File(MavenEmbedder.userHome, ".m2");
        m2Home.mkdirs();
        if(!m2Home.exists()) {
            listener.getLogger().println("Failed to create "+m2Home+
                "\nSee https://hudson.dev.java.net/cannot-create-.m2.html");
            throw new AbortException();
        }

        maven.start();

        return maven;
    }

    /**
     * Recursively resolves module POMs that are referenced from
     * the given {@link MavenProject} and parses them into
     * {@link MavenProject}s.
     *
     * @param rel
     *      Used to compute the relative path. Pass in "" to begin.
     * @param relativePathInfo
     *      Upon the completion of this method, this variable stores the relative path
     *      from the root directory of the given {@link MavenProject} to the root directory
     *      of each of the newly parsed {@link MavenProject}.
     *
     * @throws AbortException
     *      errors will be reported to the listener and the exception thrown.
     */
    public static void resolveModules(MavenEmbedder embedder, MavenProject project, String rel, Map<MavenProject,String> relativePathInfo, BuildListener listener) throws ProjectBuildingException, AbortException {

        File basedir = project.getFile().getParentFile();
        relativePathInfo.put(project,rel);

        List<MavenProject> modules = new ArrayList<MavenProject>();

        for (String modulePath : (List<String>) project.getModules()) {
            File moduleFile = new File(new File(basedir, modulePath),"pom.xml");
            if(!moduleFile.exists()) {
                listener.getLogger().println(moduleFile+" is referenced from "+project.getFile()+" but it doesn't exist");
                throw new AbortException();
            }

            String relativePath = rel;
            if(relativePath.length()>0) relativePath+='/';
            relativePath+=modulePath;

            MavenProject child = embedder.readProject(moduleFile);
            resolveModules(embedder,child,relativePath,relativePathInfo,listener);
            modules.add(child);
        }

        project.setCollectedProjects(modules);
    }

    /**
     * When we run in Jetty during development, embedded Maven will end up
     * seeing some of the Maven class visible through Jetty, and this confuses it.
     *
     * <p>
     * Specifically, embedded Maven will find all the component descriptors
     * visible through Jetty, yet when it comes to loading classes, classworlds
     * still load classes from local realms created inside embedder.
     *
     * <p>
     * This classloader prevents this issue by hiding the component descriptor
     * visible through Jetty.
     */
    private static final class MaskingClassLoader extends ClassLoader {

        public MaskingClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Enumeration<URL> getResources(String name) throws IOException {
            final Enumeration<URL> e = super.getResources(name);
            return new Enumeration<URL>() {
                URL next;

                public boolean hasMoreElements() {
                    fetch();
                    return next!=null;
                }

                public URL nextElement() {
                    fetch();
                    URL r = next;
                    next = null;
                    return r;
                }

                private void fetch() {
                    while(next==null && e.hasMoreElements()) {
                        next = e.nextElement();
                        if(next.toExternalForm().contains("maven-plugin-tools-api"))
                            next = null;
                    }
                }
            };
        }
    }
}
