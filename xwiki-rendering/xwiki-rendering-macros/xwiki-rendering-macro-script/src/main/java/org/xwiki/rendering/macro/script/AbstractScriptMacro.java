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
package org.xwiki.rendering.macro.script;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.ParserUtils;

/**
 * Base Class for script evaluation macros.
 * 
 * @param <P> the type of macro parameters bean.
 * @version $Id$
 * @since 1.7M3
 */
public abstract class AbstractScriptMacro<P extends ScriptMacroParameters> extends AbstractMacro<P>
{
    /**
     * The default description of the script macro content.
     */
    protected static final String CONTENT_DESCRIPTION = "the script to execute";

    /**
     * Used to get the current syntax parser.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Used to clean result of the parser syntax.
     */
    private ParserUtils parserUtils = new ParserUtils();

    /**
     * The constructor.
     * 
     * @param macroDescriptor the description of the macro.
     */
    public AbstractScriptMacro(MacroDescriptor macroDescriptor)
    {
        super(macroDescriptor);
    }

    /**
     * @param macroDescription the text description of the macro.
     */
    public AbstractScriptMacro(String macroDescription)
    {
        super(new DefaultMacroDescriptor(macroDescription, new DefaultContentDescriptor(CONTENT_DESCRIPTION),
            ScriptMacroParameters.class));
    }

    /**
     * @param macroDescription the text description of the macro.
     * @param contentDescriptor the description of the macro content.
     */
    public AbstractScriptMacro(String macroDescription, ContentDescriptor contentDescriptor)
    {
        super(new DefaultMacroDescriptor(macroDescription, contentDescriptor, ScriptMacroParameters.class));
    }

    /**
     * @return the component manager
     */
    protected ComponentManager getComponentManager()
    {
        return this.componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        List<Block> result = Collections.emptyList();

        if (!StringUtils.isEmpty(content)) {
            // 1) Run script engine on macro block content
            String scriptResult = evaluate(parameters, content, context);

            if (parameters.isOutput()) {
                // 2) Run the wiki syntax parser on the script-rendered content
                XDOM parsedDom = parseSourceSyntax(scriptResult, context);

                // 3) If in inline mode remove any top level paragraph
                result = parsedDom.getChildren();
                if (context.isInline()) {
                    this.parserUtils.removeTopLevelParagraph(result);
                }
            }
        }

        return result;
    }

    /**
     * Execute provided script.
     * 
     * @param parameters the macro parameters.
     * @param content the script to execute.
     * @param context the context of the macro transformation.
     * @return the result of script execution.
     * @throws MacroExecutionException failed to evaluate provided content.
     */
    protected abstract String evaluate(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException;

    /**
     * Get the parser of the current wiki syntax.
     * 
     * @param context the context of the macro transformation.
     * @return the parser of the current wiki syntax.
     * @throws MacroExecutionException Failed to find source parser.
     */
    protected Parser getSyntaxParser(MacroTransformationContext context) throws MacroExecutionException
    {
        try {
            return (Parser) getComponentManager().lookup(Parser.class, context.getSyntax().toIdString());
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException("Failed to find source parser", e);
        }
    }

    /**
     * Parse provided content with the parser of the current wiki syntax.
     * 
     * @param content the content to parse.
     * @param context the context of the macro transformation.
     * @return an XDOM containing the parser content.
     * @throws MacroExecutionException failed to parse content
     */
    protected XDOM parseSourceSyntax(String content, MacroTransformationContext context) throws MacroExecutionException
    {
        Parser parser = getSyntaxParser(context);

        try {
            return parser.parse(new StringReader(content));
        } catch (ParseException e) {
            throw new MacroExecutionException("Failed to parse content [" + content + "] with Syntax parser ["
                + parser.getSyntax() + "]", e);
        }
    }
}
