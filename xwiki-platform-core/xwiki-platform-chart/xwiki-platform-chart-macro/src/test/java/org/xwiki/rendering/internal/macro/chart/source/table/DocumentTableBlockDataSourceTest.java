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
package org.xwiki.rendering.internal.macro.chart.source.table;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentTableBlockDataSource}.
 *
 * @version $$Id$
 * @since 5.0RC1
 */
@ComponentTest
class DocumentTableBlockDataSourceTest
{
    @InjectMockComponents
    private DocumentTableBlockDataSource source;

    @MockComponent
    private DocumentAccessBridge dab;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private DocumentDisplayer documentDisplayer;

    @Test
    void isDefinedChartSourceTheCurrentDocumentWhenReferenceNotNullAndMatching() throws Exception
    {
        DocumentReference currentReference = new DocumentReference("currentwiki", "currentspace", "currentpage");
        when(this.dab.getCurrentDocumentReference()).thenReturn(currentReference);

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(this.resolver.resolve("wiki:space.page", currentReference)).thenReturn(documentReference);

        MacroBlock currentMacroBlock = mockMacroBlockWithSource("wiki:space.page");

        this.source.setParameter("document", "wiki:space.page");

        assertTrue(this.source.isDefinedChartSourceTheCurrentDocument(currentMacroBlock));
    }

    @Test
    void chartedDocumentIsDisplayedInIsolatedContext() throws Exception
    {
        DocumentReference currentReference = new DocumentReference("currentwiki", "currentspace", "currentpage");
        when(this.dab.getCurrentDocumentReference()).thenReturn(currentReference);
        when(this.resolver.resolve("currentwiki:currentspace.currentpage", currentReference))
            .thenReturn(currentReference);

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(this.resolver.resolve("wiki:space.page", currentReference)).thenReturn(documentReference);

        DocumentModelBridge document = mock();
        when(this.dab.getDocumentInstance(documentReference)).thenReturn(document);

        TableBlock tableBlock = new TableBlock(List.of());
        when(this.documentDisplayer.display(eq(document), any())).thenReturn(new XDOM(List.of(tableBlock)));

        TransformationContext transformationContext = new TransformationContext();
        transformationContext.setTargetSyntax(Syntax.XHTML_1_0);
        MacroTransformationContext context = new MacroTransformationContext(transformationContext);
        // The chart macro is in another document than the charted one, so the charted document needs to be displayed.
        context.setCurrentMacroBlock(mockMacroBlockWithSource("currentwiki:currentspace.currentpage"));

        this.source.setParameter("document", "wiki:space.page");

        assertSame(tableBlock, this.source.getTableBlock(null, context));

        ArgumentCaptor<DocumentDisplayerParameters> parametersCaptor = ArgumentCaptor.captor();
        verify(this.documentDisplayer).display(eq(document), parametersCaptor.capture());
        DocumentDisplayerParameters parameters = parametersCaptor.getValue();
        assertTrue(parameters.isExecutionContextIsolated());
        assertTrue(parameters.isTransformationContextIsolated());
        assertTrue(parameters.isContentTransformed());
        assertTrue(parameters.isContentTranslated());
        assertEquals(Syntax.XHTML_1_0, parameters.getTargetSyntax());
    }

    private static MacroBlock mockMacroBlockWithSource(String source)
    {
        MacroBlock currentMacroBlock = mock();
        MetaDataBlock metaDataBlock = new MetaDataBlock(List.of(), new MetaData(Map.of(MetaData.SOURCE, source)));
        when(currentMacroBlock.getFirstBlock(any(BlockMatcher.class), any(Block.Axes.class))).thenReturn(metaDataBlock);
        return currentMacroBlock;
    }
}
