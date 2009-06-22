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

/**
 * Provided to {@link VelocityParser} helpers to return some informations.
 * 
 * @version $Id$
 */
public class VelocityParserContext
{
    /**
     * The type of found velocity block.
     * 
     * @version $Id$
     */
    public enum VelocityType
    {
        /**
         * A simple or multilines comment.
         */
        COMMENT,

        /**
         * A Velocity directive (except macros).
         */
        DIRECTIVE,

        /**
         * A Velocity macro.
         */
        MACRO,

        /**
         * Anything starting with a $.
         */
        VAR
    }

    /**
     * Uses to take care of beginning/ending directive to be able to match a whole velocity group (like #if () #end).
     */
    private int velocityDepth;

    /**
     * The type of found velocity block.
     */
    private VelocityType type;

    /**
     * @return indicate the level of the velocity begin/end group
     */
    public int getVelocityDepth()
    {
        return this.velocityDepth;
    }

    /**
     * Increase the level of the velocity begin/end group.
     */
    public void pushVelocityDepth()
    {
        ++this.velocityDepth;
    }

    /**
     * Decrease the level of the velocity begin/end group.
     */
    public void popVelocityDepth()
    {
        --this.velocityDepth;
    }

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
}
