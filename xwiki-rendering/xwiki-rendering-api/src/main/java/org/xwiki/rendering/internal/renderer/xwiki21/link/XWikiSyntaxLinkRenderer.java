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
package org.xwiki.rendering.internal.renderer.xwiki21.link;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.internal.renderer.printer.XWikiSyntaxEscapeWikiPrinter;
import org.xwiki.rendering.internal.renderer.xwiki20.XWikiSyntaxListenerChain;
import org.xwiki.rendering.listener.DocumentLink;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.renderer.link.LinkReferenceSerializer;

/**
 * Logic to render a XWiki Link into XWiki Syntax 2.1.
 *
 * @version $Id$
 * @since 2.5M2
 */
public class XWikiSyntaxLinkRenderer extends org.xwiki.rendering.internal.renderer.xwiki20.link.XWikiSyntaxLinkRenderer
{
    /**
     * @param listenerChain the rendering chain
     * @param linkReferenceSerializer the serializer implementation to use to serialize link references
     */
    public XWikiSyntaxLinkRenderer(XWikiSyntaxListenerChain listenerChain,
        LinkReferenceSerializer linkReferenceSerializer)
    {
        super(listenerChain, linkReferenceSerializer);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.internal.renderer.xwiki20.link.XWikiSyntaxLinkRenderer#printParameters(XWikiSyntaxEscapeWikiPrinter, Link, java.util.Map)
     */
    @Override
    protected void printParameters(XWikiSyntaxEscapeWikiPrinter printer, Link link,
        Map<String, String> parameters)
    {
        // Print the Query String and Anchor as parameters if they're defined and if the link is a link to a document.
        boolean shouldPrintSeparator = true;

        // The XWiki Syntax 2.1 supports two special Link reference parameters: queryString and anchor.
        if (link.getType().equals(LinkType.DOCUMENT)) {
            // Print first the query string
            String queryString = link.getParameter(DocumentLink.QUERY_STRING);
            if (!StringUtils.isEmpty(queryString)) {
                printer.print(PARAMETER_SEPARATOR);
                printer.print(this.parametersPrinter.print("queryString", queryString, '~'));
                shouldPrintSeparator = false;
            }
            // Then print the anchor
            String anchor = link.getParameter(DocumentLink.ANCHOR);
            if (!StringUtils.isEmpty(anchor)) {
                if (shouldPrintSeparator) {
                    printer.print(PARAMETER_SEPARATOR);
                } else {
                    printer.print(" ");
                }
                printer.print(this.parametersPrinter.print("anchor", anchor, '~'));
                shouldPrintSeparator = false;
            }
        }

        // Add all Link parameters but only if there isn't a Link Reference parameter of the same name...
        if (!parameters.isEmpty()) {
            if (shouldPrintSeparator) {
                printer.print(PARAMETER_SEPARATOR);
            } else {
                printer.print(" ");
            }
            printer.print(this.parametersPrinter.print(parameters, '~'));
        }
    }
}
