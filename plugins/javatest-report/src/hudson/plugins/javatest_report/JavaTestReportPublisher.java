/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package hudson.plugins.javatest_report;

import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Rama Pulavarthi
 */

public class JavaTestReportPublisher extends Publisher {
    private final String includes;

    public JavaTestReportPublisher(String includes) {
        this.includes = includes;
    }

    /**
     * Ant "&lt;fileset @includes="..." /> pattern to specify SQE XML files
     */
    public String getIncludes() {
        return includes;
    }
    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Action getProjectAction(Project project) {
        return null;
    }

    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

     public static final Descriptor<Publisher> DESCRIPTOR = new Descriptor<Publisher>(JavaTestReportPublisher.class) {
        public String getDisplayName() {
            return "Publish JavaTest result report";
        }

        public String getHelpFile() {
            return "/plugin/javatest-report/help.html";
        }

        public Publisher newInstance(StaplerRequest req) {
            return new JavaTestReportPublisher(req.getParameter("javatest_includes"));
        }
    };
}
