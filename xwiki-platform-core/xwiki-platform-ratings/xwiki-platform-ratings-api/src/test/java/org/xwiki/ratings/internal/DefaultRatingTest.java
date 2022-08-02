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
package org.xwiki.ratings.internal;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.Rating;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultRating}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
public class DefaultRatingTest
{
    @Test
    void simpleConstructor()
    {
        Date currentDate = new Date();
        DefaultRating defaultRating = new DefaultRating("myId");
        assertEquals("myId", defaultRating.getId());
        assertTrue(currentDate.toInstant()
            .isAfter(defaultRating.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));
        assertTrue(currentDate.toInstant()
            .isAfter(defaultRating.getCreatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));
    }

    @Test
    void cloneConstructor()
    {
        UserReference userReference = mock(UserReference.class);
        Rating customRating = new Rating()
        {
            @Override
            public String getId()
            {
                return "SOmething";
            }

            @Override
            public String getManagerId()
            {
                return "Foobarbar";
            }

            @Override
            public EntityReference getReference()
            {
                return new EntityReference("Aahahha", EntityType.CLASS_PROPERTY);
            }

            @Override
            public UserReference getAuthor()
            {
                return userReference;
            }

            @Override
            public Date getCreatedAt()
            {
                return new Date(12);
            }

            @Override
            public Date getUpdatedAt()
            {
                return new Date(243);
            }

            @Override
            public int getVote()
            {
                return 42;
            }

            @Override
            public int getScaleUpperBound()
            {
                return 43;
            }
        };

        DefaultRating defaultRating = new DefaultRating(customRating);
        assertNotEquals(defaultRating, customRating);
        assertEquals("SOmething", defaultRating.getId());
        assertEquals("Foobarbar", defaultRating.getManagerId());
        assertEquals(new EntityReference("Aahahha", EntityType.CLASS_PROPERTY), defaultRating.getReference());
        assertEquals(EntityType.CLASS_PROPERTY, defaultRating.getReference().getType());
        assertSame(userReference, defaultRating.getAuthor());
        assertEquals(new Date(12), defaultRating.getCreatedAt());
        assertEquals(new Date(243), defaultRating.getUpdatedAt());
        assertEquals(42, defaultRating.getVote());
        assertEquals(43, defaultRating.getScaleUpperBound());

        assertEquals(defaultRating, new DefaultRating(defaultRating));
    }
}
