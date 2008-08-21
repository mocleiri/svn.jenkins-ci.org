package hudson.plugins.findbugs.parser;

import hudson.FilePath;
import hudson.plugins.findbugs.parser.maven.MavenFindBugsParser;
import hudson.plugins.findbugs.util.AnnotationParser;
import hudson.plugins.findbugs.util.model.FileAnnotation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentException;
import org.xml.sax.SAXException;

/**
 * A parser for FindBugs XML files.
 *
 * @author Ulli Hafner
 */
public class FindBugsParser implements AnnotationParser {
    /** Unique ID of this class. */
    private static final long serialVersionUID = 8306319007761954027L;
    /** Workspace root. */
    private final FilePath workspace;

    /**
     * Creates a new instance of {@link FindBugsParser}.
     *
     * @param workspace
     *            the workspace folder to be used as basis for source code
     *            mapping
     */
    public FindBugsParser(final FilePath workspace) {
        this.workspace = workspace;
    }

    /** {@inheritDoc} */
    public String getName() {
        return "FINDBUGS";
    }

    /** {@inheritDoc} */
    public Collection<FileAnnotation> parse(final File file, final String moduleName) throws InvocationTargetException {
        try {
            MavenFindBugsParser mavenFindBugsParser = new MavenFindBugsParser();
            if (mavenFindBugsParser.accepts(new FileInputStream(file))) {
                return mavenFindBugsParser.parse(new FileInputStream(file), moduleName, workspace);
            }
            else {
                String moduleRoot = StringUtils.substringBefore(file.getAbsolutePath().replace('\\', '/'), "/target/");
                return new NativeFindBugsParser().parse(file, moduleRoot, moduleName);
            }
        }
        catch (IOException exception) {
            throw new InvocationTargetException(exception);
        }
        catch (SAXException exception) {
            throw new InvocationTargetException(exception);
        }
        catch (DocumentException exception) {
            throw new InvocationTargetException(exception);
        }
    }

    /** {@inheritDoc} */
    public Collection<FileAnnotation> parse(final InputStream file, final String moduleName) throws InvocationTargetException {
        throw new UnsupportedOperationException("FinBugs parser does not parse input streams.");
    }
}

