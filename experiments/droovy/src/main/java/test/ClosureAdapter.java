package test;

import groovy.lang.Closure;
import hudson.remoting.DelegatingCallable;

/**
 * @author Kohsuke Kawaguchi
 */
public class ClosureAdapter implements DelegatingCallable {
    private final Closure closure;

    public ClosureAdapter(Closure closure) {
        this.closure = closure;
    }

    @Override
    public Object call() throws Throwable {
        return closure.call();
    }

    @Override
    public ClassLoader getClassLoader() {
        return closure.getClass().getClassLoader();
    }
}
