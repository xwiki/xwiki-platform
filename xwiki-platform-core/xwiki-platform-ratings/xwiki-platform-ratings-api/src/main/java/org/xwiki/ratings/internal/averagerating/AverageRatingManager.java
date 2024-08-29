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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;

/**
 * Generic interface to manage {@link AverageRating}.
 *
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Role
public interface AverageRatingManager
{
    /**
     * Fields to be used for Average rating data.
     */
    enum AverageRatingQueryField
    {
        /**
         * Field for storing manager identifier.
         */
        MANAGER_ID("managerId"),

        /**
         * Field for storing reference of the rated element.
         */
        ENTITY_REFERENCE("reference"),

        /**
         * Field for storing the average vote.
         */
        AVERAGE_VOTE("averagevote"),

        /**
         * Field for storing the total number of votes.
         */
        TOTAL_VOTE("nbvotes"),

        /**
         * Field for storing the upper bound scale of the rating manager.
         */
        SCALE("scale"),

        /**
         * Store information about the parents of the entity reference.
         */
        PARENTS("parents"),

        /**
         * Field for storing the latest updated date.
         */
        UPDATED_AT("updatedAt");

        private String fieldName;

        /**
         * Default constructor.
         *
         * @param fieldName actual name of the field.
         */
        AverageRatingQueryField(String fieldName)
        {
            this.fieldName = fieldName;
        }

        /**
         * @return the actual name of the field to be used in queries.
         */
        public String getFieldName()
        {
            return this.fieldName;
        }
    }

    /**
     * Specify the {@link RatingsManager} instance who handle the current instance.
     * This method has to be called before any operation: it allows to set the ratings manager which is internally
     * used to retrieve various information like identifier or scale.
     *
     * @param ratingsManager the instance of ratings manager who handle the current instance.
     */
    void setRatingsManager(RatingsManager ratingsManager);

    /**
     * Retrieve the average rating information for the given reference.
     * Note that if the average rating does not exist, this method will create an average rating object
     * (without storing it) and should never return null.
     *
     * @param entityReference the reference for which to return an average rating.
     * @return the average rating for this reference.
     * @throws RatingsException in case of problem to retrieve the information.
     */
    AverageRating getAverageRating(EntityReference entityReference) throws RatingsException;

    /**
     * Add a new vote for the average rating of the given reference.
     * This method will save the updated average rating and triggers the appropriate events.
     * It returns the updated average rating object.
     *
     * @param entityReference the reference for which to add a vote for the average rating computation.
     * @param newVote the new vote to take into account.
     * @return an updated average rating.
     * @throws RatingsException in case of problem during the operation.
     */
    AverageRating addVote(EntityReference entityReference, int newVote) throws RatingsException;

    /**
     * Remove a vote from the average rating of the given reference.
     * This method will save the updated average rating and triggers the appropriate events.
     * It returns the updated average rating object.
     *
     * @param entityReference the reference for which to remove a vote for the average rating computation.
     * @param oldVote the old vote to remove from the average rating.
     * @return an updated average rating.
     * @throws RatingsException in case of problem during the operation.
     */
    AverageRating removeVote(EntityReference entityReference, int oldVote) throws RatingsException;

    /**
     * Update an existing vote for the average rating of the given reference.
     * This method will save the updated average rating and triggers the appropriate events.
     * It returns the updated average rating object.
     *
     * @param entityReference the reference for which to update a vote for the average rating computation.
     * @param newVote the new vote to take into account.
     * @param oldVote the old vote to update.
     * @return an updated average rating.
     * @throws RatingsException in case of problem during the operation.
     */
    AverageRating updateVote(EntityReference entityReference, int oldVote, int newVote) throws RatingsException;

    /**
     * Force resetting the data of average rating to the given value and saving them.
     * This should be used in case of re-computation of the average rating.
     *
     * @param entityReference the reference of rated element.
     * @param averageVote the new value of the average vote.
     * @param totalVote the new value of the total numbers of vote.
     * @return the modified AverageRating.
     */
    AverageRating resetAverageRating(EntityReference entityReference, float averageVote, int totalVote)
        throws RatingsException;

    /**
     * Remove the average rating entities concerning the given reference.
     *
     * @param entityReference the reference of the entities to remove.
     * @return the number of objects deleted.
     * @throws RatingsException in case of problem when removing average ratings.
     */
    long removeAverageRatings(EntityReference entityReference) throws RatingsException;

    /**
     * Update reference data in case of a move of the given reference.
     *
     * @param oldReference the old reference that has been moved.
     * @param newReference the new reference to store.
     * @return the total number of average ratings updated.
     * @throws RatingsException in case of problem during the update.
     * @since 12.10
     */
    long moveAverageRatings(EntityReference oldReference, EntityReference newReference) throws RatingsException;
}
