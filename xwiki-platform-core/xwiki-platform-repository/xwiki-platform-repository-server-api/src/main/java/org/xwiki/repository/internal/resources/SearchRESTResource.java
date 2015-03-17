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

package org.xwiki.repository.internal.resources;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.xwiki.model.jaxb.COMPARISON;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionQuery;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionsSearchResult;
import org.xwiki.extension.repository.xwiki.model.jaxb.Filter;
import org.xwiki.extension.repository.xwiki.model.jaxb.SortClause;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.repository.Resources;
import org.xwiki.repository.internal.XWikiRepositoryModel;

/**
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("org.xwiki.repository.internal.resources.SearchRESTResource")
@Path(Resources.SEARCH)
@Singleton
public class SearchRESTResource extends AbstractExtensionRESTResource
{
    /**
     * @since 3.3M2
     */
    @GET
    public ExtensionsSearchResult searchGet(
        @QueryParam(Resources.QPARAM_SEARCH_QUERY) @DefaultValue("") String pattern,
        @QueryParam(Resources.QPARAM_LIST_START) @DefaultValue("0") int offset,
        @QueryParam(Resources.QPARAM_LIST_NUMBER) @DefaultValue("-1") int number,
        @QueryParam(Resources.QPARAM_LIST_REQUIRETOTALHITS) @DefaultValue("true") boolean requireTotalHits)
        throws QueryException
    {
        ExtensionQuery query = this.extensionObjectFactory.createExtensionQuery();

        query.setQuery(pattern);
        query.setOffset(offset);
        query.setLimit(number);

        return searchPost(query);
    }

    private String toSolrStatement(String query)
    {
        if (StringUtils.isBlank(query)) {
            return "*";
        }

        return query;
    }

    @POST
    public ExtensionsSearchResult searchPost(ExtensionQuery query) throws QueryException
    {
        ExtensionsSearchResult result = this.extensionObjectFactory.createExtensionsSearchResult();

        result.setOffset(query.getOffset());

        Query solrQuery = this.queryManager.createQuery(toSolrStatement(query.getQuery()), "solr");

        // /////////////////
        // Limit and offset
        // /////////////////

        solrQuery.setLimit(query.getLimit());
        solrQuery.setOffset(query.getOffset());

        // /////////////////
        // Boost
        // /////////////////

        solrQuery.bindValue("qf", DEFAULT_BOOST);

        // /////////////////
        // Fields
        // /////////////////

        solrQuery.bindValue("fl", DEFAULT_FL);

        // /////////////////
        // Ordering
        // /////////////////

        List<String> sortClauses = new ArrayList<String>(query.getSortClauses().size() + 1);
        for (SortClause sortClause : query.getSortClauses()) {
            String solrField = XWikiRepositoryModel.toSolrField(sortClause.getField());
            if (solrField != null) {
                sortClauses.add(solrField + ' ' + sortClause.getOrder().name().toLowerCase());
            }
        }

        // Sort by score by default
        sortClauses.add("score desc");

        solrQuery.bindValue("sort", sortClauses);

        // /////////////////
        // Filtering
        // /////////////////

        List<String> fq = new ArrayList<String>(query.getFilters().size() + 1);

        // TODO: should be filter only on current wiki ?

        // We want only valid extensions documents
        fq.add(XWikiRepositoryModel.SOLRPROP_EXTENSION_VALIDEXTENSION + ":true");

        // Request filters
        for (Filter fiter : query.getFilters()) {
            String solrField = XWikiRepositoryModel.toSolrField(fiter.getField());
            if (solrField != null) {
                StringBuilder builder = new StringBuilder();

                builder.append(solrField);
                builder.append(':');

                if (fiter.getComparison() == COMPARISON.EQUAL) {
                    builder.append(fiter.getValueString());
                } else {
                    builder.append('*' + fiter.getValueString() + '*');
                }

                fq.add(builder.toString());
            }
        }

        solrQuery.bindValue("fq", fq);

        // /////////////////
        // Execute
        // /////////////////

        QueryResponse response = (QueryResponse) solrQuery.execute().get(0);

        SolrDocumentList documents = response.getResults();

        result.setOffset((int) documents.getStart());
        result.setTotalHits((int) documents.getNumFound());

        for (SolrDocument document : documents) {
            result.getExtensions().add(createExtensionVersionFromSolrDocument(document));
        }

        return result;
    }
}
