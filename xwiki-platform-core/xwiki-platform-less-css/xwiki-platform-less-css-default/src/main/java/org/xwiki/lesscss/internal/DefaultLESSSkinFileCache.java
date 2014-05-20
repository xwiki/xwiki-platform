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
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.lesscss.LESSSkinFileCache;

/**
 * Default implementation for {@link org.xwiki.lesscss.LESSSkinFileCache}.
 *
 * @since 6.1M1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLESSSkinFileCache implements LESSSkinFileCache, Initializable
{
    /**
     * Id of the cache for generated CSS.
     */
    public static final String LESS_FILES_CACHE_ID = "lesscss.skinfiles.cache";

    private static final String CACHE_KEY_SEPARATOR = "_";

    @Inject
    private CacheManager cacheManager;

    /**
     * The cache that will store the files.
     */
    private Cache<String> cache;

    /**
     * This map stores the list of the cached files keys corresponding to a couple of (wikiId, colorTheme).
     */
    private Map<String, List<String>> cachedFilesKeysMap = new HashMap<>();

    @Override
    public void initialize() throws InitializationException
    {
        try {
            CacheConfiguration configuration = new CacheConfiguration(LESS_FILES_CACHE_ID);
            CacheFactory cacheFactory = cacheManager.getCacheFactory();
            this.cache = cacheFactory.newCache(configuration);
        } catch (ComponentLookupException | CacheException e) {
            throw new InitializationException(
                    String.format("Failed to initialize LESS skin files cache [%s].", LESS_FILES_CACHE_ID), e);
        }
    }

    @Override
    public String get(String fileName, String wikiId, String colorTheme)
    {
        return cache.get(getCacheKey(fileName, wikiId, colorTheme));
    }

    @Override
    public void set(String fileName, String wikiId, String colorTheme, String output)
    {
        // Store the output in the cache
        String cacheKey = getCacheKey(fileName, wikiId, colorTheme);
        cache.set(cacheKey, output);

        // Add the new key to cachedFilesKeysMap.
        String mapKey = getMapKey(wikiId, colorTheme);
        List<String> cachedFilesKeys = cachedFilesKeysMap.get(mapKey);
        if (cachedFilesKeys == null) {
            // if the list of cached files keys corresponding to the couple of (wikiId, colorTheme) does not exist,
            // we create it
            cachedFilesKeys = new ArrayList<>(1);
            cachedFilesKeysMap.put(mapKey, cachedFilesKeys);
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
    public void clear(String wikiId, String colorTheme)
    {
        // Get the list of cached files keys corresponding to the couple (wikiId, colorTheme)
        String mapKey = getMapKey(wikiId, colorTheme);
        List<String> cachedFilesKeys = cachedFilesKeysMap.get(mapKey);
        if (cachedFilesKeys == null) {
            return;
        }
        // Remove all the cached files corresponding to the cached keys
        for (String cachedFileKey : cachedFilesKeys) {
            cache.remove(cachedFileKey);
        }
        // Remove the list of cached keys corresponding to the couple (wikiId, colorTheme)
        cachedFilesKeysMap.remove(mapKey);
    }

    private String getCacheKey(String fileName, String wikiId, String colorTheme)
    {
        return wikiId + CACHE_KEY_SEPARATOR + colorTheme + CACHE_KEY_SEPARATOR + fileName;
    }

    private String getMapKey(String wikiId, String colorTheme)
    {
        return wikiId + CACHE_KEY_SEPARATOR + colorTheme;
    }
}
