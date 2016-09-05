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
package org.xwiki.rendering.macro.container;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;

/**
 * Layout manager to handle layouting of a block container, based on a set of parameters. The
 * {@link #layoutContainer(Block)} function should read the container children and modify them accordingly to meet the
 * required layout. Note that although the function can access the whole tree through the passed block, it is
 * recommended that the changes are limited to the container block and its children. Also, usually the container block
 * should be a group block. Parameters are used to pass various parameters required for the layouting (sizes, style
 * options, etc).
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Role
public interface LayoutManager
{
    /**
     * Performs the layout of {@code container}, modifying the blocks inside.
     * <p>
     * TODO: might as well have been with a list of blocks as parameter, but I wanted to mimic the awt LayoutManager
     * interface, which lays out a container and not a list of contents. Reviewer, WDYT?
     * 
     * @param container the block whose contents to layout
     */
    void layoutContainer(Block container);

    /**
     * Sets a parameter needed for the layout.
     * 
     * @param parameterName the name of the parameter to set
     * @param parameterValue the value of the parameter
     */
    void setParameter(String parameterName, Object parameterValue);

    /**
     * @param parameterName the name of the parameter whose value to return
     * @return the value of the parameter identified by {@code parameterName}
     */
    Object getParameter(String parameterName);
}
