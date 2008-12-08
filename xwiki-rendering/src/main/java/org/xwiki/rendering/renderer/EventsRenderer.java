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

import org.xwiki.rendering.listener.DocumentImage;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.ImageType;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.URLImage;
import org.xwiki.rendering.listener.xml.XMLNode;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
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
        getPrinter().println("beginDocument");
    }

    public void endDocument()
    {
        getPrinter().print("endDocument");
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#beginFormat(Format, Map)
     */
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        getPrinter().println("beginFormat: [" + format + "]" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#endFormat(Format, Map)
     */
    public void endFormat(Format format, Map<String, String> parameters)
    {
        getPrinter().println("endFormat: [" + format + "]" + serializeParameters(parameters));
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        getPrinter().println("beginParagraph" + serializeParameters(parameters));
    }

    public void endParagraph(Map<String, String> parameters)
    {
        getPrinter().println("endParagraph" + serializeParameters(parameters));
    }

    public void onLineBreak()
    {
        getPrinter().println("onLineBreak");
    }

    public void onNewLine()
    {
        getPrinter().println("onNewLine");
    }

    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        getPrinter().println("beginLink [" + link + "] [" + isFreeStandingURI + "]" + serializeParameters(parameters));
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        getPrinter().println("endLink [" + link + "] [" + isFreeStandingURI + "]" + serializeParameters(parameters));
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
        getPrinter().println("beginSection [" + level + "]" + serializeParameters(parameters));
    }

    public void endSection(SectionLevel level, Map<String, String> parameters)
    {
        getPrinter().println("endSection [" + level + "]" + serializeParameters(parameters));
    }

    public void onWord(String word)
    {
        getPrinter().println("onWord [" + getEscaped(word) + "]");
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        getPrinter().println("beginList [" + listType + "]" + serializeParameters(parameters));
    }

    public void beginListItem()
    {
        getPrinter().println("beginListItem");
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        getPrinter().println("endList [" + listType + "]" + serializeParameters(parameters));
    }

    public void endListItem()
    {
        getPrinter().println("endListItem");
    }

    public void onSpace()
    {
        getPrinter().println("onSpace");
    }

    public void onSpecialSymbol(char symbol)
    {
        getPrinter().println("onSpecialSymbol [" + symbol + "]");
    }

    public void beginXMLNode(XMLNode node)
    {
        getPrinter().println("beginXMLNode " + node);
    }

    public void endXMLNode(XMLNode node)
    {
        getPrinter().println("endXMLNode " + node);
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
        getPrinter().println("onId [" + name + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine(Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        getPrinter().println("onHorizontalLine" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        getPrinter().println("onEmptyLines [" + count + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatimInline(String)
     */
    public void onVerbatimInline(String protectedString)
    {
        getPrinter().println("onVerbatimInline [" + protectedString + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatimStandalone(String, Map)
     */
    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        getPrinter().println("onVerbatimStandalone [" + protectedString + "]" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList()
     * @since 1.6M2
     */
    public void beginDefinitionList()
    {
        getPrinter().println("beginDefinitionList");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList()
     * @since 1.6M2
     */
    public void endDefinitionList()
    {
        getPrinter().println("endDefinitionList");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     * @since 1.6M2
     */
    public void beginDefinitionTerm()
    {
        getPrinter().println("beginDefinitionTerm");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     * @since 1.6M2
     */
    public void beginDefinitionDescription()
    {
        getPrinter().println("beginDefinitionDescription");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     * @since 1.6M2
     */
    public void endDefinitionTerm()
    {
        getPrinter().println("endDefinitionTerm");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     * @since 1.6M2
     */
    public void endDefinitionDescription()
    {
        getPrinter().println("endDefinitionDescription");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        getPrinter().println("beginQuotation" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void endQuotation(Map<String, String> parameters)
    {
        getPrinter().println("endQuotation" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     * @since 1.6M2
     */
    public void beginQuotationLine()
    {
        getPrinter().println("beginQuotationLine");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     * @since 1.6M2
     */
    public void endQuotationLine()
    {
        getPrinter().println("endQuotationLine");
    }

    public void beginTable(Map<String, String> parameters)
    {
        getPrinter().println("beginTable" + serializeParameters(parameters));
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        getPrinter().println("beginTableCell" + serializeParameters(parameters));
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        getPrinter().println("beginTableHeadCell" + serializeParameters(parameters));
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        getPrinter().println("beginTableRow" + serializeParameters(parameters));
    }

    public void endTable(Map<String, String> parameters)
    {
        getPrinter().println("endTable" + serializeParameters(parameters));
    }

    public void endTableCell(Map<String, String> parameters)
    {
        getPrinter().println("endTableCell" + serializeParameters(parameters));
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        getPrinter().println("endTableHeadCell" + serializeParameters(parameters));
    }

    public void endTableRow(Map<String, String> parameters)
    {
        getPrinter().println("endTableRow" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onImage(org.xwiki.rendering.listener.Image, boolean, Map)
     * @since 1.7M2
     */
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        if (image.getType() == ImageType.DOCUMENT) {
            DocumentImage documentImage = (DocumentImage) image;
            getPrinter().println("onImage: " + (documentImage.getDocumentName() != null ? "[" 
                + documentImage.getDocumentName() + "] " : "") + "[" + documentImage.getAttachmentName() + "] [" 
                + isFreeStandingURI + "]" + serializeParameters(parameters));
        } else {
            URLImage urlImage = (URLImage) image;
            getPrinter().println("onImage: [" + urlImage.getURL() + "] [" + isFreeStandingURI + "]" 
                + serializeParameters(parameters));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginError(String, String)
     * @since 1.7M3
     */
    public void beginError(String message, String description)
    {
        getPrinter().println("beginError: [" + message + "] [" + description + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endError(String, String)
     * @since 1.7M3
     */
    public void endError(String message, String description)
    {
        getPrinter().println("endError: [" + message + "] [" + description + "]");
    }

    public String getEscaped(String str)
    {
        String printableStr;

        if (str == null) {
            printableStr = null;
        } else  if (StringUtils.isAsciiPrintable(str)) {
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
        getPrinter().println(eventName + " [" + name + "] [" + buffer.toString() + "] [" + content + "]");
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
