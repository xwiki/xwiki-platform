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

import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.HeaderLevel;

import java.util.Map;
import java.util.Stack;

/**
 * Allow knowing if a container block (a block which can have children) has children or not.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public class EmptyBlockChainingListener extends AbstractChainingListener
{
    private Stack<Boolean> containerBlockStates = new Stack<Boolean>();

    public EmptyBlockChainingListener(ListenerChain listenerChain)
    {
        setListenerChain(listenerChain);
    }

    public boolean isCurrentContainerBlockEmpty()
    {
        return this.containerBlockStates.peek();
    }

    // Events

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginDocument(java.util.Map)
     */
    @Override
    public void beginDocument(Map<String, String> parameters)
    {
        startContainerBlock();
        super.beginDocument(parameters);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginDefinitionDescription()
     */
    @Override
    public void beginDefinitionDescription()
    {
        markNotEmpty();
        startContainerBlock();
        super.beginDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     *
     * @see AbstractChainingListener#beginDefinitionList(java.util.Map)
     * @since 2.0RC1
     */
    @Override
    public void beginDefinitionList(Map<String, String> parameters)
    {
        markNotEmpty();
        startContainerBlock();
        super.beginDefinitionList(parameters);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginDefinitionTerm()
     */
    @Override
    public void beginDefinitionTerm()
    {
        markNotEmpty();
        startContainerBlock();
        super.beginDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginGroup(java.util.Map)
     */
    @Override
    public void beginGroup(Map<String, String> parameters)
    {
        markNotEmpty();
        startContainerBlock();
        super.beginGroup(parameters);
    }

    /**
     * {@inheritDoc}
     *
     * @see AbstractChainingListener#beginFormat(org.xwiki.rendering.listener.Format, java.util.Map) 
     */
    @Override
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        markNotEmpty();
        startContainerBlock();
        super.beginFormat(format, parameters);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginLink(
     *      org.xwiki.rendering.listener.reference.ResourceReference , boolean, java.util.Map)
     * @since 2.5RC1
     */
    @Override
    public void beginLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        markNotEmpty();
        startContainerBlock();
        super.beginLink(reference, isFreeStandingURI, parameters);
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
        markNotEmpty();
        startContainerBlock();
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
        markNotEmpty();
        startContainerBlock();
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
        markNotEmpty();
        startContainerBlock();
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
        markNotEmpty();
        startContainerBlock();
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
        markNotEmpty();
        startContainerBlock();
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
        markNotEmpty();
        startContainerBlock();
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
        markNotEmpty();
        startContainerBlock();
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
        markNotEmpty();
        startContainerBlock();
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
        markNotEmpty();
        startContainerBlock();
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
        markNotEmpty();
        startContainerBlock();
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
        markNotEmpty();
        startContainerBlock();
        super.beginTableHeadCell(parameters);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginSection(java.util.Map)
     */
    @Override
    public void beginSection(Map<String, String> parameters)
    {
        markNotEmpty();
        startContainerBlock();
        super.beginSection(parameters);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDocument(java.util.Map) 
     */
    @Override
    public void endDocument(Map<String, String> parameters)
    {
        super.endDocument(parameters);
        stopContainerBlock();
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
        stopContainerBlock();
    }

    /**
     * {@inheritDoc}
     *
     * @see AbstractChainingListener#endDefinitionList(java.util.Map)
     * @since 2.0RC1
     */
    @Override
    public void endDefinitionList(Map<String, String> parameters)
    {
        super.endDefinitionList(parameters);
        stopContainerBlock();
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
        stopContainerBlock();
    }

    /**
     * {@inheritDoc}
     *
     * @see AbstractChainingListener#endFormat(org.xwiki.rendering.listener.Format, java.util.Map)
     */
    @Override
    public void endFormat(Format format, Map<String, String> parameters)
    {
        super.endFormat(format, parameters);
        stopContainerBlock();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endGroup(java.util.Map)
     */
    @Override
    public void endGroup(Map<String, String> parameters)
    {
        super.endGroup(parameters);
        stopContainerBlock();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endLink(
     *      org.xwiki.rendering.listener.reference.ResourceReference , boolean, java.util.Map)
     * @since 2.5RC1
     */
    @Override
    public void endLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        super.endLink(reference, isFreeStandingURI, parameters);
        stopContainerBlock();
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
        stopContainerBlock();
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
        stopContainerBlock();
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
        stopContainerBlock();
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
        stopContainerBlock();
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
        stopContainerBlock();
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
        stopContainerBlock();
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
        stopContainerBlock();
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
        stopContainerBlock();
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
        stopContainerBlock();
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
        stopContainerBlock();
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
        stopContainerBlock();
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
        stopContainerBlock();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onRawText(String, org.xwiki.rendering.syntax.Syntax)
     */
    @Override
    public void onRawText(String text, Syntax syntax)
    {
        super.onRawText(text, syntax);
        markNotEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onEmptyLines(int)
     */
    @Override
    public void onEmptyLines(int count)
    {
        super.onEmptyLines(count);
        markNotEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onHorizontalLine(java.util.Map)
     */
    @Override
    public void onHorizontalLine(Map<String, String> parameters)
    {
        super.onHorizontalLine(parameters);
        markNotEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onId(java.lang.String)
     */
    @Override
    public void onId(String name)
    {
        super.onId(name);
        markNotEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onImage(
     *      org.xwiki.rendering.listener.reference.ResourceReference , boolean, java.util.Map)
     * @since 2.5RC1
     */
    @Override
    public void onImage(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        super.onImage(reference, isFreeStandingURI, parameters);
        markNotEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onNewLine()
     */
    @Override
    public void onNewLine()
    {
        super.onNewLine();
        markNotEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onSpace()
     */
    @Override
    public void onSpace()
    {
        super.onSpace();
        markNotEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onSpecialSymbol(char)
     */
    @Override
    public void onSpecialSymbol(char symbol)
    {
        super.onSpecialSymbol(symbol);
        markNotEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onVerbatim(String, boolean, Map)
     */
    @Override
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        super.onVerbatim(protectedString, isInline, parameters);
        markNotEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onWord(java.lang.String)
     */
    @Override
    public void onWord(String word)
    {
        super.onWord(word);
        markNotEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onMacro(java.lang.String, java.util.Map,
     *      java.lang.String, boolean)
     */
    @Override
    public void onMacro(String id, Map<String, String> parameters, String content, boolean isInline)
    {
        super.onMacro(id, parameters, content, isInline);
        markNotEmpty();
    }

    private void startContainerBlock()
    {
        this.containerBlockStates.push(Boolean.TRUE);
    }

    private void stopContainerBlock()
    {
        this.containerBlockStates.pop();        
    }

    private void markNotEmpty()
    {
        if (!this.containerBlockStates.isEmpty() && this.containerBlockStates.peek()) {
            this.containerBlockStates.pop();
            this.containerBlockStates.push(Boolean.FALSE);
        }
    }

}
