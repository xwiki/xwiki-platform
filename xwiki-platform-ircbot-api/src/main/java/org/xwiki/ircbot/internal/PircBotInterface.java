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
package org.xwiki.ircbot.internal;

import java.io.IOException;

import org.jibble.pircbot.IrcException;

/**
 * Wraps the PIRCBot API since PIRCBot doesn't offer an interface and we want to be able to provide different
 * implementations (for the tests for example).
 *
 * @version $Id$
 * @since 4.0M1
 */
public interface PircBotInterface
{
    /**
     * Attempt to connect to the specified IRC server.
     *
     * @param hostname the hostname of the server to connect to
     * @throws IOException if it was not possible to connect to the server
     * @throws IrcException if the server would not let us join it or if our nick is already in use on the server
     */
    void connect(String hostname) throws IOException, IrcException;

    /**
     * Reconnects to the IRC server that we were previously connected to. If necessary, the appropriate port number
     * and password will be used. This method will throw an IrcException if we have never connected to an IRC server
     * previously.
     *
     * @throws IOException if it was not possible to connect to the server
     * @throws IrcException if the server would not let us join it or if our nick is already in use on the server
     */
    void reconnect() throws IOException, IrcException;

    /**
     * Joins a channel.
     *
     * @param channel the name of the channel to join (eg "#cs")
     */
    void joinChannel(String channel);

    /**
     * This method disconnects from the server cleanly by calling the quitServer() method. Providing the PircBot was
     * connected to an IRC server, the onDisconnect() will be called as soon as the disconnection is made by the
     * server.
     */
    void disconnect();

    /**
     * Identify the bot with NickServ, supplying the appropriate password. Some IRC Networks (such as freenode)
     * require users to register and identify with NickServ before they are able to send private messages to other
     * users, thus reducing the amount of spam. If you are using an IRC network where this kind of policy is enforced,
     * you will need to make your bot identify itself to NickServ before you can send private messages. Assuming you
     * have already registered your bot's nick with NickServ, this method can be used to identify with the supplied
     * password. It usually makes sense to identify with NickServ immediately after connecting to a server.
     * <p/>
     * This method issues a raw NICKSERV command to the server, and is therefore safer than the alternative approach
     * of sending a private message to NickServ. The latter approach is considered dangerous, as it may cause you to
     * inadvertently transmit your password to an untrusted party if you connect to a network which does not run a
     * NickServ service and where the untrusted party has assumed the nick "NickServ". However, if your IRC network is
     * only compatible with the private message approach, you may typically identify like so:
     * <pre><code>
     * sendMessage("NickServ", "identify PASSWORD");
     * </code></pre>
     *
     * @param password the password which will be used to identify with NickServ.
     */
    void identify(String password);

    /**
     * Sends a message to a channel or a private message to a user. These messages are added to the outgoing message
     * queue and sent at the earliest possible opportunity.
     *
     * Some examples:
     * <pre><code>
     * // Send the message "Hello!" to the channel #cs.
     * sendMessage("#cs", "Hello!");
     *
     * // Send a private message to Paul that says "Hi".
     * sendMessage("Paul", "Hi");
     * </code></pre>
     *
     * You may optionally apply colours, boldness, underlining, etc to the message by using the {@code Colors} class.
     *
     * @param target the name of the channel or user nick to send to
     * @param message the action to send
     */
    void sendMessage(String target, String message);

    /**
     * Returns whether or not the PircBot is currently connected to a server. The result of this method should only
     * act as a rough guide, as the result may not be valid by the time you act upon it.
     *
     * @return true if and only if the Bot is currently connected to a server.
     */
    boolean isConnected();

    /**
     * Sets the name of the bot, which will be used as its nick when it tries to join an IRC server.
     * This should be set before joining any servers, otherwise the default nick will be used.
     *
     * @param botName the new name of the Bot
     */
    void setBotName(String botName);

    /**
     * Returns an array of all channels that we are in. Note that if you call this method immediately after joining a
     * new channel, the new channel may not appear in this array as it is not possible to tell if the join was
     * successful until a response is received from the IRC server.
     *
     * @return a String array containing the names of all channels that we are in
     */
    String[] getConnectedChannels();
}
