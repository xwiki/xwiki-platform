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

import java.util.List;

import org.xwiki.component.annotation.Role;

@Role
public interface RatingsManager
{
    public static final String RATINGS_CLASSNAME = "XWiki.RatingsClass";
    public static final String AVERAGE_RATINGS_CLASSNAME = "XWiki.AverageRatingsClass";
    public static final String RATING_CLASS_FIELDNAME_DATE = "date";
    public static final String RATING_CLASS_FIELDNAME_AUTHOR = "author";
    public static final String RATING_CLASS_FIELDNAME_VOTE = "vote";
    public static final String RATING_CLASS_FIELDNAME_PARENT = "parent";
    public static final String AVERAGERATING_CLASS_FIELDNAME_NBVOTES = "nbvotes";
    public static final String AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE = "averagevote";
    public static final String AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD = "method";
    public static final String RATING_REPUTATION_METHOD_BALANCED = "balanced";
    public static final String RATING_REPUTATION_METHOD_AVERAGE = "average";
    public static final String RATING_REPUTATION_METHOD_DEFAULT = "average";
    
    public static final String RATINGS_CONFIG_PARAM_PREFIX = "xwiki.ratings.";
    public static final String RATINGS_CONFIG_GLOBAL_PAGE = "XWiki.RatingsConfig";
    public static final String RATINGS_CONFIG_SPACE_PAGE = "WebPreferences";
    public static final String RATINGS_CONFIG_CLASSNAME = "XWiki.RatingsConfigClass";
    public static final String RATINGS_CONFIG_CLASS_FIELDNAME_MANAGER_HINT = "managerHint";
    public static final String RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SPACE = "storageSpace";
    public static final String RATINGS_CONFIG_CLASS_FIELDNAME_STORAGE_SEPARATE_SPACES = "storageSeparateSpaces";
    public static final String RATINGS_CONFIG_CLASS_FIELDNAME_STORE_AVERAGE_RATING = "storeAverageRating";
    public static final String RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION = "reputation";
    public static final String RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_STORED = "reputationStored";
    public static final String RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_METHOD = "reputationMethod";
    public static final String RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_ALGORITHM_HINT = "reputationAlgorithmHint";
    public static final String RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_CUSTOM_ALGORITHM = "reputationCustomAlgorithm";
    public static final String RATINGS_CONFIG_FIELDNAME_MANAGER_HINT = "managerHint";
    public static final String RATINGS_CONFIG_FIELDNAME_REPUTATIONALGORITHM_HINT = "reputationAlgorithmHint";
 
    String getRatingsClassName();

    List<Rating> getRatings(String documentName, int start, int count, boolean asc)
        throws RatingsException;

    Rating getRating(String ratingId) throws RatingsException;

    Rating getRating(String documentName, int id) throws RatingsException;

    Rating getRating(String documentName, String user) throws RatingsException;

    Rating setRating(String documentName, String author, int vote) throws RatingsException;

    boolean removeRating(Rating rating) throws RatingsException;

    // average rating and reputation

    boolean isAverageRatingStored();

    boolean isReputationStored();

    boolean hasReputation();
    
    AverageRating getUserReputation(String username) throws ReputationException;
        
    void updateUserReputation(String author, AverageRating voterRating) throws RatingsException;

    String[] getDefaultReputationMethods();

    AverageRating getAverageRating(String documentName) throws RatingsException;

    AverageRating getAverageRating(String documentName, String method) throws RatingsException;

    AverageRating getAverageRating(String documentName, String method, boolean create)
        throws RatingsException;

    AverageRating getAverageRatingFromQuery(String fromsql, String wheresql)
        throws RatingsException;

    AverageRating getAverageRatingFromQuery(String fromsql, String wheresql, String method)
        throws RatingsException;

    AverageRating calcAverageRating(String documentName, String method) throws RatingsException;

    void updateAverageRating(String documentName, Rating rating, int oldVote, String method)
        throws RatingsException;

    void updateAverageRatings(String documentName, Rating rating, int oldVote)
        throws RatingsException;

}
