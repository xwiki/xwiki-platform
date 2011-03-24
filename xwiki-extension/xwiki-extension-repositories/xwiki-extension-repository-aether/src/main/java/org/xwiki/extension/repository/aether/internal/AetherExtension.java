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
package org.xwiki.extension.repository.aether.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.aether.internal.plexus.PlexusComponentManager;

public class AetherExtension implements Extension
{
    private PlexusComponentManager plexusComponentManager;

    private AetherExtensionRepository repository;

    private ExtensionId id;

    private List<ExtensionDependency> dependencies;

    private List<ExtensionId> suggested;

    private RepositorySystem repositorySystem;

    private Model mavenModel;

    public AetherExtension(ExtensionId id, Model mavenModel, AetherExtensionRepository repository,
        PlexusComponentManager mavenComponentManager) throws ComponentLookupException
    {
        this.plexusComponentManager = mavenComponentManager;

        this.repository = repository;

        this.id = id;
        this.mavenModel = mavenModel;

        this.repositorySystem = this.plexusComponentManager.getPlexus().lookup(RepositorySystem.class);
    }

    public ExtensionId getId()
    {
        return this.id;
    }

    public String getAuthor()
    {
        // TODO
        return null;
    }

    public String getDescription()
    {
        return this.mavenModel.getDescription();
    }

    public String getWebSite()
    {
        return this.mavenModel.getUrl();
    }

    public String getType()
    {
        return this.mavenModel.getPackaging();
    }

    public List<ExtensionDependency> getDependencies()
    {
        if (this.dependencies == null) {
            this.dependencies = new ArrayList<ExtensionDependency>();

            for (Dependency mavenDependency : this.mavenModel.getDependencies()) {
                // XXX: not sure what to do about "provided"
                if (!mavenDependency.isOptional()
                    && (mavenDependency.getScope().equals("compile") || mavenDependency.getScope().equals("runtime") || mavenDependency
                        .getScope().equals("provided"))) {
                    this.dependencies.add(new AetherExtensionDependency(new ExtensionId(mavenDependency.getGroupId()
                        + ":" + mavenDependency.getArtifactId(), mavenDependency.getVersion())));
                }
            }
        }

        return this.dependencies;
    }

    // IDEA
    public List<ExtensionId> getSuggestedExtensions()
    {
        if (this.suggested == null) {
            this.suggested = new ArrayList<ExtensionId>();

            for (Dependency mavenDependency : this.mavenModel.getDependencies()) {
                if (mavenDependency.isOptional()) {
                    this.suggested.add(new ExtensionId(mavenDependency.getGroupId() + ":"
                        + mavenDependency.getArtifactId(), mavenDependency.getVersion()));
                }
            }
        }

        return this.suggested;
    }

    public void download(File file) throws ExtensionException
    {
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.addRepository(this.repository.getRemoteRepository());
        artifactRequest.setArtifact(new DefaultArtifact(this.mavenModel.getGroupId(), this.mavenModel.getArtifactId(),
            getType(), this.mavenModel.getVersion()));

        ArtifactResult artifactResult;
        try {
            artifactResult = this.repositorySystem.resolveArtifact(this.repository.getSession(), artifactRequest);
        } catch (ArtifactResolutionException e) {
            throw new ExtensionException("Failed to resolve artifact", e);
        }

        File aetherFile = artifactResult.getArtifact().getFile();

        try {
            FileUtils.copyFile(aetherFile, file);
        } catch (IOException e) {
            new ExtensionException("Failed to copy file", e);
        }
    }

    public ExtensionRepository getRepository()
    {
        return this.repository;
    }
}
