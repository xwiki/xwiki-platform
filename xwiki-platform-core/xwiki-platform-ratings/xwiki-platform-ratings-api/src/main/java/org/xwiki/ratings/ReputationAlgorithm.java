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
 * @version $Id$
 */
@Role
public interface ReputationAlgorithm
{

    /**
     * Gets current ratings manager
     * 
     * @param documentRef the document which the ratings are for
     */
    RatingsManager getRatingsManager(DocumentReference documentRef);

    /**
     * Updates reputation after a vote
     * 
     * @param documentRef document on which the rating occured
     * @param rating rating set
     * @param oldVote previous rating set
     */
    public void updateReputation(DocumentReference documentRef, Rating rating, int oldVote);

    /**
     * Recalculates the contributor reputation. Only the creator of the document will have it's reputation updated
     *
     * @param voter Voter that will have it's reputation updated
     * @param documentRef Elements that was rated
     * @param rating New rating of the element, including who did the rating
     * @param oldVote previous vote of the user
     * @param context context of the request
     * @return AverageRating of the voter
     */
    AverageRating calcNewVoterReputation(DocumentReference voter, DocumentReference documentRef, Rating rating,
        int oldVote) throws ReputationException;

    /**
     * Recalculates the contributors reputation
     *
     * @param documentRef Elements that was rated
     * @param rating New rating of the element, including who did the rating
     * @param oldVote previous vote of the user
     * @param context context of the request
     * @return Map of AverageRating of each contributor of the page that has an updated reputation
     */
    Map<String, AverageRating> calcNewAuthorsReputation(DocumentReference documentRef, Rating rating, int oldVote)
        throws ReputationException;

    /**
     * Recalculates the contributor reputation. Only the creator of the document will have it's reputation updated
     *
     * @param contributor Contributor that will have it's reputation updated
     * @param documentRef Elements that was rated
     * @param rating New rating of the element, including who did the rating
     * @param oldVote previous vote of the user
     * @param context context of the request
     * @return AverageRating of the contributor
     */
    AverageRating calcNewContributorReputation(DocumentReference contributor, DocumentReference documentRef,
        Rating rating, int oldVote) throws ReputationException;

    /**
     * Recalculated all reputation of the wiki The result is given as a set of AverageRating objects That can be saved
     * to the user page
     *
     * @param context context of the request
     * @return Map of AverageRating of each user of the wiki
     */
    Map<String, AverageRating> recalcAllReputation() throws ReputationException;
}
