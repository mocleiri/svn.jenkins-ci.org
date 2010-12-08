/**
 *	 __                                        
 *	/\ \      __                               
 *	\ \ \/'\ /\_\    ___     ___   __  __  __  
 *	 \ \ , < \/\ \ /' _ `\  / __`\/\ \/\ \/\ \ 
 *	  \ \ \\`\\ \ \/\ \/\ \/\ \L\ \ \ \_/ \_/ \
 *	   \ \_\ \_\ \_\ \_\ \_\ \____/\ \___x___/'
 *	    \/_/\/_/\/_/\/_/\/_/\/___/  \/__//__/  
 *                                          
 * Copyright (c) 1999-present Kinow
 * Casa Verde - S�o Paulo - SP. Brazil.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Kinow ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Kinow.                                      
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 16/08/2010
 */
package hudson.plugins.testlink;


import hudson.plugins.testlink.model.TestLinkReport;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

/**
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 16/08/2010
 */
public class ChartUtil
{
	
	/**
	 * Creates the TestLink Trend Chart.
	 * 
	 * @param dataset of XY series
	 * @return a chart
	 */
	public static JFreeChart buildXYChart(XYDataset dataset) {

        final JFreeChart chart = ChartFactory.createXYLineChart(
                null,                   // chart title
                "Build #",                   // unused
                null,                    // range axis label
                dataset,                  // data
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips
                false                     // urls
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

        final LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.RIGHT);

        chart.setBackgroundPaint(Color.white);

        final XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(Color.lightGray);
        //    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(1, true);
        
        renderer.setSeriesPaint(0, Color.green);
        renderer.setSeriesPaint(1, Color.red);
        renderer.setSeriesPaint(2, Color.orange);
        renderer.setSeriesPaint(3, Color.blue);
        
        plot.setRenderer(renderer);

     // change the auto tick unit selection to integer units only...
        /*final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());*/
        // OPTIONAL CUSTOMISATION COMPLETED.

        return chart;
    }
	
	/**
	 * Creates the XY dataset required to create the CCM Trend graph.
	 * 
	 * @param lastAction
	 * @return
	 */
	public static final XYDataset createXYDataset( TestLinkBuildAction lastAction )
	{
		
		TestLinkBuildAction tempAction = lastAction;
		final XYSeriesCollection dataset = new XYSeriesCollection();
		final XYSeries passedTests = new XYSeries( "Passed Tests" );
		final XYSeries failedTests = new XYSeries( "Failed Tests" );
		final XYSeries blockedTests = new XYSeries( "Blocked Tests" );
		final XYSeries totalTests = new XYSeries( "Total Tests" );
		
		dataset.addSeries(passedTests);
		dataset.addSeries(failedTests);
		dataset.addSeries(blockedTests);
		dataset.addSeries(totalTests);
		do 
		{
			TestLinkResult result = tempAction.getResult();
			TestLinkReport report = result.getReport();
			int buildNumber = tempAction.getBuild().number;			
			totalTests.add(buildNumber, report.getTestsTotal());
			passedTests.add(buildNumber, report.getTestsPassed());
			failedTests.add(buildNumber, report.getTestsFailed());
			blockedTests.add(buildNumber, report.getTestsBlocked());
			tempAction = tempAction.getPreviousAction();
		} while ( tempAction != null );
		
		return dataset;
		
	}
	
}
