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
import java.util.Stack;

import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.parser.Syntax;

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
        RAW_TEXT,
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

    private Stack<DefinitionListState> definitionListDepth = new Stack<DefinitionListState>();

    private Stack<ListState> listDepth = new Stack<ListState>();

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
        return this.definitionListDepth.size();
    }

    public boolean isInDefinitionList()
    {
        return getDefinitionListDepth() > 0;
    }

    public int getDefinitionListItemIndex()
    {
        return isInDefinitionList() ? this.definitionListDepth.peek().definitionListItemIndex : -1;
    }

    public int getListDepth()
    {
        return this.listDepth.size();
    }

    public boolean isInList()
    {
        return getListDepth() > 0;
    }

    public int getListItemIndex()
    {
        return isInList() ? this.listDepth.peek().listItemIndex : -1;
    }

    public void pushLinkDepth()
    {
        ++this.linkDepth;
    }

    public void popLinkDepth()
    {
        --this.linkDepth;
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
        return getMacroDepth() > 0;
    }

    // Events

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginDefinitionDescription()
     */
    @Override
    public void beginDefinitionDescription()
    {
        ++this.inlineDepth;
        ++this.definitionListDepth.peek().definitionListItemIndex;

        super.beginDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginDefinitionList()
     */
    @Override
    public void beginDefinitionList()
    {
        this.definitionListDepth.push(new DefinitionListState());

        super.beginDefinitionList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginDefinitionTerm()
     */
    @Override
    public void beginDefinitionTerm()
    {
        ++this.inlineDepth;
        ++this.definitionListDepth.peek().definitionListItemIndex;

        super.beginDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginLink(org.xwiki.rendering.listener.Link,
     *      boolean, java.util.Map)
     */
    @Override
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        ++this.linkDepth;

        super.beginLink(link, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginList(org.xwiki.rendering.listener.ListType,
     *      java.util.Map)
     */
    @Override
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        this.listDepth.push(new ListState());

        super.beginList(listType, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginListItem()
     */
    @Override
    public void beginListItem()
    {
        ++this.inlineDepth;
        ++this.listDepth.peek().listItemIndex;

        super.beginListItem();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginMacroMarker(java.lang.String,
     *      java.util.Map, java.lang.String, boolean)
     */
    @Override
    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        ++this.macroDepth;

        super.beginMacroMarker(name, parameters, content, isInline);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginParagraph(java.util.Map)
     */
    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        this.isInParagraph = true;
        ++this.inlineDepth;

        super.beginParagraph(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginQuotation(java.util.Map)
     */
    @Override
    public void beginQuotation(Map<String, String> parameters)
    {
        ++this.quotationDepth;

        super.beginQuotation(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginQuotationLine()
     */
    @Override
    public void beginQuotationLine()
    {
        ++this.quotationLineDepth;
        ++this.inlineDepth;
        ++this.quotationLineIndex;

        super.beginQuotationLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginHeader(org.xwiki.rendering.listener.HeaderLevel,
     *      java.lang.String, java.util.Map)
     */
    @Override
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        this.isInHeader = true;
        ++this.inlineDepth;

        super.beginHeader(level, id, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginTable(java.util.Map)
     */
    @Override
    public void beginTable(Map<String, String> parameters)
    {
        this.isInTable = true;

        super.beginTable(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginTableRow(java.util.Map)
     */
    @Override
    public void beginTableRow(Map<String, String> parameters)
    {
        ++this.cellRow;

        super.beginTableRow(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginTableCell(java.util.Map)
     */
    @Override
    public void beginTableCell(Map<String, String> parameters)
    {
        this.isInTableCell = true;
        ++this.inlineDepth;
        ++this.cellCol;

        super.beginTableCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginTableHeadCell(java.util.Map)
     */
    @Override
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.isInTableCell = true;
        ++this.inlineDepth;
        ++this.cellCol;

        super.beginTableHeadCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDefinitionDescription()
     */
    @Override
    public void endDefinitionDescription()
    {
        super.endDefinitionDescription();

        --this.inlineDepth;
        this.previousEvent = Event.DEFINITION_DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDefinitionList()
     */
    @Override
    public void endDefinitionList()
    {
        super.endDefinitionList();

        this.definitionListDepth.pop();

        this.previousEvent = Event.DEFINITION_LIST;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDefinitionTerm()
     */
    @Override
    public void endDefinitionTerm()
    {
        super.endDefinitionTerm();

        --this.inlineDepth;
        this.previousEvent = Event.DEFINITION_TERM;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDocument(java.util.Map)
     */
    @Override
    public void endDocument(Map<String, String> parameters)
    {
        this.previousEvent = Event.DOCUMENT;

        super.endDocument(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endFormat(org.xwiki.rendering.listener.Format,
     *      java.util.Map)
     */
    @Override
    public void endFormat(Format format, Map<String, String> parameters)
    {
        super.endFormat(format, parameters);

        this.previousEvent = Event.FORMAT;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endLink(org.xwiki.rendering.listener.Link,
     *      boolean, java.util.Map)
     */
    @Override
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        super.endLink(link, isFreeStandingURI, parameters);

        --this.linkDepth;
        this.previousEvent = Event.LINK;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endList(org.xwiki.rendering.listener.ListType,
     *      java.util.Map)
     */
    @Override
    public void endList(ListType listType, Map<String, String> parameters)
    {
        super.endList(listType, parameters);

        this.listDepth.pop();

        this.previousEvent = Event.LIST;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endListItem()
     */
    @Override
    public void endListItem()
    {
        super.endListItem();

        --this.inlineDepth;
        this.previousEvent = Event.LIST_ITEM;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endMacroMarker(java.lang.String,
     *      java.util.Map, java.lang.String, boolean)
     */
    @Override
    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        super.endMacroMarker(name, parameters, content, isInline);

        this.previousEvent = Event.MACRO_MARKER;
        --this.macroDepth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endParagraph(java.util.Map)
     */
    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        super.endParagraph(parameters);

        this.isInParagraph = false;
        --this.inlineDepth;
        this.previousEvent = Event.PARAGRAPH;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endQuotation(java.util.Map)
     */
    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        super.endQuotation(parameters);

        --this.quotationDepth;
        if (this.quotationDepth == 0) {
            this.quotationLineIndex = -1;
        }
        this.previousEvent = Event.QUOTATION;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endQuotationLine()
     */
    @Override
    public void endQuotationLine()
    {
        super.endQuotationLine();

        --this.quotationLineDepth;
        --this.inlineDepth;
        this.previousEvent = Event.QUOTATION_LINE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endSection(java.util.Map)
     */
    @Override
    public void endSection(Map<String, String> parameters)
    {
        super.endSection(parameters);

        this.previousEvent = Event.SECTION;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endHeader(org.xwiki.rendering.listener.HeaderLevel,
     *      java.lang.String, java.util.Map)
     */
    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        super.endHeader(level, id, parameters);

        this.isInHeader = false;
        --this.inlineDepth;
        this.previousEvent = Event.HEADER;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTable(java.util.Map)
     */
    @Override
    public void endTable(Map<String, String> parameters)
    {
        super.endTable(parameters);

        this.isInTable = false;
        this.cellRow = -1;
        this.previousEvent = Event.TABLE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTableCell(java.util.Map)
     */
    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        super.endTableCell(parameters);

        this.isInTableCell = false;
        --this.inlineDepth;
        this.previousEvent = Event.TABLE_CELL;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTableHeadCell(java.util.Map)
     */
    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        super.endTableHeadCell(parameters);

        this.isInTableCell = false;
        --this.inlineDepth;
        this.previousEvent = Event.TABLE_HEAD_CELL;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTableRow(java.util.Map)
     */
    @Override
    public void endTableRow(Map<String, String> parameters)
    {
        super.endTableRow(parameters);

        this.previousEvent = Event.TABLE_ROW;
        this.cellCol = -1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onRawText(String, Syntax)
     */
    @Override
    public void onRawText(String text, Syntax syntax)
    {
        super.onRawText(text, syntax);

        this.previousEvent = Event.RAW_TEXT;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onEmptyLines(int)
     */
    @Override
    public void onEmptyLines(int count)
    {
        this.previousEvent = Event.EMPTY_LINES;

        super.onEmptyLines(count);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onHorizontalLine(java.util.Map)
     */
    @Override
    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.previousEvent = Event.HORIZONTAL_LINE;

        super.onHorizontalLine(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onId(java.lang.String)
     */
    @Override
    public void onId(String name)
    {
        this.previousEvent = Event.ID;

        super.onId(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onImage(org.xwiki.rendering.listener.Image,
     *      boolean, java.util.Map)
     */
    @Override
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.previousEvent = Event.IMAGE;

        super.onImage(image, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onNewLine()
     */
    @Override
    public void onNewLine()
    {
        this.previousEvent = Event.NEW_LINE;

        super.onNewLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onSpace()
     */
    @Override
    public void onSpace()
    {
        this.previousEvent = Event.SPACE;

        super.onSpace();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onSpecialSymbol(char)
     */
    @Override
    public void onSpecialSymbol(char symbol)
    {
        this.previousEvent = Event.SPECIAL_SYMBOL;

        super.onSpecialSymbol(symbol);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onVerbatim(String, boolean, Map)
     */
    @Override
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        this.previousEvent = Event.VERBATIM_STANDALONE;

        super.onVerbatim(protectedString, isInline, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onWord(java.lang.String)
     */
    @Override
    public void onWord(String word)
    {
        this.previousEvent = Event.WORD;

        super.onWord(word);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onMacro(java.lang.String, java.util.Map,
     *      java.lang.String, boolean)
     */
    @Override
    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        this.previousEvent = Event.MACRO;

        super.onMacro(name, parameters, content, isInline);
    }

    private static class ListState
    {
        public int listItemIndex = -1;
    }

    private static class DefinitionListState
    {
        public int definitionListItemIndex = -1;
    }
}
