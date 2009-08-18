/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Yahoo! Inc.
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
package hudson.tasks.junit;

import hudson.model.FreeStyleProject;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.FreeStyleBuild;
import hudson.Launcher;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.Email;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.TestBuilder;
import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlElement;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class CaseResultTest extends HudsonTestCase {
//    /**
//     * Verifies that Hudson can capture the stdout/stderr output from Maven surefire.
//     */
//    public void testSurefireOutput() throws Exception {
//        setJavaNetCredential();
//        configureDefaultMaven();
//
//        MavenModuleSet p = createMavenProject();
//        p.setScm(new SubversionSCM("https://svn.dev.java.net/svn/hudson/trunk/hudson/test-projects/junit-failure@16411"));
//        MavenModuleSetBuild b = assertBuildStatus(UNSTABLE,p.scheduleBuild2(0).get());
//        AbstractTestResultAction<?> t = b.getTestResultAction();
//        assertSame(1,t.getFailCount());
//        CaseResult tc = t.getFailedTests().get(0);
//        assertTrue(tc.getStderr().contains("stderr"));
//        assertTrue(tc.getStdout().contains("stdout"));
//    }

    @Email("http://www.nabble.com/NPE-%28Fatal%3A-Null%29-in-recording-junit-test-results-td23562964.html")
    public void testIssue20090516() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("junit.xml").copyFrom(
                    getClass().getResource("junit-report-20090516.xml"));
                return true;
            }
        });
        p.getPublishersList().add(new JUnitResultArchiver("*.xml"));
        FreeStyleBuild b = assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
        TestResult tr = b.getAction(TestResultAction.class).getResult();
        assertEquals(3,tr.getFailedTests().size());
        CaseResult cr = tr.getFailedTests().get(0);
        assertEquals("org.twia.vendor.VendorManagerTest",cr.getClassName());
        assertEquals("testGetVendorFirmKeyForVendorRep",cr.getName());


        // piggy back tests for annotate methods
        assertOutput(cr,"plain text", "plain text");
        assertOutput(cr,"line #1\nhttp://nowhere.net/\nline #2\n",
                "line #1\n<a href=\"http://nowhere.net/\">http://nowhere.net/</a>\nline #2\n");
        assertOutput(cr,"failed; see http://nowhere.net/",
                "failed; see <a href=\"http://nowhere.net/\">http://nowhere.net/</a>");
        assertOutput(cr,"failed (see http://nowhere.net/)",
                "failed (see <a href=\"http://nowhere.net/\">http://nowhere.net/</a>)");
        assertOutput(cr,"http://nowhere.net/ - failed: http://elsewhere.net/",
                "<a href=\"http://nowhere.net/\">http://nowhere.net/</a> - failed: " +
                "<a href=\"http://elsewhere.net/\">http://elsewhere.net/</a>");
        assertOutput(cr,"https://nowhere.net/",
                "<a href=\"https://nowhere.net/\">https://nowhere.net/</a>");
        assertOutput(cr,"stuffhttp://nowhere.net/", "stuffhttp://nowhere.net/");
        assertOutput(cr,"a < b && c < d", "a &lt; b &amp;&amp; c &lt; d");
        assertOutput(cr,"see <http://nowhere.net/>",
                "see &lt;<a href=\"http://nowhere.net/\">http://nowhere.net/</a>>");
        assertOutput(cr,"http://google.com/?q=stuff&lang=en",
                "<a href=\"http://google.com/?q=stuff&amp;lang=en\">http://google.com/?q=stuff&amp;lang=en</a>");
        assertOutput(cr,"http://localhost:8080/stuff/",
                "<a href=\"http://localhost:8080/stuff/\">http://localhost:8080/stuff/</a>");
    }


    /**
     * Verifies that the error message and stacktrace from a failed junit test actually render properly.
     */
    @Bug(4257)
    public void testErrorMsgAndStacktraceRender() throws Exception {
        FreeStyleProject p = createFreeStyleProject("render-test");
        p.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("junit.xml").copyFrom(
                    getClass().getResource("junit-report-20090516.xml"));
                return true;
            }
        });
        p.getPublishersList().add(new JUnitResultArchiver("*.xml"));
        FreeStyleBuild b = assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
        TestResult tr = b.getAction(TestResultAction.class).getResult();
        assertEquals(3,tr.getFailedTests().size());
        CaseResult cr = tr.getFailedTests().get(1);
        assertEquals("org.twia.vendor.VendorManagerTest",cr.getClassName());
        assertEquals("testGetRevokedClaimsForAdjustingFirm",cr.getName());

	String testUrl = cr.getRelativePathFrom(tr);
	
	HtmlPage page = new WebClient().goTo("job/render-test/1/testReport/" + testUrl);

	HtmlElement errorMsg = (HtmlElement) page.getByXPath("//h3[text()='Error Message']/following-sibling::*").get(0);
	assertEquals("\"" + cr.annotate(cr.getErrorDetails()) + "\"", errorMsg.getTextContent());
	HtmlElement errorStackTrace = (HtmlElement) page.getByXPath("//h3[text()='Stacktrace']/following-sibling::*").get(0);

	// Have to do some annoying replacing here to get the same text Jelly produces in the end.
	assertEquals(cr.annotate(cr.getErrorStackTrace()).replaceAll("&lt;", "<").replace("\r\n", "\n"),
		     errorStackTrace.getTextContent());
    }


    private void assertOutput(CaseResult cr, String in, String out) throws Exception {
        assertEquals(out, cr.annotate(in));
    }

}
