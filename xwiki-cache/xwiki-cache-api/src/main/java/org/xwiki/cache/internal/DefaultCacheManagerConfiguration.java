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
package org.xwiki.cache.internal;

import org.xwiki.cache.CacheManagerConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Default implementation of {@link CacheManagerConfiguration}.
 * 
 * @version $Id$
 */
@Component
public class DefaultCacheManagerConfiguration implements CacheManagerConfiguration
{
    /**
     * Prefix for configuration keys for the Cache module.
     */
    private static final String PREFIX = "cache.";

    /**
     * The default cache implementation.
     */
    private static final String DEFAULT_CACHE_HINT = "jbosscache";

    /**
     * The default local cache implementation.
     */
    private static final String DEFAULT_LOCALCACHE_HINT = "jbosscache/local";

    /**
     * Defines from where to read the rendering configuration data. 
     */
    @Requirement
    private ConfigurationSource configuration;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.CacheManagerConfiguration#getDefaultCache()
     */
    public String getDefaultCache()
    {
        return this.configuration.getProperty(PREFIX + "defaultCache", DEFAULT_CACHE_HINT);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.CacheManagerConfiguration#getDefaultLocalCache()
     */
    public String getDefaultLocalCache()
    {
        return this.configuration.getProperty(PREFIX + "defaultLocalCache", DEFAULT_LOCALCACHE_HINT);
    }
}
