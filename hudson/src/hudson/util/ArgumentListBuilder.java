package hudson.util;

import java.util.List;
import java.util.ArrayList;

/**
 * Used to build up arguments for a process invocation.
 *
 * @author Kohsuke Kawaguchi
 */
public class ArgumentListBuilder {
    private final List<String> args = new ArrayList<String>();

    public ArgumentListBuilder add(String a) {
        args.add(a);
        return this;
    }

    public ArgumentListBuilder add(String... args) {
        for (String arg : args) {
            add(arg);
        }
        return this;
    }

    public String[] toCommandArray() {
        return args.toArray(new String[args.size()]);
    }
}
