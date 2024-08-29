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
import java.util.Set;

import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
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
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

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

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private DocumentReference testEventAuthor1 = new DocumentReference("xwiki", "XWiki", "AuthorName");

    @BeforeEach
    void setup()
    {
        when(this.contextualLocalizationManager.getTranslationPlain("EventTitle", "EventDocumentTitle"))
            .thenReturn("TranslatedEventTitle");
        when(this.contextualLocalizationManager.getTranslationPlain("EventTitle"))
            .thenReturn("TranslatedEventTitleNoDocument");
        when(this.contextualLocalizationManager.getTranslationPlain("notifications.rss.defaultTitleWithPage",
            "EventDocumentTitle")).thenReturn("DefaultTitle");
        when(this.contextualLocalizationManager.getTranslationPlain("notifications.rss.defaultTitle"))
            .thenReturn("DefaultTitleNoDocument");

        XWikiContext context = mock(XWikiContext.class);
        XWiki wiki = mock(XWiki.class);
        when(this.contextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(wiki);
        when(wiki.getPlainUserName(null, context)).thenReturn("Guest");
        when(wiki.getPlainUserName(testEventAuthor1, context)).thenReturn("Foo Bar");
    }

    private CompositeEvent mockEvent(boolean noTitle, boolean noDocument, boolean unknownTitle, boolean unknownAuthor)
    {
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

        when(this.templateManager.getTemplate(ArgumentMatchers.any())).thenReturn(Mockito.mock(Template.class));

        CompositeEvent testCompositeEvent = mock(CompositeEvent.class);
        when(testCompositeEvent.getEvents()).thenReturn(Arrays.asList(testEvent1));
        if (unknownAuthor) {
            Set<DocumentReference> authorSet = new HashSet<>();
            authorSet.add(null);
            when(testCompositeEvent.getUsers()).thenReturn(authorSet);
        } else {
            when(testCompositeEvent.getUsers()).thenReturn(Set.of(testEventAuthor1));
        }
        when(testCompositeEvent.getEventIds()).thenReturn(Arrays.asList("id1"));
        when(testCompositeEvent.getType()).thenReturn("eventType");
        when(testCompositeEvent.getDates()).thenReturn(Arrays.asList(testEventDate));
        return testCompositeEvent;
    }

    @Test
    void renderNotification() throws Exception
    {
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(Mockito.mock(ScriptContext.class));

        CompositeEvent testCompositeEvent = this.mockEvent(false, false, false, false);
        SyndEntry resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("TranslatedEventTitle", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("Foo Bar", resultEntry.getAuthors().get(0).getName());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = this.mockEvent(false, true, false, false);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("TranslatedEventTitleNoDocument", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("Foo Bar", resultEntry.getAuthors().get(0).getName());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = this.mockEvent(true, true, false, false);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitleNoDocument", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("Foo Bar", resultEntry.getAuthors().get(0).getName());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = this.mockEvent(true, false, false, false);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitle", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("Foo Bar", resultEntry.getAuthors().get(0).getName());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = this.mockEvent(false, true, true, false);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitleNoDocument", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("Foo Bar", resultEntry.getAuthors().get(0).getName());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = this.mockEvent(false, false, true, false);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitle", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("Foo Bar", resultEntry.getAuthors().get(0).getName());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = this.mockEvent(true, true, true, false);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitleNoDocument", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("Foo Bar", resultEntry.getAuthors().get(0).getName());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = this.mockEvent(true, false, true, false);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("DefaultTitle", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("Foo Bar", resultEntry.getAuthors().get(0).getName());
        assertEquals("id1", resultEntry.getUri());

        testCompositeEvent = this.mockEvent(false, true, false, true);
        resultEntry = this.defaultNotificationRSSRenderer.renderNotification(testCompositeEvent);

        assertEquals("TranslatedEventTitleNoDocument", resultEntry.getTitle());
        assertEquals(1, resultEntry.getAuthors().size());
        assertEquals("Guest", resultEntry.getAuthors().get(0).getName());
        assertEquals("id1", resultEntry.getUri());
    }
}
