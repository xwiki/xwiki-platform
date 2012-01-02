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
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.version.Version;

/**
 * Wrap an {@link ExtensionRepository}.
 * 
 * @param <T>
 * @version $Id$
 */
public class WrappingExtensionRepository<T extends ExtensionRepository> extends AbstractWrappingObject<T> implements
    ExtensionRepository
{
    /**
     * @param repository the wrapped repository
     */
    public WrappingExtensionRepository(T repository)
    {
        super(repository);
    }

    // ExtensionRepository

    @Override
    public ExtensionRepositoryId getId()
    {
        return getWrapped().getId();
    }

    @Override
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        return getWrapped().resolve(extensionId);
    }

    @Override
    public Extension resolve(ExtensionDependency extensionDependency) throws ResolveException
    {
        return getWrapped().resolve(extensionDependency);
    }

    @Override
    public boolean exists(ExtensionId extensionId)
    {
        return getWrapped().exists(extensionId);
    }

    @Override
    public IterableResult<Version> resolveVersions(String id, int offset, int nb) throws ResolveException
    {
        return getWrapped().resolveVersions(id, offset, nb);
    }
}
