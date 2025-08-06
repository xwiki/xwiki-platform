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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Strings;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.script.JSR223ScriptMacroParameters;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.jmock.AbstractComponentTestCase;

/**
 * Integration test to verify the security configuration of the Groovy Macro.
 *
 * @version $Id$
 * @since 4.1M1
 */
public class SecurityTest extends AbstractComponentTestCase
{
    private ContextualAuthorizationManager cam;

    private ConfigurationSource configurationSource;

    @Before
    public void setUpMocks() throws Exception
    {
        // Mock Model dependencies.
        registerMockComponent(DocumentAccessBridge.class);

        // Mock the authorization manager.
        this.cam = registerMockComponent(ContextualAuthorizationManager.class);
        registerMockComponent(AttachmentReferenceResolver.TYPE_STRING, "current");

        // Mock Configuration Source so that we can configure security parameters
        this.configurationSource = registerMockComponent(ConfigurationSource.class);
    }

    // Using a customizer

    @Test
    public void testExecutionWhenSecureCustomizerWithScriptRightsAndNoProgrammingRights() throws Exception
    {
        // Conclusion: Can run with a customizer and SR to avoid PR.
        testExecution(true, false, true, false);
    }

    @Test(expected = MacroExecutionException.class)
    public void testExecutionWhenSecureCustomizerWithNoScriptRightsAndNoProgrammingRights() throws Exception
    {
        // Conclusion: When running with a customizer and no PR, SR is needed.
        testExecution(true, false, false, false);
    }

    @Test
    public void testExecutionWhenSecureCustomizerWithNoScriptRightsAndProgrammingRights() throws Exception
    {
        // Conclusion: When running with a customizer and PR, SR are implied by the PR.
        testExecution(true, false, false, true);
    }

    @Test(expected = MacroExecutionException.class)
    public void testExecutionWhenSecureCustomizerAndRestricted() throws Exception
    {
        // Conclusion: When running with a customizer and SR to avoid PR, transformations have to not be restricted.
        testExecution(true, true, true, false);
    }

    @Test(expected = MacroExecutionException.class)
    public void testExecutionWhenSecureCustomizerAndRestrictedWithScriptRightsAndProgrammingRights() throws Exception
    {
        // Conclusion: When running with a customizer, event with PR (and inherited SR), transformations have to not be
        // restricted.
        testExecution(true, true, false, true);
    }

    // Not using a customizer

    @Test
    public void testExecutionWhenNoSecureCustomizerAndNoRights() throws Exception
    {
        // Conclusion: When running with no customizer and no PR, execution fails.
        try {
            testExecution(false, false, false, false);
            Assert.fail("Should have thrown an exception here!");
        } catch (MacroExecutionException expected) {
            Assert.assertTrue(Strings.CS.startsWith(expected.getMessage(),
                "The execution of the [groovy] script macro is not allowed."));
        }
    }

    @Test(expected = MacroExecutionException.class)
    public void testExecutionWhenNoSecureCustomizerAndScriptRights() throws Exception
    {
        // Conclusion: When running with no customizer, SR is not enough. You need PR.
        testExecution(false, false, true, false);
    }

    @Test
    public void testExecutionWhenNoSecureCustomizerAndProgrammingRights() throws Exception
    {
        // Conclusion: When running with no customizer, PR is needed.
        testExecution(false, false, false, true);
    }

    @Test
    public void testExecutionWhenNoSecureCustomizerAndExecutionRestrictedAndProgrammingRights() throws Exception
    {
        // Conclusion: When running with no customizer, transformation restrictions do not matter, only PR does.
        testExecution(false, true, false, true);
    }

    /*
     * Utility methods.
     */

    private void testExecution(final boolean hasCustomizer, final boolean isRestricted, final boolean hasSR,
        final boolean hasPR) throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                // Programming Rights
                allowing(cam).hasAccess(Right.PROGRAM);
                will(returnValue(hasPR));

                // Script Rights
                allowing(cam).hasAccess(Right.SCRIPT);
                will(returnValue(hasSR || hasPR));

                // Secure AST Customizer
                List<String> customizers = new ArrayList<>();
                if (hasCustomizer) {
                    customizers.add("secure");
                }
                allowing(configurationSource).getProperty("groovy.compilationCustomizers", Collections.emptyList());
                will(returnValue(customizers));

            }
        });

        // Note: We execute something that works with the Groovy Security customizer...
        executeGroovyMacro("new Integer(0)", isRestricted);
    }

    private void executeGroovyMacro(String script) throws Exception
    {
        executeGroovyMacro(script, false);
    }

    private void executeGroovyMacro(String script, boolean restricted) throws Exception
    {
        Macro macro = getComponentManager().getInstance(Macro.class, "groovy");
        JSR223ScriptMacroParameters parameters = new JSR223ScriptMacroParameters();

        MacroTransformationContext context = new MacroTransformationContext();
        context.getTransformationContext().setRestricted(restricted);
        context.setSyntax(Syntax.XWIKI_2_1);
        // The script macro checks the current block (which is a macro block) to see what engine to use.
        context.setCurrentMacroBlock(new MacroBlock("groovy", Collections.<String, String>emptyMap(), false));

        macro.execute(parameters, script, context);
    }
}
