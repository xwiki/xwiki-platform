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
package org.xwiki.web;

import java.util.Date;
import java.util.List;

import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.script.EventStreamScriptService;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@code notification/rss/default.vm} template.
 *
 * @version $Id$
 */
@ComponentList({
    TemplateScriptService.class
})
@HTML50ComponentList
class NotificationRssDefaultPageTest extends PageTest
{
    private static final DocumentReference TEST_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Test", "WebHome");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");

    private static final String RSS_TEMPLATE = "notification/rss/default.vm";

    @Mock
    private EventStreamScriptService eventStreamScriptService;

    @Mock
    private LocalizationScriptService localizationScriptService;

    private TemplateManager templateManager;

    private ScriptContext scriptContext;

    @BeforeEach
    void setUp() throws Exception
    {
        this.scriptContext = this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class)
            .getCurrentScriptContext();
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "eventstream", this.eventStreamScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "localization", this.localizationScriptService);
    }

    @Test
    void htmlEscaping() throws Exception
    {
        XWikiDocument testDocument = new XWikiDocument(TEST_DOCUMENT_REFERENCE);
        testDocument.setTitle("Hello & Co");
        testDocument.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(testDocument, this.context);
        testDocument.setContent("Content");
        this.oldcore.getSpyXWiki().saveDocument(testDocument, this.context);

        // Mock the user's name.
        when(this.oldcore.getSpyXWiki().getPlainUserName(USER_REFERENCE, this.context)).thenReturn("First & Name");
        // Mock date formatting to avoid issues with timezones.
        Date testDate = mock(Date.class);
        when(this.oldcore.getSpyXWiki().formatDate(testDate, null, this.context)).thenReturn("<formattedDate>");

        // Mock the application name
        String eventType = "test&type";
        RecordableEventDescriptor recordableEventDescriptor = mock(RecordableEventDescriptor.class);
        when(this.eventStreamScriptService.getDescriptorForEventType(eventType, true))
            .thenReturn(recordableEventDescriptor);
        when(recordableEventDescriptor.getApplicationName()).thenReturn("eventType.translationKey");
        when(this.localizationScriptService.render("eventType.translationKey")).thenReturn("RSS Event Test&Type");
        when(this.localizationScriptService.render("notifications.events.by", Syntax.HTML_5_0, List.of("First & Name")))
            .thenReturn("Event by: [First &amp; Name]");

        String expectedDiffLink = "<a href='http://null:0/xwiki/bin/view/Test/?viewer=changes&#38;rev1=1.1&#38;"
            + "rev2=2.1'>2.1</a>";
        when(this.localizationScriptService.render("notifications.rss.seeChanges", List.of(expectedDiffLink)))
            .thenReturn(String.format("See changes: [%s]", expectedDiffLink));

        Event testEvent = new DefaultEvent();
        testEvent.setApplication("test&app");
        testEvent.setType("test&type");
        testEvent.setDate(testDate);
        testEvent.setUser(USER_REFERENCE);
        testEvent.setDocument(TEST_DOCUMENT_REFERENCE);
        testEvent.setDocumentVersion(testDocument.getVersion());
        this.scriptContext.setAttribute("event", new CompositeEvent(testEvent), ScriptContext.GLOBAL_SCOPE);
        String result = this.templateManager.render(RSS_TEMPLATE);
        assertEquals("<p>\n"
            + "  <strong>RSS Event Test&#38;Type</strong>\n"
            + "      <a href=\"http://null:0/xwiki/bin/view/Test/\">Hello &amp; Co</a>\n"
            + "    .<br/>\n"
            + "  Event by: [First &amp; Name]\n"
            + "</p>\n"
            + "<p>\n"
            + "  &#60;formattedDate&#62;\n"
            + "<br/>\n"
            + "  See changes: "
            + "[<a href='http://null:0/xwiki/bin/view/Test/?viewer=changes&#38;rev1=1.1&#38;rev2=2.1'>2.1</a>]\n"
            + "</p>", result.trim());
    }
}
