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
package org.xwiki.rendering.internal.renderer.xhtml.link;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.listener.LinkListener;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;

/**
 * Renders a type of link (mailto link, document link, URL, etc) in XHTML. Components implementing this
 * interface implement the rendering logic only for a single link type and must have a role hint value
 * equal to the {@link org.xwiki.rendering.listener.reference.ResourceType} name (eg "doc" for document link, "attach"
 * for attachment link, etc).
 *
 * Implementations must handle both cases when rendering a link:
 * <ul>
 *   <li>when inside a wiki (ie when an implementation of {@link org.xwiki.rendering.wiki.WikiModel} is provided.</li>
 *   <li>when outside of a wiki. In this case links to attachmets or documents are ignored and rendered as is as
 *       direct HREF values. In other words only external links are meaningful.</li>
 * </ul>
 *
 * @version $Id$
 * @since 2.5M2
 */
@ComponentRole
public interface XHTMLLinkTypeRenderer extends LinkListener
{
    /**
     * @param printer the XHTML printer to use to output links as XHTML
     */
    void setXHTMLWikiPrinter(XHTMLWikiPrinter printer);

    /**
     * @return the XHTML printer to use to output links as XHTML
     */
    XHTMLWikiPrinter getXHTMLWikiPrinter();

    /**
     * @param hasLabel true if the link to be rendered has a label specified or false otherwise. If no label has been
     *            specified then it's up to the XHTML renderer implementation to generate a default label.
     */
    void setHasLabel(boolean hasLabel);
}
