package hudson.tasks;

import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.Result;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Triggers builds of other projects.
 *
 * @author Kohsuke Kawaguchi
 */
public class BuildTrigger implements Publisher {

    /**
     * Comma-separated list of other projects to be scheduled.
     */
    private String childProjects;

    public BuildTrigger(String childProjects) {
        this.childProjects = childProjects;
    }

    public BuildTrigger(List<Project> childProjects) {
        this(Project.toNameList(childProjects));
    }

    public String getChildProjectsValue() {
        return childProjects;
    }

    public List<Project> getChildProjects() {
        return Project.fromNameList(childProjects);
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

    /**
     * Called from {@link Job#renameTo(String)} when a job is renamed.
     *
     * @return true
     *      if this {@link BuildTrigger} is changed and needs to be saved.
     */
    public boolean onJobRenamed(String oldName, String newName) {
        // quick test
        if(!childProjects.contains(oldName))
            return false;

        boolean changed = false;

        // we need to do this per string, since old Project object is already gone.
        String[] projects = childProjects.split(",");
        for( int i=0; i<projects.length; i++ ) {
            if(projects[i].trim().equals(oldName)) {
                projects[i] = newName;
                changed = true;
            }
        }

        if(changed) {
            StringBuilder b = new StringBuilder();
            for (String p : projects) {
                if(b.length()>0)    b.append(',');
                b.append(p);
            }
            childProjects = b.toString();
        }

        return changed;
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
