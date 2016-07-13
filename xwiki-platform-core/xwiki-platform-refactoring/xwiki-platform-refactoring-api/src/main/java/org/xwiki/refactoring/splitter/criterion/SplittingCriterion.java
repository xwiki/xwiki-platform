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

import org.xwiki.refactoring.splitter.DocumentSplitter;
import org.xwiki.rendering.block.Block;

/**
 * An interface defining the office importer document splitting process.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public interface SplittingCriterion
{
    /**
     * Limits the recursive tree traversal performed by the {@link DocumentSplitter}.
     * 
     * @param block current {@link Block} being traversed by the document splitter.
     * @param depth depth of this block w.r.t root of the main xdom being split.
     * @return true if the sub-tree rooted at this block should be traversed further.
     */
    boolean shouldIterate(Block block, int depth);

    /**
     * Indicates if the sub-tree rooted at the given block should be split into a separate document.
     * 
     * @param block current {@link Block} being traversed by the document splitter.
     * @param depth depth of this block w.r.t root of the main xdom being split.
     * @return true if the sub-tree rooted at this block should be split into a separate document.
     */
    boolean shouldSplit(Block block, int depth);
}
