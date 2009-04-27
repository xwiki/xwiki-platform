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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

import org.wikimodel.wem.IWemConstants;
import org.wikimodel.wem.IWemListener;
import org.wikimodel.wem.WikiFormat;
import org.wikimodel.wem.WikiParameter;
import org.wikimodel.wem.WikiParameters;
import org.wikimodel.wem.WikiReference;
import org.wikimodel.wem.WikiStyle;
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
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ListBLock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.NumberedListBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.QuotationBlock;
import org.xwiki.rendering.block.QuotationLineBlock;
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
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ImageParser;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.util.IdGenerator;
import org.xwiki.rendering.util.RenderersUtils;

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

    private Parser parser;

    private LinkParser linkParser;

    private ImageParser imageParser;

    private Stack<Integer> currentSectionLevel = new Stack<Integer>();

    private int documentLevel = 0;

    private IdGenerator idGenerator = new IdGenerator();

    private RenderersUtils renderersUtils = new RenderersUtils();

    private class MarkerBlock extends AbstractBlock
    {
        public void traverse(Listener listener)
        {
        }
    }

    // TODO: Remove the need to pass a Parser when WikiModel implements support for wiki syntax in links.
    // See http://code.google.com/p/wikimodel/issues/detail?id=87
    public XDOMGeneratorListener(Parser parser, LinkParser linkParser, ImageParser imageParser)
    {
        this.parser = parser;
        this.linkParser = linkParser;
        this.imageParser = imageParser;
    }

    public XDOM getXDOM()
    {
        return new XDOM(generateListFromStack(), this.idGenerator);
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
        beginDocument(WikiParameters.EMPTY);
    }

    public void beginDocument(WikiParameters params)
    {
        if (this.documentLevel > 0) {
            this.stack.push(this.marker);
        }
        this.currentSectionLevel.push(0);

        ++this.documentLevel;
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
        int sectionLevel = this.currentSectionLevel.peek();

        // Close sections
        for (; sectionLevel >= level; --sectionLevel) {
            this.stack.push(new SectionBlock(generateListFromStack()));
        }

        // Open sections
        for (; sectionLevel < level; ++sectionLevel) {
            this.stack.push(this.marker);
        }

        this.currentSectionLevel.set(this.currentSectionLevel.size() - 1, sectionLevel);

        // Push header
        this.stack.push(this.marker);
    }

    public void beginInfoBlock(String infoType, WikiParameters params)
    {
        throw new RuntimeException("beginInfoBlock(" + infoType + ", " + params + ") (not handled yet)");
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
        throw new RuntimeException("beginPropertyBlock(" + propertyUri + ", " + doc + ") (not handled yet)");
    }

    public void beginPropertyInline(String str)
    {
        throw new RuntimeException("beginPropertyInline(" + str + ") (not handled yet)");
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
        endDocument(WikiParameters.EMPTY);
    }

    public void endDocument(WikiParameters params)
    {
        // Close sections
        int sectionLevel = this.currentSectionLevel.peek();
        for (; sectionLevel > 0; --sectionLevel) {
            this.stack.push(new SectionBlock(generateListFromStack()));
        }

        --this.documentLevel;

        this.currentSectionLevel.pop();
        if (this.documentLevel > 0) {
            this.stack.push(new GroupBlock(generateListFromStack(), convertParameters(params)));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see IWemListener#endFormat(WikiFormat)
     */
    public void endFormat(WikiFormat format)
    {
        // Get the styles: the styles are wiki syntax styles (i.e. styles which have a wiki syntax such as bold, italic ,etc).
        // As opposed to format parameters which don't have any specific wiki syntax (they have a generic wiki syntax such as
        // (% a='b' %) for example in XWiki Syntax 2.0.
        List<WikiStyle> styles = format.getStyles();
        
        // If there's any style or parameter defined, do something. The reason we need to check for this is because wikimodel
        // sends an empty begin/endFormat event before starting an inline block (such as a paragraph).
        if ((styles.size() > 0) || (format.getParams().size() > 0)) {

            // Generate nested FormatBlock blocks since XWiki uses nested Format blocks whereas Wikimodel doesn't.
            //
            // Simple Use Case: (% a='b' %)**//hello//**(%%)
            // WikiModel Events: 
            //   beginFormat(params: a='b', styles = BOLD, ITALIC)
            //   onWord(hello)
            //   endFormat(params: a='b', styles = BOLD, ITALIC)
            // XWiki Blocks:
            //   FormatBLock(params: a='b', format = BOLD)
            //     FormatBlock(format = ITALIC)
            //
            // More complex Use Case: **(% a='b' %)hello**world
            // WikiModel Events: 
            //   beginFormat(params: a='b', styles = BOLD)
            //   onWord(hello)
            //   endFormat(params: a='b', styles = BOLD)
            //   beginFormat(params: a='b')
            //   onWord(world)
            //   endFormat(params: a='b')
            // XWiki Blocks:
            //   FormatBlock(params: a='b', format = BOLD)
            //     WordBlock(hello)
            //   FormatBlock(params: a='b')
            //     WordBlock(world)
            
            // TODO: We should instead have the following which would allow to simplify XWikiSyntaxChaining Renderer
            // which currently has to check if the next format has the same params as the previous format to decide
            // whether to print it or not.
            //   FormatBlock(params: a='b')
            //     FormatBlock(format = BOLD)
            //       WordBlock(hello)
            //     WordBlock(world)
            
            FormatBlock block;
            if (styles.size() > 0) {
                block = new FormatBlock(generateListFromStack(), convertFormat(styles.get(styles.size() - 1)));
            } else {
                block = new FormatBlock(generateListFromStack(), Format.NONE);
            }
            

            if (styles.size() > 1) {
                ListIterator<WikiStyle> it = styles.listIterator(styles.size() - 1);
                while (it.hasPrevious()) {
                    block = new FormatBlock(Arrays.asList((Block) block), convertFormat(it.previous()));
                }
            }

            // If there are any parameters set in the format then set it on the first block so that it's printed before
            // the wiki syntax styles.
            if (format.getParams().size() > 0) {
                block.setParameters(convertParameters(new WikiParameters(format.getParams())));
            }

            // If the previous block is also a format block and it's the same style as the current
            // block then merge them.
            Block previous = this.stack.peek();
            if (FormatBlock.class.isAssignableFrom(previous.getClass())
                && (((FormatBlock) previous).getFormat() == block.getFormat())
                && (((FormatBlock) previous).getParameters().equals(block.getParameters()))) {
                previous.addChildren(block.getChildren());
            } else {
                this.stack.push(block);
            }

        } else {
            // Empty format. We need to remove our marker so pop all blocks after our marker and push them back on 
            // the stack.
            for (Block block : generateListFromStack()) {
                this.stack.push(block);
            }
        }
    }

    public void endHeader(int level, WikiParameters params)
    {
        List<Block> children = generateListFromStack();
        HeaderLevel headerLevel = HeaderLevel.parseInt(level);
        Map<String, String> parameters = convertParameters(params);
        String id = "H" + this.idGenerator.generateUniqueId(this.renderersUtils.renderPlainText(children));

        this.stack.push(new HeaderBlock(children, headerLevel, parameters, id));
    }

    public void endInfoBlock(String infoType, WikiParameters params)
    {
        throw new RuntimeException("endInfoBlock(" + infoType + ", " + params + ") (not handled yet)");
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
        throw new RuntimeException("endPropertyBlock(" + propertyUri + ", " + doc + ") (not handled yet)");
    }

    public void endPropertyInline(String inlineProperty)
    {
        throw new RuntimeException("endPropertyInline(" + inlineProperty + ") (not handled yet)");
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
        // The WikiModel XWiki parser has been modified not to generate any onEscape event so do nothing here.
        // This is because we believe that WikiModel should not have an escape event since it's the
        // responsibility of Renderers to perform escaping as required.
    }

    public void onExtensionBlock(String extensionName, WikiParameters params)
    {
        throw new RuntimeException("onExtensionBlock(" + extensionName + ", " + params + ") (not handled yet)");
    }

    public void onExtensionInline(String extensionName, WikiParameters params)
    {
        throw new RuntimeException("onExtensionInline(" + extensionName + ", " + params + ") (not handled yet)");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onHorizontalLine()
     */
    public void onHorizontalLine(WikiParameters params)
    {
        this.stack.push(new HorizontalLineBlock(convertParameters(params)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onLineBreak()
     */
    public void onLineBreak()
    {
        // Note that in XWiki we don't differentiate new lines and line breaks since it's the Renderers that decide
        // to generate new lines or line breaks depending on the context and the target syntax.
        this.stack.push(NewLineBlock.NEW_LINE_BLOCK);
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
        this.stack.push(new MacroBlock(macroName, convertParameters(params), content, false));
    }

    /**
     * @see #onMacroBlock(String, org.wikimodel.wem.WikiParameters, String)
     */
    public void onMacroInline(String macroName, WikiParameters params, String content)
    {
        this.stack.push(new MacroBlock(macroName, convertParameters(params), content, true));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onLineBreak()
     */
    public void onNewLine()
    {
        // Note that in XWiki we don't differentiate new lines and line breaks since it's the Renderers that decide
        // to generate new lines or line breaks depending on the context and the target syntax.
        this.stack.push(NewLineBlock.NEW_LINE_BLOCK);
    }

    /**
     * Called when WikiModel finds an reference such as a URI located directly in the text (free-standing URI), as
     * opposed to a link inside wiki link syntax delimiters.
     */
    public void onReference(String reference)
    {
        onReference(reference, null, true, Collections.<String, String> emptyMap());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onReference(String)
     */
    public void onReference(WikiReference reference)
    {
        onReference(reference.getLink(), reference.getLabel(), false, convertParameters(reference.getParameters()));
    }

    private void onReference(String reference, String label, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // If there's no link parser defined, don't handle links...
        // TODO: Generate some output log
        if (this.linkParser != null) {
            Block resultBlock;
            Link link = this.linkParser.parse(reference);

            // If the link failed to be constructed do nothing since parseLink will have wrapped it in an Error Block
            if (link != null) {
                // Verify if we have an image or a link. An image is identified by an "image:" uri
                // The reason we get this event is because WikiModel handles links and images in the same manner.
                resultBlock = createImageBlock(link, isFreeStandingURI, parameters);
                if (resultBlock == null) {
                    resultBlock = createLinkBlock(link, label, isFreeStandingURI, parameters);
                }
                this.stack.push(resultBlock);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListenerInline#onImage(java.lang.String)
     */
    public void onImage(String ref)
    {
        this.stack.push(new ImageBlock(this.imageParser.parse(ref), true));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListenerInline#onImage(org.wikimodel.wem.WikiReference)
     */
    public void onImage(WikiReference ref)
    {
        this.stack.push(new ImageBlock(this.imageParser.parse(ref.getLink()), false, convertParameters(ref
            .getParameters())));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onSpace(String)
     */
    public void onSpace(String spaces)
    {
        // We want one space event per space.
        for (int i = 0; i < spaces.length(); i++) {
            this.stack.push(SpaceBlock.SPACE_BLOCK);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onSpecialSymbol(String)
     */
    public void onSpecialSymbol(String symbol)
    {
        for (int i = 0; i < symbol.length(); i++) {
            this.stack.push(new SpecialSymbolBlock(symbol.charAt(i)));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onTableCaption(String)
     */
    public void onTableCaption(String str)
    {
        throw new RuntimeException("onTableCaption(" + str + ") (not handled yet)");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onVerbatimBlock(String, WikiParameters)
     */
    public void onVerbatimBlock(String protectedString, WikiParameters params)
    {
        this.stack.push(new VerbatimBlock(protectedString, convertParameters(params), false));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.IWemListener#onVerbatimInline(String, WikiParameters)
     */
    public void onVerbatimInline(String protectedString, WikiParameters params)
    {
        // TODO: we're currently not handling any inline verbatim parameters (we don't have support for this in
        // XWiki Blocks for now).
        this.stack.push(new VerbatimBlock(protectedString, true));
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

    private Format convertFormat(WikiStyle style)
    {
        Format result;
        if (style == IWemConstants.STRONG) {
            result = Format.BOLD;
        } else if (style == IWemConstants.EM) {
            result = Format.ITALIC;
        } else if (style == IWemConstants.STRIKE) {
            result = Format.STRIKEDOUT;
        } else if (style == IWemConstants.INS) {
            result = Format.UNDERLINED;
        } else if (style == IWemConstants.SUP) {
            result = Format.SUPERSCRIPT;
        } else if (style == IWemConstants.SUB) {
            result = Format.SUBSCRIPT;
        } else if (style == IWemConstants.MONO) {
            result = Format.MONOSPACE;
        } else {
            result = Format.NONE;
        }
        return result;
    }

    /**
     * Create an image block if the link passed in parameter represents an image.
     * 
     * @param link the link that potentially represents an image
     * @return an image block of the link represents an image or null if it's not an image
     */
    private Block createImageBlock(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        Block resultBlock = null;

        // Verify if we have an image or a link. An image is identified by an "image:" uri
        // The reason we get this event is because WikiModel handles links and images in the same manner.
        if ((link.getType() == LinkType.URI) && (link.getReference().startsWith("image:"))) {
            String imageLocation = link.getReference().substring("image:".length());
            resultBlock = new ImageBlock(this.imageParser.parse(imageLocation), isFreeStandingURI, parameters);
        }

        return resultBlock;
    }

    private Block createLinkBlock(Link link, String label, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        Block resultBlock;

        if (isFreeStandingURI) {
            resultBlock = new LinkBlock(Collections.<Block> emptyList(), link, true);
        } else {

            // TODO: Remove the need to parse the label passed by WikiModel when it implements support for wiki syntax
            // in links. See http://code.google.com/p/wikimodel/issues/detail?id=87
            List<Block> linkedBlocks = Collections.<Block> emptyList();
            if ((label != null) && (label.length() > 0)) {
                try {
                    // TODO: Use an inline parser. See http://jira.xwiki.org/jira/browse/XWIKI-2748
                    WikiModelParserUtils parserUtils = new WikiModelParserUtils();
                    linkedBlocks = parserUtils.parseInline(this.parser, label);
                } catch (ParseException e) {
                    // TODO: Handle errors
                }
            }

            resultBlock = new LinkBlock(linkedBlocks, link, false, parameters);
        }

        return resultBlock;
    }
}
