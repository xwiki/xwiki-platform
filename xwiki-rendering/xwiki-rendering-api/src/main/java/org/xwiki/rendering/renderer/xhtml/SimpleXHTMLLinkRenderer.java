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
package org.xwiki.rendering.renderer.xhtml;

import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;

/**
 * Simple implementation that considers all links external links and thus that adds the link reference as the 
 * XHTML HREF value.
 * 
 * @version $Id: $
 * @since 1.8RC3
 */
public class SimpleXHTMLLinkRenderer implements XHTMLLinkRenderer
{
    /**
     * @see #setXHTMLWikiPrinter(XHTMLWikiPrinter)
     */
    private XHTMLWikiPrinter xhtmlPrinter;

    /**
     * {@inheritDoc}
     * @see XHTMLLinkRenderer#setHasLabel(boolean)
     */
    public void setHasLabel(boolean hasLabel)
    {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     * @see XHTMLLinkRenderer#setWikiPrinter(XHTMLWikiPrinter)
     */
    public void setXHTMLWikiPrinter(XHTMLWikiPrinter printer)
    {
        this.xhtmlPrinter = printer;
    }

    /**
     * {@inheritDoc}
     * @see XHTMLLinkRenderer#beginLink(Link, boolean, Map)
     */
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Only handle external links since this is a simple generic XHTML link renderer.
        if (link.isExternalLink()) {
            // Add all parameters to the A attributes
            Map<String, String> aAttributes = new LinkedHashMap<String, String>();
            aAttributes.putAll(parameters);
            aAttributes.put("href", link.getReference());
            this.xhtmlPrinter.printXMLStartElement("a", aAttributes);
        }
    }

    /**
     * {@inheritDoc}
     * @see XHTMLLinkRenderer#endLink(Link, boolean, Map)
     */
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.xhtmlPrinter.printXMLEndElement("a");
    }
}
