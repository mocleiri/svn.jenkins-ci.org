package hudson.plugins.ui_samples;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Action;
import hudson.model.Describable;
import hudson.model.Hudson;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.io.IOUtils.copy;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class UISample implements ExtensionPoint, Action, Describable<UISample> {
    public String getIconFileName() {
        return "gear.gif";
    }

    public String getUrlName() {
        return getClass().getSimpleName();
    }

    /**
     * Default display name.
     */
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    /**
     * Source files associated with this sample.
     */
    public List<SourceFile> getSourceFiles() {
        return Arrays.asList(new SourceFile(getClass().getSimpleName()+".java"));
    }

    /**
     * Binds {@link SourceFile}s into URL.
     */
    public SourceFile getSourceFile(String name) {
        for (SourceFile sf : getSourceFiles())
            if (sf.name.equals(name))
                return sf;
        return null;
    }

    /**
     * Returns a paragraph of natural text that describes this sample.
     * Interpreted as HTML.
     */
    public abstract String getDescription();

    public UISampleDescriptor getDescriptor() {
        return (UISampleDescriptor)Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Returns all the registered {@link UISample}s.
     */
    public static ExtensionList<UISample> all() {
        return Hudson.getInstance().getExtensionList(UISample.class);
    }

    /**
     * @author Kohsuke Kawaguchi
     */
    public class SourceFile {
        public final String name;

        public SourceFile(String name) {
            this.name = name;
        }

        public URL resolve() {
            return UISample.this.getClass().getResource(name);
        }

        /**
         * Serves this source file.
         */
        public void doIndex(StaplerResponse rsp) throws IOException {
            rsp.setContentType("text/plain;charset=UTF-8");
            copy(resolve().openStream(),rsp.getOutputStream());
        }
    }

}
