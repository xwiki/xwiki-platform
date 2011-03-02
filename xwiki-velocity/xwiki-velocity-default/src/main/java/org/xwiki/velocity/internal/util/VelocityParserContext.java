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
package org.xwiki.velocity.internal.util;

import java.util.Stack;

import org.xwiki.velocity.internal.util.VelocityBlock.VelocityType;

/**
 * Provided to {@link VelocityParser} helpers to return some information.
 * 
 * @version $Id$
 */
public class VelocityParserContext
{
    /**
     * The type of found velocity block.
     */
    private VelocityType type;

    /**
     * The current blocks.
     */
    private Stack<VelocityBlock> blocks = new Stack<VelocityBlock>();

    /**
     * @param type the type of found velocity block.
     */
    public void setType(VelocityType type)
    {
        this.type = type;
    }

    /**
     * @return The type of found velocity block.
     */
    public VelocityType getType()
    {
        return this.type;
    }

    /**
     * @return the Velocity block in which the process is.
     */
    public VelocityBlock getCurrentElement()
    {
        return this.blocks.peek();
    }

    /**
     * Enter a Velocity block.
     * 
     * @param block the Velocity block in which the process is.
     * @return the Velocity block in which the process is.
     */
    public VelocityBlock pushVelocityElement(VelocityBlock block)
    {
        return this.blocks.push(block);
    }

    /**
     * Go out of a Velocity block.
     * 
     * @return the previous Velocity block in which the process was.
     */
    public VelocityBlock popVelocityElement()
    {
        return this.blocks.pop();
    }

    /**
     * @return indicate if the current process is inside a Velocity block.
     */
    public boolean isInVelocityBlock()
    {
        return !this.blocks.isEmpty();
    }
}
