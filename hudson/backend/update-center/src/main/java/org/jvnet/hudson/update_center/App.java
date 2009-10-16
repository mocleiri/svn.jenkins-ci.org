package org.jvnet.hudson.update_center;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.FlatSearchRequest;
import org.sonatype.nexus.index.FlatSearchResponse;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * Testing Nexus indexer based plugin discovery, as a part of the effort to move away from java.net.
 *
 * @author Kohsuke Kawaguchi
 */
public class App {

    private static final class Group {
        /**
         * ArtifactID equals short name.
         */
        final String artifactId;
        final Set<ArtifactInfo> artifacts = new HashSet<ArtifactInfo>();

        final Set<String> groupId = new TreeSet<String>();

        public Group(String shortName) {
            this.artifactId = shortName;
        }

        public Set<VersionNumber> getVersions() {
            Set<VersionNumber> r = new TreeSet<VersionNumber>();
            for (ArtifactInfo a : artifacts)
                r.add(new VersionNumber(a.version));
            return r;
        }
    }

    public static void main(String[] args) throws Exception {
        ClassWorld classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );
        ContainerConfiguration configuration = new DefaultContainerConfiguration().setClassWorld( classWorld );
        PlexusContainer plexus = new DefaultPlexusContainer( configuration );

        NexusIndexer indexer = plexus.lookup( NexusIndexer.class );
        indexer.addIndexingContext("java.net2","java.net2",null,new File("index"),null,null, NexusIndexer.DEFAULT_INDEX);

        BooleanQuery q = new BooleanQuery();
        q.add(indexer.constructQuery(ArtifactInfo.PACKAGING,"hpi"), Occur.MUST);

        FlatSearchRequest request = new FlatSearchRequest(q);
        FlatSearchResponse response = indexer.searchFlat(request);

        Map<String,Group> plugins = new TreeMap<String,Group>();

        for (ArtifactInfo a : response.getResults()) {
            if (a.version.contains("SNAPSHOT"))     continue;       // ignore snapshots

            Group p = plugins.get(a.artifactId);
            if (p==null)
                plugins.put(a.artifactId, p=new Group(a.artifactId));
            p.artifacts.add(a);
            p.groupId.add(a.groupId);
        }

        for (Map.Entry<String,Group> e : plugins.entrySet()) {
            Group g = e.getValue();
            if (g.groupId.size()>1)
                System.out.print(g.groupId+":");
            System.out.println(e.getKey()+"\t"+e.getValue().getVersions());
        }
    }
}
