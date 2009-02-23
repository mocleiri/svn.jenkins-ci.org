package hudson.tasks;

import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.JDK;
import hudson.model.Label;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import hudson.tasks.Ant.AntInstallation;
import hudson.tasks.Maven.MavenInstallation;

import java.io.File;
import java.util.TreeMap;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.HudsonTestCase;

public class EnvVarsInConfigTasksTest extends HudsonTestCase {
	public static final String DUMMY_LOCATION_VARNAME = "TOOLS_DUMMY_LOCATION";

	private DumbSlave slaveEnv = null;
	private DumbSlave slaveRegular = null;

	public void setUp() throws Exception {
		super.setUp();

		JDK defaultJDK = hudson.getJDK(null);
		JDK varJDK = new JDK("varJDK", withVariable(defaultJDK.getJavaHome()));
		hudson.getJDKs().add(varJDK);

		// Maven with a variable in its path
		configureDefaultMaven();
		MavenInstallation defaultMaven = Maven.DESCRIPTOR.getInstallations()[0];
		MavenInstallation varMaven = new MavenInstallation("varMaven",
				withVariable(defaultMaven.getMavenHome()));
		Maven.DESCRIPTOR.setInstallations(varMaven);

		// Ant with a variable in its path
		// TODO: create HudsonTestCase.configureDefaultAnt()
        String guessedAntHome = guessAntHome();
        if (guessedAntHome != null) {
            AntInstallation antInstallation = new AntInstallation("varAnt",
                    withVariable(guessedAntHome));
            Ant.DESCRIPTOR.setInstallations(antInstallation);
        }

		// createSlaves
		TreeMap<String, String> additionalEnv = new TreeMap<String, String>();
		additionalEnv.put(DUMMY_LOCATION_VARNAME, "");
		slaveEnv = createSlave(new Label("slaveEnv"), additionalEnv);

		slaveRegular = createSlave(new Label("slaveRegular"));
	}

	private String withVariable(String s) {
		return s + "${" + DUMMY_LOCATION_VARNAME + "}";
	}

	public void testFreeStyleShellOnSlave() throws Exception {
		FreeStyleProject project = createFreeStyleProject();
		if (Os.isFamily("dos")) {
			project.getBuildersList().add(new BatchFile("echo %JAVA_HOME%"));
		} else {
			project.getBuildersList().add(new Shell("echo \"$JAVA_HOME\""));
		}
		project.setJDK(hudson.getJDK("varJDK"));

		// set appropriate SCM to get the necessary build files
		project.setScm(new ExtractResourceSCM(getClass().getResource(
				"/simple-projects.zip")));

		// test the regular slave - variable not expanded
		project.setAssignedLabel(slaveRegular.getSelfLabel());
		FreeStyleBuild build = project.scheduleBuild2(0).get();
		System.out.println(build.getDisplayName() + " completed");

		assertBuildStatusSuccess(build);

		String buildLogRegular = build.getLog();
		System.out.println(buildLogRegular);
		assertTrue(buildLogRegular.contains(DUMMY_LOCATION_VARNAME));

		// test the slave with prepared environment
		project.setAssignedLabel(slaveEnv.getSelfLabel());
		build = project.scheduleBuild2(0).get();
		System.out.println(build.getDisplayName() + " completed");

		assertBuildStatusSuccess(build);

		// Check variable was expanded
		String buildLogEnv = build.getLog();
		System.out.println(buildLogEnv);
		assertFalse(buildLogEnv.contains(DUMMY_LOCATION_VARNAME));
	}

	public void testFreeStyleAntOnSlave() throws Exception {
        if (Ant.DESCRIPTOR.getInstallations().length == 0) {
            System.out.println("Cannot do testFreeStyleAntOnSlave without ANT_HOME");
            return;
        }
		FreeStyleProject project = createFreeStyleProject();
		project.setJDK(hudson.getJDK("varJDK"));
		project.setScm(new ExtractResourceSCM(getClass().getResource(
				"/simple-projects.zip")));

		String buildFile = "build.xml${" + DUMMY_LOCATION_VARNAME + "}";
		// we need additional escapes because bash itself expanding
		project.getBuildersList().add(
				new Ant("-Dtest.property=cor${" + DUMMY_LOCATION_VARNAME
						+ "}rect", "varAnt", "", buildFile, ""));

		// test the regular slave - variable not expanded
		project.setAssignedLabel(slaveRegular.getSelfLabel());
		FreeStyleBuild build = project.scheduleBuild2(0).get();
		System.out.println(build.getDisplayName() + " completed");

		assertBuildStatus(Result.FAILURE, build);

		String buildLogRegular = build.getLog();
		assertTrue(buildLogRegular.contains(Messages
				.Ant_ExecutableNotFound("varAnt")));

		// test the slave with prepared environment
		project.setAssignedLabel(slaveEnv.getSelfLabel());
		build = project.scheduleBuild2(0).get();
		System.out.println(build.getDisplayName() + " completed");

		assertBuildStatusSuccess(build);

		// Check variable was expanded
		String buildLogEnv = build.getLog();
		System.out.println(buildLogEnv);
		assertTrue(buildLogEnv.contains("Ant home: "));
		assertTrue(buildLogEnv.contains("Test property: correct"));
		assertFalse(buildLogEnv.matches("(?s)^.*Ant home: [^\\n\\r]*"
				+ DUMMY_LOCATION_VARNAME + ".*$"));
		assertFalse(buildLogEnv.matches("(?s)^.*Test property: [^\\n\\r]*"
				+ DUMMY_LOCATION_VARNAME + ".*$"));
	}

	public void testFreeStyleMavenOnSlave() throws Exception {
		FreeStyleProject project = createFreeStyleProject();
		project.setJDK(hudson.getJDK("varJDK"));
		project.setScm(new ExtractResourceSCM(getClass().getResource(
				"/simple-projects.zip")));

		project.getBuildersList().add(
				new Maven("test", "varMaven", "pom.xml${"
						+ DUMMY_LOCATION_VARNAME + "}", "", ""));

		// test the regular slave - variable not expanded
		project.setAssignedLabel(slaveRegular.getSelfLabel());
		FreeStyleBuild build = project.scheduleBuild2(0).get();
		System.out.println(build.getDisplayName() + " completed");

		assertBuildStatus(Result.FAILURE, build);

		String buildLogRegular = build.getLog();
		System.out.println(buildLogRegular);
		assertTrue(buildLogRegular.contains(DUMMY_LOCATION_VARNAME));

		// test the slave with prepared environment
		project.setAssignedLabel(slaveEnv.getSelfLabel());
		build = project.scheduleBuild2(0).get();
		System.out.println(build.getDisplayName() + " completed");

		assertBuildStatusSuccess(build);

		// Check variable was expanded
		String buildLogEnv = build.getLog();
		System.out.println(buildLogEnv);
		assertFalse(buildLogEnv.contains(DUMMY_LOCATION_VARNAME));
	}

	public static String guessAntHome() {
		String antHome = EnvVars.masterEnvVars.get("ANT_HOME");
		if (antHome != null)
			return antHome;

		// will break if PATH variable is not found (e.g. case sensitivity)
		String[] sysPath = EnvVars.masterEnvVars.get("PATH").split(
				File.pathSeparator);
		for (String p : sysPath)
			if (new File(p, "ant").isFile() || new File(p, "ant.bat").isFile())
				return new File(p).getParent();

		throw new RuntimeException("cannot find Apache Ant");
	}
}
