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
    private int documentDepth;

    private int inlineDepth = 0;

    private boolean isInParagraph;

    private boolean isInSection;

    private int linkDepth = 0;

    private boolean isInTable;

    private boolean isInTableCell;

    private int definitionListDepth = 0;

    private int listDepth;

    private int listItemDepth;

    private int quotationDepth = 0;

    private boolean isInQuotationLine;

    public void setDocumentDepth(int documentDepth)
    {
        this.documentDepth = documentDepth;
    }

    public int getDocumentDepth()
    {
        return this.documentDepth;
    }

    public boolean isInDocument()
    {
        return this.documentDepth > 0;
    }

    public boolean isInLine()
    {
        return this.inlineDepth > 0;
    }

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
        return this.definitionListDepth > 0;
    }

    public boolean isInList()
    {
        return this.listDepth > 0;
    }

    public boolean isInListItem()
    {
        return this.listItemDepth > 0;
    }

    public boolean isInLink()
    {
        return this.linkDepth > 0;
    }

    public int getLinkDepth()
    {
        return this.linkDepth;
    }

    public boolean isInQuotation()
    {
        return this.quotationDepth > 0;
    }

    public int getQuotationDepth()
    {
        return this.quotationDepth;
    }

    public boolean isInQuotationLine()
    {
        return this.isInQuotationLine;
    }

    public int getDefinitionListDepth()
    {
        return this.definitionListDepth;
    }

    // Events

    public void beginDefinitionDescription()
    {
        ++inlineDepth;
    }

    public void beginDefinitionList()
    {
        ++this.definitionListDepth;
    }

    public void beginDefinitionTerm()
    {
        ++inlineDepth;
    }

    public void beginDocument()
    {
        ++this.documentDepth;
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
        ++this.linkDepth;
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        ++this.listDepth;
    }

    public void beginListItem()
    {
        ++this.listItemDepth;
        ++this.inlineDepth;
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Nothing to do
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.isInParagraph = true;
        ++this.inlineDepth;
    }

    public void beginQuotation(Map<String, String> parameters)
    {
        ++this.quotationDepth;
    }

    public void beginQuotationLine()
    {
        this.isInQuotationLine = true;
        ++this.inlineDepth;
    }

    public void beginSection(SectionLevel level, Map<String, String> parameters)
    {
        this.isInSection = true;
        ++this.inlineDepth;
    }

    public void beginTable(Map<String, String> parameters)
    {
        this.isInTable = true;
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        this.isInTableCell = true;
        ++this.inlineDepth;
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.isInTableCell = true;
        ++this.inlineDepth;
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
        --this.inlineDepth;
    }

    public void endDefinitionList()
    {
        --this.definitionListDepth;
    }

    public void endDefinitionTerm()
    {
        --this.inlineDepth;
    }

    public void endDocument()
    {
        --this.documentDepth;
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
        --this.linkDepth;
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        --this.listDepth;
    }

    public void endListItem()
    {
        --this.listItemDepth;
        --this.inlineDepth;
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Nothing to do
    }

    public void endParagraph(Map<String, String> parameters)
    {
        this.isInParagraph = false;
        --this.inlineDepth;
    }

    public void endQuotation(Map<String, String> parameters)
    {
        --this.quotationDepth;
    }

    public void endQuotationLine()
    {
        this.isInQuotationLine = false;
        --this.inlineDepth;
    }

    public void endSection(SectionLevel level, Map<String, String> parameters)
    {
        this.isInSection = false;
        --this.inlineDepth;
    }

    public void endTable(Map<String, String> parameters)
    {
        this.isInTable = false;
    }

    public void endTableCell(Map<String, String> parameters)
    {
        this.isInTableCell = false;
        --this.inlineDepth;
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.isInTableCell = false;
        --this.inlineDepth;
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
