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

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ExpandedMacroBlock}.
 * 
 * @since 3.1
 * @version $Id$
 */
public class ExpandedMacroBlockTest
{
    private BlockRenderer contentRenderer = mock(BlockRenderer.class);

    private ComponentManager componentManager = mock(ComponentManager.class);

    @Test
    public void constructorCallsSuper()
    {
        String id = "gallery";
        Map<String, String> parameters = Collections.singletonMap("width", "300px");
        boolean inline = true;
        ExpandedMacroBlock expandedMacroBlock = new ExpandedMacroBlock(id, parameters, contentRenderer, inline);
        assertEquals(id, expandedMacroBlock.getId());
        assertEquals(parameters, expandedMacroBlock.getParameters());
        assertEquals(inline, expandedMacroBlock.isInline());
    }

    @Test
    public void getContentWrapsChildNodesInXDOM()
    {
        ExpandedMacroBlock expandedMacroBlock =
            new ExpandedMacroBlock("gallery", Collections.<String, String>emptyMap(), this.contentRenderer, false);

        expandedMacroBlock.getContent();

        ArgumentCaptor<Block> block = ArgumentCaptor.forClass(Block.class);
        verify(this.contentRenderer).render(block.capture(), any(WikiPrinter.class));
        assertTrue(block.getValue() instanceof XDOM);
        assertEquals(0, block.getValue().getChildren().size());
    }

    @Test
    public void getContentUsesChildXDOM()
    {
        XDOM content = new XDOM(Collections.<Block>emptyList());
        ExpandedMacroBlock expandedMacroBlock =
            new ExpandedMacroBlock("gallery", Collections.<String, String>emptyMap(), this.contentRenderer, false);
        expandedMacroBlock.addChild(content);

        expandedMacroBlock.getContent();

        verify(this.contentRenderer).render(same(content), any(WikiPrinter.class));
    }

    @Test
    public void getContentUsesAnotherBlockRenderer() throws Exception
    {
        ExpandedMacroBlock expandedMacroBlock = new ExpandedMacroBlock("gallery",
            Collections.<String, String>emptyMap(), this.contentRenderer, false, this.componentManager);
        XDOM parent = new XDOM(Collections.singletonList(expandedMacroBlock));
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
