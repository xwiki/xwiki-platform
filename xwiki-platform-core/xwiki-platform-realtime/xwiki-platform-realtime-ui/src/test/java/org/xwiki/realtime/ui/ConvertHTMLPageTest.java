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
package org.xwiki.realtime.ui;

import javax.inject.Provider;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiRightService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
class ConvertHTMLPageTest extends PageTest
{

    private static final String WIKI_NAME = "xwiki";

    private static final String XWIKI_SPACE = "RTFrontend";

    private static final DocumentReference RTF_FRONTEND_CONVERT_HTML =
        new DocumentReference(WIKI_NAME, XWIKI_SPACE, "ConvertHTML");

    private static final String CSRF_TOKEN = "a0a0a0a0";

    private CSRFTokenScriptService tokenService;

    @BeforeEach
    void setUp() throws Exception
    {
        // Mock the Token Service to get a consistent CSRF token throughout the tests.
        this.tokenService = this.oldcore.getMocker().registerMockComponent(ScriptService.class, "csrf",
            CSRFTokenScriptService.class, true);
        when(this.tokenService.isTokenValid(CSRF_TOKEN)).thenReturn(true);

        this.xwiki.initializeMandatoryDocuments(this.context);

        this.context = mock(XWikiContext.class);
        Provider<XWikiContext> xcontextProvider =
            this.componentManager.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getRequest()).thenReturn(this.request);
        when(this.context.getResponse()).thenReturn(this.response);
        when(this.context.getWiki()).thenReturn(this.xwiki);

        // Fake programming access level to display the complete page.
        XWikiRightService rightService = this.oldcore.getMockRightService();
        when(this.xwiki.getRightService()).thenReturn(rightService);
        when(rightService.hasProgrammingRights(this.context)).thenReturn(true);
        this.response = spy(this.response);
        when(this.context.getResponse()).thenReturn(this.response);
    }

    @Test
    void checkValidCSRFToken() throws Exception
    {
        when(this.context.getAction()).thenReturn("get");
        this.request.put("text", "Hello");
        this.request.put("form_token", CSRF_TOKEN);
        Document result = renderHTMLPage(RTF_FRONTEND_CONVERT_HTML);

        verify(this.response, never()).setStatus(anyInt());
        verify(this.tokenService).isTokenValid(CSRF_TOKEN);
        assertEquals("$xwiki.getDocument('CKEditor.ContentSheet').getRenderedContent()",
            result.getElementsByTag("body").text());
    }

    @Test
    void checkInvalidCSRFToken() throws Exception
    {
        String wrongToken = "wrong_token";
        when(this.context.getAction()).thenReturn("get");
        this.request.put("text", "Hello");
        this.request.put("form_token", wrongToken);
        Document result = renderHTMLPage(RTF_FRONTEND_CONVERT_HTML);

        verify(this.response).sendError(403, "rtfFrontend.convertHtml.invalidCsrfToken");
        verify(this.tokenService).isTokenValid(wrongToken);
        assertEquals("", result.getElementsByTag("body").text());
    }
}
