package hudson.model;

import org.kohsuke.stapler.DataBoundConstructor;

public class JobParameterValue implements ParameterValue {

    private final String name;
    private final Job job;

    @DataBoundConstructor
    public JobParameterValue(String name, Job job) {
        super();
        this.job = job;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Job getValue() {
        return job;
    }

}
