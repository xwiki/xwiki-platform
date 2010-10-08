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
package org.xwiki.rendering.internal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.xwiki.rendering.block.AbstractBlock;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BulletedListBlock;
import org.xwiki.rendering.block.DefinitionDescriptionBlock;
import org.xwiki.rendering.block.DefinitionListBlock;
import org.xwiki.rendering.block.DefinitionTermBlock;
import org.xwiki.rendering.block.EmptyLinesBlock;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.HorizontalLineBlock;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.NumberedListBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.QuotationBlock;
import org.xwiki.rendering.block.QuotationLineBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.TableCellBlock;
import org.xwiki.rendering.block.TableHeadCellBlock;
import org.xwiki.rendering.block.TableRowBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.ResourceReference;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Produce a {@link XDOM} based on events.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class XDOMGeneratorListener implements Listener
{
    private Stack<Block> stack = new Stack<Block>();

    private final MarkerBlock marker = new MarkerBlock();

    private static class MarkerBlock extends AbstractBlock
    {
        /**
         * {@inheritDoc}
         * 
         * @see AbstractBlock#traverse(Listener)
         */
        public void traverse(Listener listener)
        {
            // Nothing to do since this block is only used as a marker.
        }
    }

    public XDOM getXDOM()
    {
        return new XDOM(generateListFromStack());
    }

    private List<Block> generateListFromStack()
    {
        List<Block> blocks = new ArrayList<Block>();
        while (!this.stack.empty()) {
            if (this.stack.peek() != this.marker) {
                blocks.add(this.stack.pop());
            } else {
                this.stack.pop();
                break;
            }
        }
        Collections.reverse(blocks);
        return blocks;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     */
    public void beginDefinitionDescription()
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList(java.util.Map)
     */
    public void beginDefinitionList(Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     */
    public void beginDefinitionTerm()
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDocument(java.util.Map)
     */
    public void beginDocument(Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginFormat(org.xwiki.rendering.listener.Format, java.util.Map)
     */
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginGroup(java.util.Map)
     */
    public void beginGroup(Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginHeader(org.xwiki.rendering.listener.HeaderLevel,
     *      java.lang.String, java.util.Map)
     */
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginListItem()
     */
    public void beginListItem()
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginMacroMarker(java.lang.String, java.util.Map, java.lang.String,
     *      boolean)
     */
    public void beginMacroMarker(String name, Map<String, String> macroParameters, String content, boolean isInline)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginParagraph(java.util.Map)
     */
    public void beginParagraph(Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     */
    public void beginQuotationLine()
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginSection(java.util.Map)
     */
    public void beginSection(Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTable(java.util.Map)
     */
    public void beginTable(Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableCell(java.util.Map)
     */
    public void beginTableCell(Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableHeadCell(java.util.Map)
     */
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableRow(java.util.Map)
     */
    public void beginTableRow(Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.LinkListener#beginLink(org.xwiki.rendering.listener.ResourceReference ,
     *      boolean, java.util.Map)
     */
    public void beginLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     */
    public void endDefinitionDescription()
    {
        this.stack.push(new DefinitionDescriptionBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList(java.util.Map)
     */
    public void endDefinitionList(Map<String, String> parameters)
    {
        this.stack.push(new DefinitionListBlock(generateListFromStack(), parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     */
    public void endDefinitionTerm()
    {
        this.stack.push(new DefinitionTermBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDocument(java.util.Map)
     */
    public void endDocument(Map<String, String> parameters)
    {
        // Do nothing. This is supposed to append only once for the hole document
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endFormat(org.xwiki.rendering.listener.Format, java.util.Map)
     */
    public void endFormat(Format format, Map<String, String> parameters)
    {
        this.stack.push(new FormatBlock(generateListFromStack(), format, parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endGroup(java.util.Map)
     */
    public void endGroup(Map<String, String> parameters)
    {
        this.stack.push(new GroupBlock(generateListFromStack(), parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endHeader(org.xwiki.rendering.listener.HeaderLevel, java.lang.String,
     *      java.util.Map)
     */
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        this.stack.push(new HeaderBlock(generateListFromStack(), level, parameters, id));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void endList(ListType listType, Map<String, String> parameters)
    {
        if (listType == ListType.BULLETED) {
            this.stack.push(new BulletedListBlock(generateListFromStack(), parameters));
        } else {
            this.stack.push(new NumberedListBlock(generateListFromStack(), parameters));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endListItem()
     */
    public void endListItem()
    {
        this.stack.push(new ListItemBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endMacroMarker(java.lang.String, java.util.Map, java.lang.String,
     *      boolean)
     */
    public void endMacroMarker(String name, Map<String, String> macroParameters, String content, boolean isInline)
    {
        this.stack.push(new MacroMarkerBlock(name, macroParameters, content, generateListFromStack(), isInline));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endParagraph(java.util.Map)
     */
    public void endParagraph(Map<String, String> parameters)
    {
        this.stack.push(new ParagraphBlock(generateListFromStack(), parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     */
    public void endQuotation(Map<String, String> parameters)
    {
        this.stack.push(new QuotationBlock(generateListFromStack(), parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     */
    public void endQuotationLine()
    {
        this.stack.push(new QuotationLineBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endSection(java.util.Map)
     */
    public void endSection(Map<String, String> parameters)
    {
        this.stack.push(new SectionBlock(generateListFromStack(), parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTable(java.util.Map)
     */
    public void endTable(Map<String, String> parameters)
    {
        this.stack.push(new TableBlock(generateListFromStack(), parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableCell(java.util.Map)
     */
    public void endTableCell(Map<String, String> parameters)
    {
        this.stack.push(new TableCellBlock(generateListFromStack(), parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableHeadCell(java.util.Map)
     */
    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.stack.push(new TableHeadCellBlock(generateListFromStack(), parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableRow(java.util.Map)
     */
    public void endTableRow(Map<String, String> parameters)
    {
        this.stack.push(new TableRowBlock(generateListFromStack(), parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.LinkListener#endLink(org.xwiki.rendering.listener.ResourceReference ,
     *      boolean, java.util.Map)
     */
    public void endLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.stack.push(new LinkBlock(generateListFromStack(), reference, isFreeStandingURI, parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        this.stack.push(new EmptyLinesBlock(count));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine(java.util.Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.stack.push(new HorizontalLineBlock(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onId(java.lang.String)
     */
    public void onId(String name)
    {
        this.stack.push(new IdBlock(name));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onMacro(java.lang.String, java.util.Map, java.lang.String, boolean)
     */
    public void onMacro(String id, Map<String, String> macroParameters, String content, boolean isInline)
    {
        this.stack.push(new MacroBlock(id, macroParameters, content, isInline));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onNewLine()
     */
    public void onNewLine()
    {
        this.stack.push(NewLineBlock.NEW_LINE_BLOCK);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onRawText(java.lang.String, org.xwiki.rendering.syntax.Syntax)
     */
    public void onRawText(String rawContent, Syntax syntax)
    {
        this.stack.push(new RawBlock(rawContent, syntax));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onSpace()
     */
    public void onSpace()
    {
        this.stack.push(SpaceBlock.SPACE_BLOCK);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onSpecialSymbol(char)
     */
    public void onSpecialSymbol(char symbol)
    {
        this.stack.push(new SpecialSymbolBlock(symbol));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatim(java.lang.String, boolean, java.util.Map)
     */
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        this.stack.push(new VerbatimBlock(protectedString, parameters, isInline));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onWord(java.lang.String)
     */
    public void onWord(String word)
    {
        this.stack.push(new WordBlock(word));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.ImageListener#onImage(org.xwiki.rendering.listener.ResourceReference,
     *      boolean, java.util.Map)
     */
    public void onImage(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.stack.push(new ImageBlock(reference, isFreeStandingURI, parameters));
    }
}
