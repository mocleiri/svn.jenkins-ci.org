package org.jvnet.hudson.update_center;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.transform.ArtifactTransformationManager;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.FlatSearchRequest;
import org.sonatype.nexus.index.FlatSearchResponse;
import org.sonatype.nexus.index.NexusIndexer;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Testing Nexus indexer based plugin discovery, as a part of the effort to move away from java.net.
 *
 * Using Maven embedder 2.0.4 results in problem caused by Plexus incompatibility.
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
        ClassWorld classWorld = new ClassWorld( "plexus.core", App.class.getClassLoader() );
        ContainerConfiguration configuration = new DefaultContainerConfiguration().setClassWorld( classWorld );
        PlexusContainer plexus = new DefaultPlexusContainer( configuration );
        plexus.getComponentDescriptor(ArtifactTransformationManager.class,
                ArtifactTransformationManager.class.getName(),"default").setImplementationClass(DefaultArtifactTransformationManager.class);


        NexusIndexer indexer = plexus.lookup( NexusIndexer.class );
        indexer.addIndexingContext("java.net2","java.net2",null,new File("index"),null,null, NexusIndexer.DEFAULT_INDEX);

        BooleanQuery q = new BooleanQuery();
        q.add(indexer.constructQuery(ArtifactInfo.PACKAGING,"hpi"), Occur.MUST);

        FlatSearchRequest request = new FlatSearchRequest(q);
        FlatSearchResponse response = indexer.searchFlat(request);

        ArtifactFactory af = plexus.lookup(ArtifactFactory.class);
        ArtifactResolver ar = plexus.lookup(ArtifactResolver.class);
        ArtifactRepositoryFactory arf = plexus.lookup(ArtifactRepositoryFactory.class);

        ArtifactRepositoryPolicy policy = new ArtifactRepositoryPolicy(true, "daily", "warn");
        List<ArtifactRepository> remoteRepositories = Collections.singletonList(
                arf.createArtifactRepository("m.g.o-public", "http://maven.glassfish.org/content/groups/public/",
                        new DefaultRepositoryLayout(), policy, policy));

        ArtifactRepository local = arf.createArtifactRepository("local",
                new File(new File(System.getProperty("user.home")), ".m2/repository").toURI().toURL().toExternalForm(),
                new DefaultRepositoryLayout(), policy, policy);

        Map<String,Group> plugins = new TreeMap<String,Group>();

        for (ArtifactInfo a : response.getResults()) {
            if (a.version.contains("SNAPSHOT"))     continue;       // ignore snapshots

            Group p = plugins.get(a.artifactId);
            if (p==null)
                plugins.put(a.artifactId, p=new Group(a.artifactId));
            p.artifacts.add(a);
            p.groupId.add(a.groupId);

            Artifact artifact = af.createArtifact(a.groupId, a.artifactId, a.version, "compile", a.packaging);

            ar.resolve(artifact,remoteRepositories,local);

            System.out.println(artifact.getFile());
        }

        for (Map.Entry<String,Group> e : plugins.entrySet()) {
            Group g = e.getValue();
            if (g.groupId.size()>1)
                System.out.print(g.groupId+":");
            System.out.println(e.getKey()+"\t"+e.getValue().getVersions());
        }
    }
}
