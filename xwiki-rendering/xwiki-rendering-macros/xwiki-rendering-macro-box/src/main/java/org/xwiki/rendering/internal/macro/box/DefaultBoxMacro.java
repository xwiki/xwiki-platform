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

import java.io.StringReader;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.box.AbstractBoxMacro;
import org.xwiki.rendering.macro.box.BoxMacroParameters;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.ParserUtils;

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
        super(new DefaultMacroDescriptor(DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION),
            BoxMacroParameters.class));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.macro.box.AbstractBoxMacro#parseContent(org.xwiki.rendering.macro.box.BoxMacroParameters,
     *      java.lang.String, org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    @Override
    protected List<Block> parseContent(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return parseSourceSyntax(content, context);
    }

    /**
     * Parse provided content with the parser of the current wiki syntax.
     * 
     * @param content the content to parse.
     * @param context the context of the macro transformation.
     * @return an XDOM containing the parser content.
     * @throws MacroExecutionException failed to parse content
     */
    protected List<Block> parseSourceSyntax(String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        Parser parser = getSyntaxParser(context);

        try {
            List<Block> blocks = parser.parse(new StringReader(content)).getChildren();
            if (context.isInline()) {
                ParserUtils parseUtils = new ParserUtils();
                parseUtils.removeTopLevelParagraph(blocks);
            }

            return blocks;
        } catch (ParseException e) {
            throw new MacroExecutionException("Failed to parse content [" + content + "] with Syntax parser ["
                + parser.getSyntax() + "]", e);
        }
    }
}
