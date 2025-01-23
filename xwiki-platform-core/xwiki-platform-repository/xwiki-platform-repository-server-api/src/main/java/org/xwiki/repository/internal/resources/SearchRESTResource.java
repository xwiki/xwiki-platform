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
import org.xwiki.extension.rating.RatingExtension;
import org.xwiki.extension.repository.xwiki.model.jaxb.COMPARISON;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionQuery;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionsSearchResult;
import org.xwiki.extension.repository.xwiki.model.jaxb.Filter;
import org.xwiki.extension.repository.xwiki.model.jaxb.SortClause;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.SecureQuery;
import org.xwiki.repository.Resources;
import org.xwiki.repository.internal.XWikiRepositoryModel;

/**
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("org.xwiki.repository.internal.resources.SearchRESTResource")
@Path(Resources.SEARCH)
public class SearchRESTResource extends AbstractExtensionRESTResource
{
    /**
     * @since 3.3M2
     */
    @GET
    public ExtensionsSearchResult searchGet(@QueryParam(Resources.QPARAM_SEARCH_QUERY) @DefaultValue("") String pattern,
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

    // TODO: automatically replace Extension fields names with the actual Solr properties names (so that it's possible
    // to write query like type:jar)
    private String toSolrStatement(String query)
    {
        if (StringUtils.isBlank(query)) {
            return "*";
        } else if (StringUtils.containsNone(query, ' ', ':')) {
            return "*" + query + "*";
        }

        return query;
    }

    @POST
    public ExtensionsSearchResult searchPost(ExtensionQuery query) throws QueryException
    {
        ExtensionsSearchResult result = this.extensionObjectFactory.createExtensionsSearchResult();

        Query solrQuery = this.queryManager.createQuery(toSolrStatement(query.getQuery()), "solr");

        // /////////////////
        // Search only in the current wiki
        // /////////////////

        solrQuery.setWiki(this.xcontextProvider.get().getWikiId());

        // /////////////////
        // Limit and offset
        // /////////////////

        solrQuery.setLimit(query.getLimit());
        solrQuery.setOffset(query.getOffset());

        // /////////////////
        // Rights
        // /////////////////

        if (query instanceof SecureQuery) {
            // Show only what the current user has the right to see
            ((SecureQuery) query).checkCurrentUser(true);
        }

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

        // Convert extension ordering into solr ordering
        List<String> sortClauses = new ArrayList<>(query.getSortClauses().size() + 1);
        for (SortClause sortClause : query.getSortClauses()) {
            String solrField = XWikiRepositoryModel.toSolrField(sortClause.getField());
            if (solrField != null) {
                sortClauses.add(solrField + ' ' + sortClause.getOrder().name().toLowerCase());
            }
        }

        // Set default ordering
        if (StringUtils.isEmpty(query.getQuery())) {
            // Sort by rating by default when search query is empty
            sortClauses.add(XWikiRepositoryModel.toSolrOrderField(RatingExtension.FIELD_AVERAGE_VOTE) + " desc");
            sortClauses.add(XWikiRepositoryModel.toSolrOrderField(RatingExtension.FIELD_TOTAL_VOTES) + " desc");
        } else {
            // Sort by score by default when search query is not empty
            sortClauses.add("score desc");
        }

        solrQuery.bindValue("sort", sortClauses);

        // /////////////////
        // Filtering
        // /////////////////

        List<String> fq = new ArrayList<>(query.getFilters().size() + 1);

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

        // O means unset for solr but we want it to be literally interpreted to be consistent with previous behavior and
        // other searches behavior
        if (query.getLimit() != 0) {
            for (SolrDocument document : documents) {
                result.getExtensions().add(createExtensionVersionFromSolrDocument(document));
            }
        }

        return result;
    }
}
