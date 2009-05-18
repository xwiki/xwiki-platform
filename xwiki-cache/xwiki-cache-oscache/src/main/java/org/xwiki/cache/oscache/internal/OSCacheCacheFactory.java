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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.container.Container;

/**
 * Implements {@link CacheFactory} based on OSCache.
 * 
 * @version $Id$
 */
public class OSCacheCacheFactory implements CacheFactory
{
    /**
     * This component's role hint, used when code needs to look it up.
     */
    public static final String ROLEHINT = "oscache";

    /**
     * The logging tool.
     */
    private static final Log LOG = LogFactory.getLog(OSCacheCacheFactory.class);

    /**
     * The container used to access configuration files.
     */
    private Container container;

    /**
     * The default configuration identifier used to load cache configuration file.
     */
    private String defaultPropsId;

    /**
     * Default constructor.
     */
    public OSCacheCacheFactory()
    {
        if (this.defaultPropsId == null) {
            this.defaultPropsId = "default";
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.CacheFactory#newCache(org.xwiki.cache.config.CacheConfiguration)
     */
    public <T> org.xwiki.cache.Cache<T> newCache(CacheConfiguration configuration) throws CacheException
    {
        if (LOG.isInfoEnabled()) {
            LOG.info("Start OScache cache initialisation");
        }

        OSCacheCache<T> cache = new OSCacheCache<T>();
        cache.initialize(new OSCacheCacheConfiguration(this.container, configuration, this.defaultPropsId));

        if (LOG.isInfoEnabled()) {
            LOG.info("End OScache cache initialisation");
        }

        return cache;
    }
}
