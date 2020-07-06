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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * General manager to handle {@link LikedEntity}.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@Role
@Unstable
public interface LikeManager
{
    /**
     * Create a page like and save it.
     *
     * @param source the user who performs the like.
     * @param target the page or object to like.
     * @return a dedicated LikeEntity containing all updated like information about the target.
     */
    LikedEntity saveLike(UserReference source, EntityReference target) throws LikeException;

    /**
     * Retrieve the likes performed by the given user.
     *
     * @param source the user for whom to retrieve the entity likes.
     * @return a list of like information about entities liked by this user.
     */
    List<LikedEntity> getUserLikes(UserReference source) throws LikeException;

    /**
     * Retrieve like information a specific entity.
     *
     * @param target the page or object for which to retrieve the like information.
     * @return a like information about that entity.
     */
    LikedEntity getEntityLikes(EntityReference target) throws LikeException;

    /**
     * Allow a user to unlike an entity.
     *
     * @param source the user who performs the unlike.
     * @param target the entity to unlike.
     * @return {@code true} if the entity has been properly unliked.
     */
    boolean removeLike(UserReference source, EntityReference target) throws LikeException;

    /**
     * Check if an entity is liked by an user without loading all likers.
     *
     * @param source the user who might have liked.
     * @param target the entity which might have been liked.
     * @return {@code true} if the user liked the page already.
     * @throws LikeException in case of problem for loading the result.
     */
    boolean isLiked(UserReference source, EntityReference target) throws LikeException;

    /**
     * @return a dedicated programmatic right for Like feature.
     */
    Right getLikeRight();
}
