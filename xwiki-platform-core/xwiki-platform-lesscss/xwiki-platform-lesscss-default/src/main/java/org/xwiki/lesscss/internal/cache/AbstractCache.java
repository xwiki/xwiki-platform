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
import org.xwiki.lesscss.cache.LESSCache;
import org.xwiki.lesscss.colortheme.ColorThemeReference;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.skin.SkinReference;

/**
 * Default and abstract implementation of {@link org.xwiki.lesscss.cache.LESSCache}.
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
     * Whether or not the cache should handle the current XWikiContext object (true by default).
     */
    protected boolean isContextHandled = true;

    /**
     * The cache that will store the content.
     */
    protected Cache<T> cache;

    /**
     * This map stores the list of the cached files keys corresponding to a skin, in order to clear the corresponding
     * cache when a skin is saved.
     */
    private Map<Object, List<String>> cachedFilesKeysMapPerSkin = new HashMap<>();

    /**
     * This map stores the list of the cached files keys corresponding to a color theme, in order to clear the
     * corresponding cache when a color theme is saved.
     */
    private Map<Object, List<String>> cachedFilesKeysMapPerColorTheme = new HashMap<>();

    /**
     * This map stores the list of the cached files keys corresponding to a LESS resource, in order to clear the
     * corresponding cache when a LESS resource is saved.
     */
    private Map<Object, List<String>> cachedFilesKeysMapPerLESSResource = new HashMap<>();

    @Inject
    private CacheKeyFactory cacheKeyFactory;

    private Map<String, String> mutexList = new HashMap<>();

    @Override
    public T get(LESSResourceReference lessResourceReference, SkinReference skin, ColorThemeReference colorTheme)
    {
        return cache.get(cacheKeyFactory.getCacheKey(lessResourceReference, skin, colorTheme, isContextHandled));
    }

    @Override
    public void set(LESSResourceReference lessResourceReference, SkinReference skin,
        ColorThemeReference colorTheme, T content)
    {
        // Store the content in the cache
        String cacheKey = cacheKeyFactory.getCacheKey(lessResourceReference, skin, colorTheme, isContextHandled);
        cache.set(cacheKey, content);

        // Add the new key to maps
        registerCacheKey(cachedFilesKeysMapPerSkin, cacheKey, skin);
        registerCacheKey(cachedFilesKeysMapPerColorTheme, cacheKey, colorTheme);
        registerCacheKey(cachedFilesKeysMapPerLESSResource, cacheKey, lessResourceReference);
    }

    /**
     * Add the cache key in the specified map (cachedFilesKeysMapPerSkin or cachedFilesKeysMapPerColorTheme), to be
     * able to clear the cache when one skin or one color theme is modified.
     *
     * @param cachedFilesKeysMap could be cachedFilesKeysMapPerSkin or cachedFilesKeysMapPerColorTheme
     * @param cacheKey the cache key to register
     * @param reference name of the skin or of the color theme
     */
    private void registerCacheKey(Map<Object, List<String>> cachedFilesKeysMap, String cacheKey, Object reference)
    {
        List<String> cachedFilesKeys = cachedFilesKeysMap.get(reference);
        if (cachedFilesKeys == null) {
            // if the list of cached files keys corresponding to the skin/colortheme name does not exist, we create it
            cachedFilesKeys = new ArrayList<>();
            cachedFilesKeysMap.put(reference, cachedFilesKeys);
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

    private void clearFromCriteria(Map<Object, List<String>> cachedFilesKeysMap, Object criteria)
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
    public void clearFromSkin(SkinReference skin)
    {
        clearFromCriteria(cachedFilesKeysMapPerSkin, skin);
    }

    @Override
    public void clearFromColorTheme(ColorThemeReference colorTheme)
    {
        clearFromCriteria(cachedFilesKeysMapPerColorTheme, colorTheme);
    }

    @Override
    public void clearFromLESSResource(LESSResourceReference lessResourceReference)
    {
        clearFromCriteria(cachedFilesKeysMapPerLESSResource, lessResourceReference);
    }
    
    @Override
    public synchronized Object getMutex(LESSResourceReference lessResourceReference, SkinReference skin, 
        ColorThemeReference colorTheme)
    {
        // The mutex is a string (actually the cache key) to help debugging.
        String cacheKey = cacheKeyFactory.getCacheKey(lessResourceReference, skin, colorTheme, isContextHandled);
        String mutex = mutexList.get(cacheKey);
        if (mutex == null) {
            // the mutex is the key, so no extra memory is needed
            mutex = cacheKey;
            mutexList.put(cacheKey, mutex);
        }
        return mutex;
    }
}
