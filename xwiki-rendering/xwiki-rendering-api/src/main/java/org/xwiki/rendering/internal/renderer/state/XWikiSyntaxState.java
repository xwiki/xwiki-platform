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
import java.util.Stack;

import org.xwiki.rendering.internal.renderer.XWikiMacroPrinter;
import org.xwiki.rendering.internal.renderer.XWikiSyntaxImageRenderer;
import org.xwiki.rendering.internal.renderer.XWikiSyntaxLinkRenderer;
import org.xwiki.rendering.renderer.ChainedListener;

/**
 * The state of the XWiki syntax renderer.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public class XWikiSyntaxState extends ChainedListener
{
    // Parsers

    private XWikiSyntaxLinkRenderer linkRenderer;

    private XWikiSyntaxImageRenderer imageRenderer;

    private XWikiMacroPrinter macroPrinter;

    // States listeners

    private BlockStateListener blockListener = new BlockStateListener();

    private TextOnNewLineStateListener textListener = new TextOnNewLineStateListener();

    private ConsecutiveNewLineStateListener newLineListener = new ConsecutiveNewLineStateListener();

    // Custom states

    private boolean isFirstElementRendered = false;

    private StringBuffer listStyle = new StringBuffer();

    private boolean isBeginListItemFound = false;

    private boolean isEndListItemFound = false;

    private boolean isBeginDefinitionListItemFound = false;

    private boolean isEndDefinitionListItemFound = false;

    private boolean isBeginQuotationLineFound = false;

    private boolean isEndQuotationLineFound = false;

    private boolean escapeIfSpace = false;

    private Stack<Boolean> isEndTableRowFoundStack = new Stack<Boolean>();

    private Map<String, String> previousFormatParameters;

    public XWikiSyntaxState()
    {
        this.macroPrinter = new XWikiMacroPrinter();
        this.linkRenderer = new XWikiSyntaxLinkRenderer();
        this.imageRenderer = new XWikiSyntaxImageRenderer();

        addStateListener(this.blockListener);
        addStateListener(this.textListener);
        addStateListener(this.newLineListener);
    }

    public XWikiSyntaxLinkRenderer getLinkRenderer()
    {
        return linkRenderer;
    }

    public XWikiSyntaxImageRenderer getImageRenderer()
    {
        return this.imageRenderer;
    }

    public XWikiMacroPrinter getMacroPrinter()
    {
        return this.macroPrinter;
    }

    // States listeners

    public BlockStateListener getBlockStateListener()
    {
        return this.blockListener;
    }

    public TextOnNewLineStateListener getTextOnNewLineStateListener()
    {
        return this.textListener;
    }

    public ConsecutiveNewLineStateListener getConsecutiveNewLineStateListener()
    {
        return this.newLineListener;
    }

    // Custom states

    public void setFirstElementRendered(boolean isFirstElementRendered)
    {
        this.isFirstElementRendered = isFirstElementRendered;
    }

    public boolean isFirstElementRendered()
    {
        return isFirstElementRendered;
    }

    public void setListStyle(StringBuffer listStyle)
    {
        this.listStyle = listStyle;
    }

    public StringBuffer getListStyle()
    {
        return listStyle;
    }

    public void setBeginListItemFound(boolean isBeginListItemFound)
    {
        this.isBeginListItemFound = isBeginListItemFound;
    }

    public boolean isBeginListItemFound()
    {
        return isBeginListItemFound;
    }

    public void setEndListItemFound(boolean isEndListItemFound)
    {
        this.isEndListItemFound = isEndListItemFound;
    }

    public boolean isEndListItemFound()
    {
        return isEndListItemFound;
    }

    public void setBeginDefinitionListItemFound(boolean isBeginDefinitionListItemFound)
    {
        this.isBeginDefinitionListItemFound = isBeginDefinitionListItemFound;
    }

    public boolean isBeginDefinitionListItemFound()
    {
        return isBeginDefinitionListItemFound;
    }

    public void setEndDefinitionListItemFound(boolean isEndDefinitionListItemFound)
    {
        this.isEndDefinitionListItemFound = isEndDefinitionListItemFound;
    }

    public boolean isEndDefinitionListItemFound()
    {
        return isEndDefinitionListItemFound;
    }

    public void setBeginQuotationLineFound(boolean isBeginQuotationLineFound)
    {
        this.isBeginQuotationLineFound = isBeginQuotationLineFound;
    }

    public boolean isBeginQuotationLineFound()
    {
        return isBeginQuotationLineFound;
    }

    public void setEndQuotationLineFound(boolean isEndQuotationLineFound)
    {
        this.isEndQuotationLineFound = isEndQuotationLineFound;
    }

    public boolean isEndQuotationLineFound()
    {
        return isEndQuotationLineFound;
    }

    public void setEscapeIfSpace(boolean escapeIfSpace)
    {
        this.escapeIfSpace = escapeIfSpace;
    }

    public boolean isEscapeIfSpace()
    {
        return escapeIfSpace;
    }

    public Stack<Boolean> getIsEndTableRowFoundStack()
    {
        return this.isEndTableRowFoundStack;
    }

    public void pushEndTableRowFound(boolean isEndTableRowFound)
    {
        this.isEndTableRowFoundStack.push(isEndTableRowFound);
    }

    public void popEndTableRowFound()
    {
        this.isEndTableRowFoundStack.pop();
    }

    public boolean isEndTableRowFound()
    {
        return isEndTableRowFoundStack.peek();
    }

    public void setPreviousFormatParameters(Map<String, String> previousFormatParameters)
    {
        this.previousFormatParameters = previousFormatParameters;
    }

    public Map<String, String> getPreviousFormatParameters()
    {
        return previousFormatParameters;
    }

    // Events
}
