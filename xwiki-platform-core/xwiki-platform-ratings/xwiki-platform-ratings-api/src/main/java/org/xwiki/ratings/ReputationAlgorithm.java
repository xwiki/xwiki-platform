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

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * Algorithm to calculate a user's reputation.
 * 
 * @version $Id$
 */
@Role
public interface ReputationAlgorithm
{

    /**
     * Gets current ratings manager.
     * 
     * @param documentRef the document which the ratings are for
     * @return the current RatingsManager
     */
    RatingsManager getRatingsManager(DocumentReference documentRef);

    /**
     * Updates reputation after a vote.
     * 
     * @param documentRef document on which the rating occurred
     * @param rating rating set
     * @param oldVote previous vote value
     */
    void updateReputation(DocumentReference documentRef, Rating rating, int oldVote);

    /**
     * Recalculates the contributor reputation. Only the creator of the document will have it's reputation updated.
     *
     * @param voter Voter that will have it's reputation updated
     * @param documentRef the document that was rated
     * @param rating the new rating of the element, including who did the rating
     * @param oldVote previous vote of the user
     * @return AverageRating of the voter
     * @throws ReputationException when an error occurs while calculating the new reputation
     */
    AverageRating calcNewVoterReputation(DocumentReference voter, DocumentReference documentRef, Rating rating,
        int oldVote) throws ReputationException;

    /**
     * Recalculates the contributors reputation.
     *
     * @param documentRef the document that was rated
     * @param rating the new rating of the element, including who did the rating
     * @param oldVote previous vote of the user
     * @return map of AverageRating of each contributor of the page that has an updated reputation
     * @throws ReputationException when an error occurs while calculating the new reputation
     */
    Map<String, AverageRating> calcNewAuthorsReputation(DocumentReference documentRef, Rating rating, int oldVote)
        throws ReputationException;

    /**
     * Recalculates the contributor reputation. Only the creator of the document will have his reputation updated.
     *
     * @param contributor the contributor that will have his reputation updated
     * @param documentRef the document that was rated
     * @param rating new rating of the element, including who did the rating
     * @param oldVote previous vote of the user
     * @return AverageRating of the contributor
     * @throws ReputationException when an error occurs while calculating the new reputation
     */
    AverageRating calcNewContributorReputation(DocumentReference contributor, DocumentReference documentRef,
        Rating rating, int oldVote) throws ReputationException;

    /**
     * Recalculates all the reputations of the wiki. The result is given as a set of AverageRating objects that can be
     * saved on the user page.
     *
     * @return map of AverageRating of each user of the wiki
     * @throws ReputationException when an error occurs while recalculating all the user reputations
     */
    Map<String, AverageRating> recalcAllReputation() throws ReputationException;
}
