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
package org.xwiki.rendering.internal.parser.doxia;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.apache.maven.doxia.logging.Log;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.xwiki.rendering.block.AbstractBlock;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BulletedListBlock;
import org.xwiki.rendering.block.DefinitionDescriptionBlock;
import org.xwiki.rendering.block.DefinitionListBlock;
import org.xwiki.rendering.block.DefinitionTermBlock;
import org.xwiki.rendering.block.EmptyLinesBlock;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.HorizontalLineBlock;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.NumberedListBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.TableCellBlock;
import org.xwiki.rendering.block.TableHeadCellBlock;
import org.xwiki.rendering.block.TableRowBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.URLImage;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.util.IdGenerator;

/**
 * Doxia Sink that generates a XWiki {@link XDOM} object containing page Blocks.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XDOMGeneratorSink implements Sink
{
    private LinkParser linkParser;

    /**
     * Used to render Bocks into plain text for computing unique HTML ids for Headers.
     */
    private BlockRenderer plainTextBlockRenderer;

    /**
     * Used to parse Doxia raw text into XDOM blocks.
     */
    private Parser plainTextParser;
    
    private Stack<Block> stack = new Stack<Block>();

    private final MarkerBlock marker = new MarkerBlock();

    private IdGenerator idGenerator = new IdGenerator();

    private class MarkerBlock extends AbstractBlock
    {
        public Object param1;
        
        public void traverse(Listener listener)
        {
        }
    }
    
    private MarkerBlock currentMarker;

    /**
     * @since 2.0M3
     */
    public XDOMGeneratorSink(LinkParser linkParser, Parser plainTextParser, BlockRenderer plainTextBlockRenderer)
    {
        this.linkParser = linkParser;
        this.plainTextBlockRenderer = plainTextBlockRenderer;
        this.plainTextParser = plainTextParser;
    }

    public XDOM getXDOM()
    {
        return new XDOM(generateListFromStack(), this.idGenerator);
    }

    /**
     * {@inheritDoc}
     * @see Sink#enableLogging(Log)
     */
    public void enableLogging(Log arg0)
    {
        // Not used.
    }

    /**
     * {@inheritDoc}
     * @see Sink#anchor(String, SinkEventAttributes)
     */
    public void anchor(String name, SinkEventAttributes attributes)
    {
        // Limitation: XWiki doesn't use parameters on this Block.
        this.stack.push(new IdBlock(name));
    }

    /**
     * {@inheritDoc}
     * @see Sink#anchor(String)
     */
    public void anchor(String name)
    {
        anchor(name, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#anchor_()
     */
    public void anchor_()
    {
        // Nothing to do since for XWiki anchors don't have children and thus the XWiki Block is generated in the Sink
        // anchor start event
    }

    /**
     * {@inheritDoc}
     * @see Sink#author(SinkEventAttributes)
     */
    public void author(SinkEventAttributes attributes)
    {
        // XWiki's Listener model doesn't support authors. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * @see Sink#author()
     */
    public void author()
    {
        // XWiki's Listener model doesn't support authors. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * @see Sink#author_()
     */
    public void author_()
    {
        // XWiki's Listener model doesn't support authors. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * @see Sink#body(SinkEventAttributes)
     */
    public void body(SinkEventAttributes attributes)
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * @see Sink#body()
     */
    public void body()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * @see Sink#body_()
     */
    public void body_()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * @see Sink#bold()
     */
    public void bold()
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#bold_()
     */
    public void bold_()
    {
        this.stack.push(new FormatBlock(generateListFromStack(), Format.BOLD));
    }

    /**
     * {@inheritDoc}
     * @see Sink#close()
     */
    public void close()
    {
        // Not used.
    }

    /**
     * {@inheritDoc}
     * @see Sink#comment(String)
     */
    public void comment(String comment)
    {
        // TODO: Not supported yet by the XDOM.
    }

    /**
     * {@inheritDoc}
     * @see Sink#date(SinkEventAttributes)
     */
    public void date(SinkEventAttributes attributes)
    {
        // XWiki's Listener model doesn't support dates. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * @see Sink#date()
     */
    public void date()
    {
        // XWiki's Listener model doesn't support dates. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * @see Sink#date_()
     */
    public void date_()
    {
        // XWiki's Listener model doesn't support dates. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * @see Sink#definedTerm(SinkEventAttributes)
     */
    public void definedTerm(SinkEventAttributes attributes)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#definedTerm()
     */
    public void definedTerm()
    {
        definedTerm(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#definedTerm_()
     */
    public void definedTerm_()
    {
        // Limitation: XWiki doesn't use parameters on this Block.
        this.stack.push(new DefinitionTermBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#definition(SinkEventAttributes)
     */
    public void definition(SinkEventAttributes attributes)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#definition()
     */
    public void definition()
    {
        definition(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#definition()
     */
    public void definition_()
    {
        // Limitation: XWiki doesn't use parameters on this Block.
        this.stack.push(new DefinitionDescriptionBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#definitionList(SinkEventAttributes)
     */
    public void definitionList(SinkEventAttributes attributes)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#definitionList()
     */
    public void definitionList()
    {
        definitionList(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#definitionList_()
     */
    public void definitionList_()
    {
        // TODO: Handle parameters
        this.stack.push(new DefinitionListBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#definitionListItem(SinkEventAttributes)
     */
    public void definitionListItem(SinkEventAttributes attributes)
    {
        // Nothing to do since for XWiki the definition list items are the definition term/descriptions.
    }

    /**
     * {@inheritDoc}
     * @see Sink#definitionListItem()
     */
    public void definitionListItem()
    {
        // Nothing to do since for XWiki the definition list items are the definition term/descriptions.
    }

    /**
     * {@inheritDoc}
     * @see Sink#definitionListItem_()
     */
    public void definitionListItem_()
    {
        // Nothing to do since for XWiki the definition list items are the definition term/descriptions.
    }

    /**
     * {@inheritDoc}
     * @see Sink#figure(SinkEventAttributes)
     */
    public void figure(SinkEventAttributes attributes)
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * @see Sink#figure()
     */
    public void figure()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * @see Sink#figure_()
     */
    public void figure_()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * @see Sink#figureCaption(SinkEventAttributes)
     */
    public void figureCaption(SinkEventAttributes attributes)
    {
        // TODO: Handle caption as parameters in the future
    }

    /**
     * {@inheritDoc}
     * @see Sink#figureCaption()
     */
    public void figureCaption()
    {
        figureCaption(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#figureCaption_()
     */
    public void figureCaption_()
    {
        // TODO: Handle caption as parameters in the future
    }

    /**
     * {@inheritDoc}
     * @see Sink#figureGraphics(String, SinkEventAttributes)
     */
    public void figureGraphics(String source, SinkEventAttributes attributes)
    {
        // TODO: Handle image to attachments. For now we only handle URLs.
        Image image = new URLImage(source);
        this.stack.push(new ImageBlock(image, false));
    }

    /**
     * {@inheritDoc}
     * @see Sink#figureGraphics(String)
     */
    public void figureGraphics(String source)
    {
        figureGraphics(source, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#flush()
     */
    public void flush()
    {
        // Not used.
    }

    /**
     * {@inheritDoc}
     * @see Sink#head(SinkEventAttributes)
     */
    public void head(SinkEventAttributes sinkEventAttributes)
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * @see Sink#head()
     */
    public void head()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * @see Sink#head_()
     */
    public void head_()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * @see Sink#horizontalRule(SinkEventAttributes)
     */
    public void horizontalRule(SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        this.stack.push(new HorizontalLineBlock());
    }

    /**
     * {@inheritDoc}
     * @see Sink#horizontalRule()
     */
    public void horizontalRule()
    {
        horizontalRule(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#italic()
     */
    public void italic()
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#italic_()
     */
    public void italic_()
    {
        this.stack.push(new FormatBlock(generateListFromStack(), Format.ITALIC));
    }

    /**
     * {@inheritDoc}
     * @see Sink#lineBreak(SinkEventAttributes)
     */
    public void lineBreak(SinkEventAttributes attributes)
    {
        // If the previous block is an EmptyLineBlock, increase the line count
        if (this.stack.peek() instanceof EmptyLinesBlock) {
            EmptyLinesBlock block = (EmptyLinesBlock) this.stack.peek();
            block.setEmptyLinesCount(block.getEmptyLinesCount() + 1);
        } else if ((this.stack.peek() instanceof NewLineBlock)
            && (this.stack.get(this.stack.size() - 1) instanceof NewLineBlock))
        {
            // If the past 2 blocks are already linebreaks, then send an EmptyLinesBlock
            this.stack.push(new EmptyLinesBlock(1));
        } else {
            this.stack.push(NewLineBlock.NEW_LINE_BLOCK);
        }
    }

    /**
     * {@inheritDoc}
     * @see Sink#lineBreak()
     */
    public void lineBreak()
    {
        lineBreak(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#link(String, SinkEventAttributes)
     */
    public void link(String name, SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        MarkerBlock marker = new MarkerBlock();
        marker.param1 = name;
        this.stack.push(marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#link(String)
     */
    public void link(String name)
    {
        link(name, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#link_()
     */
    public void link_()
    {
        List<Block> children = generateListFromStack();

        // If there's no link parser defined, don't handle links and images...
        if (this.linkParser != null) {
            Link link = this.linkParser.parse((String) this.currentMarker.param1);
            // TODO: Handle freestanding links
            this.stack.push(new LinkBlock(children, link, false));
        }
    }

    /**
     * {@inheritDoc}
     * @see Sink#list()
     */
    public void list(SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#list()
     */
    public void list()
    {
        list(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#list_()
     */
    public void list_()
    {
        this.stack.push(new BulletedListBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#listItem(SinkEventAttributes)
     */
    public void listItem(SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#listItem()
     */
    public void listItem()
    {
        listItem(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#listItem_()
     */
    public void listItem_()
    {
        this.stack.push(new ListItemBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#monospaced()
     */
    public void monospaced()
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#monospaced_()
     */
    public void monospaced_()
    {
        this.stack.push(new FormatBlock(generateListFromStack(), Format.MONOSPACE));
    }

    /**
     * {@inheritDoc}
     * @see Sink#nonBreakingSpace()
     */
    public void nonBreakingSpace()
    {
        // In XWiki all spaces are non breakable
        this.stack.push(SpaceBlock.SPACE_BLOCK);
    }

    /**
     * {@inheritDoc}
     * @see Sink#numberedList(int, SinkEventAttributes)
     */
    public void numberedList(int numbering, SinkEventAttributes sinkEventAttributes)
    {
        // TODO: Handle parameters
        MarkerBlock marker = new MarkerBlock();
        marker.param1 = numbering;
        this.stack.push(marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#numberedList(int)
     */
    public void numberedList(int numbering)
    {
        numberedList(numbering, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#numberedList_()
     */
    public void numberedList_()
    {
        List<Block> children = generateListFromStack();
        // TODO: Handle numbering types
        this.stack.push(new NumberedListBlock(children));
    }

    /**
     * {@inheritDoc}
     * @see Sink#numberedListItem(SinkEventAttributes)
     */
    public void numberedListItem(SinkEventAttributes attributes)
    {
        // TODO: handle parameters
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#numberedListItem()
     */
    public void numberedListItem()
    {
        numberedListItem(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#numberedListItem_()
     */
    public void numberedListItem_()
    {
        this.stack.push(new ListItemBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#pageBreak()
     */
    public void pageBreak()
    {
        // Not supported in XWiki.
    }

    /**
     * {@inheritDoc}
     * @see Sink#paragraph(SinkEventAttributes)
     */
    public void paragraph(SinkEventAttributes attributes)
    {
        // TODO: handle parameters
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#paragraph()
     */
    public void paragraph()
    {
        paragraph(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#paragraph_()
     */
    public void paragraph_()
    {
        this.stack.push(new ParagraphBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#rawText(String)
     */
    public void rawText(String text)
    {
        // Parse the text using the plain text parser
        try {
            System.out.println("text= " + text);
            for (Block block : this.plainTextParser.parse(new StringReader(text)).getChildren()) {
                this.stack.push(block);
            }
        } catch (ParseException e) {
            // Shouldn't happen since we use a StringReader which shouldn't generate any IO.
            throw new RuntimeException("Failed to parse raw text [" + text + "]", e);
        }
    }

    /**
     * {@inheritDoc}
     * @see Sink#section(int, SinkEventAttributes)
     */
    public void section(int level, SinkEventAttributes attributes)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#section_(int)
     */
    public void section_(int level)
    {
        this.stack.push(new SectionBlock(generateListFromStack()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#section1()
     */
    public void section1()
    {
        section(1, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#section1_()
     */
    public void section1_()
    {
        section_(1);
    }

    /**
     * {@inheritDoc}
     * @see Sink#section2()
     */
    public void section2()
    {
        section(2, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#section2_()
     */
    public void section2_()
    {
        section_(2);
    }

    /**
     * {@inheritDoc}
     * @see Sink#section3()
     */
    public void section3()
    {
        section(3, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#section3_()
     */
    public void section3_()
    {
        section_(3);
    }

    /**
     * {@inheritDoc}
     * @see Sink#section4()
     */
    public void section4()
    {
        section(4, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#section4_()
     */
    public void section4_()
    {
        section_(4);
    }

    /**
     * {@inheritDoc}
     * @see Sink#section5()
     */
    public void section5()
    {
        section(5, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#section5_()
     */
    public void section5_()
    {
        section_(5);
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle(int, SinkEventAttributes)
     */
    public void sectionTitle(int level, SinkEventAttributes attributes)
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle()
     */
    public void sectionTitle()
    {
        // Should be deprecated in Doxia
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle_(int)
     */
    public void sectionTitle_(int level)
    {
        List<Block> children = generateListFromStack();
        WikiPrinter printer = new DefaultWikiPrinter();
        this.plainTextBlockRenderer.render(children, printer);
        String id = "H" + this.idGenerator.generateUniqueId(printer.toString());

        List<Block> headerTitleBlocks = generateListFromStack();
        
        this.stack.push(new HeaderBlock(headerTitleBlocks, HeaderLevel.parseInt(level), id));
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle_()
     */
    public void sectionTitle_()
    {
        // Should be deprecated in Doxia
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle1()
     */
    public void sectionTitle1()
    {
        sectionTitle(1, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle1_()
     */
    public void sectionTitle1_()
    {
        sectionTitle_(1);
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle2()
     */
    public void sectionTitle2()
    {
        sectionTitle(2, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle2_()
     */
    public void sectionTitle2_()
    {
        sectionTitle_(2);
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle3()
     */
    public void sectionTitle3()
    {
        sectionTitle(3, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle3_()
     */
    public void sectionTitle3_()
    {
        sectionTitle_(3);
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle4()
     */
    public void sectionTitle4()
    {
        sectionTitle(4, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle4_()
     */
    public void sectionTitle4_()
    {
        sectionTitle_(4);
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle5()
     */
    public void sectionTitle5()
    {
        sectionTitle(5, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#sectionTitle5_()
     */
    public void sectionTitle5_()
    {
        sectionTitle_(5);
    }

    /**
     * {@inheritDoc}
     * @see Sink#table(SinkEventAttributes)
     */
    public void table(SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#table()
     */
    public void table()
    {
        table(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#table_()
     */
    public void table_()
    {
        this.stack.push(new TableBlock(generateListFromStack(), Collections.<String, String>emptyMap()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableCaption(SinkEventAttributes)
     */
    public void tableCaption(SinkEventAttributes attributes)
    {
        // TODO: Handle this
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableCaption()
     */
    public void tableCaption()
    {
        tableCaption(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableCaption_()
     */
    public void tableCaption_()
    {
        // TODO: Handle this
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableCell(SinkEventAttributes)
     */
    public void tableCell(SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableCell()
     */
    public void tableCell()
    {
        tableCell((SinkEventAttributes) null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableCell(String)
     */
    public void tableCell(String width)
    {
        // TODO: Handle width
        tableCell((SinkEventAttributes) null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableCell_()
     */
    public void tableCell_()
    {
        this.stack.push(new TableCellBlock(generateListFromStack(), Collections.<String, String>emptyMap()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableHeaderCell(SinkEventAttributes)
     */
    public void tableHeaderCell(SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableHeaderCell()
     */
    public void tableHeaderCell()
    {
        tableHeaderCell((SinkEventAttributes) null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableHeaderCell(String)
     */
    public void tableHeaderCell(String width)
    {
        // TODO: Handle width
        tableHeaderCell((SinkEventAttributes) null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableHeaderCell_()
     */
    public void tableHeaderCell_()
    {
        this.stack.push(new TableHeadCellBlock(generateListFromStack(), Collections.<String, String>emptyMap()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableRow(SinkEventAttributes)
     */
    public void tableRow(SinkEventAttributes attributes)
    {
        // TODO: Handle parameters
        this.stack.push(this.marker);
    }
    
    /**
     * {@inheritDoc}
     * @see Sink#tableRow()
     */
    public void tableRow()
    {
        tableRow(null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableRow_()
     */
    public void tableRow_()
    {
        this.stack.push(new TableRowBlock(generateListFromStack(), Collections.<String, String>emptyMap()));
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableRows(int[], boolean)
     */
    public void tableRows(int[] arg0, boolean arg1)
    {
        // Not supported by XWiki.
    }

    /**
     * {@inheritDoc}
     * @see Sink#tableRows_()
     */
    public void tableRows_()
    {
        // Not supported by XWiki.
    }

    /**
     * {@inheritDoc}
     * @see Sink#text(String, SinkEventAttributes)
     */
    public void text(String text, SinkEventAttributes attributes)
    {
        // TODO Handle parameters
        // Since Doxia doesn't generate events at the word level we need to reparse the
        // text to extract spaces, special symbols and words.
        rawText(text);
    }

    /**
     * {@inheritDoc}
     * @see Sink#text(String)
     */
    public void text(String text)
    {
        text(text, null);
    }

    /**
     * {@inheritDoc}
     * @see Sink#title(SinkEventAttributes)
     */
    public void title(SinkEventAttributes attributes)
    {
        // XWiki's Listener model doesn't support titles. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * @see Sink#title()
     */
    public void title()
    {
        // XWiki's Listener model doesn't support titles. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * @see Sink#title_()
     */
    public void title_()
    {
        // XWiki's Listener model doesn't support titles. Don't do anything.
    }

    /**
     * {@inheritDoc}
     * @see Sink#verbatim(SinkEventAttributes)
     */
    public void verbatim(SinkEventAttributes attributes)
    {
        // Nothing to do since whitespaces are significant in the XDOM.
    }

    /**
     * {@inheritDoc}
     * @see Sink#verbatim(boolean)
     */
    public void verbatim(boolean boxed)
    {
        // Nothing to do since whitespaces are significant in the XDOM.
    }

    /**
     * {@inheritDoc}
     * @see Sink#verbatim_()
     */
    public void verbatim_()
    {
        // Nothing to do since whitespaces are significant in the XDOM.
    }

    /**
     * {@inheritDoc}
     * @see Sink#unknown(String, Object[], SinkEventAttributes)
     */
    public void unknown(String arg0, Object[] arg1, SinkEventAttributes arg2)
    {
        // TODO: Not supported yet by the XDOM. 
    }

    private List<Block> generateListFromStack()
    {
        List<Block> blocks = new ArrayList<Block>();
        while (!this.stack.empty()) {
            if (this.stack.peek() != this.marker) {
                blocks.add(this.stack.pop());
            } else {
                // Remove marker and save it so that it can be accessed
                this.currentMarker = (MarkerBlock) this.stack.pop();
                break;
            }
        }
        Collections.reverse(blocks);
        return blocks;
    }
}
