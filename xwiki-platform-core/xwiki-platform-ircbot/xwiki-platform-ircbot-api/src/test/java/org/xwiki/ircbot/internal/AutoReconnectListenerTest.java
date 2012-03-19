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

import org.jmock.Expectations;
import org.junit.Test;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.managers.ThreadedListenerManager;
import org.slf4j.Logger;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;
import org.jmock.lib.legacy.ClassImposteriser;

/**
 * Unit tests for {@link AutoReconnectListener}.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class AutoReconnectListenerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement(exceptions = { Logger.class })
    private AutoReconnectListener<ExtendedPircBotX> listener;

    @Override
    public void setUp() throws Exception
    {
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);
        super.setUp();
    }

    @Test
    public void reconnectOnDisconnect() throws Exception
    {
        final ExtendedPircBotX bot = getMockery().mock(ExtendedPircBotX.class);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).getListenerManager();
                will(returnValue(new ThreadedListenerManager<ExtendedPircBotX>()));
            oneOf(bot).shouldStop();
                will(returnValue(false));

            // The test is here: we verify that we're not connected at first and the we are after reconnect() is called
            oneOf(bot).isConnected();
                will(returnValue(false));
            oneOf(bot).reconnect();
            oneOf(bot).isConnected();
                will(returnValue(true));
        }});

        this.listener.onDisconnect(new DisconnectEvent(bot));
    }

    @Test
    public void OnDisconnectWhenShouldStop() throws Exception
    {
        final ExtendedPircBotX bot = getMockery().mock(ExtendedPircBotX.class);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).getListenerManager();
                will(returnValue(new ThreadedListenerManager<ExtendedPircBotX>()));
            oneOf(bot).shouldStop();
                will(returnValue(true));
        }});

        this.listener.onDisconnect(new DisconnectEvent(bot));
    }
}
