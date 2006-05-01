package hudson.tasks;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.Result;

import javax.servlet.http.HttpServletRequest;
import java.util.StringTokenizer;

/**
 * Triggers builds of other projects.
 *
 * @author Kohsuke Kawaguchi
 */
public class BuildTrigger implements BuildStep {

    /**
     * Comma-separated list of other projects to be scheduled.
     */
    private final String childProjects;

    public BuildTrigger(String artifacts) {
        this.childProjects = artifacts;
    }

    public String getChildProjects() {
        return childProjects;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        if(build.getResult()== Result.SUCCESS) {
            Hudson hudson = Hudson.getInstance();

            StringTokenizer tokens = new StringTokenizer(childProjects,",");
            while(tokens.hasMoreTokens()) {
                String projectName = tokens.nextToken().trim();
                listener.getLogger().println("Triggering a new build of "+projectName);

                Job job = hudson.getJob(projectName);
                if(!(job instanceof Project)) {
                    listener.getLogger().println(projectName+" is not a project");
                    return false;
                }
                Project p = (Project) job;
                p.scheduleBuild();
            }
        }

        return true;
    }

    public Descriptor<BuildStep> getDescriptor() {
        return DESCRIPTOR;
    }


    public static final Descriptor<BuildStep> DESCRIPTOR = new Descriptor<BuildStep>(BuildTrigger.class) {
        public String getDisplayName() {
            return "Build other projects";
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new BuildTrigger(req.getParameter("childProjects"));
        }
    };
}
