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
package com.xpn.xwiki.internal.cache;

import org.xwiki.cache.CacheException;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

/**
 * Specialized cache component related to documents.
 * 
 * @param <C> the class of the data stored in the cache.
 * @version $Id$
 * @since 2.4M1
 */
@ComponentRole
public interface DocumentCache<C>
{
    /**
     * Initialize the cache.
     * <p>
     * This method should be called before anything else.
     * 
     * @param cacheConfiguration the cache configuration
     * @throws CacheException failed to initialize the cache
     */
    void create(CacheConfiguration cacheConfiguration) throws CacheException;

    /**
     * Get the value associated with the provided key.
     * 
     * @param documentReference the reference of the document
     * @param extensions the extensions to the document reference
     * @return the value
     */
    C get(DocumentReference documentReference, Object... extensions);

    /**
     * Add a new value or overwrite the existing one associated with the provided key.
     * 
     * @param data the data to store
     * @param documentReference the reference of the document
     * @param extensions the extensions to the document reference
     */
    void set(C data, DocumentReference documentReference, Object... extensions);

    /**
     * Remove from the cache the value associated to the provided key elements.
     * 
     * @param data the data to store
     * @param documentReference the reference of the document
     * @param extensions the extensions to the document reference
     */
    void remove(C data, DocumentReference documentReference, Object... extensions);

    /**
     * Remove all the entries the cache contains.
     */
    void removeAll();

    /**
     * Release all the resources this cache use.
     */
    void dispose();
}
