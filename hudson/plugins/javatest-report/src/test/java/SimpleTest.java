/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */

import hudson.plugins.javatest_report.Report;
import hudson.plugins.javatest_report.TestCase;

import java.io.File;

public class SimpleTest extends TestCase {
    public void test1() throws Exception {
        Report r = new Report(null);
        File file = new File(getClass().getResource("trial1.xml").getPath());
        System.out.println(file.length());
        r.add(file);
        System.out.println(r.getFailCount()+"/"+r.getTotalCount());
    }
}
