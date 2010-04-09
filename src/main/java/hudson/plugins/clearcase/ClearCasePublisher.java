/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer, Vincent Latombe
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
package hudson.plugins.clearcase;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Display ClearCase information report  for build
 * 
 *  @author Rinat Ailon
 */
public class ClearCasePublisher extends Publisher implements Serializable {
    @DataBoundConstructor
    public ClearCasePublisher() {

   }

    public boolean prebuild(AbstractBuild<?,?> build, BuildListener listener) {
        return true;
    }
    
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        try {
            ClearCaseReportAction action = new ClearCaseReportAction(build);
            build.getActions().add(action);
        } catch (Exception e) {
            // failure to parse should not fail the build
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Indicates an orderly abortion of the processing.
     */
    private static final class AbortException extends RuntimeException {
    }



    public Descriptor<Publisher> getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }
    
    /**
     * All global configurations in global.jelly are done from the DescriptorImpl class below
     * @author rgoren
     *
     */
    public static final class DescriptorImpl extends Descriptor<Publisher> {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
        /*
         * This initializes the global configuration when loaded
         */

        private ClearCaseSCM.ClearCaseScmDescriptor scmDescriptor;
        
        public DescriptorImpl() {
            super(ClearCasePublisher.class);
            // This makes sure any existing global configuration is read from the persistence file <Hudson work dir>/hudson.plugins.logparser.LogParserPublisher.xml
            load();
        }
        
        public DescriptorImpl(ClearCaseSCM.ClearCaseScmDescriptor scmDescriptor) {
            super(ClearCasePublisher.class);
            // This makes sure any existing global configuration is read from the persistence file <Hudson work dir>/hudson.plugins.logparser.LogParserPublisher.xml
            load();
            this.scmDescriptor = scmDescriptor;
        }
        
        public String getDisplayName() {
            return "Create ClearCase report";
        }

        public String getHelpFile() {
            return "/plugin/clearcase/publisher.html";
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
        

        /*
         * This method is invoked when the global configuration "save" is pressed
         */
        @Override
        public boolean configure(StaplerRequest req) throws FormException {
            save();
            return true;
        }
    }

    private static final long serialVersionUID = 1L;

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE ;
    }
}

