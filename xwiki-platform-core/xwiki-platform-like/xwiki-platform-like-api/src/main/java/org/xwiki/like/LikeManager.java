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
import org.xwiki.user.UserReference;

/**
 * General manager to handle likes.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@Role
public interface LikeManager
{
    /**
     * Create a page like and save it.
     *
     * @param source the user who performs the like.
     * @param target the page or object to like.
     * @return the new number of likes.
     * @throws LikeException in case of problem when saving the like.
     */
    long saveLike(UserReference source, EntityReference target) throws LikeException;

    /**
     * Retrieve the likes performed by the given user.
     *
     * @param source the user for whom to retrieve the entity likes.
     * @param offset the offset used for pagination.
     * @param limit the limit number of results to retrieve for pagination.
     * @return a list of references liked by this user.
     * @throws LikeException in case of problem when getting the like.
     */
    List<EntityReference> getUserLikes(UserReference source, int offset, int limit) throws LikeException;

    /**
     * Retrieve the total number of likes performed by a user.
     *
     * @param source the user who performs the likes to count.
     * @return the total number of likes performed.
     * @throws LikeException in case of problem when getting the information.
     * @since 12.9RC1
     */
    long countUserLikes(UserReference source) throws LikeException;

    /**
     * Retrieve like information a specific entity.
     *
     * @param target the page or object for which to retrieve the like information.
     * @return the number of likes for that entity.
     * @throws LikeException in case of problem when getting the like.
     */
    long getEntityLikes(EntityReference target) throws LikeException;

    /**
     * Allow a user to unlike an entity.
     *
     * @param source the user who performs the unlike.
     * @param target the entity to unlike.
     * @return {@code true} if the entity has been properly unliked.
     * @throws LikeException in case of problem when removing the like.
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
     * Retrieve the users who liked the given reference.
     *
     * @param target the page that has been liked.
     * @param offset the offset used for pagination.
     * @param limit the limit used for pagination.
     * @return a list of user references of users who liked this page.
     * @throws LikeException in case of problem for performing the query.
     * @since 12.9RC1
     */
    List<UserReference> getLikers(EntityReference target, int offset, int limit) throws LikeException;

    /**
     * @return a dedicated programmatic right for Like feature.
     */
    Right getLikeRight();

    /**
     * Clear like data related to the given reference from cache.
     * @param target the reference for which data should be cleared.
     * @since 12.9RC1
     */
    void clearCache(EntityReference target);

    /**
     * Clear all data from caches.
     * @since 12.9RC1
     */
    void clearCache();
}
