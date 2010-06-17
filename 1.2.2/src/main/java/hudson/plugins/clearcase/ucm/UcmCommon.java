package hudson.plugins.clearcase.ucm;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.clearcase.ClearCaseDataAction;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolLauncher;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author kyosi
 */
public class UcmCommon {
    
    private static final String CONFIGURED_STREAM_VIEW_SUFFIX = "_hudson_freeze_view";
    private static final String BUILD_STREAM_PREFIX = "hudson_freeze_stream";
    private static final String BASELINE_NAME = "hudson_co_";
    private static final String BASELINE_COMMENT = "hudson_co_";

    /**
     * @param clearToolLauncher
     * @param isUseDynamicView
     * @param viewName
     * @param filePath
     * @param baselineName
     * @param baselineComment
     * @param identical
     * @param fullBaseline
     * @param readWriteComponents
     * @return list of created baselines
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<BaselineDesc> makeBaseline(ClearToolLauncher clearToolLauncher, 
                                      boolean isUseDynamicView,
                                      String viewName,
                                      FilePath filePath, 
                                      String baselineName, 
                                      String baselineComment,                                      
                                      boolean identical, 
                                      boolean fullBaseline,
                                      List<String> readWriteComponents) throws IOException, InterruptedException  {

        List<BaselineDesc> createdBaselinesList = new ArrayList<BaselineDesc>();

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("mkbl");
        if (identical) {
            cmd.add("-identical");
        }
        cmd.add("-comment");
        cmd.add(baselineComment);
        if (fullBaseline) {
            cmd.add("-full");
        } else {
            cmd.add("-incremental");
        }

        FilePath clearToolLauncherPath = filePath;
        if (isUseDynamicView) {
            cmd.add("-view");
            cmd.add(viewName);
            clearToolLauncherPath = clearToolLauncher.getWorkspace();
        }

        // Make baseline only for read/write components (identical or not)
        if (readWriteComponents != null) {
            cmd.add("-comp");
            StringBuffer lstComp = new StringBuffer();
            for (String comp : readWriteComponents) {
                lstComp.append(",");
                lstComp.append(comp);
            }
            lstComp.delete(0, 1);
            cmd.add(lstComp.toString());
        }

        cmd.add(baselineName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, clearToolLauncherPath);
        baos.close();
        String cleartoolResult = baos.toString();
        if (cleartoolResult.contains("cleartool: Error")) {
            throw new IOException("Failed to make baseline, reason: " + cleartoolResult);
        }

        Pattern pattern = Pattern.compile("Created baseline \".+?\" .+? \".+?\"");
        Matcher matcher = pattern.matcher(cleartoolResult);
        while (matcher.find()) {
            String match = matcher.group();
            String[] parts = match.split("\"");
            String newBaseline = parts[1];
            String componentName = parts[3];

            createdBaselinesList.add(new BaselineDesc(newBaseline, componentName));
        }

        return createdBaselinesList;
    }

    /**
     * @param clearToolLauncher
     * @param viewName
     * @param readWriteComponents . if null both baselines on read and read-write components will be returned
     * @return List of latest baselines on read write components (only)
     * @throws InterruptedException
     * @throws IOException
     * @throws Exception
     */
    public static List<String> getLatestBaselineNames(final ClearToolLauncher clearToolLauncher, final String viewName, final List<String> readWriteComponents) throws IOException, InterruptedException {
        final ArgumentListBuilder cmd = new ArgumentListBuilder();
        final List<String> baselineNames = new ArrayList<String>();

        cmd.add("lsstream");
        cmd.add("-view");
        cmd.add(viewName);
        cmd.add("-fmt");
        cmd.add("%[latest_bls]Xp");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        baos.close();
        final String cleartoolResult = baos.toString();
        final String prefix = "baseline:";
        if (cleartoolResult != null && cleartoolResult.startsWith(prefix)) {
            final String[] baselineNamesSplit = cleartoolResult.split(prefix);
            for (final String baselineName : baselineNamesSplit) {
                final String baselineNameTrimmed = baselineName.trim();
                if (!baselineNameTrimmed.equals("")) {
                    // Retrict to baseline bind to read/write component
                    final String blComp = getDataforBaseline(clearToolLauncher, baselineNameTrimmed).getComponentName();
                    if (readWriteComponents == null || readWriteComponents.contains(blComp)) {
                        baselineNames.add(baselineNameTrimmed);
                    }
                }
            }
        }
        return baselineNames;
    }

    /**
     * @param clearToolLauncher
     * @param componentsList
     * @param baselinesNames
     * @return list of BaselineDesc (baseline name and component name)
     * @throws InterruptedException
     * @throws IOException
     */
    public static List<BaselineDesc> getComponentsForBaselines(final ClearToolLauncher clearToolLauncher, final List<ComponentDesc> componentsList,
            final List<String> baselinesNames) throws InterruptedException, IOException {
        final List<BaselineDesc> baselinesDescList = new ArrayList<BaselineDesc>();

        // loop through baselines
        for (final String blName : baselinesNames) {
            final BaselineDesc baseLineDesc = getDataforBaseline(clearToolLauncher, blName);
            ComponentDesc matchComponentDesc = null;

            // find the equivalent componentDesc element
            for (final ComponentDesc componentDesc : componentsList) {
                if (getNoVob(componentDesc.getName()).equals(getNoVob(baseLineDesc.getComponentName()))) {
                    matchComponentDesc = componentDesc;
                    break;
                }
            }
            baselinesDescList.add(new BaselineDesc(blName, matchComponentDesc, baseLineDesc.isNotLabeled));
        }
        return baselinesDescList;
    }

    /**
     * Get the component binding to the baseline
     * 
     * @param clearToolLauncher
     * @param blName the baseline name like 'deskCore_3.2-146_2008-11-14_18-07-22.3543@\P_ORC'
     * @return A BaselineDesc object with the component name like 'Desk_Core@\P_ORC'
     * @throws InterruptedException
     * @throws IOException
     */
    public static BaselineDesc getDataforBaseline(final ClearToolLauncher clearToolLauncher, final String blName) throws InterruptedException, IOException {

        final ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("lsbl");
        cmd.add("-fmt");
        cmd.add("%[label_status]p|%[component]Xp");
        cmd.add(blName);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        baos.close();
        final String cleartoolResult = baos.toString();
        final String[] arr = cleartoolResult.split("\\|");
        final boolean isNotLabeled = arr[0].contains("Not Labeled");

        final String prefix = "component:";
        final String componentName = arr[1].substring(cleartoolResult.indexOf(cleartoolResult) + prefix.length());

        return new BaselineDesc(componentName, isNotLabeled);
    }

    /**
     * @param clearToolLauncher
     * @param parentStream
     * @param childStream
     * @throws IOException
     * @throws InterruptedException
     */
    public static void mkstream(ClearToolLauncher clearToolLauncher, String parentStream, String childStream) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("mkstream");
        cmd.add("-in");
        cmd.add(parentStream);
        cmd.add(childStream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        baos.close();
    }

    /**
     * @param clearToolLauncher
     * @param stream
     * @return boolean indicating if the given stream exists
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean isStreamExists(ClearToolLauncher clearToolLauncher, String stream) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("lsstream");
        cmd.add("-short");
        cmd.add(stream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        } catch (Exception e) {
            // empty by design
        }
        baos.close();
        String cleartoolResult = baos.toString();
        return !(cleartoolResult.contains("stream not found"));
    }

    /**
     * @param clearToolLauncher
     * @param streamName
     * @return list of components - name and isModifiable
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<ComponentDesc> getStreamComponentsDesc(ClearToolLauncher clearToolLauncher, String streamName) throws IOException, InterruptedException {
        List<ComponentDesc> componentsDescList = new ArrayList<ComponentDesc>();
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("desc");
        cmd.add("stream:" + streamName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        baos.close();
        String cleartoolResult = baos.toString();

        // searching in the result for the pattern (<component-name> (modifiable | non-modifiable)
        int idx = 0;
        int idx1;
        int idx2;
        String searchFor = "modifiable)";
        while (idx >= 0) {
            idx = cleartoolResult.indexOf(searchFor, idx + 1);

            if (idx > 0) {
                // get the component state part: modifiable or non-modifiable
                idx1 = cleartoolResult.lastIndexOf("(", idx - 1);
                idx2 = cleartoolResult.indexOf(")", idx1);
                String componentState = cleartoolResult.substring(idx1 + 1, idx2);

                // get the component name
                idx1 = cleartoolResult.lastIndexOf("(", idx1 - 1);
                idx2 = cleartoolResult.indexOf(")", idx1);

                // add to the result
                ComponentDesc componentDesc = new ComponentDesc(cleartoolResult.substring(idx1 + 1, idx2), componentState.equals("modifiable"));
                componentsDescList.add(componentDesc);
            }
        }

        return componentsDescList;
    }

    /**
     * @param clearToolLauncher the ClearToolLauncher.
     * @param stream the stream name.
     * @param view the view name.
     * 
     * @return List of latest BaseLineDesc (baseline + component) for stream. Only baselines on read-write components
     *         are returned
     * @throws InterruptedException
     * @throws IOException
     * @throws Exception
     */
    public static List<BaselineDesc> getLatestBlsWithCompOnStream(final ClearToolLauncher clearToolLauncher, final String stream, final String view) throws IOException,
        InterruptedException {
        // get the components on the build stream
        final List<ComponentDesc> componentsList = getStreamComponentsDesc(clearToolLauncher, stream);

        // get latest baselines on the stream (name only)
        final List<String> latestBlsOnBuildStream = getLatestBaselineNames(clearToolLauncher, view, null);

        // add component information to baselines
        final List<BaselineDesc> latestBlsWithComp = getComponentsForBaselines(clearToolLauncher, componentsList, latestBlsOnBuildStream);

        return latestBlsWithComp;
    }

    /**
     * @param componentsDesc
     * @return list of read-write components out of components list (removing read-only components)
     */
    public static List<String> getReadWriteComponents(List<ComponentDesc> componentsDesc) {
        List<String> res = new ArrayList<String>();

        for (ComponentDesc comp : componentsDesc)
            res.add(comp.getName());

        return res;
    }

    /**
     * @param clearToolLauncher
     * @param viewRootDirectory
     * @param bl1
     * @param bl2
     * @return list of versions that were changed between two baselines
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<String> getDiffBlVersions(ClearToolLauncher clearToolLauncher, String viewRootDirectory, String bl1, String bl2) throws IOException,
            InterruptedException {
        List<String> versionList = new ArrayList<String>();

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        FilePath clearToolLauncherPath = new FilePath(clearToolLauncher.getLauncher().getChannel(), viewRootDirectory);

        cmd.add("diffbl");
        cmd.add("-versions");
        cmd.add(bl1);
        cmd.add(bl2);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, clearToolLauncherPath);
        baos.close();
        String cleartoolResult = baos.toString();

        // remove ">>" from result
        String[] arr = cleartoolResult.split("\n");
        for (String line : arr) {
            if (line.startsWith(">>")) {
                line = line.replaceAll(">>", "");
                versionList.add(line.trim());
            }
        }

        return versionList;
    }

    /**
     * @param clearToolLauncher
     * @param version
     * @return version description
     * @throws IOException
     * @throws InterruptedException
     */
    public static String getVersionDescription(ClearToolLauncher clearToolLauncher, String version, String format) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("desc");
        cmd.add("-fmt");
        cmd.add(format);
        cmd.add(version);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        baos.close();
        String cleartoolResult = baos.toString();

        return cleartoolResult;
    }

    /**
     * @param clearToolLauncher
     * @param stream
     * @param baselines
     * @throws IOException
     * @throws InterruptedException
     */
    public static void rebase(ClearToolLauncher clearToolLauncher, String viewName, List<UcmCommon.BaselineDesc> baselines) throws IOException,
            InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("rebase");

        cmd.add("-base");
        String baselineStr = "";
        for (UcmCommon.BaselineDesc bl : baselines) {
            if (baselineStr.length() > 0)
                baselineStr += ",";
            baselineStr += bl.getBaselineName();
        }
        cmd.add(baselineStr);

        cmd.add("-view");
        cmd.add(viewName);

        cmd.add("-complete");
        cmd.add("-force");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        baos.close();
    }

    /**
     * @param clearToolLauncher
     * @param element
     * @return
     */
    public static String getNoVob(String element) {
        return element.split("@")[0];
    }

    public static String getVob(String element) {
        return element.split("@")[1];
    }

    /**
     * Determines the name of the frozen build stream for the job, 
     * and creates the stream if it does not already exist.
     * This is used with the view name configured for the job.
     * 
     * @param cleartool The cleartool instance for running commands.
     * @param build The build we are creating the stream for.
     * @param stream The stream configured for the job, to be used as the parent stream
     *               for the frozen build stream.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static String prepareFrozenBuildStream(final ClearTool cleartool, final AbstractBuild<?, ?> build, final String stream) throws IOException, InterruptedException {
        final String frozenStream = BUILD_STREAM_PREFIX + "_" + build.getProject().getName().replace(" ", "") + "." + stream;

        if (!isStreamExists(cleartool.getLauncher(), frozenStream)) {
            mkstream(cleartool.getLauncher(), stream, frozenStream);
        }
        return frozenStream;
    }

    /**
     * Returns the name of the view that is based on the configured stream (entered by the user). 
     * This is needed in the case where we are creating a frozen ucm view for the build.
     * <p>
     * We need two streams and two associated views to accomplish this.  The configured view 
     * name is used for the build, and is based on the frozen substream we create.  The configured
     * stream has the baselines we build to, and we calculate a view name to use with this stream.
     * 
     * @param build The build we are getting the view name for.
     * @param stream The configured stream entered by the user.
     * @return The view name to use for this stream.
     */
    public static String getConfiguredStreamViewName(final AbstractBuild<?, ?> build, final String stream) {
        return UcmCommon.getNoVob(stream) + "_" + build.getProject().getName().replace(" ", "") + "_" + CONFIGURED_STREAM_VIEW_SUFFIX;
    }
    
    /**
     * Creates a frozen build stream and does a checkout of code into the specified view.
     * @param cleartool
     * @param build
     * @param viewName
     * @param stream
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean checkoutCodeFreeze(final ClearTool cleartool, final AbstractBuild<?, ?> build, final String viewName, final String stream) throws IOException, InterruptedException {
        // validate no other build is running on the same stream
        synchronized (build.getProject()) {
            ClearCaseDataAction clearcaseDataAction = null;
            Run previousBuild = build.getPreviousBuild();
            while (previousBuild != null) {
                clearcaseDataAction = previousBuild.getAction(ClearCaseDataAction.class);

                if (previousBuild.isBuilding() && clearcaseDataAction != null && clearcaseDataAction.getStream().equals(stream))
                    throw new IOException("Can't run build on stream " + stream + " when build " + previousBuild.getNumber()
                            + " is currently running on the same stream.");

                previousBuild = previousBuild.getPreviousBuild();
            }
        }
        // prepare stream and views
        //prepareBuildStreamAndViews(viewName, stream);
        cleartool.mountVobs();

        final String configuredStreamViewName = getConfiguredStreamViewName(build, stream);        
        final String frozenBuildStream = prepareFrozenBuildStream(cleartool, build, stream);

        cleartool.prepareView(configuredStreamViewName, stream);
        cleartool.prepareView(viewName, frozenBuildStream);
        
        // make baselines
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy_HH_mm_ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateStr = formatter.format(build.getTimestamp().getTime()).toLowerCase();

        UcmCommon.makeBaseline(cleartool.getLauncher(), true, configuredStreamViewName, null, BASELINE_NAME + dateStr, BASELINE_COMMENT + dateStr, false,
                false, null);

        // get latest baselines on the configured stream
        List<UcmCommon.BaselineDesc> latestBlsOnConfgiuredStream = UcmCommon.getLatestBlsWithCompOnStream(cleartool.getLauncher(), stream,
                configuredStreamViewName);

        // fix Not labeled baselines
        for (BaselineDesc baseLineDesc : latestBlsOnConfgiuredStream) {
            if (baseLineDesc.isNotLabeled() && baseLineDesc.getComponentDesc().isModifiable()) {
                // if the base is not labeled create identical one
                List<String> readWriteCompList = new ArrayList<String>();
                readWriteCompList.add(baseLineDesc.getComponentDesc().getName());

                List<BaselineDesc> baseLineDescList = UcmCommon.makeBaseline(cleartool.getLauncher(), true, configuredStreamViewName, null, BASELINE_NAME
                        + dateStr, BASELINE_COMMENT + dateStr, true, false, readWriteCompList);

                String newBaseline = baseLineDescList.get(0).getBaselineName() + "@" + UcmCommon.getVob(baseLineDesc.getComponentDesc().getName());

                baseLineDesc.setBaselineName(newBaseline);
            }
        }

        // rebase build stream
        UcmCommon.rebase(cleartool.getLauncher(), viewName, latestBlsOnConfgiuredStream);

        // add baselines to build - to be later used by getChange
        ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
        if (dataAction != null)
            dataAction.setLatestBlsOnConfiguredStream(latestBlsOnConfgiuredStream);

        return true;
    }


    /**
     * @author kyosi
     */
    @ExportedBean
    public static class BaselineDesc {

        @Exported(visibility = 3)
        public String baselineName;

        @Exported(visibility = 3)
        public String componentName;

        @Exported(visibility = 3)
        public ComponentDesc componentDesc;

        private boolean isNotLabeled;

        public BaselineDesc(String componentName, boolean isNotLabeled) {
            super();
            this.componentName = componentName;
            this.isNotLabeled = isNotLabeled;
        }

        public BaselineDesc(String baselineName, String componentName) {
            super();
            this.baselineName = baselineName;
            this.componentName = componentName;
            this.componentDesc = null;
        }

        public BaselineDesc(String baselineName, ComponentDesc componentDesc) {
            super();
            this.baselineName = baselineName;
            this.componentDesc = componentDesc;
            this.componentName = componentDesc.getName();
        }

        public BaselineDesc(String baselineName, ComponentDesc componentDesc, boolean isNotLabeled) {
            super();
            this.baselineName = baselineName;
            this.componentDesc = componentDesc;
            this.componentName = componentDesc.getName();
            this.isNotLabeled = isNotLabeled;
        }

        public String getBaselineName() {
            return baselineName;
        }

        public void setBaselineName(String baselineName) {
            this.baselineName = baselineName;
        }

        public String getComponentName() {
            return (componentDesc != null ? componentDesc.getName() : componentName);
        }

        public void setComponentName(String componentName) {
            this.componentName = componentName;
        }

        public ComponentDesc getComponentDesc() {
            return componentDesc;
        }

        public void setComponentDesc(ComponentDesc componentDesc) {
            this.componentDesc = componentDesc;
        }

        public boolean isNotLabeled() {
            return isNotLabeled;
        }

        public void setNotLabeled(boolean isNotLabeled) {
            this.isNotLabeled = isNotLabeled;
        }
    }

    /**
     * @author kyosi
     */
    @ExportedBean
    public static class ComponentDesc {

        @Exported(visibility = 3)
        public String name;

        @Exported(visibility = 3)
        public boolean isModifiable;

        public ComponentDesc(String name, boolean isModifiable) {
            this.name = name;
            this.isModifiable = isModifiable;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isModifiable() {
            return isModifiable;
        }

        public void setModifiable(boolean isModifiable) {
            this.isModifiable = isModifiable;
        }
    }

}
