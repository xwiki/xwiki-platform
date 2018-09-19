/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.test.docker.junit5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.visitor.FilteringDependencyVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves Maven Artifacts either from a remote repository or from a local POM file.
 *
 * @version $Id$
 * @since 10.9RC1
 */
public final class ArtifactResolver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactResolver.class);

    private static final String PLATFORM_GROUPID = "org.xwiki.platform";

    private static final String JAR = "jar";

    private static ArtifactResolver artifactResolver = new ArtifactResolver();

    private Map<String, List<ArtifactResult>> artifactResultCache = new HashMap<>();

    private RepositoryResolver repositoryResolver = RepositoryResolver.getInstance();

    private ArtifactResolver()
    {
        // Empty voluntarily, private constructor to ensure singleton
    }

    /**
     * @return the singleton instance for this class
     */
    public static ArtifactResolver getInstance()
    {
        return artifactResolver;
    }

    /**
     * Resolve the passed artifact (will resolve against the remote and local repositories and ensure that the file is
     * available on the local file system). Example usage:
     * <p>
     * <code><pre>
     * Artifact resourceArtifact =
     *     new DefaultArtifact("org.xwiki.platform", "xwiki-platform-tool-configuration-resources",
     *     "jar", "10.7-SNAPSHOT");
     * </pre></code>
     *
     * @param artifact the artifact to resolve (will resolve against the remote and local repositories and ensure that
     * the file is available on the local file system)
     * @return the resolved artifact result
     * @throws Exception if an error occurred during resolving
     */
    public ArtifactResult resolveArtifact(Artifact artifact) throws Exception
    {
        ArtifactRequest artifactRequest =
            new ArtifactRequest(artifact, this.repositoryResolver.getRepositories(), null);
        ArtifactResult artifactResult = this.repositoryResolver.getSystem().resolveArtifact(
            this.repositoryResolver.getSession(), artifactRequest);
        if (!artifactResult.getExceptions().isEmpty()) {
            sendError(artifact, artifactResult.getExceptions());
        }
        return artifactResult;
    }

    /**
     * @param artifact the artifact to resolve along with all its dependencies (will resolve against the remote and
     * local repositories and ensure that files are available on the local file system for the artifact and all its
     * dependencies)
     * @return the collection of the artifact results for the artifact and all its dependencies
     * @throws Exception if an error occurred during resolving
     */
    public Collection<ArtifactResult> getArtifactAndDependencies(Artifact artifact) throws Exception
    {
        List<ArtifactResult> artifactResults = new ArrayList<>();
        artifactResults.add(resolveArtifact(artifact));
        artifactResults.addAll(getArtifactDependencies(artifact));
        return artifactResults;
    }

    /**
     * @param artifact the artifact for which to resolve its dependencies (will resolve against the remote and local
     * repositories and ensure that files are available on the local file system for the artifact's dependencies)
     * @return the collection of the artifact results for the artifact's dependencies
     * @throws Exception if an error occurred during resolving
     */
    public Collection<ArtifactResult> getArtifactDependencies(Artifact artifact) throws Exception
    {
        // If in cache, serve from the cache for increased performances
        String artifactAsString = String.format("%s:%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(),
            artifact.getExtension(), artifact.getVersion());
        List<ArtifactResult> artifactResults = this.artifactResultCache.get(artifactAsString);
        if (artifactResults == null) {
            DependencyFilter filter = new AndDependencyFilter(Arrays.asList(
                new ScopeDependencyFilter(
                    Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME), Collections.emptyList()),
                // - Exclude JCL and LOG4J since we want all logging to go through SLF4J. Note that we're excluding
                // log4j-<version>.jar but keeping log4j-over-slf4j-<version>.jar
                // - Exclude batik-js to prevent conflict with the patched version of Rhino used by yuicompressor used
                // for JSX. See https://jira.xwiki.org/browse/XWIKI-6151 for more details.
                new ExclusionsDependencyFilter(Arrays.asList("org.apache.xmlgraphic:batik-js",
                    "commons-logging:commons-logging", "commons-logging:commons-logging-api", "log4j:log4j"))));

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new Dependency(artifact, null));
            collectRequest.setRepositories(this.repositoryResolver.getRepositories());

            long t1 = System.currentTimeMillis();
            DependencyNode node = this.repositoryResolver.getSystem().collectDependencies(
                this.repositoryResolver.getSession(), collectRequest).getRoot();
            LOGGER.info("collect = {} ms", (System.currentTimeMillis() - t1));

            node.accept(new FilteringDependencyVisitor(new DebuggingDependencyVisitor(), filter));

            DependencyRequest request = new DependencyRequest(node, filter);
            t1 = System.currentTimeMillis();
            DependencyResult result = this.repositoryResolver.getSystem().resolveDependencies(
                this.repositoryResolver.getSession(), request);
            LOGGER.info("resolve = {} ms", (System.currentTimeMillis() - t1));
            if (!result.getCollectExceptions().isEmpty()) {
                sendError(artifact, result.getCollectExceptions());
            }

            artifactResults = result.getArtifactResults();
            this.artifactResultCache.put(artifactAsString, artifactResults);
        }

        return artifactResults;
    }

    /**
     * @param xwikiVersion the version of the artifacts to resolve
     * @return the collection of resolved artifact results for the minimal XWiki distribution/flavor to be used for
     * functional tests
     * @throws Exception if an error occurred during resolving
     */
    public Collection<ArtifactResult> getDistributionDependencies(String xwikiVersion) throws Exception
    {
        List<ArtifactResult> artifactResults = new ArrayList<>();

        // TODO: Find a way to resolve all those artifacts at once to ensure we cannot have different versions of the
        // same jars in the returned collection, and for increased performances.

        Artifact rootDistributionArtifact = new DefaultArtifact(PLATFORM_GROUPID,
            "xwiki-platform-distribution-war-minimaldependencies", "pom", xwikiVersion);
        artifactResults.addAll(getArtifactDependencies(rootDistributionArtifact));

        // We provision XAR and JAR extensions (as dependencies of XAR extensions). Thus we need the associated
        // handlers.
        Artifact xarHandlerArtifact = new DefaultArtifact(PLATFORM_GROUPID, "xwiki-platform-extension-handler-xar",
            JAR, xwikiVersion);
        artifactResults.addAll(getArtifactAndDependencies(xarHandlerArtifact));
        Artifact jarHandlerArtifact = new DefaultArtifact(PLATFORM_GROUPID, "xwiki-platform-extension-handler-jar",
            JAR, xwikiVersion);
        artifactResults.addAll(getArtifactAndDependencies(jarHandlerArtifact));

        Artifact mavenHandlerArtifact = new DefaultArtifact("org.xwiki.commons",
            "xwiki-commons-extension-repository-maven-snapshots", JAR, xwikiVersion);
        artifactResults.addAll(getArtifactAndDependencies(mavenHandlerArtifact));

        // It seems that Maven Resolver is not able to resolve the ZIP dependencies from
        // xwiki-platform-distribution-war-minimaldependencies for some reason, so we manually ask for resolving the
        // skin dependency (which of ZIP type).
        Artifact skinArtifact = new DefaultArtifact(PLATFORM_GROUPID, "xwiki-platform-flamingo-skin-resources",
            "zip", xwikiVersion);
        artifactResults.addAll(getArtifactAndDependencies(skinArtifact));

        return artifactResults;
    }

    private void sendError(Artifact artifact, Collection<Exception> exceptions) throws Exception
    {
        for (Exception exception : exceptions) {
            LOGGER.error("Problem [{}]", exception);
        }
        throw new Exception(String.format("Failed to resolve artifact [%s]", artifact));
    }
}
