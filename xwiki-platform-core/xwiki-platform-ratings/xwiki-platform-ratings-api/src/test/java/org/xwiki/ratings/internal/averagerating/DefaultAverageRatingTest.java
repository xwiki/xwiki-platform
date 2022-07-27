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
package org.xwiki.ratings.internal.averagerating;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.AverageRating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AverageRating}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
public class DefaultAverageRatingTest
{
    @Test
    void simpleConstructor()
    {
        Date currentDate = new Date();
        DefaultAverageRating defaultAverageRank = new DefaultAverageRating("myId");
        assertEquals("myId", defaultAverageRank.getId());
        assertEquals(0, defaultAverageRank.getNbVotes());
        assertEquals(0, defaultAverageRank.getAverageVote(), 0);
        assertTrue(currentDate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
    }

    @Test
    void cloneConstructor()
    {
        AverageRating customRank = new AverageRating()
        {
            @Override
            public String getId()
            {
                return "someId";
            }

            @Override
            public String getManagerId()
            {
                return "myManagerId";
            }

            @Override
            public EntityReference getReference()
            {
                return new EntityReference("Foobar", EntityType.ATTACHMENT);
            }

            @Override
            public float getAverageVote()
            {
                return 0.23f;
            }

            @Override
            public int getNbVotes()
            {
                return 1343;
            }

            @Override
            public int getScaleUpperBound()
            {
                return 3;
            }

            @Override
            public Date getUpdatedAt()
            {
                return new Date(24);
            }

            @Override
            public AverageRating updateRating(int oldRating, int newRating)
            {
                return this;
            }

            @Override
            public AverageRating removeRating(int rating)
            {
                return this;
            }

            @Override
            public AverageRating addRating(int rating)
            {
                return this;
            }
        };

        DefaultAverageRating defaultAverageRank = new DefaultAverageRating(customRank);
        assertNotEquals(defaultAverageRank, customRank);
        assertEquals("someId", defaultAverageRank.getId());
        assertEquals("myManagerId", defaultAverageRank.getManagerId());
        assertEquals(new EntityReference("Foobar", EntityType.ATTACHMENT), defaultAverageRank.getReference());
        assertEquals(EntityType.ATTACHMENT, defaultAverageRank.getReference().getType());
        assertEquals(0.23f, defaultAverageRank.getAverageVote(), 0);
        assertEquals(1343, defaultAverageRank.getNbVotes());
        assertEquals(3, defaultAverageRank.getScaleUpperBound());
        assertEquals(new Date(24), defaultAverageRank.getUpdatedAt());
        assertEquals(defaultAverageRank, new DefaultAverageRating(defaultAverageRank));
    }

    @Test
    void addVote()
    {
        DefaultAverageRating defaultAverageRank = new DefaultAverageRating("myId");
        assertEquals("myId", defaultAverageRank.getId());
        assertEquals(0, defaultAverageRank.getNbVotes());
        assertEquals(0, defaultAverageRank.getAverageVote(), 0);

        Date beforeFirstUpdate = new Date();
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));

        defaultAverageRank.addRating(4);
        assertEquals(4, defaultAverageRank.getAverageVote(), 0);
        assertEquals(1, defaultAverageRank.getNbVotes());
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));

        defaultAverageRank.addRating(1);
        defaultAverageRank.addRating(3);
        defaultAverageRank.addRating(0);
        defaultAverageRank.addRating(3);
        defaultAverageRank.addRating(2);
        assertEquals(6, defaultAverageRank.getNbVotes());
        assertEquals(2.166667, defaultAverageRank.getAverageVote(), 0.000001);
    }

    @Test
    void removeVote()
    {
        DefaultAverageRating defaultAverageRank = new DefaultAverageRating("myId")
            .setAverageVote(13.0f / 6)
            .setTotalVote(6);
        assertEquals("myId", defaultAverageRank.getId());
        assertEquals(6, defaultAverageRank.getNbVotes());
        assertEquals(2.166667, defaultAverageRank.getAverageVote(), 0.000001);

        Date beforeFirstUpdate = new Date();
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));

        defaultAverageRank.removeRating(0);
        assertEquals(2.6f, defaultAverageRank.getAverageVote(), 0);
        assertEquals(5, defaultAverageRank.getNbVotes());
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));

        // check that if we remove all votes the average is set to 0.
        defaultAverageRank.removeRating(0);
        defaultAverageRank.removeRating(0);
        defaultAverageRank.removeRating(0);
        defaultAverageRank.removeRating(0);
        defaultAverageRank.removeRating(0);
        assertEquals(0f, defaultAverageRank.getAverageVote(), 0);
        assertEquals(0, defaultAverageRank.getNbVotes());
    }

    @Test
    void updateVote()
    {
        DefaultAverageRating defaultAverageRank = new DefaultAverageRating("myId")
            .setAverageVote(13.0f / 6)
            .setTotalVote(6);
        assertEquals("myId", defaultAverageRank.getId());
        assertEquals(6, defaultAverageRank.getNbVotes());
        assertEquals(2.166667, defaultAverageRank.getAverageVote(), 0.000001);

        Date beforeFirstUpdate = new Date();
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));

        defaultAverageRank.updateRating(0, 5);
        assertEquals(3, defaultAverageRank.getAverageVote(), 0);
        assertEquals(6, defaultAverageRank.getNbVotes());
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));
    }
}
