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

import org.xwiki.rendering.renderer.XHTMLRenderer;
import org.xwiki.rendering.renderer.WysiwygEditorXHTMLRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererType;
import org.xwiki.rendering.renderer.WikiPrinter;
import org.xwiki.rendering.renderer.XWikiSyntaxRenderer;
import org.xwiki.rendering.renderer.EventsRenderer;
import org.xwiki.rendering.renderer.TexRenderer;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.bridge.DocumentAccessBridge;

/**
 * Easily create Print renderers instances.
 *
 * @version $Id: $
 * @since 1.6M2
 * @see PrintRendererFactory
 */
public class DefaultPrintRendererFactory implements PrintRendererFactory
{
    private DocumentAccessBridge documentAccessBridge;

    private RenderingConfiguration renderingConfiguration;

    public PrintRenderer createRenderer(PrintRendererType type, WikiPrinter printer)
    {
        PrintRenderer result;

        switch (type) {
            case XHTML:
                result = new XHTMLRenderer(printer, this.documentAccessBridge, this.renderingConfiguration);
                break;
            case XWIKISYNTAX:
                result = new XWikiSyntaxRenderer(printer);
                break;
            case WYSIWYG:
                result = new WysiwygEditorXHTMLRenderer(printer, this.documentAccessBridge,
                    this.renderingConfiguration);
                break;
            case EVENTS:
                result = new EventsRenderer(printer);
                break;
            case TEX:
                result = new TexRenderer(printer);
                break;
            default:
                throw new RuntimeException("Invalid Renderer type [" + type + "]");
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
}
