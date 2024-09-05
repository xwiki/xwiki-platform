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

package org.xwiki.like.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheEntry;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.like.LikeConfiguration;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Helper to perform cache operations.
 *
 * @version $Id$
 * @since 16.8.0RC1
 */
@Component(roles = LikeManagerCacheHelper.class)
@Singleton
public class LikeManagerCacheHelper implements Initializable, Disposable
{
    /**
     * A dedicated {@link CacheEntryListener} implementation for the likeExistCache.
     * The purpose of this implementation is to be able to find back the keys of the cache related to a specific entity.
     * This allows to properly clean the cache entity when using {@link #clearCache(EntityReference)}.
     *
     * @version $Id$
     * @since 12.9RC1
     */
    static class LikeExistCacheEntryListener implements CacheEntryListener<Pair<EntityReference, Boolean>>
    {
        private final Map<EntityReference, List<String>> referenceKeyMapping = new ConcurrentHashMap<>();

        @Override
        public void cacheEntryAdded(CacheEntryEvent<Pair<EntityReference, Boolean>> event)
        {
            CacheEntry<Pair<EntityReference, Boolean>> entry = event.getEntry();
            String key = entry.getKey();
            EntityReference entityReference = entry.getValue().getLeft();
            List<String> keyList;

            if (this.referenceKeyMapping.containsKey(entityReference)) {
                keyList = this.referenceKeyMapping.get(entityReference);
                keyList.add(key);
            } else {
                keyList = new ArrayList<>();
                keyList.add(key);
                this.referenceKeyMapping.put(entityReference, keyList);
            }
        }

        @Override
        public void cacheEntryRemoved(CacheEntryEvent<Pair<EntityReference, Boolean>> event)
        {
            CacheEntry<Pair<EntityReference, Boolean>> entry = event.getEntry();
            String key = entry.getKey();
            EntityReference entityReference = entry.getValue().getLeft();

            if (this.referenceKeyMapping.containsKey(entityReference)) {
                List<String> keyList = this.referenceKeyMapping.get(entityReference);
                keyList.remove(key);
                if (keyList.isEmpty()) {
                    this.referenceKeyMapping.remove(entityReference);
                }
            }
        }

        @Override
        public void cacheEntryModified(CacheEntryEvent<Pair<EntityReference, Boolean>> event)
        {
            // do nothing
        }

        public Map<EntityReference, List<String>> getReferenceKeyMapping()
        {
            return referenceKeyMapping;
        }
    }

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LikeConfiguration likeConfiguration;

    @Inject
    private UserReferenceSerializer<String> userReferenceStringSerializer;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Cache<Long> likeCountCache;
    private Cache<Pair<EntityReference, Boolean>> likeExistCache;
    private LikeExistCacheEntryListener likeExistCacheEntryListener;

    @Override
    public void initialize() throws InitializationException
    {
        int likeCacheCapacity = this.likeConfiguration.getLikeCacheCapacity();
        try {
            this.likeCountCache = this.cacheManager.createNewCache(
                new LRUCacheConfiguration("xwiki.like.count.cache", likeCacheCapacity));
            this.likeExistCache = this.cacheManager.createNewCache(
                new LRUCacheConfiguration("xwiki.like.exist.cache", likeCacheCapacity));
            this.likeExistCacheEntryListener = new LikeExistCacheEntryListener();
            this.likeExistCache.addCacheEntryListener(this.likeExistCacheEntryListener);
        } catch (CacheException e) {
            throw new InitializationException("Error while creating the cache for likes.", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.likeExistCache.dispose();
        this.likeCountCache.dispose();
    }

    private String getExistCacheKey(UserReference source, EntityReference target)
    {
        return String.format("%s_%s",
            this.userReferenceStringSerializer.serialize(source), this.entityReferenceSerializer.serialize(target));
    }

    /**
     * Remove like count value from the cache.
     * @param target the reference for which to remove the like count value in cache.
     */
    public void removeCount(EntityReference target)
    {
        this.likeCountCache.remove(this.entityReferenceSerializer.serialize(target));
    }

    /**
     * Set the list exist value in cache.
     * @param source the user who might have performed a like
     * @param target the entity which might have been liked
     * @param value the actual boolean value defining if it's liked or not
     */
    public void setExist(UserReference source, EntityReference target, boolean value)
    {
        this.likeExistCache.set(getExistCacheKey(source, target), Pair.of(target, value));
    }

    /**
     * Retrieve the value if a like exists or not for given entity and by given user.
     * @param source the user for who to check if the like exists
     * @param target the entity for which to check if the like exists
     * @return a boolean value if there's a value in cache or {@code null} if there's no value.
     */
    public Boolean isExist(UserReference source, EntityReference target)
    {
        Boolean result = null;
        Pair<EntityReference, Boolean> cacheEntry =
            this.likeExistCache.get(getExistCacheKey(source, target));
        if (cacheEntry != null) {
            result = cacheEntry.getRight();
        }
        return result;
    }

    /**
     * Retrieve the number of likes for the given entity.
     * @param target the entity for which to get number of likes.
     * @return a long corresponding to the number of likes if there's a value in cache or {@code null}.
     */
    public Long getCount(EntityReference target)
    {
        return this.likeCountCache.get(this.entityReferenceSerializer.serialize(target));
    }

    /**
     * Put in cache the number of likes for given entity.
     * @param target the entity for which to store number of likes
     * @param value the number of likes
     */
    public void setCount(EntityReference target, Long value)
    {
        this.likeCountCache.set(this.entityReferenceSerializer.serialize(target), value);
    }

    /**
     * Clear the caches for the given entity.
     * @param target the entity for which to clear caches.
     */
    public void clearCache(EntityReference target)
    {
        this.likeCountCache.remove(this.entityReferenceSerializer.serialize(target));
        List<String> impactedKeys = new ArrayList<>(
            this.likeExistCacheEntryListener.getReferenceKeyMapping().getOrDefault(target, Collections.emptyList()));
        for (String impactedKey : impactedKeys) {
            this.likeExistCache.remove(impactedKey);
        }
    }

    /**
     * Clear all caches.
     */
    public void clearCache()
    {
        this.likeCountCache.removeAll();
        this.likeExistCache.removeAll();
    }
}
