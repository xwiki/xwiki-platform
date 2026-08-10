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
package org.xwiki.rest.resources.wikis;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.SearchResults;

/**
 * Resource for executing queries (XWQL, HQL, Solr, etc.) against a single wiki.
 *
 * @version $Id$
 */
@Path("/wikis/{wikiName}/query")
public interface WikiSearchQueryResource
{
    /**
     * Executes a query against the given wiki and returns the matching results.
     *
     * @param wikiName the identifier of the wiki against which the query is executed, for example {@code xwiki} for the
     *  main wiki
     * @param query the query statement to execute, expressed in the language given by {@code queryTypeString}, for
     *  example {@code where doc.creator = 'XWiki.Admin'} for XWQL
     * @param queryTypeString the query language to use; only languages listed in the {@code rest.allowedQueryTypes}
     *  configuration property are accepted (by default only {@code solr}), and any other value is rejected with a
     *  {@code 400} response; when {@code null} no query is executed and an empty result set is returned
     * @param number the maximum number of results to return; when {@code null} the wiki's configured REST query limit
     *  is used, and a value that is negative or larger than that configured limit is rejected with a {@code 400}
     *  response
     * @param start the 0-based index of the first result to return, used together with {@code number} for pagination;
     *  defaults to {@code 0}
     * @param distinct when {@code true}, collapses duplicate results; defaults to {@code true}
     * @param orderField the document field used to order the results, for example {@code doc.title}; empty by default
     *  (the query's own ordering is kept)
     * @param order the order direction applied to {@code orderField}, either {@code asc} or {@code desc}; defaults to
     *  {@code asc}
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name) in addition to the technical references, at some extra cost; defaults to {@code false}
     * @param className restricts the results to documents holding an object of the given class, for example
     *  {@code XWiki.XWikiUsers}; empty by default (no restriction)
     * @return the results matching the query, honoring the current user's view rights
     * @throws XWikiRestException if the query cannot be executed
     */
    // Needs a lot of parameters to bind path and query parameters
    @SuppressWarnings("checkstyle:ParameterNumber")
    @GET SearchResults search(
            @PathParam("wikiName") String wikiName,
            @QueryParam("q") String query,
            @QueryParam("type") String queryTypeString,
            @QueryParam("number") Integer number,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("distinct") @DefaultValue("true") Boolean distinct,
            @QueryParam("orderField") @DefaultValue("") String orderField,
            @QueryParam("order") @DefaultValue("asc") String order,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames,
            @QueryParam("className") @DefaultValue("") String className
    ) throws XWikiRestException;
}
