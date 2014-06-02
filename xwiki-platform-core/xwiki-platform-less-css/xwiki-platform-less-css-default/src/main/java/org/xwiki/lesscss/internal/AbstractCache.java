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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.lesscss.LESSCache;

/**
 * Default and abstract implementation of {@link org.xwiki.lesscss.LESSCache}.
 *
 * @param <T> class of the object to cache
 *
 * @since 6.1M2
 * @version $Id$
 */
public abstract class AbstractCache<T> implements LESSCache<T>
{
    private static final String CACHE_KEY_SEPARATOR = "_";

    @Inject
    protected CacheManager cacheManager;

    /**
     * The cache that will store the content.
     */
    protected Cache<T> cache;

    /**
     * This map stores the list of the cached files keys corresponding to a wiki.
     */
    private Map<String, List<String>> cachedFilesKeysMap = new HashMap<>();

    @Override
    public T get(String fileName, String wiki, String fileSystemSkin, String colorTheme)
    {
        return cache.get(getCacheKey(fileName, wiki, fileSystemSkin, colorTheme));
    }

    @Override
    public void set(String fileName, String wiki, String fileSystemSkin, String colorTheme, T content)
    {
        // Store the content in the cache
        String cacheKey = getCacheKey(fileName, wiki, fileSystemSkin, colorTheme);
        cache.set(cacheKey, content);

        // Add the new key to cachedFilesKeysMap.
        List<String> cachedFilesKeys = cachedFilesKeysMap.get(wiki);
        if (cachedFilesKeys == null) {
            // if the list of cached files keys corresponding to the wiki does not exist, we create it
            cachedFilesKeys = new ArrayList<>();
            cachedFilesKeysMap.put(wiki, cachedFilesKeys);
        }
        if (!cachedFilesKeys.contains(cacheKey)) {
            cachedFilesKeys.add(cacheKey);
        }
    }

    @Override
    public void clear()
    {
        cache.removeAll();
        cachedFilesKeysMap.clear();
    }

    @Override
    public void clear(String wiki)
    {
        // Get the list of cached files keys corresponding to the wiki
        List<String> cachedFilesKeys = cachedFilesKeysMap.get(wiki);
        if (cachedFilesKeys == null) {
            return;
        }
        // Remove all the cached files corresponding to the cached keys
        for (String cachedFileKey : cachedFilesKeys) {
            cache.remove(cachedFileKey);
        }
        // Remove the list of cached keys corresponding to the wiki
        cachedFilesKeysMap.remove(wiki);
    }

    private String getCacheKey(String fileName, String wiki, String skin, String colorTheme)
    {
        return wiki.length() + wiki + CACHE_KEY_SEPARATOR + skin.length() + skin + CACHE_KEY_SEPARATOR
            + colorTheme.length() + colorTheme + CACHE_KEY_SEPARATOR + fileName.length() + fileName;
    }
}
