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
package org.xwiki.tag.internal.livedata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.tag.TagException;
import org.xwiki.tag.internal.TagQueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Provides the paginated list of tags for a given page.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Component
@Named("taggedDocument")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class TaggedDocumentLiveDataEntryStore implements LiveDataEntryStore
{
    @Inject
    private TagQueryManager tagQueryManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Override
    public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
    {
        return Optional.empty();
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        LiveData liveData = new LiveData();
        String tag = (String) query.getSource().getParameters().getOrDefault("tag", null);
        try {
            liveData.setCount(this.tagQueryManager.countPages(tag));
            List<DocumentReference> pages =
                this.tagQueryManager.getPages(tag, query.getLimit(), query.getOffset().intValue());

            XWikiContext context = this.xcontextProvider.get();
            XWiki wiki = context.getWiki();
            liveData.getEntries().addAll(pages.stream().map(page -> {
                Map<String, Object> entry = new HashMap<>();
                try {
                    XWikiDocument document = wiki.getDocument(page, context);
                    entry.put("page", document.getRenderedTitle(context));
                    entry.put("page_link", wiki.getURL(page, context));
                } catch (XWikiException e) {
                    this.logger.warn("Failed to initialize the live table entry for the page [{}]. Cause: [{}].", page,
                        getRootCauseMessage(e));
                }

                return entry;
            }).collect(Collectors.toList()));
            return liveData;
        } catch (TagException e) {
            throw new LiveDataException(e);
        }
    }
}
