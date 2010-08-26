/** *****************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
 * Author : Gregory Boissinot                                                   *
 *                                                                              *
 * Permission is hereby granted, free of charge, to any person obtaining a copy *
 * of this software and associated documentation files (the "Software"), to deal*
 * in the Software without restriction, including without limitation the rights *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
 * copies of the Software, and to permit persons to whom the Software is        *
 * furnished to do so, subject to the following conditions:                     *
 *                                                                              *
 * The above copyright notice and this permission notice shall be included in   *
 * all copies or substantial portions of the Software.                          *
 *                                                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
 * THE SOFTWARE.                                                                *
 ****************************************************************************** */

package com.thalesgroup.dtkit.maven;


import com.thalesgroup.dtkit.metrics.api.InputMetric
import com.thalesgroup.dtkit.metrics.api.InputType
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.project.MavenProject

/**
 * Generate some Hudson source file from generic input type
 *
 * @goal generate
 */
public class GeneratorHudsonMojo extends AbstractMojo {

  /**
   * @parameter expression="${project}"
   * @required
   */
  private MavenProject project;

  /**
   * <i>Maven Internal</i>: List of runtime artifacts for the projects.
   *
   * @parameter expression="${project.runtimeArtifacts}"
   * @required
   * @readonly
   */
  private List projectClasspathList;


  public void execute()
  throws MojoExecutionException {


    try {

      //Build a new classloader     
      File output =  new File(project.getBuild().getOutputDirectory())
      URL[] urls;
      if (output.exists()) {
         urls = new URL[projectClasspathList.size() + 1];
         urls[urls.length - 1] = new URL("file:///" + project.getBuild().getOutputDirectory() + "/");
      }
      else {
        urls = new URL[+ projectClasspathList.size()];
      }
      for (int i = 0; i < projectClasspathList.size(); i++) {
        urls[i] = new URL("file:///" + ((Artifact) projectClasspathList.get(i)).getFile().getPath());
      }      
      ClassLoader cl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

      // Retrieve all input type
      getLog().info("Introspecting all DTKIT metrics");
      ServiceLoader<InputMetric> metricsLoader = ServiceLoader.load(InputMetric.class, cl);
      metricsLoader.reload();

      //Create Hudson classes
      Iterator<InputMetric> commandsIterator = metricsLoader.iterator();
      while (commandsIterator.hasNext()) {
        InputMetric metric = commandsIterator.next();
        getLog().info("Genererating the Hudson class for "+ metric.getLabel() + " metric");
        switch (metric.getToolType()) {
          case InputType.TEST: generateTest("com.thalesgroup.dtkit.metrics.hudson.model", metric); break;
          case InputType.COVERAGE: generateCoverage("com.thalesgroup.dtkit.metrics.hudson.model", metric); break;
          case InputType.MEASURE: generateMeasure("com.thalesgroup.dtkit.metrics.hudson.model", metric); break;
          case InputType.VIOLATION: generateViolation("com.thalesgroup.dtkit.metrics.hudson.model", metric);  break;
        }
      }
    }
    catch (Exception e) {
      getLog().error("A problem occured during Hudson generation " + e);
    }
  }

  public generateTest(String packageName, InputMetric inputMetric) throws Exception {
    generate("TestType", "TestTypeDescriptor", packageName, inputMetric);
  }

  public generateCoverage(String packageName, InputMetric inputMetric) throws Exception {
    generate("CoverageType", "CoverageTypeDescriptor", packageName, inputMetric);
  }

  public generateMeasure(String packageName, InputMetric inputMetric) throws Exception {
    generate("MeasureType", "MeasureTypeDescriptor", packageName, inputMetric);
  }

  public generateViolation(String packageName, InputMetric inputMetric) throws Exception {
    generate("ViolationsType", "ViolationsTypeDescriptor", packageName, inputMetric);
  }

  public generate(String hudsonType, hudsonDescriptorType, String packageName, InputMetric inputMetric) throws Exception {
    // Where to write the classes
    File targetDirectory = new File(project.build.directory + "/generated-sources/groovy")

    // The directory to write the source to
    File packageDir = new File(targetDirectory, packageName.replace('.', '/'))

    def buildName = inputMetric.getToolName() + "Hudson" + hudsonType
    def classname = buildName.substring(0, 1).toUpperCase() + buildName.substring(1);

    // Now to create our enum
    def out = []

    out << "package " + packageName + ";\n"

    out << "\n"

    out << "import org.kohsuke.stapler.DataBoundConstructor;\n"
    out << "import hudson.Extension;\n"

    out << "import com.thalesgroup.dtkit.metrics.hudson.api.type.${hudsonType};\n"
    out << "import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.${hudsonDescriptorType};\n"

    out << "\n"
    out << "public class " + classname + " extends ${hudsonType} {\n"
    out << "\n"

    out << "private static ${hudsonDescriptorType}<? extends ${hudsonType}> DESCRIPTOR = new " + classname + ".DescriptorImpl();\n"
    out << "\n"
    
    out << "@DataBoundConstructor\n"
    out << "public " + classname + "(String pattern, boolean faildedIfNotNew, boolean deleteOutputFiles) {\n"
    out << "  super(pattern, faildedIfNotNew, deleteOutputFiles);\n"
    out << "}\n"
    out << "\n"

    out << "public ${hudsonDescriptorType}<? extends ${hudsonType}> getDescriptor() {\n"
    out << " return  DESCRIPTOR;\n"
    out << "}\n"
    out << "\n"

    out << "@Extension\n"
    out << "public static class DescriptorImpl  extends ${hudsonDescriptorType}<" + classname + "> {\n"
    out << "\n"

    out << "  public DescriptorImpl() {\n"
    String classType = inputMetric.getMetaClass().getTheClass().toString();
    classType = classType.substring("class ".length());
    classType = classType + ".class"
    out << "     super(" + classname + ".class, " + classType + ");\n"
    out << "  }\n"
    out << "\n"

    out << "public String getId() {\n"
    out << " return \"" +   inputMetric.getMetaClass().getTheClass() + "\";\n"
    out << "}\n"
    out << "\n"

    out << "}\n"
    out << "\n"

    // Finish the  class
    out << "}\n"

    // Convert the array into a string
    StringBuilder sb = new StringBuilder()
    out.each { sb.append(it) }

    // Now write the source, ensuring the directory exists first
    packageDir.mkdirs()
    new File(packageDir, classname + ".java").write(sb.toString());
  }

}
