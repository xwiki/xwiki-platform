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
package org.xwiki.rendering.wikimacro.macro.wikimacrocontent;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

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
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test of {@link WikiMacroContentMacro}.
 */
@ComponentTest
public class WikiMacroContentMacroTest
{
    @InjectMockComponents
    private WikiMacroContentMacro wikiMacroContentMacro;

    @MockComponent
    private Execution execution;

    @MockComponent
    private MacroContentParser contentParser;

    @MockComponent
    @Named("plain/1.0")
    private Parser plainParser;

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
     * Ensure that by default it returns an empty list of blocks.
     */
    @Test
    public void executeWithoutMacro() throws MacroExecutionException
    {
        assertEquals(Collections.emptyList(), wikiMacroContentMacro.execute(null,null,null));
    }

    /**
     * Ensure that the content of the macro in the context is parsed and a proper metadata is put around.
     */
    @Test
    public void executeWithSimpleMacro() throws MacroExecutionException
    {
        ContentDescriptor contentDescriptor = new DefaultContentDescriptor("", false, Block.LIST_BLOCK_TYPE);
        MacroDescriptor macroDescriptor = new DefaultMacroDescriptor(new MacroId("mywikimacro"), "mywikimacro", "",
            contentDescriptor);
        String content = "foobar";

        Map<String, Object> macroInfo = new HashMap<>();
        macroInfo.put("content", content);
        macroInfo.put("descriptor", macroDescriptor);
        this.xcontext.put("macro", macroInfo);
        when(this.transformationContext.isInline()).thenReturn(false);
        when(this.contentParser.parse(eq(content), eq(this.transformationContext), eq(true), eq(false))).thenReturn(
            new XDOM(Collections.singletonList(new WordBlock("foobar")))
        );

        MetaData metaData = new MetaData();
        metaData.addMetaData("non-generated-content", "java.util.List<org.xwiki.rendering.block.Block>");
        metaData.addMetaData("wikimacrocontent", "true");
        List<Block> expectedBlocks = Collections.singletonList(new MetaDataBlock(
            Collections.singletonList(new WordBlock("foobar")), metaData));

        assertEquals(expectedBlocks, this.wikiMacroContentMacro.execute(null, null, this.transformationContext));
    }

    /**
     * Ensure that the content of the macro in the context is parsed and a proper metadata is put around, even if the
     * macro descriptor is null.
     */
    @Test
    public void executeWithMacroDescriptorNull() throws MacroExecutionException, ParseException
    {
        String content = "foobar";

        Map<String, Object> macroInfo = new HashMap<>();
        macroInfo.put("content", content);
        this.xcontext.put("macro", macroInfo);
        when(this.transformationContext.isInline()).thenReturn(false);
        when(this.plainParser.parse(any())).thenReturn(
            new XDOM(Collections.singletonList(new WordBlock("foobar")))
        );

        MetaData metaData = new MetaData();
        metaData.addMetaData("non-generated-content", "java.lang.String");
        metaData.addMetaData("wikimacrocontent", "true");
        List<Block> expectedBlocks = Collections.singletonList(new MetaDataBlock(
            Collections.singletonList(new WordBlock("foobar")), metaData));

        assertEquals(expectedBlocks, this.wikiMacroContentMacro.execute(null, null, this.transformationContext));
    }

    /**
     * Ensure that the content of the macro in the context is parsed and a proper metadata is put around.
     */
    @Test
    public void executeWithSimpleMacroDefaultType() throws MacroExecutionException, ParseException
    {
        ContentDescriptor contentDescriptor = new DefaultContentDescriptor("", false);
        MacroDescriptor macroDescriptor = new DefaultMacroDescriptor(new MacroId("mywikimacro"), "mywikimacro", "",
            contentDescriptor);
        String content = "**foobar**";

        Map<String, Object> macroInfo = new HashMap<>();
        macroInfo.put("content", content);
        macroInfo.put("descriptor", macroDescriptor);
        this.xcontext.put("macro", macroInfo);
        when(this.transformationContext.isInline()).thenReturn(false);
        when(this.plainParser.parse(any())).thenReturn(
            new XDOM(Collections.singletonList(new WordBlock("foobar")))
        );

        MetaData metaData = new MetaData();
        metaData.addMetaData("non-generated-content", "java.lang.String");
        metaData.addMetaData("wikimacrocontent", "true");
        List<Block> expectedBlocks = Collections.singletonList(new MetaDataBlock(
            Collections.singletonList(new WordBlock("foobar")), metaData));

        assertEquals(expectedBlocks, this.wikiMacroContentMacro.execute(null, null, this.transformationContext));
    }
}
