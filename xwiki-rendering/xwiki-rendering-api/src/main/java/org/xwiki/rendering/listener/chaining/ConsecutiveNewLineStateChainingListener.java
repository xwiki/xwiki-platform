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
public class ConsecutiveNewLineStateChainingListener extends AbstractChainingListener implements
    StackableChainingListener
{
    private int newLineCount = 0;

    public ConsecutiveNewLineStateChainingListener(ListenerChain listenerChain)
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
        return new ConsecutiveNewLineStateChainingListener(getListenerChain());
    }

    public int getNewLineCount()
    {
        return this.newLineCount;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDefinitionDescription()
     */
    @Override
    public void endDefinitionDescription()
    {
        this.newLineCount = 0;
        super.endDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDefinitionList()
     */
    @Override
    public void endDefinitionList()
    {
        this.newLineCount = 0;
        super.endDefinitionList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDefinitionTerm()
     */
    @Override
    public void endDefinitionTerm()
    {
        this.newLineCount = 0;
        super.endDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDocument(java.util.Map)
     */
    @Override
    public void endDocument(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endDocument(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endGroup(Map)
     */
    @Override
    public void endGroup(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endGroup(parameters);
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
        this.newLineCount = 0;
        super.endFormat(format, parameters);
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
        this.newLineCount = 0;
        super.endLink(link, isFreeStandingURI, parameters);
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
        this.newLineCount = 0;
        super.endList(listType, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endListItem()
     */
    @Override
    public void endListItem()
    {
        this.newLineCount = 0;
        super.endListItem();
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
        this.newLineCount = 0;
        super.endMacroMarker(name, parameters, content, isInline);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endParagraph(java.util.Map)
     */
    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endParagraph(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endQuotation(java.util.Map)
     */
    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endQuotation(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endQuotationLine()
     */
    @Override
    public void endQuotationLine()
    {
        this.newLineCount = 0;
        super.endQuotationLine();
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
        this.newLineCount = 0;
        super.endHeader(level, id, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTable(java.util.Map)
     */
    @Override
    public void endTable(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endTable(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTableCell(java.util.Map)
     */
    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endTableCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTableHeadCell(java.util.Map)
     */
    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endTableHeadCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endTableRow(java.util.Map)
     */
    @Override
    public void endTableRow(Map<String, String> parameters)
    {
        this.newLineCount = 0;
        super.endTableRow(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endXMLNode(org.xwiki.rendering.listener.xml.XMLNode)
     */
    @Override
    public void endXMLNode(XMLNode node)
    {
        this.newLineCount = 0;
        super.endXMLNode(node);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onEmptyLines(int)
     */
    @Override
    public void onEmptyLines(int count)
    {
        this.newLineCount = 0;
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
        this.newLineCount = 0;
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
        this.newLineCount = 0;
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
        this.newLineCount = 0;
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
        this.newLineCount++;
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
        this.newLineCount = 0;
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
        this.newLineCount = 0;
        super.onSpecialSymbol(symbol);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onMacro(String, Map, String, boolean)
     */
    @Override
    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        this.newLineCount = 0;
        super.onMacro(name, parameters, content, isInline);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#onVerbatim(String, boolean, Map)
     */
    @Override
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        this.newLineCount = 0;
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
        this.newLineCount = 0;
        super.onWord(word);
    }
}
