package hudson.model;

import junit.framework.TestCase;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;

/**
 * Quick test for {@link UpdateCenter}.
 * 
 * @author Kohsuke Kawaguchi
 */
public class UpdateCenterTest extends TestCase {
    public void testData() throws IOException {
        URL url = new URL("https://hudson.dev.java.net/update-center.json?version=build");
        String jsonp = IOUtils.toString(url.openStream());
        String json = jsonp.substring(jsonp.indexOf('(')+1,jsonp.lastIndexOf(')'));

        UpdateCenter.Data data = new UpdateCenter.Data(JSONObject.fromObject(json));
        assertTrue(data.core.url.startsWith("https://hudson.dev.java.net/"));
        assertTrue(data.plugins.containsKey("rake"));
        System.out.println(data.core.url);
    }
}
