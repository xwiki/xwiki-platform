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

import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsManager;

/**
 * Event sent whenever a new {@link Rating} is recorded.
 * The event is sent with the following informations:
 *   - source: the identifier of the {@link RatingsManager}
 *   - data: the {@link Rating} created.
 *
 * @version $Id$
 * @since 12.9RC1
 */
public class CreatedRatingEvent extends AbstractRatingEvent
{
    /**
     * Default constructor.
     * @param rating the rating that has been created.
     */
    public CreatedRatingEvent(Rating rating)
    {
        super(rating);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof CreatedRatingEvent;
    }
}
