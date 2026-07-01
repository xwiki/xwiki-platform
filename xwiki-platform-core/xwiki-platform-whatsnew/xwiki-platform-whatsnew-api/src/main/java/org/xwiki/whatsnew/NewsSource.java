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
package org.xwiki.whatsnew;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.extension.version.Version;
import org.xwiki.user.UserReference;

/**
 * Query the news items for a given source of news (e.g. news located in a xwiki.org blog, news located in a Discourse
 * category, etc).
 *
 * @version $Id$
 * @since 15.1RC1
 */
public interface NewsSource
{
    /**
     * Filter the news for a given user (if the source supports that).
     *
     * @param userReference the reference to the user for which to return news for
     * @return the news source itself to allow for a fluent API
     */
    NewsSource forUser(UserReference userReference);

    /**
     * Filter the news so that only items matching the passed categories are returned.
     *
     * @param wantedCategories the set of categories to filter for
     * @return the news source itself to allow for a fluent API
     */
    NewsSource forCategories(Set<NewsCategory> wantedCategories);

    /**
     * Filter the news for a given XWiki version (if the source supports it).
     *
     * @param targetXWikiVersion the XWiki version to return news for
     * @return the news source itself to allow for a fluent API
     */
    NewsSource forXWikiVersion(Version targetXWikiVersion);

    /**
     * Filter the news in a way that's source-dependent. This is for future extensibility to allow some source
     * implementations to filter on additional constraints without having to change the news source API.
     *
     * @param extraParameters the map of extra parameters to filter against
     * @return the news source itself to allow for a fluent API
     */
    NewsSource forExtraParameters(Map<String, Object> extraParameters);

    /**
     * Filter the news to only return a certain number of news items.
     *
     * @param count the max number of new items to return
     * @return the news source itself to allow for a fluent API
     */
    NewsSource withCount(int count);

    /**
     * @return the list of news items, filtered with the passed constraints
     * @throws NewsException if there's an error while fetching the news
     */
    List<NewsSourceItem> build() throws NewsException;
}
