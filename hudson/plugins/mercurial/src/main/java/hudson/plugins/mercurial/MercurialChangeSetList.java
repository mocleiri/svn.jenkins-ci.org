package hudson.plugins.mercurial;

import hudson.scm.ChangeLogSet;
import hudson.model.AbstractBuild;

import java.util.List;
import java.util.Collections;
import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
public class MercurialChangeSetList extends ChangeLogSet<MercurialChangeSet> {
    private final List<MercurialChangeSet> changeSets;

    /*package*/ MercurialChangeSetList(AbstractBuild build, List<MercurialChangeSet> logs) {
        super(build);
        this.changeSets = Collections.unmodifiableList(logs);
        for (MercurialChangeSet log : logs)
            log.setParent(this);
    }

    public boolean isEmptySet() {
        return changeSets.isEmpty();
    }

    public Iterator<MercurialChangeSet> iterator() {
        return changeSets.iterator();
    }
}
