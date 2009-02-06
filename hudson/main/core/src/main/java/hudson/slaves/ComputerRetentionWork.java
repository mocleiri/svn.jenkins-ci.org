/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Stephen Connolly
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
package hudson.slaves;

import java.util.Map;
import java.util.WeakHashMap;

import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.triggers.SafeTimerTask;

/**
 * Periodically checks the slaves and try to reconnect dead slaves.
 *
 * @author Kohsuke Kawaguchi
 * @author Stephen Connolly
 */
public class ComputerRetentionWork extends SafeTimerTask {

    /**
     * Use weak hash map to avoid leaking {@link Computer}.
     */
    private final Map<Computer, Long> nextCheck = new WeakHashMap<Computer, Long>();

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    protected void doRun() {
        final long startRun = System.currentTimeMillis();
        for (Computer c : Hudson.getInstance().getComputers()) {
            if (!nextCheck.containsKey(c) || startRun > nextCheck.get(c)) {
                // at the moment I don't trust strategies to wait more than 60 minutes
                // strategies need to wait at least one minute
                final long waitInMins = Math.min(1, Math.max(60, c.getRetentionStrategy().check(c)));
                nextCheck.put(c, startRun + waitInMins*1000*60 /*MINS->MILLIS*/);
            }
        }
    }
}
