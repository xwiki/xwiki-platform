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
package org.xwiki.extension.wrap;

import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;

/**
 * Wrap an {@link ExtensionRepository}.
 * 
 * @param <T>
 * @version $Id$
 */
public class WrappingExtensionRepository<T extends ExtensionRepository> implements ExtensionRepository
{
    /**
     * @see #getRepository()
     */
    private T repository;

    /**
     * @param repository the wrapped repository
     */
    public WrappingExtensionRepository(T repository)
    {
        this.repository = repository;
    }

    /**
     * @return the wrapped repository
     */
    protected T getRepository()
    {
        return this.repository;
    }

    // ExtensionRepository

    @Override
    public ExtensionRepositoryId getId()
    {
        return getRepository().getId();
    }

    @Override
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        return getRepository().resolve(extensionId);
    }

    @Override
    public Extension resolve(ExtensionDependency extensionDependency) throws ResolveException
    {
        return getRepository().resolve(extensionDependency);
    }

    @Override
    public boolean exists(ExtensionId extensionId)
    {
        return getRepository().exists(extensionId);
    }
}
