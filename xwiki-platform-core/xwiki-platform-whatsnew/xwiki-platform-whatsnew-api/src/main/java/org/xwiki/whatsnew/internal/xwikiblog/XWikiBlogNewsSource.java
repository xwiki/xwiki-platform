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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.extension.version.Version;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.user.UserReference;
import org.xwiki.whatsnew.NewsCategory;
import org.xwiki.whatsnew.NewsContent;
import org.xwiki.whatsnew.NewsException;
import org.xwiki.whatsnew.NewsSource;
import org.xwiki.whatsnew.NewsSourceItem;
import org.xwiki.whatsnew.internal.DefaultNewsContent;
import org.xwiki.whatsnew.internal.DefaultNewsSourceItem;

import com.apptasticsoftware.rssreader.Enclosure;
import com.apptasticsoftware.rssreader.Item;

/**
 * The XWiki Blog source (returns news from an XWiki Blog Application installed on an XWiki instance). The XWiki
 * Blog application must be configured to have the following Categories defined (only blog posts with one or several
 * of these categories will appear in the What's New UI in XWiki):
 * <ul>
 *   <li>Blog.What's New for XWiki: Simple User</li>
 *   <li>Blog.What's New for XWiki: Advanced User</li>
 *   <li>Blog.What's New for XWiki: Admin User</li>
 *   <li>Blog.What's New for XWiki: Extension</li>
 * </ul>
 *
 * @version $Id$
 * @since 15.1RC1
 */
public class XWikiBlogNewsSource implements NewsSource
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiBlogNewsSource.class);

    private static final String QUESTION_MARK = "?";

    private UserReference userReference;

    private Set<NewsCategory> wantedCategories;

    private Version targetXWikiVersion;

    private Map<String, Object> extraParameters;

    private int count;

    private String rssURL;

    private InputStream rssStream;

    private XWikiBlogNewsCategoryConverter categoriesConverter = new XWikiBlogNewsCategoryConverter();

    private RSSContentCleaner rssContentCleaner;

    /**
     * @param rssURL the URL to the XWiki Blog RSS
     * @param rssContentCleaner the component to clean the RSS description content so that it's safe to be rendered
     */
    public XWikiBlogNewsSource(String rssURL, RSSContentCleaner rssContentCleaner)
    {
        this.rssURL = rssURL;
        this.rssContentCleaner = rssContentCleaner;
    }

    /**
     * @param rssStream the stream containing the XWiki Blog RSS data (mostly needed for tests to avoid having
     *        to connect to an XWiki instance)
     * @param rssContentCleaner the component to clean the RSS description content so that it's safe to be rendered
     */
    public XWikiBlogNewsSource(InputStream rssStream, RSSContentCleaner rssContentCleaner)
    {
        this.rssStream = rssStream;
        this.rssContentCleaner = rssContentCleaner;
    }

    @Override
    public NewsSource forUser(UserReference userReference)
    {
        this.userReference = userReference;
        return this;
    }

    @Override
    public NewsSource forCategories(Set<NewsCategory> wantedCategories)
    {
        this.wantedCategories = Collections.unmodifiableSet(wantedCategories);
        return this;
    }

    @Override
    public NewsSource forXWikiVersion(Version targetXWikiVersion)
    {
        this.targetXWikiVersion = targetXWikiVersion;
        return this;
    }

    @Override
    public NewsSource forExtraParameters(Map<String, Object> extraParameters)
    {
        this.extraParameters = Collections.unmodifiableMap(extraParameters);
        return this;
    }

    @Override
    public NewsSource withCount(int count)
    {
        this.count = count;
        return this;
    }

    @Override
    public List<NewsSourceItem> build() throws NewsException
    {
        // Known limitations:
        // - The number of news entries returned by the XWiki Blog application is not configurable and depends
        //   on the blog type. See:
        //   - https://tinyurl.com/ycyyms76
        //   - For example for xwiki.org: "<itemsPerPage>10</itemsPerPage>"
        //     at https://www.xwiki.org/xwiki/bin/view/Blog/?xpage=xml
        // - We don't support targeting news for a given user or for a given XWiki version

        String computedRSSURL = computeRSSURL();

        LOGGER.debug("URL to gather \"What's New\" news: [{}]", computedRSSURL);

        // Fetch the XWiki Blog RSS
        List<Item> articles;
        try {
            XWikiBlogRSSReader rssReader = new XWikiBlogRSSReader();
            Stream<Item> itemStream;
            if (computedRSSURL != null) {
                itemStream = rssReader.read(computedRSSURL);
            } else {
                itemStream = rssReader.read(this.rssStream);
            }
            articles = itemStream.toList();
        } catch (IOException e) {
            String message = String.format("Failed to read RSS for [%s]", computedRSSURL);
            throw new NewsException(message, e);
        }

        List<NewsSourceItem> newsItems = new ArrayList<>();
        for (Item item : articles) {
            DefaultNewsSourceItem newsItem = new DefaultNewsSourceItem();
            newsItem.setTitle(item.getTitle());
            newsItem.setDescription(getCleanedContent(item));
            newsItem.setAuthor(item.getAuthor());
            newsItem.setCategories(this.categoriesConverter.convertFromRSS(item.getCategories()));
            newsItem.setPublishedDate(parseDateTime(item.getPubDate()));
            newsItem.setOriginURL(item.getLink());
            newsItem.setImageURL(getURL(item));
            newsItems.add(newsItem);
        }

        return newsItems;
    }

    private Optional<String> getURL(Item item)
    {
        Optional<String> optionalURL;
        Optional<Enclosure> optionalEnclosure = item.getEnclosure();
        if (optionalEnclosure.isEmpty()) {
            optionalURL = Optional.empty();
        } else {
            optionalURL = Optional.of(optionalEnclosure.get().getUrl());
        }
        return optionalURL;
    }

    private Optional<NewsContent> getCleanedContent(Item item)
    {
        // Clean the HTML content from the description so that it's safe to be rendered.
        Optional<NewsContent> optionalContent;
        Optional<String> optionalDescription = item.getDescription();
        if (optionalDescription.isPresent()) {
            optionalContent = Optional.of(
                new DefaultNewsContent(this.rssContentCleaner.clean(optionalDescription.get()),
                    getContentSyntax()));
        } else {
            optionalContent = Optional.empty();
        }
        return optionalContent;
    }

    private Syntax getContentSyntax()
    {
        // The XWiki Blog Application uses the syntax of the Skin and the Skin used on xwiki.org is using HTML 5.0.
        return Syntax.HTML_5_0;
    }

    private String computeRSSURL()
    {
        String result;
        if (this.rssURL == null) {
            result = null;
        } else if (this.wantedCategories == null) {
            result = this.rssURL;
        } else {
            result = String.format("%s%s%s", this.rssURL,
                this.rssURL.contains(QUESTION_MARK) ? "&" : QUESTION_MARK,
                this.categoriesConverter.convertToQueryString(this.wantedCategories));
        }
        return result;
    }

    @Override
    public String toString()
    {
        return String.format("XWiki Blog news source for URL [%s]", this.rssURL);
    }

    private Optional<Date> parseDateTime(Optional<String> dateAsString)
    {
        Optional<Date> result;
        if (!dateAsString.isPresent()) {
            result = Optional.empty();
        } else {
            // Example of expected date: "2023-01-23T05:34:15+01:00"
            // TODO: find how to pare the blog rss dates using ZonedDateTime:
            // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("format here");
            // ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateAsString, formatter);
            // I could not find a format that works.
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'hh:mm:ssZ");
            DateTime dt = formatter.parseDateTime(dateAsString.get());
            result = Optional.of(dt.toDate());
        }
        return result;
    }
}
