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
 * Stores events without emitting them back in order to accumulate them and to provide a lookahead feature. 
 * The lookahead depth is configurable.
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
    
    public void beginDefinitionDescription()
    {
        saveEvent(EventType.BEGIN_DEFINITION_DESCRIPTION);
        firePreviousEvent();
    }

    public void beginDefinitionList()
    {
        saveEvent(EventType.BEGIN_DEFINITION_LIST);
        firePreviousEvent();
    }

    public void beginDefinitionTerm()
    {
        saveEvent(EventType.BEGIN_DEFINITION_TERM);
        firePreviousEvent();
    }

    public void beginDocument()
    {
        saveEvent(EventType.BEGIN_DOCUMENT);
        flush();
    }

    public void beginFormat(Format format, Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_FORMAT, format, parameters);
        firePreviousEvent();
    }

    public void beginHeader(HeaderLevel level, Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_HEADER, level, parameters);
        firePreviousEvent();
    }

    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_LINK, link, isFreeStandingURI, parameters);
        firePreviousEvent();
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_LIST, listType, parameters);
        firePreviousEvent();
    }

    public void beginListItem()
    {
        saveEvent(EventType.BEGIN_LIST_ITEM);
        firePreviousEvent();
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        saveEvent(EventType.BEGIN_MACRO_MARKER, name, parameters, content, isInline);
        firePreviousEvent();
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_PARAGRAPH, parameters);
        firePreviousEvent();
    }

    public void beginQuotation(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_QUOTATION, parameters);
        firePreviousEvent();
    }

    public void beginQuotationLine()
    {
        saveEvent(EventType.BEGIN_QUOTATION_LINE);
        firePreviousEvent();
    }

    public void beginSection(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_SECTION, parameters);
        firePreviousEvent();
    }

    public void beginTable(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE, parameters);
        firePreviousEvent();
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE_CELL, parameters);
        firePreviousEvent();
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE_HEAD_CELL, parameters);
        firePreviousEvent();
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE_ROW, parameters);
        firePreviousEvent();
    }

    public void beginXMLNode(XMLNode node)
    {
        saveEvent(EventType.BEGIN_XML_NODE, node);
        firePreviousEvent();
    }

    public void endDefinitionDescription()
    {
        saveEvent(EventType.END_DEFINITION_DESCRIPTION);
        firePreviousEvent();
    }

    public void endDefinitionList()
    {
        saveEvent(EventType.END_DEFINITION_LIST);
        firePreviousEvent();
    }

    public void endDefinitionTerm()
    {
        saveEvent(EventType.END_DEFINITION_TERM);
        firePreviousEvent();
    }

    public void endDocument()
    {
        saveEvent(EventType.END_DOCUMENT);
        flush();
    }

    public void endFormat(Format format, Map<String, String> parameters)
    {
        saveEvent(EventType.END_FORMAT, format, parameters);
        firePreviousEvent();
    }

    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        saveEvent(EventType.END_HEADER, level, parameters);
        firePreviousEvent();
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        saveEvent(EventType.END_LINK, link, isFreeStandingURI, parameters);
        firePreviousEvent();
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        saveEvent(EventType.END_LIST, listType, parameters);
        firePreviousEvent();
    }

    public void endListItem()
    {
        saveEvent(EventType.END_LIST_ITEM);
        firePreviousEvent();
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        saveEvent(EventType.END_MACRO_MARKER, name, parameters, content, isInline);
        firePreviousEvent();
    }

    public void endParagraph(Map<String, String> parameters)
    {
        saveEvent(EventType.END_PARAGRAPH, parameters);
        firePreviousEvent();
    }

    public void endQuotation(Map<String, String> parameters)
    {
        saveEvent(EventType.END_QUOTATION, parameters);
        firePreviousEvent();
    }

    public void endQuotationLine()
    {
        saveEvent(EventType.END_QUOTATION_LINE);
        firePreviousEvent();
    }

    public void endSection(Map<String, String> parameters)
    {
        saveEvent(EventType.END_SECTION, parameters);
        firePreviousEvent();
    }

    public void endTable(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE, parameters);
        firePreviousEvent();
    }

    public void endTableCell(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE_CELL, parameters);
        firePreviousEvent();
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE_HEAD_CELL, parameters);
        firePreviousEvent();
    }

    public void endTableRow(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE_ROW, parameters);
        firePreviousEvent();
    }

    public void endXMLNode(XMLNode node)
    {
        saveEvent(EventType.END_XML_NODE, node);
        firePreviousEvent();
    }

    public void onEmptyLines(int count)
    {
        saveEvent(EventType.ON_EMPTY_LINES, count);
        firePreviousEvent();
    }

    public void onHorizontalLine(Map<String, String> parameters)
    {
        saveEvent(EventType.ON_HORIZONTAL_LINE, parameters);
        firePreviousEvent();
    }

    public void onId(String name)
    {
        saveEvent(EventType.ON_ID, name);
        firePreviousEvent();
    }

    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        saveEvent(EventType.ON_IMAGE, image, isFreeStandingURI, parameters);
        firePreviousEvent();
    }

    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        saveEvent(EventType.ON_MACRO, name, parameters, content, isInline);
        firePreviousEvent();
    }

    public void onNewLine()
    {
        saveEvent(EventType.ON_NEW_LINE);
        firePreviousEvent();
    }

    public void onSpace()
    {
        saveEvent(EventType.ON_SPACE);
        firePreviousEvent();
    }

    public void onSpecialSymbol(char symbol)
    {
        saveEvent(EventType.ON_SPECIAL_SYMBOL, symbol);
        firePreviousEvent();
    }

    public void onVerbatim(String protectedString, Map<String, String> parameters, boolean isInline)
    {
        saveEvent(EventType.ON_VERBATIM, protectedString, parameters, isInline);
        firePreviousEvent();
    }

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
    
    private void saveEvent(EventType eventType, Object...objects)
    {
        this.previousEvents.offer(new Event(eventType, objects));
    }
}
