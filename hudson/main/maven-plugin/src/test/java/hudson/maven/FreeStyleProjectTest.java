/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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
package hudson.maven;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import hudson.model.FreeStyleProject;
import hudson.model.AbstractProject;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class FreeStyleProjectTest extends MavenTestCase {

    /**
     * Make sure that the pseudo trigger configuration works.
     */
    @Bug(2778)
    public void testUpstreamPseudoTrigger() throws Exception {
        pseudoTriggerTest(createMavenProject(), createFreeStyleProject());
    }

    private void pseudoTriggerTest(AbstractProject up, AbstractProject down) throws Exception {
        HtmlForm form = new WebClient().getPage(down, "configure").getFormByName("config");
        form.getInputByName("pseudoUpstreamTrigger").setChecked(true);
        form.getInputByName("upstreamProjects").setValueAttribute(up.getName());
        submit(form);

        // make sure this took effect
        assertTrue(up.getDownstreamProjects().contains(down));
        assertTrue(down.getUpstreamProjects().contains(up));

        // round trip again and verify that the configuration is still intact.
        submit(new WebClient().getPage(down, "configure").getFormByName("config"));
        assertTrue(up.getDownstreamProjects().contains(down));
        assertTrue(down.getUpstreamProjects().contains(up));
    }

}