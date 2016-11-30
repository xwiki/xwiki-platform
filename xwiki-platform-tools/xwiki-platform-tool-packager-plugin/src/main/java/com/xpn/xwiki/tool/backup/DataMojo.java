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
package com.xpn.xwiki.tool.backup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.repository.RepositorySystem;
import org.hibernate.cfg.Environment;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.tool.utils.LogUtils;

import com.xpn.xwiki.XWikiContext;

/**
 * Maven 2 plugin to generate XWiki data folder (database and extensions).
 *
 * @version $Id$
 * @since 9.0RC1
 * @since 8.4.2
 */
@Mojo(name = "data", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true)
public class DataMojo extends AbstractImportMojo
{
    public static class ExtensionArtifact
    {
        private String groupId;

        private String artifactId;

        private String version;

        private String type;

        public String getGroupId()
        {
            return groupId;
        }

        public void setGrouId(String grouId)
        {
            this.groupId = grouId;
        }

        public String getArtifactId()
        {
            return artifactId;
        }

        public void setArtifactId(String artifactId)
        {
            this.artifactId = artifactId;
        }

        public String getVersion()
        {
            return version;
        }

        public void setVersion(String version)
        {
            this.version = version;
        }

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }
    }

    /**
     * The list of artifacts (and their dependencies) to install.
     */
    @Parameter
    private List<ExtensionArtifact> includes;

    /**
     * The list of artifacts (and their dependencies) to exclude from the included artifacts.
     */
    @Parameter
    private List<ExtensionArtifact> excludes;

    /**
     * Local repository to be used by the plugin to resolve dependencies.
     */
    @Parameter(property = "localRepository")
    private ArtifactRepository localRepository;

    /**
     * List of remote repositories to be used by the plugin to resolve dependencies.
     */
    @Parameter(property = "project.remoteArtifactRepositories")
    private List<ArtifactRepository> remoteRepositories;

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    private RepositorySystem repositorySystem;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        LogUtils.configureXWikiLogs();
        System.setProperty("xwiki.data.dir", this.xwikiDataDir.getAbsolutePath());
        // If the package mojo was executed before, it might have left a different database connection URL in the
        // environment, which apparently overrides the value in the configuration file
        System.clearProperty(Environment.URL);

        try {
            install();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to install extensions", e);
        }
    }

    private void install() throws Exception
    {
        Importer importer = new Importer();
        XWikiContext xcontext = importer.createXWikiContext(this.databaseName, this.hibernateConfig);

        // We need to know which JAR extension we don't want to install (usually those that are already part of the WAR)
        Set<String> excludedIds = getExcludedIds();

        // Reverse artifact order to have dependencies first (despite the fact that it's a Set it's actually an ordered
        // LinkedHashSet behind the scene)
        Set<Artifact> artifacts = resolve(this.includes);
        List<Artifact> includedArtifacts = new ArrayList<>(artifacts);
        Collections.reverse(includedArtifacts);

        for (Artifact artifact : includedArtifacts) {
            if (!artifact.isOptional()) {
                switch (artifact.getType()) {
                    case "xar":
                        installXAR(artifact, importer, xcontext);
                        break;
                    case "jar":
                    case "webjar":
                    case "bundle":
                        installJAR(artifact, excludedIds, xcontext);
                        break;
                    default:
                        break;
                }
            }
        }

        // We MUST shutdown HSQLDB because otherwise the last transactions will not be flushed
        // to disk and will be lost. In practice this means the last Document imported has a
        // very high chance of not making it...
        // TODO: Find a way to implement this generically for all databases and inside
        // XWikiHibernateStore (cf http://jira.xwiki.org/jira/browse/XWIKI-471).
        importer.shutdownHSQLDB(xcontext);

        importer.disposeXWikiContext(xcontext);
    }

    private Set<Artifact> resolve(List<ExtensionArtifact> input) throws MojoExecutionException
    {
        if (input != null) {
            Set<Artifact> artifacts = new LinkedHashSet<>(input.size());
            for (ExtensionArtifact extensionArtifact : input) {
                artifacts.add(this.repositorySystem.createArtifact(extensionArtifact.getGroupId(),
                    extensionArtifact.getArtifactId(), extensionArtifact.getVersion(), null,
                    extensionArtifact.getType()));
            }

            ArtifactResolutionRequest request = new ArtifactResolutionRequest().setArtifact(this.project.getArtifact())
                .setRemoteRepositories(this.remoteRepositories).setArtifactDependencies(artifacts)
                .setLocalRepository(this.localRepository).setManagedVersionMap(this.project.getManagedVersionMap())
                .setResolveRoot(false);
            ArtifactResolutionResult resolutionResult = this.repositorySystem.resolve(request);
            if (resolutionResult.hasExceptions()) {
                throw new MojoExecutionException(
                    String.format("Failed to resolve artifacts [%s]", input, resolutionResult.getExceptions().get(0)));
            }

            return resolutionResult.getArtifacts();
        }

        return null;
    }

    private Set<String> getExcludedIds() throws MojoExecutionException
    {
        Set<Artifact> excludedArifacts = resolve(this.excludes);

        if (excludedArifacts != null) {
            Set<String> ids = new HashSet<>(excludedArifacts.size());
            for (Artifact artifact : excludedArifacts) {
                ids.add(artifact.getGroupId() + ':' + artifact.getArtifactId());
            }

            return ids;
        }

        return null;
    }

    private void installJAR(Artifact artifact, Set<String> excludedJARs, XWikiContext xcontext) throws Exception
    {
        if (!excludedJARs.contains(artifact.getGroupId() + ':' + artifact.getArtifactId())) {
            // Install extension
            installExtension(artifact, (ComponentManager) xcontext.get(ComponentManager.class.getName()), null);
        }
    }
}
