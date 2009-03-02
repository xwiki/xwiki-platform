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
     * Let the block send {@link Listener} events corresponding to its content. For example a Paragraph block will send
     * the {@link org.xwiki.rendering.listener.Listener#beginParagraph()} and
     * {@link org.xwiki.rendering.listener.Listener#endParagraph()} events when this method is called.
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
     * Replaces the current block with the list of passed blocks.
     * 
     * @param newBlocks the new blocks to replace the current block with
     */
    void replace(List<Block> newBlocks);

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
     * Gets all the Blocks in the tree which are of the passed Block class.
     * 
     * @param <T> the class of the Blocks to return
     * @param blockClass the block class to look for
     * @param recurse if true also search recursively children
     * @return all the matching blocks
     * @since 1.6M1
     */
    <T extends Block> List<T> getChildrenByType(Class<T> blockClass, boolean recurse);

    /**
     * Look forward to find a block which inherit or is provided type.
     * 
     * @param <T> the class of the Blocks to return
     * @param blockClass the block class to look for
     * @param recurse if true also search in parents levels
     * @return the found block, null if nothing is found
     * @since 1.6M1
     */
    <T extends Block> T getPreviousBlockByType(Class<T> blockClass, boolean recurse);

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
}
