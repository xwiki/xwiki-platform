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
package org.xwiki.rendering.internal.renderer.state;

import java.util.Map;

import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.xml.XMLNode;

/**
 * Counts consecutive new lines.
 * 
 * @version $Id; $
 * @since 1.8M1
 */
public class ConsecutiveNewLineStateListener implements Listener
{
    private int newLineCount = 0;

    public int getNewLineCount()
    {
        return this.newLineCount;
    }

    public void beginDefinitionDescription()
    {
        // Nothing to do
    }

    public void beginDefinitionList()
    {
        // Nothing to do
    }

    public void beginDefinitionTerm()
    {
        // Nothing to do
    }

    public void beginDocument()
    {
        // Nothing to do
    }

    public void beginError(String message, String description)
    {
        // Nothing to do
    }

    public void beginFormat(Format format, Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginListItem()
    {
        // Nothing to do
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Nothing to do
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginQuotation(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginQuotationLine()
    {
        // Nothing to do
    }

    public void beginSection(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginHeader(HeaderLevel level, Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginTable(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginXMLNode(XMLNode node)
    {
        // Nothing to do
    }

    public void endDefinitionDescription()
    {
        this.newLineCount = 0;
    }

    public void endDefinitionList()
    {
        this.newLineCount = 0;
    }

    public void endDefinitionTerm()
    {
        this.newLineCount = 0;
    }

    public void endDocument()
    {
        this.newLineCount = 0;
    }

    public void endError(String message, String description)
    {
        this.newLineCount = 0;
    }

    public void endFormat(Format format, Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void endListItem()
    {
        this.newLineCount = 0;
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.newLineCount = 0;
    }

    public void endParagraph(Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void endQuotation(Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void endQuotationLine()
    {
        this.newLineCount = 0;
    }

    public void endSection(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void endTable(Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void endTableCell(Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void endTableRow(Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void endXMLNode(XMLNode node)
    {
        this.newLineCount = 0;
    }

    public void onEmptyLines(int count)
    {
        this.newLineCount = 0;
    }

    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void onId(String name)
    {
        this.newLineCount = 0;
    }

    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        this.newLineCount = 0;
    }

    public void onNewLine()
    {
        this.newLineCount++;
    }

    public void onSpace()
    {
        this.newLineCount = 0;
    }

    public void onSpecialSymbol(char symbol)
    {
        this.newLineCount = 0;
    }

    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        this.newLineCount = 0;
    }

    public void onVerbatimInline(String protectedString)
    {
        this.newLineCount = 0;
    }

    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        this.newLineCount = 0;
    }

    public void onWord(String word)
    {
        this.newLineCount = 0;
    }
}
