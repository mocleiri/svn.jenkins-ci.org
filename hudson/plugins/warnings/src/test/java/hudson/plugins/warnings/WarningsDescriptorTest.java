package hudson.plugins.warnings;

import static junit.framework.Assert.*;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

/**
 * Tests the class {@link WarningsDescriptor}.
 *
 * @author Ulli Hafner
 */
public class WarningsDescriptorTest {
    /**
     * Test the validation of the name parameter.
     */
    @Test
    public void testNameValidation() {
        WarningsDescriptor descriptor = new WarningsDescriptor();

        assertError(descriptor.doCheckName(null));
        assertError(descriptor.doCheckName(StringUtils.EMPTY));
        assertError(descriptor.doCheckName("Java Compiler"));
        assertOk(descriptor.doCheckName("Java Parser 2"));
    }

    /**
     * Test the validation of the regexp parameter.
     */
    @Test
    public void testRegexpValidation() {
        WarningsDescriptor descriptor = new WarningsDescriptor();

        assertError(descriptor.doCheckRegexp(null));
        assertError(descriptor.doCheckRegexp(StringUtils.EMPTY));
        assertError(descriptor.doCheckRegexp("one brace ("));
        assertError(descriptor.doCheckRegexp("backslash \\"));

        assertOk(descriptor.doCheckRegexp("^.*[a-z]"));
    }

    /**
     * Test the validation of the script parameter.
     *
     * @throws IOException
     *             if the example file could not be read
     */
    @Test
    public void testScriptValidationWithoutExample() throws IOException {
        WarningsDescriptor descriptor = new WarningsDescriptor();

        assertError(descriptor.doCheckScript(null, StringUtils.EMPTY, StringUtils.EMPTY));
        assertError(descriptor.doCheckScript(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
        assertError(descriptor.doCheckScript("Hello World", StringUtils.EMPTY, StringUtils.EMPTY));

        assertOk(descriptor.doCheckScript(readScript(), StringUtils.EMPTY, StringUtils.EMPTY));
    }

    private String readScript() throws IOException {
        return IOUtils.toString(WarningsDescriptorTest.class.getResourceAsStream("groovy.snippet"));
    }

    /**
     * Test the validation of the script parameter with a given regular
     * expression and example. Expected result: the expected result is a
     * warning.
     *
     * @throws IOException
     *             if the example file could not be read
     */
    @Test
    public void testScriptValidationOneWarning() throws IOException {
        WarningsDescriptor descriptor = new WarningsDescriptor();

        assertOk(descriptor.doCheckScript(readScript(),
                "file/name/relative/unix:42:evil: this is a warning message",
                "^\\s*(.*):(\\d+):(.*):\\s*(.*)$"));
    }

    /**
     * Test the validation of the script parameter with a given regular
     * expression and example. Expected result: the regular expression will not
     * match.
     *
     * @throws IOException
     *             if the example file could not be read
     */
    @Test
    public void testScriptValidationNoMatchesFound() throws IOException {
        WarningsDescriptor descriptor = new WarningsDescriptor();

        assertError(descriptor.doCheckScript(readScript(),
                "this is a warning message",
                "^\\s*(.*):(\\d+):(.*):\\s*(.*)$"));
    }

    /**
     * Test the validation of the script parameter with a given regular
     * expression and example. Expected result: the regular expression will not
     * match.
     *
     * @throws IOException
     *             if the example file could not be read
     */
    @Test
    public void testScriptValidationIllegalMatchAccess() throws IOException {
        WarningsDescriptor descriptor = new WarningsDescriptor();

        assertError(descriptor.doCheckScript(readScript(),
                "file/name/relative/unix:42:evil: this is a warning message",
                "^\\s*(.*):(\\d+):(.*)$"));
    }

    private void assertOk(final FormValidation actualResult) {
        verify(actualResult, FormValidation.Kind.OK);
    }

    private void assertError(final FormValidation actualResult) {
        verify(actualResult, FormValidation.Kind.ERROR);
    }

    private void verify(final FormValidation actualResult, final Kind expectedResult) {
        assertEquals("Wrong validation result", expectedResult, actualResult.kind);
    }
}

