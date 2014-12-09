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
package org.xwiki.lesscss.internal.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.LESSResourceReference;
import org.xwiki.lesscss.internal.colortheme.ColorThemeFullNameGetter;

/**
 * Factory to create a cache key.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component(roles = CacheKeyFactory.class)
@Singleton
public class CacheKeyFactory
{
    @Inject
    private ColorThemeFullNameGetter colorThemeFullNameGetter;

    /**
     * Get the cache key corresponding to the given LESS resource and context.
     *
     * @param skin skin for which the resource have been compiled
     * @param colorTheme color theme for which the resource have been compiled.
     * @param lessResourceReference the reference to the LESS resource that have been compiled
     *
     * @return the corresponding cache key.
     */
    public CacheKey getCacheKey(String skin, String colorTheme, LESSResourceReference lessResourceReference)
    {
        return new CacheKey(skin, colorThemeFullNameGetter.getColorThemeFullName(colorTheme), lessResourceReference);
    }
}
