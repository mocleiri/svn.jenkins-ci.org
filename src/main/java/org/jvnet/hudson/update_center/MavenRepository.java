package org.jvnet.hudson.update_center;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.transform.ArtifactTransformationManager;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.FlatSearchRequest;
import org.sonatype.nexus.index.FlatSearchResponse;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Maven repository and its nexus index.
 *
 * Using Maven embedder 2.0.4 results in problem caused by Plexus incompatibility.
 *
 * @author Kohsuke Kawaguchi
 */
public class MavenRepository {
    private final NexusIndexer indexer;
    private final ArtifactFactory af;
    private final ArtifactResolver ar;
    private final List<ArtifactRepository> remoteRepositories;
    private final ArtifactRepository local;

    public MavenRepository() throws PlexusContainerException, ComponentLookupException, IOException, UnsupportedExistingLuceneIndexException {
        this("java.net2",new File("./index"));
    }

    public MavenRepository(String id, File indexDirectory) throws PlexusContainerException, ComponentLookupException, IOException, UnsupportedExistingLuceneIndexException {
        ClassWorld classWorld = new ClassWorld( "plexus.core", MavenRepository.class.getClassLoader() );
        ContainerConfiguration configuration = new DefaultContainerConfiguration().setClassWorld( classWorld );
        PlexusContainer plexus = new DefaultPlexusContainer( configuration );
        plexus.getComponentDescriptor(ArtifactTransformationManager.class,
                ArtifactTransformationManager.class.getName(),"default").setImplementationClass(DefaultArtifactTransformationManager.class);

        indexer = plexus.lookup( NexusIndexer.class );
        indexer.addIndexingContext(id, id,null, indexDirectory,null,null, NexusIndexer.DEFAULT_INDEX);

        af = plexus.lookup(ArtifactFactory.class);
        ar = plexus.lookup(ArtifactResolver.class);
        ArtifactRepositoryFactory arf = plexus.lookup(ArtifactRepositoryFactory.class);

        ArtifactRepositoryPolicy policy = new ArtifactRepositoryPolicy(true, "daily", "warn");
        remoteRepositories = Collections.singletonList(
                arf.createArtifactRepository("m.g.o-public", "http://maven.glassfish.org/content/groups/public/",
                        new DefaultRepositoryLayout(), policy, policy));
        local = arf.createArtifactRepository("local",
                new File(new File(System.getProperty("user.home")), ".m2/repository").toURI().toURL().toExternalForm(),
                new DefaultRepositoryLayout(), policy, policy);

    }

    File resolve(ArtifactInfo a) throws AbstractArtifactResolutionException {
        return resolve(a,a.packaging);
    }

    File resolve(ArtifactInfo a, String type) throws AbstractArtifactResolutionException {
        Artifact artifact = af.createArtifact(a.groupId, a.artifactId, a.version, "compile", type);
        ar.resolve(artifact,remoteRepositories,local);
        return artifact.getFile();
    }

    /**
     * Discover all plugins from this Maven repository.
     */
    public Collection<PluginHistory> listHudsonPlugins() throws PlexusContainerException, ComponentLookupException, IOException, UnsupportedExistingLuceneIndexException, AbstractArtifactResolutionException, ArtifactNotFoundException {
        BooleanQuery q = new BooleanQuery();
        q.add(indexer.constructQuery(ArtifactInfo.PACKAGING,"hpi"), Occur.MUST);

        FlatSearchRequest request = new FlatSearchRequest(q);
        FlatSearchResponse response = indexer.searchFlat(request);

        Map<String, PluginHistory> plugins = new TreeMap<String, PluginHistory>();

        for (ArtifactInfo a : response.getResults()) {
            if (a.version.contains("SNAPSHOT"))     continue;       // ignore snapshots

            PluginHistory p = plugins.get(a.artifactId);
            if (p==null)
                plugins.put(a.artifactId, p=new PluginHistory(a.artifactId));
            p.artifacts.put(new VersionNumber(a.version),new HPI(this,p,a));
            p.groupId.add(a.groupId);
        }

        return plugins.values();
    }

    public MavenArtifact getLatestHudsonWar() throws IOException, AbstractArtifactResolutionException {
        BooleanQuery q = new BooleanQuery();
        q.add(indexer.constructQuery(ArtifactInfo.GROUP_ID,"org.jvnet.hudson.main"), Occur.MUST);
        q.add(indexer.constructQuery(ArtifactInfo.PACKAGING,"war"), Occur.MUST);

        FlatSearchRequest request = new FlatSearchRequest(q);
        FlatSearchResponse response = indexer.searchFlat(request);

        VersionNumber latest = new VersionNumber("0.0");
        ArtifactInfo latestArtifact=null;
        for (ArtifactInfo a : response.getResults()) {
            if (a.version.contains("SNAPSHOT"))     continue;       // ignore snapshots
            if (!a.artifactId.equals("hudson-war"))  continue;      // somehow using this as a query results in 0 hits.

            VersionNumber v = new VersionNumber(a.version);
            if (v.compareTo(latest)>0) {
                latest = v;
                latestArtifact = a;
            }
        }

        if (latestArtifact==null)
            throw new IOException("No hudson.war found. Corrupt or outdated index?");

        return new MavenArtifact(this,latestArtifact);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(new MavenRepository().getLatestHudsonWar());
    }
}
