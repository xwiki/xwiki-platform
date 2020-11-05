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
package org.xwiki.test.integration.maven;

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
import org.eclipse.aether.collection.CollectResult;
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
 * @since 10.9
 */
public class ArtifactResolver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactResolver.class);

    private static final String COMMONS_GROUPID = "org.xwiki.commons";

    private static final String PLATFORM_GROUPID = "org.xwiki.platform";

    private static final String JAR = "jar";

    private boolean isOffline;

    private boolean isDebug;

    private RepositoryResolver repositoryResolver;

    private Map<String, List<ArtifactResult>> artifactResultCache = new HashMap<>();

    /**
     * @param isOffline if true then don't go out to resolve artifacts
     * @param isDebug if true then display debugging messages in the standard output
     * @param repositoryResolver the resolver to create Maven repositories and sessions
     */
    public ArtifactResolver(boolean isOffline, boolean isDebug, RepositoryResolver repositoryResolver)
    {
        this.isOffline = isOffline;
        this.isDebug = isDebug;
        this.repositoryResolver = repositoryResolver;
    }

    /**
     * Resolve the passed artifact (will resolve against the remote and local repositories and ensure that the file is
     * available on the local file system). Example usage:
     * <p>
     * Example usage:
     * <pre><code>
     * Artifact resourceArtifact =
     *     new DefaultArtifact("org.xwiki.platform", "xwiki-platform-tool-configuration-resources",
     *     "jar", "10.7-SNAPSHOT");
     * </code></pre>
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
     * @param artifact the artifact for which to resolve its dependencies (will resolve against the remote and local
     * repositories and ensure that files are available on the local file system for the artifact's dependencies)
     * @param dependentArtifacts additional dependencies for which to also find dependencies in the same request
     * @return the collection of the artifact results for the artifact's dependencies
     * @throws Exception if an error occurred during resolving
     */
    public Collection<ArtifactResult> getArtifactDependencies(Artifact artifact, List<Artifact> dependentArtifacts)
        throws Exception
    {
        // If in cache, serve from the cache for increased performances
        String cacheKey = computeCacheKey(artifact, dependentArtifacts);
        List<ArtifactResult> artifactResults = this.artifactResultCache.get(cacheKey);
        if (artifactResults == null) {
            DependencyFilter filter = new AndDependencyFilter(Arrays.asList(
                // Include compile and runtime scopes only (we don't want provided since it's supposed to be available
                // in the execution's environment).
                new ScopeDependencyFilter(
                    Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME), Collections.emptyList()),
                // - Exclude JCL and LOG4J since we want all logging to go through SLF4J. Note that we're excluding
                // log4j-<version>.jar but keeping log4j-over-slf4j-<version>.jar
                // - Exclude batik-js to prevent conflict with the patched version of Rhino used by yuicompressor used
                // for JSX. See https://jira.xwiki.org/browse/XWIKI-6151 for more details.
                new ExclusionsDependencyFilter(Arrays.asList("org.apache.xmlgraphic:batik-js",
                    "commons-logging:commons-logging", "commons-logging:commons-logging-api", "log4j:log4j"))));

            CollectRequest collectRequest = new CollectRequest()
                .setRoot(new Dependency(artifact, null))
                .setRepositories(this.repositoryResolver.getRepositories());

            if (dependentArtifacts != null && !dependentArtifacts.isEmpty()) {
                for (Artifact dependentArtifact : dependentArtifacts) {
                    // Note: we use a "runtime" scope so that the dependent artifact is included in the result (we
                    // filter on scopes and keep "compile" and "runtime" artifacts).
                    collectRequest.addDependency(new Dependency(dependentArtifact, "runtime"));
                }
            }

            long t1 = System.currentTimeMillis();
            CollectResult collectResult = this.repositoryResolver.getSystem().collectDependencies(
                this.repositoryResolver.getSession(), collectRequest);

            if (!collectResult.getExceptions().isEmpty()) {
                sendError(artifact, collectResult.getExceptions());
            }

            DependencyNode node = collectResult.getRoot();

            if (this.isDebug) {
                LOGGER.info("collect = {} ms", (System.currentTimeMillis() - t1));
                node.accept(new FilteringDependencyVisitor(new DebuggingDependencyVisitor(), filter));
            }

            DependencyRequest request = new DependencyRequest(node, filter);
            t1 = System.currentTimeMillis();
            DependencyResult dependencyResult = this.repositoryResolver.getSystem().resolveDependencies(
                this.repositoryResolver.getSession(), request);
            if (this.isDebug) {
                LOGGER.info("resolve = {} ms", (System.currentTimeMillis() - t1));
            }

            if (!dependencyResult.getCollectExceptions().isEmpty()) {
                sendError(artifact, dependencyResult.getCollectExceptions());
            }
            artifactResults = dependencyResult.getArtifactResults();
            this.artifactResultCache.put(cacheKey, artifactResults);
        }

        return artifactResults;
    }

    /**
     * @param xwikiVersion the version of the artifacts to resolve
     * @param extraArtifacts the list of extra artifacts that should be added to {@code WEB-INF/lib}. Can be empty
     * @return the collection of resolved artifact results for the minimal XWiki distribution/flavor to be used for
     * functional tests
     * @throws Exception if an error occurred during resolving
     */
    public Collection<ArtifactResult> getDistributionDependencies(String xwikiVersion, List<Artifact> extraArtifacts)
        throws Exception
    {
        Artifact rootDistributionArtifact = new DefaultArtifact(PLATFORM_GROUPID,
            "xwiki-platform-minimaldependencies", "pom", xwikiVersion);

        List<Artifact> dependentArtifacts = new ArrayList<>();

        // We provision XAR and JAR extensions (as dependencies of XAR extensions). Thus we need the associated
        // handlers.
        Artifact xarHandlerArtifact = new DefaultArtifact(PLATFORM_GROUPID, "xwiki-platform-extension-handler-xar",
            JAR, xwikiVersion);
        dependentArtifacts.add(xarHandlerArtifact);
        Artifact jarHandlerArtifact = new DefaultArtifact(PLATFORM_GROUPID, "xwiki-platform-extension-handler-jar",
            JAR, xwikiVersion);
        dependentArtifacts.add(jarHandlerArtifact);

        // Since we want to be able to provision SNAPSHOT extensions, we need to configure the SNAPSHOT
        // extension repository. We do that by adding a dependency which will inject it automatically in the
        // default list of extension repositories.
        if (!this.isOffline) {
            Artifact artifact = new DefaultArtifact(COMMONS_GROUPID,
                "xwiki-commons-extension-repository-maven-snapshots", JAR, xwikiVersion);
            dependentArtifacts.add(artifact);
        } else {
            Artifact artifact = new DefaultArtifact(COMMONS_GROUPID,
                "xwiki-commons-extension-repository-maven", JAR, xwikiVersion);
            dependentArtifacts.add(artifact);
        }

        // It seems that Maven Resolver is not able to resolve the ZIP dependencies from
        // xwiki-platform-distribution-war-minimaldependencies for some reason, so we manually ask for resolving the
        // skin dependency (which of ZIP type).
        Artifact skinArtifact = new DefaultArtifact(PLATFORM_GROUPID, "xwiki-platform-flamingo-skin-resources",
            "zip", xwikiVersion);
        dependentArtifacts.add(skinArtifact);

        // We use the ComponentScriptService in TestUtils#setPropertyInXWikiCfg so we need the dependency for our tests.
        Artifact componentScript = new DefaultArtifact(PLATFORM_GROUPID, "xwiki-platform-component-script",
            JAR, xwikiVersion);
        dependentArtifacts.add(componentScript);

        // Add specified extra artifacts
        dependentArtifacts.addAll(extraArtifacts);

        return getArtifactDependencies(rootDistributionArtifact, dependentArtifacts);
    }

    private void sendError(Artifact artifact, Collection<Exception> exceptions) throws Exception
    {
        for (Exception exception : exceptions) {
            LOGGER.error("Artifact resolving error for [{}]", artifact, exception);
        }
        throw new Exception(String.format("Failed to resolve artifact [%s]", artifact));
    }

    private String computeCacheKey(Artifact artifact, List<Artifact> dependentArtifacts)
    {
        StringBuilder builder = new StringBuilder(computeArtifactKey(artifact));
        for (Artifact dependentArtifact : dependentArtifacts) {
            builder.append('-').append(computeArtifactKey(dependentArtifact));
        }
        return builder.toString();
    }

    private String computeArtifactKey(Artifact artifact)
    {
        return String.format("%s:%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(),
            artifact.getVersion());
    }
}
