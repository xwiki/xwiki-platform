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
package org.xwiki.rendering.internal.macro.message;

import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.box.BoxMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.component.annotation.Requirement;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Common implementation for info, error and warning macros.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public abstract class AbstractMessageMacro extends AbstractMacro<Object>
{
    /**
     * Predefined error message.
     */
    public static final String CONTENT_MISSING_ERROR = "The required content is missing.";

    /**
     * Injected by the component manager.
     */
    @Requirement("box")
    private Macro<BoxMacroParameters> boxMacro;

    /**
     * Create and initialize the descriptor of the macro.
     *
     * @param macroName the macro name (eg "Error", "Info", etc)
     * @param macroDescription the macro description
     */
    public AbstractMessageMacro(String macroName, String macroDescription)
    {
        super(macroName, macroDescription, new DefaultContentDescriptor(true));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    public List<Block> execute(Object parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (StringUtils.isEmpty(content)) {
            throw new MacroExecutionException(CONTENT_MISSING_ERROR);
        }

        BoxMacroParameters boxParameters = new BoxMacroParameters();

        boxParameters.setCssClass(context.getCurrentMacroBlock().getId() + "message");

        List<Block> result;
        if (!context.isInline()) {
            boxParameters.setTitle(content);
            result = this.boxMacro.execute(boxParameters, StringUtils.EMPTY, context);
        } else {
            result = this.boxMacro.execute(boxParameters, content, context);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return true;
    }
}
