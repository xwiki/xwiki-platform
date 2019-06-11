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
package org.xwiki.rendering.wikimacro.macro.wikimacroparameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test of {@link WikiMacroParameterMacro}.
 */
@ComponentTest
public class WikiMacroParameterMacroTest
{
    @InjectMockComponents
    private WikiMacroParameterMacro wikiMacroParameterMacro;

    @MockComponent
    private Execution execution;

    @MockComponent
    private MacroContentParser contentParser;

    @Mock
    private MacroTransformationContext transformationContext;

    XWikiContext xcontext;

    @BeforeEach
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        this.xcontext = new XWikiContext();
        ExecutionContext executionContext = componentManager.registerMockComponent(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.xcontext);
    }

    /**
     * Ensure that if the parameter is null a MacroExecutionException is returned
     */
    @Test
    public void executeWithoutParameter() throws MacroExecutionException
    {
        MacroExecutionException macroExecutionException = assertThrows(MacroExecutionException.class, () -> {
            wikiMacroParameterMacro.execute(null, null, null);
        });
        assertEquals("The parameter is mandatory.", macroExecutionException.getMessage());
    }

    /**
     * Ensure that by default it returns an empty list of blocks.
     */
    @Test
    public void executeWithoutMacro() throws MacroExecutionException
    {
        assertEquals(Collections.emptyList(),
            wikiMacroParameterMacro.execute(new WikiMacroParameterMacroParameters(), null, null));
    }

    /**
     * Ensure that the content of the macro in the context is parsed and a proper metadata is put around.
     */
    @Test
    public void executeWithSimpleMacro() throws MacroExecutionException
    {
        List<WikiMacroParameterDescriptor> parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(new WikiMacroParameterDescriptor("bar",  "", true, null, Block.LIST_BLOCK_TYPE));
        ContentDescriptor contentDescriptor = new DefaultContentDescriptor();
        WikiMacroDescriptor wikiMacroDescriptor = new WikiMacroDescriptor.Builder()
            .name("foo")
            .contentDescriptor(contentDescriptor)
            .parameterDescriptors(parameterDescriptors)
            .build();
        String content = "foobar";

        WikiMacroParameters wikiMacroParameters = new WikiMacroParameters();
        wikiMacroParameters.set("bar", content);

        Map<String, Object> macroInfo = new HashMap<>();
        macroInfo.put("descriptor", wikiMacroDescriptor);
        macroInfo.put("params", wikiMacroParameters);
        this.xcontext.put("macro", macroInfo);
        when(this.transformationContext.isInline()).thenReturn(false);
        when(this.contentParser.parse(eq(content), eq(this.transformationContext), eq(true), eq(false))).thenReturn(
            new XDOM(Collections.singletonList(new WordBlock("foobar")))
        );

        WikiMacroParameterMacroParameters wikiMacroParameterMacroParameters = new WikiMacroParameterMacroParameters();
        wikiMacroParameterMacroParameters.setName("bar");

        MetaData metaData = new MetaData();
        metaData.addMetaData("non-generated-content", "java.util.List<org.xwiki.rendering.block.Block>");
        metaData.addMetaData("parameter-name", "bar");
        metaData.addMetaData("wikimacrocontent", "true");
        List<Block> expectedBlocks = Collections.singletonList(new MetaDataBlock(
            Collections.singletonList(new WordBlock("foobar")), metaData));

        assertEquals(expectedBlocks, this.wikiMacroParameterMacro.execute(wikiMacroParameterMacroParameters, null,
            this.transformationContext));
    }
}
