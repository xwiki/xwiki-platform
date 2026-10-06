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

import java.net.URI;
import java.net.URISyntaxException;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.url.script.URLSecurityScriptService;
import org.xwiki.xml.html.script.HTMLScriptService;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Test of template {@code extension.vm}.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TemplateScriptService.class
})
class ExtensionPageTest extends PageTest
{
    private static final String DEFAULT_BACK_URL = "/xwiki/bin/view/XWiki/Extensions?section=XWiki.Extensions";

    private XWikiDocument currentDocument;

    @Mock
    private HTMLScriptService htmlScriptService;

    @Mock
    private SecurityScriptService securityScriptService;

    @Mock
    private URLSecurityScriptService urlSecurityScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        this.componentManager.registerComponent(ScriptService.class, "html", this.htmlScriptService);
        this.componentManager.registerComponent(ScriptService.class, "security", this.securityScriptService);
        when(this.securityScriptService.get("url")).thenReturn(this.urlSecurityScriptService);
        // Valid URIs are considered safe by default, the scheme being checked by the HTML sanitizer.
        when(this.urlSecurityScriptService.parseToSafeURI(anyString()))
            .thenAnswer(invocation -> new URI(invocation.getArgument(0)));
        when(this.htmlScriptService.isAttributeSafe(eq("a"), eq("href"), anyString()))
            .thenAnswer(invocation -> !invocation.<String>getArgument(2).startsWith("javascript:"));

        this.currentDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Extensions"), this.context);
        this.context.setDoc(this.currentDocument);
        this.context.setAction("view");

        this.request.put("section", "XWiki.Extensions");
        this.request.put("extensionId", "org.xwiki.platform:xwiki-platform-web-templates");
        this.request.put("extensionVersion", "1.0");
    }

    @Test
    void backToTheRequestedURL() throws Exception
    {
        String backURL = "/xwiki/bin/view/Main/?a=b&c=d";
        this.request.put("xback", backURL);

        assertEquals(backURL, getBackURL());
    }

    @Test
    void backToTheCurrentPageByDefault() throws Exception
    {
        assertEquals(DEFAULT_BACK_URL, getBackURL());
    }

    @Test
    void backToTheCurrentPageWithEncodedParameters() throws Exception
    {
        this.request.put("search", "a b&c");

        assertEquals(DEFAULT_BACK_URL + "&search=a%20b%26c", getBackURL());
    }

    @Test
    void backToTheCurrentPageWhenTheRequestedURLIsUnsafe() throws Exception
    {
        this.request.put("xback", "javascript:alert(1)");

        assertEquals(DEFAULT_BACK_URL, getBackURL());
    }

    @Test
    void backToTheCurrentPageWhenTheRequestedURLCannotBeParsed() throws Exception
    {
        String backURL = "javascript:alert(1)//";
        this.request.put("xback", backURL);
        doThrow(new URISyntaxException(backURL, "Malformed URI")).when(this.urlSecurityScriptService)
            .parseToSafeURI(backURL);

        assertEquals(DEFAULT_BACK_URL, getBackURL());
    }

    @Test
    void backToTheCurrentPageWhenTheRequestedURLIsUntrusted() throws Exception
    {
        String backURL = "https://untrusted.example.org/";
        this.request.put("xback", backURL);
        doReturn(null).when(this.urlSecurityScriptService).parseToSafeURI(backURL);

        assertEquals(DEFAULT_BACK_URL, getBackURL());
    }

    @Test
    void backToTheCurrentPageWhenTheRequestedURLIsModifiedBySanitization() throws Exception
    {
        // Only the sanitized URL is known to be safe, and the requested URL could be interpreted differently by the
        // browser, so the requested URL is used only if it is accepted unchanged.
        String backURL = "/xwiki/bin/view/Main/?a=b c";
        this.request.put("xback", backURL);
        doReturn(new URI("/xwiki/bin/view/Main/?a=b%20c")).when(this.urlSecurityScriptService)
            .parseToSafeURI(backURL);

        assertEquals(DEFAULT_BACK_URL, getBackURL());
    }

    private String getBackURL() throws Exception
    {
        this.currentDocument.setContent("""
            {{velocity}}
            #template('extension.vm')
            #computeXBack()
            {{html}}#em_linkButton($xback 'extensions.actions.back' 'extension-link'){{/html}}
            {{/velocity}}""");
        this.currentDocument.setSyntax(Syntax.XWIKI_2_1);
        return Jsoup.parse(this.currentDocument.getRenderedContent(this.context)).selectFirst("a.extension-link")
            .attr("href");
    }
}
