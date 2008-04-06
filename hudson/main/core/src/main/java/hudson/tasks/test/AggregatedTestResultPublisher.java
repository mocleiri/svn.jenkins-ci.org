package hudson.tasks.test;

import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Fingerprint.RangeSet;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormFieldValidator;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.StaplerProxy;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Collections;

/**
 * Aggregates downstream test reports into a single consolidated report,
 * so that people can see the overall test results in one page
 * when tests are scattered across many different jobs.
 *
 * @author Kohsuke Kawaguchi
 */
public class AggregatedTestResultPublisher extends Publisher {
    /**
     * Jobs to aggregate. Camma separated.
     * Null if triggering downstreams.
     */
    public final String jobs;

    @DataBoundConstructor
    public AggregatedTestResultPublisher(String jobs) {
        this.jobs = Util.fixEmptyAndTrim(jobs);
    }

    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        // add a TestResult just so that it can show up later.
        build.addAction(new TestResultAction(jobs,build));
        return true;
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    /**
     * Action that serves the aggregated record.
     *
     * TODO: persist some information so that even when some of the individuals
     * are gone, we can still retain some useful information.
     */
    public static final class TestResultAction extends AbstractTestResultAction {
        /**
         * Jobs to aggregate. Camma separated.
         * Never null.
         */
        private final String jobs;

        /**
         * The last time the fields of this object is computed from the rest.
         */
        private transient long lastUpdated = 0;
        /**
         * When was the last time any build completed?
         */
        private static long lastChanged = 0;

        private transient int failCount;
        private transient int totalCount;
        private transient List<AbstractTestResultAction> individuals;

        public TestResultAction(String jobs, AbstractBuild<?,?> owner) {
            super(owner);
            if(jobs==null) {
                // resolve null as the transitive downstream jobs
                StringBuilder buf = new StringBuilder();
                for (AbstractProject p : getProject().getTransitiveDownstreamProjects()) {
                    if(buf.length()>0)  buf.append(',');
                    buf.append(p.getFullName());
                }
                jobs = buf.toString();
            }
            this.jobs = jobs;
        }

        /**
         * Gets the jobs to be monitored.
         */
        public Collection<AbstractProject> getJobs() {
            List<AbstractProject> r = new ArrayList<AbstractProject>();
            for (String job : Util.tokenize(jobs,",")) {
                AbstractProject j = Hudson.getInstance().getItemByFullName(job.trim(), AbstractProject.class);
                if(j!=null)
                    r.add(j);
            }
            return r;
        }

        private AbstractProject<?,?> getProject() {
            return owner.getProject();
        }

        public int getFailCount() {
            upToDateCheck();
            return failCount;
        }

        public int getTotalCount() {
            upToDateCheck();
            return totalCount;
        }

        public Object getResult() {
            upToDateCheck();
            return this;
        }

        /**
         * Returns the individual test results that are aggregated.
         */
        public List<AbstractTestResultAction> getIndividuals() {
            upToDateCheck();
            return Collections.unmodifiableList(individuals);
        }

        /**
         * Makes sure that the data fields are up to date.
         */
        private synchronized void upToDateCheck() {
            // up to date check
            if(lastUpdated>lastChanged)     return;
            lastUpdated = lastChanged+1;

            int failCount = 0;
            int totalCount = 0;
            List<AbstractTestResultAction> individuals = new ArrayList<AbstractTestResultAction>();

            for (AbstractProject job : getJobs()) {
                RangeSet rs = owner.getDownstreamRelationship(job);
                for (int n : rs.listNumbersReverse()) {
                    Run b = job.getBuildByNumber(n);
                    if(b==null) continue;
                    if(b.isBuilding() || b.getResult().isWorseThan(Result.UNSTABLE))
                        continue;   // don't count them

                    for( AbstractTestResultAction ta : b.getActions(AbstractTestResultAction.class)) {
                        failCount += ta.getFailCount();
                        totalCount += ta.getTotalCount();
                        individuals.add(ta);
                    }
                    break;
                }
            }
            
            this.failCount = failCount;
            this.totalCount = totalCount;
            this.individuals = individuals;
        }

        @Override
        public String getDisplayName() {
            return "Aggregated Test Result";
        }

        @Override
        public String getUrlName() {
            return "aggregatedTestReport";
        }

        static {
            new RunListener<Run>(Run.class) {
                public void onCompleted(Run run, TaskListener listener) {
                    lastChanged = System.currentTimeMillis();
                }
            }.register();
//            Hudson.getInstance().getJobListeners().add(new ItemListener() {
//                public void onCreated(Item item) {
//                    itemChanged = System.currentTimeMillis();
//                }
//
//                public void onDeleted(Item item) {
//                    itemChanged = System.currentTimeMillis();
//                }
//
//                public void onRenamed(Item item, String oldName, String newName) {
//                    itemChanged = System.currentTimeMillis();
//                }
//            });
        }
    }

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
            super(AggregatedTestResultPublisher.class);
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;    // for all types
        }

        public String getDisplayName() {
            return "Aggregate downstream test results"; // TODO: i18n after the feature is stabilized
        }

        public void doCheck(StaplerRequest req, StaplerResponse rsp, @QueryParameter("value") final String list) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,false) {
                protected void check() throws IOException, ServletException {
                    for (String name : Util.tokenize(list, ",")) {
                        name = name.trim();
                        if(Hudson.getInstance().getItemByFullName(name)==null) {
                            error(hudson.tasks.Messages.BuildTrigger_NoSuchProject(name,AbstractProject.findNearest(name).getName()));
                        }
                    }
                }
            }.process();
        }

        public AggregatedTestResultPublisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(AggregatedTestResultPublisher.class,formData);
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }

}
