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
package org.xwiki.lesscss.internal.compiler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.script.internal.CloneableSimpleScriptContext;
import org.xwiki.script.internal.ScriptExecutionContextInitializer;
import org.xwiki.velocity.VelocityContextFactory;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.internal.VelocityExecutionContextInitializer;

/**
 * Executes some code with new script and Velocity contexts, so that the code doesn't see the variables set earlier in
 * the current execution, and the variables it sets are not visible after it.
 * <p>
 * Both contexts are replaced because each Velocity variable is also copied to the script context, and a new Velocity
 * context gets back the variables of the script context. The new contexts hold only the initial bindings.
 *
 * @version $Id$
 * @since 19.0.0RC1
 */
@Component(roles = ScriptContextIsolator.class)
@Singleton
public class ScriptContextIsolator
{
    @Inject
    private Execution execution;

    @Inject
    private VelocityContextFactory velocityContextFactory;

    /**
     * Execute the given code with new script and Velocity contexts, and restore the current ones afterwards.
     *
     * @param <T> the type of the result
     * @param callable the code to execute
     * @return the result of the code
     * @throws Exception if the new contexts cannot be created or if the code fails
     */
    public <T> T call(Callable<T> callable) throws Exception
    {
        ExecutionContext executionContext = this.execution.getContext();
        if (executionContext == null) {
            return callable.call();
        }

        Map<String, Object> previousContexts = isolate(executionContext);
        try {
            return callable.call();
        } finally {
            restore(executionContext, previousContexts);
        }
    }

    private Map<String, Object> isolate(ExecutionContext executionContext) throws XWikiVelocityException
    {
        Map<String, Object> previousContexts = new HashMap<>();
        previousContexts.put(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID,
            executionContext.getProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID));
        previousContexts.put(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID,
            executionContext.getProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID));

        // Create the new Velocity context first, so that nothing is replaced when it fails.
        Object velocityContext = this.velocityContextFactory.createContext();
        executionContext.setProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID,
            new CloneableSimpleScriptContext());
        executionContext.setProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID, velocityContext);

        return previousContexts;
    }

    private void restore(ExecutionContext executionContext, Map<String, Object> previousContexts)
    {
        for (Map.Entry<String, Object> previousContext : previousContexts.entrySet()) {
            if (previousContext.getValue() != null) {
                executionContext.setProperty(previousContext.getKey(), previousContext.getValue());
            } else {
                executionContext.removeProperty(previousContext.getKey());
            }
        }
    }
}
