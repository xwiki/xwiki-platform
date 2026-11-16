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

import javax.inject.Named;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.url.script.URLSecurityScriptService;
import org.xwiki.xml.html.script.HTMLScriptService;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.StoreConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of template {@code deletespace.vm}.
 *
 * @version $Id$
 */
@ComponentList(StoreConfiguration.class)
class DeletespacePageTest extends PageTest
{
    private static final String TEMPLATE_NAME = "deletespace.vm";

    private static final String NO_BUTTON = ".xwikimessage .buttons .buttonwrapper:last-child a";

    @MockComponent(classToMock = HTMLScriptService.class)
    @Named("html")
    private ScriptService htmlScriptService;

    @MockComponent(classToMock = SecurityScriptService.class)
    @Named("security")
    private ScriptService securityScriptService;

    private TemplateManager templateManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);

        // Accept any URL: the sanitization itself is tested with the macro that performs it.
        URLSecurityScriptService urlSecurityScriptService = mock();
        when(((SecurityScriptService) this.securityScriptService).get("url")).thenReturn(urlSecurityScriptService);
        when(urlSecurityScriptService.parseToSafeURI(anyString()))
            .thenAnswer(invocation -> new URI(invocation.getArgument(0)));
        when(((HTMLScriptService) this.htmlScriptService).isAttributeSafe(eq("a"), eq("href"), any()))
            .thenReturn(true);

        XWikiDocument currentDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "WebHome"), this.context);
        this.xwiki.saveDocument(currentDocument, this.context);
        this.context.setDoc(currentDocument);
    }

    @Test
    void cancelGoesBackToTheRedirectURL() throws Exception
    {
        String redirectURL = "/xwiki/bin/view/Main/WebHome?a=1&b=2#section";
        this.request.put("xredirect", redirectURL);

        Document document = Jsoup.parse(this.templateManager.render(TEMPLATE_NAME));

        assertEquals(redirectURL, document.selectFirst(NO_BUTTON).attr("href"));
    }

    @Test
    void cancelGoesBackToTheSpaceWithoutRedirectURL() throws Exception
    {
        Document document = Jsoup.parse(this.templateManager.render(TEMPLATE_NAME));

        assertEquals("/xwiki/bin/view/Space/", document.selectFirst(NO_BUTTON).attr("href"));
    }
}
