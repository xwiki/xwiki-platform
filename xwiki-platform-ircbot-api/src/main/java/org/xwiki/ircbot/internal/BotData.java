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

/**
 * Represents Bot Data (channel, server, password, bot name, etc).
 *
 * @version $Id$
 * @since 4.0M2
 */
public class BotData
{
    /**
     * @see #isActive()
     */
    private boolean isActive;

    /**
     * @see #getChannel()
     */
    private String channel;

    /**
     * @see #getServer()
     */
    private String server;

    /**
     * @see #getName
     */
    private String name;

    /**
     * @see #getPassword()
     */
    private String password;

    /**
     * @param name see {@link #getName()}
     * @param server see {@link #getServer()}
     * @param password see {@link #getPassword()}
     * @param channel see {@link #getChannel()}
     * @param isActive see {@link #isActive()}
     */
    public BotData(String name, String server, String password, String channel, boolean isActive)
    {
        this.name = name;
        this.server = server;
        this.password = password;
        this.channel = channel;
        this.isActive = isActive;
    }

    /**
     * @return the Bot's name in the IRC Channel
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return the IRC Server to connect to
     */
    public String getServer()
    {
        return this.server;
    }

    /**
     * @return the optional password to identify against the IRC Server
     */
    public String getPassword()
    {
        return this.password;
    }

    /**
     * @return the name of the channel the Bot should connect to
     */
    public String getChannel()
    {
        return this.channel;
    }

    /**
     * @return true if the Bot is active or false otherwise. This is useful for example for the IRC Bot Scheduler so
     *         that it'll (re)start the Bot automatically if not started but only if the Bot is active.
     */
    public boolean isActive()
    {
        return this.isActive;
    }
}
