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
package org.xwiki.rendering.renderer;

import java.util.Arrays;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.listener.URLImage;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import junit.framework.TestCase;

/**
 * Verify that it's possible to use the XHTML Renderer as a POJO using the Simple XHTML Link and Image
 * Renderers.
 * 
 * @version $Id: $
 * @since 1.8RC3
 */
public class XHTMLRendererTest extends TestCase
{
    public void testSimpleLinkAndImageRenderersUsage()
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        XHTMLRenderer renderer = new XHTMLRenderer(printer);
        
        Link link = new Link();
        link.setReference("http://xwiki.org");
        link.setType(LinkType.URI);
        URLImage image = new URLImage("http://www.xwiki.org/logo.png");
        ImageBlock imageBlock = new ImageBlock(image, true);
        LinkBlock linkBlock = new LinkBlock(Arrays.asList((Block) imageBlock), link, false); 
        XDOM xdom = new XDOM(Arrays.asList((Block) linkBlock));

        xdom.traverse(renderer);
        
        assertEquals("<a href=\"http://xwiki.org\"><img src=\"http://www.xwiki.org/logo.png\" "
            + "alt=\"http://www.xwiki.org/logo.png\"/></a>", printer.toString());
    }
}
