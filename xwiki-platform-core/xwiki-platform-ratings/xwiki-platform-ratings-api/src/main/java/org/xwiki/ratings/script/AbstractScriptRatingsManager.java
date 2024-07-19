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
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
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
public abstract class AbstractScriptRatingsManager implements RatingsScriptServiceAPI
{
    @Inject
    protected Logger logger;

    private RatingsManager ratingsManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    void setRatingsManager(RatingsManager ratingsManager)
    {
        this.ratingsManager = ratingsManager;
    }

    private UserReference getCurrentUserReference()
    {
        return this.userReferenceResolver.resolve(this.contextProvider.get().getUserReference());
    }

    @Override
    public Optional<Rating> setRating(EntityReference reference, int vote)
    {
        return this.setRating(reference, this.getCurrentUserReference(), vote);
    }

    @Override
    public Optional<Rating> setRating(EntityReference reference, UserReference userReference, int vote)
    {
        Optional<Rating> result = Optional.empty();
        if (!this.isExcludedFromRatings(reference)) {
            try {
                Rating rating = this.ratingsManager.saveRating(reference, userReference, vote);
                if (rating != null) {
                    result = Optional.of(rating);
                }
            } catch (RatingsException e) {
                this.logger.error("Error while trying to rate reference [{}].", reference,
                    ExceptionUtils.getRootCause(e));
            }
        }
        return result;
    }

    @Override
    public List<Rating> getRatings(EntityReference reference, int offset, int limit)
    {
        return getRatings(reference, offset, limit, false);
    }

    @Override
    public List<Rating> getRatings(EntityReference reference, int offset, int limit, boolean asc)
    {
        try {
            Map<RatingsManager.RatingQueryField, Object> queryParameters = new HashMap<>();
            queryParameters.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, reference);
            return this.ratingsManager.getRatings(queryParameters, offset, limit,
                RatingsManager.RatingQueryField.UPDATED_DATE, asc);
        } catch (RatingsException e) {
            logger.error("Error when getting ratings for reference [{}].", reference, ExceptionUtils.getRootCause(e));
            return Collections.emptyList();
        }
    }

    @Override
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

    @Override
    public Optional<AverageRating> recomputeAverageRating(EntityReference reference)
    {
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            try {
                return Optional.of(this.ratingsManager.recomputeAverageRating(reference));
            } catch (RatingsException e) {
                logger.error("Error when computing average rating for reference [{}]", reference,
                    ExceptionUtils.getRootCause(e));
            }
        } else {
            logger.warn("Recomputation of average rating is not authorized for users without programming rights. "
                + "The script in [{}] cannot be executed properly.",
                this.contextProvider.get().getDoc().getDocumentReference());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Rating> getRating(EntityReference reference, UserReference author)
    {
        Optional<Rating> result = Optional.empty();
        Map<RatingsManager.RatingQueryField, Object> queryParameters = new HashMap<>();
        queryParameters.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, reference);
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

    @Override
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

    @Override
    public RatingsConfiguration getConfiguration()
    {
        return this.ratingsManager.getRatingConfiguration();
    }

    @Override
    public boolean isExcludedFromRatings(EntityReference entityReference)
    {
        boolean result = false;

        for (EntityReference excludedReference : this.ratingsManager.getRatingConfiguration()
            .getExcludedReferencesFromRatings()) {
            if (entityReference.equals(excludedReference) || entityReference.hasParent(excludedReference))
            {
                result = true;
                break;
            }
        }

        return result;
    }
}
