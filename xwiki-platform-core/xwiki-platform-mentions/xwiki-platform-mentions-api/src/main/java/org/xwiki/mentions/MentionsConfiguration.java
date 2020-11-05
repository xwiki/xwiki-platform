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
package org.xwiki.mentions;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Gives access to the configuration settings of the mentions.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Unstable
@Role
public interface MentionsConfiguration
{
    /**
     * The type of the mentions to local users.
     * <p>
     * This is also the default type when it is not defined.
     *
     * @since 12.10RC1
     */
    @Unstable
    String USER_MENTION_TYPE = "user";

    /**
     * 
     * @return the color for the mentions.
     */
    String getMentionsColor();

    /**
     * @return the color for the mentions to the current user.
     */
    String getSelfMentionsColor();

    /**
     * 
     * @return {@code true} if the mentions quote feature is activated.
     */
    boolean isQuoteActivated();
}
