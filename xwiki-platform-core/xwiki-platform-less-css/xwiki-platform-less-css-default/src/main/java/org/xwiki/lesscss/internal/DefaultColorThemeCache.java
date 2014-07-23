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
package org.xwiki.lesscss.internal;

import javax.inject.Singleton;

import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.lesscss.ColorTheme;
import org.xwiki.lesscss.ColorThemeCache;

/**
 * Default implementation for {@link org.xwiki.lesscss.ColorThemeCache}.
 *
 * @since 6.1M1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultColorThemeCache extends AbstractCache<ColorTheme> implements ColorThemeCache, Initializable
{
    /**
     * Id of the cache for generated CSS.
     */
    public static final String LESS_COLOR_THEMES_CACHE_ID = "lesscss.colortheme.cache";

    @Override
    public void initialize() throws InitializationException
    {
        try {
            CacheConfiguration configuration = new CacheConfiguration(LESS_COLOR_THEMES_CACHE_ID);
            CacheFactory cacheFactory = cacheManager.getCacheFactory();
            this.cache = cacheFactory.newCache(configuration);
        } catch (ComponentLookupException | CacheException e) {
            throw new InitializationException(
                    String.format("Failed to initialize LESS color themes cache [%s].", LESS_COLOR_THEMES_CACHE_ID), e);
        }
    }
}
