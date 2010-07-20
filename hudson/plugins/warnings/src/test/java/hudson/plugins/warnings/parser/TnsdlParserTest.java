package hudson.plugins.warnings.parser;

import static junit.framework.Assert.*;
import hudson.plugins.analysis.util.model.FileAnnotation;
import hudson.plugins.analysis.util.model.Priority;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

/**
 * FIXME: Document type TnsdlParserTest.
 *
 * @author Ulli Hafner
 */
public class TnsdlParserTest extends ParserTester {

    /** Error message. */
    private static final String WRONG_NUMBER_OF_WARNINGS_DETECTED = "Wrong number of warnings detected.";
    /** An error. */
    private static final String WARNING_CATEGORY = TnsdlParser.WARNING_CATEGORY;

    /**
     * Parses a file with four tnsdl warnings.
     *
     * @throws IOException
     *      if the file could not be read
     */
    @Test
    public void testWarningsParser() throws IOException {
        Collection<FileAnnotation> warnings = new TnsdlParser().parse(openFile());

        assertEquals(WRONG_NUMBER_OF_WARNINGS_DETECTED, 4, warnings.size());

        Iterator<FileAnnotation> iterator = warnings.iterator();
        FileAnnotation annotation = iterator.next();
        checkWarning(annotation,
                398,
                "unused variable sender_pid",
                "tstmasgx.sdl",
                TnsdlParser.WARNING_TYPE, WARNING_CATEGORY, Priority.NORMAL);
        annotation = iterator.next();

        checkWarning(annotation,
                399,
                "unused variable a_sender_pid",
                "tstmasgx.sdl",
                TnsdlParser.WARNING_TYPE, WARNING_CATEGORY, Priority.HIGH);
        annotation = iterator.next();

        checkWarning(annotation,
                3,
                "Id. length is reserved in PL/M 386 intrinsics",
                "s_dat:dty0132c.sdt",
                TnsdlParser.WARNING_TYPE, WARNING_CATEGORY, Priority.NORMAL);
        annotation = iterator.next();

        checkWarning(annotation,
                4,
                "Id. length is reserved in PL/M 386 intrinsics",
                "s_dat:dty0132c.sdt",
                TnsdlParser.WARNING_TYPE, WARNING_CATEGORY, Priority.HIGH);

    }

    /** {@inheritDoc} */
    @Override
    protected String getWarningsFile() {
        return "tnsdl.txt";
    }

}

