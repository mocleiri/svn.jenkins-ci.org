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


import com.thalesgroup.dtkit.metrics.model.InputMetric;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Generate some Hudson source file from generic input type
 *
 * @goal generate
 */
public class GeneratorMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * <i>Maven Internal</i>: List of compile artifacts for the projects.
     *
     * @parameter expression="${project.runtimeArtifacts}"
     * @required
     * @readonly
     */
    private List projectClasspathList;


    public void execute() throws MojoExecutionException {


        try {

            //-------------------- Filtering projectClasspathList
            List computeClassPathList = new ArrayList();

            for (int i = 0; i < projectClasspathList.size(); i++) {
                Artifact artifact = (Artifact) projectClasspathList.get(i);
                //Exclude all Hudson plugins
                if (!"org.jvnet.hudson.plugins".equals(artifact.getGroupId())) {
                    computeClassPathList.add(artifact);
                }
            }

            //Building a new classloader
            File output = new File(project.getBuild().getOutputDirectory());
            URL[] urls;
            if (output.exists()) {
                urls = new URL[computeClassPathList.size() + 1];
                urls[urls.length - 1] = new URL("file:///" + project.getBuild().getOutputDirectory() + "/");
            } else {
                urls = new URL[+computeClassPathList.size()];
            }


            for (int i = 0; i < computeClassPathList.size(); i++) {
                Artifact artifact = (Artifact) computeClassPathList.get(i);
                urls[i] = new URL("file:///" + artifact.getFile().getPath());
            }
            ClassLoader cl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

            // Retrieving all input type
            getLog().info("Introspecting all DTKIT metrics");
            ServiceLoader<InputMetric> metricsLoader = ServiceLoader.load(InputMetric.class, cl);
            metricsLoader.reload();


            //Creating Hudson classes
            Iterator<InputMetric> commandsIterator = metricsLoader.iterator();
            while (commandsIterator.hasNext()) {
                InputMetric metric = commandsIterator.next();
                getLog().info("Genererating the Hudson class for " + metric.getLabel() + " metric");
                switch (metric.getToolType()) {
                    case TEST:
                        generateTest("com.thalesgroup.dtkit.metrics.hudson.model", metric);
                        break;
                    case COVERAGE:
                        generateCoverage("com.thalesgroup.dtkit.metrics.hudson.model", metric);
                        break;
                    case MEASURE:
                        generateMeasure("com.thalesgroup.dtkit.metrics.hudson.model", metric);
                        break;
                    case VIOLATION:
                        generateViolation("com.thalesgroup.dtkit.metrics.hudson.model", metric);
                        break;
                }
            }
        }
        catch (Exception e) {
            getLog().error("A problem occured during Hudson generation " + e);
        }
    }

    private void generateHudsonClass(String packageName, InputMetric inputMetric, String hudsonType, String hudsonDescriptorType) throws IOException {

        // Where to write the classes
        File targetDirectory = new File(project.getBuild().getDirectory() + "/generated-sources/groovy");
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        // The directory to write the source to
        File packageDir = new File(targetDirectory, packageName.replace('.', '/'));

        String buildName;
        if (inputMetric.getOutputFormatType() != null) {
            String keyFormat = inputMetric.getOutputFormatType().getKey();
            keyFormat = keyFormat.substring(0, 1).toUpperCase() + keyFormat.substring(1);
            buildName = inputMetric.getToolName() + keyFormat + "Hudson" + hudsonType;
        } else {
            buildName = inputMetric.getToolName() + "Hudson" + hudsonType;
        }

        String classname = buildName.substring(0, 1).toUpperCase() + buildName.substring(1);

        String newClassContent = new HudsonGenerator().getGeneratedClass(classname, hudsonType, hudsonDescriptorType, packageName, inputMetric);

        // Now write the source, ensuring the directory exists first
        packageDir.mkdirs();
        FileOutputStream fos = new FileOutputStream(new File(packageDir, classname + ".java"));
        fos.write(newClassContent.getBytes());
        fos.close();
    }

    private void generateTest(String packageName, InputMetric inputMetric) throws Exception {
        generateHudsonClass(packageName, inputMetric, "TestType", "TestTypeDescriptor");
    }

    private void generateCoverage(String packageName, InputMetric inputMetric) throws Exception {
        generateHudsonClass(packageName, inputMetric, "CoverageType", "CoverageTypeDescriptor");
    }

    private void generateMeasure(String packageName, InputMetric inputMetric) throws Exception {
        generateHudsonClass(packageName, inputMetric, "MeasureType", "MeasureTypeDescriptor");
    }

    private void generateViolation(String packageName, InputMetric inputMetric) throws Exception {
        generateHudsonClass(packageName, inputMetric, "ViolationsType", "ViolationsTypeDescriptor");
    }

}
