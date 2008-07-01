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

import java.io.Writer;
import java.util.Map;

import org.xwiki.rendering.DocumentManager;
import org.xwiki.rendering.internal.XWikiMacroPrinter;

public class WysiwygEditorXHTMLRenderer extends XHTMLRenderer
{
	private XWikiMacroPrinter macroPrinter;
	
    public WysiwygEditorXHTMLRenderer(Writer writer, DocumentManager documentManager)
    {
    	super(writer, documentManager);
    	this.macroPrinter = new XWikiMacroPrinter();
    }

	@Override
	public void beginMacroMarker(String name, Map<String, String> parameters, String content)
	{
		write("<span class=\"macro-code\"><![CDATA[");
		// Write the source of the macro
		write(this.macroPrinter.print(name, parameters, content));
		write("]]></span><span class=\"macro-output\">");
	}

	@Override
	public void endMacroMarker(String name, Map<String, String> parameters, String content)
	{
		write("</span>");
	}
}
