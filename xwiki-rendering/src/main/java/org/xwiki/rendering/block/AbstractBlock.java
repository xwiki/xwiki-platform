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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractBlock implements Block
{
    /**
     * The Blocks this Block contains.
     */
    private List<Block> childrenBlocks = new ArrayList<Block>();

    /**
     * The Block containing this Block.
     */
    private Block parentBlock;

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
            this.addChild(blockToAdd);
        }
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
            this.childrenBlocks.add(blockToInsert);
        } else {
            this.childrenBlocks.add(this.childrenBlocks.indexOf(nextBlock), blockToInsert);
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
        blockToInsert.setParent(this);

        if (previousBlock == null) {
            this.childrenBlocks.add(blockToInsert);
        } else {
            this.childrenBlocks.add(this.childrenBlocks.indexOf(previousBlock) + 1, blockToInsert);
        }
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
     * @see org.xwiki.rendering.block.Block#getChildrenByType(java.lang.Class)
     */
    public <T extends Block> List<T> getChildrenByType(Class<T> blockClass)
    {
        List<Block> typedBlocks = new ArrayList<Block>();
        for (Block block : getChildren()) {
            if (blockClass.isAssignableFrom(block.getClass())) {
                typedBlocks.add(block);
            }
            if (!block.getChildren().isEmpty()) {
                typedBlocks.addAll(block.getChildrenByType(blockClass));
            }
        }
        return (List<T>) typedBlocks;
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
}
