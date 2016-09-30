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
package org.xwiki.ratings.script;

import java.util.Date;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.ratings.Rating;

/**
 * Wrapper around a {@link Rating}, typically returned by the {@link RatingsScriptService} and manipulated using a scripting
 * language in the wiki.
 *
 * @version $Id$
 * @see Rating
 * @since 6.4M3
 */
public class RatingApi
{
    /**
     * The wrapped rating.
     */
    protected Rating rating;

    /**
     * Constructor of this wrapper.
     *
     * @param rating the wrapped rating
     */
    public RatingApi(Rating rating)
    {
        this.rating = rating;
    }

    /**
     * Gets the rating.
     * 
     * @return the wrapped rating
     */
    protected Rating getRating()
    {
        return rating;
    }

    /**
     * Gets the global rating id.
     * 
     * @return the global rating id
     */
    public String getGlobalRatingId()
    {
        return rating.getGlobalRatingId();
    }

    /**
     * Gets the vote for the rating.
     * 
     * @return the vote for the rating consisting of the number of stars the author gave
     */
    public int getVote()
    {
        if (rating == null) {
            return 0;
        } else {
            return rating.getVote();
        }
    }

    /**
     * Gets the rating author.
     * 
     * @return a document reference to the rating author
     */
    public DocumentReference getAuthor()
    {
        return rating.getAuthor();
    }

    /**
     * Gets the rating date.
     * 
     * @return the date when the rating was done
     */
    public Date getDate()
    {
        return rating.getDate();
    }
}
