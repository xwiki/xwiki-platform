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
package org.xwiki.extension.internal;

import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryException;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.ExtensionRepositorySource;
import org.xwiki.extension.repository.LocalExtensionRepository;

/**
 * TODO: cut installation process in steps (create and validate an install plan, install, etc.)
 */
@Component
public class DefaultExtensionManager extends AbstractLogEnabled implements ExtensionManager, Initializable
{
    @Requirement
    private ExtensionRepositoryManager repositoryManager;

    @Requirement
    private List<ExtensionRepositorySource> repositoriesSources;

    @Requirement
    private CoreExtensionRepository coreExtensionRepository;

    @Requirement
    private LocalExtensionRepository localExtensionRepository;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // Load extension repositories
        for (ExtensionRepositorySource repositoriesSource : this.repositoriesSources) {
            for (ExtensionRepositoryId repositoryId : repositoriesSource.getExtensionRepositories()) {
                try {
                    this.repositoryManager.addRepository(repositoryId);
                } catch (ExtensionRepositoryException e) {
                    getLogger().error("Failed to add repository [" + repositoryId + "]", e);
                }
            }
        }
    }

    public Extension resolveExtension(ExtensionId extensionId, String namespace) throws ResolveException
    {
        Extension extension = null;

        extension = this.coreExtensionRepository.getCoreExtension(extensionId.getId());

        if (extension == null) {
            extension = this.localExtensionRepository.getInstalledExtension(extensionId.getId(), namespace);

            if (extension == null || !extension.getId().getVersion().equals(extensionId.getVersion())) {
                extension = this.repositoryManager.resolve(extensionId);
            }
        }

        return extension;
    }
}
