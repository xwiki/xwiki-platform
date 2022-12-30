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
package org.xwiki.rendering.async;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.rendering.async.internal.AsyncMacro;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.RenderingContextStore;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AsyncMacro}.
 *
 * @version $Id$
 * @since 8.3RC1
 */
@ComponentTest
@ComponentList(TestEnvironment.class)
class AsyncMacroTest
{
    @InjectMockComponents
    private AsyncMacro macro;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private MacroContentParser parser;

    private BlockAsyncRendererExecutor executor;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        this.executor = this.componentManager.getInstance(BlockAsyncRendererExecutor.class);
    }

    @Test
    void executeInRestrictedMode() throws Exception
    {
        MacroBlock macroBlock = new MacroBlock("async", Collections.<String, String>emptyMap(), false);
        MetaData metadata = new MetaData();
        metadata.addMetaData(MetaData.SOURCE, "source");
        XDOM pageXDOM = new XDOM(Arrays.asList(macroBlock), metadata);
        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setSyntax(Syntax.XWIKI_2_0);
        macroContext.setCurrentMacroBlock(macroBlock);
        macroContext.setXDOM(pageXDOM);
        macroContext.getTransformationContext().setRestricted(true);

        XDOM contentXDOM = new XDOM(Arrays.asList(new WordBlock("test")), metadata);
        when(this.parser.parse(eq(""), same(macroContext), eq(false), eq(false))).thenReturn(contentXDOM);

        when(this.executor.execute(any())).thenReturn(new WordBlock("result"));

        this.macro.execute(new AsyncMacroParameters(), "", macroContext);

        ArgumentCaptor<BlockAsyncRendererConfiguration> configurationCaptor =
            ArgumentCaptor.forClass(BlockAsyncRendererConfiguration.class);
        verify(this.executor).execute(configurationCaptor.capture());

        BlockAsyncRendererConfiguration configuration = configurationCaptor.getValue();
        assertTrue(configuration.isResricted());
        assertTrue(configuration.getContextEntries().contains(RenderingContextStore.PROP_RESTRICTED));
    }
}
