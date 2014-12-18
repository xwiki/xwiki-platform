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

public class UpdateRatingEvent implements Event
{
    private DocumentReference documentRef;
    
    private Rating newRating;
    
    private int oldRating;

    public DocumentReference getDocumentReference()
    {
        return documentRef;
    }

    public void setDocumentReference(DocumentReference documentRef)
    {
        this.documentRef = documentRef;
    }

    public Rating getNewRating()
    {
        return newRating;
    }

    public void setNewRating(Rating newRating)
    {
        this.newRating = newRating;
    }

    public int getOldRating()
    {
        return oldRating;
    }

    public void setOldRating(int oldRating)
    {
        this.oldRating = oldRating;
    }
   
    public UpdateRatingEvent() { 
    }
    
    public UpdateRatingEvent(DocumentReference documentRef, Rating newRating, int oldRating) {
        this.documentRef = documentRef;
        this.newRating = newRating;
        this.oldRating = oldRating;
    }


    @Override
    public boolean matches(Object arg0)
    {
        return true;
    }
}
