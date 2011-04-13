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
                    "Could not find extension [" + extensionId + "] in repository [" + repository.getId() + "]");
            }
        }

        throw new ResolveException("Could not find extension [" + extensionId + "]");
    }
}
