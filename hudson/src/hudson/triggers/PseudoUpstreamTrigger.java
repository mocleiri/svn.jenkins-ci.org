package hudson.triggers;

import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.tasks.BuildTrigger;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * {@link Trigger} that lets users edit {@link BuildTrigger}
 * of upstream projects.
 *
 * <p>
 * This is a special trigger in the sense that it will never become
 * a part of {@link Project#triggers}. It's just used to update
 * the other projects' {@link BuildTrigger}s accordingly.
 *
 * @author Kohsuke Kawaguchi
 */
public class PseudoUpstreamTrigger extends Trigger {

    private final String projectList;

    public PseudoUpstreamTrigger(String projectList) {
        this.projectList = projectList;
    }

    public PseudoUpstreamTrigger(List<Project> upstreamProjects) {
        this(Project.toNameList(upstreamProjects));
    }

    public List<Project> getUpstreamProjects() {
        return Project.fromNameList(projectList);
    }

    // needed for JSP
    public String getUpstreamProjectsValue() {
        return projectList;
    }

    protected void run() {
        // this shall never get exeucted
        throw new IllegalStateException();
    }

    public Descriptor<Trigger> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor<Trigger> DESCRIPTOR = new Descriptor<Trigger>(PseudoUpstreamTrigger.class) {
        public String getDisplayName() {
            return "Build after other projects are built";
        }

        public String getHelpFile() {
            return "/help/project-config/upstream.html";
        }

        public Trigger newInstance(HttpServletRequest req) {
            return new PseudoUpstreamTrigger(req.getParameter("upstreamProjects"));
        }
    };
}
