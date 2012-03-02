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

import org.xwiki.component.annotation.ComponentRole;

/**
 * IRC Bot Listeners allows to perform some actions in answer to some IRC channel event. For example you may want to
 * code a Bot Listener that would do something when a given word is typed on the IRC channel.
 *
 * @version $Id$
 * @since 4.0M1
 */
@ComponentRole
public interface IRCBotListener extends IRCBotMessageHandler, Comparable<IRCBotListener>
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

    /**
     * The priority of execution relative to the other Bot Listeners. The lowest values have the highest priorities
     * and execute first. For example a Bot Listener with a priority of 100 will execute before one with a priority of
     * 500.
     *
     * @return the execution priority
     */
    int getPriority();

    /**
     * Give the opportunity to the IRC Bot Listener writer to do something when the listener is registered and
     * thus activated. For example one could get some configuration parameter from some XWiki Class and store them
     * in the context so that they're available from other events. Another example would be to use some Groovy script
     * to register some class (such as an Event Listener); an example of this would be to write a Bot Listener that
     * listens to document change events and send them to the IRC channel.
     */
    void onRegistration();

    /**
     * Give the opportunity to the IRC Bot Listener writer to do something when the listener is unregistered and
     * thus deactivated.
     */
    void onUnregistration();
}
