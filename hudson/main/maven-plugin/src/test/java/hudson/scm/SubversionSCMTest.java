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
package hudson.scm;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * @author Kohsuke Kawaguchi
 */
public class SubversionSCMTest extends TestCase {
    public void test1() {
        check("http://foobar/");
        check("https://foobar/");
        check("file://foobar/");
        check("svn://foobar/");
        check("svn+ssh://foobar/");
    }

    public void test2() {
        String[] r = "abc\\ def ghi".split("(?<!\\\\)[ \\r\\n]+");
        for (int i = 0; i < r.length; i++) {
            r[i] = r[i].replaceAll("\\\\ "," ");
        }
        System.out.println(Arrays.asList(r));
        assertEquals(r.length,2);
    }

    private void check(String url) {
        assertTrue(SubversionSCM.URL_PATTERN.matcher(url).matches());
    }
}
