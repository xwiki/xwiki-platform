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
 * Indicate if the text being written starts a new line. By text we mean Space, Special Symbol and Words. This is useful
 * for some Renderers which need to have this information. For example the XWiki Syntax renderer uses it to decide
 * whether to escape "*" characters starting new lines since otherwise they would be confused for list items.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class TextOnNewLineStateListener implements Listener
{
    private boolean isTextOnNewLine;

    public boolean isTextOnNewLine()
    {
        return this.isTextOnNewLine;
    }
    
    public void beginDefinitionDescription()
    {
        this.isTextOnNewLine = false;
    }

    public void beginDefinitionList()
    {
        this.isTextOnNewLine = false;
    }

    public void beginDefinitionTerm()
    {
        this.isTextOnNewLine = false;
    }

    public void beginDocument()
    {
        this.isTextOnNewLine = false;
    }

    public void beginError(String message, String description)
    {
        this.isTextOnNewLine = false;
    }

    public void beginFormat(Format format, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void beginListItem()
    {
        this.isTextOnNewLine = false;
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.isTextOnNewLine = false;
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.isTextOnNewLine = true;
    }

    public void beginQuotation(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void beginQuotationLine()
    {
        this.isTextOnNewLine = false;
    }

    public void beginHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void beginTable(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void beginXMLNode(XMLNode node)
    {
        this.isTextOnNewLine = false;
    }

    public void endDefinitionDescription()
    {
        // Nothing to do
    }

    public void endDefinitionList()
    {
        // Nothing to do
    }

    public void endDefinitionTerm()
    {
        // Nothing to do
    }

    public void endDocument()
    {
        // Nothing to do
    }

    public void endError(String message, String description)
    {
        // Nothing to do
    }

    public void endFormat(Format format, Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void endListItem()
    {
        // Nothing to do
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Nothing to do
    }

    public void endParagraph(Map<String, String> parameters)
    {
        this.isTextOnNewLine = true;
    }

    public void endQuotation(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void endQuotationLine()
    {
        // Nothing to do
    }

    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void endTable(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void endTableCell(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void endTableRow(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void endXMLNode(XMLNode node)
    {
        // Nothing to do
    }

    public void onEmptyLines(int count)
    // Nothing to do
    {
    }

    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void onId(String name)
    {
        this.isTextOnNewLine = false;
    }

    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        this.isTextOnNewLine = false;
    }

    public void onLineBreak()
    {
        this.isTextOnNewLine = true;
    }

    public void onNewLine()
    {
        this.isTextOnNewLine = true;
    }

    public void onSpace()
    {
        // Nothing to do
    }

    public void onSpecialSymbol(char symbol)
    {
        // Nothing to do
    }

    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        this.isTextOnNewLine = false;
    }

    public void onVerbatimInline(String protectedString)
    {
        this.isTextOnNewLine = false;
    }

    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
    }

    public void onWord(String word)
    {
        // Nothing to do
    }
}
