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
package org.xwiki.activeinstalls2.internal.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import co.elastic.clients.elasticsearch._types.mapping.Property;

/**
 * Provide the number of documents in the XWiki instance.
 *
 * @version $Id$
 * @since 14.4RC1
 */
@Component
@Named("documents")
@Singleton
public class DocumentsPingDataProvider extends AbstractPingDataProvider
{
    private static final String PROPERTY_TOTAL = "total";

    private static final String PROPERTY_MAIN = "main";

    private static final String PROPERTY_ALL = "wikis";

    private static final String PROPERTY_DOCUMENTS = "documents";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Override
    public Map<String, Property> provideMapping()
    {
        Map<String, Property> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_TOTAL, Property.of(b1 -> b1.long_(b2 -> b2)));
        propertiesMap.put(PROPERTY_MAIN, Property.of(b1 -> b1.long_(b2 -> b2)));
        propertiesMap.put(PROPERTY_ALL, Property.of(b1 -> b1.long_(b2 -> b2)));

        return Collections.singletonMap(PROPERTY_DOCUMENTS, Property.of(b0 -> b0.object(b1 ->
            b1.properties(propertiesMap))));
    }

    @Override
    public void provideData(Ping ping)
    {
        DocumentsPing documentsPing = new DocumentsPing();

        Collection<String> wikiIds;
        try {
            wikiIds = this.wikiDescriptorManager.getAllIds();
        } catch (WikiManagerException e) {
            logWarning("Failed to get the list of wikis", e);
            return;
        }

        List<Long> allWikiCount = new ArrayList<>();
        long total = 0;
        for (String wikiId : wikiIds) {
            try {
                long wikiCount = getDocumentsCountInWiki(wikiId);
                total += wikiCount;
                if (this.wikiDescriptorManager.isMainWiki(wikiId)) {
                    documentsPing.setMain(wikiCount);
                } else {
                    allWikiCount.add(wikiCount);
                }
            } catch (QueryException e) {
                // Failed to compute the number of documents, log a warning but don't fail XWiki since the ping
                // shouldn't cause any problem to XWiki's operations.
                logWarning(String.format("Failed to get the documents count for wiki [%s]", wikiId), e);
            }
        }

        documentsPing.setTotal(total);
        documentsPing.setWikis(allWikiCount);
        ping.setDocuments(documentsPing);
    }

    private long getDocumentsCountInWiki(String wikiId) throws QueryException
    {
        List<Long> results = this.queryManager.createQuery("", Query.XWQL)
            .setWiki(wikiId)
            .addFilter(this.countFilter)
            .execute();
        return results.get(0).longValue();
    }
}
