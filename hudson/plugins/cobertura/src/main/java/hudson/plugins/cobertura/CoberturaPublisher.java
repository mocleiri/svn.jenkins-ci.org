package hudson.plugins.cobertura;

import hudson.Launcher;
import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;
import hudson.plugins.cobertura.targets.CoverageTarget;
import hudson.plugins.cobertura.targets.CoverageMetric;
import hudson.plugins.cobertura.targets.CoverageResult;
import hudson.model.*;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.StaplerRequest;
import org.apache.commons.beanutils.ConvertUtils;

import java.io.*;
import java.util.*;

/**
 * Cobertura {@link Publisher}.
 *
 * @author Stephen Connolly
 */
public class CoberturaPublisher extends Publisher {

    private final String coberturaReportFile;

    private CoverageTarget healthyTarget;
    private CoverageTarget unhealthyTarget;
    private CoverageTarget failingTarget;

    /**
     *
     * @param coberturaReportDir the report directory
     * @stapler-constructor
     */
    public CoberturaPublisher(String coberturaReportFile) {
        this.coberturaReportFile = coberturaReportFile;
        this.healthyTarget = new CoverageTarget();
        this.unhealthyTarget = new CoverageTarget();
        this.failingTarget = new CoverageTarget();
    }

    public List<CoberturaPublisherTarget> getTargets() {
        Map<CoverageMetric, CoberturaPublisherTarget> targets = new TreeMap<CoverageMetric, CoberturaPublisherTarget>();
        for (CoverageMetric metric: healthyTarget.getTargets()) {
            CoberturaPublisherTarget target = targets.get(metric);
            if (target == null) {
                target = new CoberturaPublisherTarget();
                target.setMetric(metric);
            }
            target.setHealthy(healthyTarget.getTarget(metric));
            targets.put(metric, target);
        }
        for (CoverageMetric metric: unhealthyTarget.getTargets()) {
            CoberturaPublisherTarget target = targets.get(metric);
            if (target == null) {
                target = new CoberturaPublisherTarget();
                target.setMetric(metric);
            }
            target.setUnhealthy(unhealthyTarget.getTarget(metric));
            targets.put(metric, target);
        }
        for (CoverageMetric metric: failingTarget.getTargets()) {
            CoberturaPublisherTarget target = targets.get(metric);
            if (target == null) {
                target = new CoberturaPublisherTarget();
                target.setMetric(metric);
            }
            target.setUnstable(failingTarget.getTarget(metric));
            targets.put(metric, target);
        }
        List<CoberturaPublisherTarget> result = new ArrayList<CoberturaPublisherTarget>(targets.values());
        return result;
    }

    private void setTargets(List<CoberturaPublisherTarget> targets) {
        healthyTarget.clear();
        unhealthyTarget.clear();
        failingTarget.clear();
        for (CoberturaPublisherTarget target: targets) {
            if (target.getHealthy() != null) {
                healthyTarget.setTarget(target.getMetric(), target.getHealthy());
            }
            if (target.getUnhealthy() != null) {
                unhealthyTarget.setTarget(target.getMetric(), target.getUnhealthy());
            }
            if (target.getUnstable() != null) {
                failingTarget.setTarget(target.getMetric(), target.getUnstable());
            }
        }
    }

    /**
     * Getter for property 'coberturaReportFile'.
     *
     * @return Value for property 'coberturaReportFile'.
     */
    public String getCoberturaReportFile() {
        return coberturaReportFile;
    }

    /**
     * Getter for property 'healthyTarget'.
     *
     * @return Value for property 'healthyTarget'.
     */
    public CoverageTarget getHealthyTarget() {
        return healthyTarget;
    }

    /**
     * Setter for property 'healthyTarget'.
     *
     * @param healthyTarget Value to set for property 'healthyTarget'.
     */
    public void setHealthyTarget(CoverageTarget healthyTarget) {
        this.healthyTarget = healthyTarget;
    }

    /**
     * Getter for property 'unhealthyTarget'.
     *
     * @return Value for property 'unhealthyTarget'.
     */
    public CoverageTarget getUnhealthyTarget() {
        return unhealthyTarget;
    }

    /**
     * Setter for property 'unhealthyTarget'.
     *
     * @param unhealthyTarget Value to set for property 'unhealthyTarget'.
     */
    public void setUnhealthyTarget(CoverageTarget unhealthyTarget) {
        this.unhealthyTarget = unhealthyTarget;
    }

    /**
     * Getter for property 'failingTarget'.
     *
     * @return Value for property 'failingTarget'.
     */
    public CoverageTarget getFailingTarget() {
        return failingTarget;
    }

    /**
     * Setter for property 'failingTarget'.
     *
     * @param failingTarget Value to set for property 'failingTarget'.
     */
    public void setFailingTarget(CoverageTarget failingTarget) {
        this.failingTarget = failingTarget;
    }

    /** Gets the directory where the Cobertura Report is stored for the given project. */
    /*package*/ static File getCoberturaReportDir(AbstractItem project) {
        return new File(project.getRootDir(), "cobertura");
    }

    /** Gets the directory where the Cobertura Report is stored for the given project. */
    /*package*/
    static File getCoberturaReport(Build build) {
        return new File(build.getRootDir(), "coverage.xml");
    }


    /** {@inheritDoc} */
    public boolean perform(Build<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        listener.getLogger().println("Publishing Cobertura coverage report...");
        FilePath coverageReport = build.getParent().getWorkspace().child(coberturaReportFile);

        FilePath target = new FilePath(getCoberturaReportDir(build.getParent()));
        final File buildCoberturaDir = build.getRootDir();
        FilePath buildTarget = new FilePath(buildCoberturaDir);

        try {
            // if the build has failed, then there's not
            // much point in reporting an error
            if (build.getResult().isWorseOrEqualTo(Result.FAILURE) && !coverageReport.exists())
                return true;

            coverageReport.copyTo(new FilePath(buildTarget, "coverage.xml"));
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("Unable to copy coverage from " + coverageReport + " to " + target));
            build.setResult(Result.FAILURE);
        }

        String workspacePath = "";
        try {
            workspacePath = build.getParent().getWorkspace().act(new FilePath.FileCallable<String>() {
                public String invoke(File file, VirtualChannel virtualChannel) throws IOException {
                    try {
                        return file.getCanonicalPath();
                    } catch (IOException e) {
                        return file.getAbsolutePath();
                    }
                }
            });
        } catch (IOException e) {
        }
        if (!workspacePath.endsWith(File.separator)) {
            workspacePath += File.separator;
        }

        File coberturaXmlReport = getCoberturaReport(build);
        if (coberturaXmlReport.exists()) {
            listener.getLogger().println("Publishing Cobertura coverage results...");
            CoverageResult result = null;
            try {
                result = CoberturaCoverageParser.parse(coberturaXmlReport, workspacePath);
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError("Unable to copy coverage from " + coverageReport + " to " + target));
                build.setResult(Result.FAILURE);
            }
            final CoberturaBuildAction action = CoberturaBuildAction.load(build, workspacePath, result, healthyTarget, unhealthyTarget);

            build.getActions().add(action);
            Set<CoverageMetric> failingMetrics = failingTarget.getFailingMetrics(result);
            if (!failingMetrics.isEmpty()) {
                listener.getLogger().println("Code coverage enforcement failed for the following metrics:");
                for (CoverageMetric metric : failingMetrics) {
                    listener.getLogger().println("    " + metric);
                }
                listener.getLogger().println("Setting Build to unstable.");
                build.setResult(Result.UNSTABLE);
            }

        } else {
            flagMissingCoberturaXml(listener, build);
        }

        return true;
    }

    private void flagMissingCoberturaXml(BuildListener listener, Build<?, ?> build) {
        listener.getLogger().println("Could not find '" + coberturaReportFile + "/coverage.xml'.  Did you generate " +
                "the XML report for Cobertura?");
        build.setResult(Result.FAILURE);
    }


    /** {@inheritDoc} */
    public Action getProjectAction(Project project) {
        return new CoberturaProjectAction(project);
    }

    /** {@inheritDoc} */
    public Descriptor<Publisher> getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return DESCRIPTOR;
    }

    /** Descriptor should be singleton. */
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Descriptor for {@link CoberturaPublisher}. Used as a singleton. The class is marked as public so that it can be
     * accessed from views.
     * <p/>
     * <p/>
     * See <tt>views/hudson/plugins/cobertura/CoberturaPublisher/*.jelly</tt> for the actual HTML fragment for the
     * configuration screen.
     */
    public static final class DescriptorImpl extends Descriptor<Publisher> {
        CoverageMetric[] metrics = {
                CoverageMetric.PACKAGES,
                CoverageMetric.FILES,
                CoverageMetric.CLASSES,
                CoverageMetric.METHOD,
                CoverageMetric.LINE,
                CoverageMetric.CONDITIONAL,
        };
        /** Constructs a new DescriptorImpl. */
        DescriptorImpl() {
            super(CoberturaPublisher.class);
        }

        /** This human readable name is used in the configuration screen. */
        public String getDisplayName() {
            return "Publish Cobertura Coverage Report";
        }

        /**
         * Getter for property 'metrics'.
         *
         * @return Value for property 'metrics'.
         */
        public List<CoverageMetric> getMetrics() {
            return Arrays.asList(metrics);
        }

        public List<CoberturaPublisherTarget> getDefaultTargets() {
            List<CoberturaPublisherTarget> result = new ArrayList<CoberturaPublisherTarget>();
            result.add(new CoberturaPublisherTarget(CoverageMetric.METHOD, 80, null, null));
            result.add(new CoberturaPublisherTarget(CoverageMetric.LINE, 80, null, null));
            result.add(new CoberturaPublisherTarget(CoverageMetric.CONDITIONAL, 70, null, null));
            return result;
        }

        public List<CoberturaPublisherTarget> getTargets(CoberturaPublisher instance) {
            if (instance == null) {
                return getDefaultTargets();
            }
            return instance.getTargets();
        }

        /** {@inheritDoc} */
        public boolean configure(StaplerRequest req) throws FormException {
            req.bindParameters(this, "cobertura.");
            save();
            return super.configure(req);    //To change body of overridden methods use File | Settings | File Templates.
        }

        /** Creates a new instance of {@link CoberturaPublisher} from a submitted form. */
        public CoberturaPublisher newInstance(StaplerRequest req) throws FormException {
            CoberturaPublisher instance = req.bindParameters(CoberturaPublisher.class, "cobertura.");
            ConvertUtils.register(CoberturaPublisherTarget.CONVERTER, CoverageMetric.class);
            List<CoberturaPublisherTarget> targets = req.bindParametersToList(CoberturaPublisherTarget.class, "cobertura.target.");
            instance.setTargets(targets);
            return instance;
        }
    }

}
