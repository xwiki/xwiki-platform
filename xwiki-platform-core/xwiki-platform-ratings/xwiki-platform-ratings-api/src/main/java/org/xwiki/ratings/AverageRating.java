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
 */
public interface AverageRating
{
    /**
     * @return the number of votes for the container this average rating represents.
     */
    public int getNbVotes();

    /**
     * Sets the number of votes for the container this average rating represents.
     *
     * @param nbVotes the total number of votes for the container represented by this average rating.
     */
    public void setNbVotes(int nbVotes);

    /**
     * @return the average vote for this container.
     */
    public float getAverageVote();

    /**
     * @param averageVote the average vote
     */
    public void setAverageVote(float averageVote);

    /**
     * @return the method used to compute the average rating
     */
    public String getMethod();

    /**
     * @param method the method used to compute the average rating
     */
    public void setMethod(String method);

    /**
     * Saves this average rating.
     *
     * @param context the XWiki context
     * @throws RatingsException when an error occurs while saving this average rating.
     */
    public void save() throws RatingsException;
}
