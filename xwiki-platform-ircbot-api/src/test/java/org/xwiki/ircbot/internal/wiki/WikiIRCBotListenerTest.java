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
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.test.AbstractComponentTestCase;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

import junit.framework.Assert;

/**
 * Unit tests for {@link WikiIRCBotListener}.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class WikiIRCBotListenerTest extends AbstractComponentTestCase
{
    @Test
    public void onMessage() throws Exception
    {
        BotListenerData data = new BotListenerData("id", "name", "description", true);
        Map<String, XDOM> events = new HashMap<String, XDOM>();
        events.put("onMessage", new XDOM(Collections.<Block>emptyList()));
        final Transformation macroTransformation = getMockery().mock(Transformation.class);
        final BlockRenderer renderer = getMockery().mock(BlockRenderer.class);
        final IRCBot bot = getMockery().mock(IRCBot.class);
        final Execution execution = getMockery().mock(Execution.class);
        final ExecutionContext context = new ExecutionContext();

        Utils.setComponentManager(getComponentManager());
        XWikiContext xwikiContext = new XWikiContext();
        context.setProperty("xwikicontext", xwikiContext);

        WikiIRCBotListener listener = new WikiIRCBotListener(data, events, Syntax.XWIKI_2_1, macroTransformation,
            renderer, bot, execution);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).getConnectedChannels();
                will(returnValue(new String[] {"channel"}));
            oneOf(execution).getContext();
                will(returnValue(context));
            oneOf(macroTransformation).transform(with(any(XDOM.class)), with(any(TransformationContext.class)));
            oneOf(renderer).render(with(any(XDOM.class)), with(any(WikiPrinter.class)));
        }});

        listener.onMessage("channel", "sender", "login", "hostname", "message");

        // Verify that the XWikiContext has been populated wiuth bindings
        Map<String, Object> bindings = (Map<String, Object>) xwikiContext.get("irclistener");
        Assert.assertEquals("channel", bindings.get("channel"));
        Assert.assertEquals("sender", bindings.get("sender"));
        Assert.assertEquals("login", bindings.get("login"));
        Assert.assertEquals("hostname", bindings.get("hostname"));
        Assert.assertEquals("message", bindings.get("message"));
    }
}
