package hudson.os.windows.wmi;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.kohsuke.jinterop.JIProxy;
import org.kohsuke.jinterop.Property;

/**
 * @author Kohsuke Kawaguchi
 */
public interface SWbemObjectSet extends JIProxy, Iterable<SWbemObject> {
    @Property
    int Count() throws JIException;
    IJIDispatch Item(String path/*?*/) throws JIException;
}
