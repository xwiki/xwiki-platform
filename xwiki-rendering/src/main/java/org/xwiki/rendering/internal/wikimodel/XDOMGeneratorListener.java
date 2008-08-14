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
package org.xwiki.rendering.internal.wikimodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.wikimodel.wem.IWemConstants;
import org.wikimodel.wem.IWemListener;
import org.wikimodel.wem.WikiFormat;
import org.wikimodel.wem.WikiParameter;
import org.wikimodel.wem.WikiParameters;
import org.wikimodel.wem.WikiReference;
import org.xwiki.rendering.block.*;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.ParseException;

public class XDOMGeneratorListener implements IWemListener
{
    private Stack<Block> stack = new Stack<Block>();
    
    private final MarkerBlock marker = new MarkerBlock();

    private LinkParser linkParser;

    private class MarkerBlock extends AbstractBlock
    {
        public void traverse(Listener listener)
        {
        }
    }

    public XDOMGeneratorListener(LinkParser linkParser)
    {
        this.linkParser = linkParser;
    }

    public XDOM getDocument()
    {
        return new XDOM(generateListFromStack());
    }

    public void beginDefinitionDescription()
    {
    }

    public void beginDefinitionList(WikiParameters params)
    {
    }

    public void beginDefinitionTerm()
    {
    }

    public void beginDocument()
    {
    }

    /**
     * A format is a special formatting around an inline element, such as bold, italics, etc.
     */
    public void beginFormat(WikiFormat format)
    {
        this.stack.push(this.marker);
    }

    public void beginHeader(int level, WikiParameters params)
    {
        this.stack.push(this.marker);
    }

    public void beginInfoBlock(char infoType, WikiParameters params)
    {
    }

    public void beginList(WikiParameters params, boolean ordered)
    {
        this.stack.push(this.marker);
    }

    public void beginListItem()
    {
        this.stack.push(this.marker);
    }

    public void beginParagraph(WikiParameters params)
    {
        this.stack.push(this.marker);
    }

    public void beginPropertyBlock(String propertyUri, boolean doc)
    {
        // TODO Auto-generated method stub
        System.out.println("beginPropertyBlock");
    }

    public void beginPropertyInline(String str)
    {
        // TODO Auto-generated method stub
        System.out.println("beginPropertyInline");
    }

    public void beginQuotation(WikiParameters params)
    {
        // TODO Auto-generated method stub
        System.out.println("beginQuotation");
    }

    public void beginQuotationLine()
    {
        // TODO Auto-generated method stub
        System.out.println("beginQuotationLine");
    }

    public void beginTable(WikiParameters params)
    {
        // There's no XWiki syntax for tables. Instead there's a table macro.
    }

    public void beginTableCell(boolean tableHead, WikiParameters params)
    {
        // There's no XWiki syntax for tables. Instead there's a table macro.
    }

    public void beginTableRow(WikiParameters params)
    {
        // There's no XWiki syntax for tables. Instead there's a table macro.
    }

    public void endDefinitionDescription()
    {
        // TODO Auto-generated method stub
        System.out.println("endDefinitionDescription");
    }

    public void endDefinitionList(WikiParameters params)
    {
        // TODO Auto-generated method stub
        System.out.println("endDefinitionList");
    }

    public void endDefinitionTerm()
    {
        // TODO Auto-generated method stub
        System.out.println("endDefinitionTerm");
    }

    public void endDocument()
    {
        // Voluntarily don't do anything here for now.
    }

    public void endFormat(WikiFormat format)
    {
        // TODO: Follow http://code.google.com/p/wikimodel/issues/detail?id=31 to check when
        // underline is implemented and generate an UnderlineBlock when it is...
        
        if (format.hasStyle(IWemConstants.STRONG)) {
          this.stack.push(new BoldBlock(generateListFromStack()));
        } else if (format.hasStyle(IWemConstants.EM)) {
            this.stack.push(new ItalicBlock(generateListFromStack()));
        } else {
            // WikiModel generate begin/endFormat events even for simple text with no style
            // so we need to remove our marker
            for (Block block: generateListFromStack()) {
                this.stack.push(block);
            }
        }
    }

    public void endHeader(int level, WikiParameters params)
    {
        this.stack.push(new SectionBlock(generateListFromStack(), SectionLevel.parseInt(level)));
    }

    public void endInfoBlock(char infoType, WikiParameters params)
    {
        // TODO Auto-generated method stub
        System.out.println("endInfoBlock");
    }

    public void endList(WikiParameters params, boolean ordered)
    {
        ListBLock listBlock;
        if (ordered) {
            listBlock = new NumberedListBlock(generateListFromStack());
        } else {
            listBlock = new BulletedListBlock(generateListFromStack());
        }
        this.stack.push(listBlock);
    }

    public void endListItem()
    {
        // Note: This means we support Paragraphs inside lists.
        this.stack.push(new ListItemBlock(generateListFromStack()));
    }

    public void endParagraph(WikiParameters params)
    {
        this.stack.push(new ParagraphBlock(generateListFromStack()));
    }

    public void endPropertyBlock(String propertyUri, boolean doc)
    {
        // TODO Auto-generated method stub
        System.out.println("endPropertyBlock");
    }

    public void endPropertyInline(String inlineProperty)
    {
        // TODO Auto-generated method stub
        System.out.println("endPropertyInline");
    }

    public void endQuotation(WikiParameters params)
    {
        // TODO Auto-generated method stub
        System.out.println("endQuotation");
    }

    public void endQuotationLine()
    {
        // TODO Auto-generated method stub
        System.out.println("endQuotationLine");
    }

    public void endTable(WikiParameters params)
    {
        // There's no XWiki syntax for tables. Instead there's a table macro.
    }

    public void endTableCell(boolean tableHead, WikiParameters params)
    {
        // There's no XWiki syntax for tables. Instead there's a table macro.
    }

    public void endTableRow(WikiParameters params)
    {
        // There's no XWiki syntax for tables. Instead there's a table macro.
    }

    public void onEmptyLines(int count)
    {
        // TODO Auto-generated method stub

    }

    public void onEscape(String str)
    {
        this.stack.push(new EscapeBlock(str));
    }

    public void onExtensionBlock(String extensionName, WikiParameters params)
    {
        // TODO Auto-generated method stub

    }

    public void onExtensionInline(String extensionName, WikiParameters params)
    {
        // TODO Auto-generated method stub

    }

    public void onHorizontalLine()
    {
        // TODO Auto-generated method stub

    }

    /**
     * Explicit line breaks. For example in XWiki syntax that would be "\\".
     */
    public void onLineBreak()
    {
        this.stack.push(new LineBreakBlock());
    }

    public void onMacroBlock(String macroName, WikiParameters params, String content)
    {
        Map<String, String> xwikiParams = new LinkedHashMap<String, String>();
        for (WikiParameter wikiParameter: params.toList()) {
            xwikiParams.put(wikiParameter.getKey(), wikiParameter.getValue());
        }
        
        this.stack.push(new MacroBlock(macroName, xwikiParams, content));
    }

    public void onMacroInline(String macroName, WikiParameters params, String content)
    {
        onMacroBlock(macroName, params, content);
    }

    /**
     * "\n" character.
     */
    public void onNewLine()
    {
        this.stack.push(new NewLineBlock());
    }

    /**
     * Called when WikiModel finds an inline reference such as a URI located directly in the
     * text, as opposed to a link inside wiki link syntax delimiters.
     */
    public void onReference(String ref)
    {
        // If there's no link parser defined, don't handle links...
        // TODO: Generate some output log
        if (this.linkParser != null) {
            try {
                this.stack.push(new LinkBlock(this.linkParser.parse(ref)));
            } catch (ParseException e) {
                // TODO: Should we instead generate ErrorBlocks?
                throw new RuntimeException("Failed to parse link [" + ref + "]", e);
            }
        }
    }

	public void onReference(WikiReference ref)
	{
        // If there's no link parser defined, don't handle links...
        // TODO: Generate some output log
        if (this.linkParser != null) {
            Link link;
            try {
                link = this.linkParser.parse(ref.getLink());
            } catch (ParseException e) {
                // TODO: Should we instead generate ErrorBlocks?
                throw new RuntimeException("Failed to parse link [" + ref.getLink() + "]", e);
            }
            link.setLabel(ref.getLabel());

            // Right now WikiModel puts any target element as the first element of the WikiParameters
            if (ref.getParameters().getSize() > 0) {
                link.setTarget(ref.getParameters().getParameter(0).getKey());
            }

            this.stack.push(new LinkBlock(link));
        }
    }

	public void onSpace(String str)
    {
        this.stack.push(SpaceBlock.SPACE_BLOCK);
    }

    public void onSpecialSymbol(String symbol)
    {
        this.stack.push(new SpecialSymbolBlock(symbol));
    }

    public void onTableCaption(String str)
    {
        // There's no XWiki syntax for tables. Instead there's a table macro.
    }

    public void onVerbatimBlock(String str)
    {
        // TODO Auto-generated method stub

    }

    public void onVerbatimInline(String str)
    {
        // TODO Auto-generated method stub

    }

    public void onWord(String str)
    {
        this.stack.push(new WordBlock(str));
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
