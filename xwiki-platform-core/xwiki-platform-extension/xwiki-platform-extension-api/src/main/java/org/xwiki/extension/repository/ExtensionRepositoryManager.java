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
package org.xwiki.extension.repository;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;

/**
 * Proxy behind remote repositories.
 * 
 * @version $Id$
 */
@ComponentRole
public interface ExtensionRepositoryManager
{
    /**
     * Create and add a new repository.
     * 
     * @param repositoryId the repository identifier
     * @return the newly created repository
     * @throws ExtensionRepositoryException failed to create {@link ExtensionRepository} for provided identifier
     */
    ExtensionRepository addRepository(ExtensionRepositoryId repositoryId) throws ExtensionRepositoryException;

    /**
     * @param repository add an existing repository
     */
    void addRepository(ExtensionRepository repository);

    /**
     * Remove a repository form the list.
     * 
     * @param repositoryId the repository unique identifier
     * @see ExtensionRepository#getId()
     */
    void removeRepository(String repositoryId);

    /**
     * @param repositoryId the repository unique identifier
     * @return the repository, null if none could be found
     * @see ExtensionRepository#getId()
     */
    ExtensionRepository getRepository(String repositoryId);

    /**
     * Get extension descriptor found in one of the repositories.
     * <p>
     * The proxy search in all repositories and return the first extension it could find.
     * 
     * @param extensionId the extension identifier
     * @return the found extension descriptor
     * @throws ResolveException failed to find extension in the repository
     */
    Extension resolve(ExtensionId extensionId) throws ResolveException;

    /**
     * Search among all repository implementing {@link Searchable} interface.
     * 
     * @param pattern the pattern to search
     * @param offset the offset from where to start returning search results
     * @param nb the maximum number of search results to return
     * @return the found extensions descriptors, empty list if nothing could be found
     * @see Searchable
     */
    List<Extension> search(String pattern, int offset, int nb);
}
