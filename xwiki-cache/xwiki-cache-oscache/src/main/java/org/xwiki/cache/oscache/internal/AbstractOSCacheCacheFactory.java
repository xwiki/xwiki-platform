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
package org.xwiki.cache.oscache.internal;

import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.container.Container;

/**
 * Base implementation for OSCache support.
 *  
 * @version $Id: $
 * @since 1.9M2
 */
public abstract class AbstractOSCacheCacheFactory extends AbstractLogEnabled implements CacheFactory
{
    /**
     * The container used to access configuration files.
     */
    @Requirement
    private Container container;
    
    /**
     * @return the default configuration identifier used to load cache configuration file
     */
    protected abstract String getDefaultPropsId();
    
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.CacheFactory#newCache(org.xwiki.cache.config.CacheConfiguration)
     */
    public <T> org.xwiki.cache.Cache<T> newCache(CacheConfiguration configuration) throws CacheException
    {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Start OSCache initialisation");
        }

        OSCacheCache<T> cache = new OSCacheCache<T>();
        cache.initialize(new OSCacheCacheConfiguration(this.container, configuration, getDefaultPropsId()));

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("End OSCache initialisation");
        }

        return cache;
    }
}
