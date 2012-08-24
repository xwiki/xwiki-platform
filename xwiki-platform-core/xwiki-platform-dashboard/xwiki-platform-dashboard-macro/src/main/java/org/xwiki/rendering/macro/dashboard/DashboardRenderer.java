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
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Renders the passed list of gadgets as a piece of XDOM, to be added in a tree after, handling the layout of the
 * gadgets. Various implementations of this class should provide various strategies of layouting the gadgets.
 * 
 * @version $Id$
 * @since 3.0M3
 */
@Role
public interface DashboardRenderer
{
    /**
     * Renders the passed gadgets in a list of blocks, to be added in an XDOM and rendered after.
     * 
     * @param gadgets the gadgets to render as XDOM
     * @param gadgetsRenderer the renderer to use to render the gadgets
     * @param context the macro transformation context where the dashboard is executed
     * @return the list of {@link Block}s that represent the gadgets list
     * @throws Exception if anything goes wrong during macro execution
     */
    List<Block> renderGadgets(List<Gadget> gadgets, GadgetRenderer gadgetsRenderer, MacroTransformationContext context)
        throws Exception;

    // TODO: add here function that takes dashboard layout specification string as param, to allow different layout
    // types
}
