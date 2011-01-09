package org.jfrog.hudson.util;

import hudson.model.AbstractBuild;
import hudson.tasks.LogRotator;
import org.jfrog.build.api.BuildRetention;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Tomer Cohen
 */
public class BuildRetentionFactory {

    /**
     * Create a Build retention object out of the build
     *
     * @param build The build to create the build retention out of
     * @return a new Build retention
     */
    public static BuildRetention createBuildRetention(AbstractBuild build) {
        BuildRetention buildRetention = new BuildRetention();
        LogRotator rotator = build.getProject().getLogRotator();
        if (rotator == null) {
            return buildRetention;
        }
        if (rotator.getNumToKeep() > -1) {
            buildRetention.setCount(rotator.getNumToKeep());
        }
        if (rotator.getDaysToKeep() > -1) {
            Calendar calendar = Calendar.getInstance();
            calendar.roll(Calendar.DAY_OF_MONTH, -rotator.getDaysToKeep());
            buildRetention.setMinimumBuildDate(new Date(calendar.getTimeInMillis()));
        }
        return buildRetention;
    }
}
