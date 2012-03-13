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

import java.io.IOException;
import java.util.Set;

import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.managers.ListenerManager;
import org.xwiki.component.annotation.Role;

/**
 * Represents an IRC Bot.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface IRCBot
{
    /**
     * Sets the name of the bot, which will be used as its nick when it tries to join an IRC server. This should be
     * set before joining any servers, otherwise the default nick will be used.
     *
     * The changeNick method should be used if you wish to change your nick when you are connected to a server.
     *
     * @param botName the new name of the Bot
     */
    void setName(String botName);

    /**
     * Attempt to connect to the specified IRC server. The onConnect method is called upon success.
     *
     * @param hostname the hostname of the server to connect to
     * @throws IOException if it was not possible to connect to the server
     * @throws IrcException if the server would not let us join it
     */
    void connect(String hostname) throws IOException, IrcException;

    /**
     * Joins a channel.
     *
     * @param channel the name of the channel to join (eg "#cs")
     */
    void joinChannel(String channel);

    /**
     * This method disconnects from the server cleanly by calling the quitServer() method. Providing the Bot was
     * connected to an IRC server, DisconnectEvent will be dispatched as soon as the disconnection is made by the
     * server.
     */
    void disconnect();

    /**
     * Identify the bot with NickServ, supplying the appropriate password.
     * Some IRC Networks (such as freenode) require users to <i>register</i> and
     * <i>identify</i> with NickServ before they are able to send private messages
     * to other users, thus reducing the amount of spam.  If you are using
     * an IRC network where this kind of policy is enforced, you will need
     * to make your bot <i>identify</i> itself to NickServ before you can send
     * private messages. Assuming you have already registered your bot's
     * nick with NickServ, this method can be used to <i>identify</i> with
     * the supplied password. It usually makes sense to identify with NickServ
     * immediately after connecting to a server.
     * <p>
     * This method issues a raw NICKSERV command to the server, and is therefore
     * safer than the alternative approach of sending a private message to
     * NickServ. The latter approach is considered dangerous, as it may cause
     * you to inadvertently transmit your password to an untrusted party if you
     * connect to a network which does not run a NickServ service and where the
     * untrusted party has assumed the nick "NickServ".  However, if your IRC
     * network is only compatible with the private message approach, you may
     * typically identify like so:
     * <pre>sendMessage("NickServ", "identify PASSWORD");</pre>
     * <p>
     * Note that this method will add a temporary listener for ConnectEvent if
     * the bot is not logged in yet. If the bot is logged in the command is sent
     * immediately to the server
     *
     * @param password The password which will be used to identify with NickServ.
     */
    void identify(String password);

    /**
     * Sends a message to a channel or a private message to a user.  These messages are added to the outgoing message
     * queue and sent at the earliest possible opportunity.
     * <p>
     * Some examples: -
     *  <pre>    // Send the message "Hello!" to the channel #cs.
     *    sendMessage("#cs", "Hello!");
     *
     *    // Send a private message to Paul that says "Hi".
     *    sendMessage("Paul", "Hi");</pre>
     *
     * You may optionally apply colours, boldness, underlining, etc to the message by using the <code>Colors</code>
     * class.
     *
     * @param target The name of the channel or user nick to send to.
     * @param message The message to send.
     */
    void sendMessage(String target, String message);

    /**
     * Gets all the name's of all the channels that we are connected to.
     *
     * @return An <i>Unmodifiable</i> set of Channel names
     */
    Set<String> getChannelsNames();

    /**
     * Returns whether or not the Bot is currently connected to a server.
     * The result of this method should only act as a rough guide, as the result may not be valid by the time you act
     * upon it.
     *
     * @return True if and only if the Bot is currently connected to a server.
     */
    boolean isConnected();

    /**
      * Returns the current ListenerManager in use by this bot.
      * @return the current ListenerManager
      */
    ListenerManager<? extends PircBotX> getListenerManager();
}
