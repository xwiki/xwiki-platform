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
 * Indicates block element for which we are inside and previous blocks.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class BlockStateChainingListener extends AbstractChainingListener implements StackableChainingListener
{
    public enum Event
    {
        NONE,
        DEFINITION_DESCRIPTION,
        DEFINITION_TERM,
        DEFINITION_LIST,
        DOCUMENT,
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

    private int inlineDepth;

    private boolean isInParagraph;

    private boolean isInHeader;

    private int linkDepth;

    private boolean isInTable;

    private boolean isInTableCell;

    private int definitionListDepth;

    private int definitionDescriptionDepth;

    private int definitionListItemIndex = -1;

    private int listDepth;

    private int listItemDepth;

    private int listItemIndex = -1;

    private int quotationDepth;

    private int quotationLineDepth;

    private int quotationLineIndex = -1;

    private int macroDepth;

    private int cellRow = -1;

    private int cellCol = -1;

    public BlockStateChainingListener(ListenerChain listenerChain)
    {
        super(listenerChain);
    }

    /**
     * {@inheritDoc}
     * 
     * @see StackableChainingListener#createChainingListenerInstance()
     */
    public StackableChainingListener createChainingListenerInstance()
    {
        return new BlockStateChainingListener(getListenerChain());
    }

    public Event getPreviousEvent()
    {
        return this.previousEvent;
    }

    public int getInlineDepth()
    {
        return this.inlineDepth;
    }

    public boolean isInLine()
    {
        return getInlineDepth() > 0;
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

    public int getCellCol()
    {
        return this.cellCol;
    }

    public int getCellRow()
    {
        return this.cellRow;
    }

    public int getDefinitionListDepth()
    {
        return this.definitionListDepth;
    }

    public boolean isInDefinitionList()
    {
        return getDefinitionListDepth() > 0;
    }

    public int getDefinitionDescriptionDepth()
    {
        return this.definitionDescriptionDepth;
    }

    public boolean isInDefinitionDescription()
    {
        return getDefinitionDescriptionDepth() > 0;
    }

    public int getDefinitionListItemIndex()
    {
        return definitionListItemIndex;
    }

    public int getListDepth()
    {
        return this.listDepth;
    }

    public boolean isInList()
    {
        return getListDepth() > 0;
    }

    public int getListItemDepth()
    {
        return this.listItemDepth;
    }

    public boolean isInListItem()
    {
        return getListItemDepth() > 0;
    }

    public int getListItemIndex()
    {
        return this.listItemIndex;
    }

    public int getLinkDepth()
    {
        return this.linkDepth;
    }

    public boolean isInLink()
    {
        return getLinkDepth() > 0;
    }

    public int getQuotationDepth()
    {
        return this.quotationDepth;
    }

    public boolean isInQuotation()
    {
        return getQuotationDepth() > 0;
    }

    public int getQuotationLineDepth()
    {
        return this.quotationLineDepth;
    }

    public boolean isInQuotationLine()
    {
        return getQuotationLineDepth() > 0;
    }

    public int getQuotationLineIndex()
    {
        return this.quotationLineIndex;
    }

    public int getMacroDepth()
    {
        return this.macroDepth;
    }

    public boolean isInMacro()
    {
        return getMacroDepth() < 0;
    }

    // Events

    public void beginDefinitionDescription()
    {
        ++this.inlineDepth;
        ++this.definitionDescriptionDepth;
        ++this.definitionListItemIndex;

        super.beginDefinitionDescription();
    }

    public void beginDefinitionList()
    {
        ++this.definitionListDepth;

        super.beginDefinitionList();
    }

    public void beginDefinitionTerm()
    {
        ++this.inlineDepth;
        ++this.definitionListItemIndex;

        super.beginDefinitionTerm();
    }

    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        ++this.linkDepth;

        super.beginLink(link, isFreeStandingURI, parameters);
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        ++this.listDepth;

        super.beginList(listType, parameters);
    }

    public void beginListItem()
    {
        ++this.listItemDepth;
        ++this.inlineDepth;
        ++this.listItemIndex;

        super.beginListItem();
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        ++this.macroDepth;

        super.beginMacroMarker(name, parameters, content, isInline);
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.isInParagraph = true;
        ++this.inlineDepth;

        super.beginParagraph(parameters);
    }

    public void beginQuotation(Map<String, String> parameters)
    {
        ++this.quotationDepth;

        super.beginQuotation(parameters);
    }

    public void beginQuotationLine()
    {
        ++this.quotationLineDepth;
        ++this.inlineDepth;
        ++this.quotationLineIndex;

        super.beginQuotationLine();
    }

    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        this.isInHeader = true;
        ++this.inlineDepth;

        super.beginHeader(level, id, parameters);
    }

    public void beginTable(Map<String, String> parameters)
    {
        this.isInTable = true;

        super.beginTable(parameters);
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        ++this.cellRow;

        super.beginTableRow(parameters);
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        this.isInTableCell = true;
        ++this.inlineDepth;
        ++this.cellCol;

        super.beginTableCell(parameters);
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.isInTableCell = true;
        ++this.inlineDepth;
        ++this.cellCol;

        super.beginTableHeadCell(parameters);
    }

    public void endDefinitionDescription()
    {
        super.endDefinitionDescription();

        --this.inlineDepth;
        --this.definitionDescriptionDepth;
        this.previousEvent = Event.DEFINITION_DESCRIPTION;
    }

    public void endDefinitionList()
    {
        super.endDefinitionList();

        --this.definitionListDepth;
        if (this.definitionListDepth == 0) {
            this.definitionListItemIndex = -1;
        }
        this.previousEvent = Event.DEFINITION_LIST;
    }

    public void endDefinitionTerm()
    {
        super.endDefinitionTerm();

        --this.inlineDepth;
        this.previousEvent = Event.DEFINITION_TERM;
    }

    public void endDocument()
    {
        this.previousEvent = Event.DOCUMENT;

        super.endDocument();
    }

    public void endFormat(Format format, Map<String, String> parameters)
    {
        super.endFormat(format, parameters);

        this.previousEvent = Event.FORMAT;
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        super.endLink(link, isFreeStandingURI, parameters);

        --this.linkDepth;
        this.previousEvent = Event.LINK;
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        super.endList(listType, parameters);

        --this.listDepth;
        if (this.listDepth == 0) {
            this.listItemIndex = -1;
        }
        this.previousEvent = Event.LIST;
    }

    public void endListItem()
    {
        super.endListItem();

        --this.listItemDepth;
        --this.inlineDepth;
        this.previousEvent = Event.LIST_ITEM;
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        super.endMacroMarker(name, parameters, content, isInline);

        this.previousEvent = Event.MACRO_MARKER;
        --this.macroDepth;
    }

    public void endParagraph(Map<String, String> parameters)
    {
        super.endParagraph(parameters);

        this.isInParagraph = false;
        --this.inlineDepth;
        this.previousEvent = Event.PARAGRAPH;
    }

    public void endQuotation(Map<String, String> parameters)
    {
        super.endQuotation(parameters);

        --this.quotationDepth;
        if (this.quotationDepth == 0) {
            this.quotationLineIndex = -1;
        }
        this.previousEvent = Event.QUOTATION;
    }

    public void endQuotationLine()
    {
        super.endQuotationLine();

        --this.quotationLineDepth;
        --this.inlineDepth;
        this.previousEvent = Event.QUOTATION_LINE;
    }

    public void endSection(Map<String, String> parameters)
    {
        super.endSection(parameters);

        this.previousEvent = Event.SECTION;
    }

    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        super.endHeader(level, id, parameters);

        this.isInHeader = false;
        --this.inlineDepth;
        this.previousEvent = Event.HEADER;
    }

    public void endTable(Map<String, String> parameters)
    {
        super.endTable(parameters);

        this.isInTable = false;
        this.cellRow = -1;
        this.previousEvent = Event.TABLE;
    }

    public void endTableCell(Map<String, String> parameters)
    {
        super.endTableCell(parameters);

        this.isInTableCell = false;
        --this.inlineDepth;
        this.previousEvent = Event.TABLE_CELL;
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        super.endTableHeadCell(parameters);

        this.isInTableCell = false;
        --this.inlineDepth;
        this.previousEvent = Event.TABLE_HEAD_CELL;
    }

    public void endTableRow(Map<String, String> parameters)
    {
        super.endTableRow(parameters);

        this.previousEvent = Event.TABLE_ROW;
        this.cellCol = -1;
    }

    public void endXMLNode(XMLNode node)
    {
        super.endXMLNode(node);

        this.previousEvent = Event.XML_NODE;
    }

    public void onEmptyLines(int count)
    {
        this.previousEvent = Event.EMPTY_LINES;

        super.onEmptyLines(count);
    }

    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.previousEvent = Event.HORIZONTAL_LINE;

        super.onHorizontalLine(parameters);
    }

    public void onId(String name)
    {
        this.previousEvent = Event.ID;

        super.onId(name);
    }

    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.previousEvent = Event.IMAGE;

        super.onImage(image, isFreeStandingURI, parameters);
    }

    public void onNewLine()
    {
        this.previousEvent = Event.NEW_LINE;

        super.onNewLine();
    }

    public void onSpace()
    {
        this.previousEvent = Event.SPACE;

        super.onSpace();
    }

    public void onSpecialSymbol(char symbol)
    {
        this.previousEvent = Event.SPECIAL_SYMBOL;

        super.onSpecialSymbol(symbol);
    }

    public void onVerbatim(String protectedString, Map<String, String> parameters, boolean isInline)
    {
        this.previousEvent = Event.VERBATIM_STANDALONE;

        super.onVerbatim(protectedString, parameters, isInline);
    }

    public void onWord(String word)
    {
        this.previousEvent = Event.WORD;

        super.onWord(word);
    }

    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        this.previousEvent = Event.MACRO;

        super.onMacro(name, parameters, content, isInline);
    }
}
