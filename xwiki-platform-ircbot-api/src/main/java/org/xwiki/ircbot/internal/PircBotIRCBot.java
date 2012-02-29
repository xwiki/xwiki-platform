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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.IRCBotListener;

@Component(roles = {IRCBot.class})
@Singleton
public class PircBotIRCBot implements IRCBot
{
    private static final String CONTEXT_IRCBOT_KEY = "ircbot";

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private ExecutionContextManager executionContextManager;

    /**
     * Look up IRC Bot Listener both in the wiki component manager and in the root manager since wiki IRC bot
     * listeners are registered only for the current wiki.
     */
    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    private String channel;

    /**
     * True if the IRC bot should be stopped.
     */
    private boolean shouldStop;

    protected PircBotInterface pircBot = new ExtendedPircBot(this);

    @Override
    public void connect(String botName, String hostname) throws IRCBotException
    {
        this.pircBot.setBotName(botName);
        try {
            this.pircBot.connect(hostname);
        } catch (Exception e) {
            throw new IRCBotException(String.format("Failed to connect to [%s]", hostname), e);
        }
    }

    @Override
    public void disconnect()
    {
        this.shouldStop = true;
        this.pircBot.disconnect();
    }

    @Override
    public void identify(String password)
    {
        this.pircBot.identify(password);
    }

    @Override
    public void joinChannel(String channel)
    {
        this.channel = channel;
        this.pircBot.joinChannel(channel);
    }

    @Override
    public boolean isConnected()
    {
        return this.pircBot.isConnected();
    }

    @Override
    public void sendMessage(String target, String message)
    {
        for (String line : splitMessage(message)) {
            this.pircBot.sendMessage(target, line);
        }
    }

    @Override
    public void sendMessage(String message) throws IRCBotException
    {
        if (this.channel != null) {
            sendMessage(this.channel, message);
        } else {
            throw new IRCBotException("Cannot send message to undefined channel. Make sure you've connected to a "
                + "channel first before using this API.");
        }
    }

    @Override
    public String getDescription()
    {
        return "Delegates events to IRC Bot Listeners";
    }

    @Override
    public String getName()
    {
        return "Internal Delegator";
    }

    @Override
    public void onConnect()
    {
        initExecutionContext();
        for (IRCBotListener listener : getIRCBotListeners()) {
            listener.onConnect();
        }
    }

    @Override
    public void onDisconnect()
    {
        initExecutionContext();
        for (IRCBotListener listener : getIRCBotListeners()) {
            listener.onDisconnect();
        }

        if (!this.shouldStop) {
            this.logger.debug("IRC Bot has been disconnected");
            while (!isConnected()) {
                try {
                    this.pircBot.reconnect();
                }
                catch (Exception e) {
                    // Cannot reconnect, wait for some time before trying to reconnect again
                    try {
                        Thread.sleep(1000L*30);
                    } catch (InterruptedException ie) {
                        // Failed to sleep, just ignore
                    }
                }
            }
        } else {
            // Clean up the Execution Context Thread Local but only if we're inside an Execution Context
            // set up for the IRC Bot.
            // Note that if PircBot destroys its threads internally we have no way of doing cleanup for
            // the Execution Context for that thread and that could potentially cause some memory leak
            // since the thread local Execution Context would not have been cleaned.
            ExecutionContext context = this.execution.getContext();
            if (context != null && context.getProperty(CONTEXT_IRCBOT_KEY) != null) {
                this.execution.removeContext();
            }
        }
    }

    @Override
    public void onJoin(String channel, String sender, String login, String hostname)
    {
        initExecutionContext();
        for (IRCBotListener listener : getIRCBotListeners()) {
            listener.onJoin(channel, sender, login, hostname);
        }
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message)
    {
        initExecutionContext();
        for (IRCBotListener listener : getIRCBotListeners()) {
            listener.onMessage(channel, sender, login, hostname, message);
        }
    }

    @Override
    public void onNickChange(String oldNick, String login, String hostname, String newNick)
    {
        initExecutionContext();
        for (IRCBotListener listener : getIRCBotListeners()) {
            listener.onNickChange(oldNick, login, hostname, newNick);
        }

    }

    @Override
    public void onPart(String channel, String sender, String login, String hostname)
    {
        initExecutionContext();
        for (IRCBotListener listener : getIRCBotListeners()) {
            listener.onPart(channel, sender, login, hostname);
        }
    }

    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message)
    {
        initExecutionContext();
        for (IRCBotListener listener : getIRCBotListeners()) {
            listener.onPrivateMessage(sender, login, hostname, message);
        }
    }

    @Override
    public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason)
    {
        initExecutionContext();
        for (IRCBotListener listener : getIRCBotListeners()) {
            listener.onQuit(sourceNick, sourceLogin, sourceHostname, reason);
        }
    }

    /**
     * This method is called within Pircbot's input thread and is used to ensure that there's
     * an initialized XWiki Execution Context for this thread.
     *
     * Note that we have to check all the time since Pircbot doesn't offer a hook for us to
     * configure the Execution Context when it creates its threads.
     */
    private void initExecutionContext()
    {
        ExecutionContext context = this.execution.getContext();

        if (context == null) {
            context = new ExecutionContext();
            try {
                this.executionContextManager.initialize(context);
            } catch (ExecutionContextException e) {
                throw new RuntimeException("Failed to initialize IRC Bot's execution context", e);
            }

            // Set marker to signify we're in an IRC Bot Context
            context.setProperty(CONTEXT_IRCBOT_KEY, true);
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

    /**
     * Split a message on new lines (and remove empty lines).
     *
     * @param message the message to split
     * @return the split message as a List with each entry being a line
     */
    private List<String> splitMessage(String message)
    {
        return Arrays.asList(message.split("[\\r\\n]+"));
    }
}
