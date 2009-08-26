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

import javax.script.SimpleScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

/**
 * Allow registering the Script Context in the Execution Context object since it's shared during the whole execution of
 * the current request.
 * 
 * @version $Id$
 */
@Component("scriptContext")
public class ScriptExecutionContextInitializer implements ExecutionContextInitializer
{
    /**
     * The id under which the Script Context is stored in the Execution Context.
     */
    public static final String SCRIPT_CONTEXT_ID = "scriptContext";

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.context.ExecutionContextInitializer#initialize(org.xwiki.context.ExecutionContext)
     */
    public void initialize(ExecutionContext executionContext) throws ExecutionContextException
    {
        // We're storing an instance of the Script Context class in the Execution Context so that it can be
        // shared between different script invocations during the lifetime of the Execution Context.        
        executionContext.setProperty(SCRIPT_CONTEXT_ID, new SimpleScriptContext());
    }
}
