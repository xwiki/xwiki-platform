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

import java.util.Map;
import java.util.Optional;

import org.xwiki.component.annotation.Role;

/**
 * The interface used to store and retrieve live data entries.
 * 
 * @version $Id$
 * @since 12.10
 */
@Role
public interface LiveDataEntryStore
{
    /**
     * @param entryId identifies the entry to return
     * @return the specified entry
     * @throws LiveDataException if retrieving the specified entry fails
     */
    Optional<Map<String, Object>> get(Object entryId) throws LiveDataException;

    /**
     * @param entryId identifies the entry whose property value to return
     * @param property the property whose value to return
     * @return the value of the specified property from the specified live data entry
     * @throws LiveDataException if retrieving the specified property fails
     */
    default Optional<Object> get(Object entryId, String property) throws LiveDataException
    {
        Optional<Map<String, Object>> values = get(entryId);
        if (values.isPresent()) {
            Object value = values.get().get(property);
            if (value != null) {
                return Optional.of(value);
            }
        }

        return Optional.empty();
    }

    /**
     * Executes a query on the live data.
     * 
     * @param query the query used to filter and sort the live data entries
     * @return the live data entries that match the given query
     * @throws LiveDataException if the live data query execution fails
     */
    LiveData get(LiveDataQuery query) throws LiveDataException;

    /**
     * Creates a new entry or updates an existing one.
     * 
     * @param entry the entry to save
     * @return the identifier of the saved entry
     * @throws LiveDataException if saving the given entry fails
     * @since 12.10.4
     * @since 13.0
     */
    default Optional<Object> save(Map<String, Object> entry) throws LiveDataException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Updates a property of a live data entry.
     * 
     * @param entryId the entry to update
     * @param property the property to update
     * @param value the new property value
     * @return the previous property value
     * @throws LiveDataException if updating the specified property fails
     */
    default Optional<Object> update(Object entryId, String property, Object value) throws LiveDataException
    {
        Optional<Map<String, Object>> entry = get(entryId);
        if (entry.isPresent()) {
            Object previousValue = entry.get().put(property, value);
            if (save(entry.get()).isPresent() && previousValue != null) {
                return Optional.of(previousValue);
            }
        }

        return Optional.empty();
    }

    /**
     * Removes an existing entry.
     * 
     * @param entryId identifies the entry to remove
     * @return {@code true} if the entry was removed, {@code false} otherwise
     * @throws LiveDataException if removing the specified entry fails
     */
    default Optional<Map<String, Object>> remove(Object entryId) throws LiveDataException
    {
        throw new UnsupportedOperationException();
    }
}
