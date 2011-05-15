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
package org.xwiki.extension.repository.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryException;
import org.xwiki.extension.repository.ExtensionRepositoryFactory;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.SearchException;
import org.xwiki.extension.repository.Searchable;

/**
 * Default implementation of {@link ExtensionRepositoryManager}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultExtensionRepositoryManager implements ExtensionRepositoryManager
{
    /**
     * Used to lookup {@link ExtensionRepositoryFactory}s.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The registered repositories.
     */
    private Map<String, ExtensionRepository> repositories = new ConcurrentHashMap<String, ExtensionRepository>();

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepositoryManager#addRepository(org.xwiki.extension.repository.ExtensionRepositoryId)
     */
    public ExtensionRepository addRepository(ExtensionRepositoryId repositoryId) throws ExtensionRepositoryException
    {
        ExtensionRepository repository;

        try {
            ExtensionRepositoryFactory repositoryFactory =
                this.componentManager.lookup(ExtensionRepositoryFactory.class, repositoryId.getType());

            repository = repositoryFactory.createRepository(repositoryId);

            addRepository(repository);
        } catch (ComponentLookupException e) {
            throw new ExtensionRepositoryException("Unsupported repository type[" + repositoryId.getType() + "]", e);
        }

        return repository;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepositoryManager#addRepository(org.xwiki.extension.repository.ExtensionRepository)
     */
    public void addRepository(ExtensionRepository repository)
    {
        this.repositories.put(repository.getId().getId(), repository);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepositoryManager#removeRepository(java.lang.String)
     */
    public void removeRepository(String repositoryId)
    {
        this.repositories.remove(repositoryId);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepositoryManager#getRepository(java.lang.String)
     */
    public ExtensionRepository getRepository(String repositoryId)
    {
        return this.repositories.get(repositoryId);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepositoryManager#resolve(org.xwiki.extension.ExtensionId)
     */
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        Extension artifact = null;

        for (ExtensionRepository repository : this.repositories.values()) {
            try {
                artifact = repository.resolve(extensionId);

                return artifact;
            } catch (ResolveException e) {
                this.logger.debug(
                    MessageFormat.format("Could not find extension [{0}] in repository [{1}]", extensionId,
                        repository.getId()), e);
            }
        }

        throw new ResolveException(MessageFormat.format("Could not find extension [{0}]", extensionId));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepositoryManager#search(java.lang.String, int, int)
     */
    public List<Extension> search(String pattern, int offset, int nb)
    {
        List<Extension> extensions = new ArrayList<Extension>(nb > 0 ? nb : 0);

        // A local index would avoid things like this...
        int currentOffset = 0;
        for (ExtensionRepository repository : this.repositories.values()) {
            currentOffset = search(extensions, repository, pattern, offset, nb, currentOffset);
        }

        return extensions;
    }

    /**
     * Search one repository.
     * 
     * @param extensions the extensions
     * @param repository the repository to search
     * @param pattern the pattern to search
     * @param offset the offset from where to start returning search results
     * @param nb the maximum number of search results to return
     * @param previousCurrentOffset the current offset from where to start returning search results
     * @return the updated maximum number of search results to return
     */
    private int search(List<Extension> extensions, ExtensionRepository repository, String pattern, int offset, int nb,
        int previousCurrentOffset)
    {
        int currentOffset = previousCurrentOffset;
        int currentNb = nb - extensions.size();

        if (nb > 0 && currentNb == 0) {
            return currentOffset;
        }

        if (repository instanceof Searchable) {
            Searchable searchableRepository = (Searchable) repository;

            List<Extension> foundExtensions;
            try {
                foundExtensions = searchableRepository.search(pattern, 0, offset == 0 ? currentNb : -1);

                if (!foundExtensions.isEmpty()) {
                    if (offset - currentOffset >= foundExtensions.size()) {
                        currentOffset += foundExtensions.size();
                    } else {
                        int fromIndex = offset - currentOffset;
                        int toIndex = fromIndex + currentNb;
                        extensions.addAll(foundExtensions.subList(fromIndex,
                            (toIndex <= 0 || toIndex > foundExtensions.size()) ? foundExtensions.size() : toIndex));
                        currentOffset = offset;
                    }
                }
            } catch (SearchException e) {
                this.logger.warn("Failed to search in repository [" + this + "]", e);
            }
        }

        return currentOffset;
    }
}
