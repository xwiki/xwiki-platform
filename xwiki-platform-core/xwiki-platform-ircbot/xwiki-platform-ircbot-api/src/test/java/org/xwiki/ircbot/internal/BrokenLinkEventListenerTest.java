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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.Buffer;
import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.ircbot.BrokenLinkEventListenerConfiguration;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.transformation.linkchecker.InvalidURLEvent;
import org.xwiki.rendering.transformation.linkchecker.LinkState;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

import junit.framework.Assert;

/**
 * Unit tests for {@link BrokenLinkEventListener}.
 *
 * @version $Id$
 * @since 4.2M3
 */
@MockingRequirement(BrokenLinkEventListener.class)
public class BrokenLinkEventListenerTest extends AbstractMockingComponentTestCase<EventListener>
{
    @Test
    public void onEventWhenBotNotConnected() throws Exception
    {
        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);
        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
            will(returnValue(false));

            // The test is here, ensure that no message is sent to the IRC channel
            never(bot).sendMessage(with(any(String.class)), with(any(String.class)));
        }});

        getMockedComponent().onEvent(null, null, null);
    }

    @Test
    public void onEventWhenBotIsConnectedButBrokenLinkBotListenerIsNotActive() throws Exception
    {
        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);
        final BrokenLinkEventListenerConfiguration configuration =
            getComponentManager().getInstance(BrokenLinkEventListenerConfiguration.class);
        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
            will(returnValue(true));
            oneOf(configuration).isActive();
            will(returnValue(false));

            // The test is here, ensure that no message is sent to the IRC channel
            never(bot).sendMessage(with(any(String.class)), with(any(String.class)));
        }});

        getMockedComponent().onEvent(null, null, null);
    }

    @Test
    public void onEventWhenInvalidURLEvent() throws Exception
    {
        sendInvalidURLEvent("testurl", null);

        Buffer buffer = ((BrokenLinkEventListener) getMockedComponent()).getLastBrokenLinks();
        Assert.assertEquals(1, buffer.size());
    }

    @Test
    public void onEventWhenInvalidURLEventAndContextData() throws Exception
    {
        Map<String, Object> contextData = Collections.singletonMap("datakey", (Object) "datavalue");
        sendInvalidURLEvent("testurl",
            "Invalid link testurl on page testsource (code = 404, datakey = datavalue)", contextData);

        Buffer buffer = ((BrokenLinkEventListener) getMockedComponent()).getLastBrokenLinks();
        Assert.assertEquals(1, buffer.size());
    }

    @Test
    public void onEventWhenSeveralInvalidURLEventWithSameURL() throws Exception
    {
        sendInvalidURLEvent("testurl", null);
        sendInvalidURLEvent("testurl", null);

        Buffer buffer = ((BrokenLinkEventListener) getMockedComponent()).getLastBrokenLinks();
        Assert.assertEquals(1, buffer.size());
    }

    @Test
    public void onEventWhenSeveralInvalidURLEventWithDifferentURL() throws Exception
    {
        sendInvalidURLEvent("testurl1", null);
        sendInvalidURLEvent("testurl2", null);

        Buffer buffer = ((BrokenLinkEventListener) getMockedComponent()).getLastBrokenLinks();
        Assert.assertEquals(2, buffer.size());
    }

    @Test
    public void onEventWhenSeveralInvalidURLEventSavesOnlyLastFour() throws Exception
    {
        // First one will be ejected by the 5th one since only last 4 broken links are saved
        sendInvalidURLEvent("testurl1", null);
        sendInvalidURLEvent("testurl2", null);
        sendInvalidURLEvent("testurl3", null);
        sendInvalidURLEvent("testurl4", null);
        sendInvalidURLEvent("testurl5", null);

        Buffer buffer = ((BrokenLinkEventListener) getMockedComponent()).getLastBrokenLinks();
        Assert.assertEquals(4, buffer.size());

        // Get the latest item from the buffer and verify its url is "testurl2" and not "testurl1", thus proving
        // that the first item has been ejected.
        Map<String, Object> source = (Map<String, Object>) buffer.get();
        Assert.assertEquals("testurl2", source.get("url"));
    }

    private void sendInvalidURLEvent(final String url, final Map<String, Object> contextData) throws Exception
    {
        sendInvalidURLEvent(url, "Invalid link " + url + " on page testsource (code = 404)", contextData);
    }

    private void sendInvalidURLEvent(final String url, final String sendMessageAssertMessage,
        final Map<String, Object> contextData) throws Exception
    {
        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);
        final BrokenLinkEventListenerConfiguration configuration =
                getComponentManager().getInstance(BrokenLinkEventListenerConfiguration.class);
        getMockery().checking(new Expectations()
        {{
                oneOf(bot).isConnected();
                will(returnValue(true));
                oneOf(configuration).isActive();
                will(returnValue(true));
                oneOf(bot).getChannelsNames();
                will(returnValue(Collections.singleton("channel")));
                oneOf(bot).sendMessage("channel", sendMessageAssertMessage);
            }});

        Map<String, Object> source = new HashMap<String, Object>();
        source.put("url", url);
        source.put("source", "testsource");
        source.put("state", new LinkState(404, System.currentTimeMillis()));

        if (contextData != null) {
            source.put("contextData", contextData);
        }

        getMockedComponent().onEvent(new InvalidURLEvent(), source, null);
    }
}
