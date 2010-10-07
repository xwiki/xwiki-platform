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
package org.xwiki.rendering.listener;

import java.util.LinkedList;
import java.util.Map;

import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Manage a {@link java.util.Queue} of events.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class QueueListener extends LinkedList<QueueListener.Event> implements Listener
{
    /**
     * Class ID for serialization.
     */
    private static final long serialVersionUID = 2869508092440006345L;

    /**
     * An event.
     * 
     * @version $Id$
     */
    public class Event
    {
        /**
         * The type of the event.
         */
        public EventType eventType;

        /**
         * The parameters of the event.
         */
        public Object[] eventParameters;

        /**
         * @param eventType the type of the event
         * @param eventParameters the parameters of the event
         */
        public Event(EventType eventType, Object[] eventParameters)
        {
            this.eventType = eventType;
            this.eventParameters = eventParameters;
        }
    }

    /**
     * Returns the event at the specified position in this queue.
     * 
     * @param depth index of event to return.
     * @return the evnet at the specified position in this queue.
     */
    public Event getEvent(int depth)
    {
        Event event = null;

        if (depth > 0 && size() > depth - 1) {
            event = get(depth - 1);
        }

        return event;
    }

    /**
     * Send all stored events to provided {@link Listener}.
     * 
     * @param listener the {@link Listener} on which to send events
     */
    public void consumeEvents(Listener listener)
    {
        while (!isEmpty()) {
            Event event = remove();
            event.eventType.fireEvent(listener, event.eventParameters);
        }
    }

    /**
     * Store provided event.
     * 
     * @param eventType the type of the event
     * @param objects the parameters of the event
     */
    private void saveEvent(EventType eventType, Object... objects)
    {
        offer(new Event(eventType, objects));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     */
    public void beginDefinitionDescription()
    {
        saveEvent(EventType.BEGIN_DEFINITION_DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList(java.util.Map)
     * @since 2.0RC1
     */
    public void beginDefinitionList(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_DEFINITION_LIST, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     */
    public void beginDefinitionTerm()
    {
        saveEvent(EventType.BEGIN_DEFINITION_TERM);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDocument(java.util.Map)
     */
    public void beginDocument(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_DOCUMENT, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginGroup(Map)
     */
    public void beginGroup(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_GROUP, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginFormat(org.xwiki.rendering.listener.Format, java.util.Map)
     */
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_FORMAT, format, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginHeader(org.xwiki.rendering.listener.HeaderLevel,
     *      java.lang.String, java.util.Map)
     */
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_HEADER, level, id, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginLink(ResourceReference , boolean, java.util.Map)
     * @since 2.5RC1
     */
    public void beginLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_LINK, reference, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_LIST, listType, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginListItem()
     */
    public void beginListItem()
    {
        saveEvent(EventType.BEGIN_LIST_ITEM);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginMacroMarker(java.lang.String, java.util.Map, java.lang.String,
     *      boolean)
     */
    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        saveEvent(EventType.BEGIN_MACRO_MARKER, name, parameters, content, isInline);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginParagraph(java.util.Map)
     */
    public void beginParagraph(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_PARAGRAPH, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_QUOTATION, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     */
    public void beginQuotationLine()
    {
        saveEvent(EventType.BEGIN_QUOTATION_LINE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginSection(java.util.Map)
     */
    public void beginSection(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_SECTION, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTable(java.util.Map)
     */
    public void beginTable(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableCell(java.util.Map)
     */
    public void beginTableCell(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE_CELL, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableHeadCell(java.util.Map)
     */
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE_HEAD_CELL, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableRow(java.util.Map)
     */
    public void beginTableRow(Map<String, String> parameters)
    {
        saveEvent(EventType.BEGIN_TABLE_ROW, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     */
    public void endDefinitionDescription()
    {
        saveEvent(EventType.END_DEFINITION_DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList(java.util.Map)
     * @since 2.0RC1
     */
    public void endDefinitionList(Map<String, String> parameters)
    {
        saveEvent(EventType.END_DEFINITION_LIST, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     */
    public void endDefinitionTerm()
    {
        saveEvent(EventType.END_DEFINITION_TERM);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDocument(java.util.Map)
     */
    public void endDocument(Map<String, String> parameters)
    {
        saveEvent(EventType.END_DOCUMENT, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endGroup(Map)
     */
    public void endGroup(Map<String, String> parameters)
    {
        saveEvent(EventType.END_GROUP, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endFormat(org.xwiki.rendering.listener.Format, java.util.Map)
     */
    public void endFormat(Format format, Map<String, String> parameters)
    {
        saveEvent(EventType.END_FORMAT, format, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endHeader(org.xwiki.rendering.listener.HeaderLevel, java.lang.String,
     *      java.util.Map)
     */
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        saveEvent(EventType.END_HEADER, level, id, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endLink(ResourceReference , boolean, java.util.Map)
     * @since 2.5RC1
     */
    public void endLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        saveEvent(EventType.END_LINK, reference, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void endList(ListType listType, Map<String, String> parameters)
    {
        saveEvent(EventType.END_LIST, listType, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endListItem()
     */
    public void endListItem()
    {
        saveEvent(EventType.END_LIST_ITEM);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endMacroMarker(java.lang.String, java.util.Map, java.lang.String,
     *      boolean)
     */
    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        saveEvent(EventType.END_MACRO_MARKER, name, parameters, content, isInline);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endParagraph(java.util.Map)
     */
    public void endParagraph(Map<String, String> parameters)
    {
        saveEvent(EventType.END_PARAGRAPH, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     */
    public void endQuotation(Map<String, String> parameters)
    {
        saveEvent(EventType.END_QUOTATION, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     */
    public void endQuotationLine()
    {
        saveEvent(EventType.END_QUOTATION_LINE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endSection(java.util.Map)
     */
    public void endSection(Map<String, String> parameters)
    {
        saveEvent(EventType.END_SECTION, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTable(java.util.Map)
     */
    public void endTable(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableCell(java.util.Map)
     */
    public void endTableCell(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE_CELL, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableHeadCell(java.util.Map)
     */
    public void endTableHeadCell(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE_HEAD_CELL, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableRow(java.util.Map)
     */
    public void endTableRow(Map<String, String> parameters)
    {
        saveEvent(EventType.END_TABLE_ROW, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onRawText(String, Syntax)
     */
    public void onRawText(String text, Syntax syntax)
    {
        saveEvent(EventType.ON_RAW_TEXT, text, syntax);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        saveEvent(EventType.ON_EMPTY_LINES, count);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine(java.util.Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        saveEvent(EventType.ON_HORIZONTAL_LINE, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onId(java.lang.String)
     */
    public void onId(String name)
    {
        saveEvent(EventType.ON_ID, name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onImage(org.xwiki.rendering.listener.Image, boolean, java.util.Map)
     */
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        saveEvent(EventType.ON_IMAGE, image, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onMacro(java.lang.String, java.util.Map, java.lang.String, boolean)
     */
    public void onMacro(String id, Map<String, String> parameters, String content, boolean isInline)
    {
        saveEvent(EventType.ON_MACRO, id, parameters, content, isInline);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onNewLine()
     */
    public void onNewLine()
    {
        saveEvent(EventType.ON_NEW_LINE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onSpace()
     */
    public void onSpace()
    {
        saveEvent(EventType.ON_SPACE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onSpecialSymbol(char)
     */
    public void onSpecialSymbol(char symbol)
    {
        saveEvent(EventType.ON_SPECIAL_SYMBOL, symbol);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatim(String, boolean, Map)
     */
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        saveEvent(EventType.ON_VERBATIM, protectedString, isInline, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onWord(java.lang.String)
     */
    public void onWord(String word)
    {
        saveEvent(EventType.ON_WORD, word);
    }
}
