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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.xwiki.rendering.listener.DefaultAttachment;
import org.xwiki.rendering.listener.DocumentImage;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Link;

/**
 * Unit tests for Block manipulation, testing {@link AbstractBlock}.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class BlockTest extends TestCase
{
    public void testGetBlocksByType()
    {
        ParagraphBlock pb1 =
            new ParagraphBlock(Arrays.<Block> asList(new HeaderBlock(Arrays.<Block> asList(new WordBlock("title1")),
                HeaderLevel.LEVEL1)));
        ParagraphBlock pb2 =
            new ParagraphBlock(Arrays.<Block> asList(new HeaderBlock(Arrays.<Block> asList(new WordBlock("title2")),
                HeaderLevel.LEVEL2)));
        ParagraphBlock pb3 = new ParagraphBlock(Arrays.<Block> asList(pb1, pb2));

        List<HeaderBlock> results = pb1.getChildrenByType(HeaderBlock.class, true);
        assertEquals(1, results.size());

        results = pb3.getChildrenByType(HeaderBlock.class, true);
        assertEquals(2, results.size());
    }

    public void testInsertChildAfter()
    {
        Block wb1 = new WordBlock("block1");
        Block wb2 = new WordBlock("block2");

        List<Block> children = new ArrayList<Block>();
        children.add(wb1);
        children.add(wb2);

        ParagraphBlock pb = new ParagraphBlock(children);

        Block wb = new WordBlock("block");

        pb.insertChildAfter(wb, wb1);
        assertSame(wb, pb.getChildren().get(1));

        pb.insertChildAfter(wb, wb2);
        assertSame(wb, pb.getChildren().get(3));
    }

    public void testInsertChildBefore()
    {
        Block wb1 = new WordBlock("block1");
        Block wb2 = new WordBlock("block2");

        List<Block> children = new ArrayList<Block>();
        children.add(wb1);
        children.add(wb2);

        ParagraphBlock pb = new ParagraphBlock(children);

        Block wb = new WordBlock("block");

        pb.insertChildBefore(wb, wb1);
        assertSame(wb, pb.getChildren().get(0));

        pb.insertChildBefore(wb, wb2);
        assertSame(wb, pb.getChildren().get(2));
    }

    public void testReplaceBlock()
    {
        // It's important all blocks have same content to make sure replacement api don't find the position of the
        // old block using Object#equals
        Block word1 = new WordBlock("block");
        Block word2 = new WordBlock("block");
        Block word3 = new WordBlock("block");

        Block parentBlock = new ParagraphBlock(Arrays.asList(word1, word2));

        // replace by one
        parentBlock.replaceChild(word3, word1);

        assertEquals(2, parentBlock.getChildren().size());
        assertSame(word3, parentBlock.getChildren().get(0));
        assertSame(word2, parentBlock.getChildren().get(1));

        // replace by nothing
        parentBlock.replaceChild(Collections.<Block> emptyList(), word2);

        assertEquals(1, parentBlock.getChildren().size());
        assertSame(word3, parentBlock.getChildren().get(0));

        // replace by several
        parentBlock.replaceChild(Arrays.asList(word1, word2), word3);

        assertEquals(2, parentBlock.getChildren().size());
        assertSame(word1, parentBlock.getChildren().get(0));
        assertSame(word2, parentBlock.getChildren().get(1));

        // provide not existing block to replace
        try {
            parentBlock.replaceChild(word3, new WordBlock("not existing"));
            fail("Should have thrown an InvalidParameterException exception");
        } catch (InvalidParameterException expected) {
            // expected
        }
    }

    public void testClone()
    {
        WordBlock wb = new WordBlock("block");
        ImageBlock ib = new ImageBlock(new DocumentImage(new DefaultAttachment("document", "attachment")), true);
        Link link = new Link();
        link.setReference("reference");
        LinkBlock lb = new LinkBlock(Arrays.asList((Block) new WordBlock("label")), link, false);
        Block rootBlock = new ParagraphBlock(Arrays.<Block> asList(wb, ib, lb));

        Block newRootBlock = rootBlock.clone();

        assertNotSame(rootBlock, newRootBlock);
        assertNotSame(wb, newRootBlock.getChildren().get(0));
        assertNotSame(ib, newRootBlock.getChildren().get(1));
        assertNotSame(lb, newRootBlock.getChildren().get(2));

        assertEquals(wb.getWord(), ((WordBlock) newRootBlock.getChildren().get(0)).getWord());
        assertNotSame(ib.getImage(), ((ImageBlock) newRootBlock.getChildren().get(1)).getImage());
        assertNotSame(lb.getLink(), ((LinkBlock) newRootBlock.getChildren().get(2)).getLink());
    }
    
    public void testGetPreviousBlockByType()
    {
        WordBlock lw = new WordBlock("linkword");
        SpecialSymbolBlock ls = new SpecialSymbolBlock('$');
        
        Link link = new Link();
        link.setReference("reference");
        LinkBlock pl = new LinkBlock(Arrays.<Block>asList(lw, ls), link, false);
        
        ImageBlock pi = new ImageBlock(new DocumentImage(new DefaultAttachment("document", "attachment")), true);
        
        ParagraphBlock rootBlock = new ParagraphBlock(Arrays.<Block> asList(pi, pl));
        
        assertSame(lw, ls.getPreviousBlockByType(WordBlock.class, false));
        assertNull(ls.getPreviousBlockByType(ImageBlock.class, false));
        assertSame(pl, ls.getPreviousBlockByType(LinkBlock.class, true));
        assertSame(pi, ls.getPreviousBlockByType(ImageBlock.class, true));
        assertSame(rootBlock, ls.getPreviousBlockByType(ParagraphBlock.class, true));
    }
}
