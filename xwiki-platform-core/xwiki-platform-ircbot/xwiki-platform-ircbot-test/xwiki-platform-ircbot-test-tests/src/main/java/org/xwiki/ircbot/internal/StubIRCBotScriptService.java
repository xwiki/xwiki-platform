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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.script.service.ScriptService;

/**
 * Allow accessing the IRC Bot to simulate operations on the IRC Server (typing a message on the IRC channel,
 * getting the messages sent to the channel, etc).
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named("ircserver")
@Singleton
public class StubIRCBotScriptService implements ScriptService
{
    @Inject
    private IRCBot bot;

    private class TestChannel extends Channel
    {
        public TestChannel(PircBotX bot, String name)
        {
            super(bot, name);
        }
    }

    private class TestUser extends User
    {
        protected TestUser(PircBotX bot, String nick)
        {
            super(bot, nick);
        }
    }

    public List<String> getMessages()
    {
        return ((StubIRCBot) this.bot).getMessages();
    }

    public void typeInChannel(String message)
    {
        PircBotX pircBot = (PircBotX) this.bot;
        this.bot.getListenerManager().dispatchEvent(new MessageEvent(pircBot,
            new TestChannel(pircBot, "channel"), new TestUser(pircBot, "nick"), message));
    }
}
