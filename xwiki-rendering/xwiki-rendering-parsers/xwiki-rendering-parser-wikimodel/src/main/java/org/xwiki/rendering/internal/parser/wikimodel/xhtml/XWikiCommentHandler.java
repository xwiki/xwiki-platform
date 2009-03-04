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

import java.util.Stack;

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
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.XWikiSyntaxRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;

/**
 * Handle Link and Macro definitions in comments (we store links in a comment since otherwise there are situations where
 * it's not possible to reconstruct the original reference from the rendered HTML value and for macros it wouldn't be
 * possible at all to reconstruct the macro).
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

    /**
     * We're using a stack so that we can have nested comment handling. For example when we have a link to an image we
     * need nested comment support.
     */
    private Stack<String> commentContentStack = new Stack<String>();

    public XWikiCommentHandler(Parser parser, LinkParser linkParser, ImageParser imageParser,
        PrintRendererFactory printRendererFactory)
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
        // if ignoreElements is true it means we are inside a macro or another block we don't want to parse content
        boolean ignoreElements = (Boolean) stack.getStackParameter("ignoreElements");

        // If the comment starts with "startwikilink" then we need to gather all XHTML tags inside
        // the A tag, till we get a "stopwikilink" comment.
        // Same for "startimage" and "stopimage".
        if (!ignoreElements && content.startsWith("startwikilink:")) {
            handleLinkCommentStart(content, stack);
        } else if (!ignoreElements && content.startsWith("stopwikilink")) {
            handleLinkCommentStop(content, stack);
        } else if (!ignoreElements && content.startsWith("startimage:")) {
            handleImageCommentStart(content, stack);
        } else if (!ignoreElements && content.startsWith("stopimage")) {
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
        this.commentContentStack.push(content.substring("startwikilink:".length()));
    }

    private void handleLinkCommentStop(String content, TagStack stack)
    {
        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        // Since wikimodel does not support wiki syntax in link labels we need to pass the link label "as is" (as it
        // originally appears in the parsed source) and handle it specially in the
        // XDOMGeneratorListener.createLinkBlock(), with the parser passed as the first parameter in the
        // XDOMGeneratorListener constructor.
        // Since we cannot get this label as it originally appeared in the HTML source ( we are doing a SAX-like
        // parsing), we should render the XDOM as HTML to get an HTML label.
        // Since any syntax would do it, as long as this renderer matches the corresponding XDOMGeneratorListener
        // parser, we use an xwiki 2.0 renderer for it is less complex (no context needed to render xwiki 2.0, no url
        // resolution needed, no reference validity tests).
        // see XDOMGeneratorListener#XDOMGeneratorListener(Parser, LinkParser, ImageParser)
        // see WikiModelXHTMLParser#getLinkLabelParser()
        // see http://code.google.com/p/wikimodel/issues/detail?id=87
        // TODO: remove this workaround when wiki syntax in link labels will be supported by wikimodel
        XWikiSyntaxRenderer renderer =
            (XWikiSyntaxRenderer) this.printRendererFactory
                .createRenderer(new Syntax(SyntaxType.XWIKI, "2.0"), printer);
        XDOMGeneratorListener listener = (XDOMGeneratorListener) stack.getStackParameter("xdomGeneratorListener");
        listener.getXDOM().traverse(renderer);

        boolean isFreeStandingLink = (Boolean) stack.getStackParameter("isFreeStandingLink");
        WikiParameters params = (WikiParameters) stack.getStackParameter("linkParameters");
        String linkComment = this.commentContentStack.pop();
        if (isFreeStandingLink) {
            stack.getScannerContext().onReference(linkComment);
        } else {
            WikiReference wikiReference =
                this.referenceParser.parse((printer.toString().length() > 0 ? printer.toString() + ">>" : "")
                    + linkComment + (params.getSize() > 0 ? "||" + params.toString() : ""));
            stack.getScannerContext().onReference(wikiReference);
        }

        stack.setStackParameter("xdomGeneratorListener", null);
        stack.setStackParameter("isInLink", false);
        stack.setStackParameter("isFreeStandingLink", false);
        stack.setStackParameter("linkParameters", WikiParameters.EMPTY);
    }

    private void handleImageCommentStart(String content, TagStack stack)
    {
        stack.setStackParameter("isInImage", true);
        this.commentContentStack.push(content.substring("startimage:".length()));
    }

    private void handleImageCommentStop(String content, TagStack stack)
    {
        boolean isFreeStandingImage = (Boolean) stack.getStackParameter("isFreeStandingImage");
        WikiParameters parameters = (WikiParameters) stack.getStackParameter("imageParameters");
        String imageComment = this.commentContentStack.pop();
        Image image = this.imageParser.parse(imageComment);

        if (isFreeStandingImage) {

            stack.getScannerContext().onImage(imageComment);
        } else {
            // Remove the ALT attribute if the content has the same value as the original image location
            // This is because the XHTML renderer automatically adds an ALT attribute since it is mandatory
            // in the XHTML specifications.
            WikiParameter alt = parameters.getParameter("alt");
            if (alt != null && alt.getValue().equals(image.getName())) {
                parameters = parameters.remove("alt");
                alt = null;
            }

            WikiReference reference = new WikiReference(imageComment, null, parameters);

            stack.getScannerContext().onImage(reference);
        }

        stack.setStackParameter("isInImage", false);
        stack.setStackParameter("isFreeStandingImage", false);
        stack.setStackParameter("imageParameters", WikiParameters.EMPTY);
    }
}
