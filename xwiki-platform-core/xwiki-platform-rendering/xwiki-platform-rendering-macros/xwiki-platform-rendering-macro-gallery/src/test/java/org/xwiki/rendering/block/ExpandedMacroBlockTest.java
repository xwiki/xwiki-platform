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
package org.xwiki.rendering.block;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link ExpandedMacroBlock}.
 * 
 * @since 3.1
 * @version $Id$
 */
public class ExpandedMacroBlockTest extends AbstractComponentTestCase
{
    /**
     * Tests that the child blocks of an {@link ExpandedMacroBlock} are rendered.
     */
    @Test
    public void testRenderExpandedMacroBlock() throws Exception
    {
        BlockRenderer renderer = getComponentManager().getInstance(BlockRenderer.class, "xwiki/2.0");

        Map<String, String> parameters = Collections.emptyMap();
        ExpandedMacroBlock gallery = new ExpandedMacroBlock("gallery", parameters, renderer, false);
        ResourceReference alice = new ResourceReference("alice.png", ResourceType.ATTACHMENT);
        alice.setTyped(false);
        gallery.addChild(new ParagraphBlock(Collections.<Block> singletonList(new ImageBlock(alice, true))));
        ResourceReference bob = new ResourceReference("bob.png", ResourceType.ATTACHMENT);
        bob.setTyped(false);
        gallery.addChild(new ParagraphBlock(Collections.<Block> singletonList(new ImageBlock(bob, true))));

        ParagraphBlock before = new ParagraphBlock(Collections.<Block> singletonList(new WordBlock("before")));
        ParagraphBlock after = new ParagraphBlock(Collections.<Block> singletonList(new WordBlock("after")));

        XDOM xdom = new XDOM(Arrays.<Block> asList(before, gallery, after));
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(xdom, printer);
        Assert.assertEquals("before\n\n{{gallery}}\nimage:alice.png\n\nimage:bob.png\n{{/gallery}}\n\nafter",
            printer.toString());

        // Modify and render again.
        ((ImageBlock) gallery.getChildren().get(1).getChildren().get(0)).setParameter("width", "300");
        printer = new DefaultWikiPrinter();
        renderer.render(xdom, printer);
        Assert.assertEquals(
            "before\n\n{{gallery}}\nimage:alice.png\n\n[[image:bob.png||width=\"300\"]]\n{{/gallery}}\n\nafter",
            printer.toString());
    }

    /**
     * Renders an {@link ExpandedMacroBlock} that has a XDOM block as single child.
     */
    @Test
    public void testRenderExpandedMacroBlockWithXDOM() throws Exception
    {
        BlockRenderer renderer = getComponentManager().getInstance(BlockRenderer.class, "xwiki/2.0");

        XDOM content = new XDOM(Arrays.<Block> asList(new WordBlock("1"), new SpaceBlock(), new WordBlock("2")));

        Map<String, String> parameters = Collections.emptyMap();
        ExpandedMacroBlock macro = new ExpandedMacroBlock("macro", parameters, renderer, false);
        macro.addChild(content);

        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(new XDOM(Collections.<Block> singletonList(macro)), printer);
        Assert.assertEquals("{{macro}}\n1 2\n{{/macro}}", printer.toString());
    }

    /**
     * Tries to render an empty {@link ExpandedMacroBlock}.
     */
    @Test
    public void testRenderEmptyExpandedMacroBlock() throws Exception
    {
        BlockRenderer renderer = getComponentManager().getInstance(BlockRenderer.class, "xwiki/2.0");
        Map<String, String> parameters = Collections.emptyMap();
        ExpandedMacroBlock macro = new ExpandedMacroBlock("empty", parameters, renderer, false);
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(new XDOM(Collections.<Block> singletonList(macro)), printer);
        Assert.assertEquals("{{empty}}{{/empty}}", printer.toString());
    }
}
