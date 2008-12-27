package hudson.model;

import hudson.model.MultiStageTimeSeries.Picker;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import hudson.util.ColorPalette;
import hudson.util.NoOverlapCategoryAxis;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;

import java.awt.*;
import java.util.Date;
import java.util.List;

/**
 * Utilization statistics for a node or a set of nodes.
 *
 * <h2>Implementation Note</h2>
 * <p>
 * Instances of this class is not capable of updating the statistics itself
 * &mdash; instead, it's done by the single {@link #register()} method.
 * This is more efficient (as it allows us a single pass to update all stats),
 * but it's not clear to me if the loss of autonomy is worth it.
 *
 * @author Kohsuke Kawaguchi
 * @see Label#load
 * @see Hudson#overallLoad
 */
public abstract class LoadStatistics {
    /**
     * Number of busy executors and how it changes over time.
     */
    public final MultiStageTimeSeries busyExecutors;

    /**
     * Number of total executors and how it changes over time.
     */
    public final MultiStageTimeSeries totalExecutors;

    /**
     * Number of {@link Queue.BuildableItem}s that can run on any node in this node set but blocked.
     */
    public final MultiStageTimeSeries queueLength;

    protected LoadStatistics(int initialTotalExecutors, int initialBusyExecutors) {
        this.totalExecutors = new MultiStageTimeSeries(initialTotalExecutors,DECAY);
        this.busyExecutors = new MultiStageTimeSeries(initialBusyExecutors,DECAY);
        this.queueLength = new MultiStageTimeSeries(0,DECAY);
    }

    public float getLatestIdleExecutors(Picker picker) {
        return totalExecutors.pick(picker).getLatest() - busyExecutors.pick(picker).getLatest();
    }

    /**
     * Computes the # of idle executors right now and obtains the snapshot value.
     */
    public abstract int computeIdleExecutors();

    /**
     * Computes the # of total executors right now and obtains the snapshot value.
     */
    public abstract int computeTotalExecutors();

    /**
     * Computes the # of queue length right now and obtains the snapshot value.
     */
    public abstract int computeQueueLength();

    /**
     * Creates a trend chart.
     */
    public JFreeChart createChart(Picker timeScale) {
        Date dt = new Date();

        float[] b = busyExecutors.pick(timeScale).getHistory();
        float[] t = totalExecutors.pick(timeScale).getHistory();
        float[] q = queueLength.pick(timeScale).getHistory();
        assert b.length==t.length && t.length==q.length;

        DefaultCategoryDataset ds = new DefaultCategoryDataset();

        for (int i = q.length-1; i>=0; i--) {
            ds.addValue(t[i],"Total executors",dt);
            ds.addValue(b[i],"Busy executors",dt);
            ds.addValue(q[i],"Queue length",dt);
            dt = new Date(dt.getTime()-timeScale.tick);
        }

        final JFreeChart chart = ChartFactory.createLineChart(null, // chart title
                null, // unused
                null, // range axis label
                ds, // data
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips
                false // urls
                );

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseStroke(new BasicStroke(3));
        renderer.setSeriesPaint(0, ColorPalette.BLUE);  // total
        renderer.setSeriesPaint(1, ColorPalette.RED);   // busy
        renderer.setSeriesPaint(2, ColorPalette.GREY);  // queue

        final CategoryAxis domainAxis = new NoOverlapCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

        return chart;
    }

    /**
     * Start updating the load average.
     */
    /*package*/ static void register() {
        Trigger.timer.scheduleAtFixedRate(
            new SafeTimerTask() {
                protected void doRun() {
                    Hudson h = Hudson.getInstance();
                    List<Queue.BuildableItem> bis = h.getQueue().getBuildableItems();

                    // update statistics on slaves
                    for( Label l : h.getLabels() ) {
                        l.load.totalExecutors.update(l.getTotalExecutors());
                        l.load.busyExecutors .update(l.getBusyExecutors());

                        int q=0;
                        for (Queue.BuildableItem bi : bis) {
                            if(bi.task.getAssignedLabel()==l)
                                q++;
                        }
                        l.load.queueLength.update(q);
                    }

                    // update statistics of the entire system
                    ComputerSet cs = h.getComputer();
                    h.overallLoad.totalExecutors.update(cs.getTotalExecutors());
                    h.overallLoad.busyExecutors .update(cs.getBusyExecutors());
                    int q=0;
                    for (Queue.BuildableItem bi : bis) {
                        if(bi.task.getAssignedLabel()==null)
                            q++;
                    }
                    h.overallLoad.queueLength.update(q);
                    h.overallLoad.totalQueueLength.update(bis.size());
                }
            }, CLOCK, CLOCK
        );
    }


    /**
     * With 0.90 decay ratio for every 10sec, half reduction is about 1 min.
     */
    public static final float DECAY = Float.parseFloat(System.getProperty(LoadStatistics.class.getName()+".decay","0.9"));
    /**
     * Load statistics clock cycle in milliseconds. Specify a small value for quickly debugging this feature and node provisioning through cloud.
     */
    public static int CLOCK = Integer.getInteger(LoadStatistics.class.getName()+".clock",10*1000);
}
