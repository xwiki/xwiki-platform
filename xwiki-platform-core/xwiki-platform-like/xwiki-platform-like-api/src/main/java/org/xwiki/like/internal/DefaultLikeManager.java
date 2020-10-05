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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.like.LikeConfiguration;
import org.xwiki.like.LikeEvent;
import org.xwiki.like.LikeException;
import org.xwiki.like.LikeManager;
import org.xwiki.like.UnlikeEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
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

    private RatingsManager ratingsManager;

    private Cache<Long> likeCountCache;

    private Cache<Boolean> likeExistCache;

    private Right likeRight;

    @Override
    public void initialize() throws InitializationException
    {
        int likeCacheCapacity = this.likeConfiguration.getLikeCacheCapacity();
        try {
            this.likeCountCache = this.cacheManager.createNewCache(
                new LRUCacheConfiguration("xwiki.like.count.cache", likeCacheCapacity));
            this.likeExistCache = this.cacheManager.createNewCache(
                new LRUCacheConfiguration("xwiki.like.exist.cache", likeCacheCapacity));
            this.likeRight = this.authorizationManager.register(LikeRight.INSTANCE);
        } catch (UnableToRegisterRightException e) {
            throw new InitializationException("Error while registering the Like right.", e);
        } catch (CacheException e) {
            throw new InitializationException("Error while creating the cache for likes.", e);
        }

        try {
            this.ratingsManager = this.ratingsManagerFactory
                .getRatingsManager(LikeRatingsConfiguration.RANKING_MANAGER_HINT);
        } catch (RatingsException e) {
            throw new InitializationException("Error while trying to get the RankingManager.", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        //this.authorizationManager.
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
                this.ratingsManager.saveRating(target, source, DEFAULT_LIKE_VOTE);
                this.likeCountCache.remove(this.entityReferenceSerializer.serialize(target));
                this.likeExistCache.set(getExistCacheKey(source, target), true);
                long newCount = this.getEntityLikes(target);
                this.observationManager.notify(new LikeEvent(), source, target);
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
            List<Rating> gradings = this.ratingsManager.getRatings(
                Collections.singletonMap(RatingsManager.RatingQueryField.USER_REFERENCE, source),
                offset,
                limit,
                RatingsManager.RatingQueryField.UPDATED_DATE,
                false);
            return gradings.stream().map(Rating::getReference).collect(Collectors.toList());
        } catch (RatingsException e) {
            throw new LikeException(
                String.format("Error when trying to retrieve user likes for user [%s]", source), e);
        }
    }

    @Override
    public long getEntityLikes(EntityReference target) throws LikeException
    {
        Long result = this.likeCountCache.get(this.entityReferenceSerializer.serialize(target));
        if (result == null) {
            Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
            queryMap.put(RatingsManager.RatingQueryField.ENTITY_TYPE, target.getType());
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
            Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
            queryMap.put(RatingsManager.RatingQueryField.ENTITY_TYPE, target.getType());
            queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, target);
            queryMap.put(RatingsManager.RatingQueryField.USER_REFERENCE, source);

            try {
                List<Rating> gradings =
                    this.ratingsManager
                        .getRatings(queryMap, 0, 1, RatingsManager.RatingQueryField.UPDATED_DATE, false);
                if (!gradings.isEmpty()) {
                    result = this.ratingsManager.removeRating(gradings.get(0).getId());
                    this.likeCountCache.remove(serializedTarget);
                    this.likeExistCache.set(getExistCacheKey(source, target), false);
                    this.observationManager.notify(new UnlikeEvent(), source, target);
                }
            } catch (RatingsException e) {
                throw new LikeException("Error while removing grading", e);
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
        Boolean result = this.likeExistCache.get(getExistCacheKey(source, target));
        if (result == null) {
            Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
            queryMap.put(RatingsManager.RatingQueryField.ENTITY_TYPE, target.getType());
            queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, target);
            queryMap.put(RatingsManager.RatingQueryField.USER_REFERENCE, source);

            try {
                List<Rating> gradings =
                    this.ratingsManager
                        .getRatings(queryMap, 0, 1, RatingsManager.RatingQueryField.UPDATED_DATE, false);
                result = !gradings.isEmpty();
                this.likeExistCache.set(getExistCacheKey(source, target), result);
            } catch (RatingsException e) {
                throw new LikeException("Error while checking if grading exists", e);
            }
        }
        return result;
    }

    @Override
    public List<UserReference> getLikers(EntityReference target, int offset, int limit) throws LikeException
    {
        Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_TYPE, target.getType());
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, target);
        try {
            List<Rating> gradings = this.ratingsManager
                .getRatings(queryMap, offset, limit, RatingsManager.RatingQueryField.UPDATED_DATE, false);
            return gradings.stream().map(Rating::getAuthor).collect(Collectors.toList());
        } catch (RatingsException e) {
            throw new LikeException(String.format("Error while getting likers of [%s]", target), e);
        }
    }

    @Override
    public Right getLikeRight()
    {
        return this.likeRight;
    }
}
