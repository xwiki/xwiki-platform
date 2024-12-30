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
package org.xwiki.rendering.macro.velocity;

import java.io.File;

import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.StandardEnvironment;
import org.xwiki.rendering.macro.script.JUnit5ScriptMockSetup;
import org.xwiki.rendering.test.integration.Initialized;
import org.xwiki.rendering.test.integration.Scope;
import org.xwiki.rendering.test.integration.junit5.RenderingTest;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;
import org.xwiki.test.mockito.MockitoComponentManager;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 3.0RC1
 */
@AllComponents
@ExtendWith(XWikiTempDirExtension.class)
@Scope(pattern = "macrovelocity.*")
public class IntegrationTests extends RenderingTest
{
    @XWikiTempDir
    private File permanentDir;

    @Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        new JUnit5ScriptMockSetup(componentManager);

        // Set up the permanent directory in the target directory (so that it doesn't fall back on a temporary directory
        // outside the maven build directory (bad practice)
        StandardEnvironment environment = componentManager.getInstance(Environment.class);
        environment.setPermanentDirectory(this.permanentDir);

        // Note: We're using a static Mock for the VelocityManager since it leads to a cleaner code
        // (the Include Macro is reusing it too). We register this mock as real component in components.txt.
    }
}
