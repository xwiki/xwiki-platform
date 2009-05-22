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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.box.BoxMacroParameters;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Displays a message, which can be an error, a warning or an info note.
 * 
 * @version $Id$
 * @since 2.0
 */
@Component(hints = {"info", "warning", "error" })
public class MessageMacro extends AbstractMacro<Object>
{
    /**
     * Predefined error message.
     */
    public static final String CONTENT_MISSING_ERROR = "The required content is missing.";

    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Displays a message, which can be an error, a warning or an info note.";

    /**
     * Injected by the component manager.
     */
    @Requirement("box")
    private Macro<BoxMacroParameters> boxMacro;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public MessageMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, Object.class));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(Object parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (StringUtils.isEmpty(content)) {
            throw new MacroExecutionException(CONTENT_MISSING_ERROR);
        }

        BoxMacroParameters boxParameters = new BoxMacroParameters();

        boxParameters.setCssClass(context.getCurrentMacroBlock().getName() + "message");

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
