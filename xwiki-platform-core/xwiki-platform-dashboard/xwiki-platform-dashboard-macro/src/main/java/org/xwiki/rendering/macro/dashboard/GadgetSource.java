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
 * Reads the gadgets for the dashboard macro which is being executed.
 * 
 * @version $Id$
 * @since 3.0M3
 */
@Role
public interface GadgetSource
{
    /**
     * Reads the gadgets for the passed macro transformation context.
     * 
     * @param source the source to read dashboard gadgets from (a document serialized reference)
     * @param context the dashboard macro transformation context
     * @return the list of gadgets for the currently executing macro
     * @throws Exception in case anything goes wrong reading data, the exception should be translated by the dashboard
     *             macro caller into a macro execution exception
     */
    List<Gadget> getGadgets(String source, MacroTransformationContext context) throws Exception;

    /**
     * Get the metadata about this dashboard source, such as source document fullname, gadget add url, gadget remove
     * url, etc, to pass to the client. <br>
     * TODO: find a better place for this code, ftm it's here because this is the only class that knows about XWiki data
     * model
     * 
     * @param source the source to read dashboard gadgets from (a document serialized reference)
     * @param context the dashboard macro transformation context
     * @return a list of blocks that represent the XDOM with the metadata. TODO: might as well be a map of metadata and
     *         leave the rendering in XDOM to the dashboard macro, but ftm this does the job
     */
    List<Block> getDashboardSourceMetadata(String source, MacroTransformationContext context);

    /**
     * @return {@code true} if the the current context is in edit mode (gadgets can be edited -- positions, parameters,
     *         adding gadgets etc), {@code false} otherwise <br>
     *         TODO: find a better place to put this function, but for now this is the only interface towards XWiki data
     *         model, and since the mode is set on the current context, this function needs to be here
     */
    boolean isEditing();
}
