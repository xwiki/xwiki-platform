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
package org.xwiki.like.templates;

import java.util.Date;

import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@code notification/rss/org.xwiki.like.events.LikeRecordableEvent.vm} template.
 *
 * @version $Id$
 */
@ComponentList({
    TemplateScriptService.class
})
@HTML50ComponentList
class RssLikeRecordableEventPageTest extends PageTest
{
    private static final DocumentReference TEST_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Test", "WebHome");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");

    private static final String RSS_TEMPLATE = "notification/rss/org.xwiki.like.events.LikeRecordableEvent.vm";

    private TemplateManager templateManager;

    private ScriptContext scriptContext;

    @BeforeEach
    void setUp() throws Exception
    {
        this.scriptContext = this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class)
            .getCurrentScriptContext();
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
    }

    @Test
    void htmlEscaping() throws Exception
    {
        // Create two versions of a document.
        XWikiDocument testDocument = new XWikiDocument(TEST_DOCUMENT_REFERENCE);
        testDocument.setTitle("Hello & Co");
        testDocument.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(testDocument, this.context);

        // Mock the user's name.
        when(this.oldcore.getSpyXWiki().getPlainUserName(USER_REFERENCE, this.context)).thenReturn("First & Name");

        Event testEvent = new DefaultEvent();
        testEvent.setDate(mock(Date.class));
        testEvent.setUser(USER_REFERENCE);
        testEvent.setDocument(TEST_DOCUMENT_REFERENCE);
        this.scriptContext.setAttribute("event", new CompositeEvent(testEvent), ScriptContext.GLOBAL_SCOPE);
        String result = this.templateManager.render(RSS_TEMPLATE);
        assertThat(result.trim(), equalToCompressingWhiteSpace(
            "<p>\n"
                + "  <strong>like.application.name</strong>\n"
                + "    <a href=\"http://null:0/xwiki/bin/view/Test/\">Hello &amp; Co</a>\n"
                + "  .<br/>\n"
                + "    notifications.events.org.xwiki.like.events.LikeRecordableEvent.description.by.1user"
                + " [First &amp; Name]\n"
                + "</p>\n"
                + "<p>\n"
                + "  1970/01/01 01:00\n"
                + "</p>"));
    }
}
