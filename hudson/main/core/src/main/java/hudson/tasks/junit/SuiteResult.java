package hudson.tasks.junit;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of one test suite.
 *
 * <p>
 * The notion of "test suite" is rather arbitrary in JUnit ant task.
 * It's basically one invocation of junit.
 *
 * <p>
 * This object is really only used as a part of the persisted
 * object tree.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SuiteResult implements Serializable {
    private final String name;
    private final String stdout;
    private final String stderr;
    private float duration;
    /**
     * The 'timestamp' attribute of  the test suite.
     * AFAICT, this is not a required attribute in XML, so the value may be null.
     */
    private String timestamp;

    /**
     * All test cases.
     */
    private final List<CaseResult> cases = new ArrayList<CaseResult>();
    private transient TestResult parent;

    SuiteResult(String name, String stdout, String stderr) {
        this.name = name;
        this.stderr = stderr;
        this.stdout = stdout;
    }

    /**
     * Parses the JUnit XML file into {@link SuiteResult}s.
     * This method returns a collection, as a single XML may have multiple &lt;testsuite>
     * elements wrapped into the top-level &lt;testsuites>.
     */
    static List<SuiteResult> parse(File xmlReport) throws DocumentException {
        List<SuiteResult> r = new ArrayList<SuiteResult>();

        // parse into DOM
        SAXReader saxReader = new SAXReader();
        // install EntityResolver for resolving DTDs, which are in files created by TestNG.
        // (see https://hudson.dev.java.net/servlets/ReadMsg?listName=users&msgNo=5530)
        XMLEntityResolver resolver = new XMLEntityResolver();
        saxReader.setEntityResolver(resolver);
        Document result = saxReader.read(xmlReport);
        Element root = result.getRootElement();

        if(root.getName().equals("testsuites")) {
            // multi-suite file
            for (Element suite : (List<Element>)root.elements("testsuite"))
                r.add(new SuiteResult(xmlReport,suite));
        } else {
            // single suite file
            r.add(new SuiteResult(xmlReport,root));
        }

        return r;
    }

    private SuiteResult(File xmlReport, Element suite) throws DocumentException {
        String name = suite.attributeValue("name");
        if(name==null)
            // some user reported that name is null in their environment.
            // see http://www.nabble.com/Unexpected-Null-Pointer-Exception-in-Hudson-1.131-tf4314802.html
            name = '('+xmlReport.getName()+')';
        else {
            String pkg = suite.attributeValue("package");
            if(pkg!=null)   name=pkg+'.'+name;
        }
        this.name = TestObject.safe(name);
        this.timestamp = suite.attributeValue("timestamp");

        stdout = suite.elementText("system-out");
        stderr = suite.elementText("system-err");

        Element ex = suite.element("error");
        if(ex!=null) {
            // according to junit-noframes.xsl l.229, this happens when the test class failed to load
            addCase(new CaseResult(this,suite,"<init>"));
        }

        for (Element e : (List<Element>)suite.elements("testcase")) {
            // https://hudson.dev.java.net/issues/show_bug.cgi?id=1233 indicates that
            // when <testsuites> is present, we are better off using @classname on the
            // individual testcase class.

            // https://hudson.dev.java.net/issues/show_bug.cgi?id=1463 indicates that
            // @classname may not exist in individual testcase elements. We now
            // also test if the testsuite element has a package name that can be used
            // as the class name instead of the file name which is default.
            String classname = e.attributeValue("classname");
            if (classname == null) {
                classname = suite.attributeValue("name");
            }

            // https://hudson.dev.java.net/issues/show_bug.cgi?id=1233 and
            // http://www.nabble.com/difference-in-junit-publisher-and-ant-junitreport-tf4308604.html#a12265700
            // are at odds with each other --- when both are present,
            // one wants to use @name from <testsuite>,
            // the other wants to use @classname from <testcase>.

            addCase(new CaseResult(this,classname,e));
        }
    }

    /*package*/ void addCase(CaseResult cr) {
        cases.add(cr);
        duration += cr.getDuration(); 
    }

    public String getName() {
        return name;
    }

    public float getDuration() {
        return duration; 
    }
    
    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public TestResult getParent() {
        return parent;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<CaseResult> getCases() {
        return cases;
    }

    public SuiteResult getPreviousResult() {
        TestResult pr = parent.getPreviousResult();
        if(pr==null)    return null;
        return pr.getSuite(name);
    }

    /**
     * Returns the {@link CaseResult} whose {@link CaseResult#getName()}
     * is the same as the given string.
     *
     * <p>
     * Note that test name needs not be unique.
     */
    public CaseResult getCase(String name) {
        for (CaseResult c : cases) {
            if(c.getName().equals(name))
                return c;
        }
        return null;
    }

    /*package*/ boolean freeze(TestResult owner) {
        if(this.parent!=null)
            return false;   // already frozen

        this.parent = owner;
        for (CaseResult c : cases)
            c.freeze(this);
        return true;
    }
}
