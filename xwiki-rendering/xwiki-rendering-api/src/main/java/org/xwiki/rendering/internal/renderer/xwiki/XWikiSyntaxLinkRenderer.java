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
package org.xwiki.rendering.internal.renderer.xwiki;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.internal.renderer.BasicLinkRenderer;
import org.xwiki.rendering.internal.renderer.ParametersPrinter;

/**
 * Logic to render a XWiki Link into XWiki syntax.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public class XWikiSyntaxLinkRenderer extends BasicLinkRenderer
{
    ParametersPrinter parametersPrinter = new ParametersPrinter();

    public String renderLinkReference(Link link)
    {
        return super.renderLinkReference(link).replace(">>", "~>~>").replace("||", "~|~|");
    }

    public void beginRenderLink(WikiPrinter printer, Link link, boolean isFreeStandingURI,
        Map<String, String> parameters)
    {
        if (!isFreeStandingURI || !parameters.isEmpty()) {
            printer.print("[[");
        }
    }

    public void renderLinkContent(WikiPrinter printer, String label)
    {
        // If there was some link content specified then output the character separator ">>".
        if (!StringUtils.isEmpty(label)) {
            printer.print(label);
            printer.print(">>");
        }
    }

    public void endRenderLink(WikiPrinter printer, Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        printer.print(renderLinkReference(link));

        // If there were parameters specified, output them separated by the "||" characters
        if (!parameters.isEmpty()) {
            printer.print("||");
            printer.print(this.parametersPrinter.print(parameters, '~'));
        }

        if (!isFreeStandingURI || !parameters.isEmpty()) {
            printer.print("]]");
        }
    }
}
