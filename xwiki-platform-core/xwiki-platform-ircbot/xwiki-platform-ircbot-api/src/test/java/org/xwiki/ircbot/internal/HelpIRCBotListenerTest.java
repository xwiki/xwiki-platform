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

import java.lang.reflect.Type;
import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link HelpIRCBotListener}.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class HelpIRCBotListenerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private HelpIRCBotListener<PircBotX> listener;

    @Override
    public void setUp() throws Exception
    {
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);
        super.setUp();
    }

    @Test
    public void onMessageWhenMessageDoesntStartWithCommand() throws Exception
    {
        final MessageEvent event = getMockery().mock(MessageEvent.class);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(event).getMessage();
                will(returnValue("whatever"));
            }
        });

        this.listener.onMessage(event);
    }

    @Test
    public void onMessageWhenMessageStartsWithCommand() throws Exception
    {
        final MessageEvent event = getMockery().mock(MessageEvent.class);
        final ComponentManager componentManager = getComponentManager().getInstance(ComponentManager.class, "wiki");
        final IRCBotListener botListener = getMockery().mock(IRCBotListener.class);

        // With this we also verify that the default Bot Listeners are correctly registered in the Compnent Manager.
        final IRCBotListener helpBotListener = getComponentManager().getInstance(IRCBotListener.class, "help");
        final IRCBotListener reconnectBotListener =
            getComponentManager().getInstance(IRCBotListener.class, "autoreconnect");

        getMockery().checking(new Expectations()
        {
            {
                oneOf(event).getMessage();
                will(returnValue("!help"));
                oneOf(event).respond("Available Bot listeners:");
                oneOf(componentManager).getInstanceList((Type) IRCBotListener.class);
                will(returnValue(Arrays.asList(botListener, helpBotListener, reconnectBotListener)));
                oneOf(botListener).getDescription();
                will(returnValue("listener description"));
                oneOf(event).respond(" - listener description");
                oneOf(event).respond(" - !help: List all commands available");
                oneOf(event).respond(" - Automatically reconnect the Bot to the channels it's been disconnected from");
            }
        });

        this.listener.onMessage(event);
    }
}