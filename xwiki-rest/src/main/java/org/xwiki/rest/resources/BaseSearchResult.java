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

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.Relations;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.pages.PageResource;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 */
public class BaseSearchResult extends XWikiResource
{
    protected static enum SearchScope
    {
        NAME,
        CONTENT,
        TITLE,
        OBJECTS
    }

    protected List<SearchResult> searchPages(List<SearchScope> searchScopes, String keywords, String wikiName,
        String space, boolean hasProgrammingRights, int number) throws QueryException, IllegalArgumentException,
        UriBuilderException, XWikiException
    {
        List<SearchResult> result = new ArrayList<SearchResult>();

        if (keywords == null) {
            return result;
        }

        Formatter f = new Formatter();

        if (space != null) {
            f.format("select doc.space, doc.name from XWikiDocument as doc where doc.space = :space and ( ");
        } else {
            f.format("select doc.space, doc.name from XWikiDocument as doc where ( ");
        }

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

        if (acceptedScopes == 0) {
            return result;
        }

        if (hasProgrammingRights) {
            f.format(") order by doc.date desc");
        } else {
            f
                .format(") and doc.space<>'XWiki' and doc.space<>'Admin' and doc.space<>'Panels' and doc.name<>'WebPreferences' order by doc.date desc");
        }

        String query = f.toString();

        QueryManager queryManager = (QueryManager) com.xpn.xwiki.web.Utils.getComponent(QueryManager.class);

        List<Object> queryResult = null;

        if (space != null) {
            queryResult =
                queryManager.createQuery(query, Query.XWQL).bindValue("keywords",
                    String.format("%%%s%%", keywords.toUpperCase())).bindValue("space", space).setLimit(number)
                    .execute();
        } else {
            queryResult =
                queryManager.createQuery(query, Query.XWQL).bindValue("keywords",
                    String.format("%%%s%%", keywords.toUpperCase())).setLimit(number).execute();
        }

        for (Object object : queryResult) {
            Object[] fields = (Object[]) object;

            String spaceName = (String) fields[0];
            String pageName = (String) fields[1];

            String pageId = Utils.getPageId(wikiName, spaceName, pageName);

            if (xwikiApi.hasAccessLevel("view", pageId)) {
                SearchResult searchResult = objectFactory.createSearchResult();
                searchResult.setType("page");
                searchResult.setId(pageId);
                searchResult.setPageFullName(Utils.getPageFullName(wikiName, spaceName, pageName));
                searchResult.setWiki(wikiName);
                searchResult.setSpace(spaceName);
                searchResult.setPageName(pageName);

                String pageUri =
                    UriBuilder.fromUri(uriInfo.getBaseUri()).path(PageResource.class).build(wikiName, spaceName,
                        pageName).toString();
                Link pageLink = new Link();
                pageLink.setHref(pageUri);
                pageLink.setRel(Relations.PAGE);
                searchResult.getLinks().add(pageLink);

                result.add(searchResult);
            }
        }

        return result;
    }

    protected List<SearchResult> searchObjects(String keywords, String wikiName, String space,
        boolean hasProgrammingRights, int number) throws QueryException, IllegalArgumentException, UriBuilderException,
        XWikiException
    {
        List<String> addedIds = new ArrayList<String>();
        List<SearchResult> result = new ArrayList<SearchResult>();

        if (keywords == null) {
            return result;
        }

        Formatter f = new Formatter();

        if (space != null) {
            f
                .format("select doc.space, doc.name, obj.className, obj.number from XWikiDocument as doc, BaseObject as obj, StringProperty as sp, LargeStringProperty as lsp where doc.space = :space and obj.name=doc.fullName and sp.id.id = obj.id and lsp.id.id = obj.id and (upper(sp.value) like :keywords or upper(lsp.value) like :keywords) ");
        } else {
            f
                .format("select doc.space, doc.name, obj.className, obj.number from XWikiDocument as doc, BaseObject as obj, StringProperty as sp, LargeStringProperty as lsp where obj.name=doc.fullName and sp.id.id = obj.id and lsp.id.id = obj.id and (upper(sp.value) like :keywords or upper(lsp.value) like :keywords) ");
        }

        if (hasProgrammingRights) {
            f.format(" order by doc.date desc");
        } else {
            f
                .format(" and doc.space<>'XWiki' and doc.space<>'Admin' and doc.space<>'Panels' and doc.name<>'WebPreferences' order by doc.date desc");
        }

        String query = f.toString();

        System.out.format("Query: %s\n", query);

        QueryManager queryManager = (QueryManager) com.xpn.xwiki.web.Utils.getComponent(QueryManager.class);

        List<Object> queryResult = null;
        if (space != null) {
            queryResult =
                queryManager.createQuery(query, Query.XWQL).bindValue("keywords",
                    String.format("%%%s%%", keywords.toUpperCase())).bindValue("space", space).setLimit(number)
                    .execute();
        } else {
            queryResult =
                queryManager.createQuery(query, Query.XWQL).bindValue("keywords",
                    String.format("%%%s%%", keywords.toUpperCase())).setLimit(number).execute();
        }

        for (Object object : queryResult) {
            Object[] fields = (Object[]) object;

            String spaceName = (String) fields[0];
            String pageName = (String) fields[1];
            String className = (String) fields[2];
            int objectNumber = (Integer) fields[3];

            String id = Utils.getObjectId(wikiName, spaceName, pageName, className, objectNumber);
            /* Avoid duplicates */
            if (!addedIds.contains(id)) {
                String pageId = Utils.getPageId(wikiName, spaceName, pageName);
                if (xwikiApi.hasAccessLevel("view", pageId)) {
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
                        UriBuilder.fromUri(uriInfo.getBaseUri()).path(PageResource.class).build(wikiName, spaceName,
                            pageName).toString();
                    Link pageLink = new Link();
                    pageLink.setHref(pageUri);
                    pageLink.setRel(Relations.PAGE);
                    searchResult.getLinks().add(pageLink);

                    String objectUri =
                        UriBuilder.fromUri(uriInfo.getBaseUri()).path(ObjectResource.class).build(wikiName, spaceName,
                            pageName, className, objectNumber).toString();
                    Link objectLink = new Link();
                    objectLink.setHref(objectUri);
                    objectLink.setRel(Relations.OBJECT);
                    searchResult.getLinks().add(objectLink);

                    result.add(searchResult);

                    addedIds.add(id);
                }
            }
        }

        return result;
    }

}
