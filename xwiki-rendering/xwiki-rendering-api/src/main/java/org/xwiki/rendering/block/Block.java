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

import java.util.List;
import java.util.Map;

import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.listener.Listener;

/**
 * Represents an element of a XWiki Document's content. For example there are Blocks for Paragraphs, Bold parts,
 * Sections, Links, etc. A block has a parent and can have children too for Blocks which are wrapper around other blocks
 * (e.g. Paragraph blocks, List blocks, Bold blocks).
 * 
 * @version $Id$
 * @since 1.5M2
 */
public interface Block extends Cloneable
{
    /**
     * Search axes used in searching methods. Mostly taken from XPATH axes.
     * 
     * @version $Id$
     * @since 3.0M3
     */
    enum Axes
    {

        /** Just the context block itself. */
        SELF,

        /** The parent of the context block, if there is one. */
        PARENT,

        /**
         * The ancestors of the context block; the ancestors of the context block consist of the parent of context block
         * and the parent's parent and so on; thus, the ancestor axis will always include the root block, unless the
         * context block is the root block.
         */
        ANCESTOR,

        /**
         * The context block and the ancestors of the context block; thus, the ancestor axis will always include the
         * root block.
         */
        ANCESTOR_OR_SELF,

        /** The children of the context block. */
        CHILD,

        /** The descendants of the context block; a descendant is a child or a child of a child and so on. */
        DESCENDANT,

        /** The context block and the descendants of the context block. */
        DESCENDANT_OR_SELF,

        /**
         * All blocks in the same document as the context block that are after the context block in document order,
         * excluding any descendants.
         */
        FOLLOWING,

        /** All the following siblings of the context block. */
        FOLLOWING_SIBLING,

        /**
         * All blocks in the same document as the context block that are before the context block in document order,
         * excluding any ancestors.
         */
        PRECEDING,

        /** All the preceding siblings of the context block. */
        PRECEDING_SIBLING
    }

    /**
     * Let the block send {@link Listener} events corresponding to its content. For example a Paragraph block will send
     * the {@link org.xwiki.rendering.listener.Listener#beginParagraph} and
     * {@link org.xwiki.rendering.listener.Listener#endParagraph} events when this method is called.
     * 
     * @param listener the listener to which to send the events to.
     */
    void traverse(Listener listener);

    /**
     * Helper method to add a single child block to the end of the children list of the current block. For adding
     * several blocks at once use {@link #addChildren(java.util.List)}.
     * 
     * @param blockToAdd the child block to add
     */
    void addChild(Block blockToAdd);

    /**
     * Adds several children blocks to the end of the children list of the current block. For example a bold sentence is
     * made up of a Bold block to which the different words making up the text have been added to.
     * 
     * @param blocksToAdd the children blocks to add
     */
    void addChildren(List< ? extends Block> blocksToAdd);

    /**
     * Helper method to add a single child block to the current block before the provided existing child block. For
     * adding several blocks at once use {@link #addChildren(java.util.List)}.
     * 
     * @param blockToInsert the child block to add
     * @param nextBlock the child block that will be just after the added block
     * @since 1.6M1
     */
    void insertChildBefore(Block blockToInsert, Block nextBlock);

    /**
     * Helper method to add a single child block to the current block after the provided existing child block. For
     * adding several blocks at once use {@link #addChildren(java.util.List)}.
     * 
     * @param blockToInsert the child block to add
     * @param previousBlock the child block that will be just before the added block
     * @since 1.6M1
     */
    void insertChildAfter(Block blockToInsert, Block previousBlock);

    /**
     * Replaces an existing children block with the passed new block. Also sets the new block's parent to be the current
     * block.
     * 
     * @param newBlock the new block to replace the old block with
     * @param oldBlock the block to replace with the new block
     */
    void replaceChild(Block newBlock, Block oldBlock);

    /**
     * Replaces an existing children block with the passed new blocks. Also sets the new block's parents to be the
     * current block.
     * 
     * @param newBlocks the new blocks to replace the old block with
     * @param oldBlock the block to replace with the new blocks
     */
    void replaceChild(List<Block> newBlocks, Block oldBlock);

    /**
     * Get the parent block. All blocks have a parent and the top level parent is the {@link XDOM} object.
     * 
     * @return the parent block
     */
    Block getParent();

    /**
     * Sets the parent block.
     * 
     * @param parentBlock the parent block
     */
    void setParent(Block parentBlock);

    /**
     * Gets all children blocks.
     * 
     * @return the children blocks
     * @see #addChildren(java.util.List)
     */
    List<Block> getChildren();

    /**
     * Gets the top level Block. If the current block is the top level Block, it return itself.
     * 
     * @return the top level Block
     */
    Block getRoot();

    /**
     * Removes a Block.
     * 
     * @param childBlockToRemove the child block to remove
     * @since 2.6RC1
     */
    void removeBlock(Block childBlockToRemove);

    /**
     * @return the next sibling block or null if there's no next sibling
     * @since 2.6RC1
     */
    Block getNextSibling();

    /**
     * @param nextSiblingBlock see {@link #getNextSibling()}
     * @since 2.6RC1
     */
    void setNextSiblingBlock(Block nextSiblingBlock);

    /**
     * @return the previous sibling block or null if there's no previous sibling
     * @since 2.6RC1
     */
    Block getPreviousSibling();

    /**
     * @param previousSiblingBlock see {@link #getPreviousSibling()} ()}
     * @since 2.6RC1
     */
    void setPreviousSiblingBlock(Block previousSiblingBlock);

    /**
     * Return a copy of the block with filtered children.
     * 
     * @param blockFilter the Block filter.
     * @return the filtered Block.
     * @since 1.8RC2
     */
    Block clone(BlockFilter blockFilter);

    /**
     * {@inheritDoc}
     * 
     * @see Object#clone()
     */
    Block clone();

    /**
     * @return all parameters
     * @since 3.0M1
     */
    Map<String, String> getParameters();

    /**
     * A Parameter is a generic key/value which can be used to add metadata to a block. What is done with the metadata
     * depends on the Renderer's implementations. For example the XHTML Renderer adds them as Element attributes.
     * 
     * @param name the name of the parameter to return
     * @return the parameter or null if the parameter doesn't exist
     * @since 3.0M1
     */
    String getParameter(String name);

    /**
     * Set a parameter on the current block. See {@link #getParameter(String)} for more details.
     * 
     * @param name the parameter's name
     * @param value the parameter's value
     * @since 3.0M1
     */
    void setParameter(String name, String value);

    /**
     * Set several parameters at once.
     * 
     * @param parameters the parameters to set
     * @see #getParameter(String)
     * @since 3.0M1
     */
    void setParameters(Map<String, String> parameters);

    /**
     * Get all blocks following provided {@link BlockMatcher} and {@link Axes}.
     * 
     * @param matcher filter the blocks to return
     * @param axes indicate the search axes
     * @return the matched {@link Block}s, empty list of none was found
     * @since 3.0M3
     */
    List<Block> getBlocks(BlockMatcher matcher, Axes axes);

    /**
     * Get the first matched block in the provided {@link Axes}.
     * 
     * @param matcher indicate which block to stop to
     * @param axes indicate the search axes
     * @return the matched {@link Block}, null if none was found
     * @since 3.0M3
     */
    Block getFirstBlock(BlockMatcher matcher, Axes axes);

    /**
     * Gets all the Blocks in the tree which are of the passed Block class.
     * 
     * @param <T> the class of the Blocks to return
     * @param blockClass the block class to look for
     * @param recurse if true also search recursively children
     * @return all the matching blocks
     * @since 1.6M1
     * @deprecated since 3.0M3 use {@link #getBlocks(BlockMatcher, Axes)} instead
     */
    @Deprecated
    <T extends Block> List<T> getChildrenByType(Class<T> blockClass, boolean recurse);

    /**
     * Look upward to find a block which inherit or is provided type.
     * <p>
     * The difference with {@link #getParentBlockByType(Class)} is that this one look also at previous block in the same
     * parent when {@link #getParentBlockByType(Class)} only look at parents.
     * 
     * @param <T> the class of the Blocks to return
     * @param blockClass the block class to look for
     * @param recurse if true also search in parents levels
     * @return the found block, null if nothing is found
     * @since 1.6M1
     * @deprecated since 3.0M3 use {@link #getBlocks(BlockMatcher, Axes)} instead
     */
    @Deprecated
    <T extends Block> T getPreviousBlockByType(Class<T> blockClass, boolean recurse);

    /**
     * Recursively look at parents to find a block which inherits or is provided type.
     * <p>
     * The difference with {@link #getPreviousBlockByType(Class, boolean)} is that this one only look at parent when
     * {@link #getPreviousBlockByType(Class, boolean)} look at previous block in the same parent.
     * 
     * @param <T> the class of the Blocks to return
     * @param blockClass the block class to look for
     * @return the found block, null if nothing is found
     * @since 1.9.1
     * @deprecated since 3.0M3 use {@link #getBlocks(BlockMatcher, Axes)} instead
     */
    @Deprecated
    <T extends Block> T getParentBlockByType(Class<T> blockClass);
}
