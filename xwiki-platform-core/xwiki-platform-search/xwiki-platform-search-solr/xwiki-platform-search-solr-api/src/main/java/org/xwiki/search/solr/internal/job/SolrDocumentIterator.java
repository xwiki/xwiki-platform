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
package org.xwiki.search.solr.internal.job;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.HighlightParams;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;

/**
 * Iterates the documents from the Solr search index.
 * 
 * @version $Id$
 * @since 5.4.5
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named("solr")
public class SolrDocumentIterator extends AbstractDocumentIterator<String>
{
    /**
     * The current index in the list of {@link #results}.
     */
    private int index;

    /**
     * A 'page' of results taken from the Solr index.
     */
    private List<SolrDocument> results = Collections.emptyList();

    /**
     * The query used to fetch the documents from the Solr index.
     */
    private SolrQuery query;

    /**
     * Provider for the {@link SolrInstance} that allows communication with the Solr server.
     */
    @Inject
    private Provider<SolrInstance> solrInstanceProvider;

    /**
     * Used to obtain the query corresponding to the configured root entity.
     */
    @Inject
    private SolrReferenceResolver solrReferenceResolver;

    @Override
    public boolean hasNext()
    {
        return getResults().size() > index;
    }

    @Override
    public Pair<DocumentReference, String> next()
    {
        SolrDocument result = getResults().get(index++);
        String wiki = (String) result.get(FieldUtils.WIKI);
        String space = (String) result.get(FieldUtils.SPACE);
        String name = (String) result.get(FieldUtils.NAME);
        String locale = (String) result.get(FieldUtils.DOCUMENT_LOCALE);
        String version = (String) result.get(FieldUtils.VERSION);
        DocumentReference documentReference = new DocumentReference(wiki, space, name);
        if (!locale.isEmpty()) {
            documentReference = new DocumentReference(documentReference, LocaleUtils.toLocale(locale));
        }
        return new ImmutablePair<DocumentReference, String>(documentReference, version);
    }

    @Override
    public long size()
    {
        return getResults() instanceof SolrDocumentList ? ((SolrDocumentList) results).getNumFound() : results.size();
    }

    /**
     * The current 'page' of results. If the current page has been fully iterated then a new page is fetched
     * automatically.
     * 
     * @return the current 'page' of results taken from the Solr index
     */
    private List<SolrDocument> getResults()
    {
        if (index >= results.size()) {
            try {
                results = this.solrInstanceProvider.get().query(getQuery()).getResults();
                query.setStart(query.getStart() + LIMIT);
            } catch (Exception e) {
                results = Collections.emptyList();
                logger.error("Failed to query the Solr index.", e);
            }
            index = 0;
        }
        return results;
    }

    /**
     * @return the query used to fetch the documents from the Solr index
     * @throws SolrIndexerException if we fail to obtain a query for the configured root entity
     */
    private SolrQuery getQuery() throws SolrIndexerException
    {
        if (query == null) {
            query = new SolrQuery(solrReferenceResolver.getQuery(rootReference));
            query.setFields(FieldUtils.WIKI, FieldUtils.SPACE, FieldUtils.NAME, FieldUtils.DOCUMENT_LOCALE,
                FieldUtils.VERSION);
            query.addFilterQuery(FieldUtils.TYPE + ':' + EntityType.DOCUMENT.name());
            // We must add the unique key field (ID in our case) to the list of sort fields (as a tie breaker) in order
            // to use the cursor-based pagination.
            query.addSortField(FieldUtils.WIKI, ORDER.asc).addSortField(FieldUtils.SPACE_EXACT, ORDER.asc)
                .addSortField(FieldUtils.NAME_EXACT, ORDER.asc).addSortField(FieldUtils.DOCUMENT_LOCALE, ORDER.asc)
                .addSortField(FieldUtils.ID, ORDER.asc);
            // Speed up the query by disabling the faceting and the highlighting.
            query.set(FacetParams.FACET, false);
            query.set(HighlightParams.HIGHLIGHT, false);
            query.setStart(0);
            query.setRows(LIMIT);
        }
        return query;
    }
}
