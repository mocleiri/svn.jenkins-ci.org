/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Martin Eigenbrodt
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.tasks;

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.scm.SCM;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import java.util.logging.Logger;

/**
 * Deletes old log files.
 *
 * TODO: is there any other task that follows the same pattern?
 * try to generalize this just like {@link SCM} or {@link BuildStep}.
 *
 * @author Kohsuke Kawaguchi
 */
public class LogRotator implements Describable<LogRotator> {

    /**
     * If not -1, history is only kept up to this days.
     */
    private final int daysToKeep;

    /**
     * If not -1, only this number of build logs are kept.
     */
    private final int numToKeep;

    /**
     * If not -1, artifacts are only kept up to this days.
     */
    private final String artifactDaysToKeep;

    /**
     * If not -1, only this number of builds have their artifacts kept.
     */
    private final String artifactNumToKeep;

    @DataBoundConstructor
    public LogRotator (String logrotate_days, String logrotate_nums, String logrotate_artifact_days, String logrotate_artifact_nums) {
        this (parse(logrotate_days),parse(logrotate_nums),
              logrotate_artifact_days,logrotate_artifact_nums);     	
    }

    public static int parse(String p) {
        if(p==null)     return -1;
        try {
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * @deprecated since 1.350
     */
    public LogRotator(int daysToKeep, int numToKeep) {
        this(daysToKeep, numToKeep, "-1", "-1");
    }
    
    public LogRotator(int daysToKeep, int numToKeep, String artifactDaysToKeep, String artifactNumToKeep) {
        this.daysToKeep = daysToKeep;
        this.numToKeep = numToKeep;
        this.artifactDaysToKeep = artifactDaysToKeep;
        this.artifactNumToKeep = artifactNumToKeep;
        
    }

    public void perform(Job<?,?> job) throws IOException, InterruptedException {
        LOGGER.log(FINE,"Running the log rotation for "+job.getFullDisplayName());
        
        // keep the last successful build regardless of the status
        Run lsb = job.getLastSuccessfulBuild();
        Run lstb = job.getLastStableBuild();

        if(numToKeep!=-1) {
            Run[] builds = job.getBuilds().toArray(new Run[0]);
            for( int i=numToKeep; i<builds.length; i++ ) {
                Run r = builds[i];
                if (r.isKeepLog()) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not GC-ed because it's marked as a keeper");
                    continue;
                }
                if (r==lsb) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not GC-ed because it's the last successful build");
                    continue;
                }
                if (r==lstb) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not GC-ed because it's the last stable build");
                    continue;
                }
                r.delete();
            }
        }

        if(daysToKeep!=-1) {
            Calendar cal = new GregorianCalendar();
            cal.add(Calendar.DAY_OF_YEAR,-daysToKeep);
            // copy it to the array because we'll be deleting builds as we go.
            for( Run r : job.getBuilds().toArray(new Run[0]) ) {
                if (r.isKeepLog()) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not GC-ed because it's marked as a keeper");
                    continue;
                }
                if (r==lsb) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not GC-ed because it's the last successful build");
                    continue;
                }
                if (r==lstb) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not GC-ed because it's the last stable build");
                    continue;
                }
                if (!r.getTimestamp().before(cal)) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not GC-ed because it's still new");
                    continue;
                }
                r.delete();
            }
        }

        int artNumInt = parse(artifactNumToKeep);
        int artDayInt = parse(artifactDaysToKeep);
        
        if(artNumInt!=-1) {
            Run[] builds = job.getBuilds().toArray(new Run[0]);
            for( int i=artNumInt; i<builds.length; i++ ) {
                Run r = builds[i];
                if (r.isKeepLog()) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not purged of artifacts because it's marked as a keeper");
                    continue;
                }
                if (r==lsb) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not purged of artifacts because it's the last successful build");
                    continue;
                }
                if (r==lstb) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not purged of artifacts because it's the last stable build");
                    continue;
                }
                r.deleteArtifacts();
            }
        }

        if(artDayInt!=-1) {
            Calendar cal = new GregorianCalendar();
            cal.add(Calendar.DAY_OF_YEAR,-artDayInt);
            // copy it to the array because we'll be deleting builds as we go.
            for( Run r : job.getBuilds().toArray(new Run[0]) ) {
                if (r.isKeepLog()) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not purged of artifacts because it's marked as a keeper");
                    continue;
                }
                if (r==lsb) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not purged of artifacts because it's the last successful build");
                    continue;
                }
                if (r==lstb) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not purged of artifacts because it's the last stable build");
                    continue;
                }
                if (!r.getTimestamp().before(cal)) {
                    LOGGER.log(FINER,r.getFullDisplayName()+" is not purged of artifacts because it's still new");
                    continue;
                }
                r.deleteArtifacts();
            }
        }

    }

    public int getDaysToKeep() {
        return daysToKeep;
    }

    public int getNumToKeep() {
        return numToKeep;
    }

    public int getArtifactDaysToKeep() {
        return parse(artifactDaysToKeep);
    }

    public int getArtifactNumToKeep() {
        return parse(artifactNumToKeep);
    }

    public String getDaysToKeepStr() {
        if(daysToKeep==-1)  return "";
        else                return String.valueOf(daysToKeep);
    }

    public String getNumToKeepStr() {
        if(numToKeep==-1)   return "";
        else                return String.valueOf(numToKeep);
    }

    public String getArtifactDaysToKeepStr() {
        return artifactDaysToKeep;
    }

    public String getArtifactNumToKeepStr() {
        return artifactNumToKeep;
    }

    public LRDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final LRDescriptor DESCRIPTOR = new LRDescriptor();

    public static final class LRDescriptor extends Descriptor<LogRotator> {
        public String getDisplayName() {
            return "Log Rotation";
        }
    }

    private static final Logger LOGGER = Logger.getLogger(LogRotator.class.getName());
}
