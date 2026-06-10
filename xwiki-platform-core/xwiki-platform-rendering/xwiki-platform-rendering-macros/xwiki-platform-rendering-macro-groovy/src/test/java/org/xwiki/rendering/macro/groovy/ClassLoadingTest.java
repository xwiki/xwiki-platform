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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.script.JSR223ScriptMacroParameters;
import org.xwiki.rendering.macro.script.JUnit5ScriptMockSetup;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

/**
 * Integration test to verify that we can pass extra JARs to the Groovy engine when executing a script.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
@ComponentTest
@AllComponents(excludes = {
    org.xwiki.environment.internal.StandardEnvironment.class,
    org.xwiki.velocity.internal.VelocityExecutionContextInitializer.class
})
class ClassLoadingTest
{
    @MockComponent
    private Environment environment;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private Macro<JSR223ScriptMacroParameters> macro;
    
    private MacroTransformationContext context;
    
    @BeforeEach
    void setUp() throws Exception
    {
        MemoryConfigurationSource configurationSource = new MemoryConfigurationSource();
        this.componentManager.registerComponent(ConfigurationSource.class, configurationSource);
        this.componentManager.registerComponent(ConfigurationSource.class, "xwikicfg", configurationSource);

        new JUnit5ScriptMockSetup(this.componentManager);

        Execution execution = this.componentManager.getInstance(Execution.class);
        ExecutionContextManager ecm = this.componentManager.getInstance(ExecutionContextManager.class);
        ecm.initialize(new ExecutionContext());
        execution.getContext().setProperty("xwikicontext", new HashMap<>());

        this.macro = this.componentManager.getInstance(Macro.class, "groovy");
        
        this.context = new MacroTransformationContext();
        // The script macro checks the current block (which is a macro block) to see what engine to use
        this.context.setCurrentMacroBlock(new MacroBlock("groovy", Collections.emptyMap(), false));
        // Set the syntax since the script macro needs it to parse the script result using that syntax
        this.context.setSyntax(Syntax.XWIKI_2_0);
    }

    @Test
    void defineClassInOneExecutionAndUseInAnother()
    {
        // First execution: define a class
        JSR223ScriptMacroParameters params = new JSR223ScriptMacroParameters();
        assertDoesNotThrow(() -> this.macro.execute(params, "class MyClass {}", this.context));

        // Second execution: use the defined class
        assertDoesNotThrow(() -> this.macro.execute(params, "def var = new MyClass()", this.context));
    }

    @Test
    void extraJarLocatedAtURL()
    {
        // Use a dummy JAR to verify that some passed URL is indeed added to the CL. That JAR only contains
        // an empty Dummy class.
        JSR223ScriptMacroParameters params = new JSR223ScriptMacroParameters();
        params.setJars(Objects.requireNonNull(getClass().getClassLoader().getResource("dummy.jar")).toString());
        
        // The test would fail after the next line if the Dummy class wasn't present in the classloader used to 
        // execute the script.
        assertDoesNotThrow(() -> this.macro.execute(params, "def var = new Dummy()", this.context));
    }

    /**
     * Verify that it's possible to add jars to the class loader used by the Groovy engine
     * across multiple invocations of the Groovy macro. 
     */
    @Test
    void jarParamsInSecondMacro()
    {
        JSR223ScriptMacroParameters params = new JSR223ScriptMacroParameters();

        // Execute a first macro, without any jars param passed
        assertDoesNotThrow(() -> this.macro.execute(params, "def var", this.context));

        // Execute a second macro this time with jars param and verify it works
        params.setJars(Objects.requireNonNull(getClass().getClassLoader().getResource("dummy.jar")).toString());
        assertDoesNotThrow(() -> this.macro.execute(params, "def var = new Dummy()", this.context));
    }

    /**
     * Verify that it works if we define an interface in a first macro execution and then define a second
     * macro execution which uses the defined interface and also has jar params different from the first
     * macro execution. Also test if a third execution with no params but using a previously defined
     * interface also works.
     */
    @Test
    void defineClassInFirstExecutionAndJarParamsInAnother()
    {
        assertDoesNotThrow(
            () -> this.macro.execute(new JSR223ScriptMacroParameters(), "class MyClass {}", this.context));

        // Second execution: use the defined class but with different jar params
        JSR223ScriptMacroParameters params = new JSR223ScriptMacroParameters();
        params.setJars(Objects.requireNonNull(getClass().getClassLoader().getResource("dummy.jar")).toString());
        assertDoesNotThrow(() -> this.macro.execute(params, "def var = new MyClass()", this.context));
        
        // Third execution without 
        assertDoesNotThrow(
            () -> this.macro.execute(new JSR223ScriptMacroParameters(), "def var = new MyClass()", this.context));
    }
}
