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

import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.triggers.Trigger;

/**
 * Marker interface for {@link Action}s that should be displayed
 * at the top of the project page.
 *
 * {@link #getIconFileName()}, {@link #getUrlName()}, {@link #getDisplayName()}
 * are used to create a large, more visible icon in the top page to draw
 * users' attention.
 *
 * @see BuildStep#getProjectAction(AbstractProject)
 * @see BuildWrapper#getProjectAction(AbstractProject)
 * @see Trigger#getProjectAction()
 * @see JobProperty#getJobAction(Job)
 *
 * @author Kohsuke Kawaguchi
 */
public interface ProminentProjectAction extends Action {
    // TODO: do the rendering of the part from the action page
}
