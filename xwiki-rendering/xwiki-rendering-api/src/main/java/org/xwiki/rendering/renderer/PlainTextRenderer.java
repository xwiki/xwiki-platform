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

import org.xwiki.rendering.internal.renderer.chaining.PlainTextChainingRenderer;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Print only plain text information. For example it remove anything which need a specific syntax a simple plain text
 * editor can't support like the style, link, image, etc. This renderer is mainly used to generate a simple as possible
 * label like in a TOC.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class PlainTextRenderer extends AbstractChainingPrintRenderer
{
    /**
     * @param printer the object where the XWiki Syntax output will be printed to
     * @param linkLabelGenerator the plain text renderer only print link label so it need LinkLabelGenerator to generate
     *            it when there is not explicit one. If null the reference is printed.
     */
    public PlainTextRenderer(WikiPrinter printer, LinkLabelGenerator linkLabelGenerator)
    {
        super(printer, new ListenerChain());

        new BlockStateChainingListener(getListenerChain());
        new PlainTextChainingRenderer(printer, linkLabelGenerator, getListenerChain());
    }
}
