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
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @version $Id$
 * @since 1.6M1
 */
public class IdMacro extends AbstractMacro implements Initializable
{
    private static final String DESCRIPTION = "Include other pages into the current page.";

    /**
     * The parameters manager of the macro.
     */
    private IdMacroParameterManager macroParameters = new IdMacroParameterManager();

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // TODO: Use an I8N service to translate the descriptions in several languages
    }

    /**
     * {@inheritDoc}
     * 
     * @see Macro#getDescription()
     */
    public String getDescription()
    {
        // TODO: Use an I8N service to translate the description in several languages
        return DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Macro#getAllowedParameters()
     */
    public Map<String, MacroParameterDescriptor< ? >> getAllowedParameters()
    {
        return this.macroParameters.getParametersDescriptorMap();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(java.util.Map, java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    public List<Block> execute(Map<String, String> parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        this.macroParameters.load(parameters);

        IdBlock idBlock = new IdBlock(this.macroParameters.getName());

        return Collections.singletonList((Block) idBlock);
    }
}
