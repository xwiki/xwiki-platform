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
 *
 */
package org.xwiki.rendering.example;

import java.io.StringReader;
import java.util.Collections;

import org.junit.Test;
import org.junit.Assert;

import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.Renderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Examples of using the XWiki Rendering API standalone, using the Embedded Component Manager.
 *  
 * @version $Id$
 * @since 2.0M1
 */
public class ExampleTest
{
    @Test
    public void renderXWiki20SyntaxAsXHTML() throws Exception
    {
        // Initialize Rendering components and allow getting instances
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();
        ecm.initialize(this.getClass().getClassLoader());
        
        // Parse XWiki 2.0 Syntax
        Parser parser = ecm.lookup(Parser.class, Syntax.XWIKI_2_0.toIdString());
        XDOM xdom = parser.parse(new StringReader("This is **bold**"));
        
        // Run macros
        TransformationManager txManager = ecm.lookup(TransformationManager.class);
        txManager.performTransformations(xdom, parser.getSyntax());

        // Generate HTML for example
        WikiPrinter printer = new DefaultWikiPrinter();
        PrintRendererFactory prf = ecm.lookup(PrintRendererFactory.class);
        Renderer htmlRenderer = prf.createRenderer(Syntax.XHTML_1_0, printer);
        
        xdom.traverse(htmlRenderer);

        Assert.assertEquals("<p>This is <strong>bold</strong></p>", printer.toString());
    }
    
    @Test
    public void makeAllLinksItalic() throws Exception
    {
        // Initialize Rendering components and allow getting instances
        EmbeddableComponentManager ecm = new EmbeddableComponentManager(this.getClass().getClassLoader());
        ecm.initialize();
        
        // Parse XWiki 2.0 Syntax
        Parser parser = ecm.lookup(Parser.class, Syntax.XWIKI_2_0.toIdString());
        XDOM xdom = parser.parse(new StringReader("This a [[link>MyPage]]"));
        
        // Find all links and make them italic
        for (LinkBlock block : xdom.getChildrenByType(LinkBlock.class, true)) {
            Block parentBlock = block.getParent();
            Block newBlock = new FormatBlock(Collections.<Block>singletonList(block), Format.ITALIC);
            parentBlock.replaceChild(newBlock, block);
        }
        
        // Generate XWiki 2.0 Syntax for example
        WikiPrinter printer = new DefaultWikiPrinter();
        PrintRendererFactory prf = ecm.lookup(PrintRendererFactory.class);
        Renderer xwikiRenderer = prf.createRenderer(Syntax.XWIKI_2_0, printer);

        xdom.traverse(xwikiRenderer);

        Assert.assertEquals("This a //[[link>MyPage]]//", printer.toString());
    }
}
