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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.wysiwyg.script.WysiwygEditorScriptService;

import com.xpn.xwiki.web.XWikiServletResponseStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Page test for {@code XWiki.WYSIWYG.OfficeImporter}.
 * <p>
 * The page writes the imported content straight to the response with {@code #rawResponse}, and the response stub has
 * no writer, so these tests check what the page decides rather than what it prints.
 *
 * @version $Id$
 */
@ComponentList({ ModelScriptService.class, LocalStringEntityReferenceSerializer.class })
@HTML50ComponentList
@XWikiSyntax21ComponentList
class OfficeImporterPageTest extends PageTest
{
    private static final DocumentReference OFFICE_IMPORTER =
        new DocumentReference("xwiki", List.of("XWiki", "WYSIWYG"), "OfficeImporter");

    private static final Syntax BLOCKNOTE = new Syntax(new SyntaxType("blocknote", "BlockNote"), "1.0");

    /**
     * Stands in for the rendering script service. The page needs one method from it, and declaring that method here
     * keeps this module's tests off the rendering implementation.
     */
    public static class RenderingScriptServiceStub implements ScriptService
    {
        /**
         * @param syntaxId the syntax identifier to resolve
         * @return the matching syntax, or {@code null} when no syntax matches
         */
        public Syntax resolveSyntax(String syntaxId)
        {
            return BLOCKNOTE.toIdString().equals(syntaxId) ? BLOCKNOTE : null;
        }
    }

    private WysiwygEditorScriptService wysiwygEditorScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        this.wysiwygEditorScriptService = mock(WysiwygEditorScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "wysiwyg", this.wysiwygEditorScriptService);
        this.componentManager.registerComponent(ScriptService.class, "rendering", new RenderingScriptServiceStub());
    }

    @Test
    void renderFormWhenNoFileNameIsProvided() throws Exception
    {
        this.request.put("xpage", "get");
        this.context.setAction("get");

        String result = renderPage(OFFICE_IMPORTER);

        // The page test framework mocks the localization service, so a key renders as itself. These assertions
        // check that the form uses the keys that moved to XWiki.WYSIWYG.Translations rather than the CKEditor ones.
        assertTrue(result.contains("wysiwyg.office.importer.filePath"), result);
        assertTrue(result.contains("wysiwyg.office.importer.filterStyles"), result);
        assertTrue(result.contains("wysiwyg.office.importer.useOfficeViewer"), result);
        assertTrue(result.contains("name=\"filePath\""), result);
    }

    @Test
    void importWithOutputSyntax() throws Exception
    {
        this.request.put("fileName", "my.doc");
        this.request.put("filterStyles", "true");
        this.request.put("outputSyntax", "blocknote");
        this.request.put("outputSyntaxVersion", "1.0");

        String result = renderPage(OFFICE_IMPORTER);

        // The skin extension macros come from display_macros.vm, which the page loads itself, rather than from the
        // CKEditor macros it no longer includes. Velocity renders an unresolved macro as literal text, so a macro
        // name in the output means its definition is missing.
        assertFalse(result.contains("#initRequiredSkinExtensions"), result);
        assertFalse(result.contains("#sendRequiredSkinExtensions"), result);
        assertFalse(result.contains("#rawResponse"), result);

        // The page resolves the syntax itself and hands the service a Syntax, not the identifier it was given. It
        // looks the office file up on the document the sheet is applied to, which is the edited document.
        ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.captor();
        verify(this.wysiwygEditorScriptService)
            .importOfficeAttachment(eq(new AttachmentReference("my.doc", OFFICE_IMPORTER)), parameters.capture());
        assertEquals(BLOCKNOTE, parameters.getValue().get("outputSyntax"));
    }

    @Test
    void importWithoutOutputSyntax() throws Exception
    {
        this.request.put("fileName", "my.doc");

        renderPage(OFFICE_IMPORTER);

        // No parameter means the importer picks its own default, so the page must not put one in the map.
        ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.captor();
        verify(this.wysiwygEditorScriptService)
            .importOfficeAttachment(any(AttachmentReference.class), parameters.capture());
        assertNull(parameters.getValue().get("outputSyntax"));
    }

    @Test
    void importWithUnknownOutputSyntaxFailsWithBadRequest() throws Exception
    {
        XWikiServletResponseStub response = spy(new XWikiServletResponseStub());
        this.context.setResponse(response);
        this.request.put("fileName", "my.doc");
        this.request.put("outputSyntax", "no-such-syntax");
        this.request.put("outputSyntaxVersion", "1.0");

        renderPage(OFFICE_IMPORTER);

        // A syntax identifier that resolves to nothing is the caller's mistake, so it gets a 400 naming the value
        // rather than the blank 500 that a failed import returns.
        verify(response).sendError(eq(400), startsWith("Unknown output syntax [no-such-syntax/1.0]"));
        // The import must not run at all: converting an office file is expensive and the result is unusable.
        verify(this.wysiwygEditorScriptService, never())
            .importOfficeAttachment(any(AttachmentReference.class), any(Map.class));
    }
}
