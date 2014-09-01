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

import java.util.Arrays;
import java.util.Collections;

import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Test;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.managers.ThreadedListenerManager;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.internal.ExtendedPircBotX;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.internal.transformation.macro.MacroErrorManager;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.jmock.AbstractComponentTestCase;

/**
 * Unit tests for {@link DefaultExecutor}.
 *
 * @version $Id$
 * @since 4.3M2
 */
public class DefaultExecutorTest extends AbstractComponentTestCase
{
    private ExtendedPircBotX bot;

    @Override
    public void setUp() throws Exception
    {
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);
        super.setUp();

        registerMockComponent(ContextualAuthorizationManager.class);
    }

    @Test
    public void executeWhenScriptGeneratesEmptyResult() throws Exception
    {
        final XDOM xdom = new XDOM(Collections.<Block>emptyList());
        Event event = createTestEvent();
        final RenderingContext renderingContext = getMockery().mock(MutableRenderingContext.class);
        final Transformation macroTransformation = getMockery().mock(Transformation.class);
        final BlockRenderer plainTextRenderer = getComponentManager().getInstance(BlockRenderer.class, "plain/1.0");

        getMockery().checking(new Expectations()
        {{
            oneOf((MutableRenderingContext) renderingContext).transformInContext(with(equal(macroTransformation)), with(any(TransformationContext.class)), with(equal(xdom)));
            // The XDOM is modified by the transformation; simulate it here so that it returns an empty XDOM
            xdom.addChild(new ParagraphBlock(Collections.<Block>emptyList()));
            // The test is here! We ensure that the bot isn't called and thus that no message is sent to the channel
            never(bot);
        }});

        DefaultExecutor executor =
            new DefaultExecutor(xdom, Syntax.XWIKI_2_1, event, renderingContext, macroTransformation, plainTextRenderer
            );
        executor.execute();
    }

    @Test
    public void executeWhenScriptGeneratesNonEmptyResult() throws Exception
    {
        final XDOM xdom = new XDOM(Collections.<Block>emptyList());
        Event event = createTestEvent();
        final RenderingContext renderingContext = getMockery().mock(MutableRenderingContext.class);
        final Transformation macroTransformation = getMockery().mock(Transformation.class);
        final BlockRenderer plainTextRenderer = getComponentManager().getInstance(BlockRenderer.class, "plain/1.0");

        getMockery().checking(new Expectations()
        {{
            oneOf((MutableRenderingContext) renderingContext).transformInContext(with(equal(macroTransformation)), with(any(TransformationContext.class)), with(equal(xdom)));
            // The XDOM is modified by the transformation; simulate it here so that it returns a non empty XDOM
            xdom.addChild(new ParagraphBlock(Arrays.<Block>asList(new WordBlock("test"))));
            // The test is here!
            oneOf(bot).sendMessage(with(any(Channel.class)), with(any(User.class)), with(equal("test")));
        }});

        DefaultExecutor executor =
            new DefaultExecutor(xdom, Syntax.XWIKI_2_1, event, renderingContext, macroTransformation, plainTextRenderer
            );
        executor.execute();
    }

    @Test
    public void executeWhenScriptGeneratesAMacroError() throws Exception
    {
        final MacroBlock macroBlock = new MacroBlock("testmacro", Collections.<String, String>emptyMap(), false);
        final XDOM xdom = new XDOM(Arrays.<Block>asList(macroBlock));
        Event event = createTestEvent();
        final RenderingContext renderingContext = getMockery().mock(MutableRenderingContext.class);
        final Transformation macroTransformation = getMockery().mock(Transformation.class);
        final BlockRenderer plainTextRenderer = getComponentManager().getInstance(BlockRenderer.class, "plain/1.0");

        getMockery().checking(new Expectations()
        {{
            oneOf((MutableRenderingContext) renderingContext).transformInContext(with(equal(macroTransformation)), with(any(TransformationContext.class)), with(equal(xdom)));
            // The XDOM is modified by the transformation; simulate it here so that it returns a macro error
            MacroErrorManager macroErrorManager = new MacroErrorManager();
            macroErrorManager.generateError(macroBlock, "test message", "test description");
        }});

        DefaultExecutor executor =
            new DefaultExecutor(xdom, Syntax.XWIKI_2_1, event, renderingContext, macroTransformation, plainTextRenderer
            );
        try {
            executor.execute();
            Assert.fail("Should have raised an exception");
        } catch (IRCBotException expected) {
            Assert.assertEquals("Macro error when rendering Wiki Bot Listener content [test messagetest description]",
                expected.getMessage());
        }
    }

    private Event createTestEvent()
    {
        class TestableChannel extends Channel
        {
            public TestableChannel(PircBotX bot, String name)
            {
                super(bot, name);
            }
        }

        class TestableUser extends User
        {
            protected TestableUser(PircBotX bot, String nick)
            {
                super(bot, nick);
            }
        }

        this.bot = getMockery().mock(ExtendedPircBotX.class);
        getMockery().checking(new Expectations()
        {{
            oneOf(bot).getListenerManager();
            will(returnValue(new ThreadedListenerManager<ExtendedPircBotX>()));
        }});

        return new MessageEvent(bot, new TestableChannel(bot, "channel"), new TestableUser(bot, "nick"), "message");
    }
}
