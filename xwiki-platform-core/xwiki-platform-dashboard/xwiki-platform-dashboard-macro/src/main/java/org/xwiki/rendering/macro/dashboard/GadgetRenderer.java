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
package org.xwiki.rendering.macro.dashboard;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;

/**
 * Decorates the passed gadget and renders it as a full piece of XDOM, a single list of blocks that can be added in the
 * tree after.
 * 
 * @version $Id$
 * @since 3.0rc1
 */
@Role
public interface GadgetRenderer
{
    /**
     * Decorates the passed gadget and renders it as a list of XDOM blocks.
     * 
     * @param gadget the gadget to render
     * @return the list of blocks resulted from rendering the passed gadget and decorating it (with containers, etc)
     */
    List<Block> decorateGadget(Gadget gadget);
}
