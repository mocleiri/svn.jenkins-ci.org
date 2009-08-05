package hudson.plugins.cigame.rules.plugins.opentasks;

import java.util.List;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.plugins.cigame.model.Rule;
import hudson.plugins.cigame.model.RuleResult;
import hudson.plugins.cigame.util.ActionSequenceRetriever;
import hudson.plugins.cigame.util.ResultSequenceValidator;
import hudson.plugins.tasks.TasksResultAction;
import hudson.plugins.tasks.util.model.Priority;

/**
 * Default rule for the Open tasks plugin.
 */
public class DefaultOpenTasksRule implements Rule {

    private int pointsForAddingAnAnnotation;
    private int pointsForRemovingAnAnnotation;

    private Priority tasksPriority;

    public DefaultOpenTasksRule(Priority tasksPriority,
            int pointsForAddingAnAnnotation, int pointsForRemovingAnAnnotation) {
        this.tasksPriority = tasksPriority;
        this.pointsForAddingAnAnnotation = pointsForAddingAnAnnotation;
        this.pointsForRemovingAnAnnotation = pointsForRemovingAnAnnotation;
    }

    public RuleResult evaluate(AbstractBuild<?, ?> build) {
        if (new ResultSequenceValidator(Result.UNSTABLE, 2).isValid(build)) {
            List<List<TasksResultAction>> actionSequence = new ActionSequenceRetriever<TasksResultAction>(TasksResultAction.class, 2).getSequence(build);
            if (actionSequence != null) {
                int numberOfAnnotations = getNumberOfAnnotations(actionSequence.get(0)) - getNumberOfAnnotations(actionSequence.get(1));
                if (numberOfAnnotations > 0) {
                    return new RuleResult(numberOfAnnotations * pointsForAddingAnAnnotation, 
                            String.format("%d new open %s priority tasks were found", numberOfAnnotations, tasksPriority.name()));
                }
                if (numberOfAnnotations < 0) {
                    return new RuleResult((numberOfAnnotations * -1) * pointsForRemovingAnAnnotation, 
                            String.format("%d open %s priority tasks were fixed", numberOfAnnotations * -1, tasksPriority.name()));
                }
            }
        }
        return new RuleResult(0, String.format("No new or fixed %s priority tasks found.", tasksPriority.name()));
    }
    
    private int getNumberOfAnnotations(List<TasksResultAction> actions) {
        int numberOfAnnotations = 0;
        for (TasksResultAction action : actions) {
            numberOfAnnotations += action.getResult().getAnnotations(tasksPriority.name()).size();
        }
        return numberOfAnnotations;
    }
    
    public String getName() {
        return String.format("Open %s priority tasks", tasksPriority.name());
    }
}
