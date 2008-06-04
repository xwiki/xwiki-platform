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
package org.xwiki.rendering;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.SpecialSymbol;

public class TestEventsListener implements Listener
{
    private PrintWriter writer;
    
    public TestEventsListener(Writer writer)
    {
        this.writer = new PrintWriter(writer);
    }

    public void beginBold()
    {
        write("beginBold");
    }

    public void beginItalic()
    {
        write("beginItalic");
    }

    public void beginParagraph()
    {
        write("beginParagraph");
    }

    public void endBold()
    {
        write("endBold");
    }

    public void endItalic()
    {
        write("endItalic");
    }

    public void endParagraph()
    {
        write("endParagraph");
    }

    public void onLineBreak()
    {
        write("onLineBreak");
    }

    public void onLink(String text)
    {
        write("onLink: [" + text + "]");
    }

    public void onMacro(String name, Map<String, String> parameters, String content)
    {
        StringBuffer buffer = new StringBuffer();
        for (Iterator<String> paramsIt = parameters.keySet().iterator(); paramsIt.hasNext();) {
            String paramName = paramsIt.next();
            buffer.append(paramName + "=" + parameters.get(paramName));
            if (paramsIt.hasNext()) {
                buffer.append("|");
            }
        }
        write("onMacro: [" + name + "] [" + buffer.toString() + "] [" + content + "]");
    }

    public void beginSection(SectionLevel level)
    {
        write("beginSection: [" + level + "]");
    }

    public void endSection(SectionLevel level)
    {
        write("endSection: [" + level + "]");
    }

    public void onWord(String word)
    {
        write("onWord: [" + word + "]");
    }

    public void beginList(ListType listType)
    {
        write("beginList: [" + listType + "]");
    }

    public void beginListItem()
    {
        write("beginListItem");
    }

    public void endList(ListType listType)
    {
        write("endList: [" + listType + "]");
    }

    public void endListItem()
    {
        write("endListItem");
    }

    public void onSpace()
    {
        write("onSpace");
    }

    public void onSpecialSymbol(SpecialSymbol symbol)
    {
        write("onSpecialSymbol: [" + symbol + "]");
    }

    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        write("beginXMLElement: [" + name + "] [" + toStringXMLElement(attributes) + "]");
    }

    public void endXMLElement(String name, Map<String, String> attributes)
    {
        write("endXMLElement: [" + name + "] [" + toStringXMLElement(attributes) + "]");
    }
    
    private StringBuffer toStringXMLElement(Map<String, String> attributes)
    {
        StringBuffer buffer = new StringBuffer();
        for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext();) {
            String attributeName = it.next();
            buffer.append(attributeName + "=" + attributes.get(attributeName));
            if (it.hasNext()) {
                buffer.append(",");
            }
        }
        return buffer;
    }
    
    private void write(String text)
    {
        this.writer.write(text + "\n");
    }
}
