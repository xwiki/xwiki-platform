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

import org.jmock.Expectations;
import org.junit.Test;
import org.pircbotx.hooks.managers.ListenerManager;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.ircbot.wiki.WikiIRCBotListenerFactory;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link DefaultWikiIRCBotListenerManager}.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class DefaultWikiIRCBotListenerManagerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement(exceptions = {EntityReferenceSerializer.class})
    DefaultWikiIRCBotListenerManager manager;

    @Test
    public void registerWikiBotListeners() throws Exception
    {
        final WikiIRCModel ircModel = getComponentManager().lookupComponent(WikiIRCModel.class);
        // Assume we have one Wiki Bot Listener in the wiki
        final BotListenerData listenerData = new BotListenerData("space.page", "wikiname", "wikidescription");
        final DocumentReferenceResolver<String> resolver =
            getComponentManager().lookupComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        final DocumentReference wikiBotListenerReference = new DocumentReference("wiki", "space", "page");
        final ComponentManager componentManager = getComponentManager().lookupComponent(ComponentManager.class, "wiki");
        final WikiIRCBotListenerFactory factory =
            getComponentManager().lookupComponent(WikiIRCBotListenerFactory.class);
        final WikiIRCBotListener wikiIRCBotListener = new WikiIRCBotListener(listenerData,
            Collections.singletonMap("onSomeEvent", new XDOM(Arrays.asList((Block) new WordBlock("whatever")))),
                Syntax.XWIKI_2_1, getMockery().mock(Transformation.class), getMockery().mock(BlockRenderer.class),
                    ircModel);
        final IRCBot bot = getComponentManager().lookupComponent(IRCBot.class);
        final ListenerManager listenerManager = getMockery().mock(ListenerManager.class);

        getMockery().checking(new Expectations()
        {{
            oneOf(ircModel).getWikiBotListenerData();
            will(returnValue(Arrays.asList(listenerData)));
            oneOf(resolver).resolve("space.page");
            will(returnValue(wikiBotListenerReference));
            // Assume that the Wiki Bot Listener isn't registered yet
            oneOf(componentManager).hasComponent((Type) IRCBotListener.class, "wiki:space.page");
            will(returnValue(false));
            oneOf(factory).containsWikiListener(wikiBotListenerReference);
            will(returnValue(true));
            oneOf(factory).createWikiListener(wikiBotListenerReference);
            will(returnValue(wikiIRCBotListener));
            oneOf(bot).getListenerManager();
            will(returnValue(listenerManager));

            // Real tests are here:
            // - We verify the IRC Bot Listener is registered against the Component Manager
            DefaultComponentDescriptor<IRCBotListener> cd = new DefaultComponentDescriptor<IRCBotListener>();
            cd.setRoleType(IRCBotListener.class);
            cd.setRoleHint("wiki:space.page");
            oneOf(componentManager).registerComponent(cd, wikiIRCBotListener);

            // - We verify the IRC Bot Listener is added to the IRC Bot
            oneOf(listenerManager).addListener(wikiIRCBotListener);
        }});

        this.manager.registerWikiBotListeners();
    }
}
