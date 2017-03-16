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
package org.xwiki.platform.blog.internal;

import javax.inject.Provider;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.platform.blog.events.BlogPostPublishedEvent;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * @version $Id$
 */
public class BlogNotificationDisplayerTest
{
    @Rule
    public MockitoComponentMockingRule<BlogNotificationDisplayer> mocker =
            new MockitoComponentMockingRule<>(BlogNotificationDisplayer.class);

    private TemplateManager templateManager;
    private Provider<XWikiContext> contextProvider;
    private VelocityManager velocityManager;

    private XWikiContext context;
    private XWiki xwiki;
    private VelocityContext velocityContext;

    @Before
    public void setUp() throws Exception
    {
        templateManager = mocker.getInstance(TemplateManager.class);
        velocityManager = mocker.getInstance(VelocityManager.class);

        contextProvider = mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);

        velocityContext = mock(VelocityContext.class);
        when(velocityManager.getCurrentVelocityContext()).thenReturn(velocityContext);
    }

    @Test
    public void renderNotificationWithDefaultTemplate() throws Exception
    {
        when(xwiki.evaluateVelocity(anyString(), eq("blog-notification"))).thenReturn("<h1>Notification</h1>");

        // Test
        Event event = mock(Event.class);
        assertEquals(new RawBlock("<h1>Notification</h1>", Syntax.HTML_5_0),
                mocker.getComponentUnderTest().renderNotification(event));

        // Verify
        verify(velocityContext).put("event", event);
        verify(velocityContext).remove("event");
    }

    @Test
    public void renderNotificationWhenTemplateExists() throws Exception
    {
        // Mocks
        Template template = mock(Template.class);
        when(templateManager.getTemplate(
                "notification/org.xwiki.platform.blog.events.BlogPostPublishedEvent.vm")).thenReturn(template);
        XDOM xdom = mock(XDOM.class);
        when(templateManager.executeNoException(template)).thenReturn(xdom);

        // Test
        Event event = mock(Event.class);
        assertEquals(xdom, mocker.getComponentUnderTest().renderNotification(event));

        // Verify
        verify(velocityContext).put("event", event);
        verify(velocityContext).remove("event");
    }

    @Test
    public void getSupportedEvents() throws Exception
    {
        assertEquals(1, mocker.getComponentUnderTest().getSupportedEvents().size());
        assertTrue(mocker.getComponentUnderTest().getSupportedEvents().contains(
                BlogPostPublishedEvent.class.getCanonicalName()));
    }
}
