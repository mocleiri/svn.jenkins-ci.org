package hudson.plugins.warnings.parser;

import static junit.framework.Assert.*;
import hudson.plugins.warnings.util.model.FileAnnotation;
import hudson.plugins.warnings.util.model.Priority;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

/**
 * Tests the class {@link JavacParser}.
 */
public class InvalidsParserTest extends ParserTester {
    /**
     * Parses a file with two deprecation warnings.
     *
     * @throws IOException
     *      if the file could not be read
     */
    @Test
    public void testParser() throws IOException {
        Collection<FileAnnotation> warnings = new InvalidsParser().parse(InvalidsParserTest.class.getResourceAsStream("invalids.txt"));

        assertEquals("Wrong number of warnings detected.", 2, warnings.size());

        Iterator<FileAnnotation> iterator = warnings.iterator();
        FileAnnotation annotation = iterator.next();
        String type = "Oracle Invalid";
        checkWarning(annotation,
                45,
                "Encountered the symbol \"END\" when expecting one of the following:",
                "ENV_UTIL#.PACKAGE BODY", type, "PLS-00103", Priority.NORMAL);
        assertEquals("wrong schema detected", "E", annotation.getPackageName());
        annotation = iterator.next();
        checkWarning(annotation,
                5,
                "Encountered the symbol \"END\" when expecting one of the following:",
                "ENV_ABBR#B.TRIGGER", type, "PLS-00103", Priority.NORMAL);
        assertEquals("wrong schema detected", "E", annotation.getPackageName());
    }
}

