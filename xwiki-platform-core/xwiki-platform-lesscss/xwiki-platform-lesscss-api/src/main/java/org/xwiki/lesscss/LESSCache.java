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

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Component to cache objects computed from a LESS files contained in the skin and in the wiki.
 *
 * @param <T> class of the objects to cache
 *
 * @since 6.1M2
 * @version $Id$
 */
@Unstable
public interface LESSCache<T>
{
    /**
     * Get an object from the name of the LESS source, the skin and the name of the color theme.
     * @param fileName name of the LESS source
     * @param skin name of the skin
     * @param colorTheme name of the color theme
     * @return the corresponding CSS
     *
     * @since 6.3M2
     */
    T get(String fileName, String skin, String colorTheme);

    /**
     * Get an object from the reference of an entity, the skin and the name of the color theme.
     * @param entityReference reference of an entity that contains some LESS code
     * @param skin name of the skin
     * @param colorTheme name of the color theme
     * @return the corresponding CSS
     *
     * @since 6.4M2
     */
    T get(EntityReference entityReference, String skin, String colorTheme);

    /**
     * Add an object in the cache.
     *
     * @param fileName name of the LESS source
     * @param fileSystemSkin name of the skin
     * @param colorThemeName name of the color theme
     * @param object the object to cache
     *
     * @since 6.3M2
     */
    void set(String fileName, String fileSystemSkin, String colorThemeName, T object);

    /**
     * Add an object in the cache.
     *
     * @param entityReference reference of an entity that contains some LESS code
     * @param fileSystemSkin name of the skin
     * @param colorThemeName name of the color theme
     * @param object the object to cache
     *
     * @since 6.4M2
     */
    void set(EntityReference entityReference, String fileSystemSkin, String colorThemeName, T object);

    /**
     * Clear the cache.
     */
    void clear();

    /**
     * Clear all the cached files related to a skin.
     *
     * @param fileSystemSkin name of the filesystem skin
     *
     * @since 6.3M2
     */
    void clearFromFileSystemSkin(String fileSystemSkin);

    /**
     * Clear all the cached files related to a color theme.
     *
     * @param colorTheme name of the color theme
     *
     * @since 6.3M2
     */
    void clearFromColorTheme(String colorTheme);

    /**
     * Clear all the cached files related to an entity.
     *
     * @param entity reference to an entity
     *
     * @since 6.4M2
     */
    void clearFromEntity(EntityReference entity);
}
