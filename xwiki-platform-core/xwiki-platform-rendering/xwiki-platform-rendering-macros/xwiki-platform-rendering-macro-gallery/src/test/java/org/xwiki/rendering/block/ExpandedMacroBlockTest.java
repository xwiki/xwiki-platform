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
package org.xwiki.rendering.block;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExpandedMacroBlock}.
 *
 * @version $Id$
 * @since 3.1
 */
class ExpandedMacroBlockTest
{
    private final BlockRenderer contentRenderer = mock(BlockRenderer.class);

    private final ComponentManager componentManager = mock(ComponentManager.class);

    @Test
    void constructorCallsSuper()
    {
        String id = "gallery";
        Map<String, String> parameters = Map.of("width", "300px");
        boolean inline = true;
        ExpandedMacroBlock expandedMacroBlock = new ExpandedMacroBlock(id, parameters, this.contentRenderer, inline);
        assertEquals(id, expandedMacroBlock.getId());
        assertEquals(parameters, expandedMacroBlock.getParameters());
        assertEquals(inline, expandedMacroBlock.isInline());
    }

    @Test
    void getContentWrapsChildNodesInXDOM()
    {
        ExpandedMacroBlock expandedMacroBlock =
            new ExpandedMacroBlock("gallery", Map.of(), this.contentRenderer, false);

        expandedMacroBlock.getContent();

        ArgumentCaptor<Block> block = ArgumentCaptor.forClass(Block.class);
        verify(this.contentRenderer).render(block.capture(), any(WikiPrinter.class));
        assertInstanceOf(XDOM.class, block.getValue());
        assertEquals(0, block.getValue().getChildren().size());
    }

    @Test
    void getContentUsesChildXDOM()
    {
        XDOM content = new XDOM(List.of());
        ExpandedMacroBlock expandedMacroBlock =
            new ExpandedMacroBlock("gallery", Map.of(), this.contentRenderer, false);
        expandedMacroBlock.addChild(content);

        expandedMacroBlock.getContent();

        verify(this.contentRenderer).render(same(content), any(WikiPrinter.class));
    }

    @Test
    void getContentUsesAnotherBlockRenderer() throws Exception
    {
        ExpandedMacroBlock expandedMacroBlock = new ExpandedMacroBlock("gallery", Map.of(), this.contentRenderer,
            false, this.componentManager);
        XDOM parent = new XDOM(List.of(expandedMacroBlock));
        parent.getMetaData().addMetaData(MetaData.SYNTAX, Syntax.MARKDOWN_1_1);

        BlockRenderer markdownRenderer = mock(BlockRenderer.class);
        when(this.componentManager.hasComponent(BlockRenderer.class, Syntax.MARKDOWN_1_1.toIdString()))
            .thenReturn(true);
        when(this.componentManager.getInstance(BlockRenderer.class, Syntax.MARKDOWN_1_1.toIdString()))
            .thenReturn(markdownRenderer);

        expandedMacroBlock.getContent();

        verify(markdownRenderer).render(any(XDOM.class), any(WikiPrinter.class));
        verify(this.contentRenderer, never()).render(any(XDOM.class), any(WikiPrinter.class));
    }
}
