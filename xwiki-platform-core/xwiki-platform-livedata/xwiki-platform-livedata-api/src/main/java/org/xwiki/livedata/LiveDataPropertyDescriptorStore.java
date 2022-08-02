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
package org.xwiki.livedata;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.xwiki.component.annotation.Role;

/**
 * The interface used to store and retrieve the property descriptors.
 * 
 * @version $Id$
 * @since 12.10
 */
@Role
public interface LiveDataPropertyDescriptorStore
{
    /**
     * @param propertyId identifies the property whose descriptor to return
     * @return the descriptor of the specified property
     * @throws LiveDataException if retrieving the property descriptor fails
     */
    default Optional<LiveDataPropertyDescriptor> get(String propertyId) throws LiveDataException
    {
        return get().stream().filter(property -> Objects.equals(property.getId(), propertyId)).findFirst();
    }

    /**
     * @return all property descriptors
     * @throws LiveDataException if retrieving the property descriptors fails
     */
    Collection<LiveDataPropertyDescriptor> get() throws LiveDataException;

    /**
     * Adds a new property descriptor or updates an existing one.
     * 
     * @param propertyDescriptor the property descriptor to save
     * @return {@code true} if the property descriptor was saved, {@code false} otherwise
     * @throws LiveDataException if saving the property descriptor fails
     */
    default boolean save(LiveDataPropertyDescriptor propertyDescriptor) throws LiveDataException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the specified property descriptor.
     * 
     * @param propertyId identifies the property whose descriptor to remove
     * @return the removed property descriptor
     * @throws LiveDataException if removing the property descriptor fails
     */
    default Optional<LiveDataPropertyDescriptor> remove(String propertyId) throws LiveDataException
    {
        throw new UnsupportedOperationException();
    }
}
