/**
 * 
 */
package hudson.plugins.clearcase;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * Defines constants and pattern match regexp for interesting view properties.
 * 
 * @author jborghi
 */
public enum ClearToolViewProp {

    /**
     * View uuid.
     */
    UUID("View uuid: (.*)"),
    
    /*
     * View storage directory (may be local to host).
     */
    STORAGE_DIR("View server access path: (.*)"),
    
    /*
     * Global view storage directory.
     */
    GLOBAL_PATH("Global path: (.+)");

    /**
     * Compiled regular expression to capture this property when
     * parsing the output of "cleartool lsview -l <viewname>".
     */
    private Pattern pattern;

    ClearToolViewProp(final String regexp) {
        try {
            this.pattern = Pattern.compile(regexp);
        } catch (PatternSyntaxException e) {
            this.pattern = null;
        }
    }
    
    /**
     * Gets the compiled regular expression pattern for this property.
     * @return the pattern.
     */
    public Pattern getPattern() {
        return pattern;
    }
}
