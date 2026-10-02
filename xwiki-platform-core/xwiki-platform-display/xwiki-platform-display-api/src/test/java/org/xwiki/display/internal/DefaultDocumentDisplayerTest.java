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
package org.xwiki.display.internal;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultDocumentDisplayer}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultDocumentDisplayerTest
{
    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private DefaultDocumentDisplayer displayer;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private DocumentModelBridge document;

    @Mock
    private DocumentModelBridge translatedDocument;

    private final DocumentDisplayerParameters parameters = new DocumentDisplayerParameters();

    private final XDOM expectedXDOM = new XDOM(List.of());

    @BeforeEach
    void beforeEach()
    {
        when(this.document.getDocumentReference()).thenReturn(new DocumentReference("wiki", "Space", "Page"));
        when(this.document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        when(this.translatedDocument.getSyntax()).thenReturn(Syntax.MARKDOWN_1_1);
    }

    @Test
    void displayTranslatedContentUsesTheSyntaxOfTheTranslation() throws Exception
    {
        registerContentDisplayer(Syntax.MARKDOWN_1_1);
        when(this.documentAccessBridge.getTranslatedDocumentInstance(this.document))
            .thenReturn(this.translatedDocument);
        this.parameters.setContentTranslated(true);

        assertSame(this.expectedXDOM, this.displayer.display(this.document, this.parameters));
    }

    @Test
    void displayContentUsesTheSyntaxOfTheDocument() throws Exception
    {
        registerContentDisplayer(Syntax.XWIKI_2_1);
        this.parameters.setContentTranslated(false);

        assertSame(this.expectedXDOM, this.displayer.display(this.document, this.parameters));
    }

    @Test
    void displayTranslatedContentFallsBackOnTheSyntaxOfTheDocument() throws Exception
    {
        registerContentDisplayer(Syntax.XWIKI_2_1);
        when(this.documentAccessBridge.getTranslatedDocumentInstance(this.document))
            .thenThrow(new Exception("Failed to load the translation"));
        this.parameters.setContentTranslated(true);

        assertSame(this.expectedXDOM, this.displayer.display(this.document, this.parameters));

        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to load the translation of document [wiki:Space.Page]. Falling back on the syntax of the "
            + "default translation. Root cause is [Exception: Failed to load the translation]",
            this.logCapture.getMessage(0));
    }

    @Test
    void displayTitleUsesTheSyntaxOfTheDocument() throws Exception
    {
        DocumentDisplayer titleDisplayer = this.componentManager.registerMockComponent(DocumentDisplayer.class,
            "title/" + Syntax.XWIKI_2_1.toIdString());
        when(titleDisplayer.display(this.document, this.parameters)).thenReturn(this.expectedXDOM);

        // The title is not translated by the displayer, so the syntax of the translation must not be used.
        this.parameters.setTitleDisplayed(true);
        this.parameters.setContentTranslated(true);

        assertSame(this.expectedXDOM, this.displayer.display(this.document, this.parameters));
    }

    private void registerContentDisplayer(Syntax syntax) throws Exception
    {
        DocumentDisplayer contentDisplayer =
            this.componentManager.registerMockComponent(DocumentDisplayer.class, "content/" + syntax.toIdString());
        when(contentDisplayer.display(this.document, this.parameters)).thenReturn(this.expectedXDOM);
    }
}
