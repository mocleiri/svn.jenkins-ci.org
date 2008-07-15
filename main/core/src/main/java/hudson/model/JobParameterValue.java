package hudson.model;

import org.kohsuke.stapler.DataBoundConstructor;

public class JobParameterValue extends ParameterValue {
    private final Job job;

    @DataBoundConstructor
    public JobParameterValue(String name, Job job) {
        super(name);
        this.job = job;
    }

    @Override
    public Job getValue() {
        return job;
    }

}
