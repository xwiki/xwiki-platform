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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Configuration API for Like.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@Unstable
@Role
public interface LikeConfiguration
{
    /**
     * Define the Like button possible behaviours.
     */
    enum UIClickBehaviour
    {
        /**
         * List of likers are never displayed, like actions are immediate, unlike action needs a confirmation.
         */
        NEVER_DISPLAY_LIKERS,

        /**
         * List of likers are always displayed when clicking on the Like button.
         * Like and Unlike actions are available in the modal displaying likers.
         */
        ALWAYS_DISPLAY_LIKERS,

        /**
         * When the page is not liked, the click on the button triggers a like immediately.
         * Then when the page is liked, a click on the button displays the likers in the modal which also allow
         * to unlike.
         */
        LIKE_FIRST_AND_DISPLAY_LIKERS
    }

    /**
     * @return {@code true} if the button showing Like information should be displayed even when users don't have rights
     *          to interact with it.
     */
    boolean alwaysDisplayButton();

    /**
     * @return the maximum number of like information to keep in cache.
     */
    int getLikeCacheCapacity();

    /**
     * @return the behaviour to realize when the Like button is clicked.
     */
    UIClickBehaviour getUIClickBehaviour();
}
