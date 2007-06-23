package hudson.model;

import com.thoughtworks.xstream.XStream;
import hudson.XmlFile;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.util.XStream2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Convenience methods related to {@link Item}.
 * 
 * @author Kohsuke Kawaguchi
 */
public class Items {
    /**
     * List of all installed {@link TopLevelItem} types.
     */
    public static final List<TopLevelItemDescriptor> LIST = Descriptor.toList(
        Project.DESCRIPTOR,
        MavenModuleSet.DESCRIPTOR,
        //MatrixProject.DESCRIPTOR,
        ExternalJob.DESCRIPTOR
    );

    public static TopLevelItemDescriptor getDescriptor(String displayName) {
        for (TopLevelItemDescriptor job : LIST) {
            if(job.getDisplayName().equals(displayName))
                return job;
        }
        return null;
    }

    /**
     * Converts a list of items into a camma-separated full names.
     */
    public static String toNameList(Collection<? extends Item> items) {
        StringBuilder buf = new StringBuilder();
        for (Item item : items) {
            if(buf.length()>0)
                buf.append(", ");
            buf.append(item.getFullName());
        }
        return buf.toString();
    }

    /**
     * Does the opposite of {@link #toNameList(Collection)}.
     */
    public static <T extends Item> List<T> fromNameList(String list,Class<T> type) {
        Hudson hudson = Hudson.getInstance();

        List<T> r = new ArrayList<T>();
        StringTokenizer tokens = new StringTokenizer(list,",");
        while(tokens.hasMoreTokens()) {
            String fullName = tokens.nextToken().trim();
            T item = hudson.getItemByFullName(fullName,type);
            if(item!=null)
                r.add(item);
        }
        return r;
    }

    /**
     * Loads a {@link Item} from a config file.
     *
     * @param dir
     *      The directory that contains the config file, not the config file itself.
     */
    public static Item load(ItemGroup parent, File dir) throws IOException {
        Item item = (Item)getConfigFile(dir).read();
        item.onLoad(parent,dir.getName());
        return item;
    }

    /**
     * The file we save our configuration.
     */
    static XmlFile getConfigFile(File dir) {
        return new XmlFile(XSTREAM,new File(dir,"config.xml"));
    }

    /**
     * The file we save our configuration.
     */
    public static XmlFile getConfigFile(Item item) {
        return getConfigFile(item.getRootDir());
    }

    /**
     * Used to load/save job configuration.
     *
     * When you extend {@link Job} in a plugin, try to put the alias so
     * that it produces a reasonable XML.
     */
    public static final XStream XSTREAM = new XStream2();

    static {
        XSTREAM.alias("project",Project.class);
        XSTREAM.alias("maven2", MavenModule.class);
        XSTREAM.alias("maven2-module-set", MavenModule.class);
    }
}
