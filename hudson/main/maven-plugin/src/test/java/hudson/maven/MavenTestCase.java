package hudson.maven;

import org.jvnet.hudson.test.HudsonTestCase;

import java.io.IOException;

/**
 * @author huybrechts
 */
public abstract class MavenTestCase extends HudsonTestCase {

    /**
     * Creates a empty Maven project with an unique name.
     *
     * @see #configureDefaultMaven()
     */
    protected MavenModuleSet createMavenProject() throws IOException {
        return createMavenProject(createUniqueProjectName());
    }

    /**
     * Creates a empty Maven project with the given name.
     *
     * @see #configureDefaultMaven()
     */
    protected MavenModuleSet createMavenProject(String name) throws IOException {
        return (MavenModuleSet)hudson.createProject(MavenModuleSet.DESCRIPTOR,name);
    }

}
