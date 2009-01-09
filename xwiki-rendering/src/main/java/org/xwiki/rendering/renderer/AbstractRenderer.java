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

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.xml.XMLNode;

/**
 * Common renderer implementation that all Renderers who need State information should extend. It allows registering
 * listeners to save states when needed and when events are executed all registered listeners are executed (in the 
 * order they were registered under). Renderers extending this class <b>must</b> make sure to call this class methods
 * in all events so that the registered listeners are called.
 *   
 * @version $Id$
 * @since 1.7
 */
public abstract class AbstractRenderer extends AbstractLogEnabled implements Renderer
{
    private List<Listener> stateListeners = new ArrayList<Listener>();
    
    public void registerStateListener(Listener stateListener) 
    {
        this.stateListeners.add(stateListener);
    }
    
    public void beginDefinitionDescription()
    {
        for (Listener listener: this.stateListeners) {
            listener.beginDefinitionDescription();
        }
    }

    public void beginDefinitionList()
    {
        for (Listener listener: this.stateListeners) {
            listener.beginDefinitionList();
        }
    }

    public void beginDefinitionTerm()
    {
        for (Listener listener: this.stateListeners) {
            listener.beginDefinitionTerm();
        }
    }

    public void beginDocument()
    {
        for (Listener listener: this.stateListeners) {
            listener.beginDocument();
        }
    }

    public void beginError(String message, String description)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginError(message, description);
        }
    }

    public void beginFormat(Format format, Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginFormat(format, parameters);
        }
    }

    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginLink(link, isFreeStandingURI, parameters);
        }
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginList(listType, parameters);
        }
    }

    public void beginListItem()
    {
        for (Listener listener: this.stateListeners) {
            listener.beginListItem();
        }
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginMacroMarker(name, parameters, content);
        }
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginParagraph(parameters);
        }
    }

    public void beginQuotation(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginQuotation(parameters);
        }
    }

    public void beginQuotationLine()
    {
        for (Listener listener: this.stateListeners) {
            listener.beginQuotationLine();
        }
    }

    public void beginSection(SectionLevel level, Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginSection(level, parameters);
        }
    }

    public void beginTable(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginTable(parameters);
        }
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginTableCell(parameters);
        }
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginTableHeadCell(parameters);
        }
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginTableRow(parameters);
        }
    }

    public void beginXMLNode(XMLNode node)
    {
        for (Listener listener: this.stateListeners) {
            listener.beginXMLNode(node);
        }
    }

    public void endDefinitionDescription()
    {
        for (Listener listener: this.stateListeners) {
            listener.endDefinitionDescription();
        }
    }

    public void endDefinitionList()
    {
        for (Listener listener: this.stateListeners) {
            listener.endDefinitionList();
        }
    }

    public void endDefinitionTerm()
    {
        for (Listener listener: this.stateListeners) {
            listener.endDefinitionTerm();
        }
    }

    public void endDocument()
    {
        for (Listener listener: this.stateListeners) {
            listener.endDocument();
        }
    }

    public void endError(String message, String description)
    {
        for (Listener listener: this.stateListeners) {
            listener.endError(message, description);
        }
    }

    public void endFormat(Format format, Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.endFormat(format, parameters);
        }
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.endLink(link, isFreeStandingURI, parameters);
        }
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.endList(listType, parameters);
        }
    }

    public void endListItem()
    {
        for (Listener listener: this.stateListeners) {
            listener.endListItem();
        }
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        for (Listener listener: this.stateListeners) {
            listener.endMacroMarker(name, parameters, content);
        }
    }

    public void endParagraph(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.endParagraph(parameters);
        }
    }

    public void endQuotation(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.endQuotation(parameters);
        }
    }

    public void endQuotationLine()
    {
        for (Listener listener: this.stateListeners) {
            listener.endQuotationLine();
        }
    }

    public void endSection(SectionLevel level, Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.endSection(level, parameters);
        }
    }

    public void endTable(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.endTable(parameters);
        }
    }

    public void endTableCell(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.endTableCell(parameters);
        }
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.endTableHeadCell(parameters);
        }
    }

    public void endTableRow(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.endTableRow(parameters);
        }
    }

    public void endXMLNode(XMLNode node)
    {
        for (Listener listener: this.stateListeners) {
            listener.endXMLNode(node);
        }
    }

    public void onEmptyLines(int count)
    {
        for (Listener listener: this.stateListeners) {
            listener.onEmptyLines(count);
        }
    }

    public void onHorizontalLine(Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.onHorizontalLine(parameters);
        }
    }

    public void onId(String name)
    {
        for (Listener listener: this.stateListeners) {
            listener.onId(name);
        }
    }

    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.onImage(image, isFreeStandingURI, parameters);
        }
    }

    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        for (Listener listener: this.stateListeners) {
            listener.onInlineMacro(name, parameters, content);
        }
    }

    public void onNewLine()
    {
        for (Listener listener: this.stateListeners) {
            listener.onNewLine();
        }
    }

    public void onSpace()
    {
        for (Listener listener: this.stateListeners) {
            listener.onSpace();
        }
    }

    public void onSpecialSymbol(char symbol)
    {
        for (Listener listener: this.stateListeners) {
            listener.onSpecialSymbol(symbol);
        }
    }

    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        for (Listener listener: this.stateListeners) {
            listener.onStandaloneMacro(name, parameters, content);
        }
    }

    public void onVerbatimInline(String protectedString)
    {
        for (Listener listener: this.stateListeners) {
            listener.onVerbatimInline(protectedString);
        }
    }

    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        for (Listener listener: this.stateListeners) {
            listener.onVerbatimStandalone(protectedString, parameters);
        }
    }

    public void onWord(String word)
    {
        for (Listener listener: this.stateListeners) {
            listener.onWord(word);
        }
    }
}
