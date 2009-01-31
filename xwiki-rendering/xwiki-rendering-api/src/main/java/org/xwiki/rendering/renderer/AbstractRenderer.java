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
package org.xwiki.rendering.renderer;

import java.util.Map;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.xml.XMLNode;

/**
 * Common renderer implementation that all Renderers who need State information should extend. Renderers extending this
 * class <b>must</b> make sure to call this class methods in all events so that the registered listeners are called.
 * 
 * @version $Id$
 * @since 1.7
 */
public abstract class AbstractRenderer extends AbstractLogEnabled implements Renderer
{
    private Listener stateListener;

    public AbstractRenderer(Listener stateListener)
    {
        this.stateListener = stateListener;
    }

    public Listener getStateListener()
    {
        return this.stateListener;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     */
    public void beginDefinitionDescription()
    {
        this.stateListener.beginDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList()
     */
    public void beginDefinitionList()
    {
        this.stateListener.beginDefinitionList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     */
    public void beginDefinitionTerm()
    {
        this.stateListener.beginDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDocument()
     */
    public void beginDocument()
    {
        this.stateListener.beginDocument();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginError(java.lang.String, java.lang.String)
     */
    public void beginError(String message, String description)
    {
        this.stateListener.beginError(message, description);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginFormat(org.xwiki.rendering.listener.Format, java.util.Map)
     */
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        this.stateListener.beginFormat(format, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginLink(org.xwiki.rendering.listener.Link, boolean, java.util.Map)
     */
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.stateListener.beginLink(link, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        this.stateListener.beginList(listType, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginListItem()
     */
    public void beginListItem()
    {
        this.stateListener.beginListItem();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginMacroMarker(java.lang.String, java.util.Map, java.lang.String)
     */
    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.stateListener.beginMacroMarker(name, parameters, content);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginParagraph(java.util.Map)
     */
    public void beginParagraph(Map<String, String> parameters)
    {
        this.stateListener.beginParagraph(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        this.stateListener.beginQuotation(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     */
    public void beginQuotationLine()
    {
        this.stateListener.beginQuotationLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginHeader(org.xwiki.rendering.listener.HeaderLevel, java.util.Map)
     */
    public void beginHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.stateListener.beginHeader(level, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTable(java.util.Map)
     */
    public void beginTable(Map<String, String> parameters)
    {
        this.stateListener.beginTable(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableCell(java.util.Map)
     */
    public void beginTableCell(Map<String, String> parameters)
    {
        this.stateListener.beginTableCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableHeadCell(java.util.Map)
     */
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.stateListener.beginTableHeadCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableRow(java.util.Map)
     */
    public void beginTableRow(Map<String, String> parameters)
    {
        this.stateListener.beginTableRow(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginXMLNode(org.xwiki.rendering.listener.xml.XMLNode)
     */
    public void beginXMLNode(XMLNode node)
    {
        this.stateListener.beginXMLNode(node);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     */
    public void endDefinitionDescription()
    {
        this.stateListener.endDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList()
     */
    public void endDefinitionList()
    {
        this.stateListener.endDefinitionList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     */
    public void endDefinitionTerm()
    {
        this.stateListener.endDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDocument()
     */
    public void endDocument()
    {
        this.stateListener.endDocument();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endError(java.lang.String, java.lang.String)
     */
    public void endError(String message, String description)
    {
        this.stateListener.endError(message, description);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endFormat(org.xwiki.rendering.listener.Format, java.util.Map)
     */
    public void endFormat(Format format, Map<String, String> parameters)
    {
        this.stateListener.endFormat(format, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endLink(org.xwiki.rendering.listener.Link, boolean, java.util.Map)
     */
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.stateListener.endLink(link, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void endList(ListType listType, Map<String, String> parameters)
    {
        this.stateListener.endList(listType, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endListItem()
     */
    public void endListItem()
    {
        this.stateListener.endListItem();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endMacroMarker(java.lang.String, java.util.Map, java.lang.String)
     */
    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.stateListener.endMacroMarker(name, parameters, content);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endParagraph(java.util.Map)
     */
    public void endParagraph(Map<String, String> parameters)
    {
        this.stateListener.endParagraph(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     */
    public void endQuotation(Map<String, String> parameters)
    {
        this.stateListener.endQuotation(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     */
    public void endQuotationLine()
    {
        this.stateListener.endQuotationLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endHeader(org.xwiki.rendering.listener.HeaderLevel, java.util.Map)
     */
    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.stateListener.endHeader(level, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTable(java.util.Map)
     */
    public void endTable(Map<String, String> parameters)
    {
        this.stateListener.endTable(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableCell(java.util.Map)
     */
    public void endTableCell(Map<String, String> parameters)
    {
        this.stateListener.endTableCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableHeadCell(java.util.Map)
     */
    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.stateListener.endTableHeadCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableRow(java.util.Map)
     */
    public void endTableRow(Map<String, String> parameters)
    {
        this.stateListener.endTableRow(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endXMLNode(org.xwiki.rendering.listener.xml.XMLNode)
     */
    public void endXMLNode(XMLNode node)
    {
        this.stateListener.endXMLNode(node);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        this.stateListener.onEmptyLines(count);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine(java.util.Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.stateListener.onHorizontalLine(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onId(java.lang.String)
     */
    public void onId(String name)
    {
        this.stateListener.onId(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onImage(org.xwiki.rendering.listener.Image, boolean, java.util.Map)
     */
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.stateListener.onImage(image, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onInlineMacro(java.lang.String, java.util.Map, java.lang.String)
     */
    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        this.stateListener.onInlineMacro(name, parameters, content);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onNewLine()
     */
    public void onNewLine()
    {
        this.stateListener.onNewLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onSpace()
     */
    public void onSpace()
    {
        this.stateListener.onSpace();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onSpecialSymbol(char)
     */
    public void onSpecialSymbol(char symbol)
    {
        this.stateListener.onSpecialSymbol(symbol);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onStandaloneMacro(java.lang.String, java.util.Map, java.lang.String)
     */
    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        this.stateListener.onStandaloneMacro(name, parameters, content);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatimInline(java.lang.String)
     */
    public void onVerbatimInline(String protectedString)
    {
        this.stateListener.onVerbatimInline(protectedString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatimStandalone(java.lang.String, java.util.Map)
     */
    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        this.stateListener.onVerbatimStandalone(protectedString, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onWord(java.lang.String)
     */
    public void onWord(String word)
    {
        this.stateListener.onWord(word);
    }
}
