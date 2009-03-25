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
package org.xwiki.rendering.internal.renderer.chaining;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.xml.XMLComment;
import org.xwiki.rendering.listener.xml.XMLElement;
import org.xwiki.rendering.listener.xml.XMLNode;
import org.xwiki.rendering.renderer.LinkLabelGenerator;
import org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.printer.XMLWikiPrinter;

/**
 * Print only plain text information. For example it remove anything which need a specific syntax a simple plain text
 * editor can't support like the style, link, image, etc. This renderer is mainly used to generate a simple as possible
 * label like in a TOC.
 * 
 * @version $Id$
 * @since 1.8.1
 */
public class PlainTextChainingRenderer extends AbstractChainingPrintRenderer
{
    private boolean isFirstElementRendered = false;

    private XMLWikiPrinter xmlWikiPrinter;

    /**
     * Generate link label.
     */
    private LinkLabelGenerator linkLabelGenerator;

    public PlainTextChainingRenderer(WikiPrinter printer, LinkLabelGenerator linkLabelGenerator,
        ListenerChain listenerChain)
    {
        super(printer, listenerChain);

        this.linkLabelGenerator = linkLabelGenerator;
        this.xmlWikiPrinter = new XMLWikiPrinter(printer);
    }

    protected XMLWikiPrinter getXMLWikiPrinter()
    {
        return this.xmlWikiPrinter;
    }

    // State

    private BlockStateChainingListener getBlockState()
    {
        return (BlockStateChainingListener) getListenerChain().getListener(BlockStateChainingListener.class);
    }

    // Events

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginParagraph(java.util.Map)
     */
    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        printEmptyLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onNewLine()
     */
    @Override
    public void onNewLine()
    {
        getPrinter().print("\n");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginLink(org.xwiki.rendering.listener.Link,
     *      boolean, java.util.Map)
     */
    @Override
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        if (link.getType() == LinkType.DOCUMENT) {
            getPrinter().print(this.linkLabelGenerator.generate(link));
        } else {
            getPrinter().print(link.getReference());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginHeader(org.xwiki.rendering.listener.HeaderLevel,
     *      java.util.Map)
     */
    @Override
    public void beginHeader(HeaderLevel level, Map<String, String> parameters)
    {
        printEmptyLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onWord(java.lang.String)
     */
    @Override
    public void onWord(String word)
    {
        getPrinter().print(word);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginListItem()
     */
    @Override
    public void beginListItem()
    {
        if (getBlockState().getListItemIndex() > 0) {
            getPrinter().print("\n");
        } else {
            printEmptyLine();
        }

        // TODO: add some syntax here like a - or not, that's the question
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onSpace()
     */
    @Override
    public void onSpace()
    {
        getPrinter().print(" ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onSpecialSymbol(char)
     */
    @Override
    public void onSpecialSymbol(char symbol)
    {
        getPrinter().print(String.valueOf(symbol));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginXMLNode(org.xwiki.rendering.listener.xml.XMLNode)
     */
    @Override
    public void beginXMLNode(XMLNode node)
    {
        switch (node.getNodeType()) {
            case CDATA:
                getXMLWikiPrinter().printXMLStartCData();
                break;
            case COMMENT:
                XMLComment commentNode = (XMLComment) node;
                getXMLWikiPrinter().printXMLComment(commentNode.getComment());
                break;
            case ELEMENT:
                XMLElement elementNode = (XMLElement) node;
                getXMLWikiPrinter().printXMLStartElement(elementNode.getName(), elementNode.getAttributes());
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#endXMLNode(org.xwiki.rendering.listener.xml.XMLNode)
     */
    @Override
    public void endXMLNode(XMLNode node)
    {
        switch (node.getNodeType()) {
            case CDATA:
                getXMLWikiPrinter().printXMLEndCData();
                break;
            case ELEMENT:
                XMLElement elementNode = (XMLElement) node;
                getXMLWikiPrinter().printXMLEndElement(elementNode.getName());
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onHorizontalLine(java.util.Map)
     */
    @Override
    public void onHorizontalLine(Map<String, String> parameters)
    {
        printEmptyLine();
        getPrinter().print("----");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onEmptyLines(int)
     */
    @Override
    public void onEmptyLines(int count)
    {
        getPrinter().print(StringUtils.repeat("\n", count));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onVerbatim(java.lang.String,
     *      java.util.Map, boolean)
     */
    @Override
    public void onVerbatim(String protectedString, Map<String, String> parameters, boolean isInline)
    {
        getPrinter().print(protectedString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginDefinitionTerm()
     */
    @Override
    public void beginDefinitionTerm()
    {
        if (getBlockState().getDefinitionListItemIndex() > 0 || getBlockState().getListItemIndex() >= 0) {
            getPrinter().print("\n");
        } else {
            printEmptyLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginDefinitionDescription()
     */
    @Override
    public void beginDefinitionDescription()
    {
        if (getBlockState().getDefinitionListItemIndex() > 0 || getBlockState().getListItemIndex() >= 0) {
            getPrinter().print("\n");
        } else {
            printEmptyLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginQuotationLine()
     */
    @Override
    public void beginQuotationLine()
    {
        if (getBlockState().getQuotationLineIndex() > 0) {
            getPrinter().print("\n");
        } else {
            printEmptyLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginTable(java.util.Map)
     */
    @Override
    public void beginTable(Map<String, String> parameters)
    {
        printEmptyLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginTableCell(java.util.Map)
     */
    @Override
    public void beginTableCell(Map<String, String> parameters)
    {
        if (getBlockState().getCellCol() > 0) {
            getPrinter().print("\t");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginTableHeadCell(java.util.Map)
     */
    @Override
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        beginTableCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#beginTableRow(java.util.Map)
     */
    @Override
    public void beginTableRow(Map<String, String> parameters)
    {
        if (getBlockState().getCellRow() > 0) {
            getPrinter().print("\n");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#onImage(org.xwiki.rendering.listener.Image,
     *      boolean, java.util.Map)
     */
    @Override
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // TODO: maybe something could be done here
    }

    private void printEmptyLine()
    {
        if (this.isFirstElementRendered) {
            getPrinter().print("\n\n");
        } else {
            this.isFirstElementRendered = true;
        }
    }
}
