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
package org.xwiki.rendering.internal.renderer.xhtml;

import java.util.Map;

import org.xwiki.rendering.internal.renderer.ParametersPrinter;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;

/**
 * Renders a XWiki Macro into Annotated XHTML (ie the macro definition is created as XHTML comments).
 *
 * @version $Id$
 * @since 1.7M2
 */
public class XHTMLMacroRenderer
{
    /**
     * Character to separate Macro name, content and parameters in XHTML comments.
     */
    private static final String COMMENT_SEPARATOR = "|-|";

    private ParametersPrinter parametersPrinter = new ParametersPrinter();

    public void render(XHTMLWikiPrinter printer, String name, Map<String, String> parameters, String content)
    {
        beginRender(printer, name, parameters, content);
        endRender(printer);
    }

    public void beginRender(XHTMLWikiPrinter printer, String name, Map<String, String> parameters, String content)
    {
        StringBuilder buffer = new StringBuilder("startmacro:");

        // Print name
        buffer.append(name);

        // Print parameters
        buffer.append(COMMENT_SEPARATOR);
        if (!parameters.isEmpty()) {
            buffer.append(this.parametersPrinter.print(parameters, '\\'));
        }

        // Print content
        if (content != null) {
            buffer.append(COMMENT_SEPARATOR);
            buffer.append(content);
        }

        printer.printXMLComment(buffer.toString(), true);
    }

    public void endRender(XHTMLWikiPrinter printer)
    {
        printer.printXMLComment("stopmacro");
    }
}
