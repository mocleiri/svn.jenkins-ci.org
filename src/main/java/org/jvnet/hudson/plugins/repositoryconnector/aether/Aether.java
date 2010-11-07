package org.jvnet.hudson.plugins.repositoryconnector.aether;

/*
 * Copyright (c) 2010 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0, 
 * and you may not use this file except in compliance with the Apache License Version 2.0. 
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the Apache License Version 2.0 is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.jvnet.hudson.plugins.repositoryconnector.Repository;
import org.ops4j.pax.url.aether.internal.ManualWagonProvider;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

public class Aether {
	private final Collection<Repository> remoteRepositories;
	private final RepositorySystem repositorySystem;
	private final LocalRepository localRepository;
	private final PrintStream logger;
	private final boolean extendedLogging;

	public Aether(Collection<Repository> remoteRepositories, File localRepository, PrintStream logger, boolean extendedLogging) {
		this.remoteRepositories = remoteRepositories;
		this.repositorySystem = newManualSystem();
		this.localRepository = new LocalRepository(localRepository);
		this.logger = logger;
		this.extendedLogging = extendedLogging;
	}

	public Aether(Repository remoteRepository, File localRepository, PrintStream logger, boolean extendedLogging) {
		this.remoteRepositories = new ArrayList<Repository>();
		remoteRepositories.add(remoteRepository);
		this.repositorySystem = newManualSystem();
		this.localRepository = new LocalRepository(localRepository);
		this.logger = logger;
		this.extendedLogging = extendedLogging;
	}

	private RepositorySystem newManualSystem() {
		DefaultServiceLocator locator = new DefaultServiceLocator();
		locator.setServices(WagonProvider.class, new ManualWagonProvider());
		locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
		return locator.getService(RepositorySystem.class);
	}

	private RepositorySystemSession newSession() {
		MavenRepositorySystemSession session = new MavenRepositorySystemSession();
		session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepository));
		if (extendedLogging) {
			session.setTransferListener(new ConsoleTransferListener(logger));
			session.setRepositoryListener(new ConsoleRepositoryListener(logger));
		}
		return session;
	}

	public AetherResult resolve(String groupId, String artifactId, String classifier, String extension, String version) throws DependencyCollectionException,
			ArtifactResolutionException {
		RepositorySystemSession session = newSession();
		Dependency dependency = new Dependency(new DefaultArtifact(groupId, artifactId, classifier, extension, version), "provided");

		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(dependency);

		logger.println("define repos...");
		for (Repository repo : remoteRepositories) {
			logger.println("define repo: " + repo);
			RemoteRepository repoObj = new RemoteRepository(repo.getId(), repo.getType(), repo.getUrl());
			collectRequest.addRepository(repoObj);
		}

		DependencyNode rootNode = repositorySystem.collectDependencies(session, collectRequest).getRoot();

		repositorySystem.resolveDependencies(session, rootNode, new ExcludeTranisitiveDependencyFilter());

		PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
		rootNode.accept(nlg);

		return new AetherResult(rootNode, nlg.getFiles());
	}

	public void install(Artifact artifact, Artifact pom) throws InstallationException {
		RepositorySystemSession session = newSession();

		InstallRequest installRequest = new InstallRequest();
		installRequest.addArtifact(artifact).addArtifact(pom);

		repositorySystem.install(session, installRequest);
	}

	public void deploy(Artifact artifact, Artifact pom, String remoteRepository) throws DeploymentException {
		RepositorySystemSession session = newSession();

		final Repository repo = remoteRepositories.iterator().next();
		RemoteRepository nexus = new RemoteRepository(repo.getId(), repo.getType(), repo.getUrl());
		Authentication authentication = new Authentication("admin", "admin123");
		nexus.setAuthentication(authentication);

		DeployRequest deployRequest = new DeployRequest();
		deployRequest.addArtifact(artifact);// .addArtifact(pom);
		deployRequest.setRepository(nexus);

		repositorySystem.deploy(session, deployRequest);
	}

}
