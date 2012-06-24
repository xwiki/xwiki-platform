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

import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.ScriptContextManager;

/**
 * Base Class for script evaluation macros based on JSR223.
 * 
 * @param <P> the type of macro parameters bean.
 * @version $Id$
 * @since 1.7M3
 */
public abstract class AbstractJSR223ScriptMacro<P extends JSR223ScriptMacroParameters> extends AbstractScriptMacro<P>
    implements PrivilegedScriptMacro
{
    /**
     * Key under which the Script Engines are saved in the Execution Context, see {@link #execution}.
     */
    private static final String EXECUTION_CONTEXT_ENGINE_KEY = "scriptEngines";

    /**
     * The JSR223 Script Engine Manager we use to evaluate JSR223 scripts.
     */
    protected ScriptEngineManager scriptEngineManager;

    /**
     * Used to get the current script context to give to script engine evaluation method.
     */
    @Inject
    private ScriptContextManager scriptContextManager;

    /**
     * @param macroName the name of the macro (eg "groovy")
     */
    public AbstractJSR223ScriptMacro(String macroName)
    {
        super(macroName, null, JSR223ScriptMacroParameters.class);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     */
    public AbstractJSR223ScriptMacro(String macroName, String macroDescription)
    {
        super(macroName, macroDescription, JSR223ScriptMacroParameters.class);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param contentDescriptor the description of the macro content.
     */
    public AbstractJSR223ScriptMacro(String macroName, String macroDescription, ContentDescriptor contentDescriptor)
    {
        super(macroName, macroDescription, contentDescriptor, JSR223ScriptMacroParameters.class);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param parametersBeanClass class of the parameters bean for this macro.
     */
    public AbstractJSR223ScriptMacro(String macroName, String macroDescription,
        Class< ? extends JSR223ScriptMacroParameters> parametersBeanClass)
    {
        super(macroName, macroDescription, parametersBeanClass);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param contentDescriptor the description of the macro content.
     * @param parametersBeanClass class of the parameters bean for this macro.
     */
    public AbstractJSR223ScriptMacro(String macroName, String macroDescription, ContentDescriptor contentDescriptor,
        Class< ? extends JSR223ScriptMacroParameters> parametersBeanClass)
    {
        super(macroName, macroDescription, contentDescriptor, parametersBeanClass);
    }

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();
        this.scriptEngineManager = new ScriptEngineManager();
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    /**
     * Method to overwrite to indicate the script engine name.
     * 
     * @param parameters the macro parameters.
     * @param context the context of the macro transformation.
     * @return the name of the script engine to use.
     */
    protected String getScriptEngineName(P parameters, MacroTransformationContext context)
    {
        return context.getCurrentMacroBlock().getId().toLowerCase();
    }

    /**
     * Get the current ScriptContext and refresh it.
     * 
     * @return the script context.
     */
    protected ScriptContext getScriptContext()
    {
        return this.scriptContextManager.getScriptContext();
    }

    @Override
    protected List<Block> evaluateBlock(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }

        String engineName = getScriptEngineName(parameters, context);

        List<Block> result;
        if (engineName != null) {
            try {
                ScriptEngine engine = getScriptEngine(engineName);

                if (engine != null) {
                    result = evaluateBlock(engine, parameters, content, context);
                } else {
                    throw new MacroExecutionException("Can't find script engine with name [" + engineName + "]");
                }
            } catch (ScriptException e) {
                throw new MacroExecutionException("Failed to evaluate Script Macro for content [" + content + "]", e);
            }

        } else {
            // If no language identifier is provided, don't evaluate content
            result = parseScriptResult(content, parameters, context);
        }

        return result;
    }

    /**
     * Execute provided script and return {@link Block} based result.
     * 
     * @param engine the script engine to use to evaluate the script.
     * @param parameters the macro parameters.
     * @param content the script to execute.
     * @param context the context of the macro transformation.
     * @return the result of script execution.
     * @throws ScriptException failed to evaluate script
     * @throws MacroExecutionException failed to evaluate provided content.
     */
    protected List<Block> evaluateBlock(ScriptEngine engine, P parameters, String content,
        MacroTransformationContext context) throws ScriptException, MacroExecutionException
    {
        List<Block> result;

        ScriptContext scriptContext = getScriptContext();

        StringWriter stringWriter = new StringWriter();

        // set writer in script context
        scriptContext.setWriter(stringWriter);

        try {
            Object scriptResult = eval(content, engine, scriptContext);

            if (scriptResult instanceof XDOM) {
                result = ((XDOM) scriptResult).getChildren();
            } else if (scriptResult instanceof Block) {
                result = Collections.singletonList((Block) scriptResult);
            } else if (scriptResult instanceof List && !((List< ? >) scriptResult).isEmpty()
                && ((List< ? >) scriptResult).get(0) instanceof Block) {
                result = (List<Block>) scriptResult;
            } else {
                // Run the wiki syntax parser on the script-rendered content
                result = parseScriptResult(stringWriter.toString(), parameters, context);
            }
        } finally {
            // remove writer script from context
            scriptContext.setWriter(null);
        }

        return result;
    }

    /**
     * @param engineName the script engine name (eg "groovy", etc)
     * @return the Script engine to use to evaluate the script
     * @throws MacroExecutionException in case of an error in parsing the jars parameter
     */
    private ScriptEngine getScriptEngine(String engineName) throws MacroExecutionException
    {
        // Look for a script engine in the Execution Context since we want the same engine to be used
        // for all evals during the same execution lifetime.
        // We must use the same engine because that engine may create an internal ClassLoader in which
        // it loads new classes defined in the script and if we create a new engine then defined classes
        // will be lost.
        // However we also need to be able to execute several script Macros during a single execution request
        // and for example the second macro could have jar parameters. In order to support this use case
        // we ensure in AbstractScriptMacro to reuse the same thread context ClassLoader during the whole
        // request execution.
        ExecutionContext executionContext = this.execution.getContext();
        Map<String, ScriptEngine> scriptEngines =
            (Map<String, ScriptEngine>) executionContext.getProperty(EXECUTION_CONTEXT_ENGINE_KEY);
        if (scriptEngines == null) {
            scriptEngines = new HashMap<String, ScriptEngine>();
            executionContext.setProperty(EXECUTION_CONTEXT_ENGINE_KEY, scriptEngines);
        }
        ScriptEngine engine = scriptEngines.get(engineName);

        if (engine == null) {
            engine = this.scriptEngineManager.getEngineByName(engineName);
            scriptEngines.put(engineName, engine);
        }

        return engine;
    }

    /**
     * Execute the script.
     * 
     * @param content the script to be executed by the script engine
     * @param engine the script engine
     * @param scriptContext the script context
     * @return The value returned from the execution of the script.
     * @throws ScriptException if an error occurrs in script. ScriptEngines should create and throw
     *             <code>ScriptException</code> wrappers for checked Exceptions thrown by underlying scripting
     *             implementations.
     */
    protected Object eval(String content, ScriptEngine engine, ScriptContext scriptContext) throws ScriptException
    {
        return engine.eval(content, scriptContext);
    }

    // /////////////////////////////////////////////////////////////////////
    // Compiled scripts management

    /**
     * Return a compiled version of the provided script.
     * 
     * @param content the script to compile.
     * @param engine the script engine.
     * @return the compiled version of the script.
     * @throws ScriptException failed to compile the script.
     */
    protected CompiledScript getCompiledScript(String content, Compilable engine) throws ScriptException
    {
        // TODO: add caching

        return engine.compile(content);
    }
}
