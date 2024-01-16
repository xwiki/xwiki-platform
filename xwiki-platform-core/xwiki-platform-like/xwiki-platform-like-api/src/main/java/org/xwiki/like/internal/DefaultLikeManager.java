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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
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
import org.xwiki.like.events.LikeEvent;
import org.xwiki.like.LikeException;
import org.xwiki.like.LikeManager;
import org.xwiki.like.events.UnlikeEvent;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.UnableToRegisterRightException;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Default implementation of {@link LikeManager} based on {@link RatingsManager}.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@Component
@Singleton
public class DefaultLikeManager implements LikeManager, Initializable, Disposable
{
    private static final int DEFAULT_LIKE_VOTE = 1;

    @Inject
    private RatingsManagerFactory ratingsManagerFactory;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceDocumentSerializer;

    @Inject
    private UserReferenceSerializer<String> userReferenceStringSerializer;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LikeConfiguration likeConfiguration;

    @Inject
    private EntityReferenceFactory entityReferenceFactory;

    private RatingsManager ratingsManager;

    private Cache<Long> likeCountCache;

    private Cache<Pair<EntityReference, Boolean>> likeExistCache;

    private Right likeRight;

    private LikeExistCacheEntryListener likeExistCacheEntryListener;

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
            this.likeRight = this.authorizationManager.register(LikeRight.INSTANCE);
        } catch (UnableToRegisterRightException e) {
            throw new InitializationException("Error while registering the Like right.", e);
        } catch (CacheException e) {
            throw new InitializationException("Error while creating the cache for likes.", e);
        }

        try {
            this.ratingsManager = this.ratingsManagerFactory
                .getRatingsManager(LikeRatingsConfiguration.RATING_MANAGER_HINT);
        } catch (RatingsException e) {
            throw new InitializationException("Error while trying to get the RatingManager.", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.likeExistCache.dispose();
        this.likeCountCache.dispose();
        try {
            this.authorizationManager.unregister(this.likeRight);
        } catch (AuthorizationException e) {
            throw new ComponentLifecycleException("Error while unregistering like right", e);
        }
    }

    private String getExistCacheKey(UserReference source, EntityReference target)
    {
        return String.format("%s_%s",
            this.userReferenceStringSerializer.serialize(source), this.entityReferenceSerializer.serialize(target));
    }

    @Override
    public long saveLike(UserReference source, EntityReference target) throws LikeException
    {
        DocumentReference userDoc = this.userReferenceDocumentSerializer.serialize(source);
        if (this.authorizationManager.hasAccess(this.likeRight, userDoc, target)) {
            try {
                EntityReference dedupTarget = this.entityReferenceFactory.getReference(target);
                this.ratingsManager.saveRating(dedupTarget, source, DEFAULT_LIKE_VOTE);
                this.likeCountCache.remove(this.entityReferenceSerializer.serialize(dedupTarget));
                this.likeExistCache.set(getExistCacheKey(source, target), Pair.of(dedupTarget, true));
                long newCount = this.getEntityLikes(dedupTarget);
                this.observationManager.notify(new LikeEvent(), source, dedupTarget);
                return newCount;
            } catch (RatingsException e) {
                throw new LikeException(String.format("Error while liking entity [%s]", target), e);
            }
        } else {
            throw new LikeException(String.format("User [%s] is not authorized to perform a like on [%s]",
                source, target));
        }
    }

    @Override
    public List<EntityReference> getUserLikes(UserReference source, int offset, int limit) throws LikeException
    {
        try {
            List<Rating> ratings = this.ratingsManager.getRatings(
                Collections.singletonMap(RatingsManager.RatingQueryField.USER_REFERENCE, source),
                offset,
                limit,
                RatingsManager.RatingQueryField.UPDATED_DATE,
                false);
            return ratings.stream().map(Rating::getReference).toList();
        } catch (RatingsException e) {
            throw new LikeException(
                String.format("Error when trying to retrieve user likes for user [%s]", source), e);
        }
    }

    @Override
    public long countUserLikes(UserReference source) throws LikeException
    {
        try {
            return this.ratingsManager.countRatings(
                Collections.singletonMap(RatingsManager.RatingQueryField.USER_REFERENCE, source));
        } catch (RatingsException e) {
            throw new LikeException(
                String.format("Error when trying to count user likes for user [%s]", source), e);
        }
    }

    @Override
    public long getEntityLikes(EntityReference target) throws LikeException
    {
        Long result = this.likeCountCache.get(this.entityReferenceSerializer.serialize(target));
        if (result == null) {
            Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
            queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, target);
            try {
                result = this.ratingsManager.countRatings(queryMap);
                this.likeCountCache.set(this.entityReferenceSerializer.serialize(target), result);
            } catch (RatingsException e) {
                throw
                    new LikeException(String.format("Error while getting ratings for entity [%s]", target), e);
            }
        }
        return result;
    }

    @Override
    public boolean removeLike(UserReference source, EntityReference target) throws LikeException
    {
        String serializedTarget = this.entityReferenceSerializer.serialize(target);
        DocumentReference userDoc = this.userReferenceDocumentSerializer.serialize(source);
        boolean result = false;
        if (this.authorizationManager.hasAccess(this.getLikeRight(), userDoc, target)) {
            EntityReference dedupTarget = this.entityReferenceFactory.getReference(target);
            Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
            queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, dedupTarget);
            queryMap.put(RatingsManager.RatingQueryField.USER_REFERENCE, source);

            try {
                List<Rating> ratings =
                    this.ratingsManager
                        .getRatings(queryMap, 0, 1, RatingsManager.RatingQueryField.UPDATED_DATE, false);
                if (!ratings.isEmpty()) {
                    result = this.ratingsManager.removeRating(ratings.get(0).getId());
                    this.likeCountCache.remove(serializedTarget);
                    this.likeExistCache.set(getExistCacheKey(source, dedupTarget), Pair.of(dedupTarget, false));
                    this.observationManager.notify(new UnlikeEvent(), source, dedupTarget);
                }
            } catch (RatingsException e) {
                throw new LikeException("Error while removing rating", e);
            }
        } else {
            throw new LikeException(
                String.format("User [%s] is not authorized to remove a like on [%s].",
                    userDoc, target));
        }
        return result;
    }

    @Override
    public boolean isLiked(UserReference source, EntityReference target) throws LikeException
    {
        Pair<EntityReference, Boolean> cacheValue = this.likeExistCache.get(getExistCacheKey(source, target));
        Boolean result = (cacheValue != null) ? cacheValue.getRight() : null;
        if (result == null) {
            EntityReference dedupTarget = this.entityReferenceFactory.getReference(target);
            Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
            queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, dedupTarget);
            queryMap.put(RatingsManager.RatingQueryField.USER_REFERENCE, source);

            try {
                List<Rating> ratings =
                    this.ratingsManager
                        .getRatings(queryMap, 0, 1, RatingsManager.RatingQueryField.UPDATED_DATE, false);
                result = !ratings.isEmpty();
                this.likeExistCache.set(getExistCacheKey(source, dedupTarget), Pair.of(dedupTarget, result));
            } catch (RatingsException e) {
                throw new LikeException("Error while checking if rating exists", e);
            }
        }
        return result;
    }

    @Override
    public List<UserReference> getLikers(EntityReference target, int offset, int limit) throws LikeException
    {
        Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, target);
        try {
            List<Rating> ratings = this.ratingsManager
                .getRatings(queryMap, offset, limit, RatingsManager.RatingQueryField.UPDATED_DATE, false);
            return ratings.stream().map(Rating::getAuthor).toList();
        } catch (RatingsException e) {
            throw new LikeException(String.format("Error while getting likers of [%s]", target), e);
        }
    }

    @Override
    public Right getLikeRight()
    {
        return this.likeRight;
    }

    @Override
    public void clearCache(EntityReference target)
    {
        this.likeCountCache.remove(this.entityReferenceSerializer.serialize(target));
        List<String> impactedKeys = new ArrayList<>(
            this.likeExistCacheEntryListener.getReferenceKeyMapping().getOrDefault(target, Collections.emptyList()));
        for (String impactedKey : impactedKeys) {
            this.likeExistCache.remove(impactedKey);
        }
    }

    @Override
    public void clearCache()
    {
        this.likeCountCache.removeAll();
        this.likeExistCache.removeAll();
    }
}
