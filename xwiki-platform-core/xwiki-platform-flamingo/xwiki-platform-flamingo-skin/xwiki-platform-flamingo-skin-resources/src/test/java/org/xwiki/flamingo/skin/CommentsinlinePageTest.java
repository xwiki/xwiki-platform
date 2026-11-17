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
package org.xwiki.flamingo.skin;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.container.Container;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.url.internal.DefaultURLConfiguration;
import org.xwiki.url.internal.DefaultURLSecurityManager;
import org.xwiki.url.script.URLSecurityScriptService;
import org.xwiki.xml.html.script.HTMLScriptService;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.test.LogLevel.INFO;

/**
 * Test the {@code commentsinline.vm} template of the Flamingo skin.
 *
 * @version $Id$
 */
@ComponentList({
    ModelScriptService.class,
    // URL Security script service start
    URLSecurityScriptService.class,
    DefaultURLSecurityManager.class,
    DefaultURLConfiguration.class
    // URL Security script service end
})
@SecurityScriptServiceComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
class CommentsinlinePageTest extends PageTest
{
    private static final Path TEMPLATE = Path.of("src/main/resources/flamingo/commentsinline.vm");

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    private static final DocumentReference COMMENTS_CLASS = new DocumentReference("xwiki", "XWiki", "XWikiComments");

    // Required by DefaultURLSecurityManager, but only used to resolve the current domain when checking an URL with
    // an authority against the trusted domains, and the tested redirects are all relative.
    @MockComponent
    private Container container;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(INFO, URLSecurityScriptService.class.getName());

    private TemplateManager templateManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);

        // The reply button is only displayed to users allowed to comment, checked with $xwiki.hasAccessLevel.
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("comment"), any(), any(), any())).thenReturn(true);

        // The #getSanitizedURLAttributeValue macro checks the (already sanitized) URL with $services.html, backed by
        // the HTML sanitizer whose components are heavy to wire: accept any href, URL safety being covered by the
        // real URL security components registered on this test.
        HTMLScriptService htmlScriptService = mock(HTMLScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "html", htmlScriptService);
        when(htmlScriptService.isAttributeSafe(eq("a"), eq("href"), anyString())).thenReturn(true);

        // The reply button is displayed for each comment of the current document.
        XWikiDocument document = this.xwiki.getDocument(DOCUMENT_REFERENCE, this.context).clone();
        BaseObject comment = document.newXObject(COMMENTS_CLASS, this.context);
        comment.setStringValue("comment", "Comment");
        // A guest comment, so that the template doesn't need to resolve the comment author.
        comment.setStringValue("author", "");
        comment.setDateValue("date", new Date());
        this.xwiki.saveDocument(document, this.context);
        this.context.setDoc(this.xwiki.getDocument(DOCUMENT_REFERENCE, this.context));

        // The template redirects to the view action when rendered for another action.
        this.context.setAction("view");
    }

    @Test
    void replyButtonRejectsJavascriptXRedirect() throws Exception
    {
        this.stubRequest.put("xredirect", "javascript:alert(1)//");

        assertEquals("/xwiki/bin/view/Space/Page?&replyto=0#xwikicomment_0", renderReplyButton().attr("href"));
        assertEquals("The URI [javascript:alert(1)//] is considered not safe: "
            + "[The given URI [javascript:alert(1)//] is not safe on this server.]", this.logCapture.getMessage(0));
    }

    @Test
    void replyButtonFallsBackWhenXRedirectIsMissing() throws Exception
    {
        assertEquals("/xwiki/bin/view/Space/Page?&replyto=0#xwikicomment_0", renderReplyButton().attr("href"));
    }

    @Test
    void replyButtonKeepsXRedirect() throws Exception
    {
        this.stubRequest.put("xredirect", "/xwiki/bin/view/Sandbox/WebHome?viewer=comments");

        assertEquals("/xwiki/bin/view/Sandbox/WebHome?viewer=comments&replyto=0#xwikicomment_0",
            renderReplyButton().attr("href"));
    }

    private Element renderReplyButton() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.templateManager.render(this.templateManager.createStringTemplate("commentsinline.vm",
            Files.readString(TEMPLATE, StandardCharsets.UTF_8), (DocumentReference) null, (DocumentReference) null),
            writer);
        return Jsoup.parse(writer.toString()).selectFirst("a.commentreply");
    }
}
