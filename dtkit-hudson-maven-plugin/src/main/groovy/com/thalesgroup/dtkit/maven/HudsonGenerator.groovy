/** *****************************************************************************
 * Copyright (c) 2011 Thales Corporate Services SAS                             *
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

package com.thalesgroup.dtkit.maven

import com.thalesgroup.dtkit.metrics.model.InputMetric

public class HudsonGenerator {

  public String getGeneratedClass(String className, String hudsonType, hudsonDescriptorType, String packageName, InputMetric inputMetric) {

    // Now to create our enum
    def out = []

    out << "package " + packageName + ";\n"

    out << "\n"

    out << "import org.kohsuke.stapler.DataBoundConstructor;\n"
    out << "import hudson.Extension;\n"

    out << "import com.thalesgroup.dtkit.metrics.hudson.api.type.${hudsonType};\n"
    out << "import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.${hudsonDescriptorType};\n"

    out << "\n"
    out << "public class " + className + " extends ${hudsonType} {\n"
    out << "\n"

    out << "    private static ${hudsonDescriptorType}<? extends ${hudsonType}> DESCRIPTOR = new " + className + ".DescriptorImpl();\n"
    out << "\n"

    out << "    @DataBoundConstructor\n"
    out << "    public " + className + "(String pattern, boolean faildedIfNotNew, boolean deleteOutputFiles, boolean stopProcessingIfError) {\n"
    out << "        super(pattern, faildedIfNotNew, deleteOutputFiles, stopProcessingIfError);\n"
    out << "    }\n"
    out << "\n"

    out << "    public ${hudsonDescriptorType}<? extends ${hudsonType}> getDescriptor() {\n"
    out << "        return  DESCRIPTOR;\n"
    out << "    }\n"
    out << "\n"

    out << "    @Extension\n"
    out << "    public static class DescriptorImpl  extends ${hudsonDescriptorType}<" + className + "> {\n"
    out << "\n"

    out << "        public DescriptorImpl() {\n"
    String classType = inputMetric.getMetaClass().getTheClass().toString();
    classType = classType.substring("class ".length());
    classType = classType + ".class"
    out << "            super(" + className + ".class, " + classType + ");\n"
    out << "        }\n"
    out << "\n"

    out << "        public String getId() {\n"
    out << "            return \"" + inputMetric.getMetaClass().getTheClass() + "\";\n"
    out << "        }\n"
    out << "\n"

    out << "    }\n"
    out << "\n"

    // Finish the  class
    out << "}\n"

    // Convert the array into a string
    StringBuilder sb = new StringBuilder()
    out.each { sb.append(it) }

    return sb.toString();
  }

}