
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

package org.jvnet.hudson.parsers;

import org.jvnet.hudson.metadata.MetaBuild;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author martin.todorov@fredhopper.com
 */
public class MetaBuildParserTest
{

    @Test
    public void testMetaBuildParsing()
    {
        MetaBuildParser parser = new MetaBuildParser();
        MetaBuild metaBuild = parser.parse("target/test-classes/metabuild/metabuild.xml");

        Assert.assertTrue(metaBuild != null);
    }

}