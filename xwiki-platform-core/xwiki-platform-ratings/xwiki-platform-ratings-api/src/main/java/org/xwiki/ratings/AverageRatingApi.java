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

/**
 * Api wrapper for an average rating.
 *
 * @version $Id$
 * @see AverageRating
 */
public class AverageRatingApi
{
    /**
     * The wrapped average rating.
     */
    protected AverageRating averageRating;

    /**
     * Constructor of this average rating wrapper.
     *
     * @param arating the wrapped rating
     */
    public AverageRatingApi(AverageRating arating)
    {
        this.averageRating = arating;
    }

    /**
     * Gets the number of votes for this average rating.
     * 
     * @return the number of votes for this average rating
     * @see AverageRating#getNbVotes()
     */
    public int getNbVotes()
    {
        if (averageRating == null) {
            return 0;
        } else {
            return averageRating.getNbVotes();
        }
    }

    /**
     * Gets the average vote.
     * 
     * @return the average vote
     * @see AverageRating#getAverageVote()
     */
    public float getAverageVote()
    {
        if (averageRating == null) {
            return 0;
        } else {
            return averageRating.getAverageVote();
        }
    }

    /**
     * Gets the method used to compute the average rating.
     * 
     * @return the method used to compute the average rating
     * @see AverageRating#getMethod()
     */
    public String getMethod()
    {
        if (averageRating == null) {
            return "";
        } else {
            return averageRating.getMethod();
        }
    }
}
