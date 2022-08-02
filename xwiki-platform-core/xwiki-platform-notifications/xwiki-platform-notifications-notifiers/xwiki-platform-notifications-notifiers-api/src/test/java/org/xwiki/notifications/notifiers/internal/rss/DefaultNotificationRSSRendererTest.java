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
package org.xwiki.notifications.notifiers.internal.rss;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import javax.script.ScriptContext;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.xwiki.eventstream.Event;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.rometools.rome.feed.synd.SyndEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNotificationRSSRenderer}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
@ComponentTest
public class DefaultNotificationRSSRendererTest
{
    @InjectMockComponents
    private DefaultNotificationRSSRenderer defaultNotificationRSSRenderer;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    private void mockEvent(CompositeEvent testCompositeEvent, boolean noTitle, boolean noDocument, boolean unknownTitle)
        throws Exception
    {
        when(this.contextualLocalizationManager.getTranslationPlain("EventTitle", "EventDocumentTitle"))
            .thenReturn("TranslatedEventTitle");
        when(this.contextualLocalizationManager.getTranslationPlain("EventTitle"))
            .thenReturn("TranslatedEventTitleNoDocument");
        when(this.contextualLocalizationManager.getTranslationPlain("notifications.rss.defaultTitleWithPage",
            "EventDocumentTitle")).thenReturn("DefaultTitle");
        when(this.contextualLocalizationManager.getTranslationPlain("notifications.rss.defaultTitle"))
            .thenReturn("DefaultTitleNoDocument");

        Event testEvent1 = mock(Event.class);
        Date testEventDate = mock(Date.class);
        if (!noTitle) {
            when(testEvent1.getTitle()).thenReturn("EventTitle");
        }
        if (!noDocument) {
            when(testEvent1.getDocumentTitle()).thenReturn("EventDocumentTitle");
        }
        if (!unknownTitle) {
            when(this.contextualLocalizationManager.getTranslation("EventTitle")).thenReturn(mock(Translation.class));
        } else {
            when(this.contextualLocalizationManager.getTranslation("EventTitle")).thenReturn(null);
        }

        DocumentReference testEventAuthor1 = new DocumentReference("xwiki", "XWiki", "AuthorName");

        when(this.templateManager.getTemplate(ArgumentMatchers.any())).thenReturn(Mockito.mock(Template.class));

        when(testCompositeEvent.getEvents()).thenReturn(Arrays.asList(testEvent1));
        when(testCompositeEvent.getUsers()).thenReturn(new HashSet<>(Arrays.asList(testEventAuthor1)));
        when(testCompositeEvent.getEventIds()).thenReturn(Arrays.asList("id1"));
        when(testCompositeEvent.getType()).thenReturn("eventType");
        when(testCompositeEvent.getDates()).thenReturn(Arrays.asList(testEventDate));

    }

    @Test
    void renderNotification() throws Exception
    {
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(Mockito.mock(ScriptContext.class));

        CompositeEvent testCompositeEvent = mock(CompositeEvent.class);
        this.mockEvent(testCompositeEvent, false, false, false);
        SyndEntry resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("TranslatedEventTitle", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = mock(CompositeEvent.class);
        this.mockEvent(testCompositeEvent, false, true, false);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("TranslatedEventTitleNoDocument", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = mock(CompositeEvent.class);
        this.mockEvent(testCompositeEvent, true, true, false);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitleNoDocument", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = mock(CompositeEvent.class);
        this.mockEvent(testCompositeEvent, true, false, false);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitle", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = mock(CompositeEvent.class);
        this.mockEvent(testCompositeEvent, false, true, true);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitleNoDocument", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = mock(CompositeEvent.class);
        this.mockEvent(testCompositeEvent, false, false, true);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitle", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = mock(CompositeEvent.class);
        this.mockEvent(testCompositeEvent, true, true, true);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitleNoDocument", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = mock(CompositeEvent.class);
        this.mockEvent(testCompositeEvent, true, false, true);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitle", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("id1", resultEntry.getUri());
    }
}
