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
package org.xwiki.rendering.macro.id;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import java.util.Collections;
import java.util.List;

/**
 * @version $Id$
 * @since 1.6M1
 */
public class IdMacro extends AbstractMacro<IdMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Include other pages into the current page.";

    /**
     * Create and initialize the descriptor of the macro.
     */
    public IdMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, IdMacroParameters.class));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(IdMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        IdBlock idBlock = new IdBlock(parameters.getName());

        return Collections.singletonList((Block) idBlock);
    }
}
