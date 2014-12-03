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

import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsException;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 * @see AverageRating
 */
public class MemoryAverageRating implements AverageRating
{
    private String documentName;

    private int nbVotes;

    private float averageVote;

    private String method;

    public MemoryAverageRating(String documentName, int nbVotes, float averageVote, String method)
    {
        this.documentName = documentName;
        this.nbVotes = nbVotes;
        this.averageVote = averageVote;
        this.method = method;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public void setDocumentName(String documentName)
    {
        this.documentName = documentName;
    }

    public int getNbVotes()
    {
        return nbVotes;
    }

    public void setNbVotes(int nbVotes)
    {
        this.nbVotes = nbVotes;
    }

    public float getAverageVote()
    {
        return averageVote;
    }

    public void setAverageVote(float averageVote)
    {
        this.averageVote = averageVote;
    }

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method)
    {
        this.method = method;
    }

    public void save() throws RatingsException
    {
    }
}
