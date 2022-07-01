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
package org.xwiki.notifications.notifiers.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.internal.RestrictedConfigurationSourceProvider;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.CompositeEventStatus;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationRenderer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.internal.renderer.html5.HTML5BlockRenderer;
import org.xwiki.rendering.internal.renderer.html5.HTML5Renderer;
import org.xwiki.rendering.internal.renderer.html5.HTML5RendererFactory;
import org.xwiki.rendering.internal.renderer.xhtml.image.DefaultXHTMLImageRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.image.DefaultXHTMLImageTypeRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.DefaultXHTMLLinkRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.DefaultXHTMLLinkTypeRenderer;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.xml.internal.html.DefaultHTMLElementSanitizer;
import org.xwiki.xml.internal.html.HTMLDefinitions;
import org.xwiki.xml.internal.html.HTMLElementSanitizerConfiguration;
import org.xwiki.xml.internal.html.MathMLDefinitions;
import org.xwiki.xml.internal.html.SVGDefinitions;
import org.xwiki.xml.internal.html.SecureHTMLElementSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link InternalHtmlNotificationRenderer}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({
    HTML5BlockRenderer.class,
    HTML5RendererFactory.class,
    HTML5Renderer.class,
    DefaultXHTMLLinkRenderer.class,
    DefaultXHTMLLinkTypeRenderer.class,
    DefaultXHTMLImageRenderer.class,
    DefaultXHTMLImageTypeRenderer.class,
    DefaultHTMLElementSanitizer.class,
    SecureHTMLElementSanitizer.class,
    HTMLElementSanitizerConfiguration.class,
    RestrictedConfigurationSourceProvider.class,
    HTMLDefinitions.class,
    MathMLDefinitions.class,
    SVGDefinitions.class,
    DefaultExecution.class
})
public class InternalHtmlNotificationRendererTest
{
    @InjectMockComponents
    private InternalHtmlNotificationRenderer notificationRenderer;

    @MockComponent
    private NotificationRenderer eventRenderer;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @Test
    public void renderCount()
    {
        String renderedCount = this.notificationRenderer.render(42);
        assertEquals("<span class=\"notifications-count badge\">42</span>", renderedCount);
    }

    @Test
    public void renderSingleEvent() throws Exception
    {
        CompositeEvent compositeEvent = mock(CompositeEvent.class);
        CompositeEventStatus eventStatus = mock(CompositeEventStatus.class);
        when(compositeEvent.getType()).thenReturn("FooType");
        when(compositeEvent.getEventIds()).thenReturn(Arrays.asList("foo", "bar", "baz"));
        when(compositeEvent.getDates()).thenReturn(Arrays.asList(
            new Date(0),
            new Date(10),
            new Date(12),
            new Date(42)
        ));

        GroupBlock block = new GroupBlock();
        block.setParameter("class", "my-composite-event");
        when(eventRenderer.render(compositeEvent)).thenReturn(block);
        when(eventStatus.getStatus()).thenReturn(false);

        String expectedResult = "<div data-eventtype=\"FooType\" data-ids=\"foo,bar,baz\" data-eventdate=\"42\" "
            + "class=\"notification-event notification-event-unread\">"
            + "<div class=\"my-composite-event\"></div></div>";
        assertEquals(expectedResult, this.notificationRenderer.render(compositeEvent, eventStatus));

        when(eventStatus.getStatus()).thenReturn(true);
        expectedResult = "<div data-eventtype=\"FooType\" data-ids=\"foo,bar,baz\" data-eventdate=\"42\" "
            + "class=\"notification-event\">"
            + "<div class=\"my-composite-event\"></div></div>";
        assertEquals(expectedResult, this.notificationRenderer.render(compositeEvent, eventStatus));

        expectedResult = "<div data-eventtype=\"FooType\" data-ids=\"foo,bar,baz\" data-eventdate=\"42\" "
            + "class=\"notification-event\">"
            + "<div class=\"my-composite-event\"></div></div>";
        assertEquals(expectedResult, this.notificationRenderer.render(compositeEvent, null));
    }

    @Test
    public void renderMany() throws NotificationException
    {
        WordBlock wordBlock = new WordBlock("Nothing");
        Translation translation = mock(Translation.class);
        when(localizationManager.getTranslation("notifications.menu.nothing")).thenReturn(translation);
        when(translation.render()).thenReturn(wordBlock);

        String expectedResult = "<p class=\"text-center noitems\">Nothing</p>";
        assertEquals(expectedResult, this.notificationRenderer.render(Collections.emptyList(), Collections.emptyList(),
            false));

        CompositeEvent compositeEvent1 = mock(CompositeEvent.class);
        CompositeEventStatus eventStatus1 = mock(CompositeEventStatus.class);
        when(compositeEvent1.getType()).thenReturn("FooType");
        when(compositeEvent1.getEventIds()).thenReturn(Arrays.asList("foo", "bar", "baz"));
        when(compositeEvent1.getDates()).thenReturn(Arrays.asList(
            new Date(0),
            new Date(10),
            new Date(12),
            new Date(42)
        ));

        GroupBlock block = new GroupBlock();
        block.setParameter("class", "my-composite-event1");
        when(eventRenderer.render(compositeEvent1)).thenReturn(block);
        when(eventStatus1.getStatus()).thenReturn(false);

        CompositeEvent compositeEvent2 = mock(CompositeEvent.class);
        CompositeEventStatus eventStatus2 = mock(CompositeEventStatus.class);
        when(compositeEvent2.getType()).thenReturn("BarType");
        when(compositeEvent2.getEventIds()).thenReturn(Collections.singletonList("oneId"));
        when(compositeEvent2.getDates()).thenReturn(Arrays.asList(
            new Date(12)
        ));

        Block block2 = new GroupBlock();
        block2.setParameter("class", "my-composite-event2");
        when(eventRenderer.render(compositeEvent2)).thenReturn(block2);
        when(eventStatus2.getStatus()).thenReturn(true);

        expectedResult = "<div data-eventtype=\"FooType\" data-ids=\"foo,bar,baz\" data-eventdate=\"42\" "
            + "class=\"notification-event notification-event-unread\">"
            + "<div class=\"my-composite-event1\"></div></div>"
            + "<div data-eventtype=\"BarType\" data-ids=\"oneId\" data-eventdate=\"12\" class=\"notification-event\">"
            + "<div class=\"my-composite-event2\"></div></div>"
            + "<div class=\"notifications-macro-load-more\"></div>";

        assertEquals(expectedResult, this.notificationRenderer.render(
            Arrays.asList(compositeEvent1, compositeEvent2),
            Arrays.asList(eventStatus1, eventStatus2),
            true));

        expectedResult = "<div data-eventtype=\"FooType\" data-ids=\"foo,bar,baz\" data-eventdate=\"42\" "
            + "class=\"notification-event\">"
            + "<div class=\"my-composite-event1\"></div></div>"
            + "<div data-eventtype=\"BarType\" data-ids=\"oneId\" data-eventdate=\"12\" class=\"notification-event\">"
            + "<div class=\"my-composite-event2\"></div></div>"
            + "<div class=\"notifications-macro-load-more\"></div>";

        assertEquals(expectedResult, this.notificationRenderer.render(
            Arrays.asList(compositeEvent1, compositeEvent2),
            null,
            true));
    }
}
