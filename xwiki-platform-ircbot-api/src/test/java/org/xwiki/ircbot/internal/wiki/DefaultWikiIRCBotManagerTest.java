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

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.jmock.Expectations;
import org.junit.Test;
import org.pircbotx.hooks.managers.ListenerManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.ircbot.wiki.WikiIRCBotListenerManager;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

import junit.framework.Assert;

/**
 * Unit tests for {@link DefaultWikiIRCBotManager}.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class DefaultWikiIRCBotManagerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement(exceptions = {EntityReferenceSerializer.class})
    DefaultWikiIRCBotManager manager;

    @Test
    public void startBot() throws Exception
    {
        final IRCBot bot = getComponentManager().lookupComponent(IRCBot.class);
        final WikiIRCModel ircModel = getComponentManager().lookupComponent(WikiIRCModel.class);
        final BotData botData = new BotData("botName", "server", null, "channel", true);
        final ListenerManager listenerManager = getMockery().mock(ListenerManager.class);
        final Provider<List<IRCBotListener>> botListenerComponents =
            getComponentManager().lookupComponent(new DefaultParameterizedType(null, Provider.class,
                new DefaultParameterizedType(null, List.class, IRCBotListener.class)));
        final IRCBotListener componentBotListener = getMockery().mock(IRCBotListener.class);
        final WikiIRCBotListenerManager botListenerManager =
            getComponentManager().lookupComponent(WikiIRCBotListenerManager.class);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
            will(returnValue(false));
            oneOf(ircModel).loadBotData();
            will(returnValue(botData));
            allowing(bot).getListenerManager();
            will(returnValue(listenerManager));
            oneOf(botListenerComponents).get();
            will(returnValue(Collections.singletonList(componentBotListener)));
            oneOf(bot).isConnected();
            will(returnValue(false));

            // Real tests are here:

            // - Verify that the component listener is added to the ListenerManager and that the Wiki Bot Listeners
            //    registration is made
            oneOf(listenerManager).addListener(componentBotListener);
            oneOf(botListenerManager).registerWikiBotListeners();

            // - Verify that the Bot sets the bot name, connects to the server and join the channel
            oneOf(bot).setName("botName");
            oneOf(bot).connect("server");
            oneOf(bot).joinChannel("channel");
        }});

        this.manager.startBot();
    }

    @Test
    public void getBotListenerData() throws Exception
    {
        final ComponentManager componentManager = getComponentManager().lookupComponent(ComponentManager.class, "wiki");
        final IRCBotListener botListener = getMockery().mock(IRCBotListener.class, "test");
        final WikiIRCModel ircModel = getComponentManager().lookupComponent(WikiIRCModel.class);
        final List<BotListenerData> listenerData = Arrays.asList(
            new BotListenerData("wikiid", "wikiname", "wikidescription"));

        getMockery().checking(new Expectations()
        {{
            oneOf(componentManager).lookupMap((Type) IRCBotListener.class);
                will(returnValue(Collections.singletonMap("docspace.docname", botListener)));
            oneOf(botListener).getName();
                will(returnValue("name"));
            oneOf(botListener).getDescription();
                will(returnValue("description"));
            oneOf(ircModel).getWikiBotListenerData();
                will(returnValue(listenerData));
        }});

        List<BotListenerData> data = this.manager.getBotListenerData();
        Assert.assertEquals(2, data.size());

        List<BotListenerData> expectedData = Arrays.asList(
            new BotListenerData("docspace.docname", "name", "description"),
            new BotListenerData("wikiid", "wikiname", "wikidescription"));
        Assert.assertEquals(expectedData, data);
    }
}
