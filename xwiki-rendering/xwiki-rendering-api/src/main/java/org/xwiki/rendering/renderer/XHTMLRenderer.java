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

import org.xwiki.rendering.internal.renderer.chaining.XHTMLChainingRenderer;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.DocumentStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.xhtml.SimpleXHTMLImageRenderer;
import org.xwiki.rendering.renderer.xhtml.SimpleXHTMLLinkRenderer;
import org.xwiki.rendering.renderer.xhtml.XHTMLImageRenderer;
import org.xwiki.rendering.renderer.xhtml.XHTMLLinkRenderer;

/**
 * Generates XHTML from a {@link org.xwiki.rendering.block.XDOM} object being traversed.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XHTMLRenderer extends AbstractChainingPrintRenderer
{
    /**
     * @param printer the object to which to write the XHTML output to
     * @param linkRenderer the object to render link events into XHTML. This is done so that it's pluggable because
     *        link rendering depends on how the underlying system wants to handle it. For example for XWiki we
     *        check if the document exists, we get the document URL, etc.
     * @param imageRenderer the object to render image events into XHTML. This is done so that it's pluggable
     *        because image rendering depends on how the underlying system wants to handle it. For example for XWiki
     *        we check if the image exists as a document attachments, we get its URL, etc.
     */
    public XHTMLRenderer(WikiPrinter printer, XHTMLLinkRenderer linkRenderer, XHTMLImageRenderer imageRenderer)
    {
        super(printer, new ListenerChain());

        new DocumentStateChainingListener(getListenerChain());
        new BlockStateChainingListener(getListenerChain());
        new XHTMLChainingRenderer(printer, linkRenderer, imageRenderer, getListenerChain());
    }

    /**
     * Convenience constructor that uses default simple implementations for Link and Image Renderers. This is useful
     * when using the XWiki Rendering system without the notion of XWiki Documents (for example when using it outside
     * of XWiki).
     * 
     * @param printer the object to which to write the XHTML output to
     */
    public XHTMLRenderer(WikiPrinter printer)
    {
        this(printer, new SimpleXHTMLLinkRenderer(), new SimpleXHTMLImageRenderer());
    }
}
