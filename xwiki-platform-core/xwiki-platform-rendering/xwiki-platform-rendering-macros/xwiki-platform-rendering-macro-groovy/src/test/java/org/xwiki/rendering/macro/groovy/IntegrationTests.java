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

import java.io.File;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.StandardEnvironment;
import org.xwiki.rendering.macro.script.JUnit5ScriptMockSetup;
import org.xwiki.rendering.test.integration.junit5.RenderingTests;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.Mockito.when;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 3.0RC1
 */
@AllComponents
@ExtendWith(XWikiTempDirExtension.class)
@RenderingTests.Scope(pattern = "macrogroovy.*")
public class IntegrationTests implements RenderingTests
{
    @XWikiTempDir
    private File permanentDir;

    @RenderingTests.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        new JUnit5ScriptMockSetup(componentManager);

        // Script Context Mock
        ScriptContextManager scm = componentManager.registerMockComponent(ScriptContextManager.class);
        SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute("var", "value", ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("services", componentManager.getInstance(ScriptServiceManager.class),
            ScriptContext.ENGINE_SCOPE);
        when(scm.getScriptContext()).thenReturn(scriptContext);

        // Set up the permanent directory in the target directory (so that it doesn't fall back on a temporary directory
        // outside the maven build directory (bad practice)
        StandardEnvironment environment = componentManager.getInstance(Environment.class);
        environment.setPermanentDirectory(this.permanentDir);
    }
}
