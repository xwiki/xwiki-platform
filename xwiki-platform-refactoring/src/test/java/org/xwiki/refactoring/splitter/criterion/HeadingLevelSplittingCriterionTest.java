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

import java.io.StringReader;

import org.xwiki.refactoring.internal.AbstractRefactoringTestCase;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for {@link HeadingLevelSplittingCriterion}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class HeadingLevelSplittingCriterionTest extends AbstractRefactoringTestCase
{
    /**
     * Tests the iterate condition.
     * 
     * @throws Exception
     */
    @Test
    public void testIterateCondition() throws Exception
    {
        XDOM xdom = xwikiParser.parse(new StringReader("=Test="));
        SplittingCriterion splittingCriterion = new HeadingLevelSplittingCriterion(new int[] {3, 2, 1});
        Block sectionBlock = xdom.getChildren().get(0);
        Assert.assertTrue(!splittingCriterion.shouldIterate(xdom, 0));
        Assert.assertTrue(splittingCriterion.shouldIterate(sectionBlock, 1));
        Assert.assertTrue(splittingCriterion.shouldSplit(sectionBlock, 3));
        Assert.assertTrue(!splittingCriterion.shouldSplit(sectionBlock, 4));
    }
    
    /**
     * Tests the split condition.
     * 
     * @throws Exception
     */
    @Test
    public void testSplitCondition() throws Exception
    {
        XDOM xdom = xwikiParser.parse(new StringReader("=Test="));
        SplittingCriterion splittingCriterion = new HeadingLevelSplittingCriterion(new int[] {3, 2, 1});
        Block sectionBlock = xdom.getChildren().get(0);
        Assert.assertTrue(splittingCriterion.shouldSplit(sectionBlock, 1));
        splittingCriterion = new HeadingLevelSplittingCriterion(new int[] {3, 2});
        Assert.assertTrue(!splittingCriterion.shouldSplit(sectionBlock, 1));
    }
}
