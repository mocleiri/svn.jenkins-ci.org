package com.googlecode.refit.jenkins;

import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.StackedAreaRenderer2;

import java.awt.Color;
import java.awt.Paint;
import java.util.Calendar;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;

public class ReFitGraph extends Graph {

    private CategoryDataset data;

    public ReFitGraph(CategoryDataset data) {
        super(Calendar.getInstance(), 500, 200);
        this.data = data;
    }

    @Override
    protected JFreeChart createGraph() {

        final JFreeChart chart = ChartFactory.createStackedAreaChart(null, // chart
                // title
                null, // unused
                "count", // range axis label
                data, // data
                PlotOrientation.VERTICAL, // orientation
                false, // include legend
                true, // tooltips
                false // urls
                );

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setForegroundAlpha(0.8f);
        // plot.setDomainGridlinesVisible(true);
        // plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        ChartUtil.adjustChebyshev(data, rangeAxis);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setAutoRange(true);

        StackedAreaRenderer ar = new StackedAreaRenderer2() {
            private static final long serialVersionUID = 1L;

            @Override
            public Paint getItemPaint(int row, int column) {
                ChartLabel key = (ChartLabel) data.getColumnKey(column);
                if (key.getColor() != null)
                    return key.getColor();
                return super.getItemPaint(row, column);
            }

            @Override
            public String generateURL(CategoryDataset dataset, int row, int column) {
                ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                return label.getUrl();
            }

            @Override
            public String generateToolTip(CategoryDataset dataset, int row, int column) {
                ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                return label.o.getOwner().getDisplayName() + " : " + label.o.getDurationString();
            }
        };
        plot.setRenderer(ar);
        ar.setSeriesPaint(0, ColorPalette.RED); // Failures.
        ar.setSeriesPaint(1, ColorPalette.YELLOW); // Skips.
        ar.setSeriesPaint(2, ColorPalette.BLUE); // Total.

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

        return chart;
    }
}
