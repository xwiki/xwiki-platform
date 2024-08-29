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
import org.xwiki.ratings.AverageRating;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Abstract event for {@link AverageRating} updates.
 *
 * @version $Id$
 * @since 12.9RC1
 */
public abstract class AbstractAverageRatingEvent implements Event
{
    private AverageRating newAverageRating;
    private float oldAverageVote;
    private int oldTotalVote;

    /**
     * Default constructor.
     */
    public AbstractAverageRatingEvent()
    {
    }

    /**
     * Default constructor.
     *
     * @param newAverageRating the new average rating.
     * @param oldAverageVote the old average value, before the update.
     * @param oldTotalVote the old total number of vote, before the update.
     */
    public AbstractAverageRatingEvent(AverageRating newAverageRating, float oldAverageVote, int oldTotalVote)
    {
        this.newAverageRating = newAverageRating;
        this.oldAverageVote = oldAverageVote;
        this.oldTotalVote = oldTotalVote;
    }

    /**
     * @return the old average value, before the update.
     */
    public double getOldAverageVote()
    {
        return oldAverageVote;
    }

    /**
     * @return the old total number of votes, before the update.
     */
    public long getOldTotalVote()
    {
        return oldTotalVote;
    }

    /**
     * @return the new average rating information.
     */
    public AverageRating getNewAverageRating()
    {
        return newAverageRating;
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

        AbstractAverageRatingEvent that = (AbstractAverageRatingEvent) o;

        return new EqualsBuilder()
            .append(newAverageRating, that.newAverageRating)
            .append(oldAverageVote, that.oldAverageVote)
            .append(oldTotalVote, that.oldTotalVote)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(newAverageRating)
            .append(oldAverageVote)
            .append(oldTotalVote)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("newAverageRating", newAverageRating)
            .append("oldAverageVote", oldAverageVote)
            .append("oldTotalVote", oldTotalVote)
            .toString();
    }
}
