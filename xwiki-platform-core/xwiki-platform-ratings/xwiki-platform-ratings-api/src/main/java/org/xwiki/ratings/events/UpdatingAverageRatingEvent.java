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

import org.xwiki.observation.event.BeginEvent;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsManager;

/**
 * Event sent whenever an {@link AverageRating} is updated. The event is sent with the following information:
 * <ul>
 *   <li>source: the identifier of the {@link RatingsManager}</li>
 *   <li>data: a {@link java.util.List} of updated {@link AverageRating}</li>
 * </ul>
 *
 * @version $Id$
 * @since 12.9RC1
 */
public class UpdatingAverageRatingEvent extends AbstractAverageRatingEvent implements BeginEvent
{
    /**
     * Default constructor.
     */
    public UpdatingAverageRatingEvent()
    {
    }

    /**
     * Default constructor.
     *
     * @param newAverageRating the new average rating.
     * @param oldAverageVote the old average value, before the update.
     * @param oldTotalVote the old total number of vote, before the update.
     */
    public UpdatingAverageRatingEvent(AverageRating newAverageRating, float oldAverageVote, int oldTotalVote)
    {
        super(newAverageRating, oldAverageVote, oldTotalVote);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof UpdatingAverageRatingEvent;
    }
}
