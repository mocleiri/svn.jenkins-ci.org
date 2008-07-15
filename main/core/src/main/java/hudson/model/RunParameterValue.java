package hudson.model;

import org.kohsuke.stapler.DataBoundConstructor;

public class RunParameterValue extends ParameterValue {

    private final Run run;

    @DataBoundConstructor
    public RunParameterValue(String name, Run run) {
        super(name);
        this.run = run;
    }

    @Override
    public Run getValue() {
        return run;
    }

}
