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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.aether.internal.plexus.PlexusComponentManager;

public class AetherExtensionRepository implements ExtensionRepository
{
    private ExtensionRepositoryId repositoryId;

    private PlexusComponentManager plexusComponentManager;

    private RepositorySystemSession session;

    private RemoteRepository remoteRepository;

    private ArtifactDescriptorReader artifactDescriptorReader;

    private Method loadPomMethod;

    public AetherExtensionRepository(ExtensionRepositoryId repositoryId, RepositorySystemSession session,
        PlexusComponentManager mavenComponentManager) throws Exception
    {
        this.repositoryId = repositoryId;

        this.plexusComponentManager = mavenComponentManager;

        this.session = session;

        this.artifactDescriptorReader = this.plexusComponentManager.getPlexus().lookup(ArtifactDescriptorReader.class);

        this.remoteRepository = new RemoteRepository(repositoryId.getId(), "default", repositoryId.getURI().toString());

        // FIXME: not very nice
        // * use a private method of a library we don't control is not the nicest thing...
        // * having to parse the pom.xml since we are supposed to support anything supported by aether is not very clean
        // either
        this.loadPomMethod =
            this.artifactDescriptorReader.getClass().getDeclaredMethod("loadPom", RepositorySystemSession.class,
                ArtifactDescriptorRequest.class, ArtifactDescriptorResult.class);
        this.loadPomMethod.setAccessible(true);
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
        Model model;
        try {
            model = loadPom(this.session, extensionId);
        } catch (Exception e) {
            throw new ResolveException("Failed to resolve extension [" + extensionId + "] descriptor", e);
        }

        try {
            return new AetherExtension(extensionId, model, this, this.plexusComponentManager);
        } catch (ComponentLookupException e) {
            throw new ResolveException("Failed to resolve extension [" + extensionId + "]", e);
        }
    }

    private Model loadPom(RepositorySystemSession session, ExtensionId extensionId) throws IllegalArgumentException,
        IllegalAccessException, InvocationTargetException
    {
        Artifact artifact = new DefaultArtifact(extensionId.getId() + ':' + extensionId.getVersion());

        ArtifactDescriptorRequest artifactDescriptorRequest = new ArtifactDescriptorRequest();
        artifactDescriptorRequest.setArtifact(artifact);
        artifactDescriptorRequest.addRepository(this.remoteRepository);

        ArtifactDescriptorResult artifactDescriptorResult = new ArtifactDescriptorResult(artifactDescriptorRequest);

        return (Model) this.loadPomMethod.invoke(this.artifactDescriptorReader, this.session,
            artifactDescriptorRequest, artifactDescriptorResult);
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
