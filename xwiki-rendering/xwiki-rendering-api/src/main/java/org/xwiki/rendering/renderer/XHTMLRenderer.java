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

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.Link;

public class XHTMLRenderer implements Renderer
{
    private PrintWriter writer;
    
    public XHTMLRenderer(Writer writer)
    {
        this.writer = new PrintWriter(writer);
    }

    public void beginBold()
    {
        write("<strong>");
    }

    public void beginItalic()
    {
        write("<em class=\"italic\">");
    }

    public void beginParagraph()
    {
        write("<p>");
    }

    public void endBold()
    {
        write("</strong>");
    }

    public void endItalic()
    {
        write("</em>");
    }

    public void endParagraph()
    {
        write("</p>");
    }

    public void onLineBreak()
    {
        // TODO Auto-generated method stub

    }

    public void onLink(Link link)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void onMacro(String name, Map<String, String> parameters, String content)
    {
        // Do nothing since macro output depends on Macro execution which transforms the macro
        // into a set of other events.
    }

    public void beginSection(SectionLevel level)
    {
        int levelAsInt = level.getAsInt();
        write("<h" + (levelAsInt + 1) + " class=\"heading-" + levelAsInt + "\" id=\"HSectionlevel" + levelAsInt + "\">");
        write("<span>");
    }

    public void endSection(SectionLevel level)
    {
        int levelAsInt = level.getAsInt();
        write("</span>");
        write("</h" + (levelAsInt + 1) + ">");
    }

    public void onWord(String word)
    {
        write(word);
    }

    public void onSpace()
    {
        write(" ");
    }

    public void onSpecialSymbol(String symbol)
    {
        write(StringEscapeUtils.escapeHtml(symbol));
    }

    public void beginList(ListType listType)
    {
        if (listType == ListType.BULLETED) {
            write("<ul class=\"star\">");
        } else {
            write("<ol>");
        }
    }

    public void beginListItem()
    {
        write("<li>");
    }

    public void endList(ListType listType)
    {
        write("</ul>");
    }

    public void endListItem()
    {
        write("</li>");
    }
    
    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        write("<" + name);
        for (String attributeName: attributes.keySet()) {
            write(" " + attributeName + "=\"" + attributes.get(attributeName) + "\"");
        }
        write(">");
    }

    public void endXMLElement(String name, Map<String, String> attributes)
    {
        write("</" + name + ">");
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Ignore macro markers, nothing to do.
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Ignore macro markers, nothing to do.
    }

    private void write(String text)
    {
        this.writer.write(text);
    }
}
