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

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.rendering.block.Block.Axes;
import org.xwiki.rendering.block.match.AnyBlockMatcher;
import org.xwiki.rendering.block.match.SameBlockMatcher;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Unit tests for Block manipulation, testing {@link AbstractBlock}.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class BlockTest
{
    @Test
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
        Assert.assertEquals(1, results.size());

        results = pb3.getChildrenByType(HeaderBlock.class, true);
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testInsertChildAfter()
    {
        Block wb1 = new WordBlock("block1");
        Block wb2 = new WordBlock("block2");
        ParagraphBlock pb = new ParagraphBlock(Arrays.asList(wb1, wb2));

        Block wb = new WordBlock("block");

        pb.insertChildAfter(wb, wb1);
        Assert.assertSame(wb, pb.getChildren().get(1));
        Assert.assertSame(wb1, wb.getPreviousSibling());
        Assert.assertSame(wb2, wb.getNextSibling());
        Assert.assertSame(wb, wb1.getNextSibling());
        Assert.assertSame(wb, wb2.getPreviousSibling());

        pb.insertChildAfter(wb, wb2);
        Assert.assertSame(wb, pb.getChildren().get(3));
        Assert.assertSame(wb2, wb.getPreviousSibling());
        Assert.assertSame(wb, wb2.getNextSibling());
        Assert.assertNull(wb.getNextSibling());
    }

    @Test
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
        Assert.assertSame(wb, pb.getChildren().get(0));

        pb.insertChildBefore(wb, wb2);
        Assert.assertSame(wb, pb.getChildren().get(2));
    }

    @Test
    public void testReplaceBlock()
    {
        // It's important all blocks have same content to make sure replacement api don't find the position of the
        // old block using Object#equals
        Block word1 = new WordBlock("block1");
        Block word2 = new WordBlock("block2");
        Block word3 = new WordBlock("block3");

        Block parentBlock = new ParagraphBlock(Arrays.asList(word1, word2));

        // replace by one
        parentBlock.replaceChild(word3, word1);

        Assert.assertEquals(2, parentBlock.getChildren().size());
        Assert.assertSame(word3, parentBlock.getChildren().get(0));
        Assert.assertSame(word2, parentBlock.getChildren().get(1));
        Assert.assertSame(word2, word3.getNextSibling());
        Assert.assertSame(word3, word2.getPreviousSibling());

        // replace by nothing
        parentBlock.replaceChild(Collections.<Block> emptyList(), word2);

        Assert.assertEquals(1, parentBlock.getChildren().size());
        Assert.assertSame(word3, parentBlock.getChildren().get(0));
        Assert.assertNull(word3.getNextSibling());
        Assert.assertNull(word3.getPreviousSibling());

        // replace by several
        parentBlock.replaceChild(Arrays.asList(word1, word2), word3);

        Assert.assertEquals(2, parentBlock.getChildren().size());
        Assert.assertSame(word1, parentBlock.getChildren().get(0));
        Assert.assertSame(word2, parentBlock.getChildren().get(1));
        Assert.assertSame(word2, word1.getNextSibling());
        Assert.assertSame(word1, word2.getPreviousSibling());

        // provide not existing block to replace
        try {
            parentBlock.replaceChild(word3, new WordBlock("not existing"));
            Assert.fail("Should have thrown an InvalidParameterException exception");
        } catch (InvalidParameterException expected) {
            // expected
        }
    }

    @Test
    public void testClone()
    {
        WordBlock wb = new WordBlock("block");
        ImageBlock ib = new ImageBlock(new ResourceReference("document@attachment", ResourceType.ATTACHMENT), true);
        DocumentResourceReference linkReference = new DocumentResourceReference("reference");
        LinkBlock lb = new LinkBlock(Arrays.asList((Block) new WordBlock("label")), linkReference, false);
        Block rootBlock = new ParagraphBlock(Arrays.<Block> asList(wb, ib, lb));

        Block newRootBlock = rootBlock.clone();

        Assert.assertNotSame(rootBlock, newRootBlock);
        Assert.assertNotSame(wb, newRootBlock.getChildren().get(0));
        Assert.assertNotSame(ib, newRootBlock.getChildren().get(1));
        Assert.assertNotSame(lb, newRootBlock.getChildren().get(2));

        Assert.assertEquals(wb.getWord(), ((WordBlock) newRootBlock.getChildren().get(0)).getWord());
        Assert.assertNotSame(ib.getReference(), ((ImageBlock) newRootBlock.getChildren().get(1)).getReference());
        Assert.assertNotSame(lb.getReference(), ((LinkBlock) newRootBlock.getChildren().get(2)).getReference());
    }

    @Test
    public void testGetPreviousBlockByType()
    {
        WordBlock lw = new WordBlock("linkword");
        SpecialSymbolBlock ls = new SpecialSymbolBlock('$');
        
        DocumentResourceReference linkReference = new DocumentResourceReference("reference");
        LinkBlock pl = new LinkBlock(Arrays.<Block>asList(lw, ls), linkReference, false);
        
        ImageBlock pi = new ImageBlock(new ResourceReference("document@attachment", ResourceType.ATTACHMENT), true);
        
        ParagraphBlock rootBlock = new ParagraphBlock(Arrays.<Block> asList(pi, pl));
        
        Assert.assertSame(lw, ls.getPreviousBlockByType(WordBlock.class, false));
        Assert.assertNull(ls.getPreviousBlockByType(ImageBlock.class, false));
        Assert.assertSame(pl, ls.getPreviousBlockByType(LinkBlock.class, true));
        Assert.assertSame(pi, ls.getPreviousBlockByType(ImageBlock.class, true));
        Assert.assertSame(rootBlock, ls.getPreviousBlockByType(ParagraphBlock.class, true));
    }

    @Test
    public void testGetNextSibling()
    {
        WordBlock b1 = new WordBlock("b1");
        WordBlock b2 = new WordBlock("b2");
        ParagraphBlock p = new ParagraphBlock(Arrays.<Block> asList(b1, b2));

        Assert.assertSame(b2, b1.getNextSibling());
        Assert.assertNull(b2.getNextSibling());
        Assert.assertNull(p.getNextSibling());
        Assert.assertNull(new ParagraphBlock(Collections.<Block>emptyList()).getNextSibling());
    }

    @Test
    public void testRemoveBlock()
    {
        WordBlock b1 = new WordBlock("b1");
        WordBlock b2 = new WordBlock("b2");
        ParagraphBlock p1 = new ParagraphBlock(Arrays.<Block> asList(b1, b2));

        p1.removeBlock(b1);
        Assert.assertEquals(1, p1.getChildren().size());
        Assert.assertSame(b2, p1.getChildren().get(0));
        Assert.assertNull(b1.getPreviousSibling());
        Assert.assertNull(b1.getNextSibling());
        Assert.assertNull(b2.getPreviousSibling());

        p1.removeBlock(b2);
        Assert.assertEquals(0, p1.getChildren().size());
        Assert.assertNull(b2.getPreviousSibling());
        Assert.assertNull(b2.getNextSibling());
    }

    @Test
    public void testGetBlocks()
    {
        final WordBlock precedingBlockChild1 = new WordBlock("pc1");
        final WordBlock precedingBlockChild2 = new WordBlock("pc2");
        final ParagraphBlock precedingBlock = new ParagraphBlock(Arrays.<Block> asList(precedingBlockChild1, precedingBlockChild2))
            {public String toString(){return "precedingBlock";}};

        final WordBlock contextBlockChild21 = new WordBlock("cc21");
        final WordBlock contextBlockChild22 = new WordBlock("cc22");
        final ParagraphBlock contextBlockChild2 = new ParagraphBlock(Arrays.<Block> asList(contextBlockChild21, contextBlockChild22))
            {public String toString(){return "contextBlockChild2";}};
        
        final WordBlock contextBlockChild11 = new WordBlock("cc11");
        final WordBlock contextBlockChild12 = new WordBlock("cc12");
        final ParagraphBlock contextBlockChild1 = new ParagraphBlock(Arrays.<Block> asList(contextBlockChild11, contextBlockChild12))
            {public String toString(){return "contextBlockChild1";}};
        
        final ParagraphBlock contextBlock = new ParagraphBlock(Arrays.<Block> asList(contextBlockChild1, contextBlockChild2))
            {public String toString(){return "contextBlock";}};

        final WordBlock followingBlockChild1 = new WordBlock("fc1");
        final WordBlock followingBlockChild2 = new WordBlock("fc2");
        final ParagraphBlock followingBlock = new ParagraphBlock(Arrays.<Block> asList(followingBlockChild1, followingBlockChild2))
            {public String toString(){return "followingBlock";}};

        final ParagraphBlock parentBlock = new ParagraphBlock(Arrays.<Block> asList(precedingBlock, contextBlock, followingBlock))
            {public String toString(){return "parentBlock";}};
        
        final ParagraphBlock rootBlock = new ParagraphBlock(Arrays.<Block> asList(parentBlock))
            {public String toString(){return "rootBlock";}};

        // tests
 
        Assert.assertEquals(Arrays.asList(parentBlock, rootBlock), contextBlock.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.ANCESTOR));
        Assert.assertEquals(Arrays.asList(contextBlock, parentBlock, rootBlock), contextBlock.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.ANCESTOR_OR_SELF));
        Assert.assertEquals(Arrays.asList(contextBlockChild1, contextBlockChild2), contextBlock.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.CHILD));
        Assert.assertEquals(Arrays.asList(contextBlockChild1, contextBlockChild11, contextBlockChild12, contextBlockChild2, contextBlockChild21, contextBlockChild22), contextBlock.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.DESCENDANT));
        Assert.assertEquals(Arrays.asList(contextBlock, contextBlockChild1, contextBlockChild11, contextBlockChild12, contextBlockChild2, contextBlockChild21, contextBlockChild22), contextBlock.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.DESCENDANT_OR_SELF));
        Assert.assertEquals(Arrays.asList(followingBlock, followingBlockChild1, followingBlockChild2), contextBlock.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.FOLLOWING));
        Assert.assertEquals(Arrays.asList(followingBlock), contextBlock.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.FOLLOWING_SIBLING));
        Assert.assertEquals(Arrays.asList(parentBlock), contextBlock.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.PARENT));
        Assert.assertEquals(Arrays.asList(precedingBlock, precedingBlockChild1, precedingBlockChild2), contextBlock.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.PRECEDING));
        Assert.assertEquals(Arrays.asList(precedingBlock), contextBlock.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.PRECEDING_SIBLING));
        Assert.assertEquals(Arrays.asList(contextBlock), contextBlock.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.SELF));
    }

    @Test
    public void testGetFirstBlock()
    {
        final WordBlock unexistingBlock = new WordBlock("unexistingBlock");

        final WordBlock precedingBlockChild1 = new WordBlock("pc1");
        final WordBlock precedingBlockChild2 = new WordBlock("pc2");
        final ParagraphBlock precedingBlock = new ParagraphBlock(Arrays.<Block> asList(precedingBlockChild1, precedingBlockChild2))
            {public String toString(){return "precedingBlock";}};

        final WordBlock contextBlockChild21 = new WordBlock("cc21");
        final WordBlock contextBlockChild22 = new WordBlock("cc22");
        final ParagraphBlock contextBlockChild2 = new ParagraphBlock(Arrays.<Block> asList(contextBlockChild21, contextBlockChild22))
            {public String toString(){return "contextBlockChild2";}};
        
        final WordBlock contextBlockChild11 = new WordBlock("cc11");
        final WordBlock contextBlockChild12 = new WordBlock("cc12");
        final ParagraphBlock contextBlockChild1 = new ParagraphBlock(Arrays.<Block> asList(contextBlockChild11, contextBlockChild12))
            {public String toString(){return "contextBlockChild1";}};
        
        final ParagraphBlock contextBlock = new ParagraphBlock(Arrays.<Block> asList(contextBlockChild1, contextBlockChild2))
            {public String toString(){return "contextBlock";}};

        final WordBlock followingBlockChild1 = new WordBlock("fc1");
        final WordBlock followingBlockChild2 = new WordBlock("fc2");
        final ParagraphBlock followingBlock = new ParagraphBlock(Arrays.<Block> asList(followingBlockChild1, followingBlockChild2))
            {public String toString(){return "followingBlock";}};

        final ParagraphBlock parentBlock = new ParagraphBlock(Arrays.<Block> asList(precedingBlock, contextBlock, followingBlock))
            {public String toString(){return "parentBlock";}};
        
        final ParagraphBlock rootBlock = new ParagraphBlock(Arrays.<Block> asList(parentBlock))
            {public String toString(){return "rootBlock";}};

        // tests

        Assert.assertSame(parentBlock, contextBlock.getFirstBlock(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.ANCESTOR));
        Assert.assertSame(rootBlock, contextBlock.getFirstBlock(new SameBlockMatcher(rootBlock), Axes.ANCESTOR));
        Assert.assertSame(contextBlock, contextBlock.getFirstBlock(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.ANCESTOR_OR_SELF));
        Assert.assertSame(rootBlock, contextBlock.getFirstBlock(new SameBlockMatcher(rootBlock), Axes.ANCESTOR_OR_SELF));
        Assert.assertSame(contextBlockChild1, contextBlock.getFirstBlock(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.CHILD));
        Assert.assertSame(contextBlockChild2, contextBlock.getFirstBlock(new SameBlockMatcher(contextBlockChild2), Axes.CHILD));
        Assert.assertSame(contextBlockChild1, contextBlock.getFirstBlock(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.DESCENDANT));
        Assert.assertSame(contextBlockChild22, contextBlock.getFirstBlock(new SameBlockMatcher(contextBlockChild22), Axes.DESCENDANT));
        Assert.assertSame(contextBlock, contextBlock.getFirstBlock(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.DESCENDANT_OR_SELF));
        Assert.assertSame(contextBlockChild22, contextBlock.getFirstBlock(new SameBlockMatcher(contextBlockChild22), Axes.DESCENDANT_OR_SELF));
        Assert.assertSame(followingBlock, contextBlock.getFirstBlock(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.FOLLOWING));
        Assert.assertSame(followingBlockChild2, contextBlock.getFirstBlock(new SameBlockMatcher(followingBlockChild2), Axes.FOLLOWING));
        Assert.assertSame(followingBlock, contextBlock.getFirstBlock(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.FOLLOWING_SIBLING));
        Assert.assertNull(contextBlock.getFirstBlock(new SameBlockMatcher(unexistingBlock), Axes.FOLLOWING_SIBLING));
        Assert.assertSame(parentBlock, contextBlock.getFirstBlock(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.PARENT));
        Assert.assertNull(contextBlock.getFirstBlock(new SameBlockMatcher(unexistingBlock), Axes.PARENT));
        Assert.assertSame(precedingBlock, contextBlock.getFirstBlock(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.PRECEDING));
        Assert.assertSame(precedingBlockChild2, contextBlock.getFirstBlock(new SameBlockMatcher(precedingBlockChild2), Axes.PRECEDING));
        Assert.assertSame(precedingBlock, contextBlock.getFirstBlock(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.PRECEDING_SIBLING));
        Assert.assertNull(contextBlock.getFirstBlock(new SameBlockMatcher(unexistingBlock), Axes.PRECEDING_SIBLING));
        Assert.assertSame(contextBlock, contextBlock.getFirstBlock(AnyBlockMatcher.ANYBLOCKMATCHER, Axes.SELF));
        Assert.assertNull(contextBlock.getFirstBlock(new SameBlockMatcher(unexistingBlock), Axes.SELF));
    }
}
