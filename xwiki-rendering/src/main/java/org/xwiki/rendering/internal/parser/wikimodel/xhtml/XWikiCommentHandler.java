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

import org.wikimodel.wem.WikiParameter;
import org.wikimodel.wem.WikiParameters;
import org.wikimodel.wem.WikiReference;
import org.wikimodel.wem.WikiReferenceParser;
import org.wikimodel.wem.xhtml.handler.CommentHandler;
import org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack;
import org.wikimodel.wem.xwiki.XWikiReferenceParser;
import org.xwiki.rendering.internal.parser.wikimodel.XDOMGeneratorListener;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.parser.ImageParser;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.PrintRendererType;
import org.xwiki.rendering.renderer.XHTMLRenderer;

/**
 * Handle Link and Macro definitions in comments (we store links in a comment since otherwise there are situations
 * where it's not possible to reconstruct the original reference from the rendered HTML value and for macros it
 * wouldn't be possible at all to reconstruct the macro).
 *  
 * @version $Id$
 * @since 1.7M1
 */
public class XWikiCommentHandler extends CommentHandler
{
    private Parser parser;
    
    private LinkParser linkParser;
    
    private ImageParser imageParser;
    
    private PrintRendererFactory printRendererFactory;
    
    private WikiReferenceParser referenceParser;

    private String commentContent;
    
    public XWikiCommentHandler(Parser parser, LinkParser linkParser, ImageParser imageParser, PrintRendererFactory printRendererFactory)
    {
        this.parser = parser;
        this.linkParser = linkParser;
        this.printRendererFactory = printRendererFactory;
        this.referenceParser = new XWikiReferenceParser();
        this.imageParser = imageParser;
    }
    
    @Override
    public void onComment(String content, TagStack stack)
    {
        // If the comment starts with "startwikilink" then we need to gather all XHTML tags inside
        // the A tag, till we get a "stopwikilink" comment.
        // Same for "startimage" and "stopimage".
        if (content.startsWith("startwikilink:")) {
            handleLinkCommentStart(content, stack);
        } else if (content.startsWith("stopwikilink")) {
            handleLinkCommentStop(content, stack);
        } else if (content.startsWith("startimage:")) {
            handleImageCommentStart(content, stack);
        } else if (content.startsWith("stopimage")) {
            handleImageCommentStop(content, stack);
        } else {
            super.onComment(content, stack);
        }
    }
    
    private void handleLinkCommentStart(String content, TagStack stack)
    {
        XDOMGeneratorListener listener = new XDOMGeneratorListener(this.parser, this.linkParser, this.imageParser);
        stack.setStackParameter("xdomGeneratorListener", listener);
        stack.setStackParameter("isInLink", true);
        this.commentContent = content.substring("startwikilink:".length());
    }

    private void handleLinkCommentStop(String content, TagStack stack)
    {
        DefaultWikiPrinter printer = new DefaultWikiPrinter();            
        XHTMLRenderer renderer = 
            (XHTMLRenderer) this.printRendererFactory.createRenderer(PrintRendererType.XHTML, printer);
        XDOMGeneratorListener listener = (XDOMGeneratorListener) stack.getStackParameter("xdomGeneratorListener");
        listener.getXDOM().traverse(renderer);

        boolean isFreeStandingLink = (Boolean) stack.getStackParameter("isFreeStandingLink");
        WikiParameters params = (WikiParameters) stack.getStackParameter("linkParameters");
        if (isFreeStandingLink) {
            stack.getScannerContext().onReference(this.commentContent);
        } else {
            WikiReference wikiReference = this.referenceParser.parse(
                (printer.toString().length() > 0 ? printer.toString() + ">>" : "") + this.commentContent
                + (params.getSize() > 0 ? "||" + params.toString() : ""));
            stack.getScannerContext().onReference(wikiReference);
        }
        
        stack.setStackParameter("xdomGeneratorListener", null);
        stack.setStackParameter("isInLink", false);
        stack.setStackParameter("isFreeStandingLink", false);
        stack.setStackParameter("linkParameters", WikiParameters.EMPTY);
        this.commentContent = null;
    }

    private void handleImageCommentStart(String content, TagStack stack)
    {
        stack.setStackParameter("isInImage", true);
        this.commentContent = content.substring("startimage:".length());  
    }
    
    private void handleImageCommentStop(String content, TagStack stack)
    {
        boolean isFreeStandingImage = (Boolean) stack.getStackParameter("isFreeStandingImage");
        WikiParameters params = (WikiParameters) stack.getStackParameter("imageParameters");
        Image image = this.imageParser.parse(this.commentContent);
        
        if (isFreeStandingImage) {
            stack.getScannerContext().onReference("image:" + this.commentContent);
        } else {
            // Remove the ALT attribute if the content has the same value as the original image location
            // This is because the XHTML renderer automatically adds an ALT attribute since it is mandatory
            // in the XHTML specifications.
            WikiParameter altParameter = params.getParameter("alt"); 
            if (altParameter != null && altParameter.getValue().equals(image.getAttachmentName())) {
                params = params.remove("alt");
            }
            
            WikiReference wikiReference = this.referenceParser.parse("image:" + this.commentContent
                + (params.getSize() > 0 ? "||" + params.toString() : ""));
            stack.getScannerContext().onReference(wikiReference);
        }
        
        stack.setStackParameter("isInImage", false);
        stack.setStackParameter("isFreeStandingImage", false);
        stack.setStackParameter("imageParameters", WikiParameters.EMPTY);
        this.commentContent = null;        
    }
}
