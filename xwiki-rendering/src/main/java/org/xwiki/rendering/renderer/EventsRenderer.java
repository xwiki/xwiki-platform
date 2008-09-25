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
import org.apache.commons.lang.StringUtils;

/**
 * Print names of events. Useful for debugging and tracing in general. Note that this class is not located in the test
 * source tree since it's currently used at runtime by the WYSIWYG editor for its runtime debug mode.
 * 
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
        switch (format) {
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
        switch (format) {
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

    public void beginParagraph(Map<String, String> parameters)
    {
        println("beginParagraph" + serializeParameters(parameters));
    }

    public void endParagraph(Map<String, String> parameters)
    {
        println("endParagraph" + serializeParameters(parameters));
    }

    public void onLineBreak()
    {
        println("onLineBreak");
    }

    public void onNewLine()
    {
        println("onNewLine");
    }

    public void onLink(Link link, boolean isFreeStandingURI)
    {
        println("onLink [" + link + "] [" + isFreeStandingURI + "]");
    }

    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        printMacroData("onMacroInline", name, parameters, content);
    }

    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        printMacroData("onMacroStandalone", name, parameters, content);
    }

    public void beginSection(SectionLevel level, Map<String, String> parameters)
    {
        println("beginSection [" + level + "]" + serializeParameters(parameters));
    }

    public void endSection(SectionLevel level, Map<String, String> parameters)
    {
        println("endSection [" + level + "]" + serializeParameters(parameters));
    }

    public void onWord(String word)
    {
        println("onWord [" + getEscaped(word) + "]");
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        println("beginList [" + listType + "]" + serializeParameters(parameters));
    }

    public void beginListItem()
    {
        println("beginListItem");
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        println("endList [" + listType + "]" + serializeParameters(parameters));
    }

    public void endListItem()
    {
        println("endListItem");
    }

    public void onSpace()
    {
        println("onSpace");
    }

    public void onSpecialSymbol(char symbol)
    {
        println("onSpecialSymbol [" + symbol + "]");
    }

    public void onEscape(String escapedString)
    {
        println("onEscape [" + escapedString + "]");
    }

    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        println("beginXMLElement [" + name + "] [" + toStringXMLElement(attributes) + "]");
    }

    public void endXMLElement(String name, Map<String, String> attributes)
    {
        println("endXMLElement [" + name + "] [" + toStringXMLElement(attributes) + "]");
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
        println("onId [" + name + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine(Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        println("onHorizontalLine" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        println("onEmptyLines [" + count + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatimInline(String)
     */
    public void onVerbatimInline(String protectedString)
    {
        println("onVerbatimInline [" + protectedString + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatimStandalone(String)
     */
    public void onVerbatimStandalone(String protectedString)
    {
        println("onVerbatimStandalone [" + protectedString + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList()
     * @since 1.6M2
     */
    public void beginDefinitionList()
    {
        println("beginDefinitionList");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList()
     * @since 1.6M2
     */
    public void endDefinitionList()
    {
        println("endDefinitionList");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     * @since 1.6M2
     */
    public void beginDefinitionTerm()
    {
        println("beginDefinitionTerm");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     * @since 1.6M2
     */
    public void beginDefinitionDescription()
    {
        println("beginDefinitionDescription");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     * @since 1.6M2
     */
    public void endDefinitionTerm()
    {
        println("endDefinitionTerm");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     * @since 1.6M2
     */
    public void endDefinitionDescription()
    {
        println("endDefinitionDescription");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        println("beginQuotation" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void endQuotation(Map<String, String> parameters)
    {
        println("endQuotation" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     * @since 1.6M2
     */
    public void beginQuotationLine()
    {
        println("beginQuotationLine");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     * @since 1.6M2
     */
    public void endQuotationLine()
    {
        println("endQuotationLine");
    }

    public void beginTable(Map<String, String> parameters)
    {
        println("beginTable" + serializeParameters(parameters));
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        println("beginTableCell" + serializeParameters(parameters));
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        println("beginTableHeadCell" + serializeParameters(parameters));
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        println("beginTableRow" + serializeParameters(parameters));
    }

    public void endTable(Map<String, String> parameters)
    {
        println("endTable" + serializeParameters(parameters));
    }

    public void endTableCell(Map<String, String> parameters)
    {
        println("endTableCell" + serializeParameters(parameters));
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        println("endTableHeadCell" + serializeParameters(parameters));
    }

    public void endTableRow(Map<String, String> parameters)
    {
        println("endTableRow" + serializeParameters(parameters));
    }

    public String getEscaped(String str)
    {
        String printableStr;

        if (StringUtils.isAsciiPrintable(str)) {
            printableStr = str;
        } else {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c > 126) {
                    buffer.append("(((").append((int) c).append(")))");
                } else {
                    buffer.append(c);
                }
            }
            printableStr = buffer.toString();
        }

        return printableStr;
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
        println(eventName + " [" + name + "] [" + buffer.toString() + "] [" + content + "]");
    }

    private String serializeParameters(Map<String, String> parameters)
    {
        StringBuffer buffer = new StringBuffer();
        if (!parameters.isEmpty()) {
        	buffer.append(' ').append('[');
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                buffer.append('[').append(getEscaped(entry.getKey())).append(']').append('=').append('[').append(
                    getEscaped(entry.getValue())).append(']');
            }
            buffer.append(']');
        }
        return buffer.toString();
    }
}
