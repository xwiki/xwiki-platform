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
package org.xwiki.rendering.internal.macro.box;

import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.box.AbstractBoxMacro;
import org.xwiki.rendering.macro.box.BoxMacroParameters;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Draw a box around provided content.
 * 
 * @param <P> the type of macro parameters bean.
 * @version $Id$
 * @since 1.7
 */
@Component("box")
public class DefaultBoxMacro<P extends BoxMacroParameters> extends AbstractBoxMacro<P>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Draw a box around provided content.";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "the content to put in the box";

    /**
     * Create and initialize the descriptor of the macro.
     */
    public DefaultBoxMacro()
    {
        super("Box", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION), BoxMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_FORMATTING);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractBoxMacro#parseContent(BoxMacroParameters, String, MacroTransformationContext) 
     */
    @Override
    protected List<Block> parseContent(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return getMacroContentParser().parse(content, context.getSyntax(), context.isInline());
    }
}
