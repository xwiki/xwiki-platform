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
import java.util.HashMap;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.script.JSR223ScriptMacroParameters;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Integration test to verify the security configuration of the Groovy Macro.
 *
 * @version $Id$
 * @since 4.1M1
 */
@ComponentTest
@AllComponents(excludes = {
    org.xwiki.environment.internal.StandardEnvironment.class,
    org.xwiki.velocity.internal.VelocityExecutionContextInitializer.class
})
class SecurityTest
{
    @MockComponent
    private ContextualAuthorizationManager cam;

    @MockComponent
    private ConfigurationSource configurationSource;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentReferenceResolver;

    @MockComponent
    private Environment environment;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @BeforeEach
    void setUp() throws Exception
    {
        Execution execution = this.componentManager.getInstance(Execution.class);
        ExecutionContextManager ecm = this.componentManager.getInstance(ExecutionContextManager.class);
        ecm.initialize(new ExecutionContext());
        execution.getContext().setProperty("xwikicontext", new HashMap<>());
    }

    // Using a customizer

    @Test
    void testExecutionWhenSecureCustomizerWithScriptRightsAndNoProgrammingRights() throws Exception
    {
        // Conclusion: Can run with a customizer and SR to avoid PR.
        testExecution(true, false, true, false);
    }

    @Test
    void testExecutionWhenSecureCustomizerWithNoScriptRightsAndNoProgrammingRights()
    {
        // Conclusion: When running with a customizer and no PR, SR is needed.
        assertThrows(MacroExecutionException.class, () -> testExecution(true, false, false, false));
    }

    @Test
    void testExecutionWhenSecureCustomizerWithNoScriptRightsAndProgrammingRights() throws Exception
    {
        // Conclusion: When running with a customizer and PR, SR are implied by the PR.
        testExecution(true, false, false, true);
    }

    @Test
    void testExecutionWhenSecureCustomizerAndRestricted()
    {
        // Conclusion: When running with a customizer and SR to avoid PR, transformations have to not be restricted.
        assertThrows(MacroExecutionException.class, () -> testExecution(true, true, true, false));
    }

    @Test
    void testExecutionWhenSecureCustomizerAndRestrictedWithScriptRightsAndProgrammingRights()
    {
        // Conclusion: When running with a customizer, event with PR (and inherited SR), transformations have to not be
        // restricted.
        assertThrows(MacroExecutionException.class, () -> testExecution(true, true, false, true));
    }

    // Not using a customizer

    @Test
    void testExecutionWhenNoSecureCustomizerAndNoRights()
    {
        // Conclusion: When running with no customizer and no PR, execution fails.
        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> testExecution(false, false, false, false));
        assertTrue(exception.getMessage().startsWith("The execution of the [groovy] script macro is not allowed."));
    }

    @Test
    void testExecutionWhenNoSecureCustomizerAndScriptRights()
    {
        // Conclusion: When running with no customizer, SR is not enough. You need PR.
        assertThrows(MacroExecutionException.class, () -> testExecution(false, false, true, false));
    }

    @Test
    void testExecutionWhenNoSecureCustomizerAndProgrammingRights() throws Exception
    {
        // Conclusion: When running with no customizer, PR is needed.
        testExecution(false, false, false, true);
    }

    @Test
    void testExecutionWhenNoSecureCustomizerAndExecutionRestrictedAndProgrammingRights() throws Exception
    {
        // Conclusion: When running with no customizer, transformation restrictions do not matter, only PR does.
        testExecution(false, true, false, true);
    }

    /*
     * Utility methods.
     */

    private void testExecution(boolean hasCustomizer, boolean isRestricted, boolean hasSR, boolean hasPR)
        throws Exception
    {
        when(this.cam.hasAccess(Right.PROGRAM)).thenReturn(hasPR);
        when(this.cam.hasAccess(Right.SCRIPT)).thenReturn(hasSR || hasPR);

        List<String> customizers = new ArrayList<>();
        if (hasCustomizer) {
            customizers.add("secure");
        }
        doReturn(customizers).when(this.configurationSource)
            .getProperty("groovy.compilationCustomizers", Collections.emptyList());

        // Note: We execute something that works with the Groovy Security customizer...
        executeGroovyMacro("new Integer(0)", isRestricted);
    }

    private void executeGroovyMacro(String script, boolean restricted) throws Exception
    {
        Macro<JSR223ScriptMacroParameters> macro =
            this.componentManager.getInstance(Macro.class, "groovy");
        JSR223ScriptMacroParameters parameters = new JSR223ScriptMacroParameters();

        MacroTransformationContext context = new MacroTransformationContext();
        context.getTransformationContext().setRestricted(restricted);
        context.setSyntax(Syntax.XWIKI_2_1);
        // The script macro checks the current block (which is a macro block) to see what engine to use.
        context.setCurrentMacroBlock(new MacroBlock("groovy", Collections.emptyMap(), false));

        macro.execute(parameters, script, context);
    }
}
