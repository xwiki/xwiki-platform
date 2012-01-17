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
package org.xwiki.extension.unmodifiable;

import java.util.Collection;

import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.repository.CoreExtensionRepository;

/**
 * Provide a readonly access to a core extension repository.
 * 
 * @param <T>
 * @version $Id$
 */
public class UnmodifiableCoreExtensionRepository<T extends CoreExtensionRepository> extends
    UnmodifiableExtensionRepository<T> implements CoreExtensionRepository
{
    /**
     * @param repository wrapped repository
     */
    public UnmodifiableCoreExtensionRepository(T repository)
    {
        super(repository);
    }

    // CoreExtensionRepository

    @Override
    public int countExtensions()
    {
        return getWrapped().countExtensions();
    }

    @Override
    public Collection<CoreExtension> getCoreExtensions()
    {
        return UnmodifiableUtils.unmodifiableExtensions(getWrapped().getCoreExtensions());
    }

    @Override
    public CoreExtension getCoreExtension(String id)
    {
        return UnmodifiableUtils.unmodifiableExtension(getWrapped().getCoreExtension(id));
    }

    @Override
    public boolean exists(String id)
    {
        return getWrapped().exists(id);
    }
}
