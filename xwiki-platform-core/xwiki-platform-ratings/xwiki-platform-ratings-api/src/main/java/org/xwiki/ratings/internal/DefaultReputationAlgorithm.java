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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.ConfiguredProvider;
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
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private ConfiguredProvider<RatingsManager> ratingsManagerProvider;

    /**
     * Retrieves the XWiki context from the current execution context.
     * 
     * @return the XWiki context.
     * @throws RuntimeException if there was an error retrieving the context.
     */
    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * Retrieves the XWiki private API object.
     * 
     * @return the XWiki private API object.
     */
    protected XWiki getXWiki()
    {
        return getXWikiContext().getWiki();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.ReputationAlgorithm#getRatingsManager()
     */
    public RatingsManager getRatingsManager(DocumentReference documentRef)
    {
        return ratingsManagerProvider.get(documentRef);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.ReputationAlgorithm#updateReputation()
     */
    public void updateReputation(DocumentReference documentRef, Rating rating, int oldVote)
    {
        // we only update if we are in stored mode and if the vote changed
        if (oldVote != rating.getVote()) {
            // voter reputation. This will give points to the voter
            try {
                AverageRating voterRating = calcNewVoterReputation(rating.getAuthor(), documentRef, rating, oldVote);
                // we need to save this reputation if it has changed
                try {
                    getRatingsManager(documentRef).updateUserReputation(rating.getAuthor(), voterRating);
                } catch (RatingsException re) {
                    if (logger.isErrorEnabled()) {
                        logger.error("Error while storing reputation for user " + rating.getAuthor(), re);
                    }
                }
            } catch (ReputationException e) {
                if (e.getCode() != ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED) {
                    // we should log this error
                    if (logger.isErrorEnabled()) {
                        logger.error("Error while calculating voter reputation " + rating.getAuthor()
                            + " for document " + documentRef, e);
                    }
                }
            }

            // author reputation. This will be giving points to the creator of a document or comment
            try {
                XWikiDocument doc = getXWiki().getDocument(documentRef, getXWikiContext());
                AverageRating authorRating =
                    calcNewContributorReputation(doc.getCreatorReference(), documentRef, rating, oldVote);
                // we need to save the author reputation
                try {
                    getRatingsManager(documentRef).updateUserReputation(doc.getCreatorReference(), authorRating);
                } catch (RatingsException re) {
                    if (logger.isErrorEnabled()) {
                        logger.error("Error while storing reputation for user " + doc.getCreatorReference().getName(),
                            re);
                    }
                }

            } catch (ReputationException e) {
                if (e.getCode() != ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED) {
                    // we should log this error
                    if (logger.isErrorEnabled()) {
                        logger.error("Error while calculating author reputation for document " + documentRef, e);
                    }
                }
            } catch (XWikiException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error while calculating author reputation for document " + documentRef, e);
                }
            }

            // all authors reputation. This will be used to give points to all participants to a document
            try {
                Map<String, AverageRating> authorsRatings = calcNewAuthorsReputation(documentRef, rating, oldVote);
                // TODO this is not implemented yet
            } catch (ReputationException e) {
                if (e.getCode() != ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED) {
                    // we should log this error
                    if (logger.isErrorEnabled()) {
                        logger.error("Error while calculating authors reputation for document " + documentRef, e);
                    }
                }
            } catch (XWikiException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error while calculating authors for document " + documentRef, e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.ReputationAlgorithm#calcNewVoterReputation()
     */
    public AverageRating calcNewVoterReputation(DocumentReference voter, DocumentReference documentRef, Rating rating,
        int oldVote) throws ReputationException
    {
        notimplemented();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.ReputationAlgorithm#calcNewContributorReputation()
     */
    public AverageRating calcNewContributorReputation(DocumentReference contributor, DocumentReference documentRef,
        Rating rating, int oldVote) throws ReputationException
    {
        notimplemented();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.ReputationAlgorithm#calcNewAuthorsReputation()
     */
    public Map<String, AverageRating> calcNewAuthorsReputation(DocumentReference documentRef, Rating rating, int oldVote)
        throws ReputationException
    {
        notimplemented();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.ReputationAlgorithm#recalcAllReputation()
     */
    public Map<String, AverageRating> recalcAllReputation() throws ReputationException
    {
        notimplemented();
        return null;
    }

    /**
     * Marks methods that have not been implemented.
     * 
     * @throws ReputationException when the method is called
     */
    protected void notimplemented() throws ReputationException
    {
        throw new ReputationException(ReputationException.MODULE_PLUGIN_RATINGS_REPUTATION,
            ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED, "Not implemented");
    }
}
