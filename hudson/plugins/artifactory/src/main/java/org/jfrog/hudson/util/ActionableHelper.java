package org.jfrog.hudson.util;

import hudson.maven.AbstractMavenBuild;
import hudson.maven.MavenBuild;
import hudson.maven.reporters.MavenArtifactRecord;
import hudson.model.Action;

import java.util.List;

/**
 * @author Yossi Shaul
 */
public class ActionableHelper {

    public static MavenArtifactRecord getLatestMavenArtifactRecord(MavenBuild mavenBuild) {
        return getLatestAction(mavenBuild, MavenArtifactRecord.class);
    }

    public static <T extends Action> T getLatestAction(AbstractMavenBuild mavenBuild, Class<T> actionClass) {
        // one module may produce multiple action entries of the same type, the last one contains all the info we need
        // (previous ones might only contain partial information, eg, only main artifact)
        List<T> records = mavenBuild.getActions(actionClass);
        if (records == null || records.isEmpty()) {
            return null;
        } else {
            return records.get(records.size() - 1);
        }
    }
}
