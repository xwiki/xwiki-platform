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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.like.events.LikeEvent;
import org.xwiki.like.LikeException;
import org.xwiki.like.LikeManager;
import org.xwiki.like.events.UnlikeEvent;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
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
    private AuthorizationManager authorizationManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private EntityReferenceFactory entityReferenceFactory;

    @Inject
    private LikeManagerCacheHelper likeManagerCacheHelper;

    private RatingsManager ratingsManager;

    private Right likeRight;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.likeRight = this.authorizationManager.register(LikeRight.INSTANCE);
        } catch (UnableToRegisterRightException e) {
            throw new InitializationException("Error while registering the Like right.", e);
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
        try {
            this.authorizationManager.unregister(this.likeRight);
        } catch (AuthorizationException e) {
            throw new ComponentLifecycleException("Error while unregistering like right", e);
        }
    }

    @Override
    public long saveLike(UserReference source, EntityReference target) throws LikeException
    {
        DocumentReference userDoc = this.userReferenceDocumentSerializer.serialize(source);
        if (this.authorizationManager.hasAccess(this.likeRight, userDoc, target)) {
            try {
                EntityReference dedupTarget = this.entityReferenceFactory.getReference(target);
                this.ratingsManager.saveRating(dedupTarget, source, DEFAULT_LIKE_VOTE);
                this.likeManagerCacheHelper.removeCount(dedupTarget);
                this.likeManagerCacheHelper.setExist(source, dedupTarget, true);
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
        Long result = this.likeManagerCacheHelper.getCount(target);
        if (result == null) {
            Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
            queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, target);
            try {
                result = this.ratingsManager.countRatings(queryMap);
                this.likeManagerCacheHelper.setCount(target, result);
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
                    this.likeManagerCacheHelper.removeCount(dedupTarget);
                    this.likeManagerCacheHelper.setExist(source, dedupTarget, false);
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
        Boolean result = this.likeManagerCacheHelper.isExist(source, target);
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
                this.likeManagerCacheHelper.setExist(source, dedupTarget, result);
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
        this.likeManagerCacheHelper.clearCache(target);
    }

    @Override
    public void clearCache()
    {
        this.likeManagerCacheHelper.clearCache();
    }
}
