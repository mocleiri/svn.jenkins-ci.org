/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.junitattachments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.DirectoryScanner;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;

/**
 * This class is a helper for hudson.tasks.junit.TestDataPublisher.getTestData(AbstractBuild<?, ?>, Launcher, BuildListener, TestResult).
 *
 * @author mfriedenhagen
 */
public class GetTestDataMethodObject {

    /** the build to inspect. */
    private final AbstractBuild<?, ?> build;

    /** the launcher of the build. */
    private final Launcher launcher;

    /** the listener which triggered the invocation. */
    private final BuildListener listener;

    /** the test results associated with the build. */
    private final TestResult testResult;

    /**
     * @param build see {@link GetTestDataMethodObject#build}
     * @param launcher see {@link GetTestDataMethodObject#launcher}
     * @param listener see {@link GetTestDataMethodObject#listener}
     * @param testResult see {@link GetTestDataMethodObject#testResult}
     */
    public GetTestDataMethodObject(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener,
            TestResult testResult) {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
        this.testResult = testResult;
    }

    /**
     * Returns a Map of classname vs. the stored attachments in a directory called as the test class.
     *
     * @return the map
     * @throws InterruptedException
     * @throws IOException
     * @throws IllegalStateException
     *
     */
    public Map<String, List<String>> getAttachments() throws IllegalStateException, IOException, InterruptedException {
        // build a map of className -> result xml file
        final Map<String, String> reports = new HashMap<String, String>();
        for (SuiteResult suiteResult : testResult.getSuites()) {
            String f = suiteResult.getFile();
            if (f != null) {
                for (String className : suiteResult.getClassNames()) {
                    reports.put(className, f);
                }
            }
        }

        final FilePath attachmentsStorage = AttachmentPublisher.getAttachmentPath(build);

        Map<String, List<String>> attachments = new HashMap<String, List<String>>();
        System.err.println("YYYYYYYYYYYYY" + reports);
        for (Map.Entry<String, String> report : reports.entrySet()) {
            String className = report.getKey();
            FilePath target = attachmentsStorage.child(className);
            FilePath testDir = build.getWorkspace().child(report.getValue())
            .getParent().child(className);
            if (testDir.exists()) {
                target.mkdirs();
                if (testDir.copyRecursiveTo(target) > 0) {
                    DirectoryScanner d = new DirectoryScanner();
                    d.setBasedir(target.getRemote());
                    d.scan();
                    attachments.put(className, Arrays.asList(d
                            .getIncludedFiles()));
                }
            }
            final FilePath stdInAndOut = build.getWorkspace().child(report.getValue()).getParent().child(
                    className + "-output.txt");
            System.err.println("XXXXXXXXXXXX" + stdInAndOut.absolutize());
            if (stdInAndOut.exists()) {
                target.mkdirs();
                final FilePath stdInAndOutTarget = new FilePath(target, "stdin-stdout.txt");
                stdInAndOut.copyTo(stdInAndOutTarget);
                if (attachments.containsKey(className)) {
                    final List<String> list = new ArrayList<String>(attachments.get(className));
                    list.add(stdInAndOutTarget.getName());
                    attachments.put(className, list);
                } else {
                    attachments.put(className, Arrays.asList(stdInAndOutTarget.getName()));
                }
            }
        }
        return attachments;
    }

}
