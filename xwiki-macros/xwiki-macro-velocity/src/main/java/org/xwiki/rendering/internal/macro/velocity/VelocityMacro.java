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
package org.xwiki.rendering.internal.macro.velocity;

import java.util.List;
import java.io.StringWriter;
import java.io.StringReader;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.AbstractNoParameterMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.ParserUtils;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.VelocityManager;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class VelocityMacro extends AbstractNoParameterMacro
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Executes a Velocity script.";

    /**
     * Injected by the Component Manager.
     */
    private VelocityManager velocityManager;

    /**
     * Used to cleanup wiki parser result.
     */
    private ParserUtils parserUtils = new ParserUtils();

    /**
     * Injected by the Component Manager.
     */
    private Parser parser;

    /**
     * Default constructor.
     */
    public VelocityMacro()
    {
        super(DESCRIPTION);
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
    
    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(Object parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // 1) Run Velocity on macro block content
        StringWriter writer = new StringWriter();

        try {
            this.velocityManager.getVelocityEngine().evaluate(this.velocityManager.getVelocityContext(), writer,
                "velocity macro", content);

        } catch (XWikiVelocityException e) {
            throw new MacroExecutionException("Failed to evaluate Velocity Macro for content [" + content + "]", e);
        }

        String velocityResult = writer.toString();

        // 2) Run the wiki syntax parser on the velocity-rendered content
        XDOM parsedDom;
        try {
            parsedDom = this.parser.parse(new StringReader(velocityResult));
        } catch (ParseException e) {
            throw new MacroExecutionException("Failed to parse content [" + velocityResult + "] with Syntax parser ["
                + this.parser.getSyntax() + "]", e);
        }
        
        // 3) If in inline mode remove any top level paragraph
        List<Block> result = parsedDom.getChildren();
        if (context.isInlined()) {
            this.parserUtils.removeTopLevelParagraph(result);
        }

        return result; 
    }
}
