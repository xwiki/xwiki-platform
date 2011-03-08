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
package org.xwiki.extension.repository.internal.aether;

import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.LocalArtifactRequest;
import org.sonatype.aether.repository.LocalArtifactResult;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactDescriptorException;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.SubArtifact;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.internal.plexus.PlexusComponentManager;

public class AetherExtensionRepository implements ExtensionRepository
{
    private ExtensionRepositoryId repositoryId;

    private PlexusComponentManager plexusComponentManager;

    private RepositorySystem repositorySystem;

    private RepositorySystemSession session;

    private RemoteRepository remoteRepository;

    public AetherExtensionRepository(ExtensionRepositoryId repositoryId, RepositorySystemSession session,
        PlexusComponentManager mavenComponentManager) throws ComponentLookupException
    {
        this.repositoryId = repositoryId;

        this.plexusComponentManager = mavenComponentManager;

        this.session = session;
        this.repositorySystem = this.plexusComponentManager.getPlexus().lookup(RepositorySystem.class);

        this.remoteRepository = new RemoteRepository(repositoryId.getId(), "default", repositoryId.getURI().toString());
    }

    public ExtensionRepositoryId getId()
    {
        return this.repositoryId;
    }

    public int countExtensions()
    {
        // TODO
        return 0;
    }

    public List<Extension> getExtensions(int nb, int offset)
    {
        // TODO
        return Collections.emptyList();
    }

    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        Artifact artifact = new DefaultArtifact(extensionId.getId() + ':' + extensionId.getVersion());

        Artifact pomArtifact = new SubArtifact(artifact, "", "pom");
        LocalArtifactRequest localArtifactRequest = new LocalArtifactRequest();
        localArtifactRequest.setArtifact(pomArtifact);
        LocalArtifactResult localArtifactResult =
            this.session.getLocalRepositoryManager().find(this.session, localArtifactRequest);
        if (localArtifactResult.getFile() != null) {
            localArtifactResult.getFile().delete();
        }

        ArtifactDescriptorRequest artifactDescriptorRequest = new ArtifactDescriptorRequest();
        artifactDescriptorRequest.setArtifact(artifact);
        artifactDescriptorRequest.addRepository(this.remoteRepository);

        ArtifactDescriptorResult result = resolveArtifact(extensionId);

        if (result.getRepository() instanceof LocalRepository) {
            result.getArtifact().getFile().delete();
            result = resolveArtifact(extensionId);
        }

        List<Exception> extensions = result.getExceptions();

        if (!extensions.isEmpty()) {
            throw new ResolveException("Failed to resolve extension [" + extensionId + "]", extensions.get(0));
        }

        // TODO: get details from the pom.xml file directly using Maven API (use ModelBuilder and ModelResolver)

        try {
            return new AetherExtension(extensionId, result, this, this.plexusComponentManager);
        } catch (ComponentLookupException e) {
            throw new ResolveException("Failed to resolve extension [" + extensionId + "]", e);
        }
    }

    private ArtifactDescriptorResult resolveArtifact(ExtensionId extensionId) throws ResolveException
    {
        Artifact artifact = new DefaultArtifact(extensionId.getId() + ':' + extensionId.getVersion());

        ArtifactDescriptorRequest artifactDescriptorRequest = new ArtifactDescriptorRequest();
        artifactDescriptorRequest.setArtifact(artifact);
        artifactDescriptorRequest.addRepository(this.remoteRepository);

        ArtifactDescriptorResult result;
        try {
            result = this.repositorySystem.readArtifactDescriptor(this.session, artifactDescriptorRequest);
        } catch (ArtifactDescriptorException e) {
            throw new ResolveException("Failed to resolve aether artifact", e);
        }

        return result;
    }

    public boolean exists(ExtensionId extensionId)
    {
        // TODO
        return false;
    }

    public RepositorySystemSession getSession()
    {
        return session;
    }

    public RemoteRepository getRemoteRepository()
    {
        return remoteRepository;
    }
}
