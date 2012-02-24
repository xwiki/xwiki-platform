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
package org.xwiki.ircbot.wiki;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * Constants identifying various properties used for defining wiki IRC Bot listeners.
 *
 * @version $Id$
 * @since 4.0M1
 */
public interface WikiIRCBotConstants
{
    String SPACE = "IRC";

    /**
     * Constant for representing IRC.IRCBotListenerClass xwiki class.
     */
    EntityReference WIKI_BOT_LISTENER_CLASS = new EntityReference("IRCBotListenerClass", EntityType.DOCUMENT,
        new EntityReference(SPACE, EntityType.SPACE));

    /**
     * Constant for representing a listener description property.
     */
    String DESCRIPTION_PROPERTY = "description";

    /**
     * Whether a Bot Listener or the Bot itself are marked inactive. If so they're not started.
     */
    String INACTIVE_PROPERTY = "inactive";

    /**
     * Constant for representing IRC.IRCBotListenerEventClass xwiki class. There's one such object per event (i.e.
     * per onXXX() method handled).
     */
    EntityReference WIKI_BOT_LISTENER_EVENT_CLASS = new EntityReference("IRCBotListenerEventClass", EntityType.DOCUMENT,
        new EntityReference(SPACE, EntityType.SPACE));

    /**
     * Constant for representing a listener event property.
     */
    String EVENT_NAME_PROPERTY = "event";

    /**
     * Constant for representing a listener event script name property.
     */
    String EVENT_SCRIPT_PROPERTY = "script";

    String ON_CONNECT_EVENT_NAME = "onConnect";
    String ON_DISCONNECT_EVENT_NAME = "onDisconnect";
    String ON_JOIN_EVENT_NAME = "onJoin";
    String ON_MESSAGE_EVENT_NAME = "onMessage";
    String ON_NICK_CHANGE_EVENT_NAME = "onNickChange";
    String ON_PART_EVENT_NAME = "onPart";
    String ON_PRIVATE_MESSAGE_EVENT_NAME = "onPrivateMessage";
    String ON_QUIT_EVENT_NAME = "onQuit";

    /**
     * Constant for representing IRC.IRCBot xwiki class which is the Bot's configuration class.
     */
    EntityReference WIKI_BOT_CONFIGURATION_CLASS = new EntityReference("IRCBot", EntityType.DOCUMENT,
        new EntityReference(SPACE, EntityType.SPACE));

    String CONFIGURATION_PAGE = "IRCBotConfiguration";

    /**
     * Bot's name.
     */
    String BOTNAME_PROPERTY = "botname";

    /**
     * Channel to connect to.
     */
    String CHANNEL_PROPERTY = "channel";

    String SERVER_PROPERTY = "server";

    String PASSWORD_PROPERTY = "password";
}
