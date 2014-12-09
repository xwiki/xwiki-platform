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
import org.xwiki.lesscss.LESSResourceReferenceSerializer;

/**
 * Serialize a cache key to a string.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component(roles = CacheKeySerializer.class)
@Singleton
public class CacheKeySerializer
{
    private static final String CACHE_KEY_SEPARATOR = "_";

    @Inject
    private LESSResourceReferenceSerializer lessResourceReferenceSerializer;

    /**
     * @param cacheKey the cache key to serialize
     * @return the serialized cache key
     */
    public String serialize(CacheKey cacheKey)
    {
        String serializedReference = lessResourceReferenceSerializer.serialize(cacheKey.getLessResourceReference());
        String skin = cacheKey.getSkin();
        String colorThemeFullName = cacheKey.getColorTheme();

        return skin.length() + skin + CACHE_KEY_SEPARATOR + colorThemeFullName.length() + colorThemeFullName
                + CACHE_KEY_SEPARATOR + serializedReference.length() + serializedReference;
    }
}
