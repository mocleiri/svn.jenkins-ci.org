package hudson.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;

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
        // incompatible change. ouch
        // registerConverter(new EmulatedEnumConverter());
    }
}
