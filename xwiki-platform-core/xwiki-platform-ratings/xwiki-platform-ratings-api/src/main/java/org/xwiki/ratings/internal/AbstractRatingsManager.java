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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.context.Execution;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.ReputationException;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * @version $Id$
 * @see RatingsManager
 */
public abstract class AbstractRatingsManager implements RatingsManager
{
    @Inject
    Execution execution;

    @Inject
    ObservationManager observationManager;
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractRatingsManager.class);
    
    public String getRatingsClassName()
    {
        return RATINGS_CLASSNAME;
    }

    public String getAverageRatingsClassName()
    {
        return AVERAGE_RATINGS_CLASSNAME;
    }
   
    /**
     * <p>
     * Retrieve the XWiki context from the current execution context
     * </p>
     * 
     * @return The XWiki context.
     * @throws RuntimeException If there was an error retrieving the context.
     */
    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * <p>
     * Retrieve the XWiki private API object
     * </p>
     * 
     * @return The XWiki private API object.
     */
    protected XWiki getXWiki()
    {
        return getXWikiContext().getWiki();
    }

    /**
     * Retrieve configuration parameter from the current space's WebPreferences
     * and fallback to XWiki.RatingsConfig if it does not exist
     */
    protected String getConfigParameter(String parameterName, String defaultValue)
    {
        try {
            String space = getXWikiContext().getDoc().getSpace();
            XWikiDocument spaceConfigDoc = getXWiki().getDocument(space + "." + RatingsManager.RATINGS_CONFIG_SPACE_PAGE, getXWikiContext());
            XWikiDocument globalConfigDoc = getXWiki().getDocument(RatingsManager.RATINGS_CONFIG_GLOBAL_PAGE, getXWikiContext());
            XWikiDocument configDoc = (spaceConfigDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME) == null) ? globalConfigDoc : spaceConfigDoc;

            if (!configDoc.isNew() && configDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME) != null) {
                BaseProperty prop = (BaseProperty) configDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME).get(parameterName);
                String propValue = (prop == null) ? defaultValue : prop.getValue().toString();

                return (propValue == "" ? defaultValue : propValue);
            }
        } catch(Exception e) {
            LOGGER.error("Cannot read ratings config", e);
        }

        return defaultValue;
    }

    public boolean hasRatings()
    {
        int result = (int) getXWiki().ParamAsLong("xwiki.ratings", 0);
        return (getXWiki().getXWikiPreferenceAsInt("ratings", result, getXWikiContext()) == 1);
    }

    public boolean isAverageRatingStored()
    {
        String result = getXWiki().Param("xwiki.ratings.averagerating.stored", "1");
        result = getXWiki().getXWikiPreference("ratings_averagerating_stored", result, getXWikiContext());
        return (getConfigParameter(RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_STORE_AVERAGE_RATING, result) == "1");
    }

    public boolean isReputationStored()
    {
        String result = getXWiki().Param("xwiki.ratings.reputation.stored", "0");
        result = getXWiki().getXWikiPreference("ratings_reputation_stored", result, getXWikiContext());
        return (getConfigParameter(RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_STORED, result) == "1");
    }

    public boolean hasReputation()
    {
        String result = getXWiki().Param("xwiki.ratings.reputation", "0");
        result = getXWiki().getXWikiPreference("ratings_reputation", result, getXWikiContext());
        return (getConfigParameter(RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION, result) == "1");
    }

    public String[] getDefaultReputationMethods()
    {
        String method =
            getXWiki().Param("xwiki.ratings.reputation.defaultmethod", RATING_REPUTATION_METHOD_DEFAULT);
        method = getXWiki().getXWikiPreference("ratings_reputation_defaultmethod", method, getXWikiContext());
        method = getConfigParameter(RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_METHOD, method);
        return method.split(",");
    }

    public void updateAverageRatings(String documentName, Rating rating, int oldVote)
        throws RatingsException
    {
        String[] methods = getDefaultReputationMethods();
        for (int i = 0; i < methods.length; i++) {
            updateAverageRating(documentName, rating, oldVote, methods[i]);
        }
    }

    public AverageRating getAverageRatingFromQuery(String fromsql, String wheresql)
        throws RatingsException
    {
        return getAverageRatingFromQuery(fromsql, wheresql, RATING_REPUTATION_METHOD_AVERAGE);
    }

    public AverageRating getAverageRating(String documentName) throws RatingsException
    {
        return getAverageRating(documentName, RATING_REPUTATION_METHOD_AVERAGE);
    }

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

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Running average rating with sql " + sql);
            }
            getXWikiContext().put("lastsql", sql);

            List result = getXWiki().getStore().search(sql, 0, 0, getXWikiContext());
            float vote = ((Number) ((Object[]) result.get(0))[0]).floatValue();
            int nbvotes = ((Number) ((Object[]) result.get(0))[1]).intValue();

            AverageRating avgr = new MemoryAverageRating(null, nbvotes, vote / (float) nbvotes, method);
            return avgr;
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    public boolean removeRating(Rating rating) throws RatingsException
    {
        return rating.remove();
    }
    
    /**
     * Gets or calculates the user reputation.
     *
     * @param username Person to calculate the reputation for
     * @param context context of the request
     * @return AverageRating of the voter
     */
    public AverageRating getUserReputation(String username) throws ReputationException
    {
        try {
            return getAverageRating(username, RatingsManager.RATING_REPUTATION_METHOD_AVERAGE);
        } catch (RatingsException e) {
            throw new ReputationException(e);
        }
    }

    public AverageRating calcAverageRating(String documentName, String method)
        throws RatingsException
    {
        int nbVotes = 0;
        int balancedNbVotes = 0;
        float totalVote = 0;
        float averageVote = 0;
        List<Rating> ratings = getRatings(documentName, 0, 0, true);
        if (ratings == null) {
            return null;
        }
        for (Rating rating : ratings) {
            if (method.equals(RATING_REPUTATION_METHOD_BALANCED)) {
                String author = rating.getAuthor();
                // in case we are evaluating the average rating of a user
                // we should not include votes of himself to a user
                if (!author.equals(documentName)) {
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
        return new MemoryAverageRating(documentName, nbVotes, averageVote, method);
    }

    public void updateAverageRating(String documentName, Rating rating, int oldVote, String method)
        throws RatingsException
    {
        // we only update if we are in stored mode and if the vote changed
        if (isAverageRatingStored() && oldVote != rating.getVote()) {
            AverageRating aRating = calcAverageRating(documentName, method);
            AverageRating averageRating = getAverageRating(documentName, method, true);
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

    public void updateUserReputation(String author, AverageRating voterRating)
        throws RatingsException
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
    
    public AverageRating getAverageRating(String documentName, String method)
        throws RatingsException
    {
        return getAverageRating(documentName, method, false);
    }

    public AverageRating getAverageRating(String documentName, String method, boolean create)
        throws RatingsException
    {
        try {
            if (isAverageRatingStored()) {
                String className = getAverageRatingsClassName();
                XWikiDocument doc = getXWikiContext().getWiki().getDocument(documentName, getXWikiContext());
                BaseObject averageRatingObject =
                    doc.getObject(className, RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD, method,
                        false);
                if (averageRatingObject == null) {
                    if (!create) {
                        return calcAverageRating(documentName, method);
                    }

                    // initiate a new average rating object
                    averageRatingObject = doc.newObject(className, getXWikiContext());
                    averageRatingObject.setStringValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD,
                        method);
                }

                return new StoredAverageRating(doc, averageRatingObject, getXWikiContext());
            } else {
                return calcAverageRating(documentName, method);
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }
}
