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
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.wysiwyg.script.WysiwygEditorScriptService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Page test for {@code XWiki.WYSIWYG.OfficeImporter}.
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

    private WysiwygEditorScriptService wysiwygEditorScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        this.wysiwygEditorScriptService = mock(WysiwygEditorScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "wysiwyg", this.wysiwygEditorScriptService);
    }

    @Test
    void renderFormWhenNoFileNameIsProvided() throws Exception
    {
        this.request.put("xpage", "get");
        this.context.setAction("get");

        String result = renderPage(OFFICE_IMPORTER);

        // The localization service is mocked by the page test framework, so a key renders as itself. This checks that
        // the form uses the keys that moved to XWiki.WYSIWYG.Translations, rather than the CKEditor ones.
        assertTrue(result.contains("wysiwyg.office.importer.filePath"), result);
        assertTrue(result.contains("wysiwyg.office.importer.filterStyles"), result);
        assertTrue(result.contains("wysiwyg.office.importer.useOfficeViewer"), result);
        assertTrue(result.contains("name=\"filePath\""), result);
    }

    @Test
    void importWithTargetSyntax() throws Exception
    {
        this.request.put("fileName", "my.doc");
        this.request.put("filterStyles", "true");
        this.request.put("targetSyntax", "blocknote/1.0");
        when(this.wysiwygEditorScriptService.importOfficeAttachment(any(AttachmentReference.class), any(Map.class),
            eq("blocknote/1.0"))).thenReturn("imported-as-blocknote");

        String result = renderPage(OFFICE_IMPORTER);

        // The converted content is what the page returns to the editor that asked for the import.
        assertTrue(result.contains("imported-as-blocknote"), result);
        // The skin extension macros come from display_macros.vm, which the page loads itself, rather than from the
        // CKEditor macros it no longer includes. Velocity renders an unresolved macro as literal text, so their names
        // appearing in the output is what a missing definition looks like.
        assertFalse(result.contains("#initRequiredSkinExtensions"), result);
        assertFalse(result.contains("#sendRequiredSkinExtensions"), result);
        // The office file is looked up on the document the sheet is applied on, which is the edited document when
        // the editor requests this page through the edited document's URL.
        verify(this.wysiwygEditorScriptService).importOfficeAttachment(
            eq(new AttachmentReference("my.doc", OFFICE_IMPORTER)), any(Map.class), eq("blocknote/1.0"));
    }

    @Test
    void importWithoutTargetSyntax() throws Exception
    {
        this.request.put("fileName", "my.doc");
        // outputSyntax selects the syntax this page is rendered in, so it must not be mistaken for the syntax the
        // office file is converted to.
        this.request.put("outputSyntax", "plain");

        renderPage(OFFICE_IMPORTER);

        // An unset target syntax reaches the script service as blank, which falls back to annotated XHTML there.
        verify(this.wysiwygEditorScriptService).importOfficeAttachment(any(AttachmentReference.class), any(Map.class),
            eq(""));
    }
}
