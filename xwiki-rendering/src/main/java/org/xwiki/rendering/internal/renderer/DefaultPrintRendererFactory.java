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
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.parser.AttachmentParser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.renderer.EventsRenderer;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.TexRenderer;
import org.xwiki.rendering.renderer.XHTMLRenderer;
import org.xwiki.rendering.renderer.XWikiSyntaxRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Easily create Print Renderers instances.
 * 
 * @version $Id: $
 * @since 1.6M2
 * @see PrintRendererFactory
 */
public class DefaultPrintRendererFactory implements PrintRendererFactory
{
    private DocumentAccessBridge documentAccessBridge;

    private RenderingConfiguration renderingConfiguration;

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
                new XHTMLRenderer(printer, this.documentAccessBridge, this.renderingConfiguration,
                    this.attachmentParser);
        } else if (targetSyntax.toIdString().equals("xwiki/2.0")) {
            result = new XWikiSyntaxRenderer(printer);
        } else if (targetSyntax.toIdString().equals("event/1.0")) {
            result = new EventsRenderer(printer);
        } else if (targetSyntax.toIdString().equals("tex/1.0")) {
            result = new TexRenderer(printer);
        } else {
            throw new RuntimeException("No renderer found for target syntax [" + targetSyntax + "]");
        }

        return result;
    }

    public void setDocumentAccessBridge(DocumentAccessBridge bridge)
    {
        this.documentAccessBridge = bridge;
    }

    public void setRenderingConfiguration(RenderingConfiguration renderingConfiguration)
    {
        this.renderingConfiguration = renderingConfiguration;
    }

    /**
     * @since 1.7.1
     */
    public void setAttachmentParser(AttachmentParser attachmentParser)
    {
        this.attachmentParser = attachmentParser;
    }
}
