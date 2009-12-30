
/* ===========================================================================
 *  Copyright (c) 2007 Serena Software. All rights reserved.
 *
 *  Use of the Sample Code provided by Serena is governed by the following
 *  terms and conditions. By using the Sample Code, you agree to be bound by
 *  the terms contained herein. If you do not agree to the terms herein, do
 *  not install, copy, or use the Sample Code.
 *
 *  1.  GRANT OF LICENSE.  Subject to the terms and conditions herein, you
 *  shall have the nonexclusive, nontransferable right to use the Sample Code
 *  for the sole purpose of developing applications for use solely with the
 *  Serena software product(s) that you have licensed separately from Serena.
 *  Such applications shall be for your internal use only.  You further agree
 *  that you will not: (a) sell, market, or distribute any copies of the
 *  Sample Code or any derivatives or components thereof; (b) use the Sample
 *  Code or any derivatives thereof for any commercial purpose; or (c) assign
 *  or transfer rights to the Sample Code or any derivatives thereof.
 *
 *  2.  DISCLAIMER OF WARRANTIES.  TO THE MAXIMUM EXTENT PERMITTED BY
 *  APPLICABLE LAW, SERENA PROVIDES THE SAMPLE CODE AS IS AND WITH ALL
 *  FAULTS, AND HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EITHER
 *  EXPRESSED, IMPLIED OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY
 *  IMPLIED WARRANTIES OR CONDITIONS OF MERCHANTABILITY, OF FITNESS FOR A
 *  PARTICULAR PURPOSE, OF LACK OF VIRUSES, OF RESULTS, AND OF LACK OF
 *  NEGLIGENCE OR LACK OF WORKMANLIKE EFFORT, CONDITION OF TITLE, QUIET
 *  ENJOYMENT, OR NON-INFRINGEMENT.  THE ENTIRE RISK AS TO THE QUALITY OF
 *  OR ARISING OUT OF USE OR PERFORMANCE OF THE SAMPLE CODE, IF ANY,
 *  REMAINS WITH YOU.
 *
 *  3.  EXCLUSION OF DAMAGES.  TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE
 *  LAW, YOU AGREE THAT IN CONSIDERATION FOR RECEIVING THE SAMPLE CODE AT NO
 *  CHARGE TO YOU, SERENA SHALL NOT BE LIABLE FOR ANY DAMAGES WHATSOEVER,
 *  INCLUDING BUT NOT LIMITED TO DIRECT, SPECIAL, INCIDENTAL, INDIRECT, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, DAMAGES FOR LOSS OF
 *  PROFITS OR CONFIDENTIAL OR OTHER INFORMATION, FOR BUSINESS INTERRUPTION,
 *  FOR PERSONAL INJURY, FOR LOSS OF PRIVACY, FOR NEGLIGENCE, AND FOR ANY
 *  OTHER LOSS WHATSOEVER) ARISING OUT OF OR IN ANY WAY RELATED TO THE USE
 *  OF OR INABILITY TO USE THE SAMPLE CODE, EVEN IN THE EVENT OF THE FAULT,
 *  TORT (INCLUDING NEGLIGENCE), STRICT LIABILITY, OR BREACH OF CONTRACT,
 *  EVEN IF SERENA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.  THE
 *  FOREGOING LIMITATIONS, EXCLUSIONS AND DISCLAIMERS SHALL APPLY TO THE
 *  MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW.  NOTWITHSTANDING THE ABOVE,
 *  IN NO EVENT SHALL SERENA'S LIABILITY UNDER THIS AGREEMENT OR WITH RESPECT
 *  TO YOUR USE OF THE SAMPLE CODE AND DERIVATIVES THEREOF EXCEED US$10.00.
 *
 *  4.  INDEMNIFICATION. You hereby agree to defend, indemnify and hold
 *  harmless Serena from and against any and all liability, loss or claim
 *  arising from this agreement or from (i) your license of, use of or
 *  reliance upon the Sample Code or any related documentation or materials,
 *  or (ii) your development, use or reliance upon any application or
 *  derivative work created from the Sample Code.
 *
 *  5.  TERMINATION OF THE LICENSE.  This agreement and the underlying
 *  license granted hereby shall terminate if and when your license to the
 *  applicable Serena software product terminates or if you breach any terms
 *  and conditions of this agreement.
 *
 *  6.  CONFIDENTIALITY.  The Sample Code and all information relating to the
 *  Sample Code (collectively "Confidential Information") are the
 *  confidential information of Serena.  You agree to maintain the
 *  Confidential Information in strict confidence for Serena.  You agree not
 *  to disclose or duplicate, nor allow to be disclosed or duplicated, any
 *  Confidential Information, in whole or in part, except as permitted in
 *  this Agreement.  You shall take all reasonable steps necessary to ensure
 *  that the Confidential Information is not made available or disclosed by
 *  you or by your employees to any other person, firm, or corporation.  You
 *  agree that all authorized persons having access to the Confidential
 *  Information shall observe and perform under this nondisclosure covenant.
 *  You agree to immediately notify Serena of any unauthorized access to or
 *  possession of the Confidential Information.
 *
 *  7.  AFFILIATES.  Serena as used herein shall refer to Serena Software,
 *  Inc. and its affiliates.  An entity shall be considered to be an
 *  affiliate of Serena if it is an entity that controls, is controlled by,
 *  or is under common control with Serena.
 *
 *  8.  GENERAL.  Title and full ownership rights to the Sample Code,
 *  including any derivative works shall remain with Serena.  If a court of
 *  competent jurisdiction holds any provision of this agreement illegal or
 *  otherwise unenforceable, that provision shall be severed and the
 *  remainder of the agreement shall remain in full force and effect.
 * ===========================================================================
 */

/**
 ** @brief This experimental plugin extends Hudson support for Dimensions SCM repositories
 **
 ** @author Tim Payne
 **
 **/

package hudson.plugins.dimensionsscm;

// Dimensions imports
import hudson.plugins.dimensionsscm.DimensionsAPI;
import hudson.plugins.dimensionsscm.DimensionsSCM;
import hudson.plugins.dimensionsscm.Logger;
import com.serena.dmclient.api.DimensionsResult;

// Hudson imports
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor.FormException;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.Util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

// General imports
import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

public class DimensionsBuildNotifier extends Notifier implements Serializable {

    private static DimensionsSCM scm = null;
    private boolean canBaselineDeploy = false;
    private boolean canBaselineAction = false;

    private String  actionState = null;
    private String  deployState = null;

    /*
     * Gets the action .
     * @return the action
     */
    public boolean isCanBaselineAction() {
        return this.canBaselineAction;
    }

    /*
     * Gets the deploy .
     * @return the deploy
     */
    public boolean isCanBaselineDeploy() {
        return this.canBaselineDeploy;
    }

    /*
     * Gets the action state .
     * @return String
     */
    public String getActionState() {
        return this.actionState;
    }

    /*
     * Gets the deploy state .
     * @return String
     */
    public String getDeployState() {
        return this.deployState;
    }

    /**
     * Default constructor.
     */
    public DimensionsBuildNotifier(boolean canDeploy, String deployState, boolean canAction, String actionState) {
        this.canBaselineDeploy = canDeploy;
        this.canBaselineAction = canAction;

        this.actionState = actionState;
        this.deployState = deployState;
    }

    // Run this one last
    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) throws IOException, InterruptedException {
        Logger.Debug("Invoking perform callout " + this.getClass().getName());
        try {
            if (!(build.getProject().getScm() instanceof DimensionsSCM)) {
                listener.fatalError("[DIMENSIONS] This plugin only works with the Dimensions SCM engine.");
                throw new IOException("[DIMENSIONS] This plugin only works with a Dimensions SCM engine");
            }
            if (build.getResult() == Result.SUCCESS) {
                if (scm == null)
                    scm = (DimensionsSCM)build.getProject().getScm();
                Logger.Debug("Dimensions user is "+scm.getJobUserName()+" , Dimensions installation is "+scm.getJobServer());
                if (scm.getAPI().login(scm.getJobUserName(),
                                       scm.getJobPasswd(),
                                       scm.getJobDatabase(),
                                       scm.getJobServer()))
                {
                    {
                        DimensionsResult res = scm.getAPI().createBaseline(scm.getProject(),build);
                        if (res==null) {
                            listener.getLogger().println("[DIMENSIONS] The build failed to be tagged in Dimensions");
                            listener.getLogger().flush();
                            canBaselineDeploy = canBaselineAction = false;
                        }
                        else {
                            listener.getLogger().println("[DIMENSIONS] Build was successfully tagged in Dimensions as a baseline");
                            listener.getLogger().println("[DIMENSIONS] ("+res.getMessage().replaceAll("\n","\n[DIMENSIONS] ")+")");
                            listener.getLogger().flush();
                        }
                    }
                    if (canBaselineDeploy) {
                        DimensionsResult res = scm.getAPI().deployBaseline(scm.getProject(),build,deployState);
                        if (res==null) {
                            listener.getLogger().println("[DIMENSIONS] The build baseline failed to be deployed in Dimensions");
                            listener.getLogger().flush();
                            canBaselineAction=false;
                        }
                        else {
                            listener.getLogger().println("[DIMENSIONS] Build baseline was successfully deployed in Dimensions");
                            listener.getLogger().println("[DIMENSIONS] ("+res.getMessage().replaceAll("\n","\n[DIMENSIONS] ")+")");
                            listener.getLogger().flush();
                        }
                    }

                    // This will not be supported for now, but if wanted at a future date...
                    if (canBaselineAction) {
                        DimensionsResult res = scm.getAPI().actionBaseline(scm.getProject(),build,actionState);
                        if (res==null) {
                            listener.getLogger().println("[DIMENSIONS] The build baseline failed to be actioned in Dimensions");
                            listener.getLogger().flush();
                        }
                        else {
                            listener.getLogger().println("[DIMENSIONS] Build baseline was successfully actioned in Dimensions");
                            listener.getLogger().println("[DIMENSIONS] ("+res.getMessage().replaceAll("\n","\n[DIMENSIONS] ")+")");
                            listener.getLogger().flush();
                        }
                    }
                }
            }
        } catch(Exception e) {
            listener.fatalError("Unable to tag build in Dimensions - " + e.getMessage());
            return false;
        }
        finally
        {
            if (scm != null)
                scm.getAPI().logout();
        }
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * The DimensionsBuildNotifier Descriptor class.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        /*
         * Loads the descriptor
         */
        public DescriptorImpl() {
            super(DimensionsBuildNotifier.class);
            load();
            Logger.Debug("Loading " + this.getClass().getName());
        }

        public String getDisplayName() {
            return "Tag successful builds in Dimensions as a project baseline";
        }

        @Override
        public Notifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            // Get variables and then construct a new object
            String deploy = req.getParameter("dimensionsbuildnotifier.deployState");
            String action = null;
            if (deploy != null)
                deploy = Util.fixNull(req.getParameter("dimensionsbuildnotifier.deployState").trim());
            if (action != null)
                action = Util.fixNull(req.getParameter("dimensionsbuildnotifier.actionState").trim());


            DimensionsBuildNotifier notif = new DimensionsBuildNotifier(req.getParameter("dimensionsbuildnotifier.canBaselineDeploy")!=null,
                                                                        deploy,
                                                                        false,
                                                                        null);

            return notif;
        }

        /*
         *  This builder can be used with all project types
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        /*
         * Save the descriptor configuration
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // Get the values and check them
            String deploy = req.getParameter("dimensionsbuildnotifier.deployState");
            String action = null;

            if (deploy != null)
                deploy = Util.fixNull(req.getParameter("dimensionsbuildnotifier.deployState").trim());

            if (action != null)
                action = Util.fixNull(req.getParameter("dimensionsbuildnotifier.actionState").trim());

            this.save();
            return super.configure(req, formData);
        }

        /*
         * Get help file
         */
        @Override
        public String getHelpFile() {
            return "/plugin/dimensionsscm/helpbnotifier.html";
        }
    }
}
