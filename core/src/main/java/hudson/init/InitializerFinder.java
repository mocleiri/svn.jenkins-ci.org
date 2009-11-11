/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc.
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
package hudson.init;

import org.jvnet.hudson.annotation_indexer.Index;
import org.jvnet.hudson.reactor.Milestone;
import org.jvnet.hudson.reactor.Session;
import org.jvnet.hudson.reactor.Task;
import org.jvnet.hudson.reactor.TaskBuilder;
import org.jvnet.hudson.reactor.MilestoneImpl;
import org.jvnet.localizer.ResourceBundleHolder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import hudson.model.Hudson;

/**
 * Discovers initialization tasks from {@link Initializer}.
 *
 * @author Kohsuke Kawaguchi
 */
public class InitializerFinder extends TaskBuilder {
    private final ClassLoader cl;

    public InitializerFinder(ClassLoader cl) {
        this.cl = cl;
    }

    public InitializerFinder() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public Collection<Task> discoverTasks(Session session) throws IOException {
        List<Task> result = new ArrayList<Task>();
        for ( final Method e : Index.list(Initializer.class,cl,Method.class)) {
            if (!Modifier.isStatic(e.getModifiers()))
                throw new IOException(e+" is not a static method");

            final Initializer i = e.getAnnotation(Initializer.class);
            if (i==null)        continue; // stale index

            result.add(new Task() {
                final Collection<Milestone> requires = toMilestones(i.requires(), i.after());
                final Collection<Milestone> attains = toMilestones(i.attains(), i.before());

                public Collection<Milestone> requires() {
                    return requires;
                }

                public Collection<Milestone> attains() {
                    return attains;
                }

                public String getDisplayName() {
                    return getDisplayNameOf(e,i);
                }

                public void run(Session session) {
                    invoke(e);
                }

                public String toString() {
                    return e.toString();
                }

                private Collection<Milestone> toMilestones(String[] tokens, InitMilestone m) {
                    List<Milestone> r = new ArrayList<Milestone>();
                    for (String s : tokens) {
                        try {
                            r.add(InitMilestone.valueOf(s));
                        } catch (IllegalArgumentException x) {
                            r.add(new MilestoneImpl(s));
                        }
                    }
                    r.add(m);
                    return r;
                }
            });
        }
        return result;
    }

    /**
     * Obtains the display name of the given initialization task
     */
    protected String getDisplayNameOf(Method e, Initializer i) {
        try {
            Class<?> c = e.getDeclaringClass();
            ResourceBundleHolder rb = ResourceBundleHolder.get(c.getClassLoader().loadClass(c.getPackage().getName() + ".Messages"));

            String key = i.displayName();
            if (key.length()==0)  return c.getSimpleName()+"."+e.getName();
            return rb.format(key);
        } catch (ClassNotFoundException x) {
            throw (Error)new NoClassDefFoundError(x.getMessage()+" for "+e.toString()).initCause(x);
        }
    }

    /**
     * Invokes the given initialization method.
     */
    protected void invoke(Method e) {
        try {
            Class<?>[] pt = e.getParameterTypes();
            Object[] args = new Object[pt.length];
            for (int i=0; i<args.length; i++)
                args[i] = lookUp(pt[i]);
            e.invoke(null,args);
        } catch (IllegalAccessException x) {
            throw (Error)new IllegalAccessError().initCause(x);
        } catch (InvocationTargetException x) {
            throw new Error(x);
        }
    }

    /**
     * Determines the parameter injection of the initialization method.
     */
    private Object lookUp(Class<?> type) {
        if (type== Hudson.class)
            return Hudson.getInstance();
        throw new IllegalArgumentException("Unable to inject "+type);
    }
}
