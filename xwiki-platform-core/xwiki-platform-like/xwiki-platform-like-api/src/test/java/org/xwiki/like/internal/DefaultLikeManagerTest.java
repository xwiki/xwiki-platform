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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.like.LikeConfiguration;
import org.xwiki.like.LikeEvent;
import org.xwiki.like.LikeException;
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
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
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
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceDocumentSerializer;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceStringSerializer;

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

    @MockComponent
    private RatingsManagerFactory ratingsManagerFactory;

    private RatingsManager ratingsManager;

    private Cache<Long> likedEntityCache;

    private Cache<Boolean> likeExistCache;

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
        this.likeExistCache = mock(Cache.class);
        when(this.cacheManager.createNewCache(any())).then(invocationOnMock -> {
            CacheConfiguration cacheConfiguration = invocationOnMock.getArgument(0);
            if (cacheConfiguration.getConfigurationId().equals("xwiki.like.count.cache")) {
                return this.likedEntityCache;
            } else if (cacheConfiguration.getConfigurationId().equals("xwiki.like.exist.cache")) {
                return this.likeExistCache;
            } else {
                throw new AssertionError("Cache key is wrong.");
            }
        });
        this.ratingsManager = mock(RatingsManager.class);
        when(this.ratingsManagerFactory.getRatingsManager(LikeRatingsConfiguration.RANKING_MANAGER_HINT))
            .thenReturn(this.ratingsManager);
    }

    @BeforeEach
    void setup()
    {
        this.userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.userReferenceDocumentSerializer.serialize(this.userReference)).thenReturn(this.userDocReference);
        when(this.userReferenceStringSerializer.serialize(this.userReference)).thenReturn("xwiki:XWiki.User");

        this.target = new DocumentReference("xwiki", "Foo", "WebHome");
        when(this.entityReferenceSerializer.serialize(this.target)).thenReturn("xwiki:Foo.WebHome");
    }

    @Test
    void saveLike() throws Exception
    {
        when(this.authorizationManager.hasAccess(this.likeRight, this.userDocReference, target)).thenReturn(true);

        Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_TYPE, target.getType());
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, target);
        when(this.ratingsManager.countRatings(queryMap)).thenReturn(42L);

        assertEquals(42L, this.defaultLikeManager.saveLike(this.userReference, target));
        verify(this.ratingsManager).saveRating(target, this.userReference, 1);
        verify(this.observationManager).notify(any(LikeEvent.class), eq(this.userReference), eq(target));
        verify(this.likedEntityCache).remove("xwiki:Foo.WebHome");
        verify(this.likedEntityCache).set("xwiki:Foo.WebHome", 42L);
        verify(this.likeExistCache).set("xwiki:XWiki.User_xwiki:Foo.WebHome", true);
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
        verify(this.ratingsManager, never()).saveRating(target, this.userReference, 1);
        verify(this.observationManager, never()).notify(any(LikeEvent.class), eq(this.userReference), any());
        verify(this.likedEntityCache, never()).remove(any());
        verify(this.likedEntityCache, never()).set(any(), any());
        verify(this.likedEntityCache, never()).get(any());
    }

    @Test
    void getEntityLikes() throws Exception
    {
        Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_TYPE, target.getType());
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, target);
        when(this.ratingsManager.countRatings(queryMap)).thenReturn(43L);

        assertEquals(43L, this.defaultLikeManager.getEntityLikes(target));
        verify(this.likedEntityCache).set("xwiki:Foo.WebHome", 43L);
    }

    @Test
    void getLikeRight()
    {
        assertSame(this.likeRight, this.defaultLikeManager.getLikeRight());
    }

    @Test
    void getUserLikes() throws Exception
    {
        Rating grading1 = mock(Rating.class);
        EntityReference entityReference1 = mock(EntityReference.class);
        when(grading1.getReference()).thenReturn(entityReference1);

        Rating grading2 = mock(Rating.class);
        EntityReference entityReference2 = mock(EntityReference.class);
        when(grading2.getReference()).thenReturn(entityReference2);

        Rating grading3 = mock(Rating.class);
        EntityReference entityReference3 = mock(EntityReference.class);
        when(grading3.getReference()).thenReturn(entityReference3);

        when(this.ratingsManager.getRatings(
            Collections.singletonMap(RatingsManager.RatingQueryField.USER_REFERENCE, this.userReference),
            5,
            3,
            RatingsManager.RatingQueryField.UPDATED_DATE,
            false)).thenReturn(Arrays.asList(grading1, grading2, grading3));

        assertEquals(Arrays.asList(entityReference1, entityReference2, entityReference3),
            this.defaultLikeManager.getUserLikes(this.userReference, 5, 3));
    }

    @Test
    void isLiked() throws Exception
    {
        assertFalse(this.defaultLikeManager.isLiked(this.userReference, target));

        Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_TYPE, target.getType());
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, target);
        queryMap.put(RatingsManager.RatingQueryField.USER_REFERENCE, this.userReference);

        when(this.ratingsManager.getRatings(queryMap, 0, 1, RatingsManager.RatingQueryField.UPDATED_DATE, false)).
            thenReturn(Collections.singletonList(mock(Rating.class)));
        assertTrue(this.defaultLikeManager.isLiked(this.userReference, target));
        verify(this.likeExistCache).set("xwiki:XWiki.User_xwiki:Foo.WebHome", true);
    }

    @Test
    void removeLike() throws Exception
    {
        when(this.authorizationManager.hasAccess(this.likeRight, this.userDocReference, target)).thenReturn(true);
        assertFalse(this.defaultLikeManager.removeLike(this.userReference, target));

        Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_TYPE, target.getType());
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, target);
        queryMap.put(RatingsManager.RatingQueryField.USER_REFERENCE, this.userReference);
        Rating grading = mock(Rating.class);
        when(grading.getId()).thenReturn("grading423");

        when(this.ratingsManager.getRatings(queryMap, 0, 1, RatingsManager.RatingQueryField.UPDATED_DATE, false))
            .thenReturn(Collections.singletonList(grading));
        when(this.ratingsManager.removeRating("grading423")).thenReturn(true);

        assertTrue(this.defaultLikeManager.removeLike(this.userReference, target));
        verify(this.ratingsManager).removeRating("grading423");
        verify(this.observationManager).notify(any(UnlikeEvent.class), eq(this.userReference), eq(this.target));
        verify(this.likeExistCache).set("xwiki:XWiki.User_xwiki:Foo.WebHome", false);
        verify(this.likedEntityCache).remove("xwiki:Foo.WebHome");
    }

    @Test
    void removeLikeNotAuthorized() throws Exception
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

    @Test
    void getLikers() throws Exception
    {
        Rating grading1 = mock(Rating.class);
        UserReference userReference1 = mock(UserReference.class);
        when(grading1.getAuthor()).thenReturn(userReference1);

        Rating grading2 = mock(Rating.class);
        UserReference userReference2 = mock(UserReference.class);
        when(grading2.getAuthor()).thenReturn(userReference2);

        Rating grading3 = mock(Rating.class);
        UserReference userReference3 = mock(UserReference.class);
        when(grading3.getAuthor()).thenReturn(userReference3);

        Map<RatingsManager.RatingQueryField, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_TYPE, target.getType());
        queryMap.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, target);

        when(this.ratingsManager.getRatings(queryMap, 12, 4, RatingsManager.RatingQueryField.UPDATED_DATE, false))
            .thenReturn(Arrays.asList(grading1, grading2, grading3));
        assertEquals(Arrays.asList(userReference1, userReference2, userReference3),
            this.defaultLikeManager.getLikers(this.target, 12, 4));
    }
}
