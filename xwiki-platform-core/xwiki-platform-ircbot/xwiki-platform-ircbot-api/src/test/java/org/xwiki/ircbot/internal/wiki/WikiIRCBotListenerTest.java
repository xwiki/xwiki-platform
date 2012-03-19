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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.managers.ThreadedListenerManager;
import org.xwiki.ircbot.internal.ExtendedPircBotX;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.test.AbstractComponentTestCase;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

import junit.framework.Assert;

/**
 * Unit tests for {@link WikiIRCBotListener}.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class WikiIRCBotListenerTest extends AbstractComponentTestCase
{
    @Override
    public void setUp() throws Exception
    {
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);
        super.setUp();
    }

    @Test
    public void onMessage() throws Exception
    {
        final DocumentReference wikiBotListenerReference = new DocumentReference("wiki", "space", "page");
        WikiBotListenerData data = new WikiBotListenerData(wikiBotListenerReference,
            "space.page", "name", "description");
        Map<String, XDOM> events = new HashMap<String, XDOM>();
        events.put("onMessage", new XDOM(Collections.<Block>emptyList()));
        final Transformation macroTransformation = getMockery().mock(Transformation.class);
        final BlockRenderer renderer = getMockery().mock(BlockRenderer.class);
        final WikiIRCModel ircModel = getMockery().mock(WikiIRCModel.class);
        final ExtendedPircBotX bot = getMockery().mock(ExtendedPircBotX.class);

        Utils.setComponentManager(getComponentManager());
        final XWikiContext xwikiContext = new XWikiContext();

        final DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");
        WikiIRCBotListener listener = new WikiIRCBotListener(data, events, Syntax.XWIKI_2_1, macroTransformation,
            renderer, ircModel, userReference);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).getListenerManager();
                will(returnValue(new ThreadedListenerManager<ExtendedPircBotX>()));
            oneOf(ircModel).getXWikiContext();
                will(returnValue(xwikiContext));
            oneOf(ircModel).executeAsUser(with(equal(userReference)), with(equal(wikiBotListenerReference)),
                with(any(WikiIRCModel.Executor.class)));
        }});

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

        listener.onEvent(new MessageEvent(bot, new TestableChannel(bot, "channel"), new TestableUser(bot, "nick"),
            "message"));

        // Verify that the XWikiContext has been populated with bindings
        Map < String, Object > bindings = (Map<String, Object>) xwikiContext.get("irclistener");
        Assert.assertTrue(bindings.get("channel") instanceof Channel);
        Assert.assertEquals("channel", ((Channel) bindings.get("channel")).getName());

        Assert.assertTrue(bindings.get("user") instanceof User);
        Assert.assertEquals("nick", ((User) bindings.get("user")).getNick());

        Assert.assertEquals("message", bindings.get("message"));
    }
}
