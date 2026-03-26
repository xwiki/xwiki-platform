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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.script.DefaultScriptMacro;
import org.xwiki.rendering.internal.macro.script.NestedScriptMacroValidatorListener;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.event.ScriptEvaluatingEvent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NestedScriptMacroValidatorListener}.
 * 
 * @version $Id$
 * @since 2.4M2
 */
@ComponentTest
class NestedScriptMacroValidatorTest
{
    @InjectMockComponents
    private NestedScriptMacroValidatorListener listener;

    @MockComponent
    private MacroManager macroManager;

    @BeforeEach
    void beforeEach() throws Exception
    {
        // Mock macro manager returns a script macro for "script" and null otherwise.
        final ScriptMacro scriptMacro = new DefaultScriptMacro();
        final TestNestedScriptMacroEnabled nestedScriptMacroEnabled = new TestNestedScriptMacroEnabled();
        when(this.macroManager.getMacro(new MacroId("script"))).thenReturn((Macro) scriptMacro);
        when(this.macroManager.getMacro(new MacroId("nestedscriptmacroenabled"))).thenReturn((Macro) nestedScriptMacroEnabled);
    }

    @Test
    void testNoNestedScript()
    {
        MacroTransformationContext context = buildContext("script", "script");
        CancelableEvent event = new ScriptEvaluatingEvent();
        this.listener.onEvent(event, context, null);
        assertTrue(event.isCanceled());
    }

    @Test
    void testNoNestedScriptInHtml()
    {
        MacroTransformationContext context = buildContext("script", "html", "script");
        CancelableEvent event = new ScriptEvaluatingEvent();
        this.listener.onEvent(event, context, null);
        assertTrue(event.isCanceled());
    }

    @Test
    void testIncludeInterceptsNestedChain()
    {
        MacroTransformationContext context = buildContext("script", "include", "script");
        CancelableEvent event = new ScriptEvaluatingEvent();
        this.listener.onEvent(event, context, null);
        assertFalse(event.isCanceled());
    }
    
    @Test
    void testNestedScriptMacroEnabledInterceptsNestedChain()
    {
        MacroTransformationContext context = buildContext("script", "nestedscriptmacroenabled", "script");
        CancelableEvent event = new ScriptEvaluatingEvent();
        this.listener.onEvent(event, context, null);
        assertFalse(event.isCanceled());
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

