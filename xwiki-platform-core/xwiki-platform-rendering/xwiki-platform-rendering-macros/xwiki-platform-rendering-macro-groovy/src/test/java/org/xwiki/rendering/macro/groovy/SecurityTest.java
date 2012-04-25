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
package org.xwiki.rendering.macro.groovy;

import java.util.Arrays;
import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.script.JSR223ScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.AbstractComponentTestCase;

import junit.framework.Assert;

/**
 * Integration test to verify the security configuration of the Groovy Macro.
 *
 * @version $Id$
 * @since 4.1M1
 */
public class SecurityTest extends AbstractComponentTestCase
{
    private DocumentAccessBridge dab;

    private ConfigurationSource configurationSource;

    @Before
    public void setUpMocks() throws Exception
    {
        // Mock Model dependencies.
        this.dab = registerMockComponent(DocumentAccessBridge.class);
        registerMockComponent(AttachmentReferenceResolver.TYPE_STRING, "current");

        // Mock Configuration Source so that we can configure security parameters
        this.configurationSource = registerMockComponent(ConfigurationSource.class);
    }

    @Test
    public void testExecutionWhenSecureCustomizerAndNoProgrammingRights() throws Exception
    {
        getMockery().checking(new Expectations()
        {{
            // No PR
            allowing(dab).hasProgrammingRights();
            will(returnValue(false));

            // Have the secure AST Customizer active!
            allowing(configurationSource).getProperty("groovy.compilationCustomizers", Collections.emptyList());
                will(returnValue(Arrays.asList("secure")));
        }});

        // Note: We execute something that works with the Groovy Security customizer...
        executeGroovyMacro("new Integer(0)");
    }

    @Test
    public void testExecutionWhenNoSecureCustomizerAndNoProgrammingRights() throws Exception
    {
        getMockery().checking(new Expectations()
        {{
            // No PR
            allowing(dab).hasProgrammingRights();
            will(returnValue(false));

            // The secure AST Customizer is not active
            allowing(configurationSource).getProperty("groovy.compilationCustomizers", Collections.emptyList());
                will(returnValue(Collections.emptyList()));
        }});

        Macro macro = getComponentManager().getInstance(Macro.class, "groovy");
        JSR223ScriptMacroParameters parameters = new JSR223ScriptMacroParameters();

        MacroTransformationContext context = new MacroTransformationContext();
        // The script macro checks the current block (which is a macro block) to see what engine to use
        context.setCurrentMacroBlock(new MacroBlock("groovy", Collections.<String, String>emptyMap(), false));

        try {
            macro.execute(parameters, "new Integer(0)", context);
            Assert.fail("Should have thrown an exception here!");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("You don't have the right to execute this script", expected.getMessage());
        }
    }

    @Test
    public void testExecutionWhenNoSecureCustomizerAndProgrammingRights() throws Exception
    {
        getMockery().checking(new Expectations()
        {{
            // No PR
            allowing(dab).hasProgrammingRights();
            will(returnValue(true));

            // The secure AST Customizer is not active
            allowing(configurationSource).getProperty("groovy.compilationCustomizers", Collections.emptyList());
                will(returnValue(Collections.emptyList()));
        }});

        // Note: We execute something that works with the Groovy Security customizer...
        executeGroovyMacro("new Integer(0)");
    }

    @Test
    public void testExecutionWhenSecureCustomizerAndProgrammingRights() throws Exception
    {
        getMockery().checking(new Expectations()
        {{
            // No PR
            allowing(dab).hasProgrammingRights();
            will(returnValue(true));

            // The secure AST Customizer is active
            allowing(configurationSource).getProperty("groovy.compilationCustomizers", Collections.emptyList());
                will(returnValue(Arrays.asList("secure")));
        }});

        // Note: We execute something that is normally caught by the Groovy Secure Customizer
        executeGroovyMacro("synchronized(this) {}");
    }

    private void executeGroovyMacro(String script) throws Exception
    {
        Macro macro = getComponentManager().getInstance(Macro.class, "groovy");
        JSR223ScriptMacroParameters parameters = new JSR223ScriptMacroParameters();

        MacroTransformationContext context = new MacroTransformationContext();
        // The script macro checks the current block (which is a macro block) to see what engine to use
        context.setCurrentMacroBlock(new MacroBlock("groovy", Collections.<String, String>emptyMap(), false));

        macro.execute(parameters, script, context);
    }
}
