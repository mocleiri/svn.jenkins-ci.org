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
package hudson.model;

import hudson.slaves.WorkspaceList;
import hudson.slaves.WorkspaceList.Lease;

import java.io.IOException;
import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
public class FreeStyleBuild extends Build<FreeStyleProject,FreeStyleBuild> {
    public FreeStyleBuild(FreeStyleProject project) throws IOException {
        super(project);
    }

    public FreeStyleBuild(FreeStyleProject project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    @Override
    public void run() {
        run(new RunnerImpl());
    }

    protected class RunnerImpl extends Build<FreeStyleProject,FreeStyleBuild>.RunnerImpl {
        @Override
        protected Lease decideWorkspace(Node n, WorkspaceList wsl) throws IOException, InterruptedException {
            String customWorkspace = getProject().getCustomWorkspace();
            if (customWorkspace != null)
                // we allow custom workspaces to be concurrently used between jobs.
                return Lease.createDummyLease(n.getRootPath().child(getEnvironment(listener).expand(customWorkspace)));
            return super.decideWorkspace(n,wsl);
        }
    }
}
