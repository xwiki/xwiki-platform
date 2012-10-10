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
package org.xwiki.rest.resources;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Relations;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.resources.spaces.SpaceResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.lucene.LucenePlugin;
import com.xpn.xwiki.plugin.lucene.SearchResults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class BaseSearchResult extends XWikiResource
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSearchResult.class);

    protected static final String SEARCH_TEMPLATE_INFO =
        "q={keywords}(&scope={content|name|title|spaces|objects)*(&number={number})(&start={start})(&order={documentfield asc,documentfield desc})(&prettynames=true)";

    protected static final String QUERY_TEMPLATE_INFO =
        "q={query}(&type={xwql,hql,lucene)(&number={number})(&start={start})(&order={lucenefield,-lucenefield})(&distinct=1)(&prettynames=true)(&wikis={wikis})(&classname={classname})";

    protected static enum SearchScope
    {
        SPACES,
        NAME,
        CONTENT,
        TITLE,
        OBJECTS
    }
    
    protected final static class QueryType
    {
        public final static String XWQL = "xwql";
        public final static String HQL = "hql";
        public final static String LUCENE = "lucene";
    }

    /**
     * Search for keyword in the given scopes. See {@link SearchScope} for more information.
     * 
     * @param searchScopes
     * @param keywords
     * @param wikiName
     * @param space
     * @param hasProgrammingRights
     * @param number
     * @param withPrettyNames true if the users are displayed with their full name
     * @return
     * @throws IllegalArgumentException
     * @throws UriBuilderException
     * @throws QueryException
     * @throws XWikiException
     */
    protected List<SearchResult> search(List<SearchScope> searchScopes, String keywords, String wikiName, String space,
        boolean hasProgrammingRights, int number, int start, boolean distinct, String order, Boolean withPrettyNames)
        throws IllegalArgumentException, UriBuilderException, QueryException, XWikiException
    {
        String database = Utils.getXWikiContext(componentManager).getDatabase();

        /* This try is just needed for executing the finally clause. */
        try {
            Utils.getXWikiContext(componentManager).setDatabase(wikiName);

            List<SearchResult> result = new ArrayList<SearchResult>();

            result.addAll(searchPages(searchScopes, keywords, wikiName, space, hasProgrammingRights, number, start, order, withPrettyNames));

            if (searchScopes.contains(SearchScope.SPACES)) {
                result.addAll(searchSpaces(keywords, wikiName, hasProgrammingRights, number, start));
            }

            if (searchScopes.contains(SearchScope.OBJECTS)) {
                result.addAll(searchObjects(keywords, wikiName, space, hasProgrammingRights, number, start, order, withPrettyNames));
            }

            return result;
        } finally {
            Utils.getXWikiContext(componentManager).setDatabase(database);
        }
    }

    /**
     * Search for keyword in the given scopes. Limit the search only to Pages. Search for keyword
     * 
     * @param searchScopes
     * @param keywords
     * @param wikiName
     * @param space
     * @param hasProgrammingRights
     * @param number
     * @return
     * @throws QueryException
     * @throws IllegalArgumentException
     * @throws UriBuilderException
     * @throws XWikiException
     */
    protected List<SearchResult> searchPages(List<SearchScope> searchScopes, String keywords, String wikiName,
        String space, boolean hasProgrammingRights, int number, int start, String order, Boolean withPrettyNames) throws QueryException,
        IllegalArgumentException, UriBuilderException, XWikiException
    {
        String database = Utils.getXWikiContext(componentManager).getDatabase();

        /* This try is just needed for executing the finally clause. */
        try {
            List<SearchResult> result = new ArrayList<SearchResult>();

            if (keywords == null) {
                return result;
            }

            Formatter f = new Formatter();

            String orderColumn = (order.indexOf(" ")==-1) ? order : order.substring(0, order.indexOf(" "));
            String addColumn = (order.equals("")||order.equals("fullName")||order.equals("name")||order.equals("space")) ? "" : ", doc." + orderColumn;
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

            String sOrder = (order.equals("") ? "doc.fullName asc" : "doc." + order);
            if (hasProgrammingRights) {
                f.format(") order by " + sOrder);
            } else {
                f.format(") and doc.space<>'XWiki' and doc.space<>'Admin' and doc.space<>'Panels' and doc.name<>'WebPreferences' order by " + sOrder);
            }

            String query = f.toString();

            List<Object> queryResult = null;

            /* This is needed because if the :space placeholder is not in the query, setting it would cause an exception */
            if (space != null) {
                queryResult =
                    queryManager.createQuery(query, Query.XWQL)
                        .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase()))
                        .bindValue("space", space).setLimit(number).setOffset(start).execute();
            } else {
                queryResult =
                    queryManager.createQuery(query, Query.XWQL)
                        .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase())).setLimit(number)
                        .setOffset(start).execute();
            }

            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;

                String spaceName = (String) fields[1];
                String pageName = (String) fields[2];
                String language = (String) fields[3];

                String pageId = Utils.getPageId(wikiName, spaceName, pageName);
                String pageFullName = Utils.getPageFullName(wikiName, spaceName, pageName);

                if (Utils.getXWikiApi(componentManager).hasAccessLevel("view", pageId)) {
                    Document doc = Utils.getXWikiApi(componentManager).getDocument(pageFullName);
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
                    if (withPrettyNames)
                        searchResult.setAuthorName(Utils.getAuthorName(doc.getAuthor(), componentManager));

                    String pageUri = null;
                    try {
                        if (StringUtils.isBlank(language)) {
                            pageUri =
                                UriBuilder
                                    .fromUri(this.uriInfo.getBaseUri())
                                    .path(PageResource.class)
                                    .buildFromEncoded(URLEncoder.encode(wikiName, "UTF-8"),
                                        URLEncoder.encode(spaceName, "UTF-8"), URLEncoder.encode(pageName, "UTF-8"))
                                    .toString();
                        } else {
                            searchResult.setLanguage(language);
                            pageUri =
                                UriBuilder
                                    .fromUri(this.uriInfo.getBaseUri())
                                    .path(PageTranslationResource.class)
                                    .buildFromEncoded(URLEncoder.encode(wikiName, "UTF-8"),
                                        URLEncoder.encode(spaceName, "UTF-8"), URLEncoder.encode(pageName, "UTF-8"),
                                        language).toString();
                        }
                    } catch (UnsupportedEncodingException ex) {
                        // This should never happen, UTF-8 is always valid.
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
            Utils.getXWikiContext(componentManager).setDatabase(database);
        }
    }

    /**
     * Search for keyword in the given scopes. Limit the search only to Pages. Search for keyword
     * 
     * @param searchScopes
     * @param keywords
     * @param wikiName
     * @param space
     * @param hasProgrammingRights
     * @param number
     * @return
     * @throws QueryException
     * @throws IllegalArgumentException
     * @throws UriBuilderException
     * @throws XWikiException
     */
    protected List<SearchResult> searchSpaces(String keywords, String wikiName, boolean hasProgrammingRights,
        int number, int start) throws QueryException, IllegalArgumentException, UriBuilderException, XWikiException
    {
        String database = Utils.getXWikiContext(componentManager).getDatabase();

        /* This try is just needed for executing the finally clause. */
        try {
            List<SearchResult> result = new ArrayList<SearchResult>();

            if (keywords == null) {
                return result;
            }

            Formatter f = new Formatter();

            f.format("select distinct doc.space from XWikiDocument as doc where upper(doc.space) like :keywords ");

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
                Document spaceDoc = Utils.getXWikiApi(componentManager).getDocument(spaceName + ".WebHome");
                String title = spaceDoc.getDisplayTitle();

                SearchResult searchResult = objectFactory.createSearchResult();
                searchResult.setType("space");
                searchResult.setId(String.format("%s:%s", wikiName, spaceName));
                searchResult.setWiki(wikiName);
                searchResult.setSpace(spaceName);
                searchResult.setTitle(title);

                /* Add a link to the space information */
                Link spaceLink = new Link();
                spaceLink.setRel(Relations.SPACE);
                String spaceUri =
                    UriBuilder.fromUri(uriInfo.getBaseUri()).path(SpaceResource.class).build(wikiName, spaceName)
                        .toString();
                spaceLink.setHref(spaceUri);
                searchResult.getLinks().add(spaceLink);

                /* Add a link to the webhome if it exists */
                String webHomePageId = Utils.getPageId(wikiName, spaceName, "WebHome");
                if (Utils.getXWikiApi(componentManager).exists(webHomePageId)
                    && Utils.getXWikiApi(componentManager).hasAccessLevel("view", webHomePageId)) {
                    String pageUri =
                        UriBuilder.fromUri(uriInfo.getBaseUri()).path(PageResource.class)
                            .build(wikiName, spaceName, "WebHome").toString();

                    Link pageLink = new Link();
                    pageLink.setHref(pageUri);
                    pageLink.setRel(Relations.HOME);
                    searchResult.getLinks().add(pageLink);
                }

                result.add(searchResult);
            }

            return result;
        } finally {
            Utils.getXWikiContext(componentManager).setDatabase(database);
        }
    }

    /**
     * Search for keyword in the given scopes. Limit the search only to Objects.
     * 
     * @param keywords
     * @param wikiName
     * @param space
     * @param hasProgrammingRights
     * @param number
     * @return
     * @throws QueryException
     * @throws IllegalArgumentException
     * @throws UriBuilderException
     * @throws XWikiException
     */
    protected List<SearchResult> searchObjects(String keywords, String wikiName, String space,
        boolean hasProgrammingRights, int number, int start, String order, Boolean withPrettyNames) throws QueryException, IllegalArgumentException,
        UriBuilderException, XWikiException
    {
        String database = Utils.getXWikiContext(componentManager).getDatabase();

        /* This try is just needed for executing the finally clause. */
        try {
            List<SearchResult> result = new ArrayList<SearchResult>();

            if (keywords == null) {
                return result;
            }

            Formatter f = new Formatter();

            String orderColumn = (order.indexOf(" ")==-1) ? order : order.substring(0, order.indexOf(" "));
            String addColumn = (order.equals("")||order.equals("fullName")||order.equals("name")||order.equals("space")) ? "" : ", doc." + orderColumn;
            if (space != null) {
                f.format("select distinct doc.fullName, doc.space, doc.name, obj.className, obj.number");
                f.format(addColumn);
                f.format(" from XWikiDocument as doc, BaseObject as obj, StringProperty as sp, LargeStringProperty as lsp where doc.space = :space and obj.name=doc.fullName and sp.id.id = obj.id and lsp.id.id = obj.id and (upper(sp.value) like :keywords or upper(lsp.value) like :keywords) ");
            } else {
                f.format("select distinct doc.fullName, doc.space, doc.name, obj.className, obj.number");
                f.format(addColumn);
                f.format(" from XWikiDocument as doc, BaseObject as obj, StringProperty as sp, LargeStringProperty as lsp where obj.name=doc.fullName and sp.id.id = obj.id and lsp.id.id = obj.id and (upper(sp.value) like :keywords or upper(lsp.value) like :keywords) ");
            }

            String sOrder = (order.equals("") ? "doc.fullName asc" : "doc." + order);
            if (hasProgrammingRights) {
                f.format(" order by " + sOrder);
            } else {
                f.format(" and doc.space<>'XWiki' and doc.space<>'Admin' and doc.space<>'Panels' and doc.name<>'WebPreferences' order by " + sOrder);
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

            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;

                String spaceName = (String) fields[1];
                String pageName = (String) fields[2];
                String className = (String) fields[3];
                int objectNumber = (Integer) fields[4];

                String id = Utils.getObjectId(wikiName, spaceName, pageName, className, objectNumber);

                String pageId = Utils.getPageId(wikiName, spaceName, pageName);
                String pageFullName = Utils.getPageFullName(wikiName, spaceName, pageName);

                if (Utils.getXWikiApi(componentManager).hasAccessLevel("view", pageId)) {
                    Document doc = Utils.getXWikiApi(componentManager).getDocument(pageFullName);
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
                    if (withPrettyNames)
                        searchResult.setAuthorName(Utils.getAuthorName(doc.getAuthor(), componentManager));

                    String pageUri =
                        UriBuilder.fromUri(uriInfo.getBaseUri()).path(PageResource.class)
                            .build(wikiName, spaceName, pageName).toString();
                    Link pageLink = new Link();
                    pageLink.setHref(pageUri);
                    pageLink.setRel(Relations.PAGE);
                    searchResult.getLinks().add(pageLink);

                    String objectUri =
                        UriBuilder.fromUri(uriInfo.getBaseUri()).path(ObjectResource.class)
                            .build(wikiName, spaceName, pageName, className, objectNumber).toString();
                    Link objectLink = new Link();
                    objectLink.setHref(objectUri);
                    objectLink.setRel(Relations.OBJECT);
                    searchResult.getLinks().add(objectLink);

                    result.add(searchResult);
                }
            }

            return result;
        } finally {
            Utils.getXWikiContext(componentManager).setDatabase(database);
        }
    }

    /**
     * Search for query using xwql, hql, lucene. Limit the search only to Pages. Search for keyword
     * 
     * @param query
     * @param queryType
     * @param wikiName
     * @param wikis
     * @param hasProgrammingRights
     * @param order
     * @param distinct
     * @param number
     * @param start
     * @param withPrettyNames 
     * @param className 
     * @return
     * @throws QueryException
     * @throws IllegalArgumentException
     * @throws UriBuilderException
     * @throws XWikiException
     */
    protected List<SearchResult> searchQuery(String query, String queryType, String wikiName, String wikis,
        boolean hasProgrammingRights, String order, boolean distinct, int number, int start, Boolean withPrettyNames, String className) throws QueryException,
        IllegalArgumentException, UriBuilderException, XWikiException
    {
        String database = Utils.getXWikiContext(componentManager).getDatabase();

        /* This try is just needed for executing the finally clause. */
        try {
            Utils.getXWikiContext(componentManager).setDatabase(wikiName);

            List<SearchResult> result = new ArrayList<SearchResult>();

   
            if (QueryType.LUCENE.equals(queryType)) {
                result.addAll(searchLucene(query, wikiName, wikis, hasProgrammingRights, order, number, start, withPrettyNames));
                } else {
                    result.addAll(searchDatabaseQuery(query, QueryType.XWQL.equals(queryType) ? Query.XWQL : Query.HQL, 
                        wikiName, hasProgrammingRights, distinct, number, start, withPrettyNames, className));
                }
            return result;
        } finally {
                Utils.getXWikiContext(componentManager).setDatabase(database);
        }
    }
    
    /**
     * Search for query in the given scopes. Limit the search only to Pages. Search for keyword
     * 
     * @param query
     * @param queryLanguage
     * @param wikiName
     * @param hasProgrammingRights
     * @param distinct
     * @param number
     * @param start
     * @param withPrettyNames Add the pretty names for users
     * @param className Add object of type className
     * @throws QueryException
     * @throws IllegalArgumentException
     * @throws UriBuilderException
     * @throws XWikiException
     * @return list of results
     */
    protected List<SearchResult> searchDatabaseQuery(String query, String queryType, String wikiName,
        boolean hasProgrammingRights, boolean distinct, int number, int start, Boolean withPrettyNames, String className) throws QueryException,
        IllegalArgumentException, UriBuilderException, XWikiException
    {
        String database = Utils.getXWikiContext(componentManager).getDatabase();

        /* This try is just needed for executing the finally clause. */
        try {
            List<SearchResult> result = new ArrayList<SearchResult>();

            if (query == null || query.trim().startsWith("select")) {
                return result;
            }

            Formatter f = new Formatter();
            if (distinct)
                f.format("select distinct doc.fullName, doc.space, doc.name, doc.language from XWikiDocument as doc "
                    + query);
            else
                f.format("select doc.fullName, doc.space, doc.name, doc.language from XWikiDocument as doc " + query);

            String squery = f.toString();
            if (!hasProgrammingRights) {
                squery
                    .replace("where ",
                        "where doc.space<>'XWiki' and doc.space<>'Admin' and doc.space<>'Panels' and doc.name<>'WebPreferences' and ");
            }

            List<Object> queryResult = null;

            queryResult = queryManager.createQuery(squery, queryType).setLimit(number).setOffset(start).execute();
  
            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;

                String spaceName = (String) fields[1];
                String pageName = (String) fields[2];
                String language = (String) fields[3];

                String pageId = Utils.getPageId(wikiName, spaceName, pageName);
                String pageFullName = Utils.getPageFullName(wikiName, spaceName, pageName);

                if (Utils.getXWikiApi(componentManager).hasAccessLevel("view", pageId)) {
                    Document doc = Utils.getXWikiApi(componentManager).getDocument(pageFullName);
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
                    if (withPrettyNames)
                        searchResult.setAuthorName(Utils.getAuthorName(doc.getAuthor(), componentManager));
                    if (className!=null&&!className.equals("")) {
                        BaseObject baseObject = Utils.getBaseObject(doc, className, 0, componentManager);
                        if (baseObject!=null)
                            searchResult.setObject(DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(), Utils
                                .getXWikiContext(componentManager), doc, baseObject, false, Utils.getXWikiApi(componentManager), false));
                    }

                    String pageUri = null;
                    try {
                        if (StringUtils.isBlank(language)) {
                            pageUri =
                                UriBuilder
                                    .fromUri(this.uriInfo.getBaseUri())
                                    .path(PageResource.class)
                                    .buildFromEncoded(URLEncoder.encode(wikiName, "UTF-8"),
                                        URLEncoder.encode(spaceName, "UTF-8"), URLEncoder.encode(pageName, "UTF-8"))
                                    .toString();
                        } else {
                            searchResult.setLanguage(language);
                            pageUri =
                                UriBuilder
                                    .fromUri(this.uriInfo.getBaseUri())
                                    .path(PageTranslationResource.class)
                                    .buildFromEncoded(URLEncoder.encode(wikiName, "UTF-8"),
                                        URLEncoder.encode(spaceName, "UTF-8"), URLEncoder.encode(pageName, "UTF-8"),
                                        language).toString();
                        }
                    } catch (UnsupportedEncodingException ex) {
                        // This should never happen, UTF-8 is always valid.
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
            Utils.getXWikiContext(componentManager).setDatabase(database);
        }
    }

    /**
     * Search for keyword in the given scopes. Limit the search only to Pages. Search for keyword
     * 
     * @param searchScopes
     * @param keywords
     * @param wikiName
     * @param space
     * @param hasProgrammingRights
     * @param number
     * @return
     * @throws QueryException
     * @throws IllegalArgumentException
     * @throws UriBuilderException
     * @throws XWikiException
     */
    protected List<SearchResult> searchLucene(String query, String defaultWikiName, String wikis,
        boolean hasProgrammingRights, String order, int number, int start, Boolean withPrettyNames) throws QueryException,
        IllegalArgumentException, UriBuilderException, XWikiException
    {

        /* This try is just needed for executing the finally clause. */
        try {
            List<SearchResult> result = new ArrayList<SearchResult>();

            LOGGER.error("HERE");

            if (query == null) {
                return result;
            }

            if (!hasProgrammingRights) {
                query += " AND NOT space:XWiki AND NOT space:Admin AND NOT space:Panels AND NOT name:WebPreferences";
            }

            LOGGER.error("Query is: " + query);

            try {
                XWikiContext context = Utils.getXWikiContext(componentManager);
                LucenePlugin lucene = (LucenePlugin) Utils.getXWiki(componentManager).getPlugin("lucene", context);
                SearchResults luceneSearchResults =
                    lucene.getSearchResults(query, order, (wikis == null) ? defaultWikiName : wikis, "",
                        context);
                List<com.xpn.xwiki.plugin.lucene.SearchResult> luceneResults =
                    luceneSearchResults.getResults(start, (number == -1) ? 20 : number);
                for (com.xpn.xwiki.plugin.lucene.SearchResult luceneSearchResult : luceneResults) {
                    String wikiName = luceneSearchResult.getWiki();
                    String spaceName = luceneSearchResult.getSpace();
                    String pageName = luceneSearchResult.getName();
                    String pageFullName = Utils.getPageFullName(wikiName, spaceName, pageName);
                    Document doc = Utils.getXWikiApi(componentManager).getDocument(pageFullName);
                    String title = doc.getDisplayTitle();

                    SearchResult searchResult = objectFactory.createSearchResult();
                    searchResult.setType(luceneSearchResult.getType().equals(LucenePlugin.DOCTYPE_WIKIPAGE) ? "page"
                        : "file");
                    searchResult.setId(luceneSearchResult.getId());
                    searchResult.setPageFullName(pageFullName);
                    searchResult.setTitle(title);
                    searchResult.setWiki(wikiName);
                    searchResult.setSpace(spaceName);
                    searchResult.setPageName(pageName);
                    searchResult.setVersion(doc.getVersion());
                    if (luceneSearchResult.equals(LucenePlugin.DOCTYPE_ATTACHMENT))
                        searchResult.setFilename(luceneSearchResult.getFilename());
                    searchResult.setScore(luceneSearchResult.getScore());
                    searchResult.setAuthor(luceneSearchResult.getAuthor());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(doc.getDate());
                    searchResult.setModified(calendar);
                    if (withPrettyNames)
                        searchResult.setAuthorName(Utils.getAuthorName(luceneSearchResult.getAuthor(), componentManager));

                    String language = luceneSearchResult.getLanguage();
                    if (language.equals("default"))
                        language = "";

                    String pageUri = null;
                    try {
                        if (StringUtils.isBlank(language)) {
                            pageUri =
                                UriBuilder
                                    .fromUri(this.uriInfo.getBaseUri())
                                    .path(PageResource.class)
                                    .buildFromEncoded(URLEncoder.encode(wikiName, "UTF-8"),
                                        URLEncoder.encode(spaceName, "UTF-8"), URLEncoder.encode(pageName, "UTF-8"))
                                    .toString();
                        } else {
                            searchResult.setLanguage(language);
                            pageUri =
                                UriBuilder
                                    .fromUri(this.uriInfo.getBaseUri())
                                    .path(PageTranslationResource.class)
                                    .buildFromEncoded(URLEncoder.encode(wikiName, "UTF-8"),
                                        URLEncoder.encode(spaceName, "UTF-8"), URLEncoder.encode(pageName, "UTF-8"),
                                        language).toString();
                        }
                    } catch (UnsupportedEncodingException ex) {
                        // This should never happen, UTF-8 is always valid.
                    }

                    Link pageLink = new Link();
                    pageLink.setHref(pageUri);
                    pageLink.setRel(Relations.PAGE);
                    searchResult.getLinks().add(pageLink);
                    result.add(searchResult);
                }
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Error performing lucene search", e);
            }

            return result;
        } finally {
            // Utils.getXWikiContext(componentManager).setDatabase(database);
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

