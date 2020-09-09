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

import javax.inject.Inject;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.events.UpdateAverageRatingFailedEvent;
import org.xwiki.ratings.events.UpdatedAverageRatingEvent;
import org.xwiki.ratings.events.UpdatingAverageRatingEvent;

/**
 * Abstract helper for {@link AverageRatingManager}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
public abstract class AbstractAverageRatingManager implements AverageRatingManager
{
    private RatingsManager ratingsManager;

    @Inject
    private ObservationManager observationManager;

    @Override
    public void setRatingsManager(RatingsManager ratingsManager)
    {
        this.ratingsManager = ratingsManager;
    }

    protected String getIdentifier()
    {
        return this.ratingsManager.getIdentifier();
    }

    protected int getScale()
    {
        return this.ratingsManager.getScale();
    }

    protected AverageRating createAverageRating(EntityReference entityReference, String id)
    {
        return new DefaultAverageRating(id)
            .setManagerId(this.getIdentifier())
            .setScale(this.getScale())
            .setReference(entityReference)
            .setAverageVote(0)
            .setTotalVote(0);
    }

    protected abstract void saveAverageRating(AverageRating averageRating) throws RatingsException;

    private AverageRating updateAverageRating(AverageRating averageRating, float oldAverageVote, int oldTotalVote)
        throws RatingsException
    {
        this.observationManager.notify(new UpdatingAverageRatingEvent(averageRating, oldAverageVote, oldTotalVote),
            this.getIdentifier(), averageRating);
        try {
            this.saveAverageRating(averageRating);
            this.observationManager.notify(
                new UpdatedAverageRatingEvent(averageRating, oldAverageVote, oldTotalVote), this.getIdentifier(),
                averageRating);
            return averageRating;
        } catch (RatingsException e) {
            this.observationManager.notify(
                new UpdateAverageRatingFailedEvent(averageRating, oldAverageVote, oldTotalVote), this.getIdentifier(),
                averageRating);
            throw e;
        }
    }

    @Override
    public AverageRating addVote(EntityReference entityReference, int newVote) throws RatingsException
    {
        AverageRating averageRating = getAverageRating(entityReference);
        int oldTotalVote = averageRating.getNbVotes();
        float oldAverageVote = averageRating.getAverageVote();
        averageRating.addRating(newVote);
        return this.updateAverageRating(averageRating, oldAverageVote, oldTotalVote);
    }

    @Override
    public AverageRating removeVote(EntityReference entityReference, int oldVote) throws RatingsException
    {
        AverageRating averageRating = getAverageRating(entityReference);
        int oldTotalVote = averageRating.getNbVotes();
        float oldAverageVote = averageRating.getAverageVote();
        averageRating.removeRating(oldVote);
        return this.updateAverageRating(averageRating, oldAverageVote, oldTotalVote);
    }

    @Override
    public AverageRating updateVote(EntityReference entityReference, int oldVote, int newVote)
        throws RatingsException
    {
        AverageRating averageRating = getAverageRating(entityReference);
        int oldTotalVote = averageRating.getNbVotes();
        float oldAverageVote = averageRating.getAverageVote();
        averageRating.updateRating(oldVote, newVote);
        return this.updateAverageRating(averageRating, oldAverageVote, oldTotalVote);
    }
}
