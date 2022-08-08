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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.context.Execution;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.AbstractSignableMacro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.ParserUtils;
import org.xwiki.script.event.ScriptEvaluatedEvent;
import org.xwiki.script.event.ScriptEvaluatingEvent;

/**
 * Base Class for script evaluation macros.
 * <p>
 * It is not obvious to see how macro execution works just from looking at the code. A lot of checking and
 * initialization is done in listeners to the {@link org.xwiki.script.event.ScriptEvaluatingEvent} and
 * {@link org.xwiki.script.event.ScriptEvaluatedEvent}. E.g. the check for programming rights for JSR223 scripts, check
 * for nested script macros and selecting the right class loader is done there.
 * </p>
 * 
 * @param <P> the type of macro parameters bean.
 * @version $Id$
 * @since 1.7M3
 */
public abstract class AbstractScriptMacro<P extends ScriptMacroParameters> extends AbstractSignableMacro<P> implements
    ScriptMacro
{
    /**
     * The default description of the script macro content.
     */
    protected static final String CONTENT_DESCRIPTION = "the script to execute";

    /**
     * Used to find if the current document's author has programming rights.
     *
     * @deprecated since 2.5M1 (not used any more)
     */
    @Inject
    @Deprecated
    protected org.xwiki.bridge.DocumentAccessBridge documentAccessBridge;

    /**
     * Used by subclasses.
     */
    @Inject
    protected Execution execution;

    /**
     * Used to parse the result of the script execution into a XDOM object when the macro is configured by the user to
     * not interpret wiki syntax.
     */
    @Inject
    @Named("plain/1.0")
    private Parser plainTextParser;

    /**
     * The parser used to parse box content and box title parameter.
     */
    @Inject
    private MacroContentParser contentParser;

    /**
     * Observation manager used to sent evaluation events.
     */
    @Inject
    private ObservationManager observation;

    /**
     * Utility to remove the top level paragraph.
     */
    private ParserUtils parserUtils = new ParserUtils();

    /**
     * @param macroName the name of the macro (eg "groovy")
     */
    public AbstractScriptMacro(String macroName)
    {
        super(macroName, null, ScriptMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     */
    public AbstractScriptMacro(String macroName, String macroDescription)
    {
        super(macroName, macroDescription, ScriptMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param contentDescriptor the description of the macro content.
     */
    public AbstractScriptMacro(String macroName, String macroDescription, ContentDescriptor contentDescriptor)
    {
        super(macroName, macroDescription, contentDescriptor, ScriptMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param parametersBeanClass class of the parameters bean for this macro.
     */
    public AbstractScriptMacro(String macroName, String macroDescription,
        Class< ? extends ScriptMacroParameters> parametersBeanClass)
    {
        super(macroName, macroDescription, parametersBeanClass);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param contentDescriptor the description of the macro content.
     * @param parametersBeanClass class of the parameters bean for this macro.
     */
    public AbstractScriptMacro(String macroName, String macroDescription, ContentDescriptor contentDescriptor,
        Class< ? extends ScriptMacroParameters> parametersBeanClass)
    {
        super(macroName, macroDescription, contentDescriptor, parametersBeanClass);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
    }

    @Override
    public List<Block> execute(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        List<Block> result = Collections.emptyList();

        if (StringUtils.isNotEmpty(content)) {
            try {
                // send evaluation starts event
                ScriptEvaluatingEvent event = new ScriptEvaluatingEvent(getDescriptor().getId().getId());
                this.observation.notify(event, context, parameters);
                if (event.isCanceled()) {
                    throw new MacroExecutionException(event.getReason());
                }

                // 2) Run script engine on macro block content
                List<Block> blocks = evaluateBlock(parameters, content, context);

                if (parameters.isOutput()) {
                    result = blocks;
                }
            } finally {
                // send evaluation finished event
                this.observation.notify(new ScriptEvaluatedEvent(getDescriptor().getId().getId()), context, parameters);
            }
        }

        return result;
    }

    /**
     * Convert script result as a {@link Block} list.
     * 
     * @param content the script result to parse.
     * @param parameters the macro parameters.
     * @param context the context of the macro transformation.
     * @return the {@link Block}s.
     * @throws MacroExecutionException Failed to find source parser.
     * @since 2.1M1
     */
    protected List<Block> parseScriptResult(String content, P parameters, MacroTransformationContext context)
        throws MacroExecutionException
    {
        List<Block> result;

        if (parameters.isWiki()) {
            result = parseSourceSyntax(content, context);
        } else {
            try {
                result = this.plainTextParser.parse(new StringReader(content)).getChildren();
            } catch (ParseException e) {
                // This shouldn't happen since the parser cannot throw an exception since the source is a memory
                // String.
                throw new MacroExecutionException("Failed to parse link label as plain text", e);
            }
        }

        // 3) If in inline mode remove any top level paragraph
        if (context.isInline()) {
            this.parserUtils.convertToInline(result);
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
     * @deprecated since 2.4M2 use {@link #evaluateString(ScriptMacroParameters, String, MacroTransformationContext)}
     *             instead
     */
    @Deprecated
    protected String evaluate(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return "";
    }

    /**
     * Execute provided script and return {@link String} based result.
     * 
     * @param parameters the macro parameters.
     * @param content the script to execute.
     * @param context the context of the macro transformation.
     * @return the result of script execution.
     * @throws MacroExecutionException failed to evaluate provided content.
     * @since 2.4M2
     */
    protected String evaluateString(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Call old method for retro-compatibility
        return evaluate(parameters, content, context);
    }

    /**
     * Execute provided script and return {@link Block} based result.
     * 
     * @param parameters the macro parameters.
     * @param content the script to execute.
     * @param context the context of the macro transformation.
     * @return the result of script execution.
     * @throws MacroExecutionException failed to evaluate provided content.
     * @since 2.4M2
     */
    protected List<Block> evaluateBlock(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        String scriptResult = evaluateString(parameters, content, context);

        List<Block> result = Collections.emptyList();
        if (parameters.isOutput()) {
            // Run the wiki syntax parser on the script-rendered content
            result = parseScriptResult(scriptResult, parameters, context);
        }

        return result;
    }

    /**
     * Parse provided content with the parser of the current wiki syntax.
     * 
     * @param content the content to parse.
     * @param context the context of the macro transformation.
     * @return the result of the parsing.
     * @throws MacroExecutionException failed to parse content
     */
    protected List<Block> parseSourceSyntax(String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return this.contentParser.parse(content, context, false, false).getChildren();
    }
}
