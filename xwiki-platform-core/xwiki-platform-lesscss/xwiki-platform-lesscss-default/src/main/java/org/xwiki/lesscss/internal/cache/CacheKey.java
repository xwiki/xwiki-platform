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

import org.xwiki.lesscss.LESSResourceReference;

/**
 * Represents the key for a cached less resource compilation, by holding a reference of the LESS resource and the
 * context (skin, colortheme) for which it has been compiled.
 * It can also be used as key for a mutex to avoid having 2 compilations of the same resource with the same context in
 * the same time.
 *
 * @since 6.4M2
 * @version $Id$
 */
public class CacheKey
{
    private String skin;

    private String colorTheme;

    private LESSResourceReference lessResourceReference;

    /**
     * Constructor.
     * @param skin skin for which the resource have been compiled
     * @param colorTheme color theme for which the resource have been compiled (must be a full reference like:
     * wiki:Space.Page)
     * @param lessResourceReference the reference to the LESS resource that have been compiled
     */
    public CacheKey(String skin, String colorTheme,
            LESSResourceReference lessResourceReference)
    {
        this.skin = skin;
        this.colorTheme = colorTheme;
        this.lessResourceReference = lessResourceReference;
    }

    /**
     * @return the skin for which the resource have been compiled
     */
    public String getSkin()
    {
        return skin;
    }

    /**
     * @return the fullname of the color theme for which the resource have been compiled
     */
    public String getColorTheme()
    {
        return colorTheme;
    }

    /**
     * @return the reference to the LESS resource that have been compiled
     */
    public LESSResourceReference getLessResourceReference()
    {
        return lessResourceReference;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof CacheKey) {
            CacheKey cacheKey = (CacheKey) o;
            return skin.equals(cacheKey.skin) && colorTheme.equals(cacheKey.colorTheme)
                && lessResourceReference.equals(cacheKey.lessResourceReference);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return skin.hashCode() * colorTheme.hashCode() * lessResourceReference.hashCode();
    }
}
