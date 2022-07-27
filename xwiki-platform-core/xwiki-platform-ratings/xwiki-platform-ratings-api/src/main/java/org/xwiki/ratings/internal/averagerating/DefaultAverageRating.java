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
package org.xwiki.ratings.internal.averagerating;

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.AverageRating;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Default implementation of {@link AverageRating}.
 * This implementation provides a builder API.
 *
 * @version $Id$
 * @since 12.9RC1
 */
public class DefaultAverageRating implements AverageRating
{
    private String identifier;
    private String managerId;
    private EntityReference reference;
    private float averageVote;
    private int totalVote;
    private int scale;
    private Date updatedAt;

    /**
     * Default constructor with identifier.
     *
     * @param identifier unique identifier of the average.
     */
    public DefaultAverageRating(String identifier)
    {
        this.identifier = identifier;
        this.updatedAt = new Date();
    }

    /**
     * Constructor that allows cloning an existing average rating.
     *
     * @param averageRating the already existing object to clone.
     */
    public DefaultAverageRating(AverageRating averageRating)
    {
        this.identifier = averageRating.getId();
        this.managerId = averageRating.getManagerId();
        this.reference = averageRating.getReference();
        this.averageVote = averageRating.getAverageVote();
        this.totalVote = averageRating.getNbVotes();
        this.scale = averageRating.getScaleUpperBound();
        this.updatedAt = averageRating.getUpdatedAt();
    }

    @Override
    public AverageRating updateRating(int oldRating, int newRating)
    {
        float newTotal = (this.averageVote * this.totalVote) - oldRating + newRating;
        this.averageVote = newTotal / this.totalVote;
        this.updatedAt = new Date();
        return this;
    }

    @Override
    public AverageRating removeRating(int rating)
    {
        float newTotal = (this.averageVote * this.totalVote) - rating;
        this.totalVote--;
        this.averageVote = (this.totalVote > 0) ? newTotal / this.totalVote : 0;
        this.updatedAt = new Date();
        return this;
    }

    @Override
    public AverageRating addRating(int rating)
    {
        float newTotal = (this.averageVote * this.totalVote) + rating;
        this.totalVote++;
        this.averageVote = newTotal / this.totalVote;
        this.updatedAt = new Date();
        return this;
    }

    /**
     * @param managerId the manager identifier to set.
     * @return the current instance.
     */
    public DefaultAverageRating setManagerId(String managerId)
    {
        this.managerId = managerId;
        return this;
    }

    /**
     * @param reference the reference of the element being rated.
     * @return the current instance.
     */
    public DefaultAverageRating setReference(EntityReference reference)
    {
        this.reference = reference;
        return this;
    }

    /**
     * @param averageVote the average value.
     * @return the current instance.
     */
    public DefaultAverageRating setAverageVote(float averageVote)
    {
        this.averageVote = averageVote;
        return this;
    }

    /**
     * @param totalVote the number of rated elements.
     * @return the current instance.
     */
    public DefaultAverageRating setTotalVote(int totalVote)
    {
        this.totalVote = totalVote;
        return this;
    }

    /**
     * @param scale the scale used for rating elements.
     * @return the current instance.
     */
    public DefaultAverageRating setScaleUpperBound(int scale)
    {
        this.scale = scale;
        return this;
    }

    /**
     * @param updatedAt the date of last update.
     * @return the current instance.
     */
    public DefaultAverageRating setUpdatedAt(Date updatedAt)
    {
        this.updatedAt = updatedAt;
        return this;
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
    public float getAverageVote()
    {
        return this.averageVote;
    }

    @Override
    public int getNbVotes()
    {
        return this.totalVote;
    }

    @Override
    public int getScaleUpperBound()
    {
        return this.scale;
    }

    @Override
    public Date getUpdatedAt()
    {
        return updatedAt;
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

        DefaultAverageRating that = (DefaultAverageRating) o;

        return new EqualsBuilder()
            .append(averageVote, that.averageVote)
            .append(totalVote, that.totalVote)
            .append(scale, that.scale)
            .append(identifier, that.identifier)
            .append(managerId, that.managerId)
            .append(reference, that.reference)
            .append(updatedAt, that.updatedAt)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 67)
            .append(identifier)
            .append(managerId)
            .append(reference)
            .append(averageVote)
            .append(totalVote)
            .append(scale)
            .append(updatedAt)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("identifier", identifier)
            .append("managerId", managerId)
            .append("reference", reference)
            .append("averageVote", averageVote)
            .append("totalVote", totalVote)
            .append("scale", scale)
            .append("updatedAt", updatedAt)
            .toString();
    }
}
