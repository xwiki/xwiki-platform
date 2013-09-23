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
package org.xwiki.formula.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.formula.ImageData;
import org.xwiki.formula.ImageStorage;

/**
 * Cache-based implementation for the {@link ImageStorage} component.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Singleton
public class CacheImageStorage implements ImageStorage, Initializable
{
    /**
     * Since this class implements a storage based on the {@link Cache} component, it needs to access the
     * {@link CacheManager cache manager} to obtain a valid cache.
     */
    @Inject
    private CacheManager cacheManager;

    /** Cache used as the storage back-end. */
    private Cache<ImageData> cache;

    @Override
    public ImageData get(String id)
    {
        return this.cache.get(id);
    }

    @Override
    public void put(String id, ImageData data)
    {
        this.cache.set(id, data);
    }

    @Override
    public void initialize() throws InitializationException
    {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setConfigurationId("xwiki.plugin.formula");
        try {
            this.cache = this.cacheManager.createNewCache(configuration);
        } catch (CacheException e) {
            throw new InitializationException("Failed to create cache", e);
        }
    }
}
