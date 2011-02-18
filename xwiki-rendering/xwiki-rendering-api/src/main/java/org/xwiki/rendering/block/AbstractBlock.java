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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.xwiki.rendering.block.Block.Axes;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.Listener;

/**
 * Implementation for Block operations. All blocks should extend this class. Supports the notion of generic parameters
 * which can be added to a block (see {@link #getParameter(String)} for more details.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractBlock implements Block
{
    /**
     * Store parameters, see {@link #getParameter(String)} for more explanations on what parameters are.
     */
    private Map<String, String> parameters = new LinkedHashMap<String, String>();

    /**
     * The Blocks this Block contains.
     */
    private List<Block> childrenBlocks = new ArrayList<Block>();

    /**
     * The Block containing this Block.
     */
    private Block parentBlock;

    /**
     * The next Sibling Block or null if no next sibling exists.
     */
    private Block nextSiblingBlock;

    /**
     * The previous Sibling Block or null if no previous sibling exists.
     */
    private Block previousSiblingBlock;

    /**
     * Empty constructor to construct an empty block.
     */
    public AbstractBlock()
    {
        // Nothing to do
    }

    /**
     * Construct a block with parameters.
     * 
     * @param parameters the parameters to set
     */
    public AbstractBlock(Map<String, String> parameters)
    {
        this.parameters.putAll(parameters);
    }

    /**
     * Constructs a block with a child block.
     * 
     * @param childBlock the child block of this block
     * @since 3.0M1
     */
    public AbstractBlock(Block childBlock)
    {
        this(childBlock, Collections.<String, String> emptyMap());
    }

    /**
     * Constructs a block with children blocks.
     * 
     * @param childrenBlocks the list of children blocks of the block to construct
     * @since 3.0M1
     */
    public AbstractBlock(List< ? extends Block> childrenBlocks)
    {
        this(childrenBlocks, Collections.<String, String> emptyMap());
    }

    /**
     * Construct a block with a child block and parameters.
     * 
     * @param childBlock the child block of this block
     * @param parameters the parameters to set
     * @since 3.0M1
     */
    public AbstractBlock(Block childBlock, Map<String, String> parameters)
    {
        this(parameters);
        addChild(childBlock);
    }

    /**
     * Construct a block with children blocks and parameters.
     * 
     * @param childrenBlocks the list of children blocks of the block to construct
     * @param parameters the parameters to set
     * @since 3.0M1
     */
    public AbstractBlock(List< ? extends Block> childrenBlocks, Map<String, String> parameters)
    {
        this(parameters);
        addChildren(childrenBlocks);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#addChild(org.xwiki.rendering.block.Block)
     */
    public void addChild(Block blockToAdd)
    {
        insertChildAfter(blockToAdd, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#addChildren(java.util.List)
     */
    public void addChildren(List< ? extends Block> blocksToAdd)
    {
        for (Block blockToAdd : blocksToAdd) {
            addChild(blockToAdd);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Block#setNextSiblingBlock(Block)
     * @since 2.6RC1
     */
    public void setNextSiblingBlock(Block nextSiblingBlock)
    {
        this.nextSiblingBlock = nextSiblingBlock;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Block#setPreviousSiblingBlock(Block)
     * @since 2.6RC1
     */
    public void setPreviousSiblingBlock(Block previousSiblingBlock)
    {
        this.previousSiblingBlock = previousSiblingBlock;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#insertChildBefore(org.xwiki.rendering.block.Block,
     *      org.xwiki.rendering.block.Block)
     */
    public void insertChildBefore(Block blockToInsert, Block nextBlock)
    {
        blockToInsert.setParent(this);

        if (nextBlock == null) {
            // Last block becomes last but one
            if (!this.childrenBlocks.isEmpty()) {
                Block lastBlock = this.childrenBlocks.get(this.childrenBlocks.size() - 1);
                blockToInsert.setPreviousSiblingBlock(lastBlock);
                lastBlock.setNextSiblingBlock(blockToInsert);
            } else {
                blockToInsert.setPreviousSiblingBlock(null);
            }
            blockToInsert.setNextSiblingBlock(null);
            this.childrenBlocks.add(blockToInsert);
        } else {
            // If there's a previous block to nextBlock then get it to set its next sibling
            Block previousBlock = nextBlock.getPreviousSibling();
            if (previousBlock != null) {
                previousBlock.setNextSiblingBlock(blockToInsert);
                blockToInsert.setPreviousSiblingBlock(previousBlock);
            } else {
                blockToInsert.setPreviousSiblingBlock(null);
            }
            blockToInsert.setNextSiblingBlock(nextBlock);
            nextBlock.setPreviousSiblingBlock(blockToInsert);
            this.childrenBlocks.add(indexOfChild(nextBlock), blockToInsert);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#insertChildAfter(org.xwiki.rendering.block.Block,
     *      org.xwiki.rendering.block.Block)
     */
    public void insertChildAfter(Block blockToInsert, Block previousBlock)
    {
        if (previousBlock == null) {
            insertChildBefore(blockToInsert, null);
        } else {
            // If there's a next block to previousBlock then get it to set its previous sibling
            Block nextBlock = previousBlock.getNextSibling();
            if (nextBlock != null) {
                nextBlock.setPreviousSiblingBlock(blockToInsert);
                blockToInsert.setNextSiblingBlock(nextBlock);
            } else {
                blockToInsert.setNextSiblingBlock(null);
            }
            blockToInsert.setPreviousSiblingBlock(previousBlock);
            previousBlock.setNextSiblingBlock(blockToInsert);
            this.childrenBlocks.add(indexOfChild(previousBlock) + 1, blockToInsert);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#replaceChild(Block, Block)
     */
    public void replaceChild(Block newBlock, Block oldBlock)
    {
        replaceChild(Collections.singletonList(newBlock), oldBlock);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#replaceChild(List, Block)
     */
    public void replaceChild(List<Block> newBlocks, Block oldBlock)
    {
        int position = indexOfChild(oldBlock);

        if (position == -1) {
            throw new InvalidParameterException("Provided Block to replace is not a child");
        }

        List<Block> blocks = getChildren();

        // Remove old child
        blocks.remove(position);
        oldBlock.setParent(null);

        // Insert new children
        Block previousBlock = oldBlock.getPreviousSibling();
        if (newBlocks.isEmpty()) {
            previousBlock.setNextSiblingBlock(oldBlock.getNextSibling());
        }
        Block lastBlock = null;
        for (Block block : newBlocks) {
            block.setParent(this);
            block.setPreviousSiblingBlock(previousBlock);
            if (previousBlock != null) {
                previousBlock.setNextSiblingBlock(block);
            }
            previousBlock = block;
            lastBlock = block;
        }
        Block nextBlock = oldBlock.getNextSibling();
        if (nextBlock != null) {
            nextBlock.setPreviousSiblingBlock(lastBlock);
        }
        if (lastBlock != null) {
            lastBlock.setNextSiblingBlock(nextBlock);
        }

        blocks.addAll(position, newBlocks);

        oldBlock.setNextSiblingBlock(null);
        oldBlock.setPreviousSiblingBlock(null);
    }

    /**
     * Get the position of the provided block in the list of children.
     * <p>
     * Can't use {@link List#indexOf(Object)} since it's using {@link Object#equals(Object)} internally which is not
     * what we want since two WordBlock with the same text or two spaces are equals for example but we want to be able
     * to target one specific Block.
     * 
     * @param block the block
     * @return the position of the block, -1 if the block can't be found
     */
    private int indexOfChild(Block block)
    {
        return indexOfBlock(block, getChildren());
    }

    /**
     * Get the position of the provided block in the provided list of blocks.
     * <p>
     * Can't use {@link List#indexOf(Object)} since it's using {@link Object#equals(Object)} internally which is not
     * what we want since two WordBlock with the same text or two spaces are equals for example but we want to be able
     * to target one specific Block.
     * 
     * @param block the block for which to find the position
     * @param blocks the list of blocks in which to look for the passed block
     * @return the position of the block, -1 if the block can't be found
     */
    private int indexOfBlock(Block block, List<Block> blocks)
    {
        int position = 0;

        for (Block child : blocks) {
            if (child == block) {
                return position;
            }
            ++position;
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#getChildren()
     */
    public List<Block> getChildren()
    {
        return this.childrenBlocks;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#getParent()
     */
    public Block getParent()
    {
        return this.parentBlock;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#getParameters()
     */
    public Map<String, String> getParameters()
    {
        return Collections.unmodifiableMap(this.parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#getParameter(java.lang.String)
     */
    public String getParameter(String name)
    {
        return this.parameters.get(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#setParameter(java.lang.String, java.lang.String)
     */
    public void setParameter(String name, String value)
    {
        this.parameters.put(name, value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#setParameters(java.util.Map)
     * @since 1.7M2
     */
    public void setParameters(Map<String, String> parameters)
    {
        this.parameters.putAll(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#setParent(org.xwiki.rendering.block.Block)
     */
    public void setParent(Block parentBlock)
    {
        this.parentBlock = parentBlock;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#getRoot()
     */
    public Block getRoot()
    {
        Block block = this;

        while (block.getParent() != null) {
            block = block.getParent();
        }

        return block;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Block#getNextSibling()
     * @since 2.6RC1
     */
    public Block getNextSibling()
    {
        return this.nextSiblingBlock;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Block#getPreviousSibling()
     * @since 2.6RC1
     */
    public Block getPreviousSibling()
    {
        return this.previousSiblingBlock;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#removeBlock(Block)
     * @since 2.6RC1
     */
    public void removeBlock(Block childBlockToRemove)
    {
        getChildren().remove(childBlockToRemove);
        if (childBlockToRemove != null) {
            Block previousBlock = childBlockToRemove.getPreviousSibling();
            if (previousBlock != null) {
                previousBlock.setNextSiblingBlock(childBlockToRemove.getNextSibling());
            }
            Block nextBlock = childBlockToRemove.getNextSibling();
            if (nextBlock != null) {
                nextBlock.setPreviousSiblingBlock(previousBlock);
            }
            childBlockToRemove.setNextSiblingBlock(null);
            childBlockToRemove.setPreviousSiblingBlock(null);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see EqualsBuilder#reflectionEquals(Object, Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HashCodeBuilder#reflectionHashCode(Object)
     */
    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#clone()
     */
    @Override
    public Block clone()
    {
        return clone(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#clone(org.xwiki.rendering.block.BlockFilter)
     * @since 1.8RC2
     */
    public Block clone(BlockFilter blockFilter)
    {
        Block block;
        try {
            block = (AbstractBlock) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new RuntimeException("Failed to clone object", e);
        }

        ((AbstractBlock) block).parameters = new LinkedHashMap<String, String>(this.parameters);

        ((AbstractBlock) block).childrenBlocks = new ArrayList<Block>(this.childrenBlocks.size());
        for (Block childBlock : this.childrenBlocks) {
            if (blockFilter != null) {
                Block clonedChildBlocks = childBlock.clone(blockFilter);

                List<Block> filteredBlocks = blockFilter.filter(clonedChildBlocks);

                if (filteredBlocks.size() == 0) {
                    filteredBlocks = clonedChildBlocks.getChildren();
                }

                block.addChildren(filteredBlocks);
            } else {
                block.addChild(childBlock.clone());
            }
        }

        return block;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractBlock#traverse(org.xwiki.rendering.listener.Listener)
     */
    public void traverse(Listener listener)
    {
        before(listener);

        for (Block block : getChildren()) {
            block.traverse(listener);
        }

        after(listener);
    }

    /**
     * Send {@link org.xwiki.rendering.listener.Listener} events corresponding to the start of the block. For example
     * for a Bold block, this allows an XHTML Listener (aka a Renderer) to output <code>&lt;b&gt;</code>.
     * 
     * @param listener the listener that will receive the events sent by this block before its children blocks have
     *            emitted their own events.
     */
    public void before(Listener listener)
    {
        // Do nothing by default, should be overridden by extending Blocks
    }

    /**
     * Send {@link Listener} events corresponding to the end of the block. For example for a Bold block, this allows an
     * XHTML Listener (aka a Renderer) to output <code>&lt;/b&gt;</code>.
     * 
     * @param listener the listener that will receive the events sent by this block before its children blocks have
     *            emitted their own events.
     */
    public void after(Listener listener)
    {
        // Do nothing by default, should be overridden by extending Blocks
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#getBlocks(org.xwiki.rendering.block.match.BlockMatcher,
     *      org.xwiki.rendering.block.Block.Axes)
     */
    public List<Block> getBlocks(BlockMatcher matcher, Axes axes)
    {
        List<Block> blocks = null;

        if (axes == Axes.SELF) {
            blocks = addBlock(this, matcher, blocks);
        } else if (axes.compareTo(Axes.ANCESTOR_OR_SELF) <= 0) {
            blocks = getAncestorBlocks(matcher, axes);
        } else if (axes.compareTo(Axes.DESCENDANT_OR_SELF) <= 0) {
            blocks = getDescendantBlocks(matcher, axes);
        } else {
            blocks = getSiblingBlocks(matcher, axes);
        }

        return blocks != null ? blocks : Collections.<Block> emptyList();
    }

    /**
     * Get all blocks following provided {@link BlockMatcher} and ancestor {@link Axes}.
     * 
     * @param matcher filter the blocks to return
     * @param axes indicate the search axes
     * @return the matched {@link Block}s, empty list of none was found
     */
    private List<Block> getAncestorBlocks(BlockMatcher matcher, Axes axes)
    {
        List<Block> blocks = null;

        Block nextBlock = getParent();
        Axes nextAxes = axes;

        switch (axes) {
            case ANCESTOR_OR_SELF:
                blocks = addBlock(this, matcher, blocks);
                break;
            case ANCESTOR:
                nextAxes = Axes.ANCESTOR_OR_SELF;
                break;
            case PARENT:
                nextAxes = Axes.SELF;
                break;
            default:
                break;
        }

        if (nextBlock != null) {
            blocks = getBlocks(nextBlock, matcher, nextAxes, blocks);
        }

        return blocks != null ? blocks : Collections.<Block> emptyList();
    }

    /**
     * Get all blocks following provided {@link BlockMatcher} and descendant {@link Axes}.
     * 
     * @param matcher filter the blocks to return
     * @param axes indicate the search axes
     * @return the matched {@link Block}s, empty list of none was found
     */
    private List<Block> getDescendantBlocks(BlockMatcher matcher, Axes axes)
    {
        List<Block> blocks = null;

        Block nextBlock = null;
        Axes nextAxes = axes;

        switch (axes) {
            case CHILD:
                if (!this.childrenBlocks.isEmpty()) {
                    nextBlock = this.childrenBlocks.get(0);
                    nextAxes = Axes.FOLLOWING_SIBLING;
                    blocks = addBlock(nextBlock, matcher, blocks);
                }
                break;
            case DESCENDANT_OR_SELF:
                blocks = addBlock(this, matcher, blocks);
                blocks = getBlocks(this.childrenBlocks, matcher, Axes.DESCENDANT_OR_SELF, blocks);
                break;
            case DESCENDANT:
                blocks = getBlocks(this.childrenBlocks, matcher, Axes.DESCENDANT_OR_SELF, blocks);
                break;
            default:
                break;
        }

        if (nextBlock != null) {
            blocks = getBlocks(nextBlock, matcher, nextAxes, blocks);
        }

        return blocks != null ? blocks : Collections.<Block> emptyList();
    }

    /**
     * Get all blocks following provided {@link BlockMatcher} and following/preceding sibling {@link Axes}.
     * 
     * @param matcher filter the blocks to return
     * @param axes indicate the search axes
     * @return the matched {@link Block}s, empty list of none was found
     */
    private List<Block> getSiblingBlocks(BlockMatcher matcher, Axes axes)
    {
        List<Block> blocks = null;

        Block nextBlock = null;
        Axes nextAxes = axes;

        switch (axes) {
            // FOLLOWING
            case FOLLOWING_SIBLING:
                nextBlock = getNextSibling();
                blocks = addBlock(nextBlock, matcher, blocks);
                break;
            case FOLLOWING:
                for (Block nextSibling = getNextSibling(); nextSibling != null; nextSibling =
                        nextSibling.getNextSibling()) {
                    blocks = getBlocks(nextSibling, matcher, Axes.DESCENDANT_OR_SELF, blocks);
                }
                break;
            // PRECEDING
            case PRECEDING_SIBLING:
                nextBlock = getPreviousSibling();
                blocks = addBlock(nextBlock, matcher, blocks);
                break;
            case PRECEDING:
                for (Block previousSibling = getPreviousSibling(); previousSibling != null; previousSibling =
                        previousSibling.getPreviousSibling()) {
                    blocks = getBlocks(previousSibling, matcher, Axes.DESCENDANT_OR_SELF, blocks);
                }
                break;
            default:
                break;
        }

        if (nextBlock != null) {
            blocks = getBlocks(nextBlock, matcher, nextAxes, blocks);
        }

        return blocks != null ? blocks : Collections.<Block> emptyList();
    }

    /**
     * Add provided {@link Block} to provided list (or create list of null) if block validate the provided
     * {@link BlockMatcher}.
     * 
     * @param block the block
     * @param matcher the matcher
     * @param blocks the list of blocks to fill
     * @return the modified list, null if provided list is null and provided {@link Block} does not validate provided
     *         {@link BlockMatcher}
     */
    private List<Block> addBlock(Block block, BlockMatcher matcher, List<Block> blocks)
    {
        List<Block> newBlocks = blocks;

        if (block != null && matcher.match(block)) {
            if (newBlocks == null) {
                newBlocks = new ArrayList<Block>();
            }
            newBlocks.add(block);
        }

        return newBlocks;
    }

    /**
     * Add all blocks following provided {@link BlockMatcher} and {@link Axes} in the provide list (or create a new list
     * of provided list is null).
     * 
     * @param blocks the blocks from where to search
     * @param matcher the block matcher
     * @param axes the axes
     * @param blocksOut the list of blocks to fill
     * @return the modified list, null if provided list is null and provided {@link Block} does not validate provided
     *         {@link BlockMatcher}
     */
    private List<Block> getBlocks(List<Block> blocks, BlockMatcher matcher, Axes axes, List<Block> blocksOut)
    {
        List<Block> newBlocks = blocksOut;

        for (Block child : blocks) {
            newBlocks = getBlocks(child, matcher, axes, newBlocks);
        }

        return newBlocks;
    }

    /**
     * Add all blocks following provided {@link BlockMatcher} and {@link Axes} in the provide list (or create a new list
     * of provided list is null).
     * 
     * @param block the block from where to search
     * @param matcher the block matcher
     * @param axes the axes
     * @param blocksOut the list of blocks to fill
     * @return the modified list, null if provided list is null and provided {@link Block} does not validate provided
     *         {@link BlockMatcher}
     */
    private List<Block> getBlocks(Block block, BlockMatcher matcher, Axes axes, List<Block> blocksOut)
    {
        List<Block> newBlocks = blocksOut;

        List<Block> nextBlocks = block.getBlocks(matcher, axes);
        if (!nextBlocks.isEmpty()) {
            if (newBlocks == null) {
                newBlocks = nextBlocks;
            } else {
                newBlocks.addAll(nextBlocks);
            }
        }

        return newBlocks;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#getFirstBlock(org.xwiki.rendering.block.match.BlockMatcher,
     *      org.xwiki.rendering.block.Block.Axes)
     */
    public Block getFirstBlock(BlockMatcher matcher, Axes axes)
    {
        Block block = null;

        if (axes == Axes.SELF) {
            if (matcher.match(this)) {
                block = this;
            }
        } else if (axes.compareTo(Axes.ANCESTOR_OR_SELF) <= 0) {
            block = getFirstAncestorBlock(matcher, axes);
        } else if (axes.compareTo(Axes.DESCENDANT_OR_SELF) <= 0) {
            block = getFirstDescendantBlock(matcher, axes);
        } else if (axes.compareTo(Axes.FOLLOWING_SIBLING) <= 0) {
            block = getFirstFollowingSiblingBlock(matcher, axes);
        } else {
            block = getFirstPrecedingSiblingBlock(matcher, axes);
        }

        return block;
    }

    /**
     * Get the first matched block in the provided ancestor {@link Axes}.
     * 
     * @param matcher the block matcher
     * @param axes the axes
     * @return the matched {@link Block}, null if none was found
     */
    private Block getFirstAncestorBlock(BlockMatcher matcher, Axes axes)
    {
        Block nextBlock = null;
        Axes nextAxes = axes;

        switch (axes) {
            case ANCESTOR_OR_SELF:
                if (matcher.match(this)) {
                    return this;
                }
            case ANCESTOR:
            case PARENT:
                nextAxes = axes == Axes.PARENT ? Axes.SELF : Axes.ANCESTOR_OR_SELF;
                nextBlock = getParent();
                break;
            default:
                break;
        }

        return nextBlock != null ? nextBlock.getFirstBlock(matcher, nextAxes) : null;
    }

    /**
     * Get the first matched block in the provided descendant {@link Axes}.
     * 
     * @param matcher the block matcher
     * @param axes the axes
     * @return the matched {@link Block}, null if none was found
     */
    private Block getFirstDescendantBlock(BlockMatcher matcher, Axes axes)
    {
        Block nextBlock = null;
        Axes nextAxes = axes;

        switch (axes) {
            case CHILD:
                if (!this.childrenBlocks.isEmpty()) {
                    nextBlock = this.childrenBlocks.get(0);
                    nextAxes = Axes.FOLLOWING_SIBLING;
                    if (matcher.match(nextBlock)) {
                        return nextBlock;
                    }
                }
                break;
            case DESCENDANT_OR_SELF:
                if (matcher.match(this)) {
                    return this;
                }
            case DESCENDANT:
                for (Block child : this.childrenBlocks) {
                    Block matchedBlock = child.getFirstBlock(matcher, Axes.DESCENDANT_OR_SELF);
                    if (matchedBlock != null) {
                        return matchedBlock;
                    }
                }
                break;
            default:
                break;
        }

        return nextBlock != null ? nextBlock.getFirstBlock(matcher, nextAxes) : null;
    }

    /**
     * Get the first matched block in the provided following sibling {@link Axes}.
     * 
     * @param matcher the block matcher
     * @param axes the axes
     * @return the matched {@link Block}, null if none was found
     */
    private Block getFirstFollowingSiblingBlock(BlockMatcher matcher, Axes axes)
    {
        Block nextBlock = null;
        Axes nextAxes = axes;

        switch (axes) {
            case FOLLOWING_SIBLING:
                nextBlock = getNextSibling();
                if (nextBlock != null && matcher.match(nextBlock)) {
                    return nextBlock;
                }
                break;
            case FOLLOWING:
                for (Block nextSibling = getNextSibling(); nextSibling != null; nextSibling =
                        nextSibling.getNextSibling()) {
                    Block matchedBlock = nextSibling.getFirstBlock(matcher, Axes.DESCENDANT_OR_SELF);
                    if (matchedBlock != null) {
                        return matchedBlock;
                    }
                }
                break;
            default:
                break;
        }

        return nextBlock != null ? nextBlock.getFirstBlock(matcher, nextAxes) : null;
    }

    /**
     * Get the first matched block in the provided preceding sibling {@link Axes}.
     * 
     * @param matcher the block matcher
     * @param axes the axes
     * @return the matched {@link Block}, null if none was found
     */
    private Block getFirstPrecedingSiblingBlock(BlockMatcher matcher, Axes axes)
    {
        Block nextBlock = null;
        Axes nextAxes = axes;

        switch (axes) {
            case PRECEDING_SIBLING:
                nextBlock = getPreviousSibling();
                if (nextBlock != null && matcher.match(nextBlock)) {
                    return nextBlock;
                }
                break;
            case PRECEDING:
                for (Block previousSibling = getPreviousSibling(); previousSibling != null; previousSibling =
                        previousSibling.getPreviousSibling()) {
                    Block matchedBlock = previousSibling.getFirstBlock(matcher, Axes.DESCENDANT_OR_SELF);
                    if (matchedBlock != null) {
                        return matchedBlock;
                    }
                }
                break;
            default:
                break;
        }

        return nextBlock != null ? nextBlock.getFirstBlock(matcher, nextAxes) : null;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#getChildrenByType(java.lang.Class, boolean)
     */
    @Deprecated
    public <T extends Block> List<T> getChildrenByType(Class<T> blockClass, boolean recurse)
    {
        return (List<T>) getBlocks(new ClassBlockMatcher(blockClass), Axes.DESCENDANT);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#getPreviousBlockByType(java.lang.Class, boolean)
     */
    @Deprecated
    public <T extends Block> T getPreviousBlockByType(Class<T> blockClass, boolean recurse)
    {
        // Don't use #getFirstBlock(BlockMatcher, Axes) for retro-compatibility because it's a bit different:
        // #getFirstBlock follows XPATH axes specifications and does not include "ancestors" in "preceding" axis

        if (getParent() == null) {
            return null;
        }

        int index = indexOfBlock(this, getParent().getChildren());

        // test previous brothers
        List<Block> blocks = getParent().getChildren();
        for (int i = index - 1; i >= 0; --i) {
            Block previousBlock = blocks.get(i);
            if (blockClass.isAssignableFrom(previousBlock.getClass())) {
                return blockClass.cast(previousBlock);
            }
        }

        // test parent
        if (blockClass.isAssignableFrom(getParent().getClass())) {
            return blockClass.cast(getParent());
        }

        // recurse
        return recurse ? getParent().getPreviousBlockByType(blockClass, true) : null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#getParentBlockByType(java.lang.Class)
     */
    @Deprecated
    public <T extends Block> T getParentBlockByType(Class<T> blockClass)
    {
        return (T) getFirstBlock(new ClassBlockMatcher(blockClass), Axes.ANCESTOR);
    }
}
