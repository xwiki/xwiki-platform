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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
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
 * Default implementation of {@link ExtensionManager}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultExtensionManager implements ExtensionManager, Initializable
{
    /**
     * Used to manipulate remote repositories.
     */
    @Inject
    private ExtensionRepositoryManager repositoryManager;

    /**
     * Used to initialize {@link #repositoryManager}.
     */
    @Inject
    private List<ExtensionRepositorySource> repositoriesSources;

    /**
     * Used to manipulate core extensions.
     */
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    /**
     * Used to manipulate local extensions.
     */
    @Inject
    private LocalExtensionRepository localExtensionRepository;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        // Load extension repositories
        for (ExtensionRepositorySource repositoriesSource : this.repositoriesSources) {
            for (ExtensionRepositoryId repositoryId : repositoriesSource.getExtensionRepositories()) {
                try {
                    this.repositoryManager.addRepository(repositoryId);
                } catch (ExtensionRepositoryException e) {
                    this.logger.error("Failed to add repository [" + repositoryId + "]", e);
                }
            }
        }
    }

    @Override
    public Extension resolveExtension(ExtensionId extensionId) throws ResolveException
    {
        Extension extension = null;

        extension = this.coreExtensionRepository.getCoreExtension(extensionId.getId());

        if (extension == null) {
            try {
                extension = this.localExtensionRepository.resolve(extensionId);
            } catch (ResolveException e) {
                extension = this.repositoryManager.resolve(extensionId);
            }
        }

        return extension;
    }

    @Override
    public Extension resolveExtension(ExtensionDependency extensionDependency) throws ResolveException
    {
        Extension extension = null;

        String initialId = extensionDependency.getId();

        extension = this.coreExtensionRepository.getCoreExtension(initialId);

        if (extension == null) {
            try {
                extension = this.localExtensionRepository.resolve(extensionDependency);
            } catch (ResolveException e) {
                extension = this.repositoryManager.resolve(extensionDependency);
            }
        }

        return extension;
    }
}
