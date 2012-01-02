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

import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;

/**
 * Base class for {@link ExtensionRepository} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractExtensionRepository implements ExtensionRepository
{
    /**
     * The repository identifier.
     */
    private ExtensionRepositoryId id;

    /**
     * Default constructor. Used by extended classes which can't set the id in there constructor but make sure it's set
     * later or that {@link #getId()} is overwritten.
     */
    protected AbstractExtensionRepository()
    {

    }

    /**
     * @param id the repository identifier
     */
    protected AbstractExtensionRepository(ExtensionRepositoryId id)
    {
        setId(new ExtensionRepositoryId(id));
    }

    /**
     * @param id the repository identifier
     */
    public void setId(ExtensionRepositoryId id)
    {
        this.id = id;
    }

    // ExtensionRepository

    @Override
    public ExtensionRepositoryId getId()
    {
        return this.id;
    }

    @Override
    public boolean exists(ExtensionId extensionId)
    {
        boolean exists;

        try {
            resolve(extensionId);
            exists = true;
        } catch (ResolveException e) {
            exists = false;
        }

        return exists;
    }
}
