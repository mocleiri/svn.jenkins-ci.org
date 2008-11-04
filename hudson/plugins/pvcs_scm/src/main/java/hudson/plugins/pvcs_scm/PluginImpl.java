package hudson.plugins.pvcs_scm;

import hudson.Plugin;
import hudson.scm.SCMS;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Concrete Plugin implementation for the PVCS SCM plugin.
 *
 * @author Brian Lalor &lt;blalor@bravo5.org&gt;
 */
public class PluginImpl extends Plugin
{
    private final Log logger = LogFactory.getLog(getClass());

    // {{{ start
    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws Exception {
        SCMS.SCMS.add(PvcsScm.DescriptorImpl.DESCRIPTOR);

        super.start();
    }
    // }}}
}
