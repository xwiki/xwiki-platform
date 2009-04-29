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
import org.xwiki.rendering.parser.Syntax;

/**
 * Default and basic implementation of a chaining listener that knows how to delegate event calls to the next listener
 * in the chain.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public abstract class AbstractChainingListener implements ChainingListener
{
    /**
     * The chain to use to know the next listener to call on events.
     */
    private ListenerChain listenerChain;

    /**
     * @param listenerChain the chain to use to know the next listener to call on events
     */
    public AbstractChainingListener(ListenerChain listenerChain)
    {
        this.listenerChain = listenerChain;
        this.listenerChain.addListener(this);
    }

    /**
     * @return the listener chain used to know the next listener to call on events
     */
    public ListenerChain getListenerChain()
    {
        return this.listenerChain;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginDefinitionDescription()
     */
    public void beginDefinitionDescription()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginDefinitionDescription();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginDefinitionList()
     */
    public void beginDefinitionList()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginDefinitionList();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginDefinitionTerm()
     */
    public void beginDefinitionTerm()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginDefinitionTerm();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDocument(java.util.Map)
     */
    public void beginDocument(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginDocument(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginGroup(Map)
     */
    public void beginGroup(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginGroup(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginFormat(Format, Map)
     */
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginFormat(format, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginHeader(HeaderLevel, String, Map)
     */
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginHeader(level, id, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginLink(Link, boolean, Map)
     */
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginLink(link, isFreeStandingURI, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginList(ListType, Map)
     */
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginList(listType, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginListItem()
     */
    public void beginListItem()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginListItem();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginMacroMarker(String, Map, String, boolean)
     */
    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginMacroMarker(name, parameters, content, isInline);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginParagraph(Map)
     */
    public void beginParagraph(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginParagraph(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginQuotation(Map)
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginQuotation(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginQuotationLine()
     */
    public void beginQuotationLine()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginQuotationLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginSection(Map)
     */
    public void beginSection(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginSection(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginTable(Map)
     */
    public void beginTable(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginTable(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginTableCell(Map)
     */
    public void beginTableCell(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginTableCell(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginTableHeadCell(Map)
     */
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginTableHeadCell(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#beginTableRow(Map)
     */
    public void beginTableRow(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.beginTableRow(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endDefinitionDescription()
     */
    public void endDefinitionDescription()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endDefinitionDescription();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endDefinitionList()
     */
    public void endDefinitionList()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endDefinitionList();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endDefinitionTerm()
     */
    public void endDefinitionTerm()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endDefinitionTerm();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDocument(java.util.Map)
     */
    public void endDocument(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endDocument(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endGroup(Map)
     */
    public void endGroup(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endGroup(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endFormat(Format, Map)
     */
    public void endFormat(Format format, Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endFormat(format, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endHeader(HeaderLevel, String, Map)
     */
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endHeader(level, id, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endLink(Link, boolean, Map)
     */
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endLink(link, isFreeStandingURI, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endList(ListType, Map)
     */
    public void endList(ListType listType, Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endList(listType, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endListItem()
     */
    public void endListItem()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endListItem();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endMacroMarker(String, Map, String, boolean)
     */
    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endMacroMarker(name, parameters, content, isInline);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endParagraph(Map)
     */
    public void endParagraph(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endParagraph(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endQuotation(Map)
     */
    public void endQuotation(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endQuotation(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endQuotationLine()
     */
    public void endQuotationLine()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endQuotationLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endSection(Map)
     */
    public void endSection(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endSection(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endTable(Map)
     */
    public void endTable(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endTable(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endTableCell(Map)
     */
    public void endTableCell(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endTableCell(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endTableHeadCell(Map)
     */
    public void endTableHeadCell(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endTableHeadCell(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#endTableRow(Map)
     */
    public void endTableRow(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.endTableRow(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.onEmptyLines(count);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#onHorizontalLine(Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.onHorizontalLine(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#onId(String)
     */
    public void onId(String name)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.onId(name);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#onImage(Image, boolean, Map)
     */
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.onImage(image, isFreeStandingURI, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#onMacro(String, Map, String, boolean)
     */
    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.onMacro(name, parameters, content, isInline);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#onNewLine()
     */
    public void onNewLine()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.onNewLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#onSpace()
     */
    public void onSpace()
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.onSpace();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#onSpecialSymbol(char)
     */
    public void onSpecialSymbol(char symbol)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.onSpecialSymbol(symbol);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#onVerbatim(String, boolean, Map)
     */
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.onVerbatim(protectedString, isInline, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#onWord(String)
     */
    public void onWord(String word)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.onWord(word);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChainingListener#onRawText(String, Syntax)
     */
    public void onRawText(String text, Syntax syntax)
    {
        ChainingListener next = getListenerChain().getNextListener(getClass());
        if (next != null) {
            next.onRawText(text, syntax);
        }
    }
}
