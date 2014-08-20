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
package org.xwiki.lesscss;

/**
 * Component to cache objects computed from a LESS files contained in the skin.
 *
 * @param <T> class of the objects to cache
 *
 * @since 6.1M2
 * @version $Id$
 */
public interface LESSCache<T>
{
    /**
     * Get an object from the name of the LESS source, the wiki ID, the skin and the name of the color theme.
     * @param fileName name of the LESS source
     * @param wikiId id of the wiki
     * @param skin name of the skin
     * @param colorTheme name of the color theme
     * @return the corresponding CSS
     */
    T get(String fileName, String wikiId, String skin, String colorTheme);

    /**
     * Add an object in the cache.
     * @param fileName name of the LESS source
     * @param wiki if og the wiki
     * @param fileSystemSkin name of the skin
     * @param colorThemeName name of the color theme
     * @param object the object to cache
     */
    void set(String fileName, String wiki, String fileSystemSkin, String colorThemeName, T object);

    /**
     * Clear the cache.
     */
    void clear();

    /**
     * Clear all the cached files related to a wiki.
     * @param wiki name of the wiki
     */
    void clear(String wiki);
}
