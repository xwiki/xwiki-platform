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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.lesscss.LESSCache;
import org.xwiki.lesscss.LESSResourceReference;
import org.xwiki.lesscss.LESSResourceReferenceSerializer;

/**
 * Default and abstract implementation of {@link org.xwiki.lesscss.LESSCache}.
 *
 * @param <T> class of the object to cache
 *
 * @since 6.4M2
 * @version $Id$
 */
public abstract class AbstractCache<T> implements LESSCache<T>
{
    @Inject
    protected CacheManager cacheManager;

    /**
     * The cache that will store the content.
     */
    protected Cache<T> cache;

    /**
     * This map stores the list of the cached files keys corresponding to a skin, in order to clear the corresponding
     * cache when a skin is saved.
     */
    private Map<String, List<String>> cachedFilesKeysMapPerSkin = new HashMap<>();

    /**
     * This map stores the list of the cached files keys corresponding to a color theme, in order to clear the
     * corresponding cache when a color theme is saved.
     */
    private Map<String, List<String>> cachedFilesKeysMapPerColorTheme = new HashMap<>();

    /**
     * This map stores the list of the cached files keys corresponding to a LESS resource, in order to clear the
     * corresponding cache when a LESS resource is saved.
     */
    private Map<String, List<String>> cachedFilesKeysMapPerLESSResource = new HashMap<>();

    @Inject
    private CacheKeyFactory cacheKeyFactory;

    @Inject
    private CacheKeySerializer cacheKeySerializer;

    @Inject
    private LESSResourceReferenceSerializer lessResourceReferenceSerializer;

    @Override
    public T get(LESSResourceReference lessResourceReference, String skin, String colorTheme)
    {
        CacheKey cacheKey = cacheKeyFactory.getCacheKey(skin, colorTheme, lessResourceReference);
        String serializedCacheKey = cacheKeySerializer.serialize(cacheKey);
        return cache.get(serializedCacheKey);
    }

    @Override
    public void set(LESSResourceReference lessResourceReference, String skin, String colorTheme, T content)
    {
        // Store the content in the cache
        CacheKey cacheKey = cacheKeyFactory.getCacheKey(skin, colorTheme, lessResourceReference);
        String serializedCacheKey = cacheKeySerializer.serialize(cacheKey);
        cache.set(serializedCacheKey, content);

        // Add the new key to maps
        registerCacheKey(cachedFilesKeysMapPerSkin, serializedCacheKey, cacheKey.getSkin());
        registerCacheKey(cachedFilesKeysMapPerColorTheme, serializedCacheKey, cacheKey.getColorTheme());
        registerCacheKey(cachedFilesKeysMapPerLESSResource, serializedCacheKey,
            lessResourceReferenceSerializer.serialize(lessResourceReference));
    }

    /**
     * Add the cache key in the specified map (cachedFilesKeysMapPerSkin or cachedFilesKeysMapPerColorTheme), to be
     * able to clear the cache when one skin or one color theme is modified.
     *
     * @param cachedFilesKeysMap could be cachedFilesKeysMapPerSkin or cachedFilesKeysMapPerColorTheme
     * @param cacheKey the cache key to register
     * @param name name of the skin or of the color theme
     */
    private void registerCacheKey(Map<String, List<String>> cachedFilesKeysMap, String cacheKey, String name)
    {
        List<String> cachedFilesKeys = cachedFilesKeysMap.get(name);
        if (cachedFilesKeys == null) {
            // if the list of cached files keys corresponding to the skin/colortheme name does not exist, we create it
            cachedFilesKeys = new ArrayList<>();
            cachedFilesKeysMap.put(name, cachedFilesKeys);
        }
        if (!cachedFilesKeys.contains(cacheKey)) {
            cachedFilesKeys.add(cacheKey);
        }
    }

    @Override
    public void clear()
    {
        cache.removeAll();
        cachedFilesKeysMapPerSkin.clear();
        cachedFilesKeysMapPerColorTheme.clear();
        cachedFilesKeysMapPerLESSResource.clear();
    }

    private void clearFromCriteria(Map<String, List<String>> cachedFilesKeysMap, String criteria)
    {
        // Get the list of cached files keys corresponding to the criteria
        List<String> cachedFilesKeys = cachedFilesKeysMap.get(criteria);
        if (cachedFilesKeys == null) {
            return;
        }
        // Remove all the cached files corresponding to the cached keys
        for (String cachedFileKey : cachedFilesKeys) {
            cache.remove(cachedFileKey);
        }
        // Remove the list of cached keys corresponding to the criteria
        cachedFilesKeysMap.remove(criteria);
    }

    @Override
    public void clearFromSkin(String skin)
    {
        clearFromCriteria(cachedFilesKeysMapPerSkin, skin);
    }

    @Override
    public void clearFromColorTheme(String colorTheme)
    {
        clearFromCriteria(cachedFilesKeysMapPerColorTheme, colorTheme);
    }

    @Override
    public void clearFromLESSResource(LESSResourceReference lessResourceReference)
    {
        clearFromCriteria(cachedFilesKeysMapPerLESSResource,
            lessResourceReferenceSerializer.serialize(lessResourceReference));
    }
}
