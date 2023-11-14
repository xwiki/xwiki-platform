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
package org.xwiki.rendering.internal.macro.cache;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.macro.MacroTransformation;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroPreparationException;
import org.xwiki.rendering.macro.cache.CacheMacroParameters;
import org.xwiki.rendering.macro.script.ScriptMockSetup;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.test.jmock.AbstractComponentTestCase;
import org.xwiki.velocity.VelocityManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.xwiki.rendering.test.BlockAssert.assertBlocks;

/**
 * Unit tests for {@link CacheMacro}.
 *
 * @version $Id$
 * @since 3.0M1
 */
public class CacheMacroTest extends AbstractComponentTestCase
{
    private ScriptMockSetup mockSetup;

    private CacheMacro cacheMacro;

    private PrintRendererFactory rendererFactory;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        this.mockSetup = new ScriptMockSetup(getMockery(), getComponentManager());
        this.cacheMacro = getComponentManager().getInstance(Macro.class, "cache");
        this.rendererFactory = getComponentManager().getInstance(PrintRendererFactory.class, "event/1.0");
    }

    @Test
    public void executeWhenNoIdAndSameContent() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMacroMarkerStandalone [velocity] [] [$var]\n"
            + "beginParagraph\n"
            + "onWord [content]\n"
            + "endParagraph\n"
            + "endMacroMarkerStandalone [velocity] [] [$var]\n"
            + "endDocument";

        CacheMacroParameters params = new CacheMacroParameters();
        MacroTransformationContext context = createMacroTransformationContext();

        VelocityManager velocityManager = getComponentManager().getInstance(VelocityManager.class);
        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer, "template",
            "#set ($var = 'content')");

        List<Block> result = this.cacheMacro.execute(params, "{{velocity}}$var{{/velocity}}", context);
        assertBlocks(expected, result, this.rendererFactory);

        // Execute a second time with a different value for the velocity $var variable to ensure the returned result
        // is the cached one.
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer, "template",
            "#set ($var = 'newcontent')");
        result = this.cacheMacro.execute(params, "{{velocity}}$var{{/velocity}}", context);
        assertBlocks(expected, result, this.rendererFactory);
    }

    @Test
    public void executeWhenNoIdAndDifferentContent() throws Exception
    {
        String expected1 = "beginDocument\n"
            + "beginMacroMarkerStandalone [velocity] [] [$var]\n"
            + "beginParagraph\n"
            + "onWord [content]\n"
            + "endParagraph\n"
            + "endMacroMarkerStandalone [velocity] [] [$var]\n"
            + "endDocument";

        String expected2 = "beginDocument\n"
            + "beginMacroMarkerStandalone [velocity] [] [$var##]\n"
            + "beginParagraph\n"
            + "onWord [newcontent]\n"
            + "endParagraph\n"
            + "endMacroMarkerStandalone [velocity] [] [$var##]\n"
            + "endDocument";

        CacheMacroParameters params = new CacheMacroParameters();
        MacroTransformationContext context = createMacroTransformationContext();

        VelocityManager velocityManager = getComponentManager().getInstance(VelocityManager.class);
        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer, "template",
            "#set ($var = 'content')");

        List<Block> result = this.cacheMacro.execute(params, "{{velocity}}$var{{/velocity}}", context);
        assertBlocks(expected1, result, this.rendererFactory);

        // Execute a second time with a different cache macro content to ensure it's not cached
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer, "template",
            "#set ($var = 'newcontent')");
        result = this.cacheMacro.execute(params, "{{velocity}}$var##{{/velocity}}", context);
        assertBlocks(expected2, result, this.rendererFactory);
    }

    @Test
    public void executeWhenSameIdAndDifferentContent() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMacroMarkerStandalone [velocity] [] [$var]\n"
            + "beginParagraph\n"
            + "onWord [content]\n"
            + "endParagraph\n"
            + "endMacroMarkerStandalone [velocity] [] [$var]\n"
            + "endDocument";

        CacheMacroParameters params = new CacheMacroParameters();
        params.setId("uniqueid");
        MacroTransformationContext context = createMacroTransformationContext();

        VelocityManager velocityManager = getComponentManager().getInstance(VelocityManager.class);
        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer, "template",
            "#set ($var = 'content')");

        List<Block> result = this.cacheMacro.execute(params, "{{velocity}}$var{{/velocity}}", context);
        assertBlocks(expected, result, this.rendererFactory);

        // Execute a second time with a different content but with the same id, to ensure the returned result
        // is the cached one.
        result = this.cacheMacro.execute(params, "whatever here...", context);
        assertBlocks(expected, result, this.rendererFactory);
    }

    @Test
    public void executeWithIdGeneratedByVelocityMacro() throws Exception
    {
        VelocityManager velocityManager = getComponentManager().getInstance(VelocityManager.class);
        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer, "template",
            "#set ($var = 'generatedid')");

        CacheMacroParameters params = new CacheMacroParameters();
        params.setId("{{velocity}}$var{{/velocity}}");
        MacroTransformationContext context = createMacroTransformationContext();

        List<Block> result1 = this.cacheMacro.execute(params, "whatever", context);

        // Execute a second time with the same id (specified explicitly this time) and ensures that the returned result
        // is the same even the cache macro content is different.
        params.setId("generatedid");
        List<Block> result2 = this.cacheMacro.execute(params, "something else", context);
        assertEquals(result1, result2);
    }

    @Test
    public void executeWithDifferentTimeToLive() throws Exception
    {
        CacheMacroParameters params = new CacheMacroParameters();
        MacroTransformationContext context = createMacroTransformationContext();

        params.setId("id");
        params.setMaxEntries(10);
        params.setTimeToLive(100);
        List<Block> result1 = this.cacheMacro.execute(params, "content1", context);

        // Execute a second time with different content but with different time to live param. This means another
        // cache will be used and thus the first cached content won't be returned.
        params.setTimeToLive(200);
        List<Block> result2 = this.cacheMacro.execute(params, "content2", context);
        assertFalse(result2.equals(result1));
    }

    @Test
    public void executeWithDifferentMaxEntries() throws Exception
    {
        CacheMacroParameters params = new CacheMacroParameters();
        MacroTransformationContext context = createMacroTransformationContext();

        params.setId("id");
        params.setMaxEntries(10);
        params.setTimeToLive(100);
        List<Block> result1 = this.cacheMacro.execute(params, "content1", context);

        // Execute a second time with different content but with different time to live param. This means another
        // cache will be used and thus the first cached content won't be returned.
        params.setMaxEntries(11);
        List<Block> result2 = this.cacheMacro.execute(params, "content2", context);
        assertFalse(result2.equals(result1));
    }

    private MacroTransformationContext createMacroTransformationContext() throws Exception
    {
        MacroTransformation macroTransformation = getComponentManager().getInstance(Transformation.class, "macro");
        MacroTransformationContext context = new MacroTransformationContext();
        context.setTransformation(macroTransformation);
        context.setSyntax(Syntax.XWIKI_2_0);
        return context;
    }

    @Test
    public void prepare() throws MacroPreparationException
    {
        MacroBlock macroBlock = new MacroBlock("content", Map.of(), "content", false);
        XDOM xdom = new XDOM(List.of(macroBlock));
        xdom.getMetaData().addMetaData(MetaData.SYNTAX, Syntax.PLAIN_1_0);

        this.cacheMacro.prepare(macroBlock);

        assertEquals(new XDOM(List.of(new ParagraphBlock(List.of(new WordBlock("content"))))),
            macroBlock.getAttribute(MacroContentParser.ATTRIBUTE_PREPARE_CONTENT_XDOM));
    }
}
