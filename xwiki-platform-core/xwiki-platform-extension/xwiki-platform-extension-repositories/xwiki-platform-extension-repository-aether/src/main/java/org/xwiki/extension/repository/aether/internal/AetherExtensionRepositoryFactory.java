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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryException;
import org.xwiki.extension.repository.ExtensionRepositoryFactory;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.aether.internal.configuration.AetherConfiguration;
import org.xwiki.extension.repository.aether.internal.plexus.PlexusComponentManager;

@Component
@Singleton
@Named("maven")
public class AetherExtensionRepositoryFactory implements ExtensionRepositoryFactory,
    Initializable
{
    @Inject
    private PlexusComponentManager aetherComponentManager;

    @Inject
    private AetherConfiguration aetherConfiguration;

    private DefaultRepositorySystemSession session;

    public void initialize() throws InitializationException
    {
        RepositorySystem repositorySystem;
        try {
            repositorySystem = this.aetherComponentManager.getPlexus().lookup(RepositorySystem.class);
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to lookup RepositorySystem", e);
        }

        this.session = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository(this.aetherConfiguration.getLocalRepository());
        this.session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepo));
        this.session.setIgnoreMissingArtifactDescriptor(false);
        this.session.setIgnoreInvalidArtifactDescriptor(false);
    }

    public ExtensionRepository createRepository(ExtensionRepositoryId repositoryId) throws ExtensionRepositoryException
    {
        try {
            return new AetherExtensionRepository(repositoryId, this.session, this.aetherComponentManager);
        } catch (Exception e) {
            throw new ExtensionRepositoryException("Failed to create repository [" + repositoryId + "]", e);
        }
    }
}
