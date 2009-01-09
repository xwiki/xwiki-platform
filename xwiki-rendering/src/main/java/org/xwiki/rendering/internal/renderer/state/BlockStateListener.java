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
package org.xwiki.rendering.internal.renderer.state;

import java.util.Map;

import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.xml.XMLNode;

/**
 * Indicates block element for which we are inside.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class BlockStateListener implements Listener
{
    private boolean isInParagraph;

    private boolean isInSection;

    private boolean isInLink;

    private boolean isInTable;

    private boolean isInTableCell;
    
    private boolean isInDefinitionList;
    
    private boolean isInList;
    
    private boolean isInQuotation;
    
    private boolean isInQuotationLine;
    
    public boolean isInParagraph()
    {
        return this.isInParagraph;
    }
    
    public boolean isInSection()
    {
        return this.isInSection;
    }
    
    public boolean isInTable()
    {
        return this.isInTable;
    }
    
    public boolean isInTableCell()
    {
        return this.isInTableCell;
    }

    public boolean isInDefinitionList()
    {
        return this.isInDefinitionList;
    }
    
    public boolean isInList()
    {
        return this.isInList;
    }

    public boolean isInLink()
    {
        return this.isInLink;
    }
    
    public boolean isInQuotation()
    {
        return this.isInQuotation;
    }
    
    public boolean isInQuotationLine()
    {
        return this.isInQuotationLine;
    }
    
    public void beginDefinitionDescription()
    {
        // Nothing to do
    }

    public void beginDefinitionList()
    {
        this.isInDefinitionList = true;
    }

    public void beginDefinitionTerm()
    {
        // Nothing to do
    }

    public void beginDocument()
    {
        // Nothing to do
    }

    public void beginError(String message, String description)
    {
        // Nothing to do
    }

    public void beginFormat(Format format, Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.isInLink = true;
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        this.isInList = true;
    }

    public void beginListItem()
    {
        // Nothing to do
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Nothing to do
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.isInParagraph = true;
    }

    public void beginQuotation(Map<String, String> parameters)
    {
        this.isInQuotation = true;
    }

    public void beginQuotationLine()
    {
        this.isInQuotationLine = true;
    }

    public void beginSection(SectionLevel level, Map<String, String> parameters)
    {
        this.isInSection = true;
    }

    public void beginTable(Map<String, String> parameters)
    {
        this.isInTable = true;
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        this.isInTableCell = true;
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.isInTableCell = true;
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void beginXMLNode(XMLNode node)
    {
        // Nothing to do
    }

    public void endDefinitionDescription()
    {
        // Nothing to do

    }

    public void endDefinitionList()
    {
        this.isInDefinitionList = false;
    }

    public void endDefinitionTerm()
    {
        // Nothing to do
    }

    public void endDocument()
    {
        // Nothing to do
    }

    public void endError(String message, String description)
    {
        // Nothing to do
    }

    public void endFormat(Format format, Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.isInLink = false;
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        this.isInList = false;
    }

    public void endListItem()
    {
        // Nothing to do
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Nothing to do
    }

    public void endParagraph(Map<String, String> parameters)
    {
        this.isInParagraph = false;
    }

    public void endQuotation(Map<String, String> parameters)
    {
        this.isInQuotation = false;
    }

    public void endQuotationLine()
    {
        this.isInQuotationLine = false;
    }

    public void endSection(SectionLevel level, Map<String, String> parameters)
    {
        this.isInSection = false;
    }

    public void endTable(Map<String, String> parameters)
    {
        this.isInTable = false;
    }

    public void endTableCell(Map<String, String> parameters)
    {
        this.isInTableCell = false;
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.isInTableCell = false;
    }

    public void endTableRow(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void endXMLNode(XMLNode node)
    {
        // Nothing to do
    }

    public void onEmptyLines(int count)
    {
        // Nothing to do
    }

    public void onHorizontalLine(Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void onId(String name)
    {
        // Nothing to do
    }

    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        // Nothing to do
    }

    public void onNewLine()
    {
        // Nothing to do
    }

    public void onSpace()
    {
        // Nothing to do
    }

    public void onSpecialSymbol(char symbol)
    {
        // Nothing to do
    }

    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        // Nothing to do
    }

    public void onVerbatimInline(String protectedString)
    {
        // Nothing to do
    }

    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        // Nothing to do
    }

    public void onWord(String word)
    {
        // Nothing to do
    }
}
