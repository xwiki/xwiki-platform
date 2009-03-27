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
package org.xwiki.rendering.internal.renderer;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.parser.AttachmentParser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.renderer.EventsRenderer;
import org.xwiki.rendering.renderer.LinkLabelGenerator;
import org.xwiki.rendering.renderer.PlainTextRenderer;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.TexRenderer;
import org.xwiki.rendering.renderer.XHTMLRenderer;
import org.xwiki.rendering.renderer.XWikiSyntaxRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.xhtml.XWikiXHTMLImageRenderer;
import org.xwiki.rendering.renderer.xhtml.XWikiXHTMLLinkRenderer;

/**
 * Easily create Print Renderers instances.
 * 
 * @version $Id: $
 * @since 1.6M2
 * @see PrintRendererFactory
 */
@Component
public class DefaultPrintRendererFactory implements PrintRendererFactory
{
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    @Requirement
    private LinkLabelGenerator linkLabelGenerator;

    @Requirement
    private AttachmentParser attachmentParser;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRendererFactory#createRenderer(org.xwiki.rendering.parser.Syntax,
     *      org.xwiki.rendering.renderer.printer.WikiPrinter)
     */
    public PrintRenderer createRenderer(Syntax targetSyntax, WikiPrinter printer)
    {
        PrintRenderer result;

        if (targetSyntax.toIdString().equals("xhtml/1.0")) {
            result =
                new XHTMLRenderer(printer, new XWikiXHTMLLinkRenderer(this.documentAccessBridge,
                    this.linkLabelGenerator, this.attachmentParser), new XWikiXHTMLImageRenderer(
                    this.documentAccessBridge));
        } else if (targetSyntax.toIdString().equals("xwiki/2.0")) {
            result = new XWikiSyntaxRenderer(printer);
        } else if (targetSyntax.toIdString().equals("event/1.0")) {
            result = new EventsRenderer(printer);
        } else if (targetSyntax.toIdString().equals("tex/1.0")) {
            result = new TexRenderer(printer);
        } else if (targetSyntax.toIdString().equals("plain/1.0")) {
            result = new PlainTextRenderer(printer, linkLabelGenerator);
        } else {
            throw new RuntimeException("No renderer found for target syntax [" + targetSyntax + "]");
        }

        return result;
    }

    public void setDocumentAccessBridge(DocumentAccessBridge bridge)
    {
        this.documentAccessBridge = bridge;
    }

    public void setLinkLabelGenerator(LinkLabelGenerator linkLabelGenerator)
    {
        this.linkLabelGenerator = linkLabelGenerator;
    }

    /**
     * @since 1.7.1
     */
    public void setAttachmentParser(AttachmentParser attachmentParser)
    {
        this.attachmentParser = attachmentParser;
    }
}
