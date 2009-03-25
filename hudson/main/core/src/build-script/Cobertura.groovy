import org.apache.maven.project.MavenProject;

/**
 * Cobertura invoker.
 */
public class Cobertura {
    private final MavenProject project;
    // maven helper
    private def maven;
    // ant builder
    private def ant;
    /**
     * Cobertura data file.
     */
    private final File ser;

    def Cobertura(project, maven, ant, ser) {
        this.project = maven.project;
        this.maven = maven;
        this.ant = ant;
        this.ser =ser;

        // define cobertura tasks
        ant.taskdef(resource:"tasks.properties")
    }

    // function that ensures that the given directory exists
    private String dir(String dir) {
        new File(project.basedir,dir).mkdirs();
        return dir;
    }

    /**
     * Instruments the given class dirs/jars by cobertura
     *
     * @param files
     *      List of jar files and class dirs to instrument.
     */
    def instrument(files) {
        ant."cobertura-instrument"(todir:dir("target/cobertura-classes"),datafile:ser) {
            fileset(dir:"target/classes");
            files.each{ fileset(file:it) }
        }
    }

    def runTests() {
        ant.junit(fork:true, forkMode:"once", failureproperty:"failed", printsummary:true) {
            classpath {
                junitClasspath()
            }
            batchtest(todir:dir("target/surefire-reports")) {
                fileset(dir:"src/test/java") {
                    include(name:"**/*Test.java")
                }
                formatter(type:"xml")
            }
            sysproperty(key:"net.sourceforge.cobertura.datafile",value:ser)
        }
    }

    def junitClasspath() {
        ant.pathelement(path: "target/cobertura-classes") // put the instrumented classes first
        ant.fileset(dir:"target/cobertura-classes",includes:"*.jar") // instrumented jar files
        ant.pathelement(path: maven.resolveArtifact("net.sourceforge.cobertura:cobertura:1.9")) // cobertura runtime
        project.getTestClasspathElements().each { ant.pathelement(path: it) } // the rest of the dependencies
    }

    def report(dirs) {
        maven.attachArtifact(ser,"ser","cobertura")
        ant."cobertura-report"(format:"html",datafile:ser,destdir:dir("target/cobertura-reports"),srcdir:"src/main/java") {
            dirs.each{ fileset(dir:it) }
        }
    }

    def makeBuildFailIfTestFail() {
        if(ant.project.getProperty("failed")!=null)
            throw new Exception("Some unit tests failed");
    }
}
