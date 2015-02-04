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
package org.xwiki.lesscss.cache;

import org.xwiki.lesscss.colortheme.ColorThemeReference;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.skin.SkinReference;

/**
 * Component to cache objects computed from a LESS files contained in the skin.
 *
 * @param <T> class of the objects to cache
 *
 * @since 6.4M2
 * @version $Id$
 */
public interface LESSCache<T>
{
    /**
     * Get an object from the name of the LESS source, the skin and the name of the color theme.
     * @param lessResourceReference reference of the code to compile
     * @param skin reference of the skin
     * @param colorTheme reference of the color theme
     * @return the corresponding CSS
     */
    T get(LESSResourceReference lessResourceReference, SkinReference skin, ColorThemeReference colorTheme);

    /**
     * Add an object in the cache.
     *
     * @param lessResourceReference reference of the code to compile
     * @param skin reference of the skin
     * @param colorThemeName reference of the color theme
     * @param object the object to cache
     */
    void set(LESSResourceReference lessResourceReference, SkinReference skin, ColorThemeReference colorThemeName,
        T object);

    /**
     * Clear the cache.
     */
    void clear();

    /**
     * Clear all the cached content related to a skin.
     *
     * @param skin reference to the skin
     */
    void clearFromSkin(SkinReference skin);

    /**
     * Clear all the cached content related to a color theme.
     *
     * @param colorTheme reference of the color theme
     */
    void clearFromColorTheme(ColorThemeReference colorTheme);

    /**
     * Clear all the cached content related to a LESS resource.
     *
     * @param lessResourceReference reference of a LESS resource
     */
    void clearFromLESSResource(LESSResourceReference lessResourceReference);

    /** 
     * Create a Mutex for a cache entry, to be used by someone who need a Thread-Safe access to the cache.
     *  
     * @param lessResourceReference a reference to a LESS resource
     * @param skin a reference to a Skin
     * @param colorTheme a reference to a color theme 
     * @return the mutex related to the 3 parameters
     * 
     * @since 6.4.1
     */
    Object getMutex(LESSResourceReference lessResourceReference, SkinReference skin, ColorThemeReference colorTheme);
}
