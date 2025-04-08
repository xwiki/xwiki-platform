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
package org.xwiki.rendering.internal.macro.code;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.internal.code.layout.CodeLayoutHandler;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroPreparationException;
import org.xwiki.rendering.macro.code.CodeMacroLayout;
import org.xwiki.rendering.macro.code.CodeMacroParameters;
import org.xwiki.rendering.parser.HighlightParser;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Validate {@link CodeMacro}.
 * 
 * @version $Id$
 */
@ComponentTest
class CodeMacroTest
{
    @InjectMockComponents
    private CodeMacro macro;

    @MockComponent
    private HighlightParser highlightParser;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named(CodeMacroLayout.Constants.PLAIN_HINT)
    private CodeLayoutHandler codeLayoutHandler;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
    }

    @Test
    void prepare() throws MacroPreparationException, MacroExecutionException
    {
        MacroBlock block = new MacroBlock("code", Map.of(), false);

        this.macro.prepare(block);

        assertNull(block.getAttribute(CodeMacro.ATTRIBUTE_PREPARE_RESULT));

        block = new MacroBlock("code", Map.of(), "content", false);

        this.macro.prepare(block);

        List<Block> prepared = (List<Block>) block.getAttribute(CodeMacro.ATTRIBUTE_PREPARE_RESULT);
        assertEquals(List.of(new GroupBlock(Map.of("class", "code"))), prepared);

        MacroTransformationContext context = new MacroTransformationContext();
        context.setCurrentMacroBlock(block);
        List<Block> result = this.macro.execute(new CodeMacroParameters(), "content", context);

        assertEquals(prepared.get(0), result.get(0).getChildren().get(0));
        assertNotSame(prepared.get(0), result.get(0).getChildren().get(0));
    }
}
