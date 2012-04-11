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

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.script.event.ScriptEvaluatingEvent;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.script.DefaultScriptMacro;
import org.xwiki.rendering.internal.macro.script.NestedScriptMacroValidatorListener;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link NestedScriptMacroValidatorListener}.
 * 
 * @version $Id$
 * @since 2.4M2
 */
public class NestedScriptMacroValidatorTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private NestedScriptMacroValidatorListener validator;

    /**
     * @see org.xwiki.test.AbstractMockingComponentTestCase#configure()
     */
    @Override
    public void configure() throws Exception
    {
        // Mock macro manager returns a script macro for "script" and null otherwise.
        final MacroManager macroManager = getComponentManager().getInstance(MacroManager.class);
        final ScriptMacro scriptMacro = new DefaultScriptMacro();
        final TestNestedScriptMacroEnabled nestedScriptMacroEnabled = new TestNestedScriptMacroEnabled();
        getMockery().checking(new Expectations() {{
            allowing(macroManager).getMacro(with(new MacroId("script")));
                will(returnValue(scriptMacro));
                allowing(macroManager).getMacro(with(new MacroId("nestedscriptmacroenabled")));
                will(returnValue(nestedScriptMacroEnabled));
            allowing(macroManager).getMacro(with(any(MacroId.class)));
                will(returnValue(null));
        }});
    }

    @Test
    public void testNoNestedScript() throws Exception
    {
        MacroTransformationContext context = buildContext("script", "script");
        CancelableEvent event = new ScriptEvaluatingEvent();
        this.validator.onEvent(event, context, null);
        Assert.assertTrue(event.isCanceled());
    }

    @Test
    public void testNoNestedScriptInHtml() throws Exception
    {
        MacroTransformationContext context = buildContext("script", "html", "script");
        CancelableEvent event = new ScriptEvaluatingEvent();
        this.validator.onEvent(event, context, null);
        Assert.assertTrue(event.isCanceled());
    }

    @Test
    public void testIncludeInterceptsNestedChain() throws Exception
    {
        MacroTransformationContext context = buildContext("script", "include", "script");
        CancelableEvent event = new ScriptEvaluatingEvent();
        this.validator.onEvent(event, context, null);
        Assert.assertFalse(event.isCanceled());
    }
    
    @Test
    public void testNestedScriptMacroEnabledInterceptsNestedChain() throws Exception
    {
        MacroTransformationContext context = buildContext("script", "nestedscriptmacroenabled", "script");
        CancelableEvent event = new ScriptEvaluatingEvent();
        this.validator.onEvent(event, context, null);
        Assert.assertFalse(event.isCanceled());
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

