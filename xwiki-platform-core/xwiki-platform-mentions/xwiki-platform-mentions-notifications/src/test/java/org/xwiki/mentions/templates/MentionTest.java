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
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.icon.IconManagerScriptService;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.mentions.internal.MentionView;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.platform.date.script.DateScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XHTML10ComponentList;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.tools.EscapeTool;

import com.xpn.xwiki.doc.XWikiDocument;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;
import static org.mockito.Mockito.when;

/**
 * Test of {@code mention.vm}.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@HTML50ComponentList
@XHTML10ComponentList
class MentionTest extends PageTest
{
    @Mock
    private IconManagerScriptService iconManager;

    @Mock
    private DateScriptService dateScriptService;

    @Mock
    private LocalizationScriptService localizationScriptService;

    @Test
    void mention() throws Exception
    {
        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        VelocityManager velocityManager = this.oldcore.getMocker().getInstance(VelocityManager.class);
        this.componentManager.registerComponent(ScriptService.class, "icon", this.iconManager);
        this.componentManager.registerComponent(ScriptService.class, "date", this.dateScriptService);
        this.componentManager.registerComponent(ScriptService.class, "localization", this.localizationScriptService);

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

        // Registration of usefull velocity tools.
        registerVelocityTool("escapetool", new EscapeTool());
        registerVelocityTool("stringtool", new StringUtils());

        // Mocking of the script services.
        when(this.iconManager.renderHTML("bell")).thenReturn("bell-icon");
        when(this.iconManager.renderHTML("page")).thenReturn("page-icon");
        when(this.dateScriptService.displayTimeAgo(eventDate)).thenReturn("one hundred years ago");
        when(this.localizationScriptService.render("mentions.event.mention.summary.plural", singletonList(2)))
            .thenReturn("You have received 2 mentions.");
        when(this.localizationScriptService.render("mentions.event.mention.description"))
            .thenReturn("mentioned you on page");
        when(this.localizationScriptService.render("mentions.event.mention.type")).thenReturn("Mentions");

        // Initialization of the velocity context
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
            .setAuthorURL("/U1")
            .setDocumentURL("/page1")
            .setDocument(xWikiDocument1));
        compositeEventParams.put(event2, new MentionView()
            .setAuthorURL("/U2")
            .setDocument(xWikiDocument2)
            .setDocumentURL("/page2"));
        velocityContext.put("compositeEventParams", compositeEventParams);

        // Template rendering.
        String actual = templateManager.render("templates/mentions/mention.vm");
        String expected = IOUtils.toString(getClass().getResourceAsStream("/templates/mentions/mention.mention.html"),
            UTF_8);

        assertThat(actual, equalToCompressingWhiteSpace(expected));
    }
}
