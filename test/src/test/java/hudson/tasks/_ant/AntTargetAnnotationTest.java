package hudson.tasks._ant;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Ant;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.SingleFileSCM;

/**
 * @author Kohsuke Kawaguchi
 */
public class AntTargetAnnotationTest extends HudsonTestCase {
    public void test1() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new Ant("foo",null,null,null,null));
        p.setScm(new SingleFileSCM("build.xml",getClass().getResource("simple-build.xml")));
        FreeStyleBuild b = buildAndAssertSuccess(p);

        interactiveBreak();
        
        HtmlPage c = createWebClient().getPage(b, "console");
        System.out.println(c.asText());

        HtmlElement o = c.getElementById("console-outline");
        assertEquals(2,o.selectNodes("LI").size());
    }
}
