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
package org.xwiki.search.solr.internal.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.api.SolrInstance;

/**
 * Find the indexed documents whose links target depends on the existence of some documents.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = SolrLinkExistenceDependencies.class)
@Singleton
public class SolrLinkExistenceDependencies
{
    private static final int ROWS = 1000;

    /**
     * The maximum number of documents to search in a single query, to stay far from the maximum number of clauses
     * accepted by Solr.
     */
    private static final int MAX_DOCUMENTS_PER_QUERY = 100;

    @Inject
    private SolrInstance solrInstance;

    @Inject
    private SolrUtils utils;

    @Inject
    private SolrLinkSerializer linkSerializer;

    @Inject
    @Named("withparameters")
    private EntityReferenceResolver<String> referenceResolver;

    /**
     * @param documents the documents whose existence changed
     * @return the references (with locale) of the indexed documents containing links whose target depends on the
     *     existence of at least one of the passed documents
     * @throws SolrIndexerException when failing to search the index
     */
    public Set<DocumentReference> getDependents(Collection<? extends EntityReference> documents)
        throws SolrIndexerException
    {
        Set<DocumentReference> dependents = new LinkedHashSet<>();

        List<String> values = new ArrayList<>(MAX_DOCUMENTS_PER_QUERY);
        for (EntityReference document : documents) {
            values.add(this.utils.toCompleteFilterQueryString(
                this.linkSerializer.serialize(new DocumentReference(document).withoutLocale())));

            if (values.size() == MAX_DOCUMENTS_PER_QUERY) {
                addDependents(values, dependents);
                values.clear();
            }
        }
        if (!values.isEmpty()) {
            addDependents(values, dependents);
        }

        return dependents;
    }

    private void addDependents(List<String> values, Set<DocumentReference> dependents) throws SolrIndexerException
    {
        SolrQuery solrQuery = new SolrQuery(FieldUtils.LINKS_EXISTENCE + ":(" + String.join(" OR ", values) + ')');

        solrQuery.setRows(ROWS);
        // Set sorting based on the ID for cursor-based pagination to work.
        solrQuery.addSort(FieldUtils.ID, SolrQuery.ORDER.asc);
        solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);

        // Load only the field we need
        solrQuery.setFields(FieldUtils.REFERENCE);

        QueryResponse response;
        do {
            try {
                response = this.solrInstance.query(solrQuery);
            } catch (Exception e) {
                throw new SolrIndexerException(
                    "Failed to search the documents with links depending on the existence of documents", e);
            }

            for (SolrDocument solrDocument : response.getResults()) {
                String referenceString = (String) solrDocument.getFieldValue(FieldUtils.REFERENCE);

                if (referenceString != null) {
                    dependents.add(new DocumentReference(this.referenceResolver.resolve(referenceString, null)));
                }
            }

            solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, response.getNextCursorMark());
        } while (response.getResults().size() == ROWS);
    }
}
