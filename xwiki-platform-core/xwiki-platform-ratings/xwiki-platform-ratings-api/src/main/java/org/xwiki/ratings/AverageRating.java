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

import java.util.Date;

import org.xwiki.model.reference.EntityReference;

/**
 * General interface to provide information about average rating notation.
 *
 * @version $Id$
 * @since 12.9RC1
 */
public interface AverageRating
{
    /**
     * @return the identifier of this average rating data.
     */
    String getId();

    /**
     * @return the identifier of the manager who handles this average rating data.
     */
    String getManagerId();

    /**
     * @return the reference of the element this average rating is for.
     */
    EntityReference getReference();

    /**
     * @return the actual average rating.
     */
    float getAverageVote();

    /**
     * @return the total number of ratings used to compute this average.
     */
    int getNbVotes();

    /**
     * @return the upper bound scale of the ratings.
     */
    int getScaleUpperBound();

    /**
     * @return the date of the last modification of this average.
     */
    Date getUpdatedAt();

    /**
     * Update the average rating by performing a change in an existing rating.
     *
     * @param oldRating the old rating value to be modified.
     * @param newRating the new rating value to be applied.
     * @return the current instance modified.
     */
    AverageRating updateRating(int oldRating, int newRating);

    /**
     * Update the average rating by removing the given rating.
     *
     * @param rating the old rating to be removed.
     * @return the current instance modified.
     */
    AverageRating removeRating(int rating);

    /**
     * Update the average rating by adding a new rating.
     *
     * @param rating the new rating to be taken into account.
     * @return the current instance modified.
     */
    AverageRating addRating(int rating);
}
