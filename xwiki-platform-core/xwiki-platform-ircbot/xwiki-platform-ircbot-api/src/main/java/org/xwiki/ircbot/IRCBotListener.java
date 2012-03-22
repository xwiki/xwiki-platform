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
package org.xwiki.ircbot;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Listener;
import org.xwiki.component.annotation.Role;

/**
 * IRC Bot Listeners allows to perform some actions in answer to some IRC channel event. For example you may want to
 * code a Bot Listener that would do something when a given word is typed on the IRC channel.
 *
 * @param <T> the reference to the PircBotX instance
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface IRCBotListener<T extends PircBotX> extends Listener<T>
{
    /**
     * @return the human-readable name of the Bot Listener (eg "Displays the list of command available when you type
     *         '!help'")
     */
    String getName();

    /**
     * @return the Bot listener's description
     */
    String getDescription();
}
