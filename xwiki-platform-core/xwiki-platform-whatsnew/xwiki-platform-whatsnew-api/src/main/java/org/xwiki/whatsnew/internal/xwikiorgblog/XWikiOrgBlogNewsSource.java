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
package org.xwiki.whatsnew.internal.xwikiorgblog;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.extension.version.Version;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.user.UserReference;
import org.xwiki.whatsnew.NewsCategory;
import org.xwiki.whatsnew.NewsException;
import org.xwiki.whatsnew.NewsRange;
import org.xwiki.whatsnew.NewsSource;
import org.xwiki.whatsnew.NewsSourceItem;
import org.xwiki.whatsnew.internal.DefaultNewsSourceItem;

import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;

/**
 * The XWiki.org Blog source (gathers RSS feed from xwiki.org).
 *
 * @version $Id$
 * @since 15.1RC1
 */
public class XWikiOrgBlogNewsSource implements NewsSource
{
    private UserReference userReference;

    private Set<NewsCategory> wantedCategories;

    private Version targetXWikiVersion;

    private Map<String, Object> extraParameters;

    private NewsRange range;

    private String rssURL;

    private InputStream rssStream;

    /**
     * @param rssURL the URL to the xwiki.org blog RSS
     */
    public XWikiOrgBlogNewsSource(String rssURL)
    {
        this.rssURL = rssURL;
    }

    /**
     * @param rssStream the stream containing the xwiki.org blog RSS data (mostly needed for tests to avoid having
     *        to connect to xwiki.org)
     */
    public XWikiOrgBlogNewsSource(InputStream rssStream)
    {
        this.rssStream = rssStream;
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
    public NewsSource withRange(NewsRange range)
    {
        this.range = range;
        return this;
    }

    @Override
    public List<NewsSourceItem> build() throws NewsException
    {
        // Known limitations:
        // - The number of news entries returned by the XWiki Blog application is not configurable and depends
        //   on the blog type. For XWiki.org, ATM it's set to 10 entries. See:
        //   - https://tinyurl.com/ycyyms76
        //   - "<itemsPerPage>10</itemsPerPage>" at https://www.xwiki.org/xwiki/bin/view/Blog/?xpage=xml
        // - We don't support targeting news for a given user or for a given XWiki version

        // Fetch the XWiki.org Blog RSS
        List<Item> articles;
        try {
            RssReader rssReader = new RssReader();
            // Add support for Dublin Core (dc) that XWiki's RSS feeds uses.
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
            Stream<Item> itemStream;
            if (this.rssURL != null) {
                itemStream = rssReader.read(this.rssURL);
            } else {
                itemStream = rssReader.read(this.rssStream);
            }
            // Note:
            articles = itemStream
                .filter(categoriesPredicate())
                .collect(Collectors.toList());
        } catch (IOException e) {
            String message = this.rssURL != null ? String.format("Failed to read RSS for [%s]", this.rssURL)
                : "Failed to read RSS";
            throw new NewsException(message, e);
        }

        List<NewsSourceItem> newsItems = new ArrayList<>();
        for (Item item : articles) {
            DefaultNewsSourceItem newsItem = new DefaultNewsSourceItem();
            newsItem.setTitle(item.getTitle());
            newsItem.setContent(item.getDescription());
            newsItem.setAuthor(item.getAuthor());
            newsItem.setCategories(getMappedCategories(item.getCategories()));
            newsItem.setPublishedDate(item.getPubDate());
            newsItem.setOriginURL(item.getLink());
            newsItems.add(newsItem);
        }

        return newsItems;
    }

    private Predicate<Item> categoriesPredicate()
    {
        return item -> {
            if (this.wantedCategories != null) {
                Set<NewsCategory> categories = getMappedCategories(item.getCategories());
                for (NewsCategory category : this.wantedCategories) {
                    if (categories.contains(category)) {
                        return true;
                    }
                }
                return false;
            } else {
                return true;
            }
        };
    }

    private Set<NewsCategory> getMappedCategories(List<String> rssCategories)
    {
        Set<NewsCategory> mappedCategories = new HashSet<>();
        for (String category : rssCategories) {
            // Try to find a matching category
            if ("Blog.Releases".equals(category)) {
                mappedCategories.add(NewsCategory.ADMIN_USER);
            } else if ("Blog.News".equals(category)) {
                mappedCategories.add(NewsCategory.SIMPLE_USER);
            } else if ("Blog.Development".equals(category)) {
                mappedCategories.add(NewsCategory.ADVANCED_USER);
            } else if ("Blog.Extensions".equals(category)) {
                mappedCategories.add(NewsCategory.EXTENSION);
            } else if ("Blog.Integrations".equals(category)) {
                mappedCategories.add(NewsCategory.EXTENSION);
            } else if ("Blog.Surveys".equals(category)) {
                mappedCategories.add(NewsCategory.SIMPLE_USER);
            } else if ("Blog.Tutorials".equals(category)) {
                mappedCategories.add(NewsCategory.SIMPLE_USER);
            } else {
                mappedCategories.add(NewsCategory.UNKNOWN);
            }
        }
        return mappedCategories;
    }

    @Override
    public Syntax getContentSyntax()
    {
        return Syntax.XHTML_1_0;
    }
}
