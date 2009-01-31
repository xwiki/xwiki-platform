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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.xml.XMLNode;

/**
 * A chained implementation of Listener. Contains a list of provided Listener called one by one for each event.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class ChainedListener implements Listener
{
    private List<Listener> stateListeners = new ArrayList<Listener>();

    public void addStateListener(Listener stateListener)
    {
        this.stateListeners.add(stateListener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     */
    public void beginDefinitionDescription()
    {
        for (Listener listener : this.stateListeners) {
            listener.beginDefinitionDescription();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList()
     */
    public void beginDefinitionList()
    {
        for (Listener listener : this.stateListeners) {
            listener.beginDefinitionList();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     */
    public void beginDefinitionTerm()
    {
        for (Listener listener : this.stateListeners) {
            listener.beginDefinitionTerm();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDocument()
     */
    public void beginDocument()
    {
        for (Listener listener : this.stateListeners) {
            listener.beginDocument();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginError(java.lang.String, java.lang.String)
     */
    public void beginError(String message, String description)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginError(message, description);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginFormat(org.xwiki.rendering.listener.Format, java.util.Map)
     */
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginFormat(format, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginLink(org.xwiki.rendering.listener.Link, boolean, java.util.Map)
     */
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginLink(link, isFreeStandingURI, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginList(listType, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginListItem()
     */
    public void beginListItem()
    {
        for (Listener listener : this.stateListeners) {
            listener.beginListItem();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginMacroMarker(java.lang.String, java.util.Map, java.lang.String)
     */
    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginMacroMarker(name, parameters, content);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginParagraph(java.util.Map)
     */
    public void beginParagraph(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginParagraph(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginQuotation(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     */
    public void beginQuotationLine()
    {
        for (Listener listener : this.stateListeners) {
            listener.beginQuotationLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginSection(org.xwiki.rendering.listener.HeaderLevel, java.util.Map)
     */
    public void beginSection(HeaderLevel level, Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginSection(level, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTable(java.util.Map)
     */
    public void beginTable(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginTable(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableCell(java.util.Map)
     */
    public void beginTableCell(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginTableCell(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableHeadCell(java.util.Map)
     */
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginTableHeadCell(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableRow(java.util.Map)
     */
    public void beginTableRow(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginTableRow(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginXMLNode(org.xwiki.rendering.listener.xml.XMLNode)
     */
    public void beginXMLNode(XMLNode node)
    {
        for (Listener listener : this.stateListeners) {
            listener.beginXMLNode(node);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     */
    public void endDefinitionDescription()
    {
        for (Listener listener : this.stateListeners) {
            listener.endDefinitionDescription();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList()
     */
    public void endDefinitionList()
    {
        for (Listener listener : this.stateListeners) {
            listener.endDefinitionList();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     */
    public void endDefinitionTerm()
    {
        for (Listener listener : this.stateListeners) {
            listener.endDefinitionTerm();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDocument()
     */
    public void endDocument()
    {
        for (Listener listener : this.stateListeners) {
            listener.endDocument();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endError(java.lang.String, java.lang.String)
     */
    public void endError(String message, String description)
    {
        for (Listener listener : this.stateListeners) {
            listener.endError(message, description);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endFormat(org.xwiki.rendering.listener.Format, java.util.Map)
     */
    public void endFormat(Format format, Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.endFormat(format, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endLink(org.xwiki.rendering.listener.Link, boolean, java.util.Map)
     */
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.endLink(link, isFreeStandingURI, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void endList(ListType listType, Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.endList(listType, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endListItem()
     */
    public void endListItem()
    {
        for (Listener listener : this.stateListeners) {
            listener.endListItem();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endMacroMarker(java.lang.String, java.util.Map, java.lang.String)
     */
    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        for (Listener listener : this.stateListeners) {
            listener.endMacroMarker(name, parameters, content);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endParagraph(java.util.Map)
     */
    public void endParagraph(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.endParagraph(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     */
    public void endQuotation(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.endQuotation(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     */
    public void endQuotationLine()
    {
        for (Listener listener : this.stateListeners) {
            listener.endQuotationLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endHeader(org.xwiki.rendering.listener.HeaderLevel, java.util.Map)
     */
    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.endHeader(level, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTable(java.util.Map)
     */
    public void endTable(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.endTable(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableCell(java.util.Map)
     */
    public void endTableCell(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.endTableCell(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableHeadCell(java.util.Map)
     */
    public void endTableHeadCell(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.endTableHeadCell(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableRow(java.util.Map)
     */
    public void endTableRow(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.endTableRow(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endXMLNode(org.xwiki.rendering.listener.xml.XMLNode)
     */
    public void endXMLNode(XMLNode node)
    {
        for (Listener listener : this.stateListeners) {
            listener.endXMLNode(node);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        for (Listener listener : this.stateListeners) {
            listener.onEmptyLines(count);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine(java.util.Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.onHorizontalLine(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onId(java.lang.String)
     */
    public void onId(String name)
    {
        for (Listener listener : this.stateListeners) {
            listener.onId(name);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onImage(org.xwiki.rendering.listener.Image, boolean, java.util.Map)
     */
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.onImage(image, isFreeStandingURI, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onInlineMacro(java.lang.String, java.util.Map, java.lang.String)
     */
    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        for (Listener listener : this.stateListeners) {
            listener.onInlineMacro(name, parameters, content);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onNewLine()
     */
    public void onNewLine()
    {
        for (Listener listener : this.stateListeners) {
            listener.onNewLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onSpace()
     */
    public void onSpace()
    {
        for (Listener listener : this.stateListeners) {
            listener.onSpace();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onSpecialSymbol(char)
     */
    public void onSpecialSymbol(char symbol)
    {
        for (Listener listener : this.stateListeners) {
            listener.onSpecialSymbol(symbol);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onStandaloneMacro(java.lang.String, java.util.Map, java.lang.String)
     */
    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        for (Listener listener : this.stateListeners) {
            listener.onStandaloneMacro(name, parameters, content);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatimInline(java.lang.String)
     */
    public void onVerbatimInline(String protectedString)
    {
        for (Listener listener : this.stateListeners) {
            listener.onVerbatimInline(protectedString);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatimStandalone(java.lang.String, java.util.Map)
     */
    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        for (Listener listener : this.stateListeners) {
            listener.onVerbatimStandalone(protectedString, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onWord(java.lang.String)
     */
    public void onWord(String word)
    {
        for (Listener listener : this.stateListeners) {
            listener.onWord(word);
        }
    }
}
