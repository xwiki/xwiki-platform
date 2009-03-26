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
package org.xwiki.rendering.internal.renderer.chaining;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.listener.DocumentImage;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.ImageType;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.URLImage;
import org.xwiki.rendering.listener.chaining.DocumentStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.xml.XMLNode;
import org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Prints listener event names in a format useful for testing and debugging.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class EventsChainingRenderer extends AbstractChainingPrintRenderer
{
    public EventsChainingRenderer(WikiPrinter printer, ListenerChain listenerChain)
    {
        super(printer, listenerChain);
    }

    // State

    private DocumentStateChainingListener getDocumentState()
    {
        return (DocumentStateChainingListener) getListenerChain().getListener(DocumentStateChainingListener.class);
    }

    // Events

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginDocument()
     */
    @Override
    public void beginDocument()
    {
        getPrinter().println("beginDocument");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endDocument()
     */
    @Override
    public void endDocument()
    {
        if (getDocumentState().getDocumentDepth() > 1) {
            getPrinter().println("endDocument");
        } else {
            getPrinter().print("endDocument");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginFormat(org.xwiki.rendering.listener.Format,
     *      java.util.Map)
     */
    @Override
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        getPrinter().println("beginFormat: [" + format + "]" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endFormat(org.xwiki.rendering.listener.Format,
     *      java.util.Map)
     */
    @Override
    public void endFormat(Format format, Map<String, String> parameters)
    {
        getPrinter().println("endFormat: [" + format + "]" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginParagraph(java.util.Map)
     */
    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        getPrinter().println("beginParagraph" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endParagraph(java.util.Map)
     */
    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        getPrinter().println("endParagraph" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onNewLine()
     */
    @Override
    public void onNewLine()
    {
        getPrinter().println("onNewLine");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginLink(org.xwiki.rendering.listener.Link,
     *      boolean, java.util.Map)
     */
    @Override
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        getPrinter().println("beginLink [" + link + "] [" + isFreeStandingURI + "]" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endLink(org.xwiki.rendering.listener.Link,
     *      boolean, java.util.Map)
     */
    @Override
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        getPrinter().println("endLink [" + link + "] [" + isFreeStandingURI + "]" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onMacro(java.lang.String, java.util.Map,
     *      java.lang.String, boolean)
     */
    @Override
    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        printMacroData("onMacro", name, parameters, content, isInline);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginSection(java.util.Map)
     */
    @Override
    public void beginSection(Map<String, String> parameters)
    {
        getPrinter().println("beginSection" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginHeader(org.xwiki.rendering.listener.HeaderLevel,
     *      String, java.util.Map)
     */
    @Override
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        getPrinter().println("beginHeader [" + level + ", " + id + "]" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endSection(java.util.Map)
     */
    @Override
    public void endSection(Map<String, String> parameters)
    {
        getPrinter().println("endSection" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endHeader(org.xwiki.rendering.listener.HeaderLevel,
     *      String, java.util.Map)
     */
    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        getPrinter().println("endHeader [" + level + ", " + id + "]" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onWord(java.lang.String)
     */
    @Override
    public void onWord(String word)
    {
        getPrinter().println("onWord [" + getEscaped(word) + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginList(org.xwiki.rendering.listener.ListType,
     *      java.util.Map)
     */
    @Override
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        getPrinter().println("beginList [" + listType + "]" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginListItem()
     */
    @Override
    public void beginListItem()
    {
        getPrinter().println("beginListItem");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endList(org.xwiki.rendering.listener.ListType,
     *      java.util.Map)
     */
    @Override
    public void endList(ListType listType, Map<String, String> parameters)
    {
        getPrinter().println("endList [" + listType + "]" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endListItem()
     */
    @Override
    public void endListItem()
    {
        getPrinter().println("endListItem");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onSpace()
     */
    @Override
    public void onSpace()
    {
        getPrinter().println("onSpace");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onSpecialSymbol(char)
     */
    @Override
    public void onSpecialSymbol(char symbol)
    {
        getPrinter().println("onSpecialSymbol [" + symbol + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginXMLNode(org.xwiki.rendering.listener.xml.XMLNode)
     */
    @Override
    public void beginXMLNode(XMLNode node)
    {
        getPrinter().println("beginXMLNode " + node);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endXMLNode(org.xwiki.rendering.listener.xml.XMLNode)
     */
    @Override
    public void endXMLNode(XMLNode node)
    {
        getPrinter().println("endXMLNode " + node);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginMacroMarker(java.lang.String,
     *      java.util.Map, java.lang.String, boolean)
     */
    @Override
    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        printMacroData("beginMacroMarker", name, parameters, content, isInline);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endMacroMarker(java.lang.String,
     *      java.util.Map, java.lang.String, boolean)
     */
    @Override
    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        printMacroData("endMacroMarker", name, parameters, content, isInline);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onId(java.lang.String)
     */
    @Override
    public void onId(String name)
    {
        getPrinter().println("onId [" + name + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onHorizontalLine(java.util.Map)
     */
    @Override
    public void onHorizontalLine(Map<String, String> parameters)
    {
        getPrinter().println("onHorizontalLine" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onEmptyLines(int)
     */
    @Override
    public void onEmptyLines(int count)
    {
        getPrinter().println("onEmptyLines [" + count + "]");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onVerbatim(java.lang.String,
     *      java.util.Map, boolean)
     */
    @Override
    public void onVerbatim(String protectedString, Map<String, String> parameters, boolean isInline)
    {
        getPrinter().println(
            "onVerbatim" + (isInline ? "Inline" : "Standalone") + " [" + protectedString + "]"
                + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginDefinitionList()
     */
    @Override
    public void beginDefinitionList()
    {
        getPrinter().println("beginDefinitionList");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endDefinitionList()
     */
    @Override
    public void endDefinitionList()
    {
        getPrinter().println("endDefinitionList");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginDefinitionTerm()
     */
    @Override
    public void beginDefinitionTerm()
    {
        getPrinter().println("beginDefinitionTerm");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginDefinitionDescription()
     */
    @Override
    public void beginDefinitionDescription()
    {
        getPrinter().println("beginDefinitionDescription");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endDefinitionTerm()
     */
    @Override
    public void endDefinitionTerm()
    {
        getPrinter().println("endDefinitionTerm");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endDefinitionDescription()
     */
    @Override
    public void endDefinitionDescription()
    {
        getPrinter().println("endDefinitionDescription");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginQuotation(java.util.Map)
     */
    @Override
    public void beginQuotation(Map<String, String> parameters)
    {
        getPrinter().println("beginQuotation" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endQuotation(java.util.Map)
     */
    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        getPrinter().println("endQuotation" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginQuotationLine()
     */
    @Override
    public void beginQuotationLine()
    {
        getPrinter().println("beginQuotationLine");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endQuotationLine()
     */
    @Override
    public void endQuotationLine()
    {
        getPrinter().println("endQuotationLine");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginTable(java.util.Map)
     */
    @Override
    public void beginTable(Map<String, String> parameters)
    {
        getPrinter().println("beginTable" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginTableCell(java.util.Map)
     */
    @Override
    public void beginTableCell(Map<String, String> parameters)
    {
        getPrinter().println("beginTableCell" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginTableHeadCell(java.util.Map)
     */
    @Override
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        getPrinter().println("beginTableHeadCell" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginTableRow(java.util.Map)
     */
    @Override
    public void beginTableRow(Map<String, String> parameters)
    {
        getPrinter().println("beginTableRow" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endTable(java.util.Map)
     */
    @Override
    public void endTable(Map<String, String> parameters)
    {
        getPrinter().println("endTable" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endTableCell(java.util.Map)
     */
    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        getPrinter().println("endTableCell" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endTableHeadCell(java.util.Map)
     */
    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        getPrinter().println("endTableHeadCell" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endTableRow(java.util.Map)
     */
    @Override
    public void endTableRow(Map<String, String> parameters)
    {
        getPrinter().println("endTableRow" + serializeParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onImage(org.xwiki.rendering.listener.Image,
     *      boolean, java.util.Map)
     */
    @Override
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        if (image.getType() == ImageType.DOCUMENT) {
            DocumentImage documentImage = (DocumentImage) image;
            getPrinter().println(
                "onImage: "
                    + (documentImage.getDocumentName() != null ? "[" + documentImage.getDocumentName() + "] " : "")
                    + "[" + documentImage.getAttachmentName() + "] [" + isFreeStandingURI + "]"
                    + serializeParameters(parameters));
        } else {
            URLImage urlImage = (URLImage) image;
            getPrinter().println(
                "onImage: [" + urlImage.getURL() + "] [" + isFreeStandingURI + "]" + serializeParameters(parameters));
        }
    }

    public String getEscaped(String str)
    {
        String printableStr;

        if (str == null) {
            printableStr = null;
        } else if (StringUtils.isAsciiPrintable(str)) {
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

    private void printMacroData(String eventName, String name, Map<String, String> parameters, String content,
        boolean isInline)
    {
        StringBuffer buffer = new StringBuffer();
        for (Iterator<String> paramsIt = parameters.keySet().iterator(); paramsIt.hasNext();) {
            String paramName = paramsIt.next();
            buffer.append(paramName).append("=").append(parameters.get(paramName));
            if (paramsIt.hasNext()) {
                buffer.append("|");
            }
        }
        getPrinter().println(
            eventName + (isInline ? "Inline" : "Standalone") + " [" + name + "] [" + buffer.toString() + "] ["
                + content + "]");
    }

    private String serializeParameters(Map<String, String> parameters)
    {
        StringBuffer parametersStr = new StringBuffer();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String value = entry.getValue();
            String key = entry.getKey();

            if (key != null && value != null) {
                parametersStr.append('[').append(getEscaped(entry.getKey())).append(']').append('=').append('[')
                    .append(getEscaped(entry.getValue())).append(']');
            }
        }

        if (parametersStr.length() > 0) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(' ').append('[');
            buffer.append(parametersStr);
            buffer.append(']');
            return buffer.toString();
        } else {
            return "";
        }
    }
}
