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
 * A Velocity element (can be a velocity macro, variable, etc.).
 * 
 * @version $Id$
 */
public class VelocityBlock
{
    /**
     * The type of found Velocity element.
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
     * The name of the Velocity element (if, macro, ...).
     */
    private String name;

    /**
     * The of the Velocity element.
     */
    private VelocityType type;

    /**
     * @param name the name of the Velocity element (if, macro, ...).
     * @param type the type of the Velocity element.
     */
    public VelocityBlock(String name, VelocityType type)
    {
        this.name = name;
        this.type = type;
    }

    /**
     * @return the name of the Velocity element (if, macro, ...).
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name the name of the Velocity element (if, macro, ...).
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the type of the Velocity element.
     */
    public VelocityType getType()
    {
        return type;
    }

    /**
     * @param type the type of the Velocity element.
     */
    public void setType(VelocityType type)
    {
        this.type = type;
    }
}
