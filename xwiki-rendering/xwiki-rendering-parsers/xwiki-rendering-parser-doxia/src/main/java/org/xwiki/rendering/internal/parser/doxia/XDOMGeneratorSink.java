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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.doxia.sink.Sink;
import org.xwiki.rendering.block.AbstractBlock;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BulletedListBlock;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.HorizontalLineBlock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.NumberedListBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.util.IdGenerator;
import org.xwiki.rendering.util.RenderersUtils;

/**
 * Doxia Sink that generates a XWiki {@link XDOM} object containing page Blocks.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XDOMGeneratorSink implements Sink
{
    private LinkParser linkParser;

    private Stack<Block> stack = new Stack<Block>();

    private final MarkerBlock marker = new MarkerBlock();

    private static final Pattern SPLIT_TEXT_PATTERN =
        Pattern.compile("(\\w+)?([ <>=.\"\\?\\*!#\\$%'\\(\\)\\+,/:;@\\[\\]\\\\^_`\\{\\}\\|~])?");

    private IdGenerator idGenerator = new IdGenerator();

    private RenderersUtils renderersUtils = new RenderersUtils();

    private class MarkerBlock extends AbstractBlock
    {
        public void traverse(Listener listener)
        {
        }
    }

    public XDOMGeneratorSink(LinkParser linkParser)
    {
        this.linkParser = linkParser;
    }

    public XDOM getDOM()
    {
        return new XDOM(generateListFromStack(), this.idGenerator);
    }

    public void anchor(String arg0)
    {
        // TODO Auto-generated method stub

    }

    public void anchor_()
    {
        // TODO Auto-generated method stub

    }

    public void author()
    {
        // TODO Auto-generated method stub

    }

    public void author_()
    {
        // TODO Auto-generated method stub

    }

    public void body()
    {
        // TODO Auto-generated method stub

    }

    public void body_()
    {
        // TODO Auto-generated method stub

    }

    public void bold()
    {
        this.stack.push(this.marker);
    }

    public void bold_()
    {
        this.stack.push(new FormatBlock(generateListFromStack(), Format.BOLD));
    }

    public void close()
    {
        // TODO Auto-generated method stub

    }

    public void date()
    {
        // TODO Auto-generated method stub

    }

    public void date_()
    {
        // TODO Auto-generated method stub

    }

    public void definedTerm()
    {
        // TODO Auto-generated method stub

    }

    public void definedTerm_()
    {
        // TODO Auto-generated method stub

    }

    public void definition()
    {
        // TODO Auto-generated method stub

    }

    public void definitionList()
    {
        // TODO Auto-generated method stub

    }

    public void definitionListItem()
    {
        // TODO Auto-generated method stub

    }

    public void definitionListItem_()
    {
        // TODO Auto-generated method stub

    }

    public void definitionList_()
    {
        // TODO Auto-generated method stub

    }

    public void definition_()
    {
        // TODO Auto-generated method stub

    }

    public void figure()
    {
        // TODO Auto-generated method stub

    }

    public void figureCaption()
    {
        // TODO Auto-generated method stub

    }

    public void figureCaption_()
    {
        // TODO Auto-generated method stub

    }

    public void figureGraphics(String arg0)
    {
        // TODO Auto-generated method stub

    }

    public void figure_()
    {
        // TODO Auto-generated method stub

    }

    public void flush()
    {
        // TODO Auto-generated method stub

    }

    public void head()
    {
        // TODO Auto-generated method stub

    }

    public void head_()
    {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.doxia.sink.Sink#horizontalRule()
     */
    public void horizontalRule()
    {
        this.stack.push(new HorizontalLineBlock());
    }

    public void italic()
    {
        this.stack.push(this.marker);
    }

    public void italic_()
    {
        this.stack.push(new FormatBlock(generateListFromStack(), Format.ITALIC));
    }

    public void lineBreak()
    {
        // TODO Auto-generated method stub

    }

    public void link(String arg0)
    {
        // TODO Auto-generated method stub

    }

    public void link_()
    {
        // TODO Auto-generated method stub

    }

    public void list()
    {
        this.stack.push(this.marker);
    }

    public void listItem()
    {
        this.stack.push(this.marker);
    }

    public void listItem_()
    {
        // Note: This means we support Paragraphs inside lists.
        this.stack.push(new ListItemBlock(generateListFromStack()));
    }

    public void list_()
    {
        this.stack.push(new BulletedListBlock(generateListFromStack()));
    }

    public void monospaced()
    {
        // TODO Auto-generated method stub

    }

    public void monospaced_()
    {
        // TODO Auto-generated method stub

    }

    public void nonBreakingSpace()
    {
        // TODO Auto-generated method stub

    }

    public void numberedList(int arg0)
    {
        this.stack.push(this.marker);
    }

    public void numberedListItem()
    {
        this.stack.push(this.marker);
    }

    public void numberedListItem_()
    {
        // Note: This means we support Paragraphs inside lists.
        this.stack.push(new ListItemBlock(generateListFromStack()));
    }

    public void numberedList_()
    {
        this.stack.push(new NumberedListBlock(generateListFromStack()));
    }

    public void pageBreak()
    {
        // TODO Auto-generated method stub

    }

    public void paragraph()
    {
        this.stack.push(this.marker);
    }

    public void paragraph_()
    {
        this.stack.push(new ParagraphBlock(generateListFromStack()));
    }

    public void rawText(String text)
    {
    }

    public void section1()
    {
        this.stack.push(this.marker);
    }

    public void section1_()
    {
        List<Block> children = generateListFromStack();
        String id = "H" + this.idGenerator.generateUniqueId(this.renderersUtils.renderPlainText(children));

        List<Block> headerTitleBlocks = generateListFromStack();
        this.stack.push(new HeaderBlock(headerTitleBlocks, HeaderLevel.LEVEL1, id));
    }

    public void section2()
    {
        // No need for a marker since we expect only a single TextBlock
    }

    public void section2_()
    {
        // TODO Auto-generated method stub

    }

    public void section3()
    {
        // TODO Auto-generated method stub

    }

    public void section3_()
    {
        // TODO Auto-generated method stub

    }

    public void section4()
    {
        // TODO Auto-generated method stub

    }

    public void section4_()
    {
        // TODO Auto-generated method stub

    }

    public void section5()
    {
        // TODO Auto-generated method stub

    }

    public void section5_()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle1()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle1_()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle2()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle2_()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle3()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle3_()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle4()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle4_()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle5()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle5_()
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle_()
    {
        // TODO Auto-generated method stub

    }

    public void table()
    {
        // TODO Auto-generated method stub

    }

    public void tableCaption()
    {
        // TODO Auto-generated method stub

    }

    public void tableCaption_()
    {
        // TODO Auto-generated method stub

    }

    public void tableCell()
    {
        // TODO Auto-generated method stub

    }

    public void tableCell(String arg0)
    {
        // TODO Auto-generated method stub

    }

    public void tableCell_()
    {
        // TODO Auto-generated method stub

    }

    public void tableHeaderCell()
    {
        // TODO Auto-generated method stub

    }

    public void tableHeaderCell(String arg0)
    {
        // TODO Auto-generated method stub

    }

    public void tableHeaderCell_()
    {
        // TODO Auto-generated method stub

    }

    public void tableRow()
    {
        // TODO Auto-generated method stub

    }

    public void tableRow_()
    {
        // TODO Auto-generated method stub

    }

    public void tableRows(int[] arg0, boolean arg1)
    {
        // TODO Auto-generated method stub

    }

    public void tableRows_()
    {
        // TODO Auto-generated method stub

    }

    public void table_()
    {
        // TODO Auto-generated method stub

    }

    public void text(String text)
    {
        // Since Doxia doesn't generate events at the word level we need to reparse the
        // text to extract spaces, special symbols and words.
        Matcher matcher = SPLIT_TEXT_PATTERN.matcher(text);
        while (matcher.find()) {
            String word = matcher.group(1);
            if (word != null) {
                this.stack.push(new WordBlock(word));
            }
            String symbol = matcher.group(2);
            if (symbol != null) {
                if (symbol.equals(" ")) {
                    this.stack.push(SpaceBlock.SPACE_BLOCK);
                } else {
                    this.stack.push(new SpecialSymbolBlock(symbol.charAt(0)));
                }
            }
        }
    }

    public void title()
    {
        // TODO Auto-generated method stub

    }

    public void title_()
    {
        // TODO Auto-generated method stub

    }

    public void verbatim(boolean arg0)
    {
        // TODO Auto-generated method stub

    }

    public void verbatim_()
    {
        // TODO Auto-generated method stub

    }

    public void comment(String comment)
    {
        // TODO Auto-generated method stub

    }

    public void section_(int arg0)
    {
        // TODO Auto-generated method stub

    }

    public void sectionTitle_(int arg0)
    {
        // TODO Auto-generated method stub

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
}
