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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.events.UpdatedAverageRatingEvent;
import org.xwiki.ratings.events.UpdatingAverageRatingEvent;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AbstractAverageRatingManager}.
 *
 * @version $Id$
 */
public class AbstractAverageRatingManagerTest
{
    private RatingsManager ratingsManager;
    private AverageRating averageRating;
    private EntityReference entityReference;
    private ObservationManager observationManager;

    @Component
    public class DumbAsbtractAverageRatingsManager extends AbstractAverageRatingManager {
        boolean isSaved;

        @Override
        protected void saveAverageRating(AverageRating averageRating) throws RatingsException
        {
            if (AbstractAverageRatingManagerTest.this.averageRating.equals(averageRating)
                || (averageRating.getId() != null
                && averageRating.getId().equals(AbstractAverageRatingManagerTest.this.averageRating.getId()))) {
                this.isSaved = true;
            }
        }

        @Override
        public AverageRating getAverageRating(EntityReference entityReference) throws RatingsException
        {
            if (AbstractAverageRatingManagerTest.this.entityReference.equals(entityReference)) {
                return averageRating;
            } else {
                return null;
            }
        }

        @Override
        public long removeAverageRatings(EntityReference entityReference) throws RatingsException
        {
            return 0;
        }

        @Override
        public long moveAverageRatings(EntityReference oldReference, EntityReference newReference)
            throws RatingsException
        {
            return 0;
        }

        @Override
        protected ObservationManager getObservationManager()
        {
            return AbstractAverageRatingManagerTest.this.observationManager;
        }
    }

    private DumbAsbtractAverageRatingsManager averageRatingsManager;

    @BeforeEach
    void setup()
    {
        this.ratingsManager = mock(RatingsManager.class);
        this.entityReference = mock(EntityReference.class);
        this.averageRating = mock(AverageRating.class);
        this.observationManager = mock(ObservationManager.class);
        this.averageRatingsManager = new DumbAsbtractAverageRatingsManager();
        this.averageRatingsManager.setRatingsManager(this.ratingsManager);
    }

    @Test
    void addVote() throws Exception
    {
        int oldTotalVotes = 42;
        float oldAverageVote = 3.4f;
        int newVote = 2;
        String managerId = "myRatingId";
        when(this.averageRating.getNbVotes()).thenReturn(oldTotalVotes);
        when(this.averageRating.getAverageVote()).thenReturn(oldAverageVote);
        when(this.ratingsManager.getIdentifier()).thenReturn(managerId);
        UpdatingAverageRatingEvent expectedEvent1 =
            new UpdatingAverageRatingEvent(this.averageRating, oldAverageVote, oldTotalVotes);
        UpdatedAverageRatingEvent expectedEvent2 =
            new UpdatedAverageRatingEvent(this.averageRating, oldAverageVote, oldTotalVotes);

        assertEquals(this.averageRating, this.averageRatingsManager.addVote(this.entityReference, newVote));
        assertTrue(this.averageRatingsManager.isSaved);
        verify(this.averageRating).addRating(newVote);
        verify(this.observationManager).notify(expectedEvent1, managerId, this.averageRating);
        verify(this.observationManager).notify(expectedEvent2, managerId, this.averageRating);
    }

    @Test
    void removeVote() throws Exception
    {
        int oldTotalVotes = 13;
        float oldAverageVote = 0.543f;
        int removedVote = 43;
        String managerId = "identifier";
        when(this.averageRating.getNbVotes()).thenReturn(oldTotalVotes);
        when(this.averageRating.getAverageVote()).thenReturn(oldAverageVote);
        when(this.ratingsManager.getIdentifier()).thenReturn(managerId);
        UpdatingAverageRatingEvent expectedEvent1 =
            new UpdatingAverageRatingEvent(this.averageRating, oldAverageVote, oldTotalVotes);
        UpdatedAverageRatingEvent expectedEvent2 =
            new UpdatedAverageRatingEvent(this.averageRating, oldAverageVote, oldTotalVotes);

        assertEquals(this.averageRating, this.averageRatingsManager.removeVote(this.entityReference, removedVote));
        assertTrue(this.averageRatingsManager.isSaved);
        verify(this.averageRating).removeRating(removedVote);
        verify(this.observationManager).notify(expectedEvent1, managerId, this.averageRating);
        verify(this.observationManager).notify(expectedEvent2, managerId, this.averageRating);
    }

    @Test
    void updateVote() throws Exception
    {
        int oldTotalVotes = 68;
        float oldAverageVote = 13.43f;
        int oldVote = 13;
        int newVote = 14;
        String managerId = "something";
        when(this.averageRating.getNbVotes()).thenReturn(oldTotalVotes);
        when(this.averageRating.getAverageVote()).thenReturn(oldAverageVote);
        when(this.ratingsManager.getIdentifier()).thenReturn(managerId);
        UpdatingAverageRatingEvent expectedEvent1 =
            new UpdatingAverageRatingEvent(this.averageRating, oldAverageVote, oldTotalVotes);
        UpdatedAverageRatingEvent expectedEvent2 =
            new UpdatedAverageRatingEvent(this.averageRating, oldAverageVote, oldTotalVotes);

        assertEquals(this.averageRating, this.averageRatingsManager.updateVote(this.entityReference, oldVote, newVote));
        assertTrue(this.averageRatingsManager.isSaved);
        verify(this.averageRating).updateRating(oldVote, newVote);
        verify(this.observationManager).notify(expectedEvent1, managerId, this.averageRating);
        verify(this.observationManager).notify(expectedEvent2, managerId, this.averageRating);
    }

    @Test
    void resetAverageRating() throws Exception
    {
        int oldTotalVotes = 14;
        float oldAverageVote = 5.343f;
        String managerId = "foobar";

        int newTotalVotes = 23;
        float newAverageVote = 4.569f;

        when(this.averageRating.getId()).thenReturn("myId");
        when(this.averageRating.getAverageVote()).thenReturn(oldAverageVote);
        when(this.averageRating.getNbVotes()).thenReturn(oldTotalVotes);
        when(this.ratingsManager.getIdentifier()).thenReturn(managerId);
        DefaultAverageRating expectedAverageRating = new DefaultAverageRating("myId")
            .setTotalVote(newTotalVotes)
            .setAverageVote(newAverageVote);

        UpdatingAverageRatingEvent expectedEvent1 =
            new UpdatingAverageRatingEvent(expectedAverageRating, oldAverageVote, oldTotalVotes);
        UpdatedAverageRatingEvent expectedEvent2 =
            new UpdatedAverageRatingEvent(expectedAverageRating, oldAverageVote, oldTotalVotes);

        AverageRating averageRating =
            this.averageRatingsManager.resetAverageRating(this.entityReference, newAverageVote, newTotalVotes);
        expectedAverageRating.setUpdatedAt(averageRating.getUpdatedAt());
        assertEquals(expectedAverageRating, averageRating);
        assertTrue(this.averageRatingsManager.isSaved);
        verify(this.observationManager).notify(expectedEvent1, managerId, expectedAverageRating);
        verify(this.observationManager).notify(expectedEvent2, managerId, expectedAverageRating);
    }
}
