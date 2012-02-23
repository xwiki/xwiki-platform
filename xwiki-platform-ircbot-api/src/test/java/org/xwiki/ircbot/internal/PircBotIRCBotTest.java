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
import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link PircBotIRCBot}.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class PircBotIRCBotTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private PircBotIRCBot bot;

    private Execution execution;

    private ComponentManager componentManager;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.execution = getComponentManager().lookup(Execution.class);
        this.componentManager = getComponentManager().lookup(ComponentManager.class);
    }

    @Test
    public void reconnect() throws Exception
    {
        final PircBotInterface pircBot = getMockery().mock(PircBotInterface.class);

        getMockery().checking(new Expectations()
        {{
            // Return a non null ExecutionContext to simulate a properly set up context
            oneOf(execution).getContext();
                will(returnValue(new ExecutionContext()));
            // For this test we don't need to have IRC Bot Listeners
            oneOf(componentManager).lookupList(IRCBotListener.class);
                will(returnValue(Collections.emptyList()));
            oneOf(pircBot).isConnected();
                will(returnValue(false));
            // The test is here: we verify that we reconnect when disconnected
            oneOf(pircBot).reconnect();
            oneOf(pircBot).isConnected();
                will(returnValue(true));
        }});

        ReflectionUtils.setFieldValue(this.bot, "pircBot", pircBot);

        bot.onDisconnect();
    }

    @Test
    public void sendMessageOnOneLine() throws Exception
    {
        final PircBotInterface pircBot = getMockery().mock(PircBotInterface.class);
        ReflectionUtils.setFieldValue(this.bot, "pircBot", pircBot);

        getMockery().checking(new Expectations()
        {{
            // The test is here
            oneOf(pircBot).sendMessage("target", "line");
        }});

        this.bot.sendMessage("target", "line");
    }

    @Test
    public void sendMessageOnMultipleLines() throws Exception
    {
        final PircBotInterface pircBot = getMockery().mock(PircBotInterface.class);
        ReflectionUtils.setFieldValue(this.bot, "pircBot", pircBot);

        getMockery().checking(new Expectations()
        {{
            // The test is here
            oneOf(pircBot).sendMessage("target", "line1");
            oneOf(pircBot).sendMessage("target", "line2");
        }});

        this.bot.sendMessage("target", "line1\nline2");
    }

    @Test
    public void sendMessageOnDefaultChannelFailsWhenNotConnected() throws Exception
    {
        try {
            this.bot.sendMessage("line");
            Assert.fail("Should have thrown an exception");
        } catch (IRCBotException expected) {
            Assert.assertEquals("Cannot send message to undefined channel. Make sure you've connected to a channel "
                + "first before using this API.", expected.getMessage());
        }
    }

    @Test
    public void sendMessageOnDefaultChannel() throws Exception
    {
        final PircBotInterface pircBot = getMockery().mock(PircBotInterface.class);
        ReflectionUtils.setFieldValue(this.bot, "pircBot", pircBot);

        getMockery().checking(new Expectations()
        {{
            oneOf(pircBot).joinChannel("channel");
            // The test is here!
            oneOf(pircBot).sendMessage("channel", "line");
        }});

        this.bot.joinChannel("channel");
        this.bot.sendMessage("line");
    }

    @Test
    public void onMessage() throws Exception
    {
        final PircBotInterface pircBot = getMockery().mock(PircBotInterface.class);

        final IRCBotListener listener = getMockery().mock(IRCBotListener.class);

        getMockery().checking(new Expectations()
        {{
            // Return a non null ExecutionContext to simulate a properly set up context
            oneOf(execution).getContext();
                will(returnValue(new ExecutionContext()));
            // Simulate one IRC Bot Listener
            oneOf(componentManager).lookupList(IRCBotListener.class);
                will(returnValue(Arrays.asList(listener)));
            // The test is here: we verify that the Bot listener's onMessage method is called.
            oneOf(listener).onMessage("channel", "sender", "login", "hostname", "message");
        }});

        ReflectionUtils.setFieldValue(this.bot, "pircBot", pircBot);

        this.bot.onMessage("channel", "sender", "login", "hostname", "message");
    }
}
