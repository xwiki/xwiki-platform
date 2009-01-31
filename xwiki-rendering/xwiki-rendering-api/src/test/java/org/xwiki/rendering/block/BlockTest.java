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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.xwiki.rendering.listener.DefaultAttachement;
import org.xwiki.rendering.listener.DocumentImage;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.xml.XMLElement;

import junit.framework.TestCase;

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
            new ParagraphBlock(Arrays.<Block> asList(new HeaderBlock(Arrays
                .asList(new Block[] {new WordBlock("title1")}), SectionLevel.LEVEL1)));
        ParagraphBlock pb2 =
            new ParagraphBlock(Arrays.<Block> asList(new HeaderBlock(Arrays
                .asList(new Block[] {new WordBlock("title2")}), SectionLevel.LEVEL2)));
        ParagraphBlock pb3 = new ParagraphBlock(Arrays.asList(new Block[] {pb1, pb2}));

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
        Block wb = new WordBlock("block");
        Block parentBlock = new ParagraphBlock(Arrays.asList(wb));

        Block newBlock1 = new WordBlock("block1");
        Block newBlock2 = new WordBlock("block2");
        wb.replace(Arrays.asList(newBlock1, newBlock2));

        assertEquals(2, parentBlock.getChildren().size());
        assertSame(newBlock1, parentBlock.getChildren().get(0));
        assertSame(newBlock2, parentBlock.getChildren().get(1));
    }

    public void testClone()
    {
        WordBlock wb = new WordBlock("block");
        ImageBlock ib = new ImageBlock(new DocumentImage(new DefaultAttachement("document", "attachment")), true);
        Link link = new Link();
        link.setReference("reference");
        LinkBlock lb = new LinkBlock(Arrays.asList((Block) new WordBlock("label")), link, false);
        XMLBlock xb = new XMLBlock(new XMLElement("xml", Collections.singletonMap("key", "value")));
        Block rootBlock = new ParagraphBlock(Arrays.asList((Block) wb, ib, lb, xb));

        Block newRootBlock = rootBlock.clone();

        assertNotSame(rootBlock, newRootBlock);
        assertNotSame(wb, newRootBlock.getChildren().get(0));
        assertNotSame(ib, newRootBlock.getChildren().get(1));
        assertNotSame(lb, newRootBlock.getChildren().get(2));
        assertNotSame(lb, newRootBlock.getChildren().get(3));

        assertEquals(wb.getWord(), ((WordBlock) newRootBlock.getChildren().get(0)).getWord());
        assertNotSame(ib.getImage(), ((ImageBlock) newRootBlock.getChildren().get(1)).getImage());
        assertNotSame(lb.getLink(), ((LinkBlock) newRootBlock.getChildren().get(2)).getLink());
        assertNotSame(xb.getXMLNode(), ((XMLBlock) newRootBlock.getChildren().get(3)).getXMLNode());
    }
}
