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
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.rendering.macro.script.ScriptMockSetup;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.test.ComponentManagerTestSetup;

import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Rendering tests for python macro.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class RenderingTests extends TestCase
{
    /**
     * Creates a rendering test suit for testing python macro.
     * 
     * @return rendering test suit for testing python macro.
     * @throws Exception if an error occurs while setting up the test suite.
     */
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test Python Macro");
        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        setUpMocks(testSetup.getComponentManager());

        return testSetup;
    }
    
    public static void setUpMocks(EmbeddableComponentManager componentManager) throws Exception
    {
        Mockery mockery = new Mockery();
        ScriptMockSetup mockSetup = new ScriptMockSetup(mockery, componentManager);
        
        // Script Context Mock
        final ScriptContextManager mockScriptContextManager = mockery.mock(ScriptContextManager.class);
        final SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute("var", "value", ScriptContext.ENGINE_SCOPE);
        mockery.checking(new Expectations() {{
            allowing(mockScriptContextManager).getScriptContext(); will(returnValue(scriptContext));
        }});
        DefaultComponentDescriptor<ScriptContextManager> descriptorSCM =
            new DefaultComponentDescriptor<ScriptContextManager>();
        descriptorSCM.setRole(ScriptContextManager.class);
        componentManager.registerComponent(descriptorSCM, mockScriptContextManager);
    }
}
