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
import java.util.Formatter;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
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

/**
 * @version $Id$
 */
public class BaseSearchResult extends XWikiResource
{
    protected static final String SEARCH_TEMPLATE_INFO = "q={keywords}(&scope={content|name|title|spaces|objects})*";

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
     * @param searchScopes
     * @param keywords
     * @param wikiName
     * @param space
     * @param hasProgrammingRights
     * @param number
     * @return
     * @throws IllegalArgumentException
     * @throws UriBuilderException
     * @throws QueryException
     * @throws XWikiException
     */
    protected List<SearchResult> search(List<SearchScope> searchScopes, String keywords, String wikiName, String space,
        boolean hasProgrammingRights, int number) throws IllegalArgumentException, UriBuilderException, QueryException,
        XWikiException
    {
        String database = Utils.getXWikiContext(componentManager).getDatabase();

        /* This try is just needed for executing the finally clause. */
        try {
            Utils.getXWikiContext(componentManager).setDatabase(wikiName);

            List<SearchResult> result = new ArrayList<SearchResult>();

            result.addAll(searchPages(searchScopes, keywords, wikiName, space, hasProgrammingRights, number));

            if (searchScopes.contains(SearchScope.SPACES)) {
                result.addAll(searchSpaces(keywords, wikiName, hasProgrammingRights, number));
            }

            if (searchScopes.contains(SearchScope.OBJECTS)) {
                result.addAll(searchObjects(keywords, wikiName, space, hasProgrammingRights, number));
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
        String space, boolean hasProgrammingRights, int number) throws QueryException, IllegalArgumentException,
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

            if (space != null) {
                f.format("select distinct doc.fullName, doc.space, doc.name, doc.language from XWikiDocument as doc where doc.space = :space and ( ");
            } else {
                f.format("select distinct doc.fullName, doc.space, doc.name, doc.language from XWikiDocument as doc where ( ");
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

            if (hasProgrammingRights) {
                f.format(") order by doc.fullName asc");
            } else {
                f.format(") and doc.space<>'XWiki' and doc.space<>'Admin' and doc.space<>'Panels' and doc.name<>'WebPreferences' order by doc.fullName asc");
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
                String language = (String) fields[3];

                String pageId = Utils.getPageId(wikiName, spaceName, pageName);

                if (Utils.getXWikiApi(componentManager).hasAccessLevel("view", pageId)) {
                    SearchResult searchResult = objectFactory.createSearchResult();
                    searchResult.setType("page");
                    searchResult.setId(pageId);
                    searchResult.setPageFullName(Utils.getPageFullName(wikiName, spaceName, pageName));
                    searchResult.setWiki(wikiName);
                    searchResult.setSpace(spaceName);
                    searchResult.setPageName(pageName);

                    String pageUri = null;
                    try {
                        if (StringUtils.isBlank(language)) {
                            pageUri =
                                UriBuilder.fromUri(this.uriInfo.getBaseUri()).path(PageResource.class)
                                .buildFromEncoded(URLEncoder.encode(wikiName, "UTF-8"),
                                URLEncoder.encode(spaceName, "UTF-8"),
                                URLEncoder.encode(pageName, "UTF-8")).toString();
                        } else {
                            searchResult.setLanguage(language);
                            pageUri =
                                UriBuilder.fromUri(this.uriInfo.getBaseUri()).path(PageTranslationResource.class)
                                .buildFromEncoded(URLEncoder.encode(wikiName, "UTF-8"),
                                URLEncoder.encode(spaceName, "UTF-8"),
                                URLEncoder.encode(pageName, "UTF-8"), language).toString();
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
    protected List<SearchResult> searchSpaces(String keywords, String wikiName, boolean hasProgrammingRights, int number)
        throws QueryException, IllegalArgumentException, UriBuilderException, XWikiException
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
                    .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase())).setLimit(number).execute();

            for (Object object : queryResult) {

                String spaceName = (String) object;

                SearchResult searchResult = objectFactory.createSearchResult();
                searchResult.setType("space");
                searchResult.setId(String.format("%s:%s", wikiName, spaceName));
                searchResult.setWiki(wikiName);
                searchResult.setSpace(spaceName);

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
        boolean hasProgrammingRights, int number) throws QueryException, IllegalArgumentException, UriBuilderException,
        XWikiException
    {
        String database = Utils.getXWikiContext(componentManager).getDatabase();

        /* This try is just needed for executing the finally clause. */
        try {
            List<SearchResult> result = new ArrayList<SearchResult>();

            if (keywords == null) {
                return result;
            }

            Formatter f = new Formatter();

            if (space != null) {
                f.format("select distinct doc.fullName, doc.space, doc.name, obj.className, obj.number from XWikiDocument as doc, BaseObject as obj, StringProperty as sp, LargeStringProperty as lsp where doc.space = :space and obj.name=doc.fullName and sp.id.id = obj.id and lsp.id.id = obj.id and (upper(sp.value) like :keywords or upper(lsp.value) like :keywords) ");
            } else {
                f.format("select distinct doc.fullName, doc.space, doc.name, obj.className, obj.number from XWikiDocument as doc, BaseObject as obj, StringProperty as sp, LargeStringProperty as lsp where obj.name=doc.fullName and sp.id.id = obj.id and lsp.id.id = obj.id and (upper(sp.value) like :keywords or upper(lsp.value) like :keywords) ");
            }

            if (hasProgrammingRights) {
                f.format(" order by doc.fullName asc");
            } else {
                f.format(" and doc.space<>'XWiki' and doc.space<>'Admin' and doc.space<>'Panels' and doc.name<>'WebPreferences' order by doc.fullName asc");
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
                if (Utils.getXWikiApi(componentManager).hasAccessLevel("view", pageId)) {
                    SearchResult searchResult = objectFactory.createSearchResult();
                    searchResult.setType("object");
                    searchResult.setId(id);
                    searchResult.setPageFullName(Utils.getPageFullName(wikiName, spaceName, pageName));
                    searchResult.setWiki(wikiName);
                    searchResult.setSpace(spaceName);
                    searchResult.setPageName(pageName);
                    searchResult.setClassName(className);
                    searchResult.setObjectNumber(objectNumber);

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
