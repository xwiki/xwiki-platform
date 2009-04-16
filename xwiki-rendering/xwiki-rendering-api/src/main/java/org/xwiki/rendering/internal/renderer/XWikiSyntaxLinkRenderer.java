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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Logic to render a XWiki Link into XWiki syntax.
 * 
 * @version $Id$
 * @since 1.7M1
 */
public class XWikiSyntaxLinkRenderer
{
    XWikiParametersPrinter parametersPrinter = new XWikiParametersPrinter();

    public String renderLinkReference(Link link)
    {
        StringBuilder buffer = new StringBuilder();

        if (link.getReference() != null) {
            buffer.append(link.getReference());
        }
        if (link.getAnchor() != null) {
            buffer.append('#');
            buffer.append(link.getAnchor());
        }
        if (link.getQueryString() != null) {
            buffer.append('?');
            buffer.append(link.getQueryString());
        }
        if (link.getInterWikiAlias() != null) {
            buffer.append('@');
            buffer.append(link.getInterWikiAlias());
        }

        return buffer.toString().replace(">>", "~>~>").replace("||", "~|~|");
    }

    public void beginRenderLink(WikiPrinter printer, Link link, boolean isFreeStandingURI,
        Map<String, String> parameters)
    {
        if (!isFreeStandingURI || (isFreeStandingURI && !parameters.isEmpty())) {
            printer.print("[[");
        }
    }

    public void renderLinkContent(WikiPrinter printer, String wikiSyntaxContent)
    {
        // If there was some link content specified then output the character separator ">>".
        if (!StringUtils.isEmpty(wikiSyntaxContent)) {
            printer.print(wikiSyntaxContent);
            printer.print(">>");
        }
    }

    public void endRenderLink(WikiPrinter printer, Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        printer.print(renderLinkReference(link));

        // If there were parameters specified, output them separated by the "||" characters
        if (!parameters.isEmpty()) {
            printer.print("||");
            printer.print(this.parametersPrinter.print(parameters));
        }

        if (!isFreeStandingURI || (isFreeStandingURI && !parameters.isEmpty())) {
            printer.print("]]");
        }
    }
}
