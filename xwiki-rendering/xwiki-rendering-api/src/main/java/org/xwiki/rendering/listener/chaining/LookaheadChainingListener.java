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

import java.util.LinkedList;
import java.util.Map;

import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.xml.XMLNode;

/**
 * Stores events without emitting them back in order to accumulate them and to provide a lookahead feature. The
 * lookahead depth is configurable.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class LookaheadChainingListener extends AbstractChainingListener
{
    private LinkedList<Event> previousEvents = new LinkedList<Event>();

    private int lookaheadDepth;

    public class Event
    {
        public EventType eventType;

        public Object[] eventParameters;

        public Event(EventType eventType, Object[] eventParameters)
        {
            this.eventType = eventType;
            this.eventParameters = eventParameters;
        }
    }

    public LookaheadChainingListener(ListenerChain listenerChain, int lookaheadDepth)
    {
        super(listenerChain);
        this.lookaheadDepth = lookaheadDepth;
    }

    public Event getNextEvent()
    {
        return getNextEvent(1);
    }

    public Event getNextEvent(int depth)
    {
        Event event = null;
        if (depth > 0 && this.previousEvents.size() > depth - 1) {
            event = this.previousEvents.get(depth - 1);
        }
        return event;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginDefinitionDescription()
     */
    @Override
    public void beginDefinitionDescription()
    {
        saveEvent(EventType.BEGIN_DEFINITION_DESCRIPTION);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginDefinitionList()
     */
    @Override
    public void beginDefinitionList()
    {
        saveEvent(EventType.BEGIN_DEFINITION_LIST);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginDefinitionTerm()
     */
    @Override
    public void beginDefinitionTerm()
    {
        saveEvent(EventType.BEGIN_DEFINITION_TERM);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginDocument(java.util.Map)
     */
    @Override
    public void beginDocument(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_DOCUMENT, parameters);
        flush();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginFormat(org.xwiki.rendering.listener.Format,
     *      java.util.Map)
     */
    @Override
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_FORMAT, format, parameters);
        firePreviousEvent();
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
        saveEvent(EventType.BEGIN_HEADER, level, id, parameters);
        firePreviousEvent();
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
        saveEvent(EventType.BEGIN_LINK, link, isFreeStandingURI, parameters);
        firePreviousEvent();
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
        saveEvent(EventType.BEGIN_LIST, listType, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginListItem()
     */
    @Override
    public void beginListItem()
    {
        saveEvent(EventType.BEGIN_LIST_ITEM);
        firePreviousEvent();
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
        saveEvent(EventType.BEGIN_MACRO_MARKER, name, parameters, content, isInline);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginParagraph(java.util.Map)
     */
    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_PARAGRAPH, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginQuotation(java.util.Map)
     */
    @Override
    public void beginQuotation(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_QUOTATION, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginQuotationLine()
     */
    @Override
    public void beginQuotationLine()
    {
        saveEvent(EventType.BEGIN_QUOTATION_LINE);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginSection(java.util.Map)
     */
    @Override
    public void beginSection(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_SECTION, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginTable(java.util.Map)
     */
    @Override
    public void beginTable(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginTableCell(java.util.Map)
     */
    @Override
    public void beginTableCell(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE_CELL, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginTableHeadCell(java.util.Map)
     */
    @Override
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE_HEAD_CELL, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginTableRow(java.util.Map)
     */
    @Override
    public void beginTableRow(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE_ROW, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginXMLNode(org.xwiki.rendering.listener.xml.XMLNode)
     */
    @Override
    public void beginXMLNode(XMLNode node)
    {
        saveEvent(EventType.BEGIN_XML_NODE, node);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDefinitionDescription()
     */
    @Override
    public void endDefinitionDescription()
    {
        saveEvent(EventType.END_DEFINITION_DESCRIPTION);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDefinitionList()
     */
    @Override
    public void endDefinitionList()
    {
        saveEvent(EventType.END_DEFINITION_LIST);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDefinitionTerm()
     */
    @Override
    public void endDefinitionTerm()
    {
        saveEvent(EventType.END_DEFINITION_TERM);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDocument(java.util.Map)
     */
    @Override
    public void endDocument(Map<String, String> parameters)
    {
        saveEvent(EventType.END_DOCUMENT, parameters);
        flush();
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
        saveEvent(EventType.END_FORMAT, format, parameters);
        firePreviousEvent();
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
        saveEvent(EventType.END_HEADER, level, id, parameters);
        firePreviousEvent();
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
        saveEvent(EventType.END_LINK, link, isFreeStandingURI, parameters);
        firePreviousEvent();
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
        saveEvent(EventType.END_LIST, listType, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endListItem()
     */
    @Override
    public void endListItem()
    {
        saveEvent(EventType.END_LIST_ITEM);
        firePreviousEvent();
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
        saveEvent(EventType.END_MACRO_MARKER, name, parameters, content, isInline);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endParagraph(java.util.Map)
     */
    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        saveEvent(EventType.END_PARAGRAPH, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endQuotation(java.util.Map)
     */
    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        saveEvent(EventType.END_QUOTATION, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endQuotationLine()
     */
    @Override
    public void endQuotationLine()
    {
        saveEvent(EventType.END_QUOTATION_LINE);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endSection(java.util.Map)
     */
    @Override
    public void endSection(Map<String, String> parameters)
    {
        saveEvent(EventType.END_SECTION, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTable(java.util.Map)
     */
    @Override
    public void endTable(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTableCell(java.util.Map)
     */
    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE_CELL, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTableHeadCell(java.util.Map)
     */
    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE_HEAD_CELL, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTableRow(java.util.Map)
     */
    @Override
    public void endTableRow(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE_ROW, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endXMLNode(org.xwiki.rendering.listener.xml.XMLNode)
     */
    @Override
    public void endXMLNode(XMLNode node)
    {
        saveEvent(EventType.END_XML_NODE, node);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onEmptyLines(int)
     */
    @Override
    public void onEmptyLines(int count)
    {
        saveEvent(EventType.ON_EMPTY_LINES, count);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onHorizontalLine(java.util.Map)
     */
    @Override
    public void onHorizontalLine(Map<String, String> parameters)
    {
        saveEvent(EventType.ON_HORIZONTAL_LINE, parameters);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onId(java.lang.String)
     */
    @Override
    public void onId(String name)
    {
        saveEvent(EventType.ON_ID, name);
        firePreviousEvent();
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
        saveEvent(EventType.ON_IMAGE, image, isFreeStandingURI, parameters);
        firePreviousEvent();
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
        saveEvent(EventType.ON_MACRO, name, parameters, content, isInline);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onNewLine()
     */
    @Override
    public void onNewLine()
    {
        saveEvent(EventType.ON_NEW_LINE);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onSpace()
     */
    @Override
    public void onSpace()
    {
        saveEvent(EventType.ON_SPACE);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onSpecialSymbol(char)
     */
    @Override
    public void onSpecialSymbol(char symbol)
    {
        saveEvent(EventType.ON_SPECIAL_SYMBOL, symbol);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onVerbatim(java.lang.String, java.util.Map,
     *      boolean)
     */
    @Override
    public void onVerbatim(String protectedString, Map<String, String> parameters, boolean isInline)
    {
        saveEvent(EventType.ON_VERBATIM, protectedString, parameters, isInline);
        firePreviousEvent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onWord(java.lang.String)
     */
    @Override
    public void onWord(String word)
    {
        saveEvent(EventType.ON_WORD, word);
        firePreviousEvent();
    }

    private void firePreviousEvent()
    {
        if (this.previousEvents.size() > this.lookaheadDepth) {
            Event event = this.previousEvents.remove();
            event.eventType.fireEvent(getListenerChain().getNextListener(getClass()), event.eventParameters);
        }
    }

    private void flush()
    {
        // Ensure that all remaining events are flushed
        while (!this.previousEvents.isEmpty()) {
            Event event = this.previousEvents.remove();
            event.eventType.fireEvent(getListenerChain().getNextListener(getClass()), event.eventParameters);
        }
    }

    private void saveEvent(EventType eventType, Object... objects)
    {
        this.previousEvents.offer(new Event(eventType, objects));
    }
}
