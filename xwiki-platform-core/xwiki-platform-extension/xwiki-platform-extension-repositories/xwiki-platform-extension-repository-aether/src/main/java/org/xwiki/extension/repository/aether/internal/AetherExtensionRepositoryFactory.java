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
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
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
public class AetherExtensionRepositoryFactory implements ExtensionRepositoryFactory, Initializable
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private PlexusComponentManager plexusComponentManager;

    @Inject
    private AetherConfiguration aetherConfiguration;

    private LocalRepositoryManager localRepositoryManager;

    @Override
    public void initialize() throws InitializationException
    {
        RepositorySystem repositorySystem;
        try {
            repositorySystem = this.plexusComponentManager.getPlexus().lookup(RepositorySystem.class);
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to lookup RepositorySystem", e);
        }

        LocalRepository localRepo = new LocalRepository(this.aetherConfiguration.getLocalRepository());
        this.localRepositoryManager = repositorySystem.newLocalRepositoryManager(localRepo);
    }

    public RepositorySystemSession createRepositorySystemSession()
    {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        session.setLocalRepositoryManager(this.localRepositoryManager);
        session.setIgnoreMissingArtifactDescriptor(false);
        session.setIgnoreInvalidArtifactDescriptor(false);
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);

        return session;
    }

    @Override
    public ExtensionRepository createRepository(ExtensionRepositoryId repositoryId) throws ExtensionRepositoryException
    {
        try {
            return new AetherExtensionRepository(repositoryId, this, this.plexusComponentManager, this.componentManager);
        } catch (Exception e) {
            throw new ExtensionRepositoryException("Failed to create repository [" + repositoryId + "]", e);
        }
    }
}
