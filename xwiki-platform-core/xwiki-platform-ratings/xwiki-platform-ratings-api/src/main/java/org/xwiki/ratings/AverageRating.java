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
 * Represents a computed average rating for a container or a set of containers.
 *
 * @version $Id$
 * @since 6.4M3
 */
public interface AverageRating
{
    /**
     * Gets the number of votes.
     * 
     * @return the number of votes for the container this average rating represents
     */
    int getNbVotes();

    /**
     * Sets the number of votes for the container this average rating represents.
     *
     * @param nbVotes the total number of votes for the container represented by this average rating
     */
    void setNbVotes(int nbVotes);

    /**
     * Gets the average vote.
     * 
     * @return the average vote for this container.
     */
    float getAverageVote();

    /**
     * Sets the average vote.
     * 
     * @param averageVote the average vote
     */
    void setAverageVote(float averageVote);

    /**
     * Gets the method used to compute the average rating.
     * 
     * @return the method used to compute the average rating
     */
    String getMethod();

    /**
     * Sets the method used to compute the average rating.
     * 
     * @param method the method used to compute the average rating
     */
    void setMethod(String method);

    /**
     * Saves this average rating.
     *
     * @throws RatingsException when an error occurs while saving this average rating
     */
    void save() throws RatingsException;
}
