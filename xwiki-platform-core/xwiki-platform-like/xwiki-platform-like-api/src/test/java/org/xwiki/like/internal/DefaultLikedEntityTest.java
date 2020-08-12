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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.Rating;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultLikedEntity}
 *
 * @version $Id$
 * @since 12.7RC1
 */
public class DefaultLikedEntityTest
{
    @Test
    void addRemoveLiker()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Bar");
        DefaultLikedEntity defaultLikedEntity = new DefaultLikedEntity(entityReference);
        assertEquals(0, defaultLikedEntity.getLikeNumber());
        assertEquals(new ArrayList<>(), defaultLikedEntity.getLikers());

        UserReference userReference1 = mock(UserReference.class);
        UserReference userReference2 = mock(UserReference.class);
        UserReference userReference3 = mock(UserReference.class);
        defaultLikedEntity.addLiker(userReference1);
        defaultLikedEntity.addLiker(userReference2);
        defaultLikedEntity.addLiker(userReference3);
        assertEquals(3, defaultLikedEntity.getLikeNumber());
        assertEquals(Arrays.asList(userReference1, userReference2, userReference3), defaultLikedEntity.getLikers());

        defaultLikedEntity.removeLiker(userReference2);
        assertEquals(2, defaultLikedEntity.getLikeNumber());
        assertEquals(Arrays.asList(userReference1, userReference3), defaultLikedEntity.getLikers());
    }

    @Test
    void addAllRatings()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Bar");
        DefaultLikedEntity defaultLikedEntity = new DefaultLikedEntity(entityReference);
        assertEquals(0, defaultLikedEntity.getLikeNumber());
        assertEquals(new ArrayList<>(), defaultLikedEntity.getLikers());

        Rating rating1 = mock(Rating.class);
        Rating rating2 = mock(Rating.class);
        Rating rating3 = mock(Rating.class);
        Rating rating4 = mock(Rating.class);

        DocumentReference userDoc1 = new DocumentReference("xwiki", "XWiki", "User1");
        DocumentReference userDoc2 = new DocumentReference("xwiki", "XWiki", "User2");
        DocumentReference userDoc3 = new DocumentReference("xwiki", "XWiki", "User3");
        DocumentReference userDoc4 = new DocumentReference("xwiki", "XWiki", "User4");
        when(rating1.getAuthor()).thenReturn(userDoc1);
        when(rating2.getAuthor()).thenReturn(userDoc2);
        when(rating3.getAuthor()).thenReturn(userDoc3);
        when(rating4.getAuthor()).thenReturn(userDoc4);

        UserReference userReference1 = mock(UserReference.class);
        UserReference userReference2 = mock(UserReference.class);
        UserReference userReference3 = mock(UserReference.class);
        UserReference userReference4 = mock(UserReference.class);
        UserReferenceResolver<DocumentReference> userReferenceResolver = mock(UserReferenceResolver.class);
        when(userReferenceResolver.resolve(userDoc1)).thenReturn(userReference1);
        when(userReferenceResolver.resolve(userDoc2)).thenReturn(userReference2);
        when(userReferenceResolver.resolve(userDoc3)).thenReturn(userReference3);
        when(userReferenceResolver.resolve(userDoc4)).thenReturn(userReference4);

        List<Rating> ratingList = Arrays.asList(rating1, rating2, rating3, rating4);
        defaultLikedEntity.addAllRatings(ratingList, userReferenceResolver);

        assertEquals(entityReference, defaultLikedEntity.getEntityReference());
        assertEquals(4, defaultLikedEntity.getLikeNumber());
        assertEquals(Arrays.asList(userReference1, userReference2, userReference3, userReference4),
            defaultLikedEntity.getLikers());
    }

    @Test
    void compareTo()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Bar");
        DefaultLikedEntity likedEntity1 = new DefaultLikedEntity(entityReference);
        assertEquals(0, likedEntity1.getLikeNumber());

        entityReference = new DocumentReference("xwiki", "Foo", "Baz");
        DefaultLikedEntity likedEntity2 = new DefaultLikedEntity(entityReference);
        assertEquals(0, likedEntity2.getLikeNumber());

        assertEquals(0, likedEntity1.compareTo(likedEntity2));
        assertEquals(0, likedEntity2.compareTo(likedEntity1));

        likedEntity1.addLiker(mock(UserReference.class));
        assertEquals(1, likedEntity1.getLikeNumber());

        assertEquals(1, likedEntity1.compareTo(likedEntity2));
        assertEquals(-1, likedEntity2.compareTo(likedEntity1));

        likedEntity1.addLiker(mock(UserReference.class));
        likedEntity1.addLiker(mock(UserReference.class));
        assertEquals(3, likedEntity1.getLikeNumber());

        likedEntity2.addLiker(mock(UserReference.class));
        assertEquals(1, likedEntity2.getLikeNumber());

        assertEquals(1, likedEntity1.compareTo(likedEntity2));
        assertEquals(-1, likedEntity2.compareTo(likedEntity1));

        likedEntity2.addLiker(mock(UserReference.class));
        likedEntity2.addLiker(mock(UserReference.class));
        assertEquals(3, likedEntity2.getLikeNumber());
        assertEquals(0, likedEntity1.compareTo(likedEntity2));
        assertEquals(0, likedEntity2.compareTo(likedEntity1));

        likedEntity2.addLiker(mock(UserReference.class));
        assertEquals(4, likedEntity2.getLikeNumber());

        assertEquals(-1, likedEntity1.compareTo(likedEntity2));
        assertEquals(1, likedEntity2.compareTo(likedEntity1));
    }

    @Test
    void equals()
    {
        EntityReference entityReference1 = new DocumentReference("xwiki", "Foo", "Bar");
        DefaultLikedEntity likedEntity1 = new DefaultLikedEntity(entityReference1);

        EntityReference entityReference2 = new DocumentReference("xwiki", "Foo", "Baz");
        DefaultLikedEntity likedEntity2 = new DefaultLikedEntity(entityReference2);

        DefaultLikedEntity likedEntity3 = new DefaultLikedEntity(entityReference1);

        assertNotEquals(likedEntity1, likedEntity2);
        assertEquals(likedEntity1, likedEntity3);

        UserReference userReference = mock(UserReference.class);
        likedEntity1.addLiker(userReference);
        assertNotEquals(likedEntity1, likedEntity3);

        likedEntity3.addLiker(userReference);
        assertEquals(likedEntity1, likedEntity3);
    }
}
