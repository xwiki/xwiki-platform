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
package org.xwiki.mentions.templates;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.icon.IconManagerScriptServiceComponentList;
import org.xwiki.localization.Translation;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.mentions.internal.MentionView;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.platform.date.script.DateScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.IconSetup;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XHTML10ComponentList;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.doc.XWikiDocument;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of the mention notifications template ({@code mention.vm}).
 * <p>
 * This test suite assert the html rendered by the mention notification template.
 *
 * @version $Id$
 * @since 14.0RC1
 * @since 13.10.2
 * @since 13.4.6
 */
@HTML50ComponentList
@XHTML10ComponentList
@IconManagerScriptServiceComponentList
@ComponentList({
    TemplateScriptService.class
})
class MentionPageTest extends PageTest
{
    /**
     * Needs to be mocked because don't want the test result to be dependent of the current time.
     */
    @Mock
    private DateScriptService dateScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        // Initializes then environment for the icon extension.
        IconSetup.setUp(this, "/icons.properties");
    }

    /**
     * Test the html returned by the mention notification template. For a functional test of the mention notification,
     * see the {@code MentionIT} docker test.
     * <p>
     * This test checks the rendering of a group of two mentions. The first mention occurs on a page with a title, the
     * second mention on a page without a title (in this case the name of the page must be displayed instead of its
     * title).
     */
    @Test
    void mentionNotificationTemplate() throws Exception
    {
        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        VelocityManager velocityManager = this.oldcore.getMocker().getInstance(VelocityManager.class);
        this.componentManager.registerComponent(ScriptService.class, "date", this.dateScriptService);

        DocumentReference page1 = new DocumentReference("xwiki", "XWiki", "Page1");
        DocumentReference page2 = new DocumentReference("xwiki", "YWiki", "Page2");
        DocumentReference userPage1 = new DocumentReference("xwiki", "XWiki", "U1");
        DocumentReference userPage2 = new DocumentReference("xwiki", "YWiki", "U2");
        Date eventDate = new Date();

        // Create and save page 1 with a title.
        XWikiDocument xWikiDocument1 = this.xwiki.getDocument(page1, this.context);
        xWikiDocument1.setTitle("Page 1 Title");
        this.xwiki.saveDocument(xWikiDocument1, this.context);
        // Create and save page 2 without a title.
        XWikiDocument xWikiDocument2 = this.xwiki.getDocument(page2, this.context);
        this.xwiki.saveDocument(xWikiDocument2, this.context);

        // Mocking of the date script service.
        when(this.dateScriptService.displayTimeAgo(eventDate)).thenReturn("one hundred years ago");

        LocalizationScriptService localizationScriptService =
            this.componentManager.getInstance(ScriptService.class, "localization");
        when(localizationScriptService.get(any())).thenReturn(mock(Translation.class));

        // Initialization of the velocity context.
        DefaultEvent event1 = new DefaultEvent();
        event1.setDate(eventDate);
        event1.setUser(userPage1);

        DefaultEvent event2 = new DefaultEvent();
        event2.setDate(eventDate);
        event2.setUser(userPage2);
        VelocityContext velocityContext = velocityManager.getVelocityContext();
        CompositeEvent value = new CompositeEvent(event1);
        value.getEvents().add(event2);
        velocityContext.put("compositeEvent", value);

        Map<Object, Object> compositeEventParams = new HashMap<>();
        compositeEventParams.put(event1, new MentionView()
            .setLocation("DOCUMENT")
            .setAuthorURL("/U1")
            .setDocumentURL("/page1")
            .setDocument(xWikiDocument1));
        compositeEventParams.put(event2, new MentionView()
            .setLocation("COMMENT")
            .setAuthorURL("/U2")
            .setDocument(xWikiDocument2)
            .setDocumentURL("/page2"));
        velocityContext.put("compositeEventParams", compositeEventParams);

        // Template rendering.
        String actual = templateManager.render("mentions/mention.vm");
        String expected = IOUtils.toString(getClass().getResourceAsStream("/templates/mentions/mention.html"),
            UTF_8);

        assertThat(actual, equalToCompressingWhiteSpace(expected));
        verify(localizationScriptService).get("mentions.event.mention.description.DOCUMENT");
        verify(localizationScriptService).get("mentions.event.mention.description.COMMENT");
    }

    /**
     * Test the html returned by the mention notification template. For a functional test of the mention notification,
     * see the {@code MentionIT} docker test.
     * <p>
     * This test checks the rendering of a single mention from another wiki.
     */
    @Test
    void mentionNotificationTemplateOtherWiki() throws Exception
    {
        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        VelocityManager velocityManager = this.oldcore.getMocker().getInstance(VelocityManager.class);
        this.componentManager.registerComponent(ScriptService.class, "date", this.dateScriptService);

        DocumentReference page1 = new DocumentReference("design", "XWiki", "Page1");
        DocumentReference userPage1 = new DocumentReference("xwiki", "XWiki", "U1");
        DocumentReference userPage2 = new DocumentReference("xwiki", "YWiki", "U2");
        Date eventDate = new Date();

        // Create and save page 1 with a title.
        XWikiDocument xWikiDocument1 = this.xwiki.getDocument(page1, this.context);
        xWikiDocument1.setTitle("Page 1 Title");
        this.xwiki.saveDocument(xWikiDocument1, this.context);

        // Mocking of the date script service.
        when(this.dateScriptService.displayTimeAgo(eventDate)).thenReturn("one hundred years ago");

        LocalizationScriptService localizationScriptService =
                this.componentManager.getInstance(ScriptService.class, "localization");
        when(localizationScriptService.get(any())).thenReturn(mock(Translation.class));

        // Initialization of the velocity context.
        DefaultEvent event1 = new DefaultEvent();
        event1.setDate(eventDate);
        event1.setUser(userPage1);
        event1.setDocument(page1);

        VelocityContext velocityContext = velocityManager.getVelocityContext();
        CompositeEvent value = new CompositeEvent(event1);
        velocityContext.put("compositeEvent", value);

        Map<Object, Object> compositeEventParams = new HashMap<>();
        compositeEventParams.put(event1, new MentionView()
                .setLocation("DOCUMENT")
                .setAuthorURL("/U1")
                .setDocumentURL("/page1")
                .setDocument(xWikiDocument1));
        velocityContext.put("compositeEventParams", compositeEventParams);
        velocityContext.put("xcontext", this.context);
        // Template rendering.
        Document render = Jsoup.parse(templateManager.render("mentions/mention.vm"));
        Element activitySummary = render.getElementsByClass("activity-summary").get(0);
        // Check that the activity summary contains the proper information about the wiki where the mention happened
        assertEquals("mentions.event.mention.summary.singular", activitySummary.ownText());
        assertEquals(1, activitySummary.childrenSize());
        Element activityWiki = activitySummary.child(0);
        assertEquals("text-muted", activityWiki.attr("class"));
        assertEquals("($services.wiki.getById($compositeEvent.document.wikiReference.name).prettyName)",
                activityWiki.text());
    }
}
