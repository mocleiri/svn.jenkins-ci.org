package hudson.tasks;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Action;

import javax.servlet.http.HttpServletRequest;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

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

    public String getChildProjectsValue() {
        return childProjects;
    }

    public List<Project> getChildProjects() {
        Hudson hudson = Hudson.getInstance();

        List<Project> r = new ArrayList<Project>();
        StringTokenizer tokens = new StringTokenizer(childProjects,",");
        while(tokens.hasMoreTokens()) {
            String projectName = tokens.nextToken().trim();
            Job job = hudson.getJob(projectName);
            if(!(job instanceof Project)) {
                continue; // ignore this token
            }
            r.add((Project) job);
        }
        return r;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        if(build.getResult()== Result.SUCCESS) {
            for (Project p : getChildProjects()) {
                listener.getLogger().println("Triggering a new build of "+p.getName());
                p.scheduleBuild();
            }
        }

        return true;
    }

    public Action getProjectAction(Project project) {
        return null;
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
