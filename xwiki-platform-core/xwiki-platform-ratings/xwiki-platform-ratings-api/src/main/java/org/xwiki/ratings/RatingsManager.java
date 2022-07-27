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
package org.xwiki.ratings;

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.events.CreatedRatingEvent;
import org.xwiki.ratings.events.UpdatedRatingEvent;
import org.xwiki.user.UserReference;

/**
 * Manager for handling Rating operations.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Role
public interface RatingsManager
{
    /**
     * The fields to be used for performing queries on Ratings.
     */
    enum RatingQueryField
    {
        /**
         * Represents the 'id' field.
         */
        IDENTIFIER("id"),
        /**
         * Represents the 'id' field.
         */
        ENTITY_REFERENCE("reference"),
        /**
         * Represents the 'parents' field.
         */
        PARENTS_REFERENCE("parents"),
        /**
         * Represents the 'author' field.
         */
        USER_REFERENCE("author"),
        /**
         * Represents the 'vote' field.
         */
        VOTE("vote"),
        /**
         * Represents the 'createdDate' field.
         */
        CREATED_DATE("createdDate"),
        /**
         * Represents the 'updatedDate' field.
         */
        UPDATED_DATE("updatedDate"),
        /**
         * Represents the 'managerId' field.
         */
        MANAGER_ID("managerId"),
        /**
         * Represents the 'scale' field.
         */
        SCALE("scale");

        private final String fieldName;

        RatingQueryField(String fieldName)
        {
            this.fieldName = fieldName;
        }

        /**
         * @return the name of the field used for filtering Rating queries.
         */
        public String getFieldName()
        {
            return this.fieldName;
        }
    }

    /**
     * @return the identifier of the current manager.
     */
    String getIdentifier();

    /**
     * Allows to set the identifier of the manager.
     * This method should only be used when creating the manager in a {@link RatingsManagerFactory}.
     *
     * @param identifier the identifier to be set.
     */
    void setIdentifier(String identifier);

    /**
     * @return the upper bound of the scale used by this manager for rating.
     */
    int getScale();

    /**
     * Allows to set the configuration of the manager.
     * This method should only be used when creating the manager in a {@link RatingsManagerFactory}.
     *
     * @param configuration the configuration to be set.
     */
    void setRatingConfiguration(RatingsConfiguration configuration);

    /**
     * @return the configuration used by this manager.
     */
    RatingsConfiguration getRatingConfiguration();

    /**
     * Save and return a {@link Rating} information.
     * If an existing rating has already been saved by the same user on the same reference, then this method updates the
     * existing value.
     * This method should check that the given vote matches the scale of the manager.
     * It should also take into account the {@link RatingsConfiguration#isZeroStored()} configuration to handle case when
     * the vote is equal to 0. The method returns null if the vote is equal to 0 and the configuration doesn't allow
     * to store it, but it might perform storage side effect (such as removing a previous {@link Rating} information).
     * This method also handles the computation of {@link AverageRating} if the
     * {@link RatingsConfiguration#isAverageStored()} configuration is set to true.
     * Note that this method should also handle sending the appropriate
     * {@link CreatedRatingEvent} and {@link UpdatedRatingEvent}.
     *
     * @param reference the entity for which to save a rating value.
     * @param user the user who performs the rating.
     * @param vote the actual vote to be saved.
     * @return the saved rating or null if none has been saved.
     * @throws RatingsException in case of problem for saving the rating.
     */
    Rating saveRating(EntityReference reference, UserReference user, int vote) throws RatingsException;

    /**
     * Retrieve the list of ratings based on the given query parameters.
     * Only exact matching can be used right now for the given query parameters. It's possible to provide some
     * objects as query parameters: some specific treatment can be apply depending on the type of the objects, but for
     * most type we're just relying on {@code String.valueOf(Object)}. Only the ratings of the current manager are
     * retrieved even if the store is shared.
     *
     * @param queryParameters the map of parameters to rely on for query the ratings.
     * @param offset the offset where to start getting results.
     * @param limit the limit number of results to retrieve.
     * @param orderBy the field to use for sorting the results.
     * @param asc if {@code true}, use ascending order for sorting, else use descending order.
     * @return a list containing at most {@code limit} ratings results.
     * @throws RatingsException in case of problem for querying the ratings.
     */
    List<Rating> getRatings(Map<RatingQueryField, Object> queryParameters,
        int offset, int limit, RatingQueryField orderBy, boolean asc) throws RatingsException;

    /**
     * Retrieve the number of ratings matching the given parameters but without retrieving them directly.
     * Only exact matching can be used right now for the given query parameters. It's possible to provide some
     * objects as query parameters: some specific treatment can be apply depending on the type of the objects, but for
     * most type we're just relying on {@code String.valueOf(Object)}. Only the ratings of the current manager are
     * retrieved even if the store is shared.
     *
     * @param queryParameters the map of parameters to rely on for query the ratings.
     * @return the total number of ratings matching the query parameters.
     * @throws RatingsException in case of problem during the query.
     */
    long countRatings(Map<RatingQueryField, Object> queryParameters) throws RatingsException;

    /**
     * Remove a rating based on its identifier.
     * This method also performs an update of the {@link AverageRating} if the
     * {@link RatingsConfiguration#isAverageStored()} is enabled.
     *
     * @param ratingIdentifier the identifier of the rating to remove.
     * @return {@code true} if a rating is deleted, {@code false} if no rating with the given identifier can be found.
     * @throws RatingsException in case of problem during the query.
     */
    boolean removeRating(String ratingIdentifier) throws RatingsException;

    /**
     * Remove all ratings concerning the given reference.
     * Ratings targeted the given reference will be deleted, but also ratings that concerns elements having the given
     * reference as ancestor.
     *
     * @param entityReference the reference used as a criteria for filtering ratings reference or parents.
     * @return the total number of deleted ratings.
     * @throws RatingsException in case of problem during the query.
     */
    long removeRatings(EntityReference entityReference) throws RatingsException;

    /**
     * Update all ratings concerning the given reference to point to the new reference.
     * This update is performed for both ratings targeting directly the given reference, but also for those having the
     * reference as ancestor.
     *
     * @param oldReference the old reference to be updated.
     * @param newReference the new reference.
     * @return the total number of updated ratings.
     * @throws RatingsException in case of problem during the query.
     * @since 12.10
     */
    long moveRatings(EntityReference oldReference, EntityReference newReference) throws RatingsException;

    /**
     * Retrieve the average rating information of the given reference.
     *
     * @param entityReference the reference for which to retrieve the average rating information.
     * @return the average rating data corresponding to the given reference.
     * @throws RatingsException in case of problem during the query.
     */
    AverageRating getAverageRating(EntityReference entityReference) throws RatingsException;

    /**
     * This method performs a direct save of the rating specified in parameter.
     * Note that this method does not take into account the {@link RatingsConfiguration#isZeroStored()} option and does not
     * compute back the average. Finally no event is triggered when using this method.
     * For those to be taken into account, please check {@link #saveRating(EntityReference, UserReference, int)}.
     *
     * @param rating a rating object to be saved.
     */
    void saveRating(Rating rating) throws RatingsException;

    /**
     * Compute back the average rating for all the ratings existing for the given reference.
     * Note that this method might be resource consuming in case of large amount of data.
     *
     * @param entityReference the reference for which to compute back the ratings.
     * @throws RatingsException in case of errors.
     * @return the new average rating computed.
     */
    AverageRating recomputeAverageRating(EntityReference entityReference) throws RatingsException;
}
