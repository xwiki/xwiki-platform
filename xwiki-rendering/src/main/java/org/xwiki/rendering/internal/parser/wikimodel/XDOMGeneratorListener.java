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
package org.xwiki.rendering.internal.parser.wikimodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Collections;

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
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.url.XWikiURLFactory;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.InvalidURLException;
import org.xwiki.url.serializer.DocumentNameSerializer;

/**
 * Transforms WikiModel events into XWiki Rendering events.
 * 
 * @version $Id$
 * @since 1.5M1
 */
public class XDOMGeneratorListener implements IWemListener
{
    private Stack<Block> stack = new Stack<Block>();

    private final MarkerBlock marker = new MarkerBlock();

    private LinkParser linkParser;

    private XWikiURLFactory urlFactory;

    private class MarkerBlock extends AbstractBlock
    {
        public void traverse(Listener listener)
        {
        }
    }

    public XDOMGeneratorListener(LinkParser linkParser, XWikiURLFactory urlFactory)
    {
        this.linkParser = linkParser;
        this.urlFactory = urlFactory;
    }

    public XDOM getDocument()
    {
        return new XDOM(generateListFromStack());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#beginDefinitionDescription()
     */
    public void beginDefinitionDescription()
    {
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#beginDefinitionList(org.wikimodel.wem.WikiParameters)
     */
    public void beginDefinitionList(WikiParameters params)
    {
        this.stack.push(this.marker);
    }

    public void beginDefinitionTerm()
    {
        this.stack.push(this.marker);
    }

    public void beginDocument()
    {
        // Don't do anything since there's no notion of Document block in XWiki rendering.
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
        System.out.println("beginInfoBlock(" + infoType + ", " + params + ") (not handled yet)");
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
        System.out.println("beginPropertyBlock(" + propertyUri + ", " + doc + ") (not handled yet)");
    }

    public void beginPropertyInline(String str)
    {
        System.out.println("beginPropertyInline(" + str + ") (not handled yet)");
    }

    public void beginQuotation(WikiParameters params)
    {
        this.stack.push(this.marker);
    }

    public void beginQuotationLine()
    {
        this.stack.push(this.marker);
    }

    public void beginTable(WikiParameters params)
    {
        this.stack.push(this.marker);
    }

    public void beginTableCell(boolean tableHead, WikiParameters params)
    {
        this.stack.push(this.marker);
    }

    public void beginTableRow(WikiParameters params)
    {
        this.stack.push(this.marker);
    }

    public void endDefinitionDescription()
    {
        this.stack.push(new DefinitionDescriptionBlock(generateListFromStack()));
    }

    public void endDefinitionList(WikiParameters params)
    {
        this.stack.push(new DefinitionListBlock(generateListFromStack()));
    }

    public void endDefinitionTerm()
    {
        this.stack.push(new DefinitionTermBlock(generateListFromStack()));
    }

    public void endDocument()
    {
        // Don't do anything since there's no notion of Document block in XWiki rendering.
    }

    public void endFormat(WikiFormat format)
    {
        if (format.hasStyle(IWemConstants.STRONG)) {
            this.stack.push(new FormatBlock(generateListFromStack(), Format.BOLD));
        } else if (format.hasStyle(IWemConstants.EM)) {
            this.stack.push(new FormatBlock(generateListFromStack(), Format.ITALIC));
        } else if (format.hasStyle(IWemConstants.STRIKE)) {
            this.stack.push(new FormatBlock(generateListFromStack(), Format.STRIKEDOUT));
        } else if (format.hasStyle(IWemConstants.INS)) {
            this.stack.push(new FormatBlock(generateListFromStack(), Format.UNDERLINED));
        } else if (format.hasStyle(IWemConstants.SUP)) {
            this.stack.push(new FormatBlock(generateListFromStack(), Format.SUPERSCRIPT));
        } else if (format.hasStyle(IWemConstants.SUB)) {
            this.stack.push(new FormatBlock(generateListFromStack(), Format.SUBSCRIPT));
        } else if (format.hasStyle(IWemConstants.MONO)) {
            this.stack.push(new FormatBlock(generateListFromStack(), Format.MONOSPACE));
        } else {
            // WikiModel generate begin/endFormat events even for simple text with no style
            // so we need to remove our marker
            for (Block block : generateListFromStack()) {
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
        System.out.println("endInfoBlock(" + infoType + ", " + params + ") (not handled yet)");
    }

    public void endList(WikiParameters params, boolean ordered)
    {
        ListBLock listBlock;
        if (ordered) {
            listBlock = new NumberedListBlock(generateListFromStack(), convertParameters(params));
        } else {
            listBlock = new BulletedListBlock(generateListFromStack(), convertParameters(params));
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
        this.stack.push(new ParagraphBlock(generateListFromStack(), convertParameters(params)));
    }

    public void endPropertyBlock(String propertyUri, boolean doc)
    {
        System.out.println("endPropertyBlock(" + propertyUri + ", " + doc + ") (not handled yet)");
    }

    public void endPropertyInline(String inlineProperty)
    {
        System.out.println("endPropertyInline(" + inlineProperty + ") (not handled yet)");
    }

    public void endQuotation(WikiParameters params)
    {
        this.stack.push(new QuotationBlock(generateListFromStack(), convertParameters(params)));
    }

    public void endQuotationLine()
    {
        this.stack.push(new QuotationLineBlock(generateListFromStack()));
    }

    public void endTable(WikiParameters params)
    {
        this.stack.push(new TableBlock(generateListFromStack(), convertParameters(params)));
    }

    public void endTableCell(boolean tableHead, WikiParameters params)
    {
        if (tableHead) {
            this.stack.push(new TableHeadCellBlock(generateListFromStack(), convertParameters(params)));
        } else {
            this.stack.push(new TableCellBlock(generateListFromStack(), convertParameters(params)));
        }
    }

    public void endTableRow(WikiParameters params)
    {
        this.stack.push(new TableRowBlock(generateListFromStack(), convertParameters(params)));
    }

    /**
     * Called by wikimodel when there are 2 or more empty lines between blocks. For example the following will generate
     * a call to <code>onEmptyLines(2)</code>:
     * <p>
     * <code><pre>
     * {{macro/}}
     * ... empty line 1...
     * ... empty line 2...
     * {{macro/}}
     * </pre></code>
     * 
     * @param count the number of empty lines separating the two blocks
     */
    public void onEmptyLines(int count)
    {
        this.stack.push(new EmptyLinesBlock(count));
    }

    public void onEscape(String str)
    {
        this.stack.push(new EscapeBlock(str));
    }

    public void onExtensionBlock(String extensionName, WikiParameters params)
    {
        System.out.println("onExtensionBlock(" + extensionName + ", " + params + ") (not handled yet)");
    }

    public void onExtensionInline(String extensionName, WikiParameters params)
    {
        System.out.println("onExtensionInline(" + extensionName + ", " + params + ") (not handled yet)");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onHorizontalLine()
     */
    public void onHorizontalLine()
    {
        this.stack.push(HorizontalLineBlock.HORIZONTAL_LINE_BLOCK);
    }

    /**
     * Explicit line breaks. For example in XWiki syntax that would be "\\".
     */
    public void onLineBreak()
    {
        this.stack.push(LineBreakBlock.LINE_BREAK_BLOCK);
    }

    /**
     * A macro block was found and it's separated at least by one new line from the next block. If there's no new line
     * with the next block then wikimodel calls {@link #onMacroInline(String, org.wikimodel.wem.WikiParameters, String)}
     * instead.
     * <p>
     * In wikimodel block elements can be:
     * <ul>
     * <li>at the very beginning of the document (no "\n")</li>
     * <li>just after at least one "\n"</li>
     * </ul>
     */
    public void onMacroBlock(String macroName, WikiParameters params, String content)
    {
        this.stack.push(new MacroStandaloneBlock(macroName, convertParameters(params), content));
    }

    /**
     * @see #onMacroBlock(String, org.wikimodel.wem.WikiParameters, String)
     */
    public void onMacroInline(String macroName, WikiParameters params, String content)
    {
        this.stack.push(new MacroInlineBlock(macroName, convertParameters(params), content));
    }

    /**
     * "\n" character.
     */
    public void onNewLine()
    {
        this.stack.push(NewLineBlock.NEW_LINE_BLOCK);
    }

    /**
     * Called when WikiModel finds an reference such as a URI located directly in the text (free-standing URI), 
     * as opposed to a link inside wiki link syntax delimiters.
     */
    public void onReference(String ref)
    {
        // If there's no link parser defined, don't handle links...
        // TODO: Generate some output log
        if (this.linkParser != null) {
            try {
                this.stack.push(new LinkBlock(this.linkParser.parse(ref), true));
            } catch (ParseException e) {
                // TODO: Should we instead generate ErrorBlocks?
                throw new RuntimeException("Failed to parse link [" + ref + "]", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onReference(String)
     */
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

            // Check if the reference in the link is an relative URI. If that's the case transform it into
            // a document name sincce all relative URIs should point to wiki documents.
            if (link.getReference().startsWith("/")) {
                try {
                    XWikiURL url = this.urlFactory.createURL(link.getReference());
                    link.setReference(new DocumentNameSerializer().serialize(url));

                    // If the label is the same as the reference then remove it. This to prevent having the following
                    // use case: [[Space.Page]] --HTML--> <a href="/xwiki/bin/view/Space/Page>Space.Page</a>
                    // --XWIKI--> [[Space.Page>Space.Page]]
                    if (link.getLabel().equalsIgnoreCase(link.getReference())) {
                        link.setLabel(null);
                    }

                } catch (InvalidURLException e) {
                    // If it fails it means this was not a link pointing to a xwiki document after all so we just
                    // leave it as is.
                }
            }

            this.stack.push(new LinkBlock(link));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onSpace(String)
     */
    public void onSpace(String str)
    {
        this.stack.push(SpaceBlock.SPACE_BLOCK);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onSpecialSymbol(String)
     */
    public void onSpecialSymbol(String symbol)
    {
        this.stack.push(new SpecialSymbolBlock(symbol));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onTableCaption(String)
     */
    public void onTableCaption(String str)
    {
        System.out.println("onTableCaption(" + str + ") (not handled yet)");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onVerbatimBlock(String)
     */
    public void onVerbatimBlock(String protectedString)
    {
        this.stack.push(new VerbatimStandaloneBlock(protectedString));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onVerbatimInline(String)
     */
    public void onVerbatimInline(String protectedString)
    {
        this.stack.push(new VerbatimInlineBlock(protectedString));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onWord(String)
     */
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

    /**
     * Convert Wikimodel parameters to XWiki parameters format.
     * 
     * @param params the wikimodel parameters to convert
     * @return the parameters in XWiki format
     */
    private Map<String, String> convertParameters(WikiParameters params)
    {
        Map<String, String> xwikiParams = new LinkedHashMap<String, String>();
        for (WikiParameter wikiParameter : params.toList()) {
            xwikiParams.put(wikiParameter.getKey(), wikiParameter.getValue());
        }

        return xwikiParams;
    }
}
