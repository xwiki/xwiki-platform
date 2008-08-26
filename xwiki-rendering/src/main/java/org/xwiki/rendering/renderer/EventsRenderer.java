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

import java.util.Iterator;
import java.util.Map;

import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.renderer.AbstractPrintRenderer;
import org.xwiki.rendering.renderer.WikiPrinter;
import org.apache.commons.lang.StringUtils;

/**
 * @version $Id$
 * @since 1.5M1
 */
public class EventsRenderer extends AbstractPrintRenderer
{
    public EventsRenderer(WikiPrinter printer)
    {
        super(printer);
    }

    public void beginDocument()
    {
        println("beginDocument");
    }

    public void endDocument()
    {
        print("endDocument");
    }

    /**
     * {@inheritDoc}
     *
     * @see Listener#beginFormat(org.xwiki.rendering.listener.Format)
     */
    public void beginFormat(Format format)
    {
        switch(format)
        {
            case BOLD:
                println("beginBold");
                break;
            case ITALIC:
                println("beginItalic");
                break;
            case STRIKEDOUT:
                println("beginStrikedOut");
                break;
            case UNDERLINED:
                println("beginUnderline");
                break;
            case SUPERSCRIPT:
                println("beginSuperscript");
                break;
            case SUBSCRIPT:
                println("beginSubscript");
                break;
            case MONOSPACE:
                println("beginMonospace");
                break;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see Listener#endFormat(org.xwiki.rendering.listener.Format)
     */
    public void endFormat(Format format)
    {
        switch(format)
        {
            case BOLD:
                println("endBold");
                break;
            case ITALIC:
                println("endItalic");
                break;
            case STRIKEDOUT:
                println("endStrikedOut");
                break;
            case UNDERLINED:
                println("endUnderline");
                break;
            case SUPERSCRIPT:
                println("endSuperscript");
                break;
            case SUBSCRIPT:
                println("endSubscript");
                break;
            case MONOSPACE:
                println("endMonospace");
                break;
        }
    }

    public void beginParagraph()
    {
        println("beginParagraph");
    }

    public void endParagraph()
    {
        println("endParagraph");
    }

    public void onLineBreak()
    {
        println("onLineBreak");
    }

    public void onNewLine()
    {
        println("onNewLine");
    }

    public void onLink(Link link)
    {
        println("onLink: [" + link + "]");
    }

    public void onMacro(String name, Map<String, String> parameters, String content)
    {
        printMacroData("onMacro", name, parameters, content);
    }

    public void beginSection(SectionLevel level)
    {
        println("beginSection: [" + level + "]");
    }

    public void endSection(SectionLevel level)
    {
        println("endSection: [" + level + "]");
    }

    public void onWord(String word)
    {
        String printableWord;

        // If the word has any non printable character use the "(((char value)))" notation
        if (StringUtils.isAsciiPrintable(word)) {
            printableWord = word;
        } else {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (c > 126) {
                    buffer.append("(((").append((int) c).append(")))");
                } else {
                    buffer.append(c);
                }
            }
            printableWord = buffer.toString();
        }

        println("onWord: [" + printableWord + "]");
    }

    public void beginList(ListType listType)
    {
        println("beginList: [" + listType + "]");
    }

    public void beginListItem()
    {
        println("beginListItem");
    }

    public void endList(ListType listType)
    {
        println("endList: [" + listType + "]");
    }

    public void endListItem()
    {
        println("endListItem");
    }

    public void onSpace()
    {
        println("onSpace");
    }

    public void onSpecialSymbol(String symbol)
    {
        println("onSpecialSymbol: [" + symbol + "]");
    }

    public void onEscape(String escapedString)
    {
        println("onEscape: [" + escapedString + "]");
    }

    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        println("beginXMLElement: [" + name + "] [" + toStringXMLElement(attributes) + "]");
    }

    public void endXMLElement(String name, Map<String, String> attributes)
    {
        println("endXMLElement: [" + name + "] [" + toStringXMLElement(attributes) + "]");
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        printMacroData("beginMacroMarker", name, parameters, content);
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        printMacroData("endMacroMarker", name, parameters, content);
    }

    public void onId(String name)
    {
        println("onId: [" + name + "]");
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine() 
     */
    public void onHorizontalLine()
    {
        println("onHorizontalLine");
    }

    private StringBuffer toStringXMLElement(Map<String, String> attributes)
    {
        StringBuffer buffer = new StringBuffer();
        for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext();) {
            String attributeName = it.next();
            buffer.append(attributeName).append("=").append(attributes.get(attributeName));
            if (it.hasNext()) {
                buffer.append(",");
            }
        }
        return buffer;
    }

    private void printMacroData(String eventName, String name, Map<String, String> parameters, String content)
    {
        StringBuffer buffer = new StringBuffer();
        for (Iterator<String> paramsIt = parameters.keySet().iterator(); paramsIt.hasNext();) {
            String paramName = paramsIt.next();
            buffer.append(paramName).append("=").append(parameters.get(paramName));
            if (paramsIt.hasNext()) {
                buffer.append("|");
            }
        }
        println(eventName + ": [" + name + "] [" + buffer.toString() + "] [" + content + "]");
    }
}
