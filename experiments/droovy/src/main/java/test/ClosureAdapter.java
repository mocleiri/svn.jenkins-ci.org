package test;

import groovy.lang.Closure;
import hudson.remoting.DelegatingCallable;
import hudson.remoting.Callable;

/**
 * Takes a {@link Closure} and wraps it as a {@link Callable}.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClosureAdapter implements DelegatingCallable {
    private final Closure closure;

    public ClosureAdapter(Closure closure) {
        this.closure = closure;
    }

    public Object call() throws Throwable {
        return closure.call();
    }

    public ClassLoader getClassLoader() {
        return closure.getClass().getClassLoader();
    }
}
