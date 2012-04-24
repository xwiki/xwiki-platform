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

import java.util.Arrays;
import java.util.Collections;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link SecureGroovyCompilationCustomizer}.
 *
 * @version $Id$
 * @since 4.1M1
 */
public class SecureGroovyCompilationCustomizerTest extends AbstractComponentTestCase
{
    private ScriptEngine engine;

    @Test
    public void executeWithSecureCustomizerWhenNoProgrammingRights() throws Exception
    {
        setUpWhenNoProgrammingRights();

        // Verify synchronized statements are not authorized
        assertProtectedScript("synchronized(this) { }");
        // Verify we can't call System methods
        assertProtectedScript("System.exit(0)");
        // Verify we can't access private variables
        assertProtectedScript("\"Hello World\".value[0]");

        // Verify we can do a new and use Integer class
        assertSafeScript("new Integer(6)");
    }

    @Test
    public void executeWithSecureCustomizerWhenProgrammingRights() throws Exception
    {
        final ConfigurationSource source = registerMockComponent(ConfigurationSource.class);
        final DocumentAccessBridge dab = registerMockComponent(DocumentAccessBridge.class);

        getMockery().checking(new Expectations()
        {{
            oneOf(source).getProperty("groovy.compilationCustomizers", Collections.emptyList());
                will(returnValue(Arrays.asList("secure")));
            oneOf(dab).hasProgrammingRights();
                will(returnValue(true));
        }});

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngineFactory groovyScriptEngineFactory =
            getComponentManager().getInstance(ScriptEngineFactory.class, "groovy");
        manager.registerEngineName("groovy", groovyScriptEngineFactory);

        final ScriptEngine engine = manager.getEngineByName("groovy");

        // Verify that the Secure AST Customizer is not active by running a Groovy script that raise an exception
        // when the Secure AST Customizer is active
        engine.eval("synchronized(this) { }");
    }

    private void setUpWhenNoProgrammingRights() throws Exception
    {
        final ConfigurationSource source = registerMockComponent(ConfigurationSource.class);
        final DocumentAccessBridge dab = registerMockComponent(DocumentAccessBridge.class);

        getMockery().checking(new Expectations()
        {{
            oneOf(source).getProperty("groovy.compilationCustomizers", Collections.emptyList());
                will(returnValue(Arrays.asList("secure")));
            oneOf(dab).hasProgrammingRights();
                will(returnValue(false));
        }});

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngineFactory groovyScriptEngineFactory =
            getComponentManager().getInstance(ScriptEngineFactory.class, "groovy");
        manager.registerEngineName("groovy", groovyScriptEngineFactory);

        this.engine = manager.getEngineByName("groovy");
    }

    private void assertProtectedScript(String script)
    {
        try {
            engine.eval(script);
            Assert.fail("Should have thrown an exception here");
        } catch (ScriptException e) {
            // Expected, test passed!
        }
    }

    private void assertSafeScript(String script) throws Exception
    {
        engine.eval(script);
    }
}
