/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Luca Domenico Milanesio, Tom Huybrechts
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

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import hudson.util.DescriptorList;
import hudson.ExtensionPoint;

/**
 * Defines a parameter for a build.
 *
 * <p>
 * In Hudson, a user can configure a job to require parameters for a build.
 * For example, imagine a test job that takes the bits to be tested as a parameter.
 *
 * <p>
 * The actual meaning and the purpose of parameters are entirely up to users, so
 * what the concrete parameter implmentation is pluggable. Write subclasses
 * in a plugin and hook it up to {@link #LIST} to register it.
 *
 * <p>
 * Three classes are used to model build parameters. First is the
 * {@link ParameterDescriptor}, which tells Hudson what kind of implementations are
 * available. From {@link ParameterDescriptor#newInstance(StaplerRequest, JSONObject)},
 * Hudson creates {@link ParameterDefinition}s based on the job configuration.
 * For example, if the user defines two string parameters "database-type" and
 * "appserver-type", we'll get two {@link StringParameterDefinition} instances
 * with their respective names.
 *
 * <p>
 * When a job is configured with {@link ParameterDefinition} (or more precisely,
 * {@link ParametersDefinitionProperty}, which in turns retains {@link ParameterDefinition}s),
 * user would have to enter the values for the defined build parameters.
 * The {@link #createValue(StaplerRequest, JSONObject)} method is used to convert this
 * form submission into {@link ParameterValue} objects, which are then accessible
 * during a build.
 *
 *
 *
 * <h2>Persistence</h2>
 * <p>
 * Instances of {@link ParameterDefinition}s are persisted into job <tt>config.xml</tt>
 * through XStream.
 *
 *
 * <h2>Assocaited Views</h2>
 * <h4>config.jelly</h4>
 * <p>
 * {@link ParameterDefinition} class uses <tt>config.jelly</tt> to provide contribute a form
 * fragment in the job configuration screen. Values entered there is fed back to
 * {@link ParameterDescriptor#newInstance(StaplerRequest, JSONObject)} to create {@link ParameterDefinition}s.
 *
 * <h4>index.jelly</h4>
 * The <tt>index.jelly</tt> view contributes a form fragment in the page where the user
 * enters actual values of parameters for a build. The result of this form submission
 * is then fed to {@link ParameterDefinition#createValue(StaplerRequest, JSONObject)} to
 * create {@link ParameterValue}s.
 *
 * TODO: what Jelly pages does this object need for rendering UI?
 * TODO: {@link ParameterValue} needs to have some mechanism to expose values to the build
 * @see StringParameterDefinition
 */
public abstract class ParameterDefinition implements
        Describable<ParameterDefinition>, ExtensionPoint {

    private final String name;

    public String getName() {
        return name;
    }

    public ParameterDefinition(String name) {
        super();
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public abstract ParameterDescriptor getDescriptor();

    /**
     * Create a parameter value from a form submission
     * @param req
     * @param jo
     * @return
     */
    public abstract ParameterValue createValue(StaplerRequest req, JSONObject jo);
    
    /**
     * Create a parameter value from a GET (with query string) or POST.
     * If no value is available in the request, it returns a default value if possible, or null.
     * @param req
     * @return
     */
    public abstract ParameterValue createValue(StaplerRequest req);

    /**
     * Returns default parameter value for this definition.
     * 
     * @return default parameter value or null if no defaults are available
     * @since 1.253
     */
    public ParameterValue getDefaultParameterValue() {
        return null;
    }

    /**
     * A list of available parameter definition types
     */
    public static final DescriptorList<ParameterDefinition> LIST = new DescriptorList<ParameterDefinition>();

    public abstract static class ParameterDescriptor extends
            Descriptor<ParameterDefinition> {

        protected ParameterDescriptor(Class<? extends ParameterDefinition> klazz) {
            super(klazz);
        }

        /**
         * Infers the type of the corresponding {@link ParameterDescriptor} from the outer class.
         * This version works when you follow the common convention, where a descriptor
         * is written as the static nested class of the describable class.
         *
         * @since 1.278
         */
        protected ParameterDescriptor() {
        }

        public String getValuePage() {
            return getViewPage(clazz, "index.jelly");
        }

        @Override
        public String getDisplayName() {
            return "Parameter";
        }

    }

}
