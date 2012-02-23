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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.ircbot.AbstractIRCBotListener;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotListener;

@Component
@Named("help")
@Singleton
public class HelpIRCBotListener extends AbstractIRCBotListener
{
    private static final String COMMAND = "!help";

    @Inject
    private ComponentManager componentManager;

    @Inject
    private IRCBot bot;

    @Override
    public String getDescription()
    {
        return String.format("%s: List all commands available", COMMAND);
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message)
    {
        super.onMessage(channel, sender, login, hostname, message);

        if (message.startsWith(COMMAND)) {
            for (IRCBotListener listener : getIRCBotListeners()) {
                this.bot.sendMessage(channel, listener.getDescription());
            }
        }
    }

    private List<IRCBotListener> getIRCBotListeners()
    {
        List<IRCBotListener> result;
        try {
            result = this.componentManager.lookupList(IRCBotListener.class);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to look up IRC Bot Listeners", e);
        }
        return result;
    }

}
