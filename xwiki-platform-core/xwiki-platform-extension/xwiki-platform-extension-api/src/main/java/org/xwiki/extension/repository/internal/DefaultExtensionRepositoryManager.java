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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.logging.AbstractLogEnabled;
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

@Component
@Singleton
public class DefaultExtensionRepositoryManager extends AbstractLogEnabled implements ExtensionRepositoryManager
{
    @Inject
    private ComponentManager componentManager;

    private Map<String, ExtensionRepository> repositories = new ConcurrentHashMap<String, ExtensionRepository>();

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

    public void addRepository(ExtensionRepository repository)
    {
        this.repositories.put(repository.getId().getId(), repository);
    }

    public void removeRepository(String repositoryId)
    {
        this.repositories.remove(repositoryId);
    }

    public ExtensionRepository getRepository(String repositoryId)
    {
        return this.repositories.get(repositoryId);
    }

    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        Extension artifact = null;

        for (ExtensionRepository repository : this.repositories.values()) {
            try {
                artifact = repository.resolve(extensionId);

                return artifact;
            } catch (ResolveException e) {
                getLogger().debug(
                    "Could not find extension [" + extensionId + "] in repository [" + repository.getId() + "]", e);
            }
        }

        throw new ResolveException("Could not find extension [" + extensionId + "]");
    }

    public List<Extension> search(String pattern, int offset, int nb)
    {
        List<Extension> extensions = new ArrayList<Extension>(nb > 0 ? nb : 0);

        // A local index would avoid things like this...
        int currentOffset = 0;
        for (ExtensionRepository repository : this.repositories.values()) {
            int currentNb = nb - extensions.size();
            if (nb > 0 && currentNb == 0) {
                break;
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
                    getLogger().warn("Failed to search in repository [" + this + "]", e);
                }
            }
        }

        return extensions;
    }
}
