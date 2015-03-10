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

import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionQuery;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionsSearchResult;
import org.xwiki.extension.repository.xwiki.model.jaxb.Filter;
import org.xwiki.extension.repository.xwiki.model.jaxb.ORDER;
import org.xwiki.extension.repository.xwiki.model.jaxb.SortClause;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.repository.Resources;

/**
 * @version $Id$
 * @since 3.2M3
 */
@Component("org.xwiki.repository.internal.resources.SearchRESTResource")
@Path(Resources.SEARCH)
@Singleton
public class SearchRESTResource extends AbstractExtensionRESTResource
{
    private static final String WHERE = "lower(extension.id) like :pattern or lower(extension.name) like :pattern"
        + " or lower(extension.summary) like :pattern or lower(extension.description) like :pattern";

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
        ExtensionsSearchResult result = this.extensionObjectFactory.createExtensionsSearchResult();

        result.setOffset(offset);

        if (requireTotalHits) {
            Query query = createExtensionsCountQuery(null, WHERE);

            query.bindValue("pattern", '%' + pattern.toLowerCase() + '%');

            result.setTotalHits((int) getExtensionsCountResult(query));
        } else {
            result.setTotalHits(-1);
        }

        if (number != 0 && (result.getTotalHits() == -1 || offset < result.getTotalHits())) {
            Query query = createExtensionsQuery(null, WHERE, offset, number);

            query.bindValue("pattern", '%' + pattern.toLowerCase() + '%');

            getExtensions(result.getExtensions(), query);
        }

        return result;
    }

    @POST
    public ExtensionsSearchResult searchPost(ExtensionQuery query)
    {
        ExtensionsSearchResult result = this.extensionObjectFactory.createExtensionsSearchResult();

        result.setOffset(query.getOffset());

        Query solrQuery = this.queryManager.createQuery(query.getQuery(), "solr");

        // /////////////////
        // Limit and offset
        // /////////////////

        solrQuery.setLimit(query.getLimit());
        solrQuery.setOffset(query.getOffset());

        // /////////////////
        // Boost
        // /////////////////
        // TODO

        solrQuery.bindValue("qf",
            "title^10.0 name^10.0 doccontent^2.0 objcontent^0.4 filename^0.4 attcontent^0.4 doccontentraw^0.4 "
                + "author_display^0.08 creator_display^0.08 " + "comment^0.016 attauthor_display^0.016 space^0.016");

        // /////////////////
        // Ordering
        // /////////////////
        // TODO

        if (!query.getSortClauses().isEmpty()) {
            List<String> sortClauses = new ArrayList<String>(query.getSortClauses().size());
            for (SortClause sortClause : query.getSortClauses()) {
                sortClauses.add(toSolrField(sortClause.getField()) + ' ' + sortClause.getOrder().name().toLowerCase());
            }
            solrQuery.bindValue("sort", sortClauses);
        }

        // /////////////////
        // Filtering
        // /////////////////
        // TODO

        List<String> fq = new ArrayList<String>(query.getFilters().size() + 1);

        // TODO: only current wiki ?

        // We want only documents
        fq.add("{!tag=type}type:(\"DOCUMENT\")");

        // Request filters
        for (Filter fiter : query.getFilters()) {
            // TODO: we may need to double the index of object in documents
            fq.add(e);
        }

        solrQuery.bindValue("fq", fq);

        // /////////////////
        // Execute
        // /////////////////

        ////////////////////////
        
        if (requireTotalHits) {
            Query query = createExtensionsCountQuery(null, WHERE);

            query.bindValue("pattern", '%' + pattern.toLowerCase() + '%');

            result.setTotalHits((int) getExtensionsCountResult(query));
        }

        if (query.getLimit() != 0 && (result.getTotalHits() == -1 || query.getLimit() < result.getTotalHits())) {
            Query query = createExtensionsQuery(null, WHERE, offset, number);

            query.bindValue("pattern", '%' + pattern.toLowerCase() + '%');

            getExtensions(result.getExtensions(), query);
        }

        return result;
    }

    private String toSolrField(String restField)
    {
        
    }
}
