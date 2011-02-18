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
package org.xwiki.rendering.internal.transformation.icon;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.icon.IconTransformationConfiguration;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.transformation.icon.IconTransformation}.
 *
 * @version $Id$
 * @since 2.6RC1
 */
public class IconTransformationTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement(exceptions = {Parser.class, IconTransformationConfiguration.class})
    private IconTransformation transformation;

    @Override
    public void configure() throws Exception
    {
        super.configure();
        final ConfigurationSource cs = getMockery().mock(ConfigurationSource.class);
        DefaultComponentDescriptor<ConfigurationSource> cd = new DefaultComponentDescriptor<ConfigurationSource>();
        cd.setRole(ConfigurationSource.class);
        getComponentManager().registerComponent(cd, cs);

        getMockery().checking(new Expectations(){{
            oneOf(cs).getProperty("rendering.transformation.icon.mappings", Properties.class);
            will(returnValue(new Properties()));
        }});
    }

    @Test
    public void testTransform() throws Exception
    {
        String expected = "beginDocument [[syntax]=[XWiki 2.1 (experimental)]]\n"
            + "beginParagraph\n"
            + "onWord [Some]\n"
            + "onSpace\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [emoticon_smile]] [true]\n"
            + "onSpace\n"
            + "onWord [smileys]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [emoticon_unhappy]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [emoticon_tongue]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [emoticon_grin]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [emoticon_wink]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [thumb_up]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [thumb_down]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [information]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [accept]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [cancel]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [error]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [add]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [delete]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [help]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [lightbulb]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [lightbulb_off]] [true]\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [star]] [true]\n"
            + "endParagraph\n"
            + "endDocument [[syntax]=[XWiki 2.1 (experimental)]]";

        XDOM xdom = getComponentManager().lookup(Parser.class, "xwiki/2.1").parse(new StringReader(
           "Some :) smileys:(:P:D;)(y)(n)(i)(/)(x)(!)(+)(-)(?)(on)(off)(*)"));
        this.transformation.transform(xdom, new TransformationContext());

        WikiPrinter printer = new DefaultWikiPrinter();
        getComponentManager().lookup(BlockRenderer.class, "event/1.0").render(xdom, printer);
        Assert.assertEquals(expected, printer.toString());
    }

    @Test
    public void testTransformIgnoresProtectedContent() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMacroMarkerStandalone [code] []\n"
            + "onSpecialSymbol [:]\n"
            + "onSpecialSymbol [)]\n"
            + "endMacroMarkerStandalone [code] []\n"
            + "endDocument";

        XDOM xdom = new XDOM(Arrays.asList((Block) new MacroMarkerBlock("code", Collections.<String, String>emptyMap(),
            Arrays.asList((Block) new SpecialSymbolBlock(':'), new SpecialSymbolBlock(')')), false)));
        this.transformation.transform(xdom, new TransformationContext());

        WikiPrinter printer = new DefaultWikiPrinter();
        getComponentManager().lookup(BlockRenderer.class, "event/1.0").render(xdom, printer);
        Assert.assertEquals(expected, printer.toString());
    }

    /**
     * Fixes XWIKI-5729.
     */
    @Test
    public void testTransformWhenIncompleteMatchExistsFollowedByMatch() throws Exception
    {
        String expected = "beginDocument [[syntax]=[XWiki 2.1 (experimental)]]\n"
            + "beginParagraph\n"
            + "onSpecialSymbol [(]\n"
            + "onSpace\n"
            + "onImage [Typed = [true] Type = [icon] Reference = [information]] [true]\n"
            + "endParagraph\n"
            + "endDocument [[syntax]=[XWiki 2.1 (experimental)]]";

        XDOM xdom = getComponentManager().lookup(Parser.class, "xwiki/2.1").parse(new StringReader(
           "( (i)"));
        this.transformation.transform(xdom, new TransformationContext());

        WikiPrinter printer = new DefaultWikiPrinter();
        getComponentManager().lookup(BlockRenderer.class, "event/1.0").render(xdom, printer);
        Assert.assertEquals(expected, printer.toString());
    }
}
