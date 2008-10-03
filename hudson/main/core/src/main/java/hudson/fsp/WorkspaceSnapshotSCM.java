package hudson.fsp;

import hudson.scm.SCM;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCMDescriptor;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.Launcher;
import hudson.FilePath;
import hudson.WorkspaceSnapshot;
import hudson.PermalinkList;

import java.io.IOException;
import java.io.File;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * {@link SCM} that inherits the workspace from another build through {@link WorkspaceSnapshot}
 *
 * @author Kohsuke Kawaguchi
 */
public class WorkspaceSnapshotSCM extends SCM {
    /**
     * The job name from which we inherit the workspace.
     */
    public String jobName;
    /**
     * The permalink name indicating the build from which to inherit the workspace.
     */
    public String permalink;

    @DataBoundConstructor
    public WorkspaceSnapshotSCM(String jobName, String permalink) {
        this.jobName = jobName;
        this.permalink = permalink;
    }

    /**
     * {@link Exception} indicating that the resolution of the job/permalink failed.
     */
    private final class ResolvedFailedException extends Exception {
        private ResolvedFailedException(String message) {
            super(message);
        }
    }

    private static class Snapshot {
        final WorkspaceSnapshot snapshot;
        final AbstractBuild<?,?> owner;
        private Snapshot(WorkspaceSnapshot snapshot, AbstractBuild<?,?> owner) {
            this.snapshot = snapshot;
            this.owner = owner;
        }

        void restoreTo(FilePath dst,TaskListener listener) throws IOException, InterruptedException {
            snapshot.restoreTo(owner,dst,listener);
        }
    }
    /**
     * Obtains the {@link WorkspaceSnapshot} object that this {@link SCM} points to,
     * or throws {@link ResolvedFailedException} upon failing.
     *
     * @return never null.
     */
    public Snapshot resolve() throws ResolvedFailedException {
        Hudson h = Hudson.getInstance();
        AbstractProject<?,?> job = h.getItemByFullName(jobName, AbstractProject.class);
        if(job==null) {
            if(h.getItemByFullName(jobName)==null) {
                AbstractProject nearest = AbstractProject.findNearest(jobName);
                throw new ResolvedFailedException(Messages.WorkspaceSnapshotSCM_NoSuchJob(jobName,nearest.getFullName()));
            } else
                throw new ResolvedFailedException(Messages.WorkspaceSnapshotSCM_IncorrectJobType(jobName));
        }

        PermalinkList permalinks = job.getPermalinks();
        Permalink p = permalinks.get(permalink);
        if(p==null)
            throw new ResolvedFailedException(Messages.WorkspaceSnapshotSCM_NoSuchPermalink(permalink,jobName));

        AbstractBuild<?,?> b = (AbstractBuild<?,?>)p.resolve(job);
        if(b==null)
            throw new ResolvedFailedException(Messages.WorkspaceSnapshotSCM_NoBuild(permalink,jobName));

        WorkspaceSnapshot snapshot = b.getAction(WorkspaceSnapshot.class);
        if(snapshot==null)
            throw new ResolvedFailedException(Messages.WorkspaceSnapshotSCM_NoWorkspace(jobName,permalink));

        return new Snapshot(snapshot,b);
    }

    public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener) throws IOException, InterruptedException {
        AbstractBuild lastBuild = (AbstractBuild) project.getLastBuild();
        if (lastBuild == null) {
            listener.getLogger().println("No existing build. Starting a new one");
            return true;
        }


        return false;
    }

    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {
        try {
            resolve().restoreTo(workspace,listener);
            return true;
        } catch (ResolvedFailedException e) {
            listener.error(e.getMessage()); // stack trace is meaningless
            build.setResult(Result.FAILURE);
            return false;
        }
    }

    public ChangeLogParser createChangeLogParser() {
        return null;
    }

    public SCMDescriptor<?> getDescriptor() {
        return null;
    }
}
