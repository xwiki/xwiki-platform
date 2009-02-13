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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.internal.renderer.chaining.XHTMLChainingRenderer;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.DocumentStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.parser.AttachmentParser;
import org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

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
     * @param documentAccessBridge see {@link #documentAccessBridge}
     * @param configuration the rendering configuration
     * @param attachmentParser the parser to be used to parse attachment locations specified as strings
     */
    public XHTMLRenderer(WikiPrinter printer, DocumentAccessBridge documentAccessBridge,
        RenderingConfiguration configuration, AttachmentParser attachmentParser)
    {
        super(printer, new ListenerChain());

        new DocumentStateChainingListener(getListenerChain());
        new BlockStateChainingListener(getListenerChain());
        new XHTMLChainingRenderer(printer, documentAccessBridge, configuration, attachmentParser, getListenerChain());
    }
}
