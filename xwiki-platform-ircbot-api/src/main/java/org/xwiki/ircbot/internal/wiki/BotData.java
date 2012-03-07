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
package org.xwiki.ircbot.internal.wiki;

/**
 * Represents Bot Data (channel, server, password, bot name, etc).
 *
 * @version $Id$
 * @since 4.0M1
 */
public class BotData
{
    private boolean isActive;

    private String channel;

    private String server;

    private String botName;

    private String password;

    public BotData(String botName, String server, String password, String channel, boolean isActive)
    {
        this.botName = botName;
        this.server = server;
        this.password = password;
        this.channel = channel;
        this.isActive = isActive;
    }

    public String getBotName()
    {
        return this.botName;
    }

    public String getServer()
    {
        return this.server;
    }

    public String getPassword()
    {
        return this.password;
    }

    public String getChannel()
    {
        return this.channel;
    }

    public boolean isActive()
    {
        return this.isActive;
    }
}
