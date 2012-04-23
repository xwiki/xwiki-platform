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

import java.util.Collections;

import org.junit.Test;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.script.JSR223ScriptMacroParameters;
import org.xwiki.rendering.macro.script.ScriptMockSetup;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Integration test to verify that we can pass extra JARs to the Groovy engine when executing a script.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
public class ClassLoadingTest extends AbstractComponentTestCase
{
    private ScriptMockSetup mockSetup;

    private Macro<JSR223ScriptMacroParameters> macro;
    
    private MacroTransformationContext context;
    
    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();
        this.mockSetup = new ScriptMockSetup(getComponentManager());
        
        this.macro = getComponentManager().getInstance(Macro.class, "groovy");
        
        this.context = new MacroTransformationContext();
        // The script macro checks the current block (which is a macro block) to see what engine to use
        this.context.setCurrentMacroBlock(new MacroBlock("groovy", Collections.<String, String>emptyMap(), false));
        // Set the syntax since the script macro needs it to parse the script result using that syntax
        this.context.setSyntax(Syntax.XWIKI_2_0);
    }

    @Test
    public void testDefineClassInOneExecutionAndUseInAnother() throws Exception
    {
        // First execution: define a class
        JSR223ScriptMacroParameters params = new JSR223ScriptMacroParameters();
        this.macro.execute(params, "class MyClass {}", this.context);

        // Second execution: use the defined class
        this.macro.execute(params, "def var = new MyClass()", this.context);
    }

    @Test
    public void testExtraJarLocatedAtURL() throws Exception
    {
        // Use a dummy JAR to verify that some passed URL is indeed added to the CL. That JAR only contains
        // an empty Dummy class.
        JSR223ScriptMacroParameters params = new JSR223ScriptMacroParameters();
        params.setJars(getClass().getClassLoader().getResource("dummy.jar").toString());
        
        // The test would fail after the next line if the Dummy class wasn't present in the classloader used to 
        // execute the script.
        this.macro.execute(params, "def var = new Dummy()", this.context);
    }
    
    /**
     * Verify that it's possible to add jars to the class loader used by the Groovy engine
     * across multiple invocations of the Groovy macro. 
     */
    @Test
    public void testJarParamsInSecondMacro() throws Exception
    {
        JSR223ScriptMacroParameters params = new JSR223ScriptMacroParameters();

        // Execute a first macro without any jars param passed
        this.macro.execute(params, "def var", this.context);

        // Execute a second macro this time with jars param and verify it works
        params.setJars(getClass().getClassLoader().getResource("dummy.jar").toString());
        this.macro.execute(params, "def var = new Dummy()", this.context);
    }

    /**
     * Verify that it works if we define an interface in a first macro execution and then define a second
     * macro execution which uses the defined interface and also has jar params different from the first
     * macro execution. Also test if a third execution with no params but using a previously defined
     * interface also works.
     */
    @Test
    public void testDefineClassInFirstExecutionAndJarParamsInAnother() throws Exception
    {
        this.macro.execute(new JSR223ScriptMacroParameters(), "class MyClass {}", this.context);

        // Second execution: use the defined class but with different jar params
        JSR223ScriptMacroParameters params = new JSR223ScriptMacroParameters();
        params.setJars(getClass().getClassLoader().getResource("dummy.jar").toString());
        this.macro.execute(params, "def var = new MyClass()", this.context);
        
        // Third execution without 
        this.macro.execute(new JSR223ScriptMacroParameters(), "def var = new MyClass()", this.context);
    }
}
