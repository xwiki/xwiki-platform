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
package org.xwiki.icon;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * A cache to store the already loaded icon sets.
 *
 * @since 6.2M1
 * @version $Id$
 */
@Role
public interface IconSetCache
{
    /**
     * Get an icon set, from its name.
     * @param name name of the icon set to get
     * @return the icon set corresponding to the given name
     */
    IconSet get(String name);

    /**
     * Get an icon set, from its name and the icon where it is located.
     * @param name name of the icon set to get
     * @param wikiId id of the wiki
     * @return the icon set corresponding to the given name
     *
     * @since 6.3RC1
     */
    IconSet get(String name, String wikiId);

    /**
     * Get the icon set corresponding to a document on the wiki.
     * @param documentReference reference of the document
     * @return the icon set corresponding to the given document reference
     */
    IconSet get(DocumentReference documentReference);

    /**
     * Put an iconset into this cache.
     * @param name name of the icon set to cache
     * @param iconSet the icon set to cache
     */
    void put(String name, IconSet iconSet);

    /**
     * Put an iconset into this cache.
     * @param name name of the icon set to cache
     * @param wikiId id of the wiki
     * @param iconSet the icon set to cache
     *
     * @since 6.3RC1
     */
    void put(String name, String wikiId, IconSet iconSet);

    /**
     * Put an iconset into this cache.
     * @param documentReference reference to the document containing the icon set
     * @param iconSet the icon set to cache
     */
    void put(DocumentReference documentReference, IconSet iconSet);

    /**
     * Clear all the cache.
     */
    void clear();

    /**
     * Remove from the cache an icon set corresponding to a document in the wiki.
     * @param documentReference reference of the document
     */
    void clear(DocumentReference documentReference);

    /**
     * Remove from the cache an icon set corresponding to a given name.
     * @param name the name of the icon set
     */
    void clear(String name);

    /**
     * Remove from the cache an icon set corresponding to a given name.
     * @param name the name of the icon set
     * @param wikiId id of the wiki where the icon theme is stored
     *
     * @since 6.3RC1
     */
    void clear(String name, String wikiId);
}
