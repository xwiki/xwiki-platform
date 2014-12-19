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
import java.util.Map;

import javax.inject.Singleton;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.ReputationAlgorithm;
import org.xwiki.ratings.ReputationException;

import com.xpn.xwiki.XWikiException;

/**
 * Default very simple reputation algorithm. It won't include recalculation put only flow level reputation
 *
 * @version $Id$
 * @see ReputationAlgorithm
 */
@Component
@Singleton
@Named("simple")
public class SimpleReputationAlgorithm extends DefaultReputationAlgorithm
{
    protected float totalReputation;

    protected float constantX = -2;

    protected float constantY = 50;

    /**
     * Gets or calculates the user reputation.
     *
     * @param documentRef the document which the ratings are for
     * @param username Person to calculate the reputation for
     * @param context context of the request
     * @return AverageRating of the voter
     */
    public AverageRating getUserReputation(DocumentReference documentRef, DocumentReference username) throws ReputationException
    {
        try {
            AverageRating aveRating =
                getRatingsManager(documentRef).getAverageRating(username, RatingsManager.RATING_REPUTATION_METHOD_AVERAGE);
            float oldRep = aveRating.getAverageVote();
            aveRating.setAverageVote(aveRating.getAverageVote() * 100 / getTotalReputation());
            totalReputation += aveRating.getAverageVote() - oldRep;
            return aveRating;
        } catch (RatingsException e) {
            throw new ReputationException(e);
        }
    }

    /**
     * Not implemented. Voters don't receive reputation
     */
    public AverageRating calcNewVoterReputation(DocumentReference voter, DocumentReference documentRef, Rating rating, int oldVote) throws ReputationException
    {
        notimplemented();
        return null;
    }

    /**
     * Implemented. Authors will receive a simple reputation.
     */
    public AverageRating calcNewContributorReputation(DocumentReference contributor, DocumentReference documentRef, Rating rating, int oldVote) throws ReputationException
    {
        DocumentReference voter = rating.getAuthor();
        float voterRep = getUserReputation(documentRef, voter).getAverageVote();
        float constantX = getConstantX();
        float constantY = getConstantY();
        AverageRating currentRep = getUserReputation(documentRef, contributor);
        currentRep.setAverageVote(currentRep.getAverageVote() + (rating.getVote() + constantX) * voterRep / constantY);
        notimplemented();
        return null;
    }

    private float getTotalReputation()
    {
        if (totalReputation == 0) {
            // recalculate it
            try {
                List<Float> result =
                        getXWiki()
                        .search(
                            "select sum(prop.value) from XWikiDocument as doc, BaseObject as obj, FloatProperty as prop where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers' and obj.id=prop.id.id and prop.id.name='averagevote'",
                            getXWikiContext());
                if ((result == null) || (result.size() == 0)) {
                    totalReputation = 0;
                } else {
                    totalReputation = result.get(0).floatValue();
                }
            } catch (XWikiException e) {
                totalReputation = 0;
            }
        }
        return (totalReputation <= 1) ? 1 : totalReputation;
    }

    private float getConstantX()
    {
        return constantX;
    }

    private float getConstantY()
    {
        return constantY;
    }

    /**
     * Not implemented
     */
    public Map<String, AverageRating> calcNewAuthorsReputation(DocumentReference documentRef, Rating rating, int oldVote) throws ReputationException
    {
        notimplemented();
        return null;
    }

    /**
     * Not implemented
     */
    public Map<String, AverageRating> recalcAllReputation() throws ReputationException
    {
        notimplemented();
        return null;
    }

    protected void notimplemented() throws ReputationException
    {
        throw new ReputationException(ReputationException.MODULE_PLUGIN_RATINGS_REPUTATION,
            ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED, "Not implemented");
    }
}
