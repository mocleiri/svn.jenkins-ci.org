package hudson.plugins.tasks.parser;

import static org.junit.Assert.*;
import hudson.plugins.tasks.parser.MavenJavaClassifier;
import hudson.plugins.tasks.util.model.WorkspaceFile;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 *  Tests the class {@link MavenJavaClassifier}.
 */
public class MavenJavaClassifierTest {
    /**
     * Checks whether we could identify a java package name and maven module.
     *
     * @throws IOException
     *             in case of an error
     */
    @Test
    public void checkPackage() throws IOException {
        WorkspaceFile workspaceFile = new WorkspaceFile();
        workspaceFile.setName("MavenJavaTest.txt");

        checkClassification(workspaceFile, "com.avaloq.adt.core/src/com/avaloq/adt/core/job/AvaloqJob.java");
        checkClassification(workspaceFile, "base/com.hello.world/com.avaloq.adt.core/src/com/avaloq/adt/core/job/AvaloqJob.java");
    }

    /**
     * Checks the classification for the specified file name.
     *
     * @param file
     *            the workspace file
     * @param fileName
     *            the file name
     * @throws IOException in case of an error
     */
    private void checkClassification(final WorkspaceFile file, final String fileName) throws IOException {
        InputStream stream;
        stream = MavenJavaClassifierTest.class.getResourceAsStream("MavenJavaTest.txt");
        file.setName(fileName);
        MavenJavaClassifier classifier = new MavenJavaClassifier();
        classifier.classify(file, stream);

        assertEquals("Wrong package name guessed.", "hudson.plugins.tasks.util", file.getPackageName());
        assertEquals("Wrong module name guessed", "com.avaloq.adt.core", file.getModuleName());
    }
}
