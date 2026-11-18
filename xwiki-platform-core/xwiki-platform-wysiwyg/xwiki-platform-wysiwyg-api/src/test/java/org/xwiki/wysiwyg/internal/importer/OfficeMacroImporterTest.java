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
package org.xwiki.wysiwyg.internal.importer;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OfficeMacroImporter}.
 *
 * @version $Id$
 * @since 9.8
 */
@ComponentTest
class OfficeMacroImporterTest
{
    @InjectMockComponents
    private OfficeMacroImporter officeMacroImporter;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    @Named("macro")
    private Transformation macroTransformation;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    private MutableRenderingContext renderingContext;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        this.renderingContext = mock(MutableRenderingContext.class);
        componentManager.registerComponent(RenderingContext.class, this.renderingContext);
    }

    @Test
    void buildXDOM() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        XWikiDocument xwikiDocument = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getTranslatedDocumentInstance(documentReference)).thenReturn(xwikiDocument);
        when(xwikiDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        XDOM xdom =
            this.officeMacroImporter.buildXDOM(new AttachmentReference("file", documentReference), true);

        assertNotNull(xdom);

        // Verify we have the proper METADATA set
        assertEquals("xwiki/2.1", ((Syntax) xdom.getMetaData().getMetaData(MetaData.SYNTAX)).toIdString());
    }

    @Test
    void renderWithOutputSyntax() throws Exception
    {
        ComponentManager contextComponentManager = mock(ComponentManager.class);
        when(this.contextComponentManagerProvider.get()).thenReturn(contextComponentManager);
        BlockRenderer blockNoteRenderer = mock(BlockRenderer.class);
        when(contextComponentManager.getInstance(BlockRenderer.class, "blocknote/1.0"))
            .thenReturn(blockNoteRenderer);
        doAnswer(invocation -> {
            invocation.<WikiPrinter>getArgument(1).print("[]");
            return null;
        }).when(blockNoteRenderer).render(any(XDOM.class), any(WikiPrinter.class));

        XDOM xdom = new XDOM(List.of());

        assertEquals("[]", this.officeMacroImporter.render(xdom, "blocknote/1.0"));

        // The office macro must run before the renderer sees the XDOM, otherwise its output is missing.
        verify(this.renderingContext).transformInContext(eq(this.macroTransformation),
            any(TransformationContext.class), eq(xdom));
    }
}
