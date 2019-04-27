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
package org.xwiki.rendering.internal.code.layout;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;
import org.xwiki.stability.Unstable;

/**
 * Arranges a list of code rendering blocks into a specific layout.
 * 
 * @version $Id$
 * @since 11.4RC1
 */
@Role
public interface CodeLayoutHandler
{
    /**
     * @param blocks the code blocks
     * @param originalContent the original code content, may provide useful information
     * @return the result of applying the layout to the code blocks
     */
    List<Block> layout(List<Block> blocks, String originalContent);
}
