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
package org.xwiki.whatsnew.internal.xwikiblog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.apptasticsoftware.rssreader.Enclosure;
import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;

/**
 * Wraps a {@link RssReader} and add some extra mappings for XWiki Blog application's support of Dublin Core.
 *
 * @version $Id$
 * @since 15.2RC1
 */
public class XWikiBlogRSSReader
{
    private static final String XWIKI_ITEM_IMAGE_TAG = "xwiki:image";

    private static final String USER_AGENT = "XWikiWhatsNew";

    /**
     * @param rssURL the rss URL to get the RSS feed
     * @return the stream of items
     * @throws IOException in case of an error loading the RSS feed
     */
    public Stream<Item> read(String rssURL) throws IOException
    {
        RssReader rssReader = createRssReader();
        return rssReader.read(rssURL);
    }

    /**
     * @param rssStream the data stream containing the RSS data
     * @return the stream of items
     */
    public Stream<Item> read(InputStream rssStream)
    {
        RssReader rssReader = createRssReader();
        return rssReader.read(rssStream);
    }

    private RssReader createRssReader()
    {
        RssReader rssReader = new RssReader();
        // Add support for Dublin Core (dc) that XWiki's RSS feeds uses.
        addXWikiDublinCoreSupport(rssReader);
        // Set the user agent to simply mark that it's coming from an XWiki instance and be a good citizen
        rssReader.setUserAgent(USER_AGENT);
        return rssReader;
    }

    private void addXWikiDublinCoreSupport(RssReader rssReader)
    {
        rssReader.addItemExtension("dc:subject", (item, categoryString) -> {
            // The category string can contain subcategories. For example:
            //   <dc:subject>Blog.Development, Blog.GSoC, Blog.Tutorials, Blog.XWiki Days</dc:subject>
            // Thus we need to parse this string
            for (String singleCategory : StringUtils.split(categoryString, ",")) {
                item.addCategory(singleCategory.trim());
            }
        });
        rssReader.addItemExtension("dc:creator", Item::setAuthor);
        rssReader.addItemExtension("dc:date", Item::setPubDate);
        rssReader.addItemExtension(XWIKI_ITEM_IMAGE_TAG, (item, image) ->
            // Map the image URL to a RSS 2.0 Enclosure
            getEnclosure(item).setUrl(image)
        );
        rssReader.addItemExtension(XWIKI_ITEM_IMAGE_TAG, "type", (item, type) ->
            // Set the mimetype of the Enclosure
            getEnclosure(item).setType(type)
        );
        rssReader.addItemExtension(XWIKI_ITEM_IMAGE_TAG, "length", (item, length) ->
            // Set the image content length of the Enclosure
            getEnclosure(item).setLength(Long.valueOf(length))
        );
    }

    private Enclosure getEnclosure(Item item)
    {
        Enclosure result;
        Optional<Enclosure> optionalEnclosure = item.getEnclosure();
        if (!optionalEnclosure.isPresent()) {
            result = new Enclosure();
            item.setEnclosure(result);
        } else {
            result = optionalEnclosure.get();
        }
        return result;
    }
}
