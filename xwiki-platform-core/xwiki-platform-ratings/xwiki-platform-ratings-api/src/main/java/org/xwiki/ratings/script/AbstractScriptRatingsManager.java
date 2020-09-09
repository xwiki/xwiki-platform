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
package org.xwiki.ratings.script;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;

/**
 * This abstract class contains all the public method to be used in {@link RatingsScriptService}.
 * It relies internally on a dedicated instance of {@link RatingsManager}: the method
 * {@link #setRatingsManager(RatingsManager)} needs to be called when any concrete class is instantiated.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Unstable
public abstract class AbstractScriptRatingsManager
{
    private RatingsManager ratingsManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private Logger logger;

    void setRatingsManager(RatingsManager ratingsManager)
    {
        this.ratingsManager = ratingsManager;
    }

    private UserReference getCurrentUserReference()
    {
        return this.userReferenceResolver.resolve(this.contextProvider.get().getUserReference());
    }

    /**
     * Allows to save a rating for the given reference, with the current user reference.
     *
     * @param reference the reference for which to save a rating.
     * @param vote the rating to save.
     * @return an optional containing the {@link Rating} value, or empty in case of problem or if the rating is 0 and
     *      the configuration doesn't allow to save 0 values (see {@link RatingsConfiguration#storeZero()}).
     */
    public Optional<Rating> setRating(EntityReference reference, int vote)
    {
        return this.setRating(reference, this.getCurrentUserReference(), vote);
    }

    /**
     * Allows to save a rating for the given reference, with the given user reference.
     *
     * @param reference the reference for which to save a rating.
     * @param userReference the reference of the user who performs the rating.
     * @param vote the rating to save.
     * @return an optional containing the {@link Rating} value, or empty in case of problem or if the rating is 0 and
     *      the configuration doesn't allow to save 0 values (see {@link RatingsConfiguration#storeZero()}).
     */
    public Optional<Rating> setRating(EntityReference reference, UserReference userReference, int vote)
    {
        try {
            Rating rating = this.ratingsManager.saveRating(reference, userReference, vote);
            if (rating != null) {
                return Optional.of(rating);
            }
        } catch (RatingsException e) {
            logger.error("Error while trying to rate reference [{}].", reference, ExceptionUtils.getRootCause(e));
        }
        return Optional.empty();
    }

    /**
     * Retrieve ratings information for the given reference on the given manager.
     *
     * @param reference the reference for which to retrieve rating information.
     * @param offset the offset at which to start for retrieving information.
     * @param limit the limit number of information to retrieve.
     * @return a list of ratings containing a maximum of {@code limit} values sorted by
     *         updated date in descending order.
     */
    public List<Rating> getRatings(EntityReference reference, int offset, int limit)
    {
        return getRatings(reference, offset, limit, false);
    }

    /**
     * Retrieve ratings information for the given reference on the given manager.
     *
     * @param reference the reference for which to retrieve rating information.
     * @param offset the offset at which to start for retrieving information.
     * @param limit the limit number of information to retrieve.
     * @param asc if {@code true} return the results sorted by updated date in ascending order, else in descending order
     * @return a list of ratings containing a maximum of {@code limit} values.
     */
    public List<Rating> getRatings(EntityReference reference, int offset, int limit, boolean asc)
    {
        try {
            Map<RatingsManager.RatingQueryField, Object> queryParameters = new HashMap<>();
            queryParameters.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, reference);
            queryParameters.put(RatingsManager.RatingQueryField.ENTITY_TYPE, reference.getType());
            return this.ratingsManager.getRatings(queryParameters, offset, limit,
                RatingsManager.RatingQueryField.UPDATED_DATE, asc);
        } catch (RatingsException e) {
            logger.error("Error when getting ratings for reference [{}].", reference, ExceptionUtils.getRootCause(e));
            return Collections.emptyList();
        }
    }

    /**
     * Retrieve the average rating of a specific reference.
     *
     * @param reference the reference for which to retrieve the average rating information.
     * @return the average rating in an optional or an empty optional in case of error.
     */
    public Optional<AverageRating> getAverageRating(EntityReference reference)
    {
        try {
            return Optional.of(this.ratingsManager.getAverageRating(reference));
        } catch (RatingsException e) {
            logger.error("Error when getting average rating for reference [{}]", reference,
                ExceptionUtils.getRootCause(e));
        }
        return Optional.empty();
    }

    /**
     * Retrieve the rating performed by the given author on the given reference.
     * The method returns an empty optional if the rating cannot be found.
     *
     * @param reference the entity being rated
     * @param author the author of the rate
     * @return an optional containing the rating object or being empty if it cannot be found or in case of error.
     */
    public Optional<Rating> getRating(EntityReference reference, UserReference author)
    {
        Optional<Rating> result = Optional.empty();
        Map<RatingsManager.RatingQueryField, Object> queryParameters = new HashMap<>();
        queryParameters.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, reference);
        queryParameters.put(RatingsManager.RatingQueryField.ENTITY_TYPE, reference.getType());
        queryParameters.put(RatingsManager.RatingQueryField.USER_REFERENCE, author);
        try {
            List<Rating> ratings = this.ratingsManager.getRatings(queryParameters, 0, 1,
                RatingsManager.RatingQueryField.UPDATED_DATE, false);
            if (!ratings.isEmpty()) {
                result = Optional.of(ratings.get(0));
            }
        } catch (RatingsException e) {
            logger.error("Error when getting rating for reference [{}] by user [{}].", reference, author,
                ExceptionUtils.getRootCause(e));
        }
        return result;
    }

    /**
     * Retrieve all the ratings of the current user.
     * @param offset offset of ratings to start retrieve.
     * @param limit maximum number of ratings to retrieve.
     * @param asc if the results should be ordered in updated date ascending or descending order.
     * @return a list of ratings.
     */
    public List<Rating> getCurrentUserRatings(int offset, int limit, boolean asc)
    {
        List<Rating> result;
        Map<RatingsManager.RatingQueryField, Object> queryParameters = new HashMap<>();
        queryParameters.put(RatingsManager.RatingQueryField.USER_REFERENCE, this.getCurrentUserReference());
        try {
            result = this.ratingsManager.getRatings(queryParameters, offset, limit,
                RatingsManager.RatingQueryField.UPDATED_DATE, asc);
        } catch (RatingsException e) {
            logger.error("Error when getting ratings of user [{}].", this.getCurrentUserReference(),
                ExceptionUtils.getRootCause(e));
            result = Collections.emptyList();
        }
        return result;
    }
}
