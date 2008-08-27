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

import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.internal.XWikiMacroPrinter;
import org.xwiki.rendering.renderer.WikiPrinter;

public class WysiwygEditorXHTMLRenderer extends XHTMLRenderer
{
    private XWikiMacroPrinter macroPrinter;

    public WysiwygEditorXHTMLRenderer(WikiPrinter printer, DocumentAccessBridge documentAccessBridge,
        RenderingConfiguration configuration)
    {
        super(printer, documentAccessBridge, configuration);
        this.macroPrinter = new XWikiMacroPrinter();
    }

    @Override
    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        print("<span class=\"macro-code\"><![CDATA[");
        // Print the source of the macro
        print(this.macroPrinter.print(name, parameters, content));
        print("]]></span><span class=\"macro-output\">");
    }

    @Override
    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        print("</span>");
    }
}
