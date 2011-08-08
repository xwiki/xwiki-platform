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
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Model;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.aether.internal.plexus.PlexusComponentManager;
import org.xwiki.properties.ConverterManager;

public class AetherExtension extends AbstractExtension
{
    private String PKEY_GROUPID = "aether.groupid";

    private String PKEY_ARTIFACTID = "aether.artifactid";

    private String MPKEYPREFIX = "xwiki.extension.";

    private String MPKEY_FEATURES = MPKEYPREFIX + "features";

    private PlexusComponentManager plexusComponentManager;

    private List<ExtensionId> suggested;

    private Model mavenModel;

    public AetherExtension(ExtensionId id, Model mavenModel, AetherExtensionRepository repository,
        PlexusComponentManager mavenComponentManager, ConverterManager converter)
    {
        super(repository, id, mavenModel.getPackaging());

        this.plexusComponentManager = mavenComponentManager;
        this.mavenModel = mavenModel;

        setName(this.mavenModel.getName());
        setDescription(this.mavenModel.getDescription());
        for (Developer developer : this.mavenModel.getDevelopers()) {
            addAuthor(developer.getId());
        }
        setWebsite(this.mavenModel.getUrl());

        // features
        String featuresString = this.mavenModel.getProperties().getProperty(MPKEY_FEATURES);
        if (StringUtils.isNotBlank(featuresString)) {
            setFeatures(converter.<Collection<String>> convert(List.class, featuresString));
        }

        // TODO: parse features list

        // dependencies
        for (Dependency mavenDependency : this.mavenModel.getDependencies()) {
            if (!mavenDependency.isOptional()
                && (mavenDependency.getScope().equals("compile") || mavenDependency.getScope().equals("runtime"))) {
                addDependency(new AetherExtensionDependency(new ExtensionId(mavenDependency.getGroupId() + ":"
                    + mavenDependency.getArtifactId(), mavenDependency.getVersion())));
            }
        }

        // custom properties
        putProperty(PKEY_GROUPID, this.mavenModel.getGroupId());
        putProperty(PKEY_ARTIFACTID, this.mavenModel.getArtifactId());
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#download(java.io.File)
     */
    public void download(File file) throws ExtensionException
    {
        RepositorySystem repositorySystem;
        try {
            repositorySystem = this.plexusComponentManager.getPlexus().lookup(RepositorySystem.class);
        } catch (ComponentLookupException e) {
            throw new ExtensionException("Failed to get org.sonatype.aether.RepositorySystem component", e);
        }

        AetherExtensionRepository aetherRepository = (AetherExtensionRepository) getRepository();

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.addRepository(aetherRepository.getRemoteRepository());
        artifactRequest.setArtifact(new DefaultArtifact(this.mavenModel.getGroupId(), this.mavenModel.getArtifactId(),
            getType(), this.mavenModel.getVersion()));

        ArtifactResult artifactResult;
        try {
            artifactResult = repositorySystem.resolveArtifact(aetherRepository.getSession(), artifactRequest);
        } catch (ArtifactResolutionException e) {
            throw new ExtensionException("Failed to resolve artifact", e);
        }

        File aetherFile = artifactResult.getArtifact().getFile();

        try {
            FileUtils.copyFile(aetherFile, file);
        } catch (IOException e) {
            throw new ExtensionException("Failed to copy file", e);
        }
    }
}
