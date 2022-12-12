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
package org.xwiki.rendering.macro.code;

import javax.script.ScriptContext;

import org.junit.runner.RunWith;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 3.0RC1
 */
@RunWith(RenderingTestSuite.class)
//@RenderingTestSuite.Scope(pattern = "macrocode20.test")
@AllComponents
public class IntegrationTests
{
    @RenderingTestSuite.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        // Initialize a ScriptContext
        ExecutionContextManager execution = componentManager.getInstance(ExecutionContextManager.class);
        execution.initialize(new ExecutionContext());

        // Inject a custom binding in the script context
        ScriptContextManager scriptContextManager = componentManager.getInstance(ScriptContextManager.class);
        scriptContextManager.getScriptContext().setAttribute("myvar", "<i>script</i>", ScriptContext.GLOBAL_SCOPE);

        // Allow scripting
        componentManager.registerMockComponent(ContextualAuthorizationManager.class);
    }
}
