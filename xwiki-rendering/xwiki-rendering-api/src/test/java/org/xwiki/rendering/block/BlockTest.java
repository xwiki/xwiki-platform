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

import java.util.Arrays;
import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.SectionLevel;

import junit.framework.TestCase;

public class BlockTest extends TestCase
{
    public void testGetBlocksByType()
    {
        ParagraphBlock pb1 = new ParagraphBlock(new SectionBlock(
            Arrays.asList(new Block[] {new WordBlock("title1")}), SectionLevel.LEVEL1));
        ParagraphBlock pb2 = new ParagraphBlock(new SectionBlock(
            Arrays.asList(new Block[] {new WordBlock("title2")}), SectionLevel.LEVEL2));
        ParagraphBlock pb3 = new ParagraphBlock(Arrays.asList(new Block[] {pb1, pb2}));
        
        List<SectionBlock> results = pb1.getChildrenByType(SectionBlock.class);
        assertEquals(1, results.size());

        results = pb3.getChildrenByType(SectionBlock.class);
        assertEquals(2, results.size());
    }
}
