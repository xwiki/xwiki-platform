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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
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
    private static final String SOLR_ANYVALUE = "[* TO *]";

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
    private SolrInstance solrInstance;

    /**
     * Used to obtain the query corresponding to the configured root entity.
     */
    @Inject
    private SolrReferenceResolver solrReferenceResolver;

    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @Override
    public boolean hasNext()
    {
        return getResults().size() > index;
    }

    @Override
    public Pair<DocumentReference, String> next()
    {
        SolrDocument result = getResults().get(index++);
        DocumentReference documentReference = this.solrDocumentReferenceResolver.resolve(result);
        String version = (String) result.get(FieldUtils.VERSION);
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
                // Cursor-based pagination.
                String cursorMark = getQuery().get(CursorMarkParams.CURSOR_MARK_PARAM);
                QueryResponse response = this.solrInstance.query(query);
                if (cursorMark.equals(response.getNextCursorMark())) {
                    results = Collections.emptyList();
                } else {
                    results = response.getResults();
                    query.set(CursorMarkParams.CURSOR_MARK_PARAM, response.getNextCursorMark());
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to query the Solr index.", e);
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
            query.setFields(FieldUtils.WIKI, FieldUtils.SPACES, FieldUtils.NAME, FieldUtils.DOCUMENT_LOCALE,
                FieldUtils.VERSION);
            query.addFilterQuery(FieldUtils.TYPE + ':' + EntityType.DOCUMENT.name());
            // Make sure to skip invalid documents, they will be re-indexed
            query.addFilterQuery(FieldUtils.WIKI + ':' + SOLR_ANYVALUE);
            query.addFilterQuery(FieldUtils.DOC_ID + ':' + SOLR_ANYVALUE);
            // This iterator must have the same order as the database iterator, otherwise the synchronization fails.
            query.addSort(FieldUtils.WIKI, ORDER.asc);
            query.addSort(FieldUtils.DOC_ID, ORDER.asc);
            // Cursor-based deep-paging requires the unique key to be included in the sort fields as a tie-breaker.
            // See https://issues.apache.org/jira/browse/SOLR-6277 .
            query.addSort(FieldUtils.ID, ORDER.asc);
            // Paginate using a cursor because it performs better than basic pagination (using absolute offset,
            // especially when the offset is big) and because the impact of index modifications is much smaller (and we
            // plan to update the index to match the database during the synchronization process).
            // See https://cwiki.apache.org/confluence/display/solr/Pagination+of+Results
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);
            query.setRows(getLimit());
        }
        return query;
    }
}
