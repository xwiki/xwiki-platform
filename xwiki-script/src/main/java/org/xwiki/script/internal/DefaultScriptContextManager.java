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
package org.xwiki.script.internal;

import java.util.List;

import javax.script.ScriptContext;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.context.Execution;
import org.xwiki.script.ScriptContextInitializer;
import org.xwiki.script.ScriptContextManager;

/**
 * Default implementation of {@link ScriptContextManager}.
 * 
 * @version $Id$
 */
public class DefaultScriptContextManager extends AbstractLogEnabled implements ScriptContextManager
{
    /**
     * Used to get and insert script context in current execution context.
     */
    private Execution execution;

    /**
     * The {@link ScriptContextInitializer} list used to initialize {@link ScriptContext}.
     */
    private List<ScriptContextInitializer> scriptContextInitializerList;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.script.ScriptContextManager#getScriptContext()
     */
    public ScriptContext getScriptContext()
    {
        // The Script Context is set in ScriptRequestInterceptor, when the XWiki Request is initialized so we are
        // guaranteed it is defined when this method is called.
        ScriptContext context =
            (ScriptContext) this.execution.getContext()
                .getProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID);

        // Make sure the script context contains the rights value. For example if the context doc could change.
        for (ScriptContextInitializer scriptContextInitializer : this.scriptContextInitializerList) {
            scriptContextInitializer.initialize(context);
        }

        return context;
    }
}
