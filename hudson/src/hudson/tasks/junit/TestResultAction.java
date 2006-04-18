package hudson.tasks.junit;

import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.StaplerProxy;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.thoughtworks.xstream.XStream;

/**
 * {@link Action} that displays the JUnit test result.
 *
 * <p>
 * The actual test reports are isolated by {@link WeakReference}
 * so that it doesn't eat up too much memory.
 *
 * @author Kohsuke Kawaguchi
 */
public class TestResultAction implements Action, StaplerProxy {
    public final Build owner;

    private transient WeakReference<TestResult> result;

    TestResultAction(Build owner, DirectoryScanner results, BuildListener listener) {
        this.owner = owner;

        TestResult r = new TestResult(this,results,listener);

        // persist the data
        try {
            getDataFile().write(r);
        } catch (IOException e) {
            e.printStackTrace(listener.fatalError("Failed to save the JUnit test result"));
        }

        this.result = new WeakReference<TestResult>(r);
    }

    private XmlFile getDataFile() {
        return new XmlFile(XSTREAM,new File(owner.getRootDir(), "junitResult.xml"));
    }

    public synchronized TestResult getResult() {
        if(result==null) {
            TestResult r = load();
            result = new WeakReference<TestResult>(r);
            return r;
        }
        TestResult r = result.get();
        if(r==null) {
            r = load();
            result = new WeakReference<TestResult>(r);
        }
        return r;
    }

    /**
     * Loads a {@link TestResult} from disk.
     */
    private TestResult load() {
        TestResult r;
        try {
            r = (TestResult)getDataFile().read();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load "+getDataFile(),e);
            r = new TestResult();   // return a dummy
        }
        r.parent = this;
        r.freeze();
        return r;
    }

    public Object getTarget() {
        return getResult();
    }

    public String getDisplayName() {
        return "Test Result";
    }

    public String getUrlName() {
        return "testReport";
    }

    public String getIconFileName() {
        return "clipboard.gif";
    }

    private static final Logger logger = Logger.getLogger(TestResultAction.class.getName());

    private static final XStream XSTREAM = new XStream();

    static {
        XSTREAM.alias("result",TestResult.class);
        XSTREAM.alias("suite",SuiteResult.class);
        XSTREAM.alias("case",CaseResult.class);
    }
}
