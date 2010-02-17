package hudson.console;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.Launcher;
import hudson.MarkupText;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.TestExtension;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class ConsoleAnnotatorTest extends HudsonTestCase {
    /**
     * Let the build complete, and see if stateless {@link ConsoleAnnotator} annotations happen as expected.
     */
    public void testCompletedStatelessLogAnnotation() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                listener.getLogger().println("---");
                listener.getLogger().println("ooo");
                listener.getLogger().println("ooo");
                return true;
            }
        });

        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0).get());

        // make sure we see the annotation
        HtmlPage rsp = createWebClient().getPage(b, "console");
        assertEquals(1,rsp.selectNodes("//B[@class='demo']").size());

        // there should be two 'ooo's
        assertEquals(3,rsp.asXml().split("ooo").length);
    }

    /**
     * Only annotates the first occurrence of "ooo".
     */
    @TestExtension
    public static class DemoAnnotator extends ConsoleAnnotator {
        @Override
        public ConsoleAnnotator annotate(Run<?, ?> build, MarkupText text) {
            if (text.getText().equals("ooo\n")) {
                text.addMarkup(0,3,"<b class=demo>","</b>");
                return null;
            }
            return this;
        }
    }
}
