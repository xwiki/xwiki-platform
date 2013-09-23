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
package org.xwiki.cache;

import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Role;
import org.xwiki.component.manager.ComponentLookupException;

/**
 * Choose the cache component to use for cache or local cache.
 * 
 * @version $Id$
 * @since 1.7M1
 */
@Role
public interface CacheManager
{
    /**
     * Lookup the cache creation component with provided hint return it.
     * 
     * @return the cache creation service to use.
     * @throws ComponentLookupException error when searching for cache component.
     */
    CacheFactory getCacheFactory() throws ComponentLookupException;

    /**
     * Lookup the local cache creation component with provided hint return it.
     * 
     * @return the local cache creation service to use.
     * @throws ComponentLookupException error when searching for cache component.
     */
    CacheFactory getLocalCacheFactory() throws ComponentLookupException;

    /**
     * Find the cache implementation component to use and create a new cache.
     * 
     * @param <T> the class of the data stored in the cache.
     * @param config the cache configuration.
     * @return a new {@link Cache}.
     * @throws CacheException error when creating the cache.
     */
    <T> Cache<T> createNewCache(CacheConfiguration config) throws CacheException;

    /**
     * Find the local cache implementation component to use and create a new cache.
     * <p>
     * The new cache is based on a local storing implementation (to oppose to distributed cache) and is generally used
     * for not big cache needing very quick access to datas.
     * 
     * @param <T> the class of the data stored in the cache.
     * @param config the cache configuration.
     * @return a new local {@link Cache}.
     * @throws CacheException error when creating the cache.
     */
    <T> Cache<T> createNewLocalCache(CacheConfiguration config) throws CacheException;
}
