package hudson.plugins.cigame.rules.plugins.warnings;

import java.util.List;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.plugins.cigame.model.Rule;
import hudson.plugins.cigame.model.RuleResult;
import hudson.plugins.cigame.util.ActionSequenceRetriever;
import hudson.plugins.cigame.util.ResultSequenceValidator;
import hudson.plugins.warnings.WarningsResultAction;

/**
 * Default rule for the Warnings plugin.
 */
public class DefaultWarningsRule implements Rule {

    private int pointsForAddingAWarning;
    private int pointsForRemovingAWarning;

    public DefaultWarningsRule(int pointsForAddingAWarning, int pointsForRemovingAWarning) {
        this.pointsForAddingAWarning = pointsForAddingAWarning;
        this.pointsForRemovingAWarning = pointsForRemovingAWarning;
    }

    public RuleResult evaluate(AbstractBuild<?, ?> build) {        
        if (new ResultSequenceValidator(Result.UNSTABLE, 2).isValid(build)) {
            List<List<WarningsResultAction>> sequence = new ActionSequenceRetriever<WarningsResultAction>(WarningsResultAction.class, 2).getSequence(build);
            if (sequence != null) {
                int numberOfAnnotations = getNumberOfAnnotations(sequence.get(0)) - getNumberOfAnnotations(sequence.get(1));
                if (numberOfAnnotations > 0) {
                    return new RuleResult(numberOfAnnotations * pointsForAddingAWarning, 
                            String.format("%d new compiler warnings were found", numberOfAnnotations));
                }
                if (numberOfAnnotations < 0) {
                    return new RuleResult((numberOfAnnotations * -1) * pointsForRemovingAWarning, 
                            String.format("%d compiler warnings were fixed", numberOfAnnotations * -1));
                }
            }
        }
        return new RuleResult(0, "No new or fixed compiler warnings found.");
    }
    
    private int getNumberOfAnnotations(List<WarningsResultAction> actions) {
        int numberOfAnnotations = 0;
        for (WarningsResultAction action : actions) {
            numberOfAnnotations += action.getResult().getNumberOfAnnotations();
        }
        return numberOfAnnotations;
    }

    public String getName() {
        return "Changed number of compiler warnings";
    }
}
