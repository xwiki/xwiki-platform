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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DocumentTableBlockDataSource}.
 *
 * @version $$Id$
 * @since 5.0RC1
 */
public class DocumentTableBlockDataSourceTest
{
    @Rule
    public MockitoComponentMockingRule<DocumentTableBlockDataSource> componentManager =
        new MockitoComponentMockingRule<DocumentTableBlockDataSource>(DocumentTableBlockDataSource.class);

    @Test
    public void isDefinedChartSourceTheCurrentDocumentWhenReferenceNotNullAndMatching() throws Exception
    {
        DocumentAccessBridge dab = this.componentManager.getInstance(DocumentAccessBridge.class);
        DocumentReference currentReference = new DocumentReference("currentwiki", "currentspace", "currentpage");
        when(dab.getCurrentDocumentReference()).thenReturn(currentReference);

        DocumentReferenceResolver<String> resolver =
            this.componentManager.getInstance(DocumentReferenceResolver.TYPE_STRING);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(resolver.resolve("wiki:space.page", currentReference)).thenReturn(documentReference);

        MacroBlock currentMacroBlock = mock(MacroBlock.class);
        MetaDataBlock metaDataBlock = new MetaDataBlock(Collections.EMPTY_LIST,
            new MetaData(Collections.singletonMap(MetaData.SOURCE, (Object) "wiki:space.page")));
        when(currentMacroBlock.getFirstBlock(any(BlockMatcher.class), any(Block.Axes.class))).thenReturn(metaDataBlock);

        DocumentTableBlockDataSource source = this.componentManager.getComponentUnderTest();
        source.setParameter("document", "wiki:space.page");

        Assert.assertTrue(source.isDefinedChartSourceTheCurrentDocument(currentMacroBlock));
    }
}
