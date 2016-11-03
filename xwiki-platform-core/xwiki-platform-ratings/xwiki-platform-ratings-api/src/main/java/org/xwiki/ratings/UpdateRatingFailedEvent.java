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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.EndEvent;

/**
 * Event fired when failing to update a Rating.
 *
 * @version $Id$
 * @since 7.1M1
 */
public class UpdateRatingFailedEvent extends AbstractRatingEvent implements EndEvent
{
    /**
     * Default UpdateRatingEvent constructor.
     */
    public UpdateRatingFailedEvent()
    {
    }

    /**
     * UpdateRatingEvent constructor.
     * 
     * @param documentRef the document which is being rated
     * @param newRating the new rating in the shape of a Rating object
     * @param oldRating the old rating value
     */
    public UpdateRatingFailedEvent(DocumentReference documentRef, Rating newRating, int oldRating)
    {
        super(documentRef, newRating, oldRating);
    }

    @Override
    public boolean matches(Object arg0)
    {
        return arg0 instanceof UpdateRatingFailedEvent;
    }
}
