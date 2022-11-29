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

import java.util.List;
import java.util.Optional;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.util.Programming;

/**
 * Defines the available methods for {@link RatingsScriptService}.
 * Those methods are available when using {@code $services.ratings.RatingsManagerName} where {@code RatingsManagerName}
 * should be replaced with the proper name of the {@link org.xwiki.ratings.RatingsManager} to use. Note that using
 * {@code $services.ratings} without a specific {@code RatingsManagerName} will automatically rely to the default
 * {@link org.xwiki.ratings.RatingsManagerFactory#DEFAULT_APP_HINT}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
public interface RatingsScriptServiceAPI
{
    /**
     * Allows to save a rating for the given reference, with the current user reference.
     *
     * @param reference the reference for which to save a rating.
     * @param vote the rating to save.
     * @return an optional containing the {@link Rating} value, or empty in case of problem or if the rating is 0 and
     *      the configuration doesn't allow to save 0 values (see {@link RatingsConfiguration#isZeroStored()}).
     */
    Optional<Rating> setRating(EntityReference reference, int vote);

    /**
     * Allows to save a rating for the given reference, with the given user reference.
     *
     * @param reference the reference for which to save a rating.
     * @param userReference the reference of the user who performs the rating.
     * @param vote the rating to save.
     * @return an optional containing the {@link Rating} value, or empty in case of problem or if the rating is 0 and
     *      the configuration doesn't allow to save 0 values (see {@link RatingsConfiguration#isZeroStored()}).
     */
    Optional<Rating> setRating(EntityReference reference, UserReference userReference, int vote);

    /**
     * Retrieve ratings information for the given reference on the given manager.
     *
     * @param reference the reference for which to retrieve rating information.
     * @param offset the offset at which to start for retrieving information.
     * @param limit the limit number of information to retrieve.
     * @return a list of ratings containing a maximum of {@code limit} values sorted by
     *         updated date in descending order.
     */
    List<Rating> getRatings(EntityReference reference, int offset, int limit);

    /**
     * Retrieve ratings information for the given reference on the given manager.
     *
     * @param reference the reference for which to retrieve rating information.
     * @param offset the offset at which to start for retrieving information.
     * @param limit the limit number of information to retrieve.
     * @param asc if {@code true} return the results sorted by updated date in ascending order, else in descending order
     * @return a list of ratings containing a maximum of {@code limit} values.
     */
    List<Rating> getRatings(EntityReference reference, int offset, int limit, boolean asc);

    /**
     * Retrieve the average rating of a specific reference.
     *
     * @param reference the reference for which to retrieve the average rating information.
     * @return the average rating in an optional or an empty optional in case of error.
     */
    Optional<AverageRating> getAverageRating(EntityReference reference);

    /**
     * Recompute the average rating of a reference.
     * Note that this method can be resource consuming if the set of data is large.
     *
     * @param reference the reference for which to recompute the average ratings.
     * @return the average rating in an optional or an empty optional in case of error.
     * @since 13.7RC1
     * @since 13.4.3
     * @since 12.10.9
     */
    @Programming
    default Optional<AverageRating> recomputeAverageRating(EntityReference reference)
    {
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
    Optional<Rating> getRating(EntityReference reference, UserReference author);

    /**
     * Retrieve all the ratings of the current user.
     * @param offset offset of ratings to start retrieve.
     * @param limit maximum number of ratings to retrieve.
     * @param asc if the results should be ordered in updated date ascending or descending order.
     * @return a list of ratings.
     */
    List<Rating> getCurrentUserRatings(int offset, int limit, boolean asc);

    /**
     * @return the current configuration.
     */
    RatingsConfiguration getConfiguration();

    /**
     * Define if the given reference is excluded from ratings.
     *
     * @param entityReference the reference to check.
     * @return {@code true} only if the given reference is excluded from ratings.
     */
    boolean isExcludedFromRatings(EntityReference entityReference);
}
