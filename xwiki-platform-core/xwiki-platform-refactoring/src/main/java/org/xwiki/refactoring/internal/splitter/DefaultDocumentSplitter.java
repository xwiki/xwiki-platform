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
package org.xwiki.refactoring.internal.splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.xwiki.component.annotation.Component;
import org.xwiki.refactoring.WikiDocument;
import org.xwiki.refactoring.splitter.DocumentSplitter;
import org.xwiki.refactoring.splitter.criterion.SplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.NamingCriterion;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BlockFilter;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;

/**
 * Default implementation of {@link DocumentSplitter}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
@Component
public class DefaultDocumentSplitter implements DocumentSplitter
{
    @Override
    public List<WikiDocument> split(WikiDocument rootDoc, SplittingCriterion splittingCriterion,
        NamingCriterion namingCriterion)
    {
        List<WikiDocument> result = new ArrayList<WikiDocument>();
        // Add the rootDoc into the result
        result.add(rootDoc);
        // Recursively split the root document.
        split(rootDoc, rootDoc.getXdom().getChildren(), 1, result, splittingCriterion, namingCriterion);
        return result;
    }

    /**
     * A recursive method for traversing the xdom of the root document and splitting it into sub documents.
     * 
     * @param parentDoc the parent {@link WikiDocument} under which the given list of children reside.
     * @param children current list of blocks being traversed.
     * @param depth the depth from the root xdom to current list of children.
     * @param result space for storing the resulting documents.
     * @param splittingCriterion the {@link SplittingCriterion}.
     * @param namingCriterion the {@link NamingCriterion}.
     */
    private void split(WikiDocument parentDoc, List<Block> children, int depth, List<WikiDocument> result,
        SplittingCriterion splittingCriterion, NamingCriterion namingCriterion)
    {
        ListIterator<Block> it = children.listIterator();
        while (it.hasNext()) {
            Block block = it.next();
            if (splittingCriterion.shouldSplit(block, depth)) {
                // Split a new document and add it to the results list.
                XDOM xdom = new XDOM(block.getChildren());
                String newDocumentName = namingCriterion.getDocumentName(xdom);
                WikiDocument newDoc = new WikiDocument(newDocumentName, xdom, parentDoc);
                result.add(newDoc);
                // Remove the original block from the parent document.
                it.remove();
                // Place a link from the parent to child.
                it.add(new NewLineBlock());
                it.add(createLink(block, newDocumentName));
                // Check whether this node should be further traversed.
                if (splittingCriterion.shouldIterate(block, depth)) {
                    split(newDoc, newDoc.getXdom().getChildren(), depth + 1, result, splittingCriterion,
                        namingCriterion);
                }
            } else if (splittingCriterion.shouldIterate(block, depth)) {
                split(parentDoc, block.getChildren(), depth + 1, result, splittingCriterion, namingCriterion);
            }
        }
    }

    /**
     * Creates a {@link LinkBlock} suitable to be placed in the parent document.
     * 
     * @param block the {@link Block} that has just been split into a separate document.
     * @param target name of the target wiki document.
     * @return a {@link LinkBlock} representing the link from the parent document to new document.
     */
    private LinkBlock createLink(Block block, String target)
    {
        Block firstBlock = block.getChildren().get(0);
        if (firstBlock instanceof HeaderBlock) {
            DocumentResourceReference reference = new DocumentResourceReference(target);
            // Clone the header block and remove any unwanted stuff
            Block clonedHeaderBlock = firstBlock.clone(new BlockFilter()
            {
                public List<Block> filter(Block block)
                {
                    List<Block> blocks = new ArrayList<Block>();
                    if (block instanceof WordBlock || block instanceof SpaceBlock
                        || block instanceof SpecialSymbolBlock) {
                        blocks.add(block);
                    }
                    return blocks;
                }
            });
            return new LinkBlock(clonedHeaderBlock.getChildren(), reference, false);
        } else if (firstBlock instanceof SectionBlock) {
            return createLink(firstBlock, target);
        } else {
            throw new IllegalArgumentException(
                "A SectionBlock should either begin with a HeaderBlock or another SectionBlock.");
        }
    }
}
