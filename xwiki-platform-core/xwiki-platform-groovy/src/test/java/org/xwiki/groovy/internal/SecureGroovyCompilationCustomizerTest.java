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

import java.security.Permission;
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
    private static class NoExitSecurityManager extends SecurityManager
    {
        @Override
        public void checkPermission(Permission perm)
        {
            // Allow anything.
        }

        @Override
        public void checkPermission(Permission perm, Object context)
        {
            // Allow anything.
        }

        @Override
        public void checkExit(int status)
        {
            super.checkExit(status);
            throw new RuntimeException("System.exit called!");
        }
    }

    @Test
    public void executeWithSecureCustomizerWhenNoProgrammingRights() throws Exception
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

        final ScriptEngine engine = manager.getEngineByName("groovy");

        try {
            engine.eval("System.exit(0)");
            Assert.fail("Should have thrown an exception here");
        } catch (ScriptException e) {
            Assert.assertTrue(e.getMessage().contains(
                "Expression [MethodCallExpression] is not allowed: java.lang.System.exit(0)"));
        }
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

        // Run with a special Security Manager so that calling System.exit() will not exit the JVM.
        SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());

        try {
            engine.eval("System.exit(0)");
            Assert.fail("Should have thrown an exception here");
        } catch (ScriptException e) {
            Assert.assertTrue(e.getMessage().contains("System.exit called!"));
        } finally {
            System.setSecurityManager(sm);
        }
    }
}
