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
 * Indicate if the text being written starts a new line. By text we mean Space, Special Symbol and Words. This is useful
 * for some Renderers which need to have this information. For example the XWiki Syntax renderer uses it to decide
 * whether to escape "*" characters starting new lines since otherwise they would be confused for list items.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class TextOnNewLineStateChainingListener extends AbstractChainingListener implements StackableChainingListener
{
    private boolean isTextOnNewLine;

    public TextOnNewLineStateChainingListener(ListenerChain listenerChain)
    {
        super(listenerChain);
    }

    /**
     * {@inheritDoc}
     * @see StackableChainingListener#createChainingListenerInstance()
     */
    public StackableChainingListener createChainingListenerInstance()
    {
        return new TextOnNewLineStateChainingListener(getListenerChain());
    }

    public boolean isTextOnNewLine()
    {
        return this.isTextOnNewLine;
    }

    public void beginDefinitionDescription()
    {
        this.isTextOnNewLine = false;
        super.beginDefinitionDescription();
    }

    public void beginDefinitionList()
    {
        this.isTextOnNewLine = false;
        super.beginDefinitionList();
    }

    public void beginDefinitionTerm()
    {
        this.isTextOnNewLine = false;
        super.beginDefinitionTerm();
    }

    public void beginDocument()
    {
        this.isTextOnNewLine = false;
        super.beginDocument();
    }

    public void beginFormat(Format format, Map<String, String> parameters)
    {
        // This is an exception and a bit of a hack. The reason we're calling super before
        // setting that we're no longer on a new line is for cases when we have "**" (BOLD)
        // at the beginning of a line and we need to escape the following character if it's
        // a space as otherwise it would be confused for a list.
        super.beginFormat(format, parameters);

        this.isTextOnNewLine = false;
    }

    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
        super.beginLink(link, isFreeStandingURI, parameters);
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
        super.beginList(listType, parameters);
    }

    public void beginListItem()
    {
        this.isTextOnNewLine = false;
        super.beginListItem();
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        this.isTextOnNewLine = false;
        super.beginMacroMarker(name, parameters, content, isInline);
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.isTextOnNewLine = true;
        super.beginParagraph(parameters);
    }

    public void beginQuotation(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
        super.beginQuotation(parameters);
    }

    public void beginQuotationLine()
    {
        this.isTextOnNewLine = false;
        super.beginQuotationLine();
    }

    public void beginHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
        super.beginHeader(level, parameters);
    }

    public void beginTable(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
        super.beginTable(parameters);
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
        super.beginTableCell(parameters);
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
        super.beginTableHeadCell(parameters);
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
        super.beginTableRow(parameters);
    }

    public void beginXMLNode(XMLNode node)
    {
        this.isTextOnNewLine = false;
        super.beginXMLNode(node);
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        super.endLink(link, isFreeStandingURI, parameters);
        this.isTextOnNewLine = false;
    }

    public void endParagraph(Map<String, String> parameters)
    {
        super.endParagraph(parameters);
        this.isTextOnNewLine = true;
    }

    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        super.endHeader(level, parameters);
        this.isTextOnNewLine = false;
    }

    public void endTable(Map<String, String> parameters)
    {
        super.endTable(parameters);
        this.isTextOnNewLine = false;
    }

    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
        super.onHorizontalLine(parameters);
    }

    public void onId(String name)
    {
        this.isTextOnNewLine = false;
        super.onId(name);
    }

    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.isTextOnNewLine = false;
        super.onImage(image, isFreeStandingURI, parameters);
    }

    public void onNewLine()
    {
        this.isTextOnNewLine = true;
        super.onNewLine();
    }

    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        this.isTextOnNewLine = false;
        super.onMacro(name, parameters, content, isInline);
    }

    public void onVerbatim(String protectedString, Map<String, String> parameters, boolean isInline)
    {
        this.isTextOnNewLine = false;
        super.onVerbatim(protectedString, parameters, isInline);
    }
}
