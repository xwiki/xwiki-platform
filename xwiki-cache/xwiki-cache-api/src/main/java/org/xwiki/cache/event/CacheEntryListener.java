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
package org.xwiki.cache.event;

import java.util.EventListener;

/**
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 */
public interface CacheEntryListener<T> extends EventListener
{
    /**
     * Called when a new value is inserted in the cache.
     * 
     * @param event the event object containing all entry informations.
     */
    void cacheEntryAdded(CacheEntryEvent<T> event);

    /**
     * Called when an existing value is removed from the cache.
     * <p>
     * The event object contains the former entry.
     * 
     * @param event the event object containing all entry informations.
     */
    void cacheEntryRemoved(CacheEntryEvent<T> event);

    /**
     * Called when an existing value is overwritten in the cache.
     * <p>
     * The event object contains the new value.
     * 
     * @param event the event object containing all entry informations.
     */
    void cacheEntryModified(CacheEntryEvent<T> event);
}
