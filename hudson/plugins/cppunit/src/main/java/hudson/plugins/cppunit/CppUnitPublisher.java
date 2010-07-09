package hudson.plugins.cppunit;

import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;

import java.io.Serializable;


@SuppressWarnings("unused")
public class CppUnitPublisher extends Recorder implements Serializable {

    private String testResultsPattern = null;

    private boolean useWorkspaceBaseDir = false;

    public String getTestResultsPattern() {
        return testResultsPattern;
    }

    public boolean isUseWorkspaceBaseDir() {
        return useWorkspaceBaseDir;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
}
