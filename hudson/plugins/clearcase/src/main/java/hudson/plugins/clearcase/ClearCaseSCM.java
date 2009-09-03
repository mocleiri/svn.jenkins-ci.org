/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
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

import static hudson.Util.fixEmpty;
import static hudson.Util.fixEmptyAndTrim;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.DefaultPollAction;
import hudson.plugins.clearcase.action.DynamicCheckoutAction;
import hudson.plugins.clearcase.action.PollAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.action.SnapshotCheckoutAction;
import hudson.plugins.clearcase.base.BaseChangeLogAction;
import hudson.plugins.clearcase.base.BaseHistoryAction;
import hudson.plugins.clearcase.base.BasePollAction;
import hudson.plugins.clearcase.base.BaseSaveChangeLogAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.ByteBuffer;
import hudson.util.FormFieldValidator;
import hudson.util.VariableResolver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


/**
 * Base ClearCase SCM.
 * 
 * This SCM is for base ClearCase repositories.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseSCM extends AbstractClearCaseScm {

	private String configSpec;
	private final String branch;
	private boolean doNotUpdateConfigSpec;

	@DataBoundConstructor
	public ClearCaseSCM(String branch, String configspec, String viewname,
                            boolean useupdate, String loadRules, boolean usedynamicview,
                            String viewdrive, String mkviewoptionalparam,
                            boolean filterOutDestroySubBranchEvent,
                            boolean doNotUpdateConfigSpec, boolean rmviewonrename,
                            String excludedRegions) {
		super(viewname, mkviewoptionalparam, filterOutDestroySubBranchEvent,
                      (!usedynamicview) && useupdate, rmviewonrename,
                      excludedRegions, usedynamicview, viewdrive, loadRules);
		this.branch = branch;
		this.configSpec = configspec;
		this.doNotUpdateConfigSpec = doNotUpdateConfigSpec;

	}

    public ClearCaseSCM(String branch, String configspec, String viewname,
                            boolean useupdate, String loadRules, boolean usedynamicview,
                            String viewdrive, String mkviewoptionalparam,
                            boolean filterOutDestroySubBranchEvent,
                            boolean doNotUpdateConfigSpec, boolean rmviewonrename) {
            this(branch, configspec, viewname, useupdate, loadRules, usedynamicview, viewdrive,
                 mkviewoptionalparam, filterOutDestroySubBranchEvent, doNotUpdateConfigSpec, 
                 rmviewonrename, "");
        }


	public String getBranch() {
		return branch;
	}

	public String getConfigSpec() {
		return configSpec;
	}

	public boolean isDoNotUpdateConfigSpec() {
		return doNotUpdateConfigSpec;
	}

	@Override
	public ClearCaseScmDescriptor getDescriptor() {
	    return PluginImpl.BASE_DESCRIPTOR;
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		return new ClearCaseChangeLogParser();
	}

	@Override
	public void buildEnvVars(AbstractBuild build, Map<String, String> env) {
		super.buildEnvVars(build, env);
		if (isUseDynamicView()) {
                    if (getViewDrive() != null) {
                        env.put(CLEARCASE_VIEWPATH_ENVSTR, getViewDrive() + File.separator
						+ getNormalizedViewName());
			} else {
				env.remove(CLEARCASE_VIEWPATH_ENVSTR);
			}
		}
	}


	@Override
	protected CheckOutAction createCheckOutAction(
			VariableResolver variableResolver, ClearToolLauncher launcher) {
		CheckOutAction action;
		if (isUseDynamicView()) {
			action = new DynamicCheckoutAction(createClearTool(
					variableResolver, launcher), configSpec,
					doNotUpdateConfigSpec);
		} else {
			action = new SnapshotCheckoutAction(createClearTool(
                                                                            variableResolver, launcher), 
                                                            configSpec, getLoadRules(), isUseUpdate());
		}
		return action;
	}

	@Override
	protected HistoryAction createHistoryAction(
			VariableResolver variableResolver, ClearToolLauncher launcher) {
            ClearTool ct = createClearTool(variableResolver, launcher);
	    BaseHistoryAction action = new BaseHistoryAction(ct, configureFilters(launcher),
							     getDescriptor().getLogMergeTimeWindow());

            try {
		String pwv = ct.pwv(generateNormalizedViewName((BuildVariableResolver) variableResolver));
                if (pwv != null) {
                    if (pwv.contains("/")) {
                        pwv += "/";
                    }
                    else {
                        pwv += "\\";
                    }
                    action.setExtendedViewPath(pwv);
                }
            } catch (Exception e) {
                Logger.getLogger(ClearCaseSCM.class.getName()).log(Level.WARNING,
                                                                   "Exception when running 'cleartool pwv'",
                                                                   e);
            } 
            
            return action;
	}

	@Override
	protected SaveChangeLogAction createSaveChangeLogAction(
			ClearToolLauncher launcher) {
		return new BaseSaveChangeLogAction();
	}

	/**
	 * Split the branch names into a string array.
	 * 
	 * @param branchString
	 *            string containing none or several branches
	 * @return a string array (never empty)
	 */
	@Override
	public String[] getBranchNames() {
		// split by whitespace, except "\ "
		String[] branchArray = branch.split("(?<!\\\\)[ \\r\\n]+");
		// now replace "\ " to " ".
		for (int i = 0; i < branchArray.length; i++)
			branchArray[i] = branchArray[i].replaceAll("\\\\ ", " ");
		return branchArray;
	}

	@Override
	protected ClearTool createClearTool(VariableResolver variableResolver,
			ClearToolLauncher launcher) {
            if (isUseDynamicView()) {
                return new ClearToolDynamic(variableResolver, launcher, getViewDrive());
		} else {
			return super.createClearTool(variableResolver, launcher);
		}
	}

	/**
	 * ClearCase SCM descriptor
	 * 
	 * @author Erik Ramfelt
	 */
	public static class ClearCaseScmDescriptor extends
			SCMDescriptor<ClearCaseSCM> implements ModelObject {
		private String cleartoolExe;
		private int changeLogMergeTimeWindow = 5;

		protected ClearCaseScmDescriptor() {
			super(ClearCaseSCM.class, null);
			load();
		}

		public int getLogMergeTimeWindow() {
			return changeLogMergeTimeWindow;
		}

		public String getCleartoolExe() {
			if (cleartoolExe == null) {
				return "cleartool";
			} else {
				return cleartoolExe;
			}
		}

		@Override
		public String getDisplayName() {
			return "Base ClearCase";
		}

		@Override
		public boolean configure(StaplerRequest req) {
			cleartoolExe = fixEmpty(req.getParameter("clearcase.cleartoolExe")
					.trim());
			String mergeTimeWindow = fixEmpty(req
					.getParameter("clearcase.logmergetimewindow"));
			if (mergeTimeWindow != null) {
				try {
					changeLogMergeTimeWindow = DecimalFormat
							.getIntegerInstance().parse(mergeTimeWindow)
							.intValue();
				} catch (ParseException e) {
					changeLogMergeTimeWindow = 5;
				}
			} else {
				changeLogMergeTimeWindow = 5;
			}
			save();
			return true;
		}

		@Override
		public SCM newInstance(StaplerRequest req) throws FormException {
			AbstractClearCaseScm scm = new ClearCaseSCM(
					req.getParameter("cc.branch"),
					req.getParameter("cc.configspec"),
					req.getParameter("cc.viewname"),
					req.getParameter("cc.useupdate") != null,
					req.getParameter("cc.loadrules"),
					req.getParameter("cc.usedynamicview") != null,
					req.getParameter("cc.viewdrive"),
					req.getParameter("cc.mkviewoptionalparam"),
					req.getParameter("cc.filterOutDestroySubBranchEvent") != null,
					req.getParameter("cc.doNotUpdateConfigSpec") != null,
					req.getParameter("cc.rmviewonrename") != null,
                                        req.getParameter("cc.excludedRegions")
                                                                    );			
			return scm;
		}

		/**
		 * Checks if cleartool executable exists.
		 */
		public void doCleartoolExeCheck(StaplerRequest req, StaplerResponse rsp)
				throws IOException, ServletException {
			new FormFieldValidator.Executable(req, rsp).process();
		}
                
                /**
                 * Validates the excludedRegions Regex
                 */
                public void doExcludedRegionsCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
                    new FormFieldValidator(req,rsp,false) {
                        protected void check() throws IOException, ServletException {
                            String v = fixEmptyAndTrim(request.getParameter("value"));
                            
                            if(v != null) {
                                String[] regions = v.split("[\\r\\n]+");
                                for (String region : regions) {
		                    try {
                                        Pattern.compile(region);
		                    }
		                    catch (PatternSyntaxException e) {
                                        error("Invalid regular expression. " + e.getMessage());
		                    }
                                }
                            }
                            ok();
                        }
                    }.process();
                }

		public void doConfigSpecCheck(StaplerRequest req, StaplerResponse rsp)
				throws IOException, ServletException {
			new FormFieldValidator(req, rsp, false) {
				@Override
				protected void check() throws IOException, ServletException {

					String v = fixEmpty(request.getParameter("value"));
					if ((v == null) || (v.length() == 0)) {
						error("Config spec is mandatory");
						return;
					}
                                        for (String cSpecLine : v.split("[\\r\\n]+")) {
                                            if (cSpecLine.startsWith("load ")) {
                                                error("Config spec can not contain load rules");
                                                return;
                                            }
                                        }
					// all tests passed so far
					ok();
				}
			}.process();
		}

		/**
		 * Raises an error if the parameter value isnt set.
		 * 
		 * @param req
		 *            containing the parameter value and the errorText to
		 *            display if the value isnt set
		 * @param rsp
		 * @throws IOException
		 * @throws ServletException
		 */
		public void doMandatoryCheck(StaplerRequest req, StaplerResponse rsp)
				throws IOException, ServletException {
			new FormFieldValidator(req, rsp, false) {
				@Override
				protected void check() throws IOException, ServletException {
					String v = fixEmpty(request.getParameter("value"));
					if (v == null) {
						error(fixEmpty(request.getParameter("errorText")));
						return;
					}
					// all tests passed so far
					ok();
				}
			}.process();
		}

		/**
		 * Displays "cleartool -version" for trouble shooting.
		 */
		public void doVersion(StaplerRequest req, StaplerResponse rsp)
				throws IOException, ServletException, InterruptedException {
			ByteBuffer baos = new ByteBuffer();
			try {
				Proc proc = Hudson.getInstance().createLauncher(
						TaskListener.NULL).launch(
						new String[] { getCleartoolExe(), "-version" },
						new String[0], baos, null);
				proc.join();
				rsp.setContentType("text/plain");
				baos.writeTo(rsp.getOutputStream());
			} catch (IOException e) {
				req.setAttribute("error", e);
				rsp.forward(this, "versionCheckError", req);
			}
		}

		public void doListViews(StaplerRequest req, StaplerResponse rsp)
				throws IOException, ServletException, InterruptedException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Proc proc = Hudson.getInstance().createLauncher(TaskListener.NULL)
					.launch(
							new String[] { getCleartoolExe(), "lsview",
									"-short" }, new String[0], baos, null);
			proc.join();
			rsp.setContentType("text/plain");
			rsp.getOutputStream().println("ClearCase Views found:\n");
			baos.writeTo(rsp.getOutputStream());
		}
	}
}
