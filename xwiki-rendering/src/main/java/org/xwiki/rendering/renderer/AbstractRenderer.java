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
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.xml.XMLNode;

/**
 * Common renderer implementation that all Renderers who need State information should extend. It saves current state
 * in a {@link RendererState} object that can later be accessed. Renderers extending this class <b>must</b> make sure
 * to call this class methods in all events so that the state is correctly handled.
 *   
 * @version $Id$
 * @since 1.7
 */
public abstract class AbstractRenderer extends AbstractLogEnabled implements Renderer
{
    private RendererState state = new RendererState();
    
    public RendererState getState()
    {
        return this.state;
    }

    public void beginDefinitionDescription()
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginDefinitionList()
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginDefinitionTerm()
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginDocument()
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginError(String message, String description)
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginFormat(Format format, Map<String, String> parameters)
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.state.setInLink(true);
        this.state.setTextOnNewLine(false);
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginListItem()
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.state.setInParagraph(true);
        this.state.setTextOnNewLine(true);
    }

    public void beginQuotation(Map<String, String> parameters)
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginQuotationLine()
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginSection(SectionLevel level, Map<String, String> parameters)
    {
        this.state.setInSection(true);
        this.state.setTextOnNewLine(false);
    }

    public void beginTable(Map<String, String> parameters)
    {
        this.state.setInTable(true);
        this.state.setTextOnNewLine(false);
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        this.state.setTextOnNewLine(false);
    }

    public void beginXMLNode(XMLNode node)
    {
        this.state.setTextOnNewLine(false);
    }

    public void endDefinitionDescription()
    {
    }

    public void endDefinitionList()
    {
    }

    public void endDefinitionTerm()
    {
    }

    public void endDocument()
    {
    }

    public void endError(String message, String description)
    {
    }

    public void endFormat(Format format, Map<String, String> parameters)
    {
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.state.setInLink(false);
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
    }

    public void endListItem()
    {
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
    }

    public void endParagraph(Map<String, String> parameters)
    {
        this.state.setInParagraph(true);
    }

    public void endQuotation(Map<String, String> parameters)
    {
    }

    public void endQuotationLine()
    {
    }

    public void endSection(SectionLevel level, Map<String, String> parameters)
    {
        this.state.setInSection(false);
    }

    public void endTable(Map<String, String> parameters)
    {
        this.state.setInTable(false);
    }

    public void endTableCell(Map<String, String> parameters)
    {
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
    }

    public void endTableRow(Map<String, String> parameters)
    {
    }

    public void endXMLNode(XMLNode node)
    {
    }

    public void onEmptyLines(int count)
    {
    }

    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.state.setTextOnNewLine(false);
    }

    public void onId(String name)
    {
        this.state.setTextOnNewLine(false);
    }

    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.state.setTextOnNewLine(false);
    }

    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        this.state.setTextOnNewLine(false);
    }

    public void onLineBreak()
    {
        this.state.setTextOnNewLine(true);
    }

    public void onNewLine()
    {
        this.state.setTextOnNewLine(true);
    }

    public void onSpace()
    {
        
    }

    public void onSpecialSymbol(char symbol)
    {
        
    }

    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        this.state.setTextOnNewLine(false);
    }

    public void onVerbatimInline(String protectedString)
    {
        this.state.setTextOnNewLine(false);
    }

    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        this.state.setTextOnNewLine(false);
    }

    public void onWord(String word)
    {
        
    }
}
