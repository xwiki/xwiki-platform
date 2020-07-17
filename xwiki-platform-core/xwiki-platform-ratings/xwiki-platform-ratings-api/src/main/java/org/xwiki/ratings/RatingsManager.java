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

import java.util.Collections;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWiki;

/**
 * @version $Id$
 * @since 6.4M3
 */
@Role
public interface RatingsManager
{
    String RATINGS_CLASSPAGE = "RatingsClass";

    String RATINGS_CLASSNAME = XWiki.SYSTEM_SPACE + '.' + RATINGS_CLASSPAGE;

    LocalDocumentReference RATINGS_CLASSREFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, RATINGS_CLASSPAGE);

    String AVERAGE_RATINGS_CLASSPAGE = "AverageRatingsClass";

    String AVERAGE_RATINGS_CLASSNAME = XWiki.SYSTEM_SPACE + '.' + AVERAGE_RATINGS_CLASSPAGE;

    LocalDocumentReference AVERAGE_RATINGS_CLASSREFERENCE =
        new LocalDocumentReference("XWiki", AVERAGE_RATINGS_CLASSPAGE);

    String RATING_CLASS_FIELDNAME_DATE = "date";

    String RATING_CLASS_FIELDNAME_AUTHOR = "author";

    String RATING_CLASS_FIELDNAME_VOTE = "vote";

    String RATING_CLASS_FIELDNAME_PARENT = "parent";

    String AVERAGERATING_CLASS_FIELDNAME_NBVOTES = "nbvotes";

    String AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE = "averagevote";

    String AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD = "method";

    String RATING_REPUTATION_METHOD_BALANCED = "balanced";

    String RATING_REPUTATION_METHOD_AVERAGE = "average";

    String RATING_REPUTATION_METHOD_DEFAULT = "average";

    String RATINGS_CONFIG_PARAM_PREFIX = "xwiki.ratings.";

    String RATINGS_CONFIG_GLOBAL_PAGE = "XWiki.RatingsConfig";

    LocalDocumentReference RATINGS_CONFIG_GLOBAL_REFERENCE = new LocalDocumentReference("XWiki", "RatingsConfig");

    String RATINGS_CONFIG_SPACE_PAGE = "WebPreferences";

    LocalDocumentReference RATINGS_CONFIG_CLASSREFERENCE = new LocalDocumentReference("XWiki", "RatingsConfigClass");

    String RATINGS_CONFIG_CLASS_FIELDNAME_MANAGER_HINT = "managerHint";

    String RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SPACE = "storageSpace";

    String RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SEPARATE_SPACES = "storageSeparateSpaces";

    String RATINGS_CONFIG_CLASS_FIELDNAME_STORE_AVERAGE_RATING = "storeAverageRating";

    String RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION = "reputation";

    String RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_STORED = "reputationStored";

    String RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_METHOD = "reputationMethod";

    String RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_ALGORITHM_HINT = "reputationAlgorithmHint";

    String RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_CUSTOM_ALGORITHM = "reputationCustomAlgorithm";

    String RATINGS_CONFIG_FIELDNAME_MANAGER_HINT = "managerHint";

    String RATINGS_CONFIG_FIELDNAME_REPUTATIONALGORITHM_HINT = "reputationAlgorithmHint";

    /**
     * Gets the ratings class.
     * 
     * @return the XWiki document representing the RatingsClass
     */
    String getRatingsClassName();

    /**
     * Gets a list of ratings.
     * Note that if count and start are both 0, retrieve all ratings.
     * 
     * @param documentRef the document to which the ratings belong to
     * @param start the offset from where to fetch the ratings
     * @param count how many ratings to fetch
     * @param asc sort the results in ascending order or not
     * @return a list of Rating objects
     * @throws RatingsException when an error occurs while fetching the list of ratings
     */
    List<Rating> getRatings(DocumentReference documentRef, int start, int count, boolean asc) throws RatingsException;

    /**
     * Retrieve the ratings performed by the given user.
     *
     * @param userReference the user who performed ratings.
     * @param start the offset from where to fetch the ratings
     * @param count how many ratings to fetch
     * @param asc if {@code true} sort the results in ascending date order else in descending date order.
     * @return a list of Rating objects made by the given user.
     * @throws RatingsException when an error occurs while fetching the list of ratings
     * @since 12.6
     */
    @Unstable
    default List<Rating> getRatings(UserReference userReference, int start, int count, boolean asc)
        throws RatingsException
    {
        return Collections.emptyList();
    }

    /**
     * Gets a rating based on its id.
     * 
     * @param ratingId the id of a certain rating
     * @return a Rating object containing all the rating information
     * @throws RatingsException when an error occurs while fetching the rating
     */
    Rating getRating(String ratingId) throws RatingsException;

    /**
     * Gets a rating based on a document and the id of the rating.
     * 
     * @param documentRef the document to which the rating belongs to
     * @param id the id of a certain rating
     * @return a Rating object containing all the rating information
     * @throws RatingsException when an error occurs while fetching the rating
     */
    Rating getRating(DocumentReference documentRef, int id) throws RatingsException;

    /**
     * Gets a rating based on a document and user.
     * 
     * @param documentRef the document to which the rating belongs to
     * @param user the author of the rating
     * @return a Rating object containing all the rating information
     * @throws RatingsException when an error occurs while fetching the rating
     */
    Rating getRating(DocumentReference documentRef, DocumentReference user) throws RatingsException;

    /**
     * Sets a rating based on a document, user and vote.
     * 
     * @param documentRef the document to which the rating belongs to
     * @param author the author of the rating
     * @param vote the author's vote
     * @return a Rating object containing all the rating information
     * @throws RatingsException when an error occurs while setting the rating
     */
    Rating setRating(DocumentReference documentRef, DocumentReference author, int vote) throws RatingsException;

    /**
     * Removes a rating.
     * 
     * @param rating the rating to be removed
     * @return whether the action was successful or not
     * @throws RatingsException when an error occurs while removing the rating
     */
    boolean removeRating(Rating rating) throws RatingsException;

    /**
     * Checks if the average rating stored in the rated document as an object or is it kept in memory.
     * 
     * @return answer to: is the average rating stored?
     */
    boolean isAverageRatingStored(DocumentReference documentRef);

    /**
     * Checks if the user reputation stored in a document or is it kept in memory.
     * 
     * @return answer to: is user reputation kept in memory?
     */
    boolean isReputationStored(DocumentReference documentRef);

    /**
     * Checks if to calculate the user reputation or not.
     * 
     * @return answer to: calculate the user reputation or not?
     */
    boolean hasReputation(DocumentReference documentRef);

    /**
     * Gets or calculates the user reputation.
     *
     * @param username person to calculate the reputation for
     * @return AverageRating of the voter
     */
    AverageRating getUserReputation(DocumentReference username) throws ReputationException;

    /**
     * Updates user reputation.
     * 
     * @param author the user for which to update the reputation
     * @param voterRating the users current reputation
     */
    void updateUserReputation(DocumentReference author, AverageRating voterRating) throws RatingsException;

    /**
     * Gets the default methods of calculating the user reputation.
     * 
     * @return the default methods of calculating the user reputation
     */
    String[] getDefaultReputationMethods(DocumentReference documentRef);

    /**
     * Gets the average rating for a document using the default average rating calculation method.
     * 
     * @param documentRef the document for which to calculate the average rating
     * @return the calculated average rating
     * @throws RatingsException when an error occurs while fetching the average rating
     */
    AverageRating getAverageRating(DocumentReference documentRef) throws RatingsException;

    /**
     * Gets the average rating calculated by the specified method.
     * 
     * @param documentRef the document for which to get the average rating
     * @param method the method of calculating the average rating
     * @throws RatingsException when encountering an error while fetching the average rating
     */
    AverageRating getAverageRating(DocumentReference documentRef, String method) throws RatingsException;

    /**
     * Gets the average rating with the option to store it if it's not yet stored.
     * 
     * @param documentRef the document for which to get the average rating
     * @param method the method of calculating the average rating
     * @param create create the average rating object on the rated document if stored ratings is active
     * @throws RatingsException when encountering an error while fetching the average rating
     */
    AverageRating getAverageRating(DocumentReference documentRef, String method, boolean create)
        throws RatingsException;

    /**
     * Gets the average rating for a SQL query.
     * 
     * @param fromsql the from clause of the SQL query
     * @param wheresql the where clause of the SQL query
     * @return the calculated average rating
     * @throws RatingsException when an error occurs while fetching the average rating
     */
    AverageRating getAverageRatingFromQuery(String fromsql, String wheresql) throws RatingsException;

    /**
     * Gets the average rating for a SQL query with specifying the average rating calculation method.
     * 
     * @param fromsql the from clause of the SQL query
     * @param wheresql the where clause of the SQL query
     * @param method the method used to calculate the average rating
     * @return the calculated average rating
     * @throws RatingsException when an error occurs while fetching the average rating
     */
    AverageRating getAverageRatingFromQuery(String fromsql, String wheresql, String method) throws RatingsException;

    /**
     * Calculates the average rating.
     * 
     * @param documentRef the document for which to calculate the average rating
     * @param method the method of calculating the average rating
     * @return the calculated average rating
     * @throws RatingsException when an error occurs while calculating the average rating
     */
    AverageRating calcAverageRating(DocumentReference documentRef, String method) throws RatingsException;

    /**
     * Updates the average rating. This is done only if the average rating is stored.
     * 
     * @param documentRef the document for which to update the average rating
     * @param rating the new rating which was performed
     * @param oldVote the value of the old rating
     * @param method the method of calculating the average rating
     * @throws RatingsException when an error occurred while updating the average rating
     */
    void updateAverageRating(DocumentReference documentRef, Rating rating, int oldVote, String method)
        throws RatingsException;

    /**
     * Updates the average rating of a document.
     * 
     * @param documentRef the document being rated
     * @param rating the new rating
     * @param oldVote the old vote
     * @throws RatingsException when an error occurs while updating the average rating
     */
    void updateAverageRatings(DocumentReference documentRef, Rating rating, int oldVote) throws RatingsException;
}
