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
package org.xwiki.rendering.macro.script;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.script.DefaultScriptMacro;
import org.xwiki.rendering.internal.macro.script.NestedScriptMacroValidator;
import org.xwiki.rendering.internal.macro.script.ScriptMacroValidator;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.AbstractMockingComponentTest;
import org.xwiki.test.annotation.ComponentTest;


/**
 * Tests nested script macro validator.
 * 
 * @version $Id$
 * @since 2.4M2
 */
@ComponentTest(value = NestedScriptMacroValidator.class)
public class NestedScriptMacroValidatorTest extends AbstractMockingComponentTest
{
    /** Nested script validator to test. */
    private ScriptMacroValidator<?> validator;

    /**
     * @see org.xwiki.test.AbstractMockingComponentTest#setUp()
     */
    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // fake macro manager returns a script macro for "script" and null else
        final MacroManager macroManager = getComponentManager().lookup(MacroManager.class);
        final ScriptMacro scriptMacro = new DefaultScriptMacro();
        getMockery().checking(new Expectations() {{
            allowing(macroManager).getMacro(with(new MacroId("script")));
                will(returnValue(scriptMacro));
            allowing(macroManager).getMacro(with(any(MacroId.class)));
                will(returnValue(null));
        }});

        this.validator = getComponentManager().lookup(ScriptMacroValidator.class, "nested");
    }

    @Test
    public void testNoNestedScript()
    {
        MacroTransformationContext context = buildContext("script", "script");
        try {
            validator.validate(null, null, context);
            Assert.fail("Nested scripts are allowed");
        } catch (MacroExecutionException exception) {
            // expected
        }
    }

    @Test
    public void testNoNestedScriptInHtml()
    {
        MacroTransformationContext context = buildContext("script", "html", "script");
        try {
            validator.validate(null, null, context);
            Assert.fail("Nested scripts are allowed inside HTML macro");
        } catch (MacroExecutionException exception) {
            // expected
        }
    }

    @Test
    public void testIncludeInterceptsNestedChain()
    {
        MacroTransformationContext context = buildContext("script", "include", "script");
        try {
            validator.validate(null, null, context);
        } catch (MacroExecutionException exception) {
            Assert.fail("Nested scripts are forbidden inside include macro");
        }
    }

    /**
     * Build a chain of nested macros ({@link MacroMarkerBlock} blocks) and put them into a macro transformation
     * context. The chain will be the only child of a top level XDOM, the last child will be the current
     * {@link MacroBlock}.
     * 
     * @param chain list of nested macro names (starting with parent)
     * @return an initialized macro transformation context
     */
    private MacroTransformationContext buildContext(String... chain)
    {
        MacroTransformationContext context = new MacroTransformationContext();
        if (chain == null || chain.length < 1) {
            context.setXDOM(new XDOM(new LinkedList<Block>()));
            return context;
        }

        Map<String, String> parameters = new HashMap<String, String>();
        MacroBlock child = new MacroBlock(chain[chain.length-1], parameters, false);
        Block current = child;
        for (int i = chain.length-2; i >= 0; i--) {
            Block parent = new MacroMarkerBlock(chain[i], parameters, Collections.singletonList(current), false);
            current = parent;
        }
        XDOM root = new XDOM(Collections.singletonList(current));
        context.setXDOM(root);
        context.setCurrentMacroBlock(child);
        return context;
    }
}

