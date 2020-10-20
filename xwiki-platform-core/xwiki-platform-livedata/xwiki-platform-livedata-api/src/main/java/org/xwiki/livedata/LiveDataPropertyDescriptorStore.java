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
import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * The interface used to store and retrieve the property descriptors.
 * 
 * @version $Id$
 * @since 12.9
 */
@Role
@Unstable
public interface LiveDataPropertyDescriptorStore
{
    /**
     * Adds a new property descriptor.
     * 
     * @param propertyDescriptor the property descriptor to add
     * @return {@code true} if the property descriptor was added, {@code false} otherwise
     * @throws LiveDataException if adding the property descriptor fails
     */
    boolean add(LiveDataPropertyDescriptor propertyDescriptor) throws LiveDataException;

    /**
     * @param propertyId identifies the property whose descriptor to return
     * @return the descriptor of the specified property
     * @throws LiveDataException if retrieving the property descriptor fails
     */
    Optional<LiveDataPropertyDescriptor> get(String propertyId) throws LiveDataException;

    /**
     * @return all property descriptors
     * @throws LiveDataException if retrieving the property descriptors fails
     */
    Collection<LiveDataPropertyDescriptor> get() throws LiveDataException;

    /**
     * @param propertyDescriptor the property descriptor to update
     * @return {@code true} if the property descriptor was updated, {@code false} otherwise
     * @throws LiveDataException if updating the property descriptor fails
     */
    boolean update(LiveDataPropertyDescriptor propertyDescriptor) throws LiveDataException;

    /**
     * Removes the specified property descriptor.
     * 
     * @param propertyId identifies the property whose descriptor to remove
     * @return the removed property descriptor
     * @throws LiveDataException if removing the property descriptor fails
     */
    Optional<LiveDataPropertyDescriptor> remove(String propertyId) throws LiveDataException;
}
