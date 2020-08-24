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

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.text.XWikiToStringBuilder;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Ratings stored in Solr.
 *
 * @version $Id$
 * @since 12.7RC1
 */
public class SolrRating implements Rating
{
    private static final String NOT_IMPLEMENTED_MESSAGE = "Not implemented with Solr core.";

    private DocumentReference documentReference;
    private String ratingId;
    private String globalId;
    private DocumentReference author;
    private Date date;
    private int vote;

    /**
     * Default constructor.
     *
     * @param documentReference rated document reference.
     * @param globalId global id used to retrieve this rating.
     * @param ratingId local id unique to a document reference.
     */
    public SolrRating(DocumentReference documentReference, String globalId, int ratingId)
    {
        this.documentReference = documentReference;
        this.ratingId = String.valueOf(ratingId);
        this.globalId = globalId;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    @Override
    public BaseObject getAsObject() throws RatingsException
    {
        throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS, RatingsException.ERROR_RATING_SOLR_CORE,
            NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    public String getRatingId()
    {
        return this.ratingId;
    }

    @Override
    public String getGlobalRatingId()
    {
        return this.globalId;
    }

    @Override
    public DocumentReference getAuthor()
    {
        return this.author;
    }

    @Override
    public Date getDate()
    {
        return this.date;
    }

    @Override
    public int getVote()
    {
        return this.vote;
    }

    @Override
    public Object get(String propertyName)
    {
        return null;
    }

    @Override
    public String display(String propertyName, String mode)
    {
        return null;
    }

    @Override
    public void setAuthor(DocumentReference author)
    {
        this.author = author;
    }

    @Override
    public void setDate(Date date)
    {
        this.date = date;
    }

    @Override
    public void setVote(int vote)
    {
        this.vote = vote;
    }

    @Override
    public void save() throws RatingsException
    {
        throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS, RatingsException.ERROR_RATING_SOLR_CORE,
            NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    public boolean remove() throws RatingsException
    {
        throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS, RatingsException.ERROR_RATING_SOLR_CORE,
            NOT_IMPLEMENTED_MESSAGE);
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

        SolrRating that = (SolrRating) o;

        return new EqualsBuilder()
            .append(vote, that.vote)
            .append(documentReference, that.documentReference)
            .append(ratingId, that.ratingId)
            .append(globalId, that.globalId)
            .append(author, that.author)
            .append(date, that.date)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(documentReference)
            .append(ratingId)
            .append(globalId)
            .append(author)
            .append(date)
            .append(vote)
            .toHashCode();
    }

    @Override public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("documentReference", documentReference)
            .append("ratingId", ratingId)
            .append("globalId", globalId)
            .append("author", author)
            .append("date", date)
            .append("vote", vote)
            .toString();
    }
}
