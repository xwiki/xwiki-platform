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
package org.xwiki.whatsnew.internal.configured;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.extension.version.Version;
import org.xwiki.user.UserReference;
import org.xwiki.whatsnew.NewsCategory;
import org.xwiki.whatsnew.NewsException;
import org.xwiki.whatsnew.NewsSource;
import org.xwiki.whatsnew.NewsSourceItem;

/**
 * A News Source that aggregates several other News Source.
 *
 * @version $Id$
 */
public class CompositeNewsSource implements NewsSource
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeNewsSource.class);

    private List<NewsSource> wrappedSources;

    /**
     * @param wrappedSources the list of other News sources to aggregate
     */
    public CompositeNewsSource(List<NewsSource> wrappedSources)
    {
        this.wrappedSources = Collections.unmodifiableList(wrappedSources);
    }

    @Override
    public NewsSource forUser(UserReference userReference)
    {
        for (NewsSource source : getWrappedSources()) {
            source.forUser(userReference);
        }
        return this;
    }

    @Override
    public NewsSource forCategories(Set<NewsCategory> wantedCategories)
    {
        for (NewsSource source : getWrappedSources()) {
            source.forCategories(wantedCategories);
        }
        return this;
    }

    @Override
    public NewsSource forXWikiVersion(Version targetXWikiVersion)
    {
        for (NewsSource source : getWrappedSources()) {
            source.forXWikiVersion(targetXWikiVersion);
        }
        return this;
    }

    @Override
    public NewsSource forExtraParameters(Map<String, Object> extraParameters)
    {
        for (NewsSource source : getWrappedSources()) {
            source.forExtraParameters(extraParameters);
        }
        return this;
    }

    @Override
    public NewsSource withCount(int count)
    {
        for (NewsSource source : getWrappedSources()) {
            source.withCount(count);
        }
        return this;
    }

    @Override
    public List<NewsSourceItem> build()
    {
        List<NewsSourceItem> items = new ArrayList<>();
        for (NewsSource source : getWrappedSources()) {
            try {
                items.addAll(source.build());
            } catch (NewsException e) {
                // If an error happens for a given source, just ignore the source, but log the problem as a warning
                LOGGER.warn("Failed to get news items for source [{}]. Root cause: [{}]", source,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        // Sort by having the latest published date items come first in the list so that they're rendered first.
        Collections.sort(items);
        return items;
    }

    private List<NewsSource> getWrappedSources()
    {
        return this.wrappedSources;
    }
}
