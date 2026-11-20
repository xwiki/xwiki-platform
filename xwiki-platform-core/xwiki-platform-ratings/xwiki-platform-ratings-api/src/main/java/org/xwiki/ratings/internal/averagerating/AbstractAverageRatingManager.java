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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.function.FailableSupplier;
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
    /**
     * The number of times an average rating update is attempted before giving up when the stored average rating keeps
     * being modified concurrently.
     */
    private static final int UPDATE_ATTEMPTS = 5;

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

    protected ObservationManager getObservationManager()
    {
        return this.observationManager;
    }

    protected int getScale()
    {
        return this.ratingsManager.getScale();
    }

    protected AverageRating createAverageRating(EntityReference entityReference, String id)
    {
        return new DefaultAverageRating(id)
            .setManagerId(this.getIdentifier())
            .setScaleUpperBound(this.getScale())
            .setReference(entityReference)
            .setAverageVote(0)
            .setTotalVote(0);
    }

    protected abstract void saveAverageRating(AverageRating averageRating) throws RatingsException;

    private AverageRating updateAverageRating(AverageRating averageRating, float oldAverageVote, int oldTotalVote)
        throws RatingsException
    {
        List<AverageRating> averageRatingList = Collections.singletonList(averageRating);
        this.getObservationManager().notify(new UpdatingAverageRatingEvent(averageRating, oldAverageVote, oldTotalVote),
            this.getIdentifier(), averageRatingList);
        try {
            this.saveAverageRating(averageRating);
            this.getObservationManager().notify(
                new UpdatedAverageRatingEvent(averageRating, oldAverageVote, oldTotalVote), this.getIdentifier(),
                averageRatingList);
            return averageRating;
        } catch (RatingsException e) {
            this.getObservationManager().notify(
                new UpdateAverageRatingFailedEvent(averageRating, oldAverageVote, oldTotalVote), this.getIdentifier(),
                averageRatingList);
            throw e;
        }
    }

    /**
     * Indicates if the given exception was thrown because the stored average rating was modified concurrently, in which
     * case the update that failed can be performed again, starting from the newly stored average rating.
     *
     * @param exception an exception thrown while updating the stored average rating
     * @return {@code true} if the update failed because of a concurrent modification of the stored average rating
     * @since 18.10.0RC1
     */
    protected boolean isConcurrentModification(RatingsException exception)
    {
        return false;
    }

    /**
     * Performs an update of the stored average ratings, and performs it again when it fails because of a concurrent
     * modification (see {@link #isConcurrentModification(RatingsException)}). The update must read the stored average
     * ratings itself, so that each attempt starts from the latest stored state instead of overwriting the concurrent
     * modification.
     *
     * @param update reads, modifies and saves the stored average ratings; it may be called several times
     * @param <T> the type of the result of the update
     * @return the result of the update
     * @throws RatingsException if the update failed, or still failed because of concurrent modifications after
     *     several attempts
     * @since 18.10.0RC1
     */
    protected <T> T retryOnConcurrentModification(FailableSupplier<T, RatingsException> update)
        throws RatingsException
    {
        for (int attempt = 1;; attempt++) {
            try {
                return update.get();
            } catch (RatingsException e) {
                if (attempt >= UPDATE_ATTEMPTS || !isConcurrentModification(e)) {
                    throw e;
                }
            }
        }
    }

    /**
     * Reads the stored average rating in order to modify it and save it with {@link #saveAverageRating(AverageRating)}.
     * An implementation storing the average rating in a versioned container (e.g. a document) can return an average
     * rating that remembers the version it was read from, and save it on top of that version, so that the save fails
     * (see {@link #isConcurrentModification(RatingsException)}) if the stored average rating was modified concurrently
     * after it was read, instead of overwriting that modification with a value computed from the old one.
     *
     * @param entityReference the reference of the rated entity
     * @return the stored average rating, to be modified and saved
     * @throws RatingsException if the average rating can't be read
     * @since 18.10.0RC1
     */
    protected AverageRating getAverageRatingForUpdate(EntityReference entityReference) throws RatingsException
    {
        return getAverageRating(entityReference);
    }

    @Override
    public AverageRating addVote(EntityReference entityReference, int newVote) throws RatingsException
    {
        return retryOnConcurrentModification(() -> {
            AverageRating averageRating = getAverageRatingForUpdate(entityReference);
            int oldTotalVote = averageRating.getNbVotes();
            float oldAverageVote = averageRating.getAverageVote();
            averageRating.addRating(newVote);
            return this.updateAverageRating(averageRating, oldAverageVote, oldTotalVote);
        });
    }

    @Override
    public AverageRating removeVote(EntityReference entityReference, int oldVote) throws RatingsException
    {
        return retryOnConcurrentModification(() -> {
            AverageRating averageRating = getAverageRatingForUpdate(entityReference);
            int oldTotalVote = averageRating.getNbVotes();
            float oldAverageVote = averageRating.getAverageVote();
            averageRating.removeRating(oldVote);
            return this.updateAverageRating(averageRating, oldAverageVote, oldTotalVote);
        });
    }

    @Override
    public AverageRating updateVote(EntityReference entityReference, int oldVote, int newVote)
        throws RatingsException
    {
        return retryOnConcurrentModification(() -> {
            AverageRating averageRating = getAverageRatingForUpdate(entityReference);
            int oldTotalVote = averageRating.getNbVotes();
            float oldAverageVote = averageRating.getAverageVote();
            averageRating.updateRating(oldVote, newVote);
            return this.updateAverageRating(averageRating, oldAverageVote, oldTotalVote);
        });
    }

    @Override
    public AverageRating resetAverageRating(EntityReference entityReference, float averageVote, int totalVote)
        throws RatingsException
    {
        return retryOnConcurrentModification(() -> {
            // The new values don't depend on the stored ones, which are only read for the events: the reset wins over
            // a concurrent modification.
            AverageRating averageRating = getAverageRating(entityReference);
            int oldTotalVote = averageRating.getNbVotes();
            float oldAverageVote = averageRating.getAverageVote();
            DefaultAverageRating updatedAverageRating = new DefaultAverageRating(averageRating);
            updatedAverageRating.setAverageVote(averageVote);
            updatedAverageRating.setTotalVote(totalVote);
            updatedAverageRating.setUpdatedAt(new Date());
            return this.updateAverageRating(updatedAverageRating, oldAverageVote, oldTotalVote);
        });
    }
}
