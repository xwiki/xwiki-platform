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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.Rating;
import org.xwiki.text.XWikiToStringBuilder;
import org.xwiki.user.UserReference;

/**
 * Default implementation of {@link Rating}.
 * This class provides a builder API for setting the values.
 *
 *
 * @version $Id$
 * @since 12.9RC1
 */
public class DefaultRating implements Rating
{
    private String identifier;
    private String managerId;
    private EntityReference reference;
    private UserReference user;
    private Date createdAt;
    private Date updatedAt;
    private int vote;
    private int scale;

    /**
     * Default constructor.
     *
     * @param identifier the unique identifier.
     */
    public DefaultRating(String identifier)
    {
        this.identifier = identifier;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * Constructor to clone an existing instance of rating.
     *
     * @param rating the instance to clone.
     */
    public DefaultRating(Rating rating)
    {
        this.identifier = rating.getId();
        this.managerId = rating.getManagerId();
        this.reference = rating.getReference();
        this.vote = rating.getVote();
        this.updatedAt = rating.getUpdatedAt();
        this.createdAt = rating.getCreatedAt();
        this.scale = rating.getScaleUpperBound();
        this.user = rating.getAuthor();
    }

    @Override
    public String getId()
    {
        return this.identifier;
    }

    @Override
    public String getManagerId()
    {
        return this.managerId;
    }

    @Override
    public EntityReference getReference()
    {
        return this.reference;
    }

    @Override
    public UserReference getAuthor()
    {
        return this.user;
    }

    @Override
    public Date getCreatedAt()
    {
        return this.createdAt;
    }

    @Override
    public Date getUpdatedAt()
    {
        return this.updatedAt;
    }

    /**
     * @param date the date of latest update.
     * @return the current instance.
     */
    public DefaultRating setUpdatedAt(Date date)
    {
        this.updatedAt = date;
        return this;
    }

    @Override
    public int getVote()
    {
        return this.vote;
    }

    /**
     * @param id the identifier of the ranking.
     * @return the current instance.
     */
    public DefaultRating setId(String id)
    {
        this.identifier = id;
        return this;
    }

    /**
     * @param vote the vote value to set.
     * @return the current instance.
     */
    public DefaultRating setVote(int vote)
    {
        this.vote = vote;
        return this;
    }

    /**
     * @param managerId the identifier of the manager.
     * @return the current instance.
     */
    public DefaultRating setManagerId(String managerId)
    {
        this.managerId = managerId;
        return this;
    }

    /**
     * @param reference the element that has been ranked.
     * @return the current instance.
     */
    public DefaultRating setReference(EntityReference reference)
    {
        this.reference = reference;
        return this;
    }

    /**
     * @param user the user who performs the vote.
     * @return the current instance.
     */
    public DefaultRating setAuthor(UserReference user)
    {
        this.user = user;
        return this;
    }

    /**
     * @param createdAt the date of creation.
     * @return the current instance.
     */
    public DefaultRating setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * @param scale the scale of the ranks.
     * @return the current instance.
     */
    public DefaultRating setScaleUpperBound(int scale)
    {
        this.scale = scale;
        return this;
    }

    @Override
    public int getScaleUpperBound()
    {
        return this.scale;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("identifier", identifier)
            .append("managerId", managerId)
            .append("rankedElement", reference)
            .append("voter", user)
            .append("createdAt", createdAt)
            .append("updatedAt", updatedAt)
            .append("rank", vote)
            .append("scale", scale)
            .toString();
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

        DefaultRating that = (DefaultRating) o;

        return new EqualsBuilder()
            .append(vote, that.vote)
            .append(scale, that.scale)
            .append(identifier, that.identifier)
            .append(managerId, that.managerId)
            .append(reference, that.reference)
            .append(user, that.user)
            .append(createdAt, that.createdAt)
            .append(updatedAt, that.updatedAt)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(13, 97)
            .append(identifier)
            .append(managerId)
            .append(reference)
            .append(user)
            .append(createdAt)
            .append(updatedAt)
            .append(vote)
            .append(scale)
            .toHashCode();
    }
}
