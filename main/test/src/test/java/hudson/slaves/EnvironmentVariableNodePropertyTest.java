package hudson.slaves;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Proc;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Slave;
import hudson.model.StringParameterDefinition;
import hudson.model.Node.Mode;
import hudson.remoting.Which;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class EnvironmentVariableNodePropertyTest extends HudsonTestCase {

	private Proc slaveProc;
	private Computer slave;
	private FreeStyleProject project;

	public void testSlavePropertyOnSlave() throws Exception {
		setVariables(slave.getNode(), new EnvVars.Entry("KEY", "slaveValue"));
		Map<String, String> envVars = executeBuild(slave);
		Assert.assertEquals("slaveValue", envVars.get("KEY"));
	}

	public void testMasterPropertyOnMaster() throws Exception {
		setVariables(Hudson.getInstance(), new EnvVars.Entry("KEY", "masterValue"));

		Map<String, String> envVars = executeBuild(slave);

		Assert.assertEquals("masterValue", envVars.get("KEY"));
	}
	
	public void testSlaveAndMasterPropertyOnSlave() throws Exception {
		setVariables(Hudson.getInstance(), new EnvVars.Entry("KEY", "masterValue"));
		setVariables(slave.getNode(), new EnvVars.Entry("KEY", "slaveValue"));

		Map<String, String> envVars = executeBuild(slave);

		Assert.assertEquals("slaveValue", envVars.get("KEY"));
	}

	public void testSlaveAndMasterAndParameterOnSlave() throws Exception {
		ParametersDefinitionProperty pdp = new ParametersDefinitionProperty(new StringParameterDefinition("KEY", "parameterValue"));
		project.addProperty(pdp);
		
		setVariables(Hudson.getInstance(), new EnvVars.Entry("KEY", "masterValue"));
		setVariables(slave.getNode(), new EnvVars.Entry("KEY", "slaveValue"));

		Map<String, String> envVars = executeBuild(slave);

		Assert.assertEquals("parameterValue", envVars.get("KEY"));
	}

	// //////////////////////// setup //////////////////////////////////////////

	public void setUp() throws Exception {
		super.setUp();
		slave = addTestSlave();
		slaveProc = launchJnlp(slave, buildJnlpArgs(slave).add("-arg",
				"-headless"));

		project = createFreeStyleProject();
	}

	public void tearDown() throws Exception {
		slaveProc.kill();
		super.tearDown();
	}

	// ////////////////////// helper methods /////////////////////////////////

	private void setVariables(Node node, EnvVars.Entry... entries)
			throws IOException {
		node.getNodeProperties().replaceBy(
				Collections.singleton(new EnvironmentVariablesNodeProperty(
						entries)));

	}

	public Map<String, String> executeBuild(Computer computer) throws Exception {
		CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();

		project.getBuildersList().add(builder);
		project.setAssignedLabel(computer.getNode().getSelfLabel());

		project.scheduleBuild2(0).get();
		
		return builder.getEnvVars();
	}

	/**
	 * Adds a JNLP {@link Slave} to the system and returns it.
	 */
	private Computer addTestSlave(NodeProperty<?>... np) throws Exception {
		List<Slave> slaves = new ArrayList<Slave>(hudson.getSlaves());
		File dir = Util.createTempDir();
		slaves.add(new DumbSlave("test", "dummy", dir.getAbsolutePath(), "1",
				Mode.NORMAL, "", new JNLPLauncher(),
				RetentionStrategy.INSTANCE, Arrays.asList(np)));
		hudson.setSlaves(slaves);
		Computer c = hudson.getComputer("test");
		assertNotNull(c);
		return c;
	}

	private ArgumentListBuilder buildJnlpArgs(Computer c) throws Exception {
		ArgumentListBuilder args = new ArgumentListBuilder();
		args.add(
				new File(new File(System.getProperty("java.home")), "bin/java")
						.getPath(), "-jar");
		args.add(Which.jarFile(netx.jnlp.runtime.JNLPRuntime.class)
				.getAbsolutePath());
		args.add("-headless", "-basedir");
		args.add(createTmpDir());
		args.add("-nosecurity", "-jnlp", getJnlpLink(c));
		return args;
	}

	/**
	 * Launches the JNLP slave agent and asserts its basic operations.
	 */
	private Proc launchJnlp(Computer c, ArgumentListBuilder args)
			throws Exception {
		Proc proc = createLocalLauncher().launch(args.toCommandArray(),
				new String[0], System.out, new FilePath(new File(".")));

		// verify that the connection is established, up to 10 secs
		for (int i = 0; i < 100; i++) {
			Thread.sleep(100);
			if (!c.isOffline())
				break;
		}

		assertFalse("Slave failed to go online", c.isOffline());

		return proc;
	}

	/**
	 * Determines the link to the .jnlp file.
	 */
	private String getJnlpLink(Computer c) throws Exception {
		HtmlPage p = new WebClient().goTo("computer/" + c.getName() + "/");
		String href = ((HtmlAnchor) p.getElementById("jnlp-link"))
				.getHrefAttribute();
		href = new URL(new URL(p.getDocumentURI()), href).toExternalForm();
		return href;
	}

}
