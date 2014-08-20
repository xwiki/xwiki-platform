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
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;

import javax.ws.rs.core.UriBuilderException;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.search.SearchSource;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.resources.spaces.SpaceResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;

/**
 * @version $Id$
 */
public class BaseSearchResult extends XWikiResource
{
    protected static final String SEARCH_TEMPLATE_INFO =
        "q={keywords}(&scope={content|name|title|spaces|objects})*(&number={number})(&start={start})(&orderField={fieldname}(&order={asc|desc}))(&prettyNames={false|true})";

    protected static final String QUERY_TEMPLATE_INFO =
        "q={query}(&type={xwql,hql,lucene})(&number={number})(&start={start})(&orderField={fieldname}(&order={asc|desc}))(&distinct=1)(&prettyNames={false|true})(&wikis={wikis})(&className={classname})";

    protected static enum SearchScope
    {
        SPACES,
        NAME,
        CONTENT,
        TITLE,
        OBJECTS
    }

    /**
     * Search for keyword in the given scopes. See {@link SearchScope} for more information.
     * 
     * @param number number of results to be returned.
     * @param start 0-based start offset.
     * @param withPrettyNames true if the users are displayed with their full name
     */
    protected List<SearchResult> search(List<SearchScope> searchScopes, String keywords, String wikiName, String space,
        boolean hasProgrammingRights, int number, int start, boolean distinct, String orderField, String order,
        Boolean withPrettyNames) throws IllegalArgumentException, UriBuilderException, QueryException, XWikiException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        /* This try is just needed for executing the finally clause. */
        try {
            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

            List<SearchResult> result = new ArrayList<SearchResult>();

            result.addAll(searchPages(searchScopes, keywords, wikiName, space, hasProgrammingRights, number, start,
                orderField, order, withPrettyNames));

            if (searchScopes.contains(SearchScope.SPACES)) {
                result.addAll(searchSpaces(keywords, wikiName, hasProgrammingRights, number, start));
            }

            if (searchScopes.contains(SearchScope.OBJECTS)) {
                result.addAll(searchObjects(keywords, wikiName, space, hasProgrammingRights, number, start, orderField,
                    order, withPrettyNames));
            }

            return result;
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }
    }

    /**
     * Search for keyword in the given scopes. Limit the search only to Pages. Search for keyword
     * 
     * @param keywords the string that will be used in a "like" XWQL clause.
     * @param number number of results to be returned.
     * @param start 0-based start offset.
     * @param orderField the field to be used to order the results.
     * @param order "asc" or "desc"
     * @return the results.
     */
    protected List<SearchResult> searchPages(List<SearchScope> searchScopes, String keywords, String wikiName,
        String space, boolean hasProgrammingRights, int number, int start, String orderField, String order,
        Boolean withPrettyNames) throws QueryException, IllegalArgumentException, UriBuilderException, XWikiException
    {
        XWiki xwikiApi = Utils.getXWikiApi(componentManager);

        String database = Utils.getXWikiContext(componentManager).getWikiId();

        /* This try is just needed for executing the finally clause. */
        try {
            List<SearchResult> result = new ArrayList<SearchResult>();

            if (keywords == null) {
                return result;
            }

            Formatter f = new Formatter();

            /*
             * If the order field is already one of the field hard coded in the base query, then do not add it to the
             * select clause.
             */
            String addColumn = "";
            if (!StringUtils.isBlank(orderField)) {
                addColumn =
                    (orderField.equals("") || orderField.equals("fullName") || orderField.equals("name") || orderField
                        .equals("space")) ? "" : ", doc." + orderField;
            }

            if (space != null) {
                f.format("select distinct doc.fullName, doc.space, doc.name, doc.language");
                f.format(addColumn);
                f.format(" from XWikiDocument as doc where doc.space = :space and ( ");
            } else {
                f.format("select distinct doc.fullName, doc.space, doc.name, doc.language");
                f.format(addColumn);
                f.format(" from XWikiDocument as doc where ( ");
            }

            /* Look for scopes related to pages */
            int acceptedScopes = 0;
            for (int i = 0; i < searchScopes.size(); i++) {
                SearchScope scope = searchScopes.get(i);

                switch (scope) {
                    case CONTENT:
                        f.format("upper(doc.content) like :keywords ");
                        acceptedScopes++;
                        break;
                    case NAME:
                        f.format("upper(doc.fullName) like :keywords ");
                        acceptedScopes++;
                        break;
                    case TITLE:
                        f.format("upper(doc.title) like :keywords ");
                        acceptedScopes++;
                        break;
                }

                if (i != searchScopes.size() - 1) {
                    f.format(" or ");
                }
            }

            /* If we don't find any scope related to pages then return empty results */
            if (acceptedScopes == 0) {
                return result;
            }

            /* Build the order clause. */
            String orderClause = null;
            if (StringUtils.isBlank(orderField)) {
                orderClause = "doc.fullName asc";
            } else {
                /* Check if the order parameter is a valid "asc" or "desc" string, otherwise use "asc" */
                if ("asc".equals(order) || "desc".equals(order)) {
                    orderClause = String.format("doc.%s %s", orderField, order);
                } else {
                    orderClause = String.format("doc.%s asc", orderField);
                }
            }

            // Add ordering
            f.format(") order by %s", orderClause);

            String query = f.toString();

            List<Object> queryResult = null;

            /* This is needed because if the :space placeholder is not in the query, setting it would cause an exception */
            if (space != null) {
                queryResult =
                    this.queryManager.createQuery(query, Query.XWQL)
                        .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase()))
                        .bindValue("space", space).addFilter(Utils.getHiddenQueryFilter(this.componentManager))
                        .setOffset(start).setLimit(number).execute();
            } else {
                queryResult =
                    this.queryManager.createQuery(query, Query.XWQL)
                        .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase()))
                        .addFilter(Utils.getHiddenQueryFilter(this.componentManager)).setOffset(start).setLimit(number)
                        .execute();
            }

            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;

                String spaceName = (String) fields[1];
                String pageName = (String) fields[2];
                String language = (String) fields[3];

                String pageId = Utils.getPageId(wikiName, spaceName, pageName);
                String pageFullName = Utils.getPageFullName(wikiName, spaceName, pageName);

                /* Check if the user has the right to see the found document */
                if (xwikiApi.hasAccessLevel("view", pageId)) {
                    Document doc = xwikiApi.getDocument(pageFullName);
                    String title = doc.getDisplayTitle();
                    SearchResult searchResult = objectFactory.createSearchResult();
                    searchResult.setType("page");
                    searchResult.setId(pageId);
                    searchResult.setPageFullName(pageFullName);
                    searchResult.setTitle(title);
                    searchResult.setWiki(wikiName);
                    searchResult.setSpace(spaceName);
                    searchResult.setPageName(pageName);
                    searchResult.setVersion(doc.getVersion());
                    searchResult.setAuthor(doc.getAuthor());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(doc.getDate());
                    searchResult.setModified(calendar);

                    if (withPrettyNames) {
                        searchResult.setAuthorName(Utils.getAuthorName(doc.getAuthor(), componentManager));
                    }

                    String pageUri = null;
                    if (StringUtils.isBlank(language)) {
                        pageUri =
                            Utils.createURI(this.uriInfo.getBaseUri(), PageResource.class, wikiName, spaceName,
                                pageName).toString();
                    } else {
                        searchResult.setLanguage(language);
                        pageUri =
                            Utils.createURI(this.uriInfo.getBaseUri(), PageTranslationResource.class, wikiName,
                                spaceName, pageName, language).toString();
                    }

                    Link pageLink = new Link();
                    pageLink.setHref(pageUri);
                    pageLink.setRel(Relations.PAGE);
                    searchResult.getLinks().add(pageLink);

                    result.add(searchResult);
                }
            }

            return result;
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }
    }

    /**
     * Search for keyword in the given scopes. Limit the search only to spaces.
     * 
     * @param keywords the string that will be used in a "like" XWQL clause
     * @param number number of results to be returned
     * @param start 0-based start offset
     * @return the results.
     */
    protected List<SearchResult> searchSpaces(String keywords, String wikiName, boolean hasProgrammingRights,
        int number, int start) throws QueryException, IllegalArgumentException, UriBuilderException, XWikiException
    {
        XWiki xwikiApi = Utils.getXWikiApi(componentManager);

        String database = Utils.getXWikiContext(componentManager).getWikiId();

        /* This try is just needed for executing the finally clause. */
        try {
            List<SearchResult> result = new ArrayList<SearchResult>();

            if (keywords == null) {
                return result;
            }

            Formatter f = new Formatter();

            f.format("select distinct doc.space from XWikiDocument as doc where upper(doc.space) like :keywords ");

            /* Add some filters if the user doesn't have programming rights. */
            if (hasProgrammingRights) {
                f.format(" order by doc.space asc");
            } else {
                f.format(" and doc.space<>'XWiki' and doc.space<>'Admin' and doc.space<>'Panels' order by doc.space asc");
            }

            String query = f.toString();

            List<Object> queryResult = null;
            queryResult =
                queryManager.createQuery(query, Query.XWQL)
                    .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase())).setLimit(number)
                    .setOffset(start).execute();

            for (Object object : queryResult) {
                String spaceName = (String) object;
                Document spaceDoc = xwikiApi.getDocument(String.format("%s.WebHome", spaceName));

                /* Check if the user has the right to see the found document */
                if (xwikiApi.hasAccessLevel("view", spaceDoc.getPrefixedFullName())) {
                    String title = spaceDoc.getDisplayTitle();

                    SearchResult searchResult = objectFactory.createSearchResult();
                    searchResult.setType("space");
                    searchResult.setId(Utils.getSpaceId(wikiName, spaceName));
                    searchResult.setWiki(wikiName);
                    searchResult.setSpace(spaceName);
                    searchResult.setTitle(title);

                    /* Add a link to the space information */
                    Link spaceLink = new Link();
                    spaceLink.setRel(Relations.SPACE);
                    String spaceUri =
                        Utils.createURI(uriInfo.getBaseUri(), SpaceResource.class, wikiName, spaceName).toString();
                    spaceLink.setHref(spaceUri);
                    searchResult.getLinks().add(spaceLink);

                    /* Add a link to the webhome if it exists */
                    String webHomePageId = Utils.getPageId(wikiName, spaceName, "WebHome");
                    if (xwikiApi.exists(webHomePageId) && xwikiApi.hasAccessLevel("view", webHomePageId)) {
                        String pageUri =
                            Utils.createURI(uriInfo.getBaseUri(), PageResource.class, wikiName, spaceName, "WebHome")
                                .toString();

                        Link pageLink = new Link();
                        pageLink.setHref(pageUri);
                        pageLink.setRel(Relations.HOME);
                        searchResult.getLinks().add(pageLink);
                    }

                    result.add(searchResult);
                }
            }

            return result;
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }
    }

    /**
     * Search for keyword in the given scopes. Limit the search only to Objects.
     * 
     * @param number number of results to be returned
     * @param start 0-based start offset
     * @param orderField the field to be used to order the results
     * @param order "asc" or "desc"
     * @return the results
     */
    protected List<SearchResult> searchObjects(String keywords, String wikiName, String space,
        boolean hasProgrammingRights, int number, int start, String orderField, String order, Boolean withPrettyNames)
        throws QueryException, IllegalArgumentException, UriBuilderException, XWikiException
    {
        XWikiContext xwikiContext = Utils.getXWikiContext(componentManager);

        XWiki xwikiApi = Utils.getXWikiApi(componentManager);

        String database = Utils.getXWikiContext(componentManager).getWikiId();

        /* This try is just needed for executing the finally clause. */
        try {
            List<SearchResult> result = new ArrayList<SearchResult>();

            if (keywords == null) {
                return result;
            }

            Formatter f = new Formatter();

            /*
             * If the order field is already one of the field hard coded in the base query, then do not add it to the
             * select clause.
             */
            String addColumn =
                (orderField.equals("") || orderField.equals("fullName") || orderField.equals("name") || orderField
                    .equals("space")) ? "" : ", doc." + orderField;

            if (space != null) {
                f.format("select distinct doc.fullName, doc.space, doc.name, obj.className, obj.number");
                f.format(addColumn);
                f.format(" from XWikiDocument as doc, BaseObject as obj, StringProperty as sp, LargeStringProperty as lsp where doc.space = :space and obj.name=doc.fullName and sp.id.id = obj.id and lsp.id.id = obj.id and (upper(sp.value) like :keywords or upper(lsp.value) like :keywords) ");
            } else {
                f.format("select distinct doc.fullName, doc.space, doc.name, obj.className, obj.number");
                f.format(addColumn);
                f.format(" from XWikiDocument as doc, BaseObject as obj, StringProperty as sp, LargeStringProperty as lsp where obj.name=doc.fullName and sp.id.id = obj.id and lsp.id.id = obj.id and (upper(sp.value) like :keywords or upper(lsp.value) like :keywords) ");
            }

            /* Build the order clause. */
            String orderClause = null;
            if (StringUtils.isBlank(orderField)) {
                orderClause = "doc.fullName asc";
            } else {
                /* Check if the order parameter is a valid "asc" or "desc" string, otherwise use "asc" */
                if ("asc".equals(order) || "desc".equals(order)) {
                    orderClause = String.format("doc.%s %s", orderField, order);
                } else {
                    orderClause = String.format("doc.%s asc", orderField);
                }
            }

            /* Add some filters if the user doesn't have programming rights. */
            if (hasProgrammingRights) {
                f.format(" order by %s", orderClause);
            } else {
                f.format(
                    " and doc.space<>'XWiki' and doc.space<>'Admin' and doc.space<>'Panels' and doc.name<>'WebPreferences' order by %s",
                    orderClause);
            }

            String query = f.toString();

            List<Object> queryResult = null;

            /* This is needed because if the :space placeholder is not in the query, setting it would cause an exception */
            if (space != null) {
                queryResult =
                    queryManager.createQuery(query, Query.XWQL)
                        .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase()))
                        .bindValue("space", space).setLimit(number).execute();
            } else {
                queryResult =
                    queryManager.createQuery(query, Query.XWQL)
                        .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase())).setLimit(number)
                        .execute();
            }

            /* Build the result. */
            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;

                String spaceName = (String) fields[1];
                String pageName = (String) fields[2];
                String className = (String) fields[3];
                int objectNumber = (Integer) fields[4];

                String id = Utils.getObjectId(wikiName, spaceName, pageName, className, objectNumber);

                String pageId = Utils.getPageId(wikiName, spaceName, pageName);
                String pageFullName = Utils.getPageFullName(wikiName, spaceName, pageName);

                /*
                 * Check if the user has the right to see the found document. We also prevent guest users to access
                 * object data in order to avoid leaking important information such as emails to crawlers.
                 */
                if (xwikiApi.hasAccessLevel("view", pageId) && xwikiContext.getUserReference() != null) {
                    Document doc = xwikiApi.getDocument(pageFullName);
                    String title = doc.getDisplayTitle();
                    SearchResult searchResult = objectFactory.createSearchResult();
                    searchResult.setType("object");
                    searchResult.setId(id);
                    searchResult.setPageFullName(pageFullName);
                    searchResult.setTitle(title);
                    searchResult.setWiki(wikiName);
                    searchResult.setSpace(spaceName);
                    searchResult.setPageName(pageName);
                    searchResult.setVersion(doc.getVersion());
                    searchResult.setClassName(className);
                    searchResult.setObjectNumber(objectNumber);
                    searchResult.setAuthor(doc.getAuthor());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(doc.getDate());
                    searchResult.setModified(calendar);

                    if (withPrettyNames) {
                        searchResult.setAuthorName(Utils.getAuthorName(doc.getAuthor(), componentManager));
                    }

                    String pageUri =
                        Utils.createURI(uriInfo.getBaseUri(), PageResource.class, wikiName, spaceName, pageName)
                            .toString();
                    Link pageLink = new Link();
                    pageLink.setHref(pageUri);
                    pageLink.setRel(Relations.PAGE);
                    searchResult.getLinks().add(pageLink);

                    String objectUri =
                        Utils.createURI(uriInfo.getBaseUri(), ObjectResource.class, wikiName, spaceName, pageName,
                            className, objectNumber).toString();
                    Link objectLink = new Link();
                    objectLink.setHref(objectUri);
                    objectLink.setRel(Relations.OBJECT);
                    searchResult.getLinks().add(objectLink);

                    result.add(searchResult);
                }
            }

            return result;
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }
    }

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
    protected List<SearchResult> searchQuery(String query, String queryTypeString, String wikiName, String wikis,
        boolean hasProgrammingRights, String orderField, String order, boolean distinct, int number, int start,
        Boolean withPrettyNames, String className) throws Exception
    {
        String currentWiki = Utils.getXWikiContext(componentManager).getWikiId();

        /* This try is just needed for executing the finally clause. */
        try {
            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

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
     * Return a list of {@link SearchScope} objects by parsing the strings provided in the search scope strings. If the
     * list doesn't contain any valid scope string, then CONTENT is added by default.
     * 
     * @param searchScopeStrings The list of string to be parsed.
     * @return The list of the parsed SearchScope elements.
     */
    protected List<SearchScope> parseSearchScopeStrings(List<String> searchScopeStrings)
    {
        List<SearchScope> searchScopes = new ArrayList<SearchScope>();
        for (String searchScopeString : searchScopeStrings) {
            if (searchScopeString != null && !searchScopes.contains(searchScopeString)) {
                try {
                    SearchScope searchScope = SearchScope.valueOf(searchScopeString.toUpperCase());
                    searchScopes.add(searchScope);
                } catch (IllegalArgumentException e) {
                    // Ignore unrecognized scopes
                }
            }
        }

        if (searchScopes.isEmpty()) {
            searchScopes.add(SearchScope.CONTENT);
        }

        return searchScopes;
    }
}
