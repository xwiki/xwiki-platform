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
import org.xwiki.rendering.renderer.PrintRendererFactory;
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
import static org.mockito.Mockito.when;

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

    private WysiwygEditorScriptService wysiwygEditorScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        this.wysiwygEditorScriptService = mock(WysiwygEditorScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "wysiwyg", this.wysiwygEditorScriptService);

        // The page resolves the requested syntax the way core does, by looking for a renderer that produces it.
        PrintRendererFactory blockNoteRendererFactory = mock(PrintRendererFactory.class);
        when(blockNoteRendererFactory.getSyntax()).thenReturn(BLOCKNOTE);
        this.componentManager.registerComponent(PrintRendererFactory.class, BLOCKNOTE.toIdString(),
            blockNoteRendererFactory);
    }

    @Test
    void renderFormWhenNoFileNameIsProvided() throws Exception
    {
        this.request.put("xpage", "get");
        this.context.setAction("get");

        String result = renderPage(OFFICE_IMPORTER);

        // The page test framework mocks the localization service, so a key renders as itself.
        assertTrue(result.contains("wysiwyg.officeImporter.filePath.label"), result);
        assertTrue(result.contains("wysiwyg.officeImporter.filterStyles.label"), result);
        assertTrue(result.contains("wysiwyg.officeImporter.useOfficeViewer.label"), result);
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

        // Velocity renders an unresolved macro as literal text, so a macro name in the output means it is missing.
        assertFalse(result.contains("#initRequiredSkinExtensions"), result);
        assertFalse(result.contains("#sendRequiredSkinExtensions"), result);
        assertFalse(result.contains("#rawResponse"), result);

        // The page looks the office file up on the document the sheet applies to, which is the edited document.
        ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.captor();
        verify(this.wysiwygEditorScriptService)
            .importOfficeAttachment(eq(new AttachmentReference("my.doc", OFFICE_IMPORTER)), parameters.capture());
        assertEquals(BLOCKNOTE, parameters.getValue().get("outputSyntax"));
    }

    @Test
    void importWithOutputSyntaxWithoutVersion() throws Exception
    {
        this.request.put("fileName", "my.doc");
        this.request.put("outputSyntax", "blocknote");

        renderPage(OFFICE_IMPORTER);

        // Core accepts the syntax type on its own and takes the newest version of it, so this page does too.
        ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.captor();
        verify(this.wysiwygEditorScriptService)
            .importOfficeAttachment(any(AttachmentReference.class), parameters.capture());
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

        // An unknown syntax is the caller's mistake, so it gets a 400 instead of the blank 500 a failure returns.
        verify(response).sendError(eq(400), startsWith("Unknown output syntax [no-such-syntax/1.0]"));
        // Converting an office file is expensive and the result would be unusable, so the import must not run.
        verify(this.wysiwygEditorScriptService, never())
            .importOfficeAttachment(any(AttachmentReference.class), any(Map.class));
    }
}
