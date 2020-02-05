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
package org.xwiki.rendering.macro.python;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.xwiki.rendering.macro.script.ScriptMockSetup;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.test.jmock.MockingComponentManager;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 3.0RC1
 */
@RunWith(RenderingTestSuite.class)
@RenderingTestSuite.Scope(pattern = "macropython.*")
public class IntegrationTests
{
    @RenderingTestSuite.Initialized
    public void initialize(MockingComponentManager cm) throws Exception
    {
        Mockery mockery = new JUnit4Mockery();

        new ScriptMockSetup(mockery, cm);

        // Script Context Mock
        final ScriptContextManager scm = cm.registerMockComponent(mockery, ScriptContextManager.class);
        final SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute("var", "value", ScriptContext.ENGINE_SCOPE);
        mockery.checking(new Expectations() {{
            allowing(scm).getScriptContext(); will(returnValue(scriptContext));
        }});
    }
}
