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
package org.xwiki.rendering.listener.chaining;

import java.util.Map;

import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.xml.XMLNode;

/**
 * Counts consecutive new lines.
 * 
 * @version $Id; $
 * @since 1.8RC1
 */
public class ConsecutiveNewLineStateChainingListener extends AbstractChainingListener implements StackableChainingListener
{
    private int newLineCount = 0;

    public ConsecutiveNewLineStateChainingListener(ListenerChain listenerChain)
    {
        super(listenerChain);
    }

    /**
     * {@inheritDoc}
     * @see StackableChainingListener#createChainingListenerInstance()
     */
    public StackableChainingListener createChainingListenerInstance()
    {
        return new ConsecutiveNewLineStateChainingListener(getListenerChain());
    }

    public int getNewLineCount()
    {
        return this.newLineCount;
    }

    public void endDefinitionDescription()
    {
        this.newLineCount = 0;
        super.endDefinitionDescription();
    }

    public void endDefinitionList()
    {
        this.newLineCount = 0;
        super.endDefinitionList();
    }

    public void endDefinitionTerm()
    {
        this.newLineCount = 0;
        super.endDefinitionTerm();
    }

    public void endDocument()
    {
        this.newLineCount = 0;
        super.endDocument();
    }

    public void endError(String message, String description)
    {
        this.newLineCount = 0;
        super.endError(message, description);
    }

    public void endFormat(Format format, Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endFormat(format, parameters);
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endLink(link, isFreeStandingURI, parameters);
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endList(listType, parameters);
    }

    public void endListItem()
    {
        this.newLineCount = 0;
        super.endListItem();
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        this.newLineCount = 0;
        super.endMacroMarker(name, parameters, content, isInline);
    }

    public void endParagraph(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endParagraph(parameters);
    }

    public void endQuotation(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endQuotation(parameters);
    }

    public void endQuotationLine()
    {
        this.newLineCount = 0;
        super.endQuotationLine();
    }

    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endHeader(level, parameters);
    }

    public void endTable(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endTable(parameters);
    }

    public void endTableCell(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endTableCell(parameters);
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endTableHeadCell(parameters);
    }

    public void endTableRow(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endTableRow(parameters);
    }

    public void endXMLNode(XMLNode node)
    {
        this.newLineCount = 0;
        super.endXMLNode(node);
    }

    public void onEmptyLines(int count)
    {
        this.newLineCount = 0;
        super.onEmptyLines(count);
    }

    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.onHorizontalLine(parameters);
    }

    public void onId(String name)
    {
        this.newLineCount = 0;
        super.onId(name);
    }

    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.onImage(image, isFreeStandingURI, parameters);
    }

    public void onNewLine()
    {
        this.newLineCount++;
        super.onNewLine();
    }

    public void onSpace()
    {
        this.newLineCount = 0;
        super.onSpace();
    }

    public void onSpecialSymbol(char symbol)
    {
        this.newLineCount = 0;
        super.onSpecialSymbol(symbol);
    }

    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        this.newLineCount = 0;
        super.onMacro(name, parameters, content, isInline);
    }

    public void onVerbatim(String protectedString, Map<String, String> parameters, boolean isInline)
    {
        this.newLineCount = 0;
        super.onVerbatim(protectedString, parameters, isInline);
    }

    public void onWord(String word)
    {
        this.newLineCount = 0;
        super.onWord(word);
    }
}
