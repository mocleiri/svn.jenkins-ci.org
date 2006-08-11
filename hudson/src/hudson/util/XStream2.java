package hudson.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import hudson.model.Hudson;

/**
 * {@link XStream} enhanced for retroweaver support.
 * @author Kohsuke Kawaguchi
 */
public class XStream2 extends XStream {
    public XStream2() {
        init();
    }

    public XStream2(HierarchicalStreamDriver hierarchicalStreamDriver) {
        super(hierarchicalStreamDriver);
        init();
    }

    private void init() {
        registerConverter(new RobustCollectionConverter(getClassMapper()),10);

        // defensive because some use of XStream happens before plugins are initialized.
        Hudson h = Hudson.getInstance();
        if(h!=null && h.pluginManager!=null && h.pluginManager.uberClassLoader!=null) {
            setClassLoader(h.pluginManager.uberClassLoader);
        }
    }
}
