package hudson.maven;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;

import java.util.List;

import hudson.tasks.Publisher;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.model.Job;

/**
 * Click all the help links and make sure they resolve to some text, not 404.
 *
 * @author Kohsuke Kawaguchi
 */
public class HelpLinkTest extends MavenTestCase {

    public void testMavenConfig() throws Exception {
        clickAllHelpLinks(createMavenProject());
    }

    private void clickAllHelpLinks(Job j) throws Exception {
        clickAllHelpLinks(new WebClient().getPage(j,"configure"));
    }

    private void clickAllHelpLinks(HtmlPage p) throws Exception {
        List<?> helpLinks = p.selectNodes("//a[@class='help-button']");
        assertTrue(helpLinks.size()>0);
        System.out.println("Clicking "+helpLinks.size()+" help links");

        for (HtmlAnchor helpLink : (List<HtmlAnchor>)helpLinks)
            helpLink.click();
    }

    public static class HelpNotFoundBuilder extends Builder {
        public static final class DescriptorImpl extends BuildStepDescriptor {
            public boolean isApplicable(Class jobType) {
                return true;
            }

            @Override
            public String getHelpFile() {
                return "no-such-file/exists";
            }

            public String getDisplayName() {
                return "I don't have the help file";
            }
        }
    }

}