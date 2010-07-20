package com.thalesgroup.dtkit.maven;

import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Generate some Hudson source file from generic input type
 *
 * @goal packager
 */
public class PackagerHudsonMojo extends AbstractSourceJarMojo {

    public PackagerHudsonMojo(){
    }

    /**
     * {@inheritDoc}
     */
    protected List getSources(MavenProject p) {
        return Arrays.asList(new String[]{p.getBuild().getOutputDirectory()});
    }

    /**
     * {@inheritDoc}
     */
    protected List getResources(MavenProject p) {

        return Collections.EMPTY_LIST;
    }

    /**
     * {@inheritDoc}
     */
    protected String getClassifier() {
        return "hudson";
    }
}

