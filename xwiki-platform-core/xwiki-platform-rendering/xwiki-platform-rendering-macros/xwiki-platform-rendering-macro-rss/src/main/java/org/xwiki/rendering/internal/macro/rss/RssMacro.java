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

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.SkinAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.box.AbstractBoxMacro;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.rss.RssMacroParameters;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * Macro that output latest feed entries from a RSS feed.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
@Component
@Named("rss")
@Singleton
public class RssMacro extends AbstractBoxMacro<RssMacroParameters>
{
    /**
     * The name of the CSS class attribute.
     */
    private static final String CLASS_ATTRIBUTE = "class";

    private static final String FEED_CLASS_VALUE = "rssfeed";

    private static final String FEED_PROPERTY = "RssMacro.feed";

    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Output latest feed entries from a RSS feed.";

    /**
     * The relative skin path of the feed icon to be displayed in the channel title.
     */
    private static final String FEED_ICON_RESOURCE_PATH = "icons/silk/feed.png";

    /**
     * Used to get the RSS icon.
     */
    @Inject
    private SkinAccessBridge skinAccessBridge;

    /**
     * Needed to parse the ordinary text.
     */
    @Inject
    @Named("plain/1.0")
    private Parser plainTextParser;

    /**
     * Needed to keep the feed information.
     */
    @Inject
    private Execution execution;

    /**
     * Create a Feed object from a feed specified as a URL.
     */
    private RomeFeedFactory romeFeedFactory = new DefaultRomeFeedFactory();

    /**
     * Create and initialize the descriptor of the macro.
     */
    public RssMacro()
    {
        super("RSS", DESCRIPTION, new DefaultContentDescriptor(DESCRIPTION, false), RssMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    /**
     * Allow to save the current processed feed in the context, to be able to retrieve without concurrency issue.
     * @param feed the feed to save in the current context.
     */
    private void setFeed(SyndFeed feed)
    {
        this.execution.getContext().setProperty(FEED_PROPERTY, feed);
    }

    /**
     * Retrieve the feed of the current context.
     * @return the feed that is being processed in the macro.
     */
    private SyndFeed getFeed()
    {
        return (SyndFeed) this.execution.getContext().getProperty(FEED_PROPERTY);
    }

    /**
     * Remove the feed information from the current context.
     */
    private void removeContextFeed()
    {
        this.execution.getContext().removeProperty(FEED_PROPERTY);
    }

    @Override
    protected ResourceReference getImageReference(RssMacroParameters parameters, String content,
        MacroTransformationContext context)
    {
        if (parameters.isDecoration() && parameters.isImage()) {
            return new ResourceReference(getFeed().getImage().getUrl(), ResourceType.URL);
        } else {
            return super.getImageReference(parameters, content, context);
        }
    }

    @Override
    protected List<? extends Block> getBlockTitle(RssMacroParameters parameters, String content,
        MacroTransformationContext context)
    {
        List<? extends Block> blockTitle = super.getBlockTitle(parameters, content, context);

        if (blockTitle == null) {
            return generateBoxTitle("rsschanneltitle", getFeed());
        } else {
            return blockTitle;
        }
    }

    @Override
    protected String getClassProperty()
    {
        return super.getClassProperty() + ' ' + FEED_CLASS_VALUE;
    }

    @Override
    public List<Block> execute(RssMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        List<Block> result;
        SyndFeed feed = this.romeFeedFactory.createFeed(parameters);

        // we save the feed in the current context to be able to retrieve it
        // we avoid to put it in a class field, to avoid concurrent exceptions
        this.setFeed(feed);

        if (parameters.isDecoration()) {
            result = super.execute(parameters, content == null ? StringUtils.EMPTY : content, context);
        } else {
            result = Arrays.<Block>asList(new GroupBlock(Collections.singletonMap(CLASS_ATTRIBUTE, FEED_CLASS_VALUE)));
        }

        generateEntries(result.get(0), feed, parameters);

        // clean the context
        this.removeContextFeed();
        return result;
    }

    @Override
    protected List<Block> parseContent(RssMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        return this.getMacroContentParser().parse(content, context, false, context.isInline()).getChildren();
    }

    /**
     * Renders the RSS's title.
     * 
     * @param cssClass the CSS sheet
     * @param feed the RSS feed data
     * @return the list of blocks making the RSS Box title
     */
    private List< ? extends Block> generateBoxTitle(String cssClass, SyndFeed feed)
    {
        List<Block> titleBlocks;

        if (feed.getLink() == null) {
            titleBlocks = parsePlainText(feed.getTitle());
        } else {
            // Title link.
            ResourceReference titleResourceReference = new ResourceReference(feed.getLink(), ResourceType.URL);

            // Title text link.
            Block titleTextLinkBlock = new LinkBlock(parsePlainText(feed.getTitle()), titleResourceReference, true);

            // Rss icon.
            String imagePath = this.skinAccessBridge.getSkinFile(FEED_ICON_RESOURCE_PATH);
            ImageBlock imageBlock = new ImageBlock(new ResourceReference(imagePath, ResourceType.URL), false);

            // Title rss icon link.
            Block titleImageLinkBlock = new LinkBlock(Arrays.<Block> asList(imageBlock), titleResourceReference, true);

            titleBlocks = Arrays.<Block> asList(titleTextLinkBlock, titleImageLinkBlock);
        }
        ParagraphBlock titleBlock = new ParagraphBlock(titleBlocks);
        titleBlock.setParameter(CLASS_ATTRIBUTE, cssClass);

        return Collections.singletonList(titleBlock);
    }

    /**
     * Renders the given RSS's entries.
     * 
     * @param parentBlock the parent Block to which the output is going to be added
     * @param feed the RSS Channel we retrieved via the Feed URL
     * @param parameters our parameter helper object
     * @throws MacroExecutionException if the content cannot be rendered
     */
    private void generateEntries(Block parentBlock, SyndFeed feed, RssMacroParameters parameters)
        throws MacroExecutionException
    {
        int maxElements = parameters.getCount();
        int count = 0;

        for (Object item : feed.getEntries()) {
            ++count;
            if (count > maxElements) {
                break;
            }
            SyndEntry entry = (SyndEntry) item;

            ResourceReference titleResourceReference = new ResourceReference(entry.getLink(), ResourceType.URL);
            Block titleBlock = new LinkBlock(parsePlainText(entry.getTitle()), titleResourceReference, true);
            ParagraphBlock paragraphTitleBlock = new ParagraphBlock(Collections.singletonList(titleBlock));
            paragraphTitleBlock.setParameter(CLASS_ATTRIBUTE, "rssitemtitle");
            parentBlock.addChild(paragraphTitleBlock);

            if (parameters.isContent() && entry.getDescription() != null) {
                // We are wrapping the feed entry content in a HTML macro, not considering what the declared content
                // is, because some feed will declare text while they actually contain HTML.
                // See http://stuffthathappens.com/blog/2007/10/29/i-hate-rss/
                // A case where doing this might hurt is if a feed declares "text" and has any XML inside it does
                // not want to be interpreted as such, but displayed as is instead. But this certainly is too rare
                // compared to mis-formed feeds that say text while they want to say HTML.
                Block html = new RawBlock(entry.getDescription().getValue(), Syntax.XHTML_1_0);
                parentBlock.addChild(new GroupBlock(Arrays.asList(html), Collections.singletonMap(CLASS_ATTRIBUTE,
                    "rssitemdescription")));
            }
        }
    }

    /**
     * @param romeFeedFactory a custom implementation to use instead of the default, useful for tests
     */
    protected void setFeedFactory(RomeFeedFactory romeFeedFactory)
    {
        this.romeFeedFactory = romeFeedFactory;
    }

    /**
     * Convenience method to not have to handle exceptions in several places.
     * 
     * @param content the content to parse as plain text
     * @return the parsed Blocks
     * @since 2.0M3
     */
    private List<Block> parsePlainText(String content)
    {
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }

        try {
            return this.plainTextParser.parse(new StringReader(content)).getChildren().get(0).getChildren();
        } catch (ParseException e) {
            // This shouldn't happen since the parser cannot throw an exception since the source is a memory
            // String.
            throw new RuntimeException("Failed to parse [" + content + "] as plain text", e);
        }
    }
}
