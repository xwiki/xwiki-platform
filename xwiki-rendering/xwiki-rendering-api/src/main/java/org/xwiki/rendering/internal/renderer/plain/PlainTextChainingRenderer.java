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
package org.xwiki.rendering.internal.renderer.plain;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.EmptyBlockChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.renderer.link.LinkLabelGenerator;
import org.xwiki.rendering.renderer.AbstractChainingPrintRenderer;

/**
 * Print only plain text information. For example it remove anything which need a specific syntax a simple plain text
 * editor can't support like the style, link, image, etc. This renderer is mainly used to generate a simple as possible
 * label like in a TOC.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class PlainTextChainingRenderer extends AbstractChainingPrintRenderer
{
    /**
     * New Line character.
     */
    private static final String NL = "\n";

    private boolean isFirstElementRendered;

    /**
     * Generate link label.
     */
    private LinkLabelGenerator linkLabelGenerator;

    /**
     * The plain text renderer supports when no link label generator is set.
     */
    public PlainTextChainingRenderer(ListenerChain listenerChain)
    {
        this(null, listenerChain);
    }

    public PlainTextChainingRenderer(LinkLabelGenerator linkLabelGenerator, ListenerChain listenerChain)
    {
        setListenerChain(listenerChain);

        this.linkLabelGenerator = linkLabelGenerator;
    }

    // State

    private BlockStateChainingListener getBlockState()
    {
        return (BlockStateChainingListener) getListenerChain().getListener(BlockStateChainingListener.class);
    }

    protected EmptyBlockChainingListener getEmptyBlockState()
    {
        return (EmptyBlockChainingListener) getListenerChain().getListener(EmptyBlockChainingListener.class);
    }

    // Events

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginParagraph(java.util.Map)
     */
    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        printEmptyLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#onNewLine()
     */
    @Override
    public void onNewLine()
    {
        getPrinter().print(NL);
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
        if (getEmptyBlockState().isCurrentContainerBlockEmpty()) {
            if (link.getType() == LinkType.DOCUMENT && this.linkLabelGenerator != null) {
                getPrinter().print(this.linkLabelGenerator.generate(link));
            } else {
                getPrinter().print(link.getReference());
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginHeader(org.xwiki.rendering.listener.HeaderLevel,
     *      String, java.util.Map)
     */
    @Override
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        printEmptyLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#onWord(java.lang.String)
     */
    @Override
    public void onWord(String word)
    {
        getPrinter().print(word);
    }

    @Override
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        if (getBlockState().getListDepth() == 1) {
            printEmptyLine();
        } else {
            getPrinter().print(NL);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginListItem()
     */
    @Override
    public void beginListItem()
    {
        if (getBlockState().getListItemIndex() > 0) {
            getPrinter().print(NL);
        }

        // TODO: maybe add some syntax here like a - or not
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#onSpace()
     */
    @Override
    public void onSpace()
    {
        getPrinter().print(" ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#onSpecialSymbol(char)
     */
    @Override
    public void onSpecialSymbol(char symbol)
    {
        getPrinter().print(String.valueOf(symbol));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#onHorizontalLine(java.util.Map)
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
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#onEmptyLines(int)
     */
    @Override
    public void onEmptyLines(int count)
    {
        getPrinter().print(StringUtils.repeat(NL, count));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#onVerbatim(String, boolean, Map)
     */
    @Override
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        getPrinter().print(protectedString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList(java.util.Map)
     * @since 2.0RC1
     */
    @Override
    public void beginDefinitionList(Map<String, String> parameters)
    {
        if (getBlockState().getDefinitionListDepth() == 1 && !getBlockState().isInList()) {
            printEmptyLine();
        } else {
            getPrinter().print(NL);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginDefinitionTerm()
     */
    @Override
    public void beginDefinitionTerm()
    {
        if (getBlockState().getDefinitionListItemIndex() > 0) {
            getPrinter().print(NL);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginDefinitionDescription()
     */
    @Override
    public void beginDefinitionDescription()
    {
        if (getBlockState().getDefinitionListItemIndex() > 0) {
            getPrinter().print(NL);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginQuotationLine()
     */
    @Override
    public void beginQuotationLine()
    {
        if (getBlockState().getQuotationLineIndex() > 0) {
            getPrinter().print(NL);
        } else {
            printEmptyLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginTable(java.util.Map)
     */
    @Override
    public void beginTable(Map<String, String> parameters)
    {
        printEmptyLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginTableCell(java.util.Map)
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
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginTableHeadCell(java.util.Map)
     */
    @Override
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        beginTableCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginTableRow(java.util.Map)
     */
    @Override
    public void beginTableRow(Map<String, String> parameters)
    {
        if (getBlockState().getCellRow() > 0) {
            getPrinter().print(NL);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#onImage(org.xwiki.rendering.listener.Image,
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
            getPrinter().print(NL + NL);
        } else {
            this.isFirstElementRendered = true;
        }
    }
}
