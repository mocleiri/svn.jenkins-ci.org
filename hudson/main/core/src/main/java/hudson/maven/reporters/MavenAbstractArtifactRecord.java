package hudson.maven.reporters;

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenUtil;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BallColor;
import hudson.model.LargeText;
import hudson.model.Result;
import hudson.model.TaskAction;
import hudson.model.TaskListener;
import hudson.model.TaskThread;
import hudson.model.TaskThread.ListenerAndText;
import hudson.security.Permission;
import hudson.util.Iterators;
import hudson.widgets.HistoryWidget;
import hudson.widgets.HistoryWidget.Adapter;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.embedder.MavenEmbedderException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * UI to redeploy artifacts after the fact.
 *
 * <p>
 * There are two types &mdash; one for the module, the other for the whole project.
 * The semantics specific to these cases are defined in subtypes.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class MavenAbstractArtifactRecord<T extends AbstractBuild<?,?>> extends TaskAction {
    public final class Record {
        /**
         * Repository URL that artifacts were deployed.
         */
        public final String url;

        /**
         * Log file name. Relative to {@link AbstractBuild#getRootDir()}.
         */
        private final String fileName;

        /**
         * Status of this record.
         */
        private Result result;

        private final Calendar timeStamp;

        public Record(String url, String fileName) {
            this.url = url;
            this.fileName = fileName;
            timeStamp = new GregorianCalendar();
        }

        /**
         * Returns the log of this deployment record.
         */
        public LargeText getLog() {
            return new LargeText(new File(getBuild().getRootDir(),fileName),true);
        }

        /**
         * Result of the deployment. During the build, this value is null.
         */
        public Result getResult() {
            return result;
        }

        public int getNumber() {
            return records.indexOf(this);
        }

        public boolean isBuilding() {
            return result==null;
        }

        public Calendar getTimestamp() {
            return (Calendar) timeStamp.clone();
        }

        public String getBuildStatusUrl() {
            return getIconColor().getImage();
        }

        public BallColor getIconColor() {
            if(result==null)
                return BallColor.GREY_ANIME;
            else
                return result.color;
        }

        // TODO: Eventually provide a better UI
        public final void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
            rsp.setContentType("text/plain;charset=UTF-8");
            getLog().writeLogTo(0,rsp.getWriter());
        }
    }

    /**
     * Records of a deployment.
     */
    public final CopyOnWriteArrayList<Record> records = new CopyOnWriteArrayList<Record>();

    /**
     * Gets the parent build object to which this record is registered.
     */
    public abstract T getBuild();

    public final String getIconFileName() {
        return "redo.gif";
    }

    public final String getDisplayName() {
        return "Redeploy Artifacts";
    }

    public final String getUrlName() {
        return "redeploy";
    }

    protected Permission getPermission() {
        return REDEPLOY;
    }

    public HistoryWidgetImpl getHistoryWidget() {
        return new HistoryWidgetImpl();
    }

    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        return records.get(Integer.valueOf(token));
    }

    /**
     * Performs a redeployment.
     */
    public final void doRedeploy(StaplerRequest req, StaplerResponse rsp,
                           @QueryParameter("id") final String id,
                           @QueryParameter("url") final String repositoryUrl,
                           @QueryParameter("uniqueVersion") final boolean uniqueVersion) throws ServletException, IOException {
        getBuild().checkPermission(REDEPLOY);

        File logFile = new File(getBuild().getRootDir(),"maven-deployment."+records.size()+".log");
        final Record record = new Record(repositoryUrl, logFile.getName());
        records.add(record);

        new TaskThread(this,ListenerAndText.forFile(logFile)) {
            protected void perform(TaskListener listener) throws Exception {
                try {
                    MavenEmbedder embedder = MavenUtil.createEmbedder(listener, null);
                    ArtifactRepositoryLayout layout =
                        (ArtifactRepositoryLayout) embedder.getContainer().lookup( ArtifactRepositoryLayout.ROLE,"default");
                    ArtifactRepositoryFactory factory =
                        (ArtifactRepositoryFactory) embedder.lookup(ArtifactRepositoryFactory.ROLE);

                    ArtifactRepository repository = factory.createDeploymentArtifactRepository(
                            id, repositoryUrl, layout, uniqueVersion);

                    deploy(embedder,repository,listener);

                    embedder.stop();
                    record.result = Result.SUCCESS;
                } finally {
                    if(record.result==null)
                        record.result = Result.FAILURE;
                    // persist the record
                    getBuild().save();
                }
            }
        }.start();

        rsp.sendRedirect(".");
    }

    /**
     * Deploys the artifacts to the specified {@link ArtifactRepository}.
     *
     * @param embedder
     *      This component hosts all the Maven components we need to do the work.
     * @param deploymentRepository
     *      The remote repository to deploy to.
     * @param listener
     *      The status and error goes to this listener.
     */
    public abstract void deploy(MavenEmbedder embedder, ArtifactRepository deploymentRepository, TaskListener listener) throws MavenEmbedderException, IOException, ComponentLookupException, ArtifactDeploymentException;

    private final class HistoryWidgetImpl extends HistoryWidget<MavenAbstractArtifactRecord,Record> {
        private HistoryWidgetImpl() {
            super(MavenAbstractArtifactRecord.this, Iterators.reverse(records), ADAPTER);
        }

        public String getDisplayName() {
            return "Deployment History";
        }
    }

    private static final Adapter<MavenAbstractArtifactRecord<?>.Record> ADAPTER = new Adapter<MavenAbstractArtifactRecord<?>.Record>() {
        public int compare(MavenAbstractArtifactRecord<?>.Record record, String key) {
            return record.getNumber()-Integer.parseInt(key);
        }

        public String getKey(MavenAbstractArtifactRecord<?>.Record record) {
            return String.valueOf(record.getNumber());
        }

        public boolean isBuilding(MavenAbstractArtifactRecord<?>.Record record) {
            return record.isBuilding();
        }

        public String getNextKey(String key) {
            return String.valueOf(Integer.parseInt(key)+1);
        }
    };


    /**
     * Permission for redeploying artifacts.
     */
    public static final Permission REDEPLOY = AbstractProject.BUILD;
}
