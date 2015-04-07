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
import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;

/**
 * Event fired when updating a Rating.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Unstable
public class UpdateRatingEvent implements Event
{
    private DocumentReference documentRef;

    private Rating newRating;

    private int oldRating;

    /**
     * Default UpdateRatingEvent constructor.
     */
    public UpdateRatingEvent()
    {
    }

    /**
     * UpdateRatingEvent constructor.
     * 
     * @param documentRef the document which is being rated
     * @param newRating the new rating in the shape of a Rating object
     * @param oldRating the old rating value
     */
    public UpdateRatingEvent(DocumentReference documentRef, Rating newRating, int oldRating)
    {
        this.documentRef = documentRef;
        this.newRating = newRating;
        this.oldRating = oldRating;
    }

    /**
     * Gets the document reference of the document being rated.
     * 
     * @return document reference of the document being rated
     */
    public DocumentReference getDocumentReference()
    {
        return documentRef;
    }

    /**
     * Sets the document reference of the document being rated. 
     * 
     * @param documentRef the document being rated
     */
    public void setDocumentReference(DocumentReference documentRef)
    {
        this.documentRef = documentRef;
    }

    /**
     * Gets the new rating set.
     * 
     * @return Rating object representing the new rating
     */
    public Rating getNewRating()
    {
        return newRating;
    }

    /**
     * Sets the new rating.
     * 
     * @param newRating the new rating
     */
    public void setNewRating(Rating newRating)
    {
        this.newRating = newRating;
    }

    /**
     * Gets the old rating value.
     * 
     * @return the old rating value
     */
    public int getOldRating()
    {
        return oldRating;
    }

    /**
     * Sets the old rating value.
     * 
     * @param oldRating the old rating value
     */
    public void setOldRating(int oldRating)
    {
        this.oldRating = oldRating;
    }

    @Override
    public boolean matches(Object arg0)
    {
        return true;
    }
}
