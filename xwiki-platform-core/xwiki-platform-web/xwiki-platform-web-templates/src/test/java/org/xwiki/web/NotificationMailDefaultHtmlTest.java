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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.diff.display.UnifiedDiffElement;
import org.xwiki.diff.script.DiffDisplayerScriptService;
import org.xwiki.diff.script.DiffScriptService;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.script.EventStreamScriptService;
import org.xwiki.icon.IconManagerScriptService;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.platform.date.script.DateScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.user.DefaultUserComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@code notification/email/default.html.vm} template.
 *
 * @version $Id$
 */
@ComponentList({
    TemplateScriptService.class
})
@HTML50ComponentList
@DefaultUserComponentList
class NotificationMailDefaultHtmlTest extends PageTest
{
    private static final DocumentReference TEST_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Test", "WebHome");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");

    private static final String MAIL_NOTIF_TEMPLATE = "notification/email/default.html.vm";

    @Mock
    private EventStreamScriptService eventStreamScriptService;

    @Mock
    private LocalizationScriptService localizationScriptService;

    @Mock
    private DateScriptService dateScriptService;

    @Mock
    private IconManagerScriptService iconManagerScriptService;

    @Mock
    private DiffScriptService diffScriptService;

    @Mock
    private DiffDisplayerScriptService diffDisplayerScriptService;

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
        this.oldcore.getMocker().registerComponent(ScriptService.class, "date", this.dateScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "icon", this.iconManagerScriptService);

        this.oldcore.getMocker().registerComponent(ScriptService.class, "diff", this.diffScriptService);
        when(this.diffScriptService.get("display")).thenReturn(this.diffDisplayerScriptService);
    }

    @Test
    void htmlEscaping() throws Exception
    {
        XWikiDocument testDocument = new XWikiDocument(TEST_DOCUMENT_REFERENCE);
        testDocument.setTitle("Hello & Co");
        testDocument.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(testDocument, this.context);
        XWikiDocument clone = testDocument.clone();
        testDocument.setContent("Content");
        this.oldcore.getSpyXWiki().saveDocument(testDocument, this.context);
        testDocument.setOriginalDocument(clone);

        Event testEvent = new DefaultEvent();
        testEvent.setApplication("test&app");
        testEvent.setType("test&type");
        // Mock date formatting to avoid issues with timezones.
        Date testDate = new Date(12);
        when(this.dateScriptService.displayTimeAgo(testDate)).thenReturn("A few minutes ago");
        testEvent.setDate(testDate);
        testEvent.setUser(USER_REFERENCE);
        // Mock the user's name.
        when(this.oldcore.getSpyXWiki().getPlainUserName(USER_REFERENCE, this.context)).thenReturn("First & Name");
        testEvent.setDocument(TEST_DOCUMENT_REFERENCE);
        testEvent.setDocumentVersion(testDocument.getVersion());

        String eventType = "test&type";
        RecordableEventDescriptor recordableEventDescriptor = mock(RecordableEventDescriptor.class);
        when(this.eventStreamScriptService.getDescriptorForEventType(eventType, true))
            .thenReturn(recordableEventDescriptor);
        when(recordableEventDescriptor.getApplicationName()).thenReturn("eventType.translationKey");
        when(this.localizationScriptService.render("eventType.translationKey")).thenReturn("Event Test&Type");

        when(this.localizationScriptService.render(eq(List.of(
            "test&type.description.by.1user",
            "notifications.events.test&type.description.by.1user",
            "test&type.description",
            "notifications.events.test&type.description"
        )), any(Collection.class))).then(invocationOnMock -> {
            List<String> parameters = invocationOnMock.getArgument(1);
            assertEquals(1, parameters.size());

            return "User information: "+ parameters.get(0);
        });
        when(this.localizationScriptService.render("web.history.changes.summary.documentProperties"))
            .thenReturn("Document properties");
        when(this.localizationScriptService.render("web.history.changes.document.content"))
            .thenReturn("Content");

        when(this.iconManagerScriptService.renderHTML("file-text")).thenReturn("Icon file text");

        String expectedDiffLink = "<a href='http://null:0/xwiki/bin/view/Test/?viewer=changes&#38;rev1=1.1&#38;"
            + "rev2=2.1'>2.1</a>";
        when(this.localizationScriptService.render("notifications.rss.seeChanges", List.of(expectedDiffLink)))
            .thenReturn(String.format("See changes: [%s]", expectedDiffLink));

        when(this.diffDisplayerScriptService.unified(any(String.class), any(String.class), isNull()))
            .then(invocationOnMock -> {
            String previous = invocationOnMock.getArgument(0);
            assertEquals("", previous);

            String next = invocationOnMock.getArgument(1);
            assertEquals("Content", next);
            UnifiedDiffBlock<String, Character> result = new UnifiedDiffBlock<>();
            result.add(0, new UnifiedDiffElement<>(0, UnifiedDiffElement.Type.ADDED, "Content"));
            return List.of(result);
        });

        this.scriptContext.setAttribute("event", new CompositeEvent(testEvent), ScriptContext.GLOBAL_SCOPE);

        String result = this.templateManager.render(MAIL_NOTIF_TEMPLATE);
        String expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        "
            + "<td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "              <strong>Event Test&#38;Type</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                  <div>\n"
            // The hierarchy should go inside this div but it's not an easy one to mock.
            + "    <div style=\"background-color: #f5f5f5; color: #777777; padding: 4px 8px; border-radius: 7px; "
            + "font-size: 8px;\">\n"
            + "      \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                                                                                                                                                                        xwiki / Hello &#38; Co / Hello &#38; Co\n"
            + "      </div>\n"
            + "                  <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/Test/\">Hello &amp; Co</a>\n"
            + "  </div>\n"
            + "                        User information:                       "
            + "<img src=\"cid:User.jpg\" alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User\">First & Name</a>\n"
            + "      \n"
            + "      <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      A few minutes ago\n"
            + "    </small>\n"
            + "  </div>\n"
            // No details as there's a single event
            + "                              \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                            \n"
            + "                                                                                            "
            + "<div style=\"border-top: 1px dashed #e8e8e8; font-size: 0.9em;\">\n"
            + "                                <dl class=\"diff-group\">\n"
            + "            <dt id=\"diff-329847360\"   data-reference=\"DOCUMENT:\"\n"
            + ">\n"
            + "          <span class=\"diff-icon diff-icon-change\" title=\"change\">\n"
            + "    Icon file text\n"
            + "  </span>\n"
            + "      Document properties\n"
            + "    </dt>\n"
            + "    <dd style=\"margin-left: 0\">\n"
            + "      <dl>\n"
            + "                                    <dt style=\"border: 1px solid #E8E8E8; "
            + "border-left: 5px solid #E8E8E8; color: #656565; padding: .5em .2em;\" data-property-name=\"content\">\n"
            + "    Content\n"
            + "      </dt>\n"
            + "            <dd style=\"margin-left: 0\">  <div style=\"border: 1px solid #E8E8E8; "
            + "font-family: Monospace; overflow: auto;\">\n"
            + "    <table>\n"
            + "          <tr >\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "                <td class=\"diff-line \" style=\"background-color: #eee; "
            + "color: rgba(101, 101, 101, 0.5); font-family: Monospace; padding: .4em .5em;\">@@ -1,0 +1,1 @@</td>\n"
            + "      </tr>\n"
            + "              <tr >\n"
            + "                    <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\"></td>\n"
            + "          <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">1</td>\n"
            + "          <td style=\"background-color: #ccffcc;\">+Content</td>\n"
            + "        </tr>\n"
            + "                    </table>\n"
            + "  </div>\n"
            + "</dd>\n"
            + "          </dl>\n"
            + "    </dd>\n"
            + "  </dl>\n"
            + "            </div>\n"
            + "        \n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";

        assertEquals(expectedResult, result.trim());
    }

    @Test
    void htmlEscapingMissingPrevious() throws Exception
    {
        XWikiDocument testDocument = new XWikiDocument(TEST_DOCUMENT_REFERENCE);
        testDocument.setTitle("Hello & Co");
        testDocument.setSyntax(Syntax.XWIKI_2_1);
        testDocument.setContent("Content");
        this.oldcore.getSpyXWiki().saveDocument(testDocument, this.context);

        Event testEvent = new DefaultEvent();
        testEvent.setApplication("test&app");
        testEvent.setType("test&type");
        // Mock date formatting to avoid issues with timezones.
        Date testDate = new Date(12);
        when(this.dateScriptService.displayTimeAgo(testDate)).thenReturn("A few minutes ago");
        testEvent.setDate(testDate);
        testEvent.setUser(USER_REFERENCE);
        // Mock the user's name.
        when(this.oldcore.getSpyXWiki().getPlainUserName(USER_REFERENCE, this.context)).thenReturn("First & Name");
        testEvent.setDocument(TEST_DOCUMENT_REFERENCE);
        testEvent.setDocumentVersion(testDocument.getVersion());

        String eventType = "test&type";
        RecordableEventDescriptor recordableEventDescriptor = mock(RecordableEventDescriptor.class);
        when(this.eventStreamScriptService.getDescriptorForEventType(eventType, true))
            .thenReturn(recordableEventDescriptor);
        when(recordableEventDescriptor.getApplicationName()).thenReturn("eventType.translationKey");
        when(this.localizationScriptService.render("eventType.translationKey")).thenReturn("Event Test&Type");

        when(this.localizationScriptService.render(eq(List.of(
            "test&type.description.by.1user",
            "notifications.events.test&type.description.by.1user",
            "test&type.description",
            "notifications.events.test&type.description"
        )), any(Collection.class))).then(invocationOnMock -> {
            List<String> parameters = invocationOnMock.getArgument(1);
            assertEquals(1, parameters.size());

            return "User information: "+ parameters.get(0);
        });
        when(this.localizationScriptService.render("web.history.changes.summary.documentProperties"))
            .thenReturn("Document properties");
        when(this.localizationScriptService.render("web.history.changes.document.content"))
            .thenReturn("Content");
        when(this.localizationScriptService.render("core.viewers.diff.metadata.title"))
            .thenReturn("Title");

        when(this.iconManagerScriptService.renderHTML("file-text")).thenReturn("Icon file text");

        String expectedDiffLink = "<a href='http://null:0/xwiki/bin/view/Test/?viewer=changes&#38;rev1=1.1&#38;"
            + "rev2=2.1'>2.1</a>";
        when(this.localizationScriptService.render("notifications.rss.seeChanges", List.of(expectedDiffLink)))
            .thenReturn(String.format("See changes: [%s]", expectedDiffLink));

        when(this.diffDisplayerScriptService.unified(any(String.class), any(String.class), isNull()))
            .then(invocationOnMock -> {
                String previous = invocationOnMock.getArgument(0);
                assertEquals("", previous);

                String next = invocationOnMock.getArgument(1);
                assertEquals("Hello & Co", next);
                UnifiedDiffBlock<String, Character> result = new UnifiedDiffBlock<>();
                result.add(0, new UnifiedDiffElement<>(0, UnifiedDiffElement.Type.ADDED, "Hello & Co"));
                return List.of(result);
            }).then(
                invocationOnMock -> {
                    String previous = invocationOnMock.getArgument(0);
                    assertEquals("", previous);

                    String next = invocationOnMock.getArgument(1);
                    assertEquals("Content", next);
                    UnifiedDiffBlock<String, Character> result = new UnifiedDiffBlock<>();
                    result.add(0, new UnifiedDiffElement<>(0, UnifiedDiffElement.Type.ADDED, "Content"));
                    return List.of(result);
                }
            );

        this.scriptContext.setAttribute("event", new CompositeEvent(testEvent), ScriptContext.GLOBAL_SCOPE);

        String result = this.templateManager.render(MAIL_NOTIF_TEMPLATE);
        String expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        <td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "              <strong>Event Test&#38;Type</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                  <div>\n"
            + "    <div style=\"background-color: #f5f5f5; color: #777777; padding: 4px 8px; border-radius: 7px; "
            + "font-size: 8px;\">\n"
            + "      \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                                                                                                                                                                        xwiki / Hello &#38; Co / Hello &#38; Co\n"
            + "      </div>\n"
            + "                  <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/Test/\">Hello &amp; Co</a>\n"
            + "  </div>\n"
            + "                        User information:                       "
            + "<img src=\"cid:User.jpg\" alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User\">First & Name</a>\n"
            + "      \n"
            + "      <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      A few minutes ago\n"
            + "    </small>\n"
            + "  </div>\n"
            + "                              \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                              \n"
            + "                                                                                          "
            + "<div style=\"border-top: 1px dashed #e8e8e8; font-size: 0.9em;\">\n"
            + "                                <dl class=\"diff-group\">\n"
            + "                <dt id=\"diff-329847360\"   data-reference=\"DOCUMENT:\"\n"
            + ">\n"
            + "          <span class=\"diff-icon diff-icon-change\" title=\"change\">\n"
            + "    Icon file text\n"
            + "  </span>\n"
            + "      Document properties\n"
            + "    </dt>\n"
            + "    <dd style=\"margin-left: 0\">\n"
            + "      <dl>\n"
            + "                                <dt style=\"border: 1px solid #E8E8E8; border-left: 5px solid #E8E8E8; "
            + "color: #656565; padding: .5em .2em;\" data-property-name=\"title\">\n"
            + "    Title\n"
            + "      </dt>\n"
            + "            <dd style=\"margin-left: 0\">  <div style=\"border: 1px solid #E8E8E8; "
            + "font-family: Monospace; overflow: auto;\">\n"
            + "    <table>\n"
            + "          <tr >\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "                <td class=\"diff-line \" style=\"background-color: #eee; "
            + "color: rgba(101, 101, 101, 0.5); font-family: Monospace; padding: .4em .5em;\">@@ -1,0 +1,1 @@</td>\n"
            + "      </tr>\n"
            + "              <tr >\n"
            + "                    <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\"></td>\n"
            + "          <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">1</td>\n"
            + "          <td style=\"background-color: #ccffcc;\">+Hello &#38; Co</td>\n"
            + "        </tr>\n"
            + "                    </table>\n"
            + "  </div>\n"
            + "</dd>\n"
            + "                                      <dt style=\"border: 1px solid #E8E8E8; "
            + "border-left: 5px solid #E8E8E8; color: #656565; padding: .5em .2em;\" data-property-name=\"content\">\n"
            + "    Content\n"
            + "      </dt>\n"
            + "            <dd style=\"margin-left: 0\">  <div style=\"border: 1px solid #E8E8E8; "
            + "font-family: Monospace; overflow: auto;\">\n"
            + "    <table>\n"
            + "          <tr >\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "                <td class=\"diff-line \" style=\"background-color: #eee; "
            + "color: rgba(101, 101, 101, 0.5); font-family: Monospace; padding: .4em .5em;\">@@ -1,0 +1,1 @@</td>\n"
            + "      </tr>\n"
            + "              <tr >\n"
            + "                    <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\"></td>\n"
            + "          <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">1</td>\n"
            + "          <td style=\"background-color: #ccffcc;\">+Content</td>\n"
            + "        </tr>\n"
            + "                    </table>\n"
            + "  </div>\n"
            + "</dd>\n"
            + "          </dl>\n"
            + "    </dd>\n"
            + "  </dl>\n"
            + "            </div>\n"
            + "        \n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";

        assertEquals(expectedResult, result.trim());
    }
}
