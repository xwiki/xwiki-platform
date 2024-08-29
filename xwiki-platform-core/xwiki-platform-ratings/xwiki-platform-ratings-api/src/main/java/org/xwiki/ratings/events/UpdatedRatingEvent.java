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
import org.xwiki.ratings.RatingsManager;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Event sent whenever a {@link Rating} is updated.
 * The event is sent with the following informations:
 *   - source: the identifier of the {@link RatingsManager}
 *   - data: the {@link Rating} updated.
 *
 * @version $Id$
 * @since 12.9RC1
 */
public class UpdatedRatingEvent extends AbstractRatingEvent implements Event
{
    private int oldVote;

    /**
     * Default constructor.
     *
     * @param rating the updated rating.
     * @param oldVote the old vote of the rating before the update.
     */
    public UpdatedRatingEvent(Rating rating, int oldVote)
    {
        super(rating);
        this.oldVote = oldVote;
    }

    /**
     * @return the old vote, before the update.
     */
    public int getOldVote()
    {
        return oldVote;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof UpdatedRatingEvent;
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

        UpdatedRatingEvent that = (UpdatedRatingEvent) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(oldVote, that.oldVote)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(super.hashCode())
            .append(oldVote)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .appendSuper(super.toString())
            .append("oldVote", oldVote)
            .toString();
    }
}
