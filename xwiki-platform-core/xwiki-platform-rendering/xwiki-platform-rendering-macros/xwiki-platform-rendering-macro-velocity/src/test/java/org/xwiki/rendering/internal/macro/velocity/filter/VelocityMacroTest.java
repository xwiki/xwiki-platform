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
package org.xwiki.rendering.internal.macro.velocity.filter;

import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.velocity.VelocityMacro;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroPreparationException;
import org.xwiki.rendering.macro.velocity.VelocityMacroParameters;
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;
import org.xwiki.velocity.XWikiVelocityException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link VelocityMacro}.
 * 
 * @version $Id$
 */
@ComponentTest
class VelocityMacroTest
{
    @Mock
    private VelocityEngine velocityEngine;

    @MockComponent
    private VelocityManager velocityManager;

    @MockComponent
    private MacroContentParser contentParser;

    @MockComponent
    @Named("filter")
    private VelocityMacroFilter filter;

    @InjectMockComponents
    private VelocityMacro macro;

    private VelocityTemplate getVelocityTemplate(Block block) throws IllegalAccessException
    {
        return (VelocityTemplate) FieldUtils.readField(block.getAttribute(VelocityMacro.MACRO_ATTRIBUTE),
            "velocityTemplate", true);
    }

    @Test
    void evaluatePreparedAndFiltered() throws XWikiVelocityException, MacroExecutionException, MacroPreparationException
    {
        MacroBlock block = new MacroBlock("velocity", Map.of("filter", "filter"), "content", false);

        VelocityTemplate template1 = mock();
        when(this.velocityManager.compile(any(), any())).thenReturn(template1);
        when(this.velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);

        MacroTransformationContext context = new MacroTransformationContext();
        context.setCurrentMacroBlock(block);

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                invocation.<Writer>getArgument(1).write("result");

                return null;
            }
        }).when(this.velocityEngine).evaluate(any(), any(), any(), same(template1));

        List<Block> resultBlocks = List.of(new WordBlock("result"));
        when(this.contentParser.parse("filteredresult", context, false, false)).thenReturn(new XDOM(resultBlocks));

        when(this.filter.isPreparationSupported()).thenReturn(true);
        when(this.filter.prepare("content")).thenReturn("filteredcontent");
        when(this.filter.after(eq("result"), any())).thenReturn("filteredresult");

        // Prepare the block
        this.macro.prepare(block);

        // Transform the block
        VelocityMacroParameters macroParameters = new VelocityMacroParameters();
        macroParameters.setFilter("filter");
        assertEquals(resultBlocks, this.macro.execute(macroParameters, "content", context));
    }

    @Test
    void prepare() throws MacroPreparationException, XWikiVelocityException, IllegalAccessException
    {
        MacroBlock block = new MacroBlock("velocity", Map.of(), "content", true);

        VelocityTemplate template1 = mock();
        when(this.velocityManager.compile(eq("Unknown velocity MacroBlock"), any())).thenReturn(template1);
        VelocityTemplate template2 = mock();
        when(this.velocityManager.compile(eq("reference"), any())).thenReturn(template2);

        assertNull(block.getAttribute(VelocityMacro.MACRO_ATTRIBUTE));

        this.macro.prepare(block);

        assertSame(template1, getVelocityTemplate(block));

        MetaDataBlock metadataBlock = new MetaDataBlock(List.of(block));
        metadataBlock.getMetaData().addMetaData(MetaData.SOURCE, "reference");

        this.macro.prepare(block);

        assertSame(template2, getVelocityTemplate(block));
    }
}
