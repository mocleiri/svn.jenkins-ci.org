/* 
 * The MIT License
 * 
 * Copyright (c) 2010 Bruno P. Kinoshita <http://www.kinoshita.eti.br>
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
package hudson.plugins.testlink;

import hudson.plugins.testlink.result.TestLinkReport;

import java.lang.reflect.Constructor;
import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import br.eti.kinoshita.testlinkjavaapi.model.Build;
import br.eti.kinoshita.testlinkjavaapi.model.ExecutionStatus;
import br.eti.kinoshita.testlinkjavaapi.model.ExecutionType;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan;
import br.eti.kinoshita.testlinkjavaapi.model.TestProject;

/**
 * Tests the ReportSummary class.
 *
 * @see {@link ReportSummary}
 *
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 2.1
 */
public class TestReportSummary
{

	/**
	 * Prepares for the tests.
	 */
	@BeforeClass
	public void setUp()
	{
		Locale.setDefault(new Locale("en", "US"));
	}
	
	/**
	 * Tests the hidden constructor of the ReportSummary class.
	 */
	@Test(description="Tests the hidden constructor of the ReportSummary class.")
	public void testConstructor()
	{
		try
		{
			final Constructor<?> c = ReportSummary.class.getDeclaredConstructors()[0];
			c.setAccessible(true);
			final Object o = c.newInstance((Object[]) null);

			Assert.assertNotNull(o);
		}
		catch (Exception e)
		{
			Assert.fail("Failed to instantiate constructor: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Tests printDifference() method.
	 */
	@Test(description="Tests the printDifference() method.")
	public void testPrintDifference()
	{
		StringBuilder builder = new StringBuilder();
		ReportSummary.printDifference(1, 0, builder);
		Assert.assertEquals( builder.toString(), " (+1)" );
		
		builder = new StringBuilder();
		ReportSummary.printDifference(0, 1, builder);
		Assert.assertEquals( builder.toString(), "" );
	}
	
	/**
	 * Tests the createReportSummary() method with a previous TestLinkReport.
	 */
	@Test(description="Tests the createReportSummary() method with a previous TestLinkReport.")
	public void testSummary()
	{
		Build build = new Build(1, 1, "My build", "Notes about my build");
		TestPlan plan = new TestPlan(1, "My test plan", "My project", "Notes about my test plan", true, true);
		TestProject project = new TestProject(1, "My project", "MP", "Notes about my project", true, true, true, true, true, true);
		
		TestLinkReport report = new TestLinkReport();
		report.setBuild(build);
		report.setTestPlan(plan);
		report.setTestProject(project);
		
		TestCase testCase1 = new TestCase(1, "tc1", 1, 1, "kinow", "No summary", null, "", null, ExecutionType.AUTOMATED, 1, 1, false, null, 1, 1, null, null, ExecutionStatus.PASSED );
		TestCase testCase2 = new TestCase(1, "tc1", 2, 2, "kinow", "No summary", null, "", null, ExecutionType.AUTOMATED, 2, 2, false, null, 2, 2, null, null, ExecutionStatus.FAILED );
		TestCase testCase3 = new TestCase(1, "tc1", 3, 3, "kinow", "No summary", null, "", null, ExecutionType.AUTOMATED, 3, 3, false, null, 3, 3, null, null, ExecutionStatus.BLOCKED );
		
		report.addTestCase( testCase1 );
		report.addTestCase( testCase2 );
		report.addTestCase( testCase3 );
		
		String reportSummary = ReportSummary.createReportSummary(report, null);
		Assert.assertNotNull(reportSummary);
		Assert.assertEquals(reportSummary, "<p><b>TestLink Build ID: 1</b></p><p><b>TestLink Build Name: My build</b></p><p><a href=\"testLinkResult\">Total of 3 tests.</a> Where 1 tests passed, 1 tests failed and 1 tests were blocked.</p>");
	}
	
	/**
	 * Tests the createReportSummary() method with a previous TestLinkReport.
	 */
	@Test(description="Tests the createReportSummary() method with a previous TestLinkReport.")
	public void testSummaryWithPrevious()
	{
		Build build = new Build(1, 1, "My build", "Notes about my build");
		TestPlan plan = new TestPlan(1, "My test plan", "My project", "Notes about my test plan", true, true);
		TestProject project = new TestProject(1, "My project", "MP", "Notes about my project", true, true, true, true, true, true);
		
		TestLinkReport report = new TestLinkReport();
		report.setBuild(build);
		report.setTestPlan(plan);
		report.setTestProject(project);
		
		TestCase testCase1 = new TestCase(1, "tc1", 1, 1, "kinow", "No summary", null, "", null, ExecutionType.AUTOMATED, 1, 1, false, null, 1, 1, null, null, ExecutionStatus.PASSED );
		TestCase testCase2 = new TestCase(1, "tc1", 2, 2, "kinow", "No summary", null, "", null, ExecutionType.AUTOMATED, 2, 2, false, null, 2, 2, null, null, ExecutionStatus.FAILED );
		TestCase testCase3 = new TestCase(1, "tc1", 3, 3, "kinow", "No summary", null, "", null, ExecutionType.AUTOMATED, 3, 3, false, null, 3, 3, null, null, ExecutionStatus.BLOCKED );
		
		report.addTestCase( testCase1 );
		report.addTestCase( testCase2 );
		report.addTestCase( testCase3 );
		
		TestLinkReport previous = new TestLinkReport();
		previous.setBuild(build);
		previous.setTestPlan(plan);
		previous.setTestProject(project);
		
		previous.addTestCase( testCase1 );
		previous.addTestCase( testCase2 );
		
		String reportSummary = ReportSummary.createReportSummary(report, previous);
		Assert.assertNotNull(reportSummary);
		Assert.assertEquals(reportSummary, "<p><b>TestLink Build ID: 1</b></p><p><b>TestLink Build Name: My build</b></p><p><a href=\"testLinkResult\">Total of 3 (+1) tests.</a> Where 1 tests passed, 1 tests failed and 1 (+1) tests were blocked.</p>");
	}
	
	/**
	 * Tests the createReportSummaryDetails() method.
	 */
	@Test(description="Tests the createReportSummaryDetails() method.")
	public void testSummaryDetails()
	{
		Build build = new Build(1, 1, "My build", "Notes about my build");
		TestPlan plan = new TestPlan(1, "My test plan", "My project", "Notes about my test plan", true, true);
		TestProject project = new TestProject(1, "My project", "MP", "Notes about my project", true, true, true, true, true, true);
		
		TestLinkReport report = new TestLinkReport();
		report.setBuild(build);
		report.setTestPlan(plan);
		report.setTestProject(project);
		
		TestCase testCase1 = new TestCase(1, "tc1", 1, 1, "kinow", "No summary", null, "", null, ExecutionType.AUTOMATED, 1, 1, false, null, 1, 1, null, null, ExecutionStatus.PASSED );
		TestCase testCase2 = new TestCase(1, "tc1", 2, 2, "kinow", "No summary", null, "", null, ExecutionType.AUTOMATED, 2, 2, false, null, 2, 2, null, null, ExecutionStatus.FAILED );
		TestCase testCase3 = new TestCase(1, "tc1", 3, 3, "kinow", "No summary", null, "", null, ExecutionType.AUTOMATED, 3, 3, false, null, 3, 3, null, null, ExecutionStatus.BLOCKED );
		
		report.addTestCase( testCase1 );
		report.addTestCase( testCase2 );
		report.addTestCase( testCase3 );
		
		TestLinkReport previous = new TestLinkReport();
		previous.setBuild(build);
		previous.setTestPlan(plan);
		previous.setTestProject(project);
		
		previous.addTestCase( testCase1 );
		previous.addTestCase( testCase2 );
		
		String reportSummaryDetails = ReportSummary.createReportSummaryDetails(report, previous);
		Assert.assertNotNull(reportSummaryDetails);
		
		String expectedDetails = ""+
		"<p>List of test cases and execution result status</p><table border=\"1\">\n" +
"<tr><th>Test case ID</th><th>Version</th><th>Name</th><th>Test Project ID</th><th>Execution Status</th></tr>\n" +
"<tr>\n" +
"<td>1</td><td>1</td><td>tc1</td><td>1</td><td><span style='color: green'>Passed</span></td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1</td><td>2</td><td>tc1</td><td>2</td><td><span style='color: red'>Failed</span></td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1</td><td>3</td><td>tc1</td><td>3</td><td><span style='color: yellow'>Blocked</span></td>\n" +
"</tr>\n" +
"</table>";
		
		Assert.assertEquals(reportSummaryDetails, expectedDetails);
	}
	
}
