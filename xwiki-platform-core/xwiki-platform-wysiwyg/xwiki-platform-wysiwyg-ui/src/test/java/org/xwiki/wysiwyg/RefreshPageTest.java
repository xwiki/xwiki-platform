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
package org.xwiki.wysiwyg;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.script.RenderingScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.page.PageTest;
import org.xwiki.wysiwyg.script.WysiwygEditorScriptService;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@code wysiwyg/refresh.vm} template.
 *
 * @version $Id$
 */
class RefreshPageTest extends PageTest
{
    private static final String TEMPLATE = "wysiwyg/refresh.vm";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "Space", "Page");

    private static final Syntax EDITOR_SYNTAX = new Syntax(new SyntaxType("blocknote", "BlockNote"), "1.0");

    private static final String TOKEN = "token";

    private TemplateManager templateManager;

    private WysiwygEditorScriptService wysiwygScriptService;

    private XWikiRequest request;

    private XWikiResponse response;

    private final StringWriter responseBody = new StringWriter();

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.componentManager.getInstance(TemplateManager.class);

        this.wysiwygScriptService = this.componentManager.registerMockComponent(ScriptService.class, "wysiwyg",
            WysiwygEditorScriptService.class, false);
        CSRFTokenScriptService csrfScriptService = this.componentManager.registerMockComponent(ScriptService.class,
            "csrf", CSRFTokenScriptService.class, false);
        when(csrfScriptService.isTokenValid(TOKEN)).thenReturn(true);
        RenderingScriptService renderingScriptService = this.componentManager.registerMockComponent(
            ScriptService.class, "rendering", RenderingScriptService.class, false);
        when(renderingScriptService.resolveSyntax("blocknote/1.0")).thenReturn(EDITOR_SYNTAX);
        when(renderingScriptService.resolveSyntax("xwiki/2.0")).thenReturn(Syntax.XWIKI_2_0);

        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        document.setSyntax(Syntax.XWIKI_2_1);
        this.context.setDoc(document);

        this.request = mock();
        when(this.request.getMethod()).thenReturn("POST");
        when(this.request.get("form_token")).thenReturn(TOKEN);
        when(this.request.get("editorSyntax")).thenReturn("blocknote/1.0");
        when(this.request.get("content")).thenReturn("edited");
        this.context.setRequest(this.request);

        this.response = mock();
        when(this.response.getWriter()).thenReturn(new PrintWriter(this.responseBody));
        this.context.setResponse(this.response);
    }

    @Test
    void refresh() throws Exception
    {
        when(this.request.get("sourceSyntax")).thenReturn("xwiki/2.0");
        when(this.request.get("restricted")).thenReturn("true");
        when(this.wysiwygScriptService.refresh("edited", EDITOR_SYNTAX, Syntax.XWIKI_2_0, DOCUMENT_REFERENCE,
            true)).thenReturn("refreshed");

        this.templateManager.render(TEMPLATE);

        assertEquals("refreshed", this.responseBody.toString());
        verify(this.response).setContentType("text/plain");
        verify(this.response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void refreshWithTheDocumentSyntax() throws Exception
    {
        when(this.wysiwygScriptService.refresh("edited", EDITOR_SYNTAX, Syntax.XWIKI_2_1, DOCUMENT_REFERENCE, false))
            .thenReturn("refreshed");

        this.templateManager.render(TEMPLATE);

        assertEquals("refreshed", this.responseBody.toString());
    }

    @Test
    void refreshToEmptyContent() throws Exception
    {
        // Empty content is a valid result for some editor syntaxes.
        when(this.wysiwygScriptService.refresh("edited", EDITOR_SYNTAX, Syntax.XWIKI_2_1, DOCUMENT_REFERENCE, false))
            .thenReturn("");

        this.templateManager.render(TEMPLATE);

        assertEquals("", this.responseBody.toString());
        verify(this.response).setContentType("text/plain");
        verify(this.response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void refreshWithGET() throws Exception
    {
        when(this.request.getMethod()).thenReturn("GET");

        this.templateManager.render(TEMPLATE);

        verify(this.response).sendError(403, "A valid form token is required.");
        verifyNoInteractions(this.wysiwygScriptService);
    }

    @Test
    void refreshWithoutValidFormToken() throws Exception
    {
        when(this.request.get("form_token")).thenReturn("invalid");

        this.templateManager.render(TEMPLATE);

        verify(this.response).sendError(403, "A valid form token is required.");
        verifyNoInteractions(this.wysiwygScriptService);
    }

    @Test
    void refreshWithoutEditorSyntax() throws Exception
    {
        when(this.request.get("editorSyntax")).thenReturn(null);

        this.templateManager.render(TEMPLATE);

        verify(this.response).sendError(400, "The editor syntax is missing or not valid.");
        verifyNoInteractions(this.wysiwygScriptService);
    }

    @Test
    void refreshFailure() throws Exception
    {
        when(this.wysiwygScriptService.refresh("edited", EDITOR_SYNTAX, Syntax.XWIKI_2_1, DOCUMENT_REFERENCE, false))
            .thenThrow(new Exception("Failed to execute the macros."));

        this.templateManager.render(TEMPLATE);

        verify(this.response).sendError(500, "Failed to execute the macro calls of the edited content.");
        assertEquals("", this.responseBody.toString());
    }
}
