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
package hudson.plugins.testlink.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import br.eti.kinoshita.testlinkjavaapi.model.Attachment;
import br.eti.kinoshita.testlinkjavaapi.model.Build;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan;

/**
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 2.0
 */
public class TestResult 
implements Serializable
{

	private TestCase testCase;
	private Build build;
	private TestPlan testPlan;
	private List<Attachment> attachments;
	private String notes;
	
	public TestResult(
		TestCase testCase, 
		Build build, 
		TestPlan testPlan
	)
	{
		this.testCase = testCase;
		this.build = build;
		this.testPlan = testPlan;
		
		this.attachments = new ArrayList<Attachment>();
	}
	
	public void addAttachment(Attachment attachment)
	{
		this.attachments.add( attachment );
	}

	public TestCase getTestCase()
	{
		return testCase;
	}

	public void setTestCase( TestCase testCase )
	{
		this.testCase = testCase;
	}

	public Build getBuild()
	{
		return build;
	}

	public void setBuild( Build build )
	{
		this.build = build;
	}

	public TestPlan getTestPlan()
	{
		return testPlan;
	}

	public void setTestPlan( TestPlan testPlan )
	{
		this.testPlan = testPlan;
	}

	public List<Attachment> getAttachments()
	{
		return attachments;
	}

	public String getNotes()
	{
		return notes;
	}

	public void setNotes( String notes )
	{
		this.notes = notes;
	}

	@Override
	public String toString()
	{
		return "TestResult [testCase=" + testCase + ", build=" + build
				+ ", testPlan=" + testPlan + ", attachments=" + attachments
				+ ", notes=" + notes + "]";
	}
	
}
