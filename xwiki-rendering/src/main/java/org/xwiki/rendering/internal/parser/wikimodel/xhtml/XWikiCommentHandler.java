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
package org.xwiki.rendering.internal.parser.wikimodel.xhtml;

import org.wikimodel.wem.WikiReference;
import org.wikimodel.wem.WikiReferenceParser;
import org.wikimodel.wem.xhtml.handler.CommentHandler;
import org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack;
import org.wikimodel.wem.xwiki.XWikiReferenceParser;
import org.xwiki.rendering.internal.parser.wikimodel.XDOMGeneratorListener;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.PrintRendererType;
import org.xwiki.rendering.renderer.XHTMLRenderer;
import org.xwiki.url.XWikiURLFactory;

/**
 * Handle Link definition in comments (we store the link reference in a comment since otherwise there are situations
 * where it's not possible to reconstruct the original reference from the rendered HTML value).
 *  
 * @version $Id$
 * @since 1.7M1
 */
public class XWikiCommentHandler extends CommentHandler
{
    private Parser parser;
    
    private LinkParser linkParser;
    
    private XWikiURLFactory urlFactory;
    
    private PrintRendererFactory printRendererFactory;
    
    private WikiReferenceParser referenceParser;

    private String reference;
    
    public XWikiCommentHandler(Parser parser, LinkParser linkParser, XWikiURLFactory urlFactory, PrintRendererFactory printRendererFactory)
    {
        this.parser = parser;
        this.linkParser = linkParser;
        this.urlFactory = urlFactory;
        this.printRendererFactory = printRendererFactory;
        this.referenceParser = new XWikiReferenceParser();
    }
    
    @Override
    public void onComment(String content, TagStack stack)
    {
        // If the comment starts with "startwikilink" then we need to gather all XHTML tags inside
        // the A tag, till we get a "stopwikilink" comment.
        if (content.startsWith("startwikilink:")) {
            XDOMGeneratorListener listener = new XDOMGeneratorListener(this.parser, this.linkParser, this.urlFactory);
            stack.setStackParameter("xdomGeneratorListener", listener);
            stack.setStackParameter("isInLink", true);
            this.reference = content.substring("startwikilink:".length());
        } else if (content.startsWith("stopwikilink")) {
            DefaultWikiPrinter printer = new DefaultWikiPrinter();            
            XHTMLRenderer renderer = 
                (XHTMLRenderer) this.printRendererFactory.createRenderer(PrintRendererType.XHTML, printer);
            XDOMGeneratorListener listener = (XDOMGeneratorListener) stack.getStackParameter("xdomGeneratorListener");
            listener.getDocument().traverse(renderer);

            boolean isFreeStandingLink = (Boolean) stack.getStackParameter("isFreeStandingLink");
            if (isFreeStandingLink) {
                stack.getScannerContext().onReference(this.reference);
            } else {
                WikiReference reference = this.referenceParser.parse(
                    (printer.toString().length() > 0 ? printer.toString() + ">>" : "") + this.reference);
                stack.getScannerContext().onReference(reference);
            }
            
            stack.setStackParameter("xdomGeneratorListener", null);
            stack.setStackParameter("isInLink", false);
            stack.setStackParameter("isFreeStandingLink", false);
            this.reference = null;
        }
    }
}
