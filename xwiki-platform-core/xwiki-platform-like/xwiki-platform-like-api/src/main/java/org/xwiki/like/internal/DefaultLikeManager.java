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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.like.LikeConfiguration;
import org.xwiki.like.LikeEvent;
import org.xwiki.like.LikeException;
import org.xwiki.like.LikeManager;
import org.xwiki.like.LikedEntity;
import org.xwiki.like.UnlikeEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.UnableToRegisterRightException;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Default implementation of {@link LikeManager} based on {@link org.xwiki.ratings.RatingsManager}.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@Component
@Singleton
public class DefaultLikeManager implements LikeManager, Initializable
{
    private static final int DEFAULT_LIKE_VOTE = 1;
    private static final int RATING_PAGINATION = 100;
    private static final String NOT_YET_IMPLEMENTED_MSG =
        "Like is not implemented yet for other references than Document.";

    @Inject
    @Named("like/solr")
    private RatingsManager ratingsManager;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

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

    private Cache<LikedEntity> likedEntityCache;

    private Right likeRight;

    @Override
    public void initialize() throws InitializationException
    {
        int likeCacheCapacity = this.likeConfiguration.getLikeCacheCapacity();
        try {
            this.likedEntityCache = this.cacheManager.createNewCache(
                new LRUCacheConfiguration("xwiki.like.cache", likeCacheCapacity));
            this.likeRight = this.authorizationManager.register(LikeRight.INSTANCE);
        } catch (UnableToRegisterRightException e) {
            throw new InitializationException("Error while registering the Like right.", e);
        } catch (CacheException e) {
            throw new InitializationException("Error while creating the cache for likes.", e);
        }
    }

    @Override
    public LikedEntity saveLike(UserReference source, EntityReference target) throws LikeException
    {
        DocumentReference userDoc = this.userReferenceSerializer.serialize(source);
        if (this.authorizationManager.hasAccess(this.likeRight, userDoc, target)) {
            if (target instanceof DocumentReference) {
                DocumentReference targetDocument = (DocumentReference) target;
                try {
                    this.ratingsManager.setRating(targetDocument, userDoc, DEFAULT_LIKE_VOTE);
                    this.likedEntityCache.remove(this.entityReferenceSerializer.serialize(target));
                    LikedEntity entityLikes = getEntityLikes(target);
                    this.observationManager.notify(new LikeEvent(), source, entityLikes);
                    return entityLikes;
                } catch (RatingsException e) {
                    throw new LikeException(String.format("Error while liking document [%s]", targetDocument), e);
                }
            } else {
                throw new LikeException(NOT_YET_IMPLEMENTED_MSG);
            }
        } else {
            throw new LikeException(String.format("User [%s] is not authorized to perform a like on [%s]",
                source, target));
        }
    }

    @Override
    public List<LikedEntity> getUserLikes(UserReference source) throws LikeException
    {
        List<LikedEntity> result = new ArrayList<>();
        List<Rating> ratings;

        try {
            ratings = this.ratingsManager.getRatings(source, 0, 0, true);
            for (Rating rating : ratings) {
                LikedEntity likedEntity =
                    this.likedEntityCache.get(this.entityReferenceSerializer.serialize(rating.getDocumentReference()));
                if (likedEntity == null) {
                    result.add(new DefaultLikedEntity(rating.getDocumentReference()));
                } else {
                    result.add(likedEntity);
                }
            }
        } catch (RatingsException e) {
            throw
                new LikeException(String.format("Error while getting ratings for user [%s]", source), e);
        }
        return result;
    }

    @Override
    public LikedEntity getEntityLikes(EntityReference target) throws LikeException
    {
        LikedEntity likedEntity = this.likedEntityCache.get(this.entityReferenceSerializer.serialize(target));
        if (likedEntity != null) {
            return likedEntity;
        } else {
            if (target instanceof DocumentReference) {
                DocumentReference targetDoc = (DocumentReference) target;
                DefaultLikedEntity result = new DefaultLikedEntity(target);
                List<Rating> ratings;
                try {
                    ratings = this.ratingsManager.getRatings(targetDoc, 0, 0, true);
                    result.addAllRatings(ratings, this.userReferenceResolver);
                } catch (RatingsException e) {
                    throw
                        new LikeException(String.format("Error while getting ratings for document [%s]", targetDoc), e);
                }
                this.likedEntityCache.set(this.entityReferenceSerializer.serialize(target), result);
                return result;
            } else {
                throw new LikeException(NOT_YET_IMPLEMENTED_MSG);
            }
        }
    }

    @Override
    public boolean removeLike(UserReference source, EntityReference target) throws LikeException
    {
        this.likedEntityCache.remove(this.entityReferenceSerializer.serialize(target));
        DocumentReference userDoc = this.userReferenceSerializer.serialize(source);
        if (this.authorizationManager.hasAccess(this.getLikeRight(), userDoc, target)) {
            if (target instanceof DocumentReference) {
                DocumentReference likedDoc = (DocumentReference) target;
                try {
                    Rating rating = this.ratingsManager.getRating(likedDoc, userDoc);
                    if (rating != null) {
                        this.ratingsManager.removeRating(rating);
                        this.likedEntityCache.remove(this.entityReferenceSerializer.serialize(target));
                        LikedEntity entityLikes = getEntityLikes(target);
                        this.observationManager.notify(new UnlikeEvent(), source, entityLikes);
                        return true;
                    }
                } catch (RatingsException e) {
                    throw new LikeException("Error while removing rating", e);
                }
            } else {
                throw new LikeException(NOT_YET_IMPLEMENTED_MSG);
            }
        } else {
            throw new LikeException(
                String.format("User [%s] is not authorized to remove a like on [%s].",
                    userDoc, target));
        }
        return false;
    }

    @Override
    public boolean isLiked(UserReference source, EntityReference target) throws LikeException
    {
        LikedEntity likedEntity = this.likedEntityCache.get(this.entityReferenceSerializer.serialize(target));
        if (likedEntity != null) {
            return likedEntity.getLikers().contains(source);
        } else {
            DocumentReference userDoc = this.userReferenceSerializer.serialize(source);
            if (target instanceof DocumentReference) {
                DocumentReference likedDoc = (DocumentReference) target;
                try {
                    Rating rating = this.ratingsManager.getRating(likedDoc, userDoc);
                    return (rating != null);
                } catch (RatingsException e) {
                    throw new LikeException("Error while loading rating", e);
                }
            } else {
                throw new LikeException(NOT_YET_IMPLEMENTED_MSG);
            }
        }
    }

    @Override
    public Right getLikeRight()
    {
        return this.likeRight;
    }
}
