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

public abstract class AbstractBlock implements Block
{
    private List<Block> childrenBlocks = new ArrayList<Block>();
    
    private Block parentBlock;

    public void addChild(Block block)
    {
        block.setParent(this);
        this.childrenBlocks.add(block);
        block.setParent(this);
    }

    public void addChildren(List<? extends Block> blocks)
    {
        for (Block block: blocks) {
            this.addChild(block);
        }
    }

    public List<Block> getChildren()
    {
        return this.childrenBlocks;
    }

    public Block getParent()
    {
        return this.parentBlock;
    }

    public void setParent(Block parentBlock)
    {
        this.parentBlock = parentBlock;
    }

    public Block getRoot()
    {
        Block block = getParent();
        while (block.getParent() != null) {
            block = block.getParent();
        }
        return block;
    }

    public <T extends Block> List<T> getChildrenByType(Class<T> blockClass)
    {
        List<Block> typedBlocks = new ArrayList<Block>();
        for (Block block: getChildren()) {
            if (blockClass.isAssignableFrom(block.getClass())) {
                typedBlocks.add(block);
            }
            if (!block.getChildren().isEmpty()) {
                typedBlocks.addAll(block.getChildrenByType(blockClass));
            }
        }
        return (List<T>)typedBlocks;
    }
    
    /**
     * {@inheritDoc}
     * @see EqualsBuilder#reflectionEquals(Object, Object)
     */
    public boolean equals(Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);        
    }

    /**
     * {@inheritDoc}
     * @see HashCodeBuilder#reflectionHashCode(Object)
     */
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
