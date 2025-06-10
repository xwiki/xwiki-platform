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
package org.xwiki.rest.internal.resources;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.search.SearchSource;
import org.xwiki.rest.model.jaxb.SearchResult;

import static org.xwiki.rest.internal.resources.KeywordSearchScope.CONTENT;

/**
 * @version $Id$
 */
public class BaseSearchResult extends XWikiResource
{
    protected static final String SEARCH_TEMPLATE_INFO =
        "q={keywords}(&scope={content|name|title|spaces|objects})*(&number={number})(&start={start})"
            + "(&orderField={fieldname}(&order={asc|desc}))(&prettyNames={false|true})";

    protected static final String QUERY_TEMPLATE_INFO =
        "q={query}(&type={type})(&number={number})(&start={start})(&orderField={fieldname}(&order={asc|desc}))"
            + "(&distinct=1)(&prettyNames={false|true})(&wikis={wikis})(&className={classname})";

    /**
     * Search for query using xwql, hql, lucene. Limit the search only to Pages. Search for keyword
     * 
     * @param query the query to be executed
     * @param queryTypeString can be "xwql", "hql" or "lucene".
     * @param orderField the field to be used to order the results.
     * @param order "asc" or "desc"
     * @param number number of results to be returned
     * @param start 0-based start offset.
     * @return a list of {@link SearchResult} objects containing the found items, or an empty list if the specified
     *         query type string doesn't represent a supported query type.
     */
    // Legacy code.
    @SuppressWarnings("checkstyle:ParameterNumber")
    protected List<SearchResult> searchQuery(String query, String queryTypeString, String wikiName, String wikis,
        boolean hasProgrammingRights, String orderField, String order, boolean distinct, int number, int start,
        Boolean withPrettyNames, String className) throws Exception
    {
        String currentWiki = Utils.getXWikiContext(componentManager).getWikiId();

        /* This try is just needed for executing the finally clause. */
        try {
            if (wikiName != null) {
                Utils.getXWikiContext(componentManager).setWikiId(wikiName);
            }

            List<SearchResult> result;

            if (queryTypeString != null) {
                SearchSource searchSource =
                    this.componentManager.getInstance(SearchSource.class, queryTypeString.toLowerCase());

                result =
                    searchSource.search(query, wikiName, wikis, hasProgrammingRights, orderField, order,
                        distinct, number, start, withPrettyNames, className, uriInfo);
            } else {
                result = new ArrayList<SearchResult>();
            }

            return result;
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(currentWiki);
        }
    }

    /**
     * Return a list of {@link KeywordSearchScope} objects by parsing the strings provided in the search scope strings. If the
     * list doesn't contain any valid scope string, then CONTENT is added by default.
     * 
     * @param searchScopeStrings The list of string to be parsed.
     * @return The list of the parsed SearchScope elements.
     */
    protected List<KeywordSearchScope> parseSearchScopeStrings(List<String> searchScopeStrings)
    {
        List<KeywordSearchScope> searchScopes = new ArrayList<KeywordSearchScope>();
        for (String searchScopeString : searchScopeStrings) {
            if (searchScopeString != null && !searchScopes.contains(searchScopeString)) {
                try {
                    KeywordSearchScope searchScope = KeywordSearchScope.valueOf(searchScopeString.toUpperCase());
                    searchScopes.add(searchScope);
                } catch (IllegalArgumentException e) {
                    // Ignore unrecognized scopes
                }
            }
        }

        if (searchScopes.isEmpty()) {
            searchScopes.add(CONTENT);
        }

        return searchScopes;
    }
}
