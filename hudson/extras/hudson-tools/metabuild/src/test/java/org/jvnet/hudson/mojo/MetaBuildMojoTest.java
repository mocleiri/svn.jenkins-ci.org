
/**
 * Copyright 2010 Fredhopper Research and Development
 *      http://www.fredhopper.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This code has been developed at Fredhopper and is hereby contributed
 * to the Hudson Continuous Integration project.
 */

package org.jvnet.hudson.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;

import static org.jvnet.hudson.constants.PathConstants.*;

/**
 * @author martin.todorov@fredhopper.com
 */
public class MetaBuildMojoTest extends AbstractMojoTestCase
{

    MetaBuildMojo mojo;


    protected void setUp() throws Exception
    {
        super.setUp();

        mojo = (MetaBuildMojo) lookupMojo("generate", POM_PLUGIN_METABUILD);
    }

    @Test
    public void testGenerateMetaBuild() throws MojoExecutionException, MojoFailureException
    {
        mojo.setMetaBuildInputFile(XML_METABUILD);
        mojo.setMetaBuildOutputFile(XML_METABUILD);
        mojo.setOutputDirectory(DIR_HUDSON_JOBS);
        mojo.execute();
    }

}
