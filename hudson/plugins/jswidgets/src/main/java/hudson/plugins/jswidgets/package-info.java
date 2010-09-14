/**
 * <p>This package contains all classes for the
 * <a href="http://wiki.hudson-ci.org/display/HUDSON/JSWidgets+Plugin">JSWidgets Plugin</a>.</p>
 * 
 * <p>The widgets do only render information generated by other Plugins or the core.</p>
 * 
 * <p>A note about Logging:</p>
 * <ul>
 * <li><tt>LOG.fine</tt> is used, where Builds or Jobs are modified resp. Actions attached, see {@link JsRunListener} 
 * or {@link JsJobAction}.</li>
 * <li><tt>LOG.finest</tt> is used, where values are calculated for rendering.</li>
 * </ul>
 */
package hudson.plugins.jswidgets;
