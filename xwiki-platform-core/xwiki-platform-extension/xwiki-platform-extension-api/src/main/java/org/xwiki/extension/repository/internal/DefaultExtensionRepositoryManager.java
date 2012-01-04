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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryException;
import org.xwiki.extension.repository.ExtensionRepositoryFactory;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.result.AggregatedIterableResult;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.repository.search.Searchable;
import org.xwiki.extension.version.Version;

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

    @Override
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

    @Override
    public void addRepository(ExtensionRepository repository)
    {
        this.repositories.put(repository.getId().getId(), repository);
    }

    @Override
    public void removeRepository(String repositoryId)
    {
        this.repositories.remove(repositoryId);
    }

    @Override
    public ExtensionRepository getRepository(String repositoryId)
    {
        return this.repositories.get(repositoryId);
    }

    @Override
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        for (ExtensionRepository repository : this.repositories.values()) {
            try {
                return repository.resolve(extensionId);
            } catch (ResolveException e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Could not find extension [{}] in repository [{}]", new Object[] {extensionId,
                        repository.getId(), e});
                }
            }
        }

        throw new ResolveException(MessageFormat.format("Could not find extension [{0}]", extensionId));
    }

    @Override
    public Extension resolve(ExtensionDependency extensionDependency) throws ResolveException
    {
        for (ExtensionRepository repository : this.repositories.values()) {
            try {
                return repository.resolve(extensionDependency);
            } catch (ResolveException e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Could not find extension dependency [{}] in repository [{}]", new Object[] {
                        extensionDependency, repository.getId(), e});
                }
            }
        }

        throw new ResolveException(MessageFormat.format("Could not find extension dependency [{0}]",
            extensionDependency));
    }

    @Override
    public IterableResult<Version> resolveVersions(String id, int offset, int nb) throws ResolveException
    {
        for (ExtensionRepository repository : this.repositories.values()) {
            try {
                return repository.resolveVersions(id, offset, nb);
            } catch (ResolveException e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Could not find versions for extension with id [{}]", id, e);
                }
            }
        }

        throw new ResolveException(MessageFormat.format("Could not find versions for extension with id [{0}]", id));
    }

    @Override
    public IterableResult<Extension> search(String pattern, int offset, int nb)
    {
        IterableResult<Extension> searchResult = null;

        int currentOffset = offset > 0 ? offset : 0;
        int currentNb = nb;

        // A local index would avoid things like this...
        for (ExtensionRepository repository : this.repositories.values()) {
            try {
                searchResult = search(repository, pattern, currentOffset, currentNb, searchResult);

                if (searchResult != null) {
                    if (currentOffset > 0) {
                        currentOffset = offset - searchResult.getTotalHits();
                        if (currentOffset < 0) {
                            currentOffset = 0;
                        }
                    }

                    if (currentNb > 0) {
                        currentNb = nb - searchResult.getSize();
                        if (currentNb < 0) {
                            currentNb = 0;
                        }
                    }
                }
            } catch (SearchException e) {
                this.logger.error("Failed to search on repository [{}] with pattern=[{}], offset=[{}] and nb=[{}]."
                    + " Ignore and go to next repository.", new Object[] {repository, pattern, offset, nb, e});
            }
        }

        return searchResult != null ? searchResult : new CollectionIterableResult<Extension>(0, offset,
            Collections.<Extension> emptyList());
    }

    /**
     * Search one repository.
     * 
     * @param repository the repository to search
     * @param pattern the pattern to search
     * @param offset the offset from where to start returning search results
     * @param nb the maximum number of search results to return
     * @param previousSearchResult the current search result merged from all previous repositories
     * @return the updated maximum number of search results to return
     * @throws SearchException error while searching on provided repository
     */
    private IterableResult<Extension> search(ExtensionRepository repository, String pattern, int offset, int nb,
        IterableResult<Extension> previousSearchResult) throws SearchException
    {
        IterableResult<Extension> result;

        if (repository instanceof Searchable) {
            Searchable searchableRepository = (Searchable) repository;

            result = searchableRepository.search(pattern, offset, nb);

            if (previousSearchResult != null) {
                result = appendSearchResults(previousSearchResult, result);
            }
        } else {
            result = previousSearchResult;
        }

        return result;
    }

    /**
     * @param previousSearchResult all the previous search results
     * @param result the new search result to append
     * @return the new aggregated search result
     */
    private AggregatedIterableResult<Extension> appendSearchResults(IterableResult<Extension> previousSearchResult,
        IterableResult<Extension> result)
    {
        AggregatedIterableResult<Extension> newResult;

        if (previousSearchResult instanceof AggregatedIterableResult) {
            newResult = ((AggregatedIterableResult<Extension>) previousSearchResult);
        } else {
            newResult = new AggregatedIterableResult<Extension>(previousSearchResult.getOffset());
            newResult.addSearchResult(previousSearchResult);
        }

        newResult.addSearchResult(result);

        return newResult;
    }
}
