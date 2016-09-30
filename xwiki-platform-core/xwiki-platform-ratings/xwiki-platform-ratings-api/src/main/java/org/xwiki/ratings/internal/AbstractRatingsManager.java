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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.ReputationException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 * @see RatingsManager
 * @since 6.4M3
 */
public abstract class AbstractRatingsManager implements RatingsManager
{
    @Inject
    protected ObservationManager observationManager;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private RatingsConfiguration ratingsConfiguration;

    @Override
    public String getRatingsClassName()
    {
        return RATINGS_CLASSNAME;
    }

    /**
     * Gets the average ratings class.
     * 
     * @return the XWiki document representing the AverageRatingsClass.
     */
    public String getAverageRatingsClassName()
    {
        return AVERAGE_RATINGS_CLASSNAME;
    }

    /**
     * Gets the ratings configuration component.
     *
     * @return the ratingsConfiguration representing the RatingsConfiguration component.
     */
    protected RatingsConfiguration getRatingsConfiguration()
    {
        return ratingsConfiguration;
    }

    /**
     * Retrieves the XWiki context from the current execution context.
     * 
     * @return the XWiki context
     * @throws RuntimeException if there was an error retrieving the context
     */
    protected XWikiContext getXWikiContext()
    {
        return this.xcontextProvider.get();
    }

    /**
     * Retrieves the XWiki private API object.
     * 
     * @return The XWiki private API object
     */
    protected XWiki getXWiki()
    {
        return getXWikiContext().getWiki();
    }

    /**
     * Checks if ratings are active.
     * 
     * @return answer to: are ratings active?
     */
    public boolean hasRatings()
    {
        int result = (int) getXWiki().ParamAsLong("xwiki.ratings", 0);
        return (getXWiki().getXWikiPreferenceAsInt("ratings", result, getXWikiContext()) == 1);
    }

    @Override
    public boolean isAverageRatingStored(DocumentReference documentRef)
    {
        String result = getXWiki().Param("xwiki.ratings.averagerating.stored", "1");
        result = getXWiki().getXWikiPreference("ratings_averagerating_stored", result, getXWikiContext());
        return (ratingsConfiguration.getConfigurationParameter(documentRef,
            RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_STORE_AVERAGE_RATING, result).equals("1"));
    }

    @Override
    public boolean isReputationStored(DocumentReference documentRef)
    {
        String result = getXWiki().Param("xwiki.ratings.reputation.stored", "0");
        result = getXWiki().getXWikiPreference("ratings_reputation_stored", result, getXWikiContext());
        return (ratingsConfiguration.getConfigurationParameter(documentRef,
            RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_STORED, result).equals("1"));
    }

    @Override
    public boolean hasReputation(DocumentReference documentRef)
    {
        String result = getXWiki().Param("xwiki.ratings.reputation", "0");
        result = getXWiki().getXWikiPreference("ratings_reputation", result, getXWikiContext());
        return (ratingsConfiguration.getConfigurationParameter(documentRef,
            RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION, result).equals("1"));
    }

    @Override
    public String[] getDefaultReputationMethods(DocumentReference documentRef)
    {
        String method = getXWiki().Param("xwiki.ratings.reputation.defaultmethod", RATING_REPUTATION_METHOD_DEFAULT);
        method = getXWiki().getXWikiPreference("ratings_reputation_defaultmethod", method, getXWikiContext());
        method = ratingsConfiguration.getConfigurationParameter(documentRef,
            RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_METHOD, method);
        return method.split(",");
    }

    @Override
    public void updateAverageRatings(DocumentReference documentRef, Rating rating, int oldVote) throws RatingsException
    {
        String[] methods = getDefaultReputationMethods(documentRef);
        for (int i = 0; i < methods.length; i++) {
            updateAverageRating(documentRef, rating, oldVote, methods[i]);
        }
    }

    @Override
    public AverageRating getAverageRatingFromQuery(String fromsql, String wheresql) throws RatingsException
    {
        return getAverageRatingFromQuery(fromsql, wheresql, RATING_REPUTATION_METHOD_AVERAGE);
    }

    @Override
    public AverageRating getAverageRating(DocumentReference documentRef) throws RatingsException
    {
        return getAverageRating(documentRef, RATING_REPUTATION_METHOD_AVERAGE);
    }

    @Override
    public AverageRating getAverageRatingFromQuery(String fromsql, String wheresql, String method)
        throws RatingsException
    {
        try {
            String fromsql2 =
                fromsql + ", BaseObject as avgobj, FloatProperty as avgvote, StringProperty as avgmethod ";
            String wheresql2 =
                (wheresql.equals("") ? "where " : wheresql + " and ")
                    + "doc.fullName=avgobj.name and avgobj.className='" + getAverageRatingsClassName()
                    + "' and avgobj.id=avgvote.id.id and avgvote.id.name='" + AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE
                    + "' and avgobj.id=avgmethod.id.id and avgmethod.id.name='"
                    + AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD + "' and avgmethod.value='" + method + "'";
            String sql =
                "select sum(avgvote.value) as vote, count(avgvote.value) as nbvotes from XWikiDocument as doc "
                    + fromsql2 + wheresql2;

            if (logger.isDebugEnabled()) {
                logger.debug("Running average rating with sql " + sql);
            }
            getXWikiContext().put("lastsql", sql);

            List result = getXWiki().getStore().search(sql, 0, 0, getXWikiContext());
            float vote = ((Number) ((Object[]) result.get(0))[0]).floatValue();
            int nbvotes = ((Number) ((Object[]) result.get(0))[1]).intValue();

            AverageRating avgr = new MemoryAverageRating(null, nbvotes, vote / nbvotes, method);
            return avgr;
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public boolean removeRating(Rating rating) throws RatingsException
    {
        return rating.remove();
    }

    @Override
    public AverageRating getUserReputation(DocumentReference username) throws ReputationException
    {
        try {
            return getAverageRating(username, RatingsManager.RATING_REPUTATION_METHOD_AVERAGE);
        } catch (RatingsException e) {
            throw new ReputationException(e);
        }
    }

    @Override
    public AverageRating calcAverageRating(DocumentReference documentRef, String method) throws RatingsException
    {
        int nbVotes = 0;
        int balancedNbVotes = 0;
        float totalVote = 0;
        float averageVote = 0;
        List<Rating> ratings = getRatings(documentRef, 0, 0, true);
        if (ratings == null) {
            return null;
        }
        for (Rating rating : ratings) {
            if (method.equals(RATING_REPUTATION_METHOD_BALANCED)) {
                DocumentReference author = rating.getAuthor();
                // in case we are evaluating the average rating of a user
                // we should not include votes of himself to a user
                if (!author.equals(documentRef)) {
                    AverageRating reputation = getUserReputation(author);
                    if ((reputation == null) || (reputation.getAverageVote() == 0)) {
                        totalVote += rating.getVote();
                        balancedNbVotes++;
                    } else {
                        totalVote += rating.getVote() * reputation.getAverageVote();
                        balancedNbVotes += reputation.getAverageVote();
                    }
                }
            } else {
                totalVote += rating.getVote();
                balancedNbVotes++;
            }
            nbVotes++;
        }

        if (balancedNbVotes != 0) {
            averageVote = totalVote / balancedNbVotes;
        }
        return new MemoryAverageRating(documentRef, nbVotes, averageVote, method);
    }

    @Override
    public void updateAverageRating(DocumentReference documentRef, Rating rating, int oldVote, String method)
        throws RatingsException
    {
        // we only update if we are in stored mode and if the vote changed
        if (isAverageRatingStored(documentRef) && oldVote != rating.getVote()) {
            AverageRating aRating = calcAverageRating(documentRef, method);
            AverageRating averageRating = getAverageRating(documentRef, method, true);
            averageRating.setAverageVote(aRating.getAverageVote());
            averageRating.setNbVotes(aRating.getNbVotes());
            averageRating.save();
            /*
             * StoredAverageRating averageRating = (StoredAverageRating) getAverageRating(container, method, true,
             * context); int diffTotal = rating.getVote() - oldVote; int diffNbVotes = (oldVote==0) ? 1 : 0; int
             * oldNbVotes = averageRating.getNbVotes(); averageRating.setNbVotes(oldNbVotes + diffNbVotes);
             * averageRating.setAverageVote((averageRating.getAverageVote()*oldNbVotes + diffTotal) / (oldNbVotes +
             * diffNbVotes));
             */
        }
    }

    @Override
    public void updateUserReputation(DocumentReference author, AverageRating voterRating) throws RatingsException
    {
        try {
            // We should update the user rating
            AverageRating rating = getAverageRating(author, voterRating.getMethod(), true);
            rating.setAverageVote(voterRating.getAverageVote());
            rating.setMethod(voterRating.getMethod());
            rating.setNbVotes(voterRating.getNbVotes());
            rating.save();
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public AverageRating getAverageRating(DocumentReference documentRef, String method) throws RatingsException
    {
        return getAverageRating(documentRef, method, false);
    }

    @Override
    public AverageRating getAverageRating(DocumentReference documentRef, String method, boolean create)
        throws RatingsException
    {
        try {
            if (isAverageRatingStored(documentRef)) {
                String className = getAverageRatingsClassName();
                XWikiDocument doc = getXWikiContext().getWiki().getDocument(documentRef, getXWikiContext());
                BaseObject averageRatingObject =
                    doc.getObject(className, RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD, method,
                        false);
                if (averageRatingObject == null) {
                    if (!create) {
                        return calcAverageRating(documentRef, method);
                    }

                    // initiate a new average rating object
                    averageRatingObject = doc.newObject(className, getXWikiContext());
                    averageRatingObject.setStringValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD,
                        method);
                }

                return new StoredAverageRating(doc, averageRatingObject, getXWikiContext());
            } else {
                return calcAverageRating(documentRef, method);
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }
}
