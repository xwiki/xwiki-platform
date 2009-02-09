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
 * Indicates block element for which we are inside and previous blocks.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class BlockStateListener implements Listener
{
    public enum Event
    {
        NONE,
        DEFINITION_DESCRIPTION,
        DEFINITION_TERM,
        DEFINITION_LIST,
        DOCUMENT,
        ERROR,
        FORMAT,
        HEADER,
        LINK,
        LIST,
        LIST_ITEM,
        MACRO_MARKER,
        PARAGRAPH,
        QUOTATION,
        QUOTATION_LINE,
        SECTION,
        TABLE,
        TABLE_CELL,
        TABLE_HEAD_CELL,
        TABLE_ROW,
        XML_NODE,
        EMPTY_LINES,
        HORIZONTAL_LINE,
        ID,
        IMAGE,
        NEW_LINE,
        SPACE,
        SPECIAL_SYMBOL,
        MACRO,
        VERBATIM_INLINE,
        VERBATIM_STANDALONE,
        WORD
    }
    
    private Event previousEvent = Event.NONE;
    
    private int documentDepth;

    private int inlineDepth = 0;

    private boolean isInParagraph;

    private boolean isInHeader;

    private int linkDepth = 0;

    private boolean isInTable;

    private boolean isInTableCell;

    private int definitionListDepth = 0;

    private int listDepth;

    private int listItemDepth;

    private int quotationDepth = 0;

    private boolean isInQuotationLine;

    public Event getPreviousEvent()
    {
        return this.previousEvent;
    }
    
    public void setDocumentDepth(int documentDepth)
    {
        this.documentDepth = documentDepth;
    }

    public int getDocumentDepth()
    {
        return this.documentDepth;
    }

    public boolean isInDocument()
    {
        return this.documentDepth > 0;
    }

    public boolean isInLine()
    {
        return this.inlineDepth > 0;
    }

    public boolean isInParagraph()
    {
        return this.isInParagraph;
    }

    public boolean isInHeader()
    {
        return this.isInHeader;
    }

    public boolean isInTable()
    {
        return this.isInTable;
    }

    public boolean isInTableCell()
    {
        return this.isInTableCell;
    }

    public boolean isInDefinitionList()
    {
        return this.definitionListDepth > 0;
    }

    public boolean isInList()
    {
        return this.listDepth > 0;
    }

    public boolean isInListItem()
    {
        return this.listItemDepth > 0;
    }

    public boolean isInLink()
    {
        return this.linkDepth > 0;
    }

    public int getLinkDepth()
    {
        return this.linkDepth;
    }

    public boolean isInQuotation()
    {
        return this.quotationDepth > 0;
    }

    public int getQuotationDepth()
    {
        return this.quotationDepth;
    }

    public boolean isInQuotationLine()
    {
        return this.isInQuotationLine;
    }

    public int getDefinitionListDepth()
    {
        return this.definitionListDepth;
    }

    // Events

    public void beginDefinitionDescription()
    {
        ++this.inlineDepth;
    }

    public void beginDefinitionList()
    {
        ++this.definitionListDepth;
    }

    public void beginDefinitionTerm()
    {
        ++this.inlineDepth;
    }

    public void beginDocument()
    {
        ++this.documentDepth;
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
        ++this.linkDepth;
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        ++this.listDepth;
    }

    public void beginListItem()
    {
        ++this.listItemDepth;
        ++this.inlineDepth;
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        // Nothing to do
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.isInParagraph = true;
        ++this.inlineDepth;
    }

    public void beginQuotation(Map<String, String> parameters)
    {
        ++this.quotationDepth;
    }

    public void beginQuotationLine()
    {
        this.isInQuotationLine = true;
        ++this.inlineDepth;
    }

    public void beginSection(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.isInHeader = true;
        ++this.inlineDepth;
    }

    public void beginTable(Map<String, String> parameters)
    {
        this.isInTable = true;
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        this.isInTableCell = true;
        ++this.inlineDepth;
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.isInTableCell = true;
        ++this.inlineDepth;
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
        --this.inlineDepth;
        this.previousEvent = Event.DEFINITION_DESCRIPTION;
    }

    public void endDefinitionList()
    {
        --this.definitionListDepth;
        this.previousEvent = Event.DEFINITION_LIST;
    }

    public void endDefinitionTerm()
    {
        --this.inlineDepth;
        this.previousEvent = Event.DEFINITION_TERM;
    }

    public void endDocument()
    {
        --this.documentDepth;
        this.previousEvent = Event.DOCUMENT;
    }

    public void endError(String message, String description)
    {
        this.previousEvent = Event.ERROR;
    }

    public void endFormat(Format format, Map<String, String> parameters)
    {
        this.previousEvent = Event.FORMAT;
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        --this.linkDepth;
        this.previousEvent = Event.LINK;
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        --this.listDepth;
        this.previousEvent = Event.LIST;
    }

    public void endListItem()
    {
        --this.listItemDepth;
        --this.inlineDepth;
        this.previousEvent = Event.LIST_ITEM;
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        this.previousEvent = Event.MACRO_MARKER;
    }

    public void endParagraph(Map<String, String> parameters)
    {
        this.isInParagraph = false;
        --this.inlineDepth;
        this.previousEvent = Event.PARAGRAPH;
    }

    public void endQuotation(Map<String, String> parameters)
    {
        --this.quotationDepth;
        this.previousEvent = Event.QUOTATION;
    }

    public void endQuotationLine()
    {
        this.isInQuotationLine = false;
        --this.inlineDepth;
        this.previousEvent = Event.QUOTATION_LINE;
    }

    public void endSection(Map<String, String> parameters)
    {
        this.previousEvent = Event.SECTION;
    }

    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.isInHeader = false;
        --this.inlineDepth;
        this.previousEvent = Event.HEADER;
    }

    public void endTable(Map<String, String> parameters)
    {
        this.isInTable = false;
        this.previousEvent = Event.TABLE;
    }

    public void endTableCell(Map<String, String> parameters)
    {
        this.isInTableCell = false;
        --this.inlineDepth;
        this.previousEvent = Event.TABLE_CELL;
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.isInTableCell = false;
        --this.inlineDepth;
        this.previousEvent = Event.TABLE_HEAD_CELL;
    }

    public void endTableRow(Map<String, String> parameters)
    {
        this.previousEvent = Event.TABLE_ROW;
    }

    public void endXMLNode(XMLNode node)
    {
        this.previousEvent = Event.XML_NODE;
    }

    public void onEmptyLines(int count)
    {
        this.previousEvent = Event.EMPTY_LINES;
    }

    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.previousEvent = Event.HORIZONTAL_LINE;
    }

    public void onId(String name)
    {
        this.previousEvent = Event.ID;
    }

    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.previousEvent = Event.IMAGE;
    }

    public void onNewLine()
    {
        this.previousEvent = Event.NEW_LINE;
    }

    public void onSpace()
    {
        this.previousEvent = Event.SPACE;
    }

    public void onSpecialSymbol(char symbol)
    {
        this.previousEvent = Event.SPECIAL_SYMBOL;
    }

    public void onVerbatim(String protectedString, Map<String, String> parameters, boolean isInline)
    {
        this.previousEvent = Event.VERBATIM_STANDALONE;
    }

    public void onWord(String word)
    {
        this.previousEvent = Event.WORD;
    }

    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        this.previousEvent = Event.MACRO;
    }
}
