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
package org.xwiki.ratings.internal;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsException;

/**
 * @version $Id$
 * @see AverageRating
 */
public class MemoryAverageRating implements AverageRating
{
    private DocumentReference documentRef;

    private int nbVotes;

    private float averageVote;

    private String method;

    /**
     * MemoryAverageRating constructor.
     * 
     * @param documentRef the document with which the average rating is associated
     * @param nbVotes the total number of votes
     * @param averageVote the average rating
     * @param method the method to use when calculating the average
     */
    public MemoryAverageRating(DocumentReference documentRef, int nbVotes, float averageVote, String method)
    {
        this.documentRef = documentRef;
        this.nbVotes = nbVotes;
        this.averageVote = averageVote;
        this.method = method;
    }

    /**
     * Gets the document with which the average rating is associated.
     * 
     * @return the document with which the average rating is associated
     */
    public DocumentReference getDocumentReference()
    {
        return documentRef;
    }

    /**
     * Sets the document with which the average rating is associated.
     * 
     * @param documentRef the document with which the average rating is associated
     */
    public void setDocumentReference(DocumentReference documentRef)
    {
        this.documentRef = documentRef;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#getNbVotes()
     */
    public int getNbVotes()
    {
        return nbVotes;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#setNbVotes()
     */
    public void setNbVotes(int nbVotes)
    {
        this.nbVotes = nbVotes;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#getAverageVote()
     */
    public float getAverageVote()
    {
        return averageVote;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#setAverageVote()
     */
    public void setAverageVote(float averageVote)
    {
        this.averageVote = averageVote;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#getMethod()
     */
    public String getMethod()
    {
        return method;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#setMethod()
     */
    public void setMethod(String method)
    {
        this.method = method;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#save()
     */
    public void save() throws RatingsException
    {
    }
}
