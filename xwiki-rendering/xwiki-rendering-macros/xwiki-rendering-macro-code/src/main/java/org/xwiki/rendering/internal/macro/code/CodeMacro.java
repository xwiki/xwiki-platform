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
package org.xwiki.rendering.internal.macro.code;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.VerbatimInlineBlock;
import org.xwiki.rendering.block.VerbatimStandaloneBlock;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.box.AbstractBoxMacro;
import org.xwiki.rendering.macro.code.CodeMacroParameters;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.parser.HighlightParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Highlight provided content depending of the content syntax.
 * 
 * @version $Id$
 * @since 1.7RC1
 */
public class CodeMacro extends AbstractBoxMacro<CodeMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "";

    /**
     * Create and initialize the descriptor of the macro.
     */
    public CodeMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, CodeMacroParameters.class));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.macro.box.DefaultBoxMacro#parseContent(org.xwiki.rendering.macro.box.BoxMacroParameters,
     *      java.lang.String, org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    @Override
    protected List<Block> parseContent(CodeMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        List<Block> result;
        try {
            if (CodeMacroParameters.LANGUAGE_NONE.equalsIgnoreCase(parameters.getLanguage())) {
                if (context.isInlined()) {
                    result = Collections.<Block> singletonList(new VerbatimInlineBlock(content));
                } else {
                    result = Collections.<Block> singletonList(new VerbatimStandaloneBlock(content));
                }
            } else {
                result = highlight(parameters, content);
            }
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to highlight content", e);
        }

        return result;
    }

    /**
     * Return a highlighted version of the provided content.
     * 
     * @param parameters the code macro parameters.
     * @param content the content to highlight.
     * @return the highlighted version of the provided content.
     * @throws ParseException the highlight parser failed.
     * @throws ComponentLookupException failed to find highlight parser for provided language.
     */
    protected List<Block> highlight(CodeMacroParameters parameters, String content) throws ParseException,
        ComponentLookupException
    {
        HighlightParser parser = null;

        if (parameters.getLanguage() != null) {
            try {
                parser = (HighlightParser) getComponentManager().lookup(HighlightParser.ROLE, parameters.getLanguage());

                return parser.highlight(parameters.getLanguage(), new StringReader(content));
            } catch (ComponentLookupException e) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug(
                        "Can't find specific highlighting parser for language [" + parameters.getLanguage() + "]", e);
                }
            }
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Trying the default highlighting parser");
        }

        parser = (HighlightParser) getComponentManager().lookup(HighlightParser.ROLE, "default");

        return parser.highlight(parameters.getLanguage(), new StringReader(content));
    }
}
