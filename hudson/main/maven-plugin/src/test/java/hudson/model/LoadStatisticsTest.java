/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.model;

import hudson.model.MultiStageTimeSeries.TimeScale;
import junit.framework.TestCase;
import org.jfree.chart.JFreeChart;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class LoadStatisticsTest extends TestCase {
    public void testGraph() throws IOException {
        LoadStatistics ls = new LoadStatistics(0, 0) {
            public int computeIdleExecutors() {
                throw new UnsupportedOperationException();
            }

            public int computeTotalExecutors() {
                throw new UnsupportedOperationException();
            }

            public int computeQueueLength() {
                throw new UnsupportedOperationException();
            }
        };

        for(int i=0;i<50;i++) {
            ls.totalExecutors.update(4);
            ls.busyExecutors.update(3);
            ls.queueLength.update(3);
        }

        for(int i=0;i<50;i++) {
            ls.totalExecutors.update(0);
            ls.busyExecutors.update(0);
            ls.queueLength.update(1);
        }

        JFreeChart chart = ls.createChart(ls.createDataset(TimeScale.SEC10));
        BufferedImage image = chart.createBufferedImage(400,200);
        ImageIO.write(image, "PNG", new FileOutputStream("chart.png"));
    }
}
