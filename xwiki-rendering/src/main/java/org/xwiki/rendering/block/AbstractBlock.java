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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Implementation for Block operations. All blocks should extend this class.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractBlock implements Block
{
    private Map<String, String> parameters = new LinkedHashMap<String, String>();

    public AbstractBlock()
    {
        // Nothing to do
    }

    public AbstractBlock(Map<String, String> parameters)
    {
        this.parameters.putAll(parameters);
    }

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

    public Map<String, String> getParameters()
    {
        return Collections.unmodifiableMap(this.parameters);
    }

    public String getParameter(String name)
    {
        return this.parameters.get(name);
    }

    public void setParameter(String name, String value)
    {
        this.parameters.put(name, value);
    }

    public void setParameter(String name, Object value)
    {
        setParameter(name, ConvertUtils.convert(value));
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
     * @see org.xwiki.rendering.block.Block#getChildrenByType(java.lang.Class, boolean)
     */
    public <T extends Block> List<T> getChildrenByType(Class<T> blockClass, boolean recurse)
    {
        List<Block> typedBlocks = new ArrayList<Block>();
        for (Block block : getChildren()) {
            if (blockClass.isAssignableFrom(block.getClass())) {
                typedBlocks.add(block);
            }
            if (recurse && !block.getChildren().isEmpty()) {
                typedBlocks.addAll(block.getChildrenByType(blockClass, true));
            }
        }

        return (List<T>) typedBlocks;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#getPreviousBlockByType(java.lang.Class, boolean)
     */
    public <T extends Block> T getPreviousBlockByType(Class<T> blockClass, boolean recurse)
    {
        if (getParent() == null) {
            return null;
        }

        List<Block> blocks = getParent().getChildren();
        int index = blocks.indexOf(this);

        for (int i = index - 1; i >= 0; --i) {
            Block previousBlock = blocks.get(i);
            if (previousBlock instanceof SectionBlock) {
                return (T) previousBlock;
            }
        }

        return getParent().getPreviousBlockByType(blockClass, true);
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
