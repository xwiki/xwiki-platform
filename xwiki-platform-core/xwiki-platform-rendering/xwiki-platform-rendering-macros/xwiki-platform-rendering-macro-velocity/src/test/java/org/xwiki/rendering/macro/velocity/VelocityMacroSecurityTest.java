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

import java.util.Collections;

import org.junit.Test;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.script.ScriptMockSetup;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Verify that a Velocity macro defined in one page is not visible from another page.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class VelocityMacroSecurityTest extends AbstractComponentTestCase
{
    private Macro<VelocityMacroParameters> velocityMacro;

    @Override
    protected void registerComponents() throws Exception
    {
        new ScriptMockSetup(getMockery(), getComponentManager());
        this.velocityMacro = getComponentManager().getInstance(Macro.class, "velocity");
    }

    @Test(expected=MacroExecutionException.class)
    public void testVelocityMacroIsolation() throws Exception
    {

        VelocityMacroParameters params = new VelocityMacroParameters();
        MacroTransformationContext context = new MacroTransformationContext();
        context.getTransformationContext().setRestricted(true);
        context.setSyntax(Syntax.XWIKI_2_0);
        context.setCurrentMacroBlock(new MacroBlock("velocity", Collections.<String, String>emptyMap(), false));

        context.setId("page1");
        this.velocityMacro.execute(params, "#macro(testMacrosAreLocal)mymacro#end", context);
    }
}