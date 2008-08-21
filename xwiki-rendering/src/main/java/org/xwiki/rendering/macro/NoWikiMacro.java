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
package org.xwiki.rendering.macro;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.macro.parameter.MacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import java.util.List;
import java.util.Arrays;

/**
 * Prevents wiki syntax rendering.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class NoWikiMacro extends AbstractNoParametersMacro
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Wiki syntax inside this macro is not rendered.";

    /**
     * Create and initialize the descriptor of the macro.
     */
    public NoWikiMacro()
    {
        super(DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(org.xwiki.rendering.macro.parameter.MacroParameters,
     *      java.lang.String, org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    public List<Block> execute(MacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return Arrays.asList((Block) new WordBlock(content));
    }
}
