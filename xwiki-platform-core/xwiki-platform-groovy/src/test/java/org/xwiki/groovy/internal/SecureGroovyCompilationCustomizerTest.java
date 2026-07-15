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
package org.xwiki.groovy.internal;

import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SecureGroovyCompilationCustomizer}.
 *
 * @version $Id$
 * @since 4.1M1
 */
@ComponentTest
@AllComponents
class SecureGroovyCompilationCustomizerTest
{
    @MockComponent
    private ConfigurationSource source;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @BeforeEach
    void setUp()
    {
        when(this.source.getProperty("groovy.compilationCustomizers", Collections.emptyList()))
            .thenReturn(List.of("secure"));
    }

    @Test
    void executeWithSecureCustomizerWhenNoProgrammingRights() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);
        ScriptEngine engine = createGroovyEngine();

        // Verify synchronized statements are not authorized
        assertThrows(ScriptException.class, () -> engine.eval("synchronized(this) { }"));
        // Verify we can't call System methods
        assertThrows(ScriptException.class, () -> engine.eval("System.exit(0)"));
        // Verify we can't access private variables
        assertThrows(ScriptException.class, () -> engine.eval("\"Hello World\".value[0]"));

        // Verify we can do a new and use Integer class
        assertDoesNotThrow(() -> engine.eval("new Integer(6)"));
    }

    @Test
    void executeWithSecureCustomizerWhenProgrammingRights() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
        ScriptEngine engine = createGroovyEngine();

        // Verify that the Secure AST Customizer is not active by running a Groovy script that raise an exception
        // when the Secure AST Customizer is active
        assertDoesNotThrow(() -> engine.eval("synchronized(this) { }"));
    }

    private ScriptEngine createGroovyEngine() throws Exception
    {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngineFactory groovyScriptEngineFactory =
            this.componentManager.getInstance(ScriptEngineFactory.class, "groovy");
        manager.registerEngineName("groovy", groovyScriptEngineFactory);
        return manager.getEngineByName("groovy");
    }
}
