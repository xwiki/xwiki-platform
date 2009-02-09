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
package org.xwiki.rendering.internal.macro.script;

import java.io.StringWriter;
import java.util.List;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.script.AbstractScriptMacro;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.ScriptContextManager;

/**
 * Execute script in provided script language.
 * 
 * @version $Id$
 * @since 1.7M3
 */
public class ScriptMacro extends AbstractScriptMacro<ScriptMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Execute script in provided script language.";

    /**
     * Used to get the current script context to give to script engine evaluation method.
     */
    private ScriptContextManager scriptContextManager;

    /**
     * Used to find if the current document's author has programming rights.
     */
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The identifier of the script language. If null, {@link ScriptMacroParameters#getLanguage()} as to be not null.
     */
    private String language;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public ScriptMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, ScriptMacroParameters.class));
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
     * @see org.xwiki.rendering.macro.script.AbstractScriptMacro#execute(java.lang.Object, java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    @Override
    public List<Block> execute(ScriptMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (!canExecuteScript()) {
            throw new MacroExecutionException("You don't have the right to execute this script");
        }

        return super.execute(parameters, content, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.script.AbstractScriptMacro#evaluate(java.lang.Object, java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    @Override
    protected String evaluate(ScriptMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (StringUtils.isEmpty(content)) {
            return "";
        }

        String engineName;

        // 1) resolve script engine name
        if (this.language == null) {
            String macroName = context.getCurrentMacroBlock().getName().toLowerCase();

            if (macroName.equals("script")) {
                engineName = parameters.getLanguage();
            } else {
                engineName = macroName;
            }
        } else {
            engineName = this.language;
        }

        String scriptResult;
        if (engineName != null) {
            // 2) execute script
            try {
                ScriptEngineManager sem = new ScriptEngineManager(this.getClass().getClassLoader());
                ScriptEngine engine = sem.getEngineByName(engineName);
                if (engine != null) {
                    ScriptContext scriptContext = this.scriptContextManager.getScriptContext();

                    StringWriter stringWriter = new StringWriter();

                    // set writer in script context
                    scriptContext.setWriter(stringWriter);

                    if (engine instanceof Compilable) {
                        CompiledScript compiledScript = getCompiledScript(content, (Compilable) engine);

                        compiledScript.eval(scriptContext);
                    } else {
                        engine.eval(content, scriptContext);
                    }

                    // remove writer script from context
                    scriptContext.setWriter(null);

                    scriptResult = stringWriter.toString();
                } else {
                    throw new MacroExecutionException("Can't find script engine with name [" + engineName + "]");
                }
            } catch (ScriptException e) {
                throw new MacroExecutionException("Failed to evaluate Script Macro for content [" + content + "]", e);
            }
        } else {
            // If no language identifier is provided, don't evaluate content
            scriptResult = content;
        }

        return scriptResult;
    }

    /**
     * Indicate if the script is executable in the current context.
     * <p>
     * For example with not protected script engine, we are testing if the current dcument's author has "programming"
     * right.
     * 
     * @return true if the script can be evaluated, false otherwise.
     */
    protected boolean canExecuteScript()
    {
        return this.documentAccessBridge.hasProgrammingRights();
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
