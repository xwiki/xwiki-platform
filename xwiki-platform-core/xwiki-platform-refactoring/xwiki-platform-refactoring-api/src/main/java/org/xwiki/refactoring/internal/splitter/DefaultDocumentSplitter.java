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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.refactoring.WikiDocument;
import org.xwiki.refactoring.splitter.DocumentSplitter;
import org.xwiki.refactoring.splitter.criterion.SplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.NamingCriterion;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.Block.Axes;
import org.xwiki.rendering.block.BlockFilter;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Default implementation of {@link DocumentSplitter}.
 *
 * @version $Id$
 * @since 1.9M1
 */
@Component
@Singleton
public class DefaultDocumentSplitter implements DocumentSplitter
{
    /**
     * The name of the anchor link parameter.
     */
    private static final String ANCHOR_PARAMETER = "anchor";

    @Override
    public List<WikiDocument> split(WikiDocument rootDoc, SplittingCriterion splittingCriterion,
        NamingCriterion namingCriterion)
    {
        List<WikiDocument> result = new ArrayList<WikiDocument>();
        // Add the rootDoc into the result
        result.add(rootDoc);
        // Recursively split the root document.
        split(rootDoc, rootDoc.getXdom().getChildren(), 1, result, splittingCriterion, namingCriterion);
        updateAnchors(result);
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
                @Override
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

    /**
     * Update the links to internal document fragments after those fragments have been moved as a result of the split.
     * For instance the "#Chapter1" anchor will be updated to "ChildDocument#Chapter1" if the document fragment
     * identified by "Chapter1" has been moved to "ChildDocument" as a result of the split.
     *
     * @param documents the list of documents whose anchors to update
     */
    private void updateAnchors(List<WikiDocument> documents)
    {
        // First we need to collect all the document fragments and map them to their new location.
        Map<String, String> fragments = collectDocumentFragments(documents);

        // Update the anchors.
        for (WikiDocument document : documents) {
            updateAnchors(document, fragments);
        }
    }

    private boolean isDocument(ResourceType resoureceType)
    {
        return ResourceType.DOCUMENT.equals(resoureceType) || ResourceType.SPACE.equals(resoureceType)
            || ResourceType.PAGE.equals(resoureceType);
    }

    /**
     * @param document the document whose anchors to update
     * @param fragments see {@link #collectDocumentFragments(List)}
     */
    private void updateAnchors(WikiDocument document, Map<String, String> fragments)
    {
        for (LinkBlock linkBlock : document.getXdom().<LinkBlock> getBlocks(new ClassBlockMatcher(LinkBlock.class),
            Axes.DESCENDANT)) {
            ResourceReference reference = linkBlock.getReference();
            ResourceType resoureceType = reference.getType();
            String fragment = null;
            if (isDocument(resoureceType) && StringUtils.isEmpty(reference.getReference())) {
                fragment = reference.getParameter(ANCHOR_PARAMETER);
            } else if (StringUtils.startsWith(reference.getReference(), "#")
                && (ResourceType.PATH.equals(resoureceType) || ResourceType.URL.equals(resoureceType))) {
                fragment = reference.getReference().substring(1);
            }

            String targetDocument = fragments.get(fragment);
            if (targetDocument != null && !targetDocument.equals(document.getFullName())) {
                // The fragment has been moved so we need to update the link.
                reference.setType(ResourceType.DOCUMENT);
                reference.setReference(targetDocument);
                reference.setParameter(ANCHOR_PARAMETER, fragment);
            }
        }
    }

    /**
     * Looks for document fragments in the given documents. A document fragment is identified by an {@link IdBlock} for
     * instance.
     *
     * @param documents the list of documents whose fragments to collect
     * @return the collection of document fragments mapped to the document that contains them
     */
    private Map<String, String> collectDocumentFragments(List<WikiDocument> documents)
    {
        Map<String, String> fragments = new HashMap<String, String>();
        for (WikiDocument document : documents) {
            for (IdBlock idBlock : document.getXdom().<IdBlock> getBlocks(new ClassBlockMatcher(IdBlock.class),
                Axes.DESCENDANT)) {
                fragments.put(idBlock.getName(), document.getFullName());
            }
        }
        return fragments;
    }
}
