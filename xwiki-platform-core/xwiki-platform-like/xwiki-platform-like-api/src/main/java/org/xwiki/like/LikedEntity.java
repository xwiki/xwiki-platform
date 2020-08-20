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
package org.xwiki.like;

import java.util.List;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Represents a like counter on a specific entity (e.g. a page or a comment).
 *
 * @version $Id$
 * @since 12.7RC1
 */
@Unstable
public interface LikedEntity extends Comparable<LikedEntity>
{
    /**
     * @return a reference to the entity that is liked.
     */
    EntityReference getEntityReference();

    /**
     * Retrieve and return the whole list of people who like the entity.
     *
     * @return the list of users who liked this entity in the order of their likes.
     */
    List<UserReference> getLikers();

    /**
     * @return the number of likes.
     */
    int getLikeNumber();

    /**
     * Add a new liker to this liked entity.
     * Note that this method does not persist the change, use
     * {@link LikeManager#saveLike(UserReference, EntityReference)} to save a new like properly.
     *
     * @param userReference the new liker.
     * @return {@code true} if the liker has been added or {@code false} it he already liked it.
     */
    boolean addLiker(UserReference userReference);

    /**
     * Add all given user references as likers.
     *
     * @param userReferences the likers to add.
     */
    void addAllLikers(List<UserReference> userReferences);

    /**
     * Remove a liker to this liked entity.
     * Note that this method does not persist the change.
     * Use {@link LikeManager#removeLike(UserReference, EntityReference)} to remove a like properly.
     *
     * @param userReference the liker who unlikes.
     * @return {@code true} if the liker has been removed or {@code false} if he hadn't liked before.
     */
    boolean removeLiker(UserReference userReference);
}
