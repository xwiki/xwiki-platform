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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.ReputationAlgorithm;
import org.xwiki.ratings.ReputationException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default very simple reputation algorithm. It won't include recalculation put only flow level reputation
 *
 * @version $Id$
 * @see ReputationAlgorithm
 */
@Component
@Singleton
public class DefaultReputationAlgorithm implements ReputationAlgorithm
{   
    @Inject
    Logger LOGGER;
    
    @Inject
    Execution execution;
    
    @Inject
    Provider<RatingsManager> ratingsManagerProvider;
  
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
   
    public RatingsManager getRatingsManager() {
        return ratingsManagerProvider.get();
    }
        
    public void updateReputation(String documentName, Rating rating, int oldVote)
    {
        // we only update if we are in stored mode and if the vote changed
        if (oldVote != rating.getVote()) {
            // voter reputation. This will give points to the voter
            try {
                AverageRating voterRating =
                    calcNewVoterReputation(rating.getAuthor(), documentName, rating, oldVote);
                // we need to save this reputation if it has changed
                try {
                 getRatingsManager().updateUserReputation(rating.getAuthor(), voterRating);
                } catch (RatingsException re) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("Error while storing reputation for user " + rating.getAuthor(), re);
                    }                    
                }
            } catch (ReputationException e) {
                if (e.getCode() != ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED) {
                    // we should log this error
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("Error while calculating voter reputation " + rating.getAuthor() + " for document "
                            + documentName, e);
                    }
                }
            }

            // author reputation. This will be giving points to the creator of a document or comment
            try {
                XWikiDocument doc = getXWiki().getDocument(documentName, getXWikiContext());
                AverageRating authorRating = calcNewContributorReputation(doc.getCreator(), documentName, rating, oldVote);
                // we need to save the author reputation
                try {
                    getRatingsManager().updateUserReputation(doc.getCreator(), authorRating);
                } catch (RatingsException re) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("Error while storing reputation for user " + doc.getCreator(), re);
                    }                    
                }
 
            } catch (ReputationException e) {
                if (e.getCode() != ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED) {
                    // we should log this error
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("Error while calculating author reputation for document "
                            + documentName, e);
                    }
                }
            } catch (XWikiException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Error while calculating author reputation for document " + documentName,
                        e);
                }
            }

            // all authors reputation. This will be used to give points to all participants to a document
            try {
                Map<String, AverageRating> authorsRatings = calcNewAuthorsReputation(documentName, rating, oldVote);
                // TODO this is not implemented yet
            } catch (ReputationException e) {
                if (e.getCode() != ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED) {
                    // we should log this error
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("Error while calculating authors reputation for document "
                            + documentName, e);
                    }
                }
            } catch (XWikiException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Error while calculating authors for document " + documentName, e);
                }
            }
        }
    }
    
    /**
     * Not implemented. Voters don't receive reputation
     */
    public AverageRating calcNewVoterReputation(String voter, String documentName, Rating rating, int oldVote) throws ReputationException
    {
        notimplemented();
        return null;
    }

    /**
     * Implemented. Authors will receive a simple reputation.
     */
    public AverageRating calcNewContributorReputation(String contributor, String documentName, Rating rating, int oldVote) throws ReputationException
    {
        notimplemented();
        return null;
    }

    /**
     * Not implemented
     */
    public Map<String, AverageRating> calcNewAuthorsReputation(String documentName, Rating rating, int oldVote) throws ReputationException
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
