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

import java.util.Collections;
import java.util.List;

import javax.script.ScriptContext;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.script.ScriptContextInitializer;
import org.xwiki.script.ScriptContextManager;

/**
 * Default implementation of {@link ScriptContextManager}.
 * 
 * @version $Id$
 */
public class DefaultScriptContextManager extends AbstractLogEnabled implements ScriptContextManager, Composable,
    Initializable
{
    /**
     * Used to get and insert script context in current execution context.
     */
    private Execution execution;

    /**
     * Used to get all the {@link ScriptContextInitializer}.
     */
    private ComponentManager componentManager;

    /**
     * The {@link ScriptContextInitializer} list used to initialize {@link ScriptContext}.
     */
    private List<ScriptContextInitializer> scriptContextInitialiserList;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Composable#compose(org.xwiki.component.manager.ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        try {
            this.scriptContextInitialiserList = this.componentManager.lookupList(ScriptContextInitializer.ROLE);
        } catch (ComponentLookupException e) {
            getLogger().error("Failed to lookup script context initializers", e);

            this.scriptContextInitialiserList = Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.script.ScriptContextManager#getScriptContext()
     */
    public ScriptContext getScriptContext()
    {
        // The Script Context is set in ScriptRequestInterceptor, when the XWiki Request is initialized so we are
        // guaranteed it is defined when this method is called.
        ScriptContext scriptContext =
            (ScriptContext) this.execution.getContext().getProperty(
                ScriptExecutionContextInitializer.REQUEST_SCRIPT_CONTEXT);

        for (ScriptContextInitializer scriptContextInitializer : this.scriptContextInitialiserList) {
            scriptContextInitializer.initialize(scriptContext);
        }

        return scriptContext;
    }
}
