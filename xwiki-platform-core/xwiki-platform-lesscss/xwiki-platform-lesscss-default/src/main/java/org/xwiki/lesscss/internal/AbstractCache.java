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

import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.lesscss.LESSCache;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

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
     * Whether or not the cache should handle the current XWikiContext object (true by default).
     * @since 6.2.6
     */
    protected boolean isContextHandled = true;

    /**
     * The cache that will store the content.
     */
    protected Cache<T> cache;

    @Inject
    protected Logger logger;

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

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private XWikiContextCacheKeyFactory xwikiContextCacheKeyFactory;

    @Override
    public T get(String fileName, String fileSystemSkin, String colorTheme)
    {
        return cache.get(getCacheKey(fileName, fileSystemSkin, getColorThemeFullName(colorTheme)));
    }

    @Override
    public void set(String fileName, String fileSystemSkin, String colorTheme, T content)
    {
        // Get the fullname of the color theme
        String colorThemeFullName = getColorThemeFullName(colorTheme);

        // Store the content in the cache
        String cacheKey = getCacheKey(fileName, fileSystemSkin, colorThemeFullName);
        cache.set(cacheKey, content);

        // Add the new key to maps
        registerCacheKey(cachedFilesKeysMapPerSkin, cacheKey, fileSystemSkin);
        registerCacheKey(cachedFilesKeysMapPerColorTheme, cacheKey, colorTheme);
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
    public void clearFromFileSystemSkin(String fileSystemSkin)
    {
        clearFromCriteria(cachedFilesKeysMapPerSkin, fileSystemSkin);
    }

    @Override
    public void clearFromColorTheme(String colorTheme)
    {
        clearFromCriteria(cachedFilesKeysMapPerColorTheme, colorTheme);
    }

    private String getColorThemeFullName(String colorTheme)
    {
        // Current Wiki Reference
        WikiReference currentWikiRef = new WikiReference(wikiDescriptorManager.getCurrentWikiId());
        // Get the full reference of the color theme
        DocumentReference colorThemeRef = documentReferenceResolver.resolve(colorTheme, currentWikiRef);
        // Return the serialized reference
        return entityReferenceSerializer.serialize(colorThemeRef);
    }

    private String getCacheKey(String fileName, String skin, String colorThemeFullName)
    {
        String result = skin.length() + skin + CACHE_KEY_SEPARATOR  + colorThemeFullName.length() + colorThemeFullName
                + CACHE_KEY_SEPARATOR + fileName.length() + fileName;
        if (isContextHandled) {
            try {
                String xcontext = xwikiContextCacheKeyFactory.getCacheKey();
                result += CACHE_KEY_SEPARATOR + xcontext.length() + xcontext;
            } catch (LESSCompilerException e) {
                logger.warn("Failed to generate a cache key handling the XWikiContext", e);
            }
        }
        return result;
    }
}
