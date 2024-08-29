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
package org.xwiki.ratings.events;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.observation.event.Event;
import org.xwiki.ratings.Rating;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Abstract helper for all rating events.
 *
 * @version $Id$
 * @since 12.9RC1
 */
public abstract class AbstractRatingEvent implements Event
{
    private Rating rating;

    /**
     * Default constructor.
     *
     * @param newRating the new rating information
     */
    public AbstractRatingEvent(Rating newRating)
    {
        this.rating = newRating;
    }

    /**
     * @return the rating concerned by this event.
     */
    public Rating getRating()
    {
        return this.rating;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractRatingEvent that = (AbstractRatingEvent) o;

        return new EqualsBuilder()
            .append(rating, that.rating)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(19, 41)
            .append(rating)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("rating", rating)
            .toString();
    }
}
