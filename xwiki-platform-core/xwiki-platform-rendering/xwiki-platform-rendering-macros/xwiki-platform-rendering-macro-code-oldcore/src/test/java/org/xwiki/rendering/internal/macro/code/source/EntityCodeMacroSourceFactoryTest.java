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
package org.xwiki.rendering.internal.macro.code.source;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.internal.model.reference.CurrentPageReferenceDocumentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.macro.CurrentMacroEntityReferenceResolver;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.code.source.CodeMacroSource;
import org.xwiki.rendering.macro.code.source.CodeMacroSourceFactory;
import org.xwiki.rendering.macro.code.source.CodeMacroSourceReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validate the various entity macro source factories.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList(value = {CurrentMacroEntityReferenceResolver.class, DocumentAttachmentCodeMacroSourceLoader.class,
    DocumentCodeMacroSourceLoader.class, DocumentObjectPropertyCodeMacroSourceLoader.class,
    CurrentPageReferenceDocumentReferenceResolver.class, MacroCodeEntitySoureConfiguration.class})
class EntityCodeMacroSourceFactoryTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private DocumentCodeMacroSourceFactory documentFactory;

    @InjectMockComponents
    private PageCodeMacroSourceFactory pageFactory;

    @InjectMockComponents
    private DocumentAttachmentCodeMacroSourceFactory documentAttachmentFactory;

    @InjectMockComponents
    private PageAttachmentCodeMacroSourceFactory pageAttachmentFactory;

    private MacroTransformationContext macroContext;

    @BeforeEach
    public void beforeEach()
    {
        this.macroContext = new MacroTransformationContext();

        MacroBlock macro = new MacroBlock("code", Map.of(), false);
        this.macroContext.setCurrentMacroBlock(macro);

        XDOM xdom = new XDOM(List.of(macro));
        this.macroContext.setXDOM(xdom);
    }

    private void assertCodeMacroSource(CodeMacroSourceFactory factory, CodeMacroSourceReference reference,
        String expectedContent, String expectedLanguage, boolean fail) throws MacroExecutionException
    {
        if (fail) {
            assertThrows(MacroExecutionException.class, () -> factory.getContent(reference, this.macroContext));
        } else {
            assertEquals(new CodeMacroSource(reference, expectedContent, expectedLanguage),
                factory.getContent(reference, this.macroContext));
        }
    }

    @Test
    void getContentDocument() throws MacroExecutionException, XWikiException
    {
        assertCodeMacroSource(this.documentFactory, new CodeMacroSourceReference("document", "wiki:Space.Document"), "",
            null, true);
        assertCodeMacroSource(this.pageFactory, new CodeMacroSourceReference("page", "wiki:Space/Document"), "", null,
            true);

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Document");

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext());
        document.setContent("document content");
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        assertCodeMacroSource(this.documentFactory, new CodeMacroSourceReference("document", "wiki:Space.Document"),
            "document content", null, false);
        assertCodeMacroSource(this.pageFactory, new CodeMacroSourceReference("page", "wiki:Space/Document"),
            "document content", null, false);

        document.setSyntax(Syntax.HTML_5_0);
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        assertCodeMacroSource(this.documentFactory, new CodeMacroSourceReference("document", "wiki:Space.Document"),
            "document content", "html", false);
        assertCodeMacroSource(this.pageFactory, new CodeMacroSourceReference("page", "wiki:Space/Document"),
            "document content", "html", false);
    }

    @Test
    void getContentAttachment() throws MacroExecutionException, XWikiException, IOException
    {
        assertCodeMacroSource(this.documentAttachmentFactory,
            new CodeMacroSourceReference("attachment", "wiki:Space.Document@attachment.ext"), "", null, true);
        assertCodeMacroSource(this.pageAttachmentFactory,
            new CodeMacroSourceReference("page_attachment", "wiki:Space/Document/attachment.ext"), "", null, true);

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Document");

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext());
        XWikiAttachment attachment = document.setAttachment("attachment.ext",
            new ByteArrayInputStream("attachment content".getBytes(StandardCharsets.UTF_8)),
            this.oldcore.getXWikiContext());
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        assertCodeMacroSource(this.documentAttachmentFactory,
            new CodeMacroSourceReference("attachment", "wiki:Space.Document@attachment.ext"), "attachment content",
            null, false);
        assertCodeMacroSource(this.pageAttachmentFactory,
            new CodeMacroSourceReference("page_attachment", "wiki:Space/Document/attachment.ext"), "attachment content",
            null, false);

        attachment.setMimeType("text/html");
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        assertCodeMacroSource(this.documentAttachmentFactory,
            new CodeMacroSourceReference("attachment", "wiki:Space.Document@attachment.ext"), "attachment content",
            "html", false);
        assertCodeMacroSource(this.pageAttachmentFactory,
            new CodeMacroSourceReference("page_attachment", "wiki:Space/Document/attachment.ext"), "attachment content",
            "html", false);
    }
}
