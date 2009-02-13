/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Jean-Baptiste Quenot, Seiji Sogabe, Tom Huybrechts
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.model;

import hudson.Util;
import hudson.model.Queue.QueueAction;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.util.VariableResolver;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Records the parameter values used for a build.
 *
 * <P>
 * This object is associated with the build record so that we remember what parameters
 * were used for what build. It is also attached to the queue item to remember parameter
 * that were specified when scheduling.
 */
@ExportedBean
public class ParametersAction implements Action, Iterable<ParameterValue>, QueueAction {

    private final List<ParameterValue> parameters;

    public ParametersAction(List<ParameterValue> parameters) {
        this.parameters = parameters;
    }
    
    public ParametersAction(ParameterValue... parameters) {
    	this(Arrays.asList(parameters));
    }

    public void createBuildWrappers(AbstractBuild<?,?> build, Collection<? super BuildWrapper> result) {
        for (ParameterValue p : parameters) {
            BuildWrapper w = p.createBuildWrapper(build);
            if(w!=null) result.add(w);
        }
    }

    public void buildEnvVars(AbstractBuild<?,?> build, Map<String,String> env) {
        for (ParameterValue p : parameters)
            p.buildEnvVars(build,env);
    }

    /**
     * Performs a variable subsitution to the given text and return it.
     */
    public String substitute(AbstractBuild<?,?> build, String text) {
        return Util.replaceMacro(text,createVariableResolver(build));
    }

    /**
     * Creates an {@link VariableResolver} that aggregates all the parameters.
     *
     * <p>
     * If you are a {@link BuildStep}, most likely you should call {@link AbstractBuild#getBuildVariableResolver()}. 
     */
    public VariableResolver<String> createVariableResolver(AbstractBuild<?,?> build) {
        VariableResolver[] resolvers = new VariableResolver[parameters.size()+1];
        int i=0;
        for (ParameterValue p : parameters)
            resolvers[i++] = p.createVariableResolver(build);

        resolvers[i] = build.getBuildVariableResolver();

        return new VariableResolver.Union<String>(resolvers);
    }

    public Iterator<ParameterValue> iterator() {
        return parameters.iterator();
    }

    @Exported(visibility=2)
    public List<ParameterValue> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public String getDisplayName() {
        return Messages.ParameterAction_DisplayName();
    }

    public String getIconFileName() {
        return "document-properties.gif";
    }

    public String getUrlName() {
        return "parameters";
    }

    /**
     * Allow an other build of the same project to be scheduled, if it has other parameters parameters.
     */
	public boolean shouldSchedule(List<Action> actions) {
		List<ParametersAction> others = Util.filter(actions, ParametersAction.class);
		if (others.isEmpty()) {
			return parameters.isEmpty();
		} else {
			// I don't think we need multiple ParametersActions, but let's be defensive 
			Set<ParameterValue> parameters = new HashSet<ParameterValue>();
			for (ParametersAction other: others) {
				parameters.addAll(other.parameters);
			}
		return !parameters.equals(new HashSet<ParameterValue>(this.parameters));
		}
	}

}
