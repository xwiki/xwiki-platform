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

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.script.internal.ScriptExecutionContextInitializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityContextFactory;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.internal.VelocityExecutionContextInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Validate {@link ScriptContextIsolator}.
 *
 * @version $Id$
 */
@ComponentTest
class ScriptContextIsolatorTest
{
    private static final String VARIABLE = "themeObj";

    private static final String REQUEST_VALUE = "request value";

    private static final String ISOLATED_VALUE = "isolated value";

    @InjectMockComponents
    private ScriptContextIsolator isolator;

    @MockComponent
    private Execution execution;

    @MockComponent
    private VelocityContextFactory velocityContextFactory;

    private ExecutionContext executionContext;

    private VelocityContext requestVelocityContext;

    private ScriptContext requestScriptContext;

    @BeforeEach
    void beforeEach() throws Exception
    {
        // A Velocity variable set earlier in the request is also copied to the script context.
        this.executionContext = new ExecutionContext();
        this.requestVelocityContext = new VelocityContext();
        this.requestVelocityContext.put(VARIABLE, REQUEST_VALUE);
        this.executionContext.setProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID,
            this.requestVelocityContext);
        this.requestScriptContext = new SimpleScriptContext();
        this.requestScriptContext.setAttribute(VARIABLE, REQUEST_VALUE, ScriptContext.ENGINE_SCOPE);
        this.executionContext.setProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID,
            this.requestScriptContext);
        when(this.execution.getContext()).thenReturn(this.executionContext);
        when(this.velocityContextFactory.createContext()).thenReturn(new VelocityContext());
    }

    @Test
    void call() throws Exception
    {
        // Read the variable and then set it, like the color theme initialization of the skin does.
        String result = this.isolator.call(() -> {
            VelocityContext velocityContext = getVelocityContext();
            ScriptContext scriptContext = getScriptContext();
            String values = velocityContext.get(VARIABLE) + " " + scriptContext.getAttribute(VARIABLE);
            velocityContext.put(VARIABLE, ISOLATED_VALUE);
            scriptContext.setAttribute(VARIABLE, ISOLATED_VALUE, ScriptContext.ENGINE_SCOPE);
            return values;
        });

        assertEquals("null null", result);
        assertRequestContextsRestored();
    }

    @Test
    void callWhenTheCodeFails()
    {
        IllegalStateException exception = new IllegalStateException();

        assertSame(exception, assertThrows(IllegalStateException.class, () -> this.isolator.call(() -> {
            getVelocityContext().put(VARIABLE, ISOLATED_VALUE);
            throw exception;
        })));
        assertRequestContextsRestored();
    }

    @Test
    void callWhenTheVelocityContextCannotBeCreated() throws Exception
    {
        XWikiVelocityException exception = new XWikiVelocityException("error");
        when(this.velocityContextFactory.createContext()).thenThrow(exception);

        assertSame(exception, assertThrows(XWikiVelocityException.class, () -> this.isolator.call(() -> "result")));
        assertRequestContextsRestored();
    }

    @Test
    void callWithoutRequestContexts() throws Exception
    {
        this.executionContext.removeProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID);
        this.executionContext.removeProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID);

        assertEquals("result", this.isolator.call(() -> "result"));

        assertFalse(this.executionContext.hasProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID));
        assertFalse(this.executionContext.hasProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID));
    }

    @Test
    void callWithoutExecutionContext() throws Exception
    {
        when(this.execution.getContext()).thenReturn(null);

        assertEquals("result", this.isolator.call(() -> "result"));
    }

    private VelocityContext getVelocityContext()
    {
        return (VelocityContext) this.executionContext
            .getProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID);
    }

    private ScriptContext getScriptContext()
    {
        return (ScriptContext) this.executionContext.getProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID);
    }

    private void assertRequestContextsRestored()
    {
        assertSame(this.requestVelocityContext, getVelocityContext());
        assertSame(this.requestScriptContext, getScriptContext());
        assertEquals(REQUEST_VALUE, this.requestVelocityContext.get(VARIABLE));
        assertEquals(REQUEST_VALUE, this.requestScriptContext.getAttribute(VARIABLE));
    }
}
