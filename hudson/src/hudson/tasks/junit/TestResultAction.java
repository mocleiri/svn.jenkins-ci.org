package hudson.tasks.junit;

import com.thoughtworks.xstream.XStream;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.StringConverter2;
import hudson.util.XStream2;
import org.apache.tools.ant.DirectoryScanner;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.AreaRendererEndType;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    // Hudson < 1.25 didn't set these fields, so use Integer
    // so that we can distinguish between 0 tests vs not-computed-yet.
    private int failCount;
    private Integer totalCount;

    TestResultAction(Build owner, DirectoryScanner results, BuildListener listener) {
        this.owner = owner;

        TestResult r = new TestResult(this,results,listener);

        totalCount = r.getTotalCount();
        failCount = r.getFailCount();

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
        if(totalCount==null) {
            totalCount = r.getTotalCount();
            failCount = r.getFailCount();
        }
        return r;
    }

    /**
     * Gets the number of failed tests.
     */
    public int getFailCount() {
        if(totalCount==null)
            getResult();    // this will compute the result
        return failCount;
    }

    /**
     * Gets the total number of tests.
     */
    public Integer getTotalCount() {
        if(totalCount==null)
            getResult();    // this will compute the result
        return totalCount;
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

    public TestResultAction getPreviousResult() {
        Build b = owner;
        while(true) {
            b = b.getPreviousBuild();
            if(b==null)
                return null;
            if(b.getResult()== Result.FAILURE)
                continue;
            TestResultAction r = b.getAction(TestResultAction.class);
            if(r!=null)
                return r;
        }
    }

    /**
     * Generates a PNG image for the test result trend.
     */
    public void doGraph( StaplerRequest req, StaplerResponse rsp) throws IOException {

        if(req.checkIfModified(owner.getTimestamp(),rsp))
            return;

        class BuildLabel implements Comparable<BuildLabel> {
            private final Build build;

            public BuildLabel(Build build) {
                this.build = build;
            }

            public int compareTo(BuildLabel that) {
                return this.build.number-that.build.number;
            }

            public boolean equals(Object o) {
                BuildLabel that = (BuildLabel) o;
                return build==that.build;
            }

            public int hashCode() {
                return build.hashCode();
            }

            public String toString() {
                return build.getDisplayName();
            }
        }

        boolean failureOnly = Boolean.parseBoolean(req.getParameter("failureOnly"));

        DataSetBuilder<String,BuildLabel> dsb = new DataSetBuilder<String,BuildLabel>();

        for( TestResultAction a=this; a!=null; a=a.getPreviousResult() ) {
            dsb.add( a.getFailCount(), "failed", new BuildLabel(a.owner));
            if(!failureOnly)
                dsb.add( a.getTotalCount()-a.getFailCount(),"total", new BuildLabel(a.owner));
        }

        String w = req.getParameter("width");
        if(w==null)     w="500";
        String h = req.getParameter("height");
        if(h==null)     h="200";
        BufferedImage image = createChart(dsb.build()).createBufferedImage(Integer.parseInt(w),Integer.parseInt(h));
        rsp.setContentType("image/png");
        ServletOutputStream os = rsp.getOutputStream();
        ImageIO.write(image, "PNG", os);
        os.close();
    }

    private JFreeChart createChart(CategoryDataset dataset) {

        final JFreeChart chart = ChartFactory.createStackedAreaChart(
            null,                   // chart title
            null,                   // unused
            "count",                  // range axis label
            dataset,                  // data
            PlotOrientation.VERTICAL, // orientation
            false,                     // include legend
            true,                     // tooltips
            false                     // urls
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

        // set the background color for the chart...

//        final StandardLegend legend = (StandardLegend) chart.getLegend();
//        legend.setAnchor(StandardLegend.SOUTH);

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setForegroundAlpha(0.8f);
//        plot.setDomainGridlinesVisible(true);
//        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        AreaRenderer ar = (AreaRenderer) plot.getRenderer();
        ar.setEndType(AreaRendererEndType.TRUNCATE);
        ar.setSeriesPaint(0,new Color(0xEF,0x29,0x29));
        ar.setSeriesPaint(1,new Color(0x72,0x9F,0xCF));

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(0,0,0,5.0));

        return chart;
    }


    private static final Logger logger = Logger.getLogger(TestResultAction.class.getName());

    private static final XStream XSTREAM = new XStream2();

    static {
        XSTREAM.alias("result",TestResult.class);
        XSTREAM.alias("suite",SuiteResult.class);
        XSTREAM.alias("case",CaseResult.class);
        XSTREAM.registerConverter(new StringConverter2(),100);
    }
}
