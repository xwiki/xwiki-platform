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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.macro.CurrentMacroEntityReferenceResolver;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.code.source.CodeMacroSource;
import org.xwiki.rendering.macro.code.source.CodeMacroSourceReference;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate the various entity macro source factories.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList(value = {CurrentMacroEntityReferenceResolver.class, DocumentAttachmentCodeMacroSourceLoader.class,
    DocumentCodeMacroSourceLoader.class, DocumentObjectPropertyCodeMacroSourceLoader.class})
class EntityCodeMacroSourceFactoryTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private DocumentCodeMacroSourceFactory factory;

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

    private void assertCodeMacroSource(CodeMacroSourceReference reference, String expectedContent,
        String expectedLanguage) throws MacroExecutionException
    {
        assertEquals(new CodeMacroSource(reference, expectedContent, expectedLanguage),
            this.factory.getContent(reference, this.macroContext));
    }

    @Test
    void getContentDocument() throws MacroExecutionException, XWikiException
    {
        assertCodeMacroSource(new CodeMacroSourceReference("document", "wiki:Space.Document"), "", null);

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Document");

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext());

        document.setContent("document content");

        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        assertCodeMacroSource(new CodeMacroSourceReference("document", "wiki:Space.Document"), "document content",
            null);
    }
}
