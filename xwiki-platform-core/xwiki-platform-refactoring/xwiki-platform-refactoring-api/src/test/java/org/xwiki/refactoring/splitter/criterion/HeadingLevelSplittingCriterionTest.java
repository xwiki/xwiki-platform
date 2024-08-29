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
package org.xwiki.refactoring.splitter.criterion;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.HeaderLevel;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link HeadingLevelSplittingCriterion}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
class HeadingLevelSplittingCriterionTest
{
    private XDOM xdom = new XDOM(Arrays.asList(
        new SectionBlock(Arrays.asList(new HeaderBlock(Arrays.asList(new WordBlock("Test")), HeaderLevel.LEVEL1)))));

    @Test
    void testIterateCondition() throws Exception
    {
        SplittingCriterion splittingCriterion = new HeadingLevelSplittingCriterion(new int[] {3, 2, 1});
        Block sectionBlock = this.xdom.getChildren().get(0);
        assertTrue(!splittingCriterion.shouldIterate(this.xdom, 0));
        assertTrue(splittingCriterion.shouldIterate(sectionBlock, 1));
        assertTrue(splittingCriterion.shouldSplit(sectionBlock, 3));
        assertTrue(!splittingCriterion.shouldSplit(sectionBlock, 4));
    }

    @Test
    void testSplitCondition() throws Exception
    {
        SplittingCriterion splittingCriterion = new HeadingLevelSplittingCriterion(new int[] {3, 2, 1});
        Block sectionBlock = this.xdom.getChildren().get(0);
        assertTrue(splittingCriterion.shouldSplit(sectionBlock, 1));
        splittingCriterion = new HeadingLevelSplittingCriterion(new int[] {3, 2});
        assertTrue(!splittingCriterion.shouldSplit(sectionBlock, 1));
    }
}
