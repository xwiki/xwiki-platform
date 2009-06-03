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
package org.xwiki.rendering.internal.macro.groovy;

import java.io.PrintWriter;
import java.io.Writer;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.script.AbstractJRSR223ScriptMacro;
import org.xwiki.rendering.macro.script.JSR223ScriptMacroParameters;

/**
 * Execute script in provided script language.
 * 
 * @version $Id$
 * @since 1.7M3
 */
@Component("groovy")
public class GroovyMacro extends AbstractJRSR223ScriptMacro<JSR223ScriptMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Execute a groovy script.";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "the groovy script to execute";

    /**
     * Create and initialize the descriptor of the macro.
     */
    public GroovyMacro()
    {
        super(DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.script.AbstractJRSR223ScriptMacro#eval(java.lang.String,
     *      javax.script.ScriptEngine, javax.script.ScriptContext)
     */
    @Override
    protected Object eval(String content, ScriptEngine engine, ScriptContext scriptContext) throws ScriptException
    {
        // There is a bug in groovy that make it not take care of configured writer when there is something in
        // "context"

        Writer writer = scriptContext.getWriter();

        scriptContext.setAttribute("out", (writer instanceof PrintWriter) ? writer : new PrintWriter(writer, true),
            ScriptContext.ENGINE_SCOPE);

        return super.eval(content, engine, scriptContext);
    }
}
