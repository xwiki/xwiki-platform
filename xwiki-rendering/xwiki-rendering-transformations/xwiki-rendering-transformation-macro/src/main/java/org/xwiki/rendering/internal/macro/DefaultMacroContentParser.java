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
package org.xwiki.rendering.internal.macro;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.Block.Axes;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.util.ParserUtils;

/**
 * Default implementation for {@link org.xwiki.rendering.internal.macro.MacroContentParser}.
 * 
 * @version $Id$
 * @since 3.0M1
 */
@Component
public class DefaultMacroContentParser implements MacroContentParser
{
    /**
     * Used to look up the syntax parser to use for parsing the content.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Utility to remove the top level paragraph.
     */
    private ParserUtils parserUtils = new ParserUtils();

    /**
     * {@inheritDoc}
     * 
     * @see MacroContentParser#parse(String, MacroTransformationContext, boolean, boolean)
     */
    public List<Block> parse(String content, MacroTransformationContext macroContext, boolean transform,
        boolean removeTopLevelParagraph) throws MacroExecutionException
    {
        return parseXDOM(content, macroContext, transform, removeTopLevelParagraph).getChildren();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.macro.MacroContentParser#parseXDOM(java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext, boolean, boolean)
     */
    public XDOM parseXDOM(String content, MacroTransformationContext macroContext, boolean transform,
        boolean removeTopLevelParagraph) throws MacroExecutionException
    {
        // If the content is empty return an empty list
        if (StringUtils.isEmpty(content)) {
            return new XDOM(Collections.<Block> emptyList());
        }

        Syntax syntax = getCurrentSyntax(macroContext);

        // If there's no syntax specified in the Transformation throw an error
        if (syntax == null) {
            throw new MacroExecutionException("Invalid Transformation: missing Syntax");
        }

        try {
            XDOM result = getSyntaxParser(syntax).parse(new StringReader(content));

            if (transform && macroContext.getTransformation() != null) {
                TransformationContext txContext = new TransformationContext(result, syntax);
                txContext.setId(macroContext.getId());
                try {
                    macroContext.getTransformation().transform(result, txContext);
                } catch (Exception e) {
                    throw new MacroExecutionException("Failed to perform transformation", e);
                }
            }

            if (removeTopLevelParagraph) {
                List<Block> children = new ArrayList<Block>(result.getChildren());
                this.parserUtils.removeTopLevelParagraph(children);
                result.setChildren(children);
            }

            return result;
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to parse content [" + content + "]", e);
        }
    }

    /**
     * Get the parser for the current syntax.
     * 
     * @param syntax the current syntax of the title content
     * @return the parser for the current syntax
     * @throws org.xwiki.rendering.macro.MacroExecutionException Failed to find source parser.
     */
    private Parser getSyntaxParser(Syntax syntax) throws MacroExecutionException
    {
        try {
            return this.componentManager.lookup(Parser.class, syntax.toIdString());
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException("Failed to find source parser for syntax [" + syntax + "]", e);
        }
    }

    /**
     * Find the current syntax to use for macro supporting wiki content/parameters/whatever.
     * 
     * @param context the macro execution context containing the default syntax and the current macro block
     * @return the current syntax
     */
    protected Syntax getCurrentSyntax(MacroTransformationContext context)
    {
        Syntax currentSyntax = context.getSyntax();

        MacroBlock currentMacroBlock = context.getCurrentMacroBlock();

        if (currentMacroBlock != null) {
            MetaDataBlock metaDataBlock =
                (MetaDataBlock) currentMacroBlock.getFirstBlock(new MetadataBlockMatcher(MetaData.SYNTAX),
                    Axes.ANCESTOR_OR_SELF);

            if (metaDataBlock != null) {
                currentSyntax = (Syntax) metaDataBlock.getMetaData().getMetaData(MetaData.SYNTAX);
            }
        }

        return currentSyntax;
    }
}
