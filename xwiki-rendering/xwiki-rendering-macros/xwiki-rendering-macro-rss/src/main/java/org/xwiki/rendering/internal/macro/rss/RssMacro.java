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

package org.xwiki.rendering.internal.macro.rss;

import java.util.Collections;
import java.util.List;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.box.BoxMacroParameters;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.rss.RssMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.ParserUtils;

/**
 * Macro that output latest feed entries from a RSS feed.
 * 
 * @version $Id: $
 * @since 1.8RC1
 */
public class RssMacro extends AbstractMacro<RssMacroParameters>
{
    /**
     * The name of the CSS class attribute.
     */
    private static final String CLASS_ATTRIBUTE = "class";

    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Output latest feed entries from a RSS feed.";

    /**
     * Injected by the Component Manager.
     */
    protected Macro<BoxMacroParameters> boxMacro;
    
    /** 
     * Needed to parse the ordinary text. 
     */
    private ParserUtils parserUtils;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public RssMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, RssMacroParameters.class));
    }

    /**
     * Used for implementing the lazy initialization of the parseUtils.
     * @return the parseUtils needed to parse the ordinary text
     */
    private ParserUtils getParserUtils()
    {
        if (parserUtils == null) {
            parserUtils = new ParserUtils();
        }
        return parserUtils;
    }

    /**
     * Renders the given RSS's entries.
     * @param parentBlock the parent Block to which the output is going to be added
     * @param feed the RSS Channel we retrieved via the Feed URL
     * @param parameters our parameter helper object
     * @param context the macro's transformation context
     * @throws MacroExecutionException if the content cannot be rendered
     */ 

    private void renderEntries(Block parentBlock, SyndFeed feed, RssMacroParameters parameters, 
        MacroTransformationContext context) throws MacroExecutionException 
    {
        int maxElements = parameters.getCount();
        int count = 0;

        for (Object item : feed.getEntries()) {
            ++count;
            if (count > maxElements) {
                break;
            }
            SyndEntry entry = (SyndEntry) item;
            
            Link titleLink = new Link();
            titleLink.setType(LinkType.URI);
            titleLink.setReference(entry.getLink());
            Block titleBlock = new LinkBlock(
                getParserUtils().parseInlineNonWiki(entry.getTitle()), titleLink, true);
            ParagraphBlock paragraphTitleBlock = new ParagraphBlock(Collections.singletonList(titleBlock));
            if (parameters.isCss()) {
                paragraphTitleBlock.setParameter(CLASS_ATTRIBUTE, "rssitemtitle");
            }
            parentBlock.addChild(paragraphTitleBlock);
            
            if (parameters.isFull() && entry.getDescription() != null) {
                // We are wrapping the feed entry content in a HTML macro, not considering what the declared content
                // is, because some feed will declare text while they actually contain HTML.
                // See http://stuffthathappens.com/blog/2007/10/29/i-hate-rss/
                // A case where doing this might hurt is if a feed declares "text" and has any XML inside it does
                // not want to be interpreted as such, but displayed as is instead. But this certainly is too rare
                // compared to mis-formed feeds that say text while they want to say HTML.
                Block html = new MacroBlock("html", Collections.singletonMap("wiki", "false"),
                    entry.getDescription().getValue(), context.isInline());
                
                ParagraphBlock descriptionBlock =
                    new ParagraphBlock(Collections.singletonList(html));
                if (parameters.isCss()) {
                    descriptionBlock.setParameter(CLASS_ATTRIBUTE, "rssitemdescription");
                }
                parentBlock.addChild(descriptionBlock);
            }
        }
    }

    /** 
     * {@inheritDoc}
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(RssMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = null;
        try {
            feed = input.build(new XmlReader(parameters.getFeedURL()));
        } catch (Exception ex) {
            throw new MacroExecutionException("Error processing " + parameters.getFeedURL() + ": " + ex.getMessage());
        }
        if (feed == null) { 
            throw new MacroExecutionException("No feed found at " + parameters.getFeedURL());
        }
        
        BoxMacroParameters boxParameters = new BoxMacroParameters();
        boolean hasImage = parameters.isImage() && feed.getImage() != null;

        if (parameters.isCss()) {
            boxParameters.setCssClass("rssfeed");
        }
        renderFeedOrEntryTitle(boxParameters, parameters.isCss(), "rsschanneltitle", feed.getTitle(), feed.getLink());

        List<Block> result = null;
        if (hasImage) {
            boxParameters.setImage(feed.getImage().getUrl());
            result = boxMacro.execute(boxParameters, content, context);
        } else {
            result = boxMacro.execute(boxParameters, content, context);
        }
        
        renderEntries(result.get(0), feed, parameters, context);

        return result;
    }
    
    /**
     * Renders the RSS's title.
     * @param boxParameters the BoxParameters where the title will be fitted
     * @param isCss whether CSS formatting should be applied
     * @param cssClass the CSS sheet
     * @param title the title's text
     * @param link the title's link (if there is one)
     */
    private void renderFeedOrEntryTitle(BoxMacroParameters boxParameters, boolean isCss, 
        String cssClass, String title, String link) {
        List<Block> titleBlocks = null;
        
        if (link == null) {
            titleBlocks = getParserUtils().parseInlineNonWiki(title);
        } else {
            Link titleLink = new Link();
            titleLink.setReference(link);
            titleLink.setType(LinkType.URI);
            Block linkBlock = new LinkBlock(getParserUtils().parseInlineNonWiki(title), titleLink, true);
            titleBlocks = Collections.singletonList(linkBlock);
        }
        ParagraphBlock titleBlock = new ParagraphBlock(titleBlocks);
        if (isCss) {
            titleBlock.setParameter(CLASS_ATTRIBUTE, cssClass);
        }
        
        boxParameters.setBlockTitle(Collections.singletonList(titleBlock));
    }
}
