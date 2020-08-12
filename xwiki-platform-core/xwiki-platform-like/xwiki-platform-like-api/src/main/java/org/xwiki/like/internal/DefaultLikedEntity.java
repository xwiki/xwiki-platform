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
package org.xwiki.like.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.like.LikedEntity;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.Rating;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Default implementation of {@link LikedEntity}.
 *
 * @version $Id$
 * @since 12.7RC1
 */
public class DefaultLikedEntity implements LikedEntity
{
    private EntityReference entityReference;
    private List<UserReference> likers;

    /**
     * Default constructor.
     *
     * @param entityReference the reference this LikedEntity is about.
     */
    public DefaultLikedEntity(EntityReference entityReference)
    {
        this.entityReference = entityReference;
        this.likers = new ArrayList<>();
    }

    @Override
    public EntityReference getEntityReference()
    {
        return this.entityReference;
    }

    @Override
    public List<UserReference> getLikers()
    {
        return new ArrayList<>(this.likers);
    }

    @Override
    public int getLikeNumber()
    {
        return this.likers.size();
    }

    @Override
    public boolean addLiker(UserReference userReference)
    {
        if (!this.likers.contains(userReference)) {
            return this.likers.add(userReference);
        }
        return false;
    }

    @Override
    public boolean removeLiker(UserReference userReference)
    {
        return this.likers.remove(userReference);
    }

    /**
     * Helper for adding all information retrieved as a ratings list.
     *
     * @param ratings a list of ratings concerning this entity to register.
     * @param userReferenceResolver the resolver to use to transform the author from ratings in UserReferences.
     */
    public void addAllRatings(List<Rating> ratings, UserReferenceResolver<DocumentReference> userReferenceResolver)
    {
        for (Rating rating : ratings) {
            this.likers.add(userReferenceResolver.resolve(rating.getAuthor()));
        }
    }

    @Override
    public void addAllLikers(List<UserReference> userReferences)
    {
        this.likers.addAll(userReferences);
    }

    @Override
    public int compareTo(LikedEntity likedEntity)
    {
        return Integer.compare(this.getLikeNumber(), likedEntity.getLikeNumber());
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

        DefaultLikedEntity that = (DefaultLikedEntity) o;

        return new EqualsBuilder()
            .append(entityReference, that.entityReference)
            .append(likers, that.likers)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(entityReference)
            .append(likers)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("entityReference", entityReference)
            .append("likeNumber", likers.size())
            .toString();
    }
}
