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

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.SectionBlock;

/**
 * A {@link SplittingCriterion} based on the heading levels present in an xwiki document.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class HeadingLevelSplittingCriterion implements SplittingCriterion
{
    /**
     * Array of heading levels (ascending) used for splitting.
     */
    private int[] headingLevels;
    
    /**
     * Constructs a new {@link HeadingLevelSplittingCriterion}.
     * 
     * @param headingLevels sorted array of heading levels (ascending)
     */
    public HeadingLevelSplittingCriterion(int[] headingLevels)
    {
        this.headingLevels = headingLevels;
        Arrays.sort(this.headingLevels);
    }
    
    @Override
    public boolean shouldIterate(Block block, int depth)
    {
        return (block instanceof SectionBlock && depth < headingLevels[headingLevels.length - 1]);
    }

    @Override
    public boolean shouldSplit(Block block, int depth)
    {
        boolean shouldSplit = false;
        if (block instanceof SectionBlock) {
            SectionBlock section = (SectionBlock) block;
            Block firstChild = section.getChildren().get(0);
            shouldSplit = (firstChild instanceof HeaderBlock) && (Arrays.binarySearch(headingLevels, depth) >= 0);
        }
        return shouldSplit;
    }
}
