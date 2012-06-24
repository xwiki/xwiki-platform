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
 * @since 4.0M2
 */
public class DefaultWikiIRCBotListenerManagerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement(exceptions = {EntityReferenceSerializer.class})
    DefaultWikiIRCBotListenerManager manager;

    private DocumentReference wikiBotListenerReference1;
    private DocumentReference wikiBotListenerReference2;

    private WikiIRCBotListener wikiIRCBotListener1;
    private WikiIRCBotListener wikiIRCBotListener2;

    private ListenerManager listenerManager;

    private ComponentManager componentManager;

    @Override
    public void configure() throws Exception
    {
        final WikiIRCModel ircModel = getComponentManager().getInstance(WikiIRCModel.class);

        // Assume we have two Wiki Bot Listeners in the wiki
        final WikiBotListenerData listenerData1 = new WikiBotListenerData(
            new DocumentReference("wik1", "space1", "page1"),
            "space1.page1", "wikiname1", "wikidescription1");
        final WikiBotListenerData listenerData2 = new WikiBotListenerData(
            new DocumentReference("wik2", "space2", "page2"),
            "space2.page2", "wikiname2", "wikidescription2");
        final DocumentReferenceResolver<String> resolver =
            getComponentManager().getInstance(DocumentReferenceResolver.TYPE_STRING, "current");

        this.wikiBotListenerReference1 = new DocumentReference("wiki1", "space1", "page1");
        this.wikiBotListenerReference2 = new DocumentReference("wiki2", "space2", "page2");

        Transformation macroTransformation = getMockery().mock(Transformation.class);
        BlockRenderer plainTextRenderer = getMockery().mock(BlockRenderer.class);
        this.wikiIRCBotListener1 = new WikiIRCBotListener(listenerData1,
            Collections.singletonMap("onSomeEvent1", new XDOM(Arrays.asList((Block) new WordBlock("whatever1")))),
                Syntax.XWIKI_2_1, macroTransformation, plainTextRenderer, ircModel,
                    new DocumentReference("userwiki1", "userspace1", "userpage1"));
        this.wikiIRCBotListener2 = new WikiIRCBotListener(listenerData2,
            Collections.singletonMap("onSomeEvent2", new XDOM(Arrays.asList((Block) new WordBlock("whatever2")))),
                Syntax.XWIKI_2_1, macroTransformation, plainTextRenderer, ircModel,
                    new DocumentReference("userwiki2", "userspace2", "userpage2"));
        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);
        this.listenerManager = getMockery().mock(ListenerManager.class);

        this.componentManager = getComponentManager().getInstance(ComponentManager.class, "wiki");

        getMockery().checking(new Expectations()
        {{
            allowing(ircModel).getWikiBotListenerData();
            will(returnValue(Arrays.asList(listenerData1, listenerData2)));

            allowing(resolver).resolve("space1.page1");
            will(returnValue(wikiBotListenerReference1));
            allowing(resolver).resolve("space2.page2");
            will(returnValue(wikiBotListenerReference2));

            allowing(bot).getListenerManager();
            will(returnValue(listenerManager));
        }});
    }

    @Test
    public void registerWikiBotListeners() throws Exception
    {
        final WikiIRCBotListenerFactory factory =
            getComponentManager().getInstance(WikiIRCBotListenerFactory.class);

        getMockery().checking(new Expectations()
        {{
            // Assume that the first Bot Listener isn't registered yet but that the second is
            oneOf(componentManager).hasComponent((Type) IRCBotListener.class, "wiki1:space1.page1");
            will(returnValue(false));
            oneOf(componentManager).hasComponent((Type) IRCBotListener.class, "wiki2:space2.page2");
            will(returnValue(true));
            oneOf(factory).containsWikiListener(wikiBotListenerReference1);
            will(returnValue(true));
            oneOf(factory).createWikiListener(wikiBotListenerReference1);
            will(returnValue(wikiIRCBotListener1));

            // Real tests are here:
            // - We verify the IRC Bot Listener is registered against the Component Manager
            DefaultComponentDescriptor<IRCBotListener> cd = new DefaultComponentDescriptor<IRCBotListener>();
            cd.setRoleType(IRCBotListener.class);
            cd.setRoleHint("wiki1:space1.page1");
            oneOf(componentManager).registerComponent(cd, wikiIRCBotListener1);

            // - We verify the IRC Bot Listener is added to the IRC Bot
            oneOf(listenerManager).addListener(wikiIRCBotListener1);
        }});

        this.manager.registerWikiBotListeners();
    }

    @Test
    public void registerWikiBotListenersWhenTheyDontContainValidXObjects() throws Exception
    {
        final WikiIRCBotListenerFactory factory =
            getComponentManager().getInstance(WikiIRCBotListenerFactory.class);

        getMockery().checking(new Expectations()
        {{
            // Assume that the first Bot Listener isn't registered yet but that the second is
            oneOf(componentManager).hasComponent((Type) IRCBotListener.class, "wiki1:space1.page1");
            will(returnValue(false));
            oneOf(componentManager).hasComponent((Type) IRCBotListener.class, "wiki2:space2.page2");
            will(returnValue(true));
            oneOf(factory).containsWikiListener(wikiBotListenerReference1);
            will(returnValue(false));
        }});

        this.manager.registerWikiBotListeners();
    }
    @Test
    public void unregisterWikiBotListeners() throws Exception
    {
        getMockery().checking(new Expectations()
        {{
                // Assume that the first Bot Listener is registered and that the second isn't
                oneOf(componentManager).hasComponent((Type) IRCBotListener.class, "wiki1:space1.page1");
                will(returnValue(true));
                oneOf(componentManager).hasComponent((Type) IRCBotListener.class, "wiki2:space2.page2");
                will(returnValue(false));
                oneOf(componentManager).getInstance(IRCBotListener.class, "wiki1:space1.page1");
                will(returnValue(wikiIRCBotListener1));

                // Real tests are here:
                // - The Wiki Bot Listener in unregistered
                oneOf(componentManager).unregisterComponent((Type) IRCBotListener.class, "wiki1:space1.page1");

                // - The Wiki Bot Listener is removed from the Bot Listener Manager
                oneOf(listenerManager).removeListener(wikiIRCBotListener1);
            }});

        this.manager.unregisterWikiBotListeners();
    }
}
