/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson.maven2;

import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.MavenAbstractArtifactRecord;
import hudson.maven.reporters.MavenAggregatedArtifactRecord;
import hudson.maven.reporters.MavenArtifact;
import hudson.maven.reporters.MavenArtifactRecord;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Result;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.client.IncludeExcludePatterns;
import org.jfrog.build.client.PatternMatcher;
import org.jfrog.hudson.ArtifactoryRedeployPublisher;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.util.IncludesExcludes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * Deploys artifacts to Artifactory.
 *
 * @author Yossi Shaul
 */
public class ArtifactsDeployer {

    private final ArtifactoryServer artifactoryServer;
    private final String targetReleasesRepository;
    private final String targetSnapshotsRepository;
    private final ArtifactoryBuildInfoClient client;
    private final MavenModuleSetBuild mavenModuleSetBuild;
    private final MavenAbstractArtifactRecord mar;
    private final BuildListener listener;
    private final IncludeExcludePatterns patterns;

    public ArtifactsDeployer(ArtifactoryRedeployPublisher artifactoryPublisher, ArtifactoryBuildInfoClient client,
            MavenModuleSetBuild mavenModuleSetBuild, MavenAbstractArtifactRecord mar,
            BuildListener listener) {
        this.client = client;
        this.mavenModuleSetBuild = mavenModuleSetBuild;
        this.mar = mar;
        this.listener = listener;
        this.artifactoryServer = artifactoryPublisher.getArtifactoryServer();
        this.targetReleasesRepository = artifactoryPublisher.getRepositoryKey();
        this.targetSnapshotsRepository = artifactoryPublisher.getSnapshotsRepositoryKey();
        IncludesExcludes patterns = artifactoryPublisher.getArtifactDeploymentPatterns();
        if (patterns != null) {
            this.patterns = new IncludeExcludePatterns(patterns.getIncludePatterns(), patterns.getExcludePatterns());
        } else {
            this.patterns = IncludeExcludePatterns.EMPTY;
        }
    }

    public void deploy() throws IOException, InterruptedException {
        listener.getLogger().println("Deploying artifacts to " + artifactoryServer.getUrl());
        MavenAggregatedArtifactRecord mar2 = (MavenAggregatedArtifactRecord) mar;
        MavenModuleSetBuild moduleSetBuild = mar2.getBuild();
        Map<MavenModule, MavenBuild> mavenBuildMap = moduleSetBuild.getModuleLastBuilds();

        for (Map.Entry<MavenModule, MavenBuild> mavenBuildEntry : mavenBuildMap.entrySet()) {
            MavenBuild mavenBuild = mavenBuildEntry.getValue();
            Result result = mavenBuild.getResult();
            if (Result.NOT_BUILT.equals(result)) {
                // HAP-52 - the module build might be skipped if using incremental build
                listener.getLogger().println(
                        "Module: '" + mavenBuildEntry.getKey().getName() + "' wasn't built. Skipping.");
                continue;
            }
            listener.getLogger().println("Deploying artifacts of module: " + mavenBuildEntry.getKey().getName());
            MavenArtifactRecord mar = ActionableHelper.getLatestMavenArtifactRecord(mavenBuild);
            MavenArtifact mavenArtifact = mar.mainArtifact;

            // deploy main artifact
            deployArtifact(mavenBuild, mavenArtifact);
            if (!mar.isPOM() && mar.pomArtifact != null && mar.pomArtifact != mar.mainArtifact) {
                // deploy the pom if the main artifact is not the pom
                deployArtifact(mavenBuild, mar.pomArtifact);
            }

            // deploy attached artifacts
            for (MavenArtifact attachedArtifact : mar.attachedArtifacts) {
                deployArtifact(mavenBuild, attachedArtifact);
            }
        }
    }

    private void deployArtifact(MavenBuild mavenBuild, MavenArtifact mavenArtifact)
            throws IOException, InterruptedException {
        String artifactPath = buildArtifactPath(mavenArtifact);

        if (PatternMatcher.pathConflicts(artifactPath, patterns)) {
            listener.getLogger().println("Skipping the deployment of '" + artifactPath +
                    "' due to the defined include-exclude patterns.");
            return;
        }

        File artifactFile = getArtifactFile(mavenBuild, mavenArtifact);

        DeployDetails.Builder builder = new DeployDetails.Builder()
                .file(artifactFile)
                .artifactPath(artifactPath)
                .targetRepository(getTargetRepository(mavenArtifact.version))
                .md5(mavenArtifact.md5sum)
                .addProperty("build.name", mavenModuleSetBuild.getParent().getDisplayName())
                .addProperty("build.number", mavenModuleSetBuild.getNumber() + "");

        Cause.UpstreamCause parent = ActionableHelper.getUpstreamCause(mavenModuleSetBuild);
        if (parent != null) {
            builder.addProperty("build.parentName", parent.getUpstreamProject())
                    .addProperty("build.parentNumber", parent.getUpstreamBuild() + "");
        }
        String revision = mavenModuleSetBuild.getEnvironment(listener).get("SVN_REVISION");
        if (StringUtils.isNotBlank(revision)) {
            builder.addProperty(BuildInfoProperties.PROP_VCS_REVISION, revision);
        }
        DeployDetails deployDetails = builder.build();
        logDeploymentPath(deployDetails, artifactPath);
        client.deployArtifact(deployDetails);
    }

    private void logDeploymentPath(DeployDetails deployDetails, String artifactPath) {
        String deploymentPath =
                artifactoryServer.getUrl() + "/" + deployDetails.getTargetRepository() + "/" + artifactPath;
        listener.getLogger().println("Deploying artifact: " + deploymentPath);
    }

    /**
     * @return Return the target deployment repository. Either the releases repository (default) or snapshots if defined
     *         and the deployed version is a snapshot.
     */
    public String getTargetRepository(String version) {
        if (targetSnapshotsRepository != null && version.endsWith("SNAPSHOT")) {
            return targetSnapshotsRepository;
        }
        return targetReleasesRepository;
    }

    private String buildArtifactPath(MavenArtifact mavenArtifact) {
        String directoryPath =
                mavenArtifact.groupId.replace('.', '/') + "/" + mavenArtifact.artifactId + "/" + mavenArtifact.version;
        return directoryPath + "/" + mavenArtifact.canonicalName;
    }

    /**
     * Obtains the {@link java.io.File} representing the archived artifact.
     */
    private File getArtifactFile(MavenBuild build, MavenArtifact mavenArtifact) throws FileNotFoundException {
        File file = new File(new File(new File(new File(build.getArtifactsDir(), mavenArtifact.groupId),
                mavenArtifact.artifactId), mavenArtifact.version), mavenArtifact.fileName);
        if (!file.exists()) {
            throw new FileNotFoundException("Archived artifact is missing: " + file);
        }
        return file;
    }
}
