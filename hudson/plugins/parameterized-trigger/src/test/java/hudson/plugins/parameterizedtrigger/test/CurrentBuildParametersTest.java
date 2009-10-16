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
package hudson.plugins.parameterizedtrigger.test;

import hudson.model.ParametersAction;
import hudson.model.Project;
import hudson.model.StringParameterValue;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;

import org.junit.Assert;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

public class CurrentBuildParametersTest extends HudsonTestCase {

	public void test() throws Exception {

		Project projectA = createFreeStyleProject("projectA");
		String properties = "KEY=value";
		projectA.getPublishersList().add(
				new BuildTrigger(new BuildTriggerConfig("projectB", ResultCondition.SUCCESS,
						new CurrentBuildParameters())));

		CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
		Project projectB = createFreeStyleProject("projectB");
		projectB.getBuildersList().add(builder);

		projectA.scheduleBuild2(0, null, new ParametersAction(new StringParameterValue("KEY", "value"))).get();

		Thread.sleep(1000);

		Assert.assertEquals("value", builder.getEnvVars().get("KEY"));
	}

}
