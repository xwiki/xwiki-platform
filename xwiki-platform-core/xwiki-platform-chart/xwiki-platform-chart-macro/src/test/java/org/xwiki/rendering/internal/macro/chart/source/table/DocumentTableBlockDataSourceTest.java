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

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

    @Test
    void isDefinedChartSourceTheCurrentDocumentWhenReferenceNotNullAndMatching() throws Exception
    {
        DocumentReference currentReference = new DocumentReference("currentwiki", "currentspace", "currentpage");
        when(this.dab.getCurrentDocumentReference()).thenReturn(currentReference);

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(this.resolver.resolve("wiki:space.page", currentReference)).thenReturn(documentReference);

        MacroBlock currentMacroBlock = mock(MacroBlock.class);
        MetaDataBlock metaDataBlock = new MetaDataBlock(Collections.EMPTY_LIST,
            new MetaData(Collections.singletonMap(MetaData.SOURCE, (Object) "wiki:space.page")));
        when(currentMacroBlock.getFirstBlock(any(BlockMatcher.class), any(Block.Axes.class))).thenReturn(metaDataBlock);

        this.source.setParameter("document", "wiki:space.page");

        assertTrue(source.isDefinedChartSourceTheCurrentDocument(currentMacroBlock));
    }
}
