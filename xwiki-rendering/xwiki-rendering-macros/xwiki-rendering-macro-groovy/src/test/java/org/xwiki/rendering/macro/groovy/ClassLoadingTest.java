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
        
        this.macro = getComponentManager().lookup(Macro.class, "groovy");
        
        this.context = new MacroTransformationContext();
        // The script macro checks the current block (which is a macro block) to see what engine to use
        this.context.setCurrentMacroBlock(new MacroBlock("groovy", Collections.<String, String>emptyMap(), false));
        // Set the syntax since the script macro needs it to parse the script result using that syntax
        this.context.setSyntax(Syntax.XWIKI_2_0);
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
}
