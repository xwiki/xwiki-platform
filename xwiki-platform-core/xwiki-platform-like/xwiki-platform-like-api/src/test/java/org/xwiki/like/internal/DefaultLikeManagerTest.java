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

import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.like.LikeConfiguration;
import org.xwiki.like.LikeEvent;
import org.xwiki.like.LikeException;
import org.xwiki.like.LikedEntity;
import org.xwiki.like.UnlikeEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultLikeManager}.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@ComponentTest
public class DefaultLikeManagerTest
{
    @InjectMockComponents
    private DefaultLikeManager defaultLikeManager;

    @MockComponent
    @Named("like/solr")
    private RatingsManager ratingsManager;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private LikeConfiguration likeConfiguration;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Cache<LikedEntity> likedEntityCache;

    private Right likeRight;

    @Mock
    private UserReference userReference;

    private DocumentReference userDocReference;

    private DocumentReference target;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        this.likeRight = mock(Right.class);
        when(this.authorizationManager.register(LikeRight.INSTANCE)).thenReturn(this.likeRight);
        when(this.likeConfiguration.getLikeCacheCapacity()).thenReturn(500);
        this.likedEntityCache = mock(Cache.class);
        CacheConfiguration cacheConfiguration = new LRUCacheConfiguration("xwiki.like.cache", 500);
        when(this.cacheManager.createNewCache(cacheConfiguration)).thenReturn((Cache) this.likedEntityCache);
    }

    @BeforeEach
    void setup()
    {
        this.userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.userReferenceSerializer.serialize(this.userReference)).thenReturn(this.userDocReference);

        this.target = new DocumentReference("xwiki", "Foo", "WebHome");
        when(this.entityReferenceSerializer.serialize(this.target)).thenReturn("xwiki:Foo.WebHome");
    }

    /**
     * Prepare ratings for getLikes.
     * @param size wanted size
     * @return the list of ratings to be returned by getRatings and the list of expected user references
     */
    private Pair<List<Rating>, List<UserReference>> prepareRatings(int size)
    {
        List<UserReference> expectedUserReferences = new ArrayList<>();

        List<Rating> ratingList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Rating rating = mock(Rating.class);
            DocumentReference userDoc = new DocumentReference("xwiki", "XWiki",
                String.format("RatingUser_%s", i));
            when(rating.getAuthor()).thenReturn(userDoc);

            UserReference ratingUserReference = mock(UserReference.class);
            when(userReferenceResolver.resolve(userDoc)).thenReturn(ratingUserReference);

            ratingList.add(rating);
            expectedUserReferences.add(ratingUserReference);
        }

        return Pair.of(ratingList, expectedUserReferences);
    }

    @Test
    void saveLike() throws RatingsException, LikeException
    {
        when(this.authorizationManager.hasAccess(this.likeRight, this.userDocReference, target)).thenReturn(true);

        Pair<List<Rating>, List<UserReference>> listLikedEntityPair = this.prepareRatings(3);
        LikedEntity expectedLikeEntity = new DefaultLikedEntity(target);
        expectedLikeEntity.addAllLikers(listLikedEntityPair.getRight());
        when(this.ratingsManager.getRatings(target, 0, 0, true)).thenReturn(listLikedEntityPair.getLeft());
        assertEquals(expectedLikeEntity, this.defaultLikeManager.saveLike(this.userReference, target));
        verify(this.ratingsManager).setRating(target, this.userDocReference, 1);
        verify(this.observationManager).notify(any(LikeEvent.class), eq(this.userReference), eq(expectedLikeEntity));
        verify(this.likedEntityCache).remove("xwiki:Foo.WebHome");
        verify(this.likedEntityCache).set("xwiki:Foo.WebHome", expectedLikeEntity);
    }

    @Test
    void saveLikeNoAuthorization() throws RatingsException
    {
        when(this.authorizationManager.hasAccess(this.likeRight, this.userDocReference, target)).thenReturn(false);

        LikeException likeException = assertThrows(LikeException.class, () -> {
            this.defaultLikeManager.saveLike(this.userReference, target);
        });
        assertEquals("User [userReference] is not authorized to perform a like on [xwiki:Foo.WebHome]",
            likeException.getMessage());
        verify(this.ratingsManager, never()).setRating(target, this.userDocReference, 1);
        verify(this.observationManager, never()).notify(any(LikeEvent.class), eq(this.userReference), any());
        verify(this.likedEntityCache, never()).remove(any());
        verify(this.likedEntityCache, never()).set(any(), any());
        verify(this.likedEntityCache, never()).get(any());
    }

    @Test
    void getEntityLikes() throws RatingsException, LikeException
    {
        LikedEntity expectedLikedEntity = new DefaultLikedEntity(target);
        Pair<List<Rating>, List<UserReference>> listLikedEntityPair = this.prepareRatings(125);
        when(this.ratingsManager.getRatings(target, 0, 0, true)).thenReturn(listLikedEntityPair.getLeft());
        expectedLikedEntity.addAllLikers(listLikedEntityPair.getRight());

        LikedEntity obtainedLikeEntity = this.defaultLikeManager.getEntityLikes(target);
        assertEquals(expectedLikedEntity, obtainedLikeEntity);
        verify(this.likedEntityCache).set("xwiki:Foo.WebHome", expectedLikedEntity);
    }

    @Test
    void getLikeRight()
    {
        assertSame(this.likeRight, this.defaultLikeManager.getLikeRight());
    }

    /**
     * Prepare ratings for getUserLikes.
     * @param size wanted size
     * @return the list of ratings to be returned by getRatings and the list of expected target document references
     */
    private Pair<List<Rating>, List<DocumentReference>> prepareUserRatings(int size)
    {
        List<DocumentReference> expectedDocReferences = new ArrayList<>();

        List<Rating> ratingList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Rating rating = mock(Rating.class);
            DocumentReference ratedDoc = new DocumentReference("xwiki",
                String.format("RatedDoc_%s", i), "WebHome");
            when(rating.getDocumentReference()).thenReturn(ratedDoc);

            ratingList.add(rating);
            expectedDocReferences.add(ratedDoc);
        }

        return Pair.of(ratingList, expectedDocReferences);
    }

    @Test
    void getUserLikes() throws RatingsException, LikeException
    {
        List<LikedEntity> expectedLikes = new ArrayList<>();
        Pair<List<Rating>, List<DocumentReference>> expectedRatings = prepareUserRatings(42);
        when(this.ratingsManager.getRatings(this.userReference, 0, 0, true)).thenReturn(expectedRatings.getLeft());

        for (DocumentReference documentReference : expectedRatings.getRight()) {
            expectedLikes.add(new DefaultLikedEntity(documentReference));
        }

        assertEquals(expectedLikes, this.defaultLikeManager.getUserLikes(this.userReference));
    }

    @Test
    void isLiked() throws LikeException, RatingsException
    {
        assertFalse(this.defaultLikeManager.isLiked(this.userReference, target));

        when(this.ratingsManager.getRating(target, this.userDocReference)).thenReturn(mock(Rating.class));
        assertTrue(this.defaultLikeManager.isLiked(this.userReference, target));
    }

    @Test
    void removeLike() throws LikeException, RatingsException
    {
        when(this.authorizationManager.hasAccess(this.likeRight, this.userDocReference, target)).thenReturn(true);

        assertFalse(this.defaultLikeManager.removeLike(this.userReference, target));

        Rating ratingToRemove = mock(Rating.class);
        when(this.ratingsManager.getRating(target, this.userDocReference)).thenReturn(ratingToRemove);
        Pair<List<Rating>, List<UserReference>> ratings = prepareRatings(3);

        when(this.ratingsManager.getRatings(target, 0, 0, true)).thenReturn(ratings.getLeft());
        DefaultLikedEntity expectedEntity = new DefaultLikedEntity(target);
        expectedEntity.addAllLikers(ratings.getRight());

        assertTrue(this.defaultLikeManager.removeLike(this.userReference, target));
        verify(this.ratingsManager).removeRating(ratingToRemove);
        verify(this.observationManager).notify(any(UnlikeEvent.class), eq(this.userReference), eq(expectedEntity));
    }

    @Test
    void removeLikeNotAuthorized() throws RatingsException
    {
        when(this.authorizationManager.hasAccess(this.likeRight, this.userDocReference, target)).thenReturn(false);

        LikeException likeException = assertThrows(LikeException.class, () -> {
            this.defaultLikeManager.removeLike(this.userReference, target);
        });
        assertEquals("User [xwiki:XWiki.User] is not authorized to remove a like on [xwiki:Foo.WebHome].",
            likeException.getMessage());
        verify(this.ratingsManager, never()).removeRating(any());
        verify(this.observationManager, never()).notify(any(UnlikeEvent.class), eq(this.userReference), any());
    }
}
