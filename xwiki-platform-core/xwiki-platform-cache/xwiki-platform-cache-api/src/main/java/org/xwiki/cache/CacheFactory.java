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

/**
 * This interface is implemented by a XWiki cache component implementation to provide cache creation. It's the entry
 * point of the cache component.
 * 
 * @version $Id$
 */
@Role
public interface CacheFactory
{
    /**
     * Create and return a custom cache.
     * 
     * @param <T> the class of the data stored in the cache.
     * @param config the cache configuration.
     * @return a new {@link Cache}.
     * @throws CacheException error when creating the cache.
     */
    <T> Cache<T> newCache(CacheConfiguration config) throws CacheException;
}
