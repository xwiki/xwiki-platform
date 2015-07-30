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
package org.xwiki.rest.internal.resources.search;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 * @since 6.1M2
 */
public abstract class AbstractDatabaseSearchSource extends AbstractSearchSource
{
    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected QueryManager queryManager;

    protected final String queryLanguage;

    public AbstractDatabaseSearchSource(String queryLanguage)
    {
        this.queryLanguage = queryLanguage;
    }

    @Override
    public List<SearchResult> search(String query, String wikiName, String wikis, boolean hasProgrammingRights,
        String orderField, String order, boolean distinct, int number, int start, Boolean withPrettyNames,
        String className, UriInfo uriInfo) throws Exception
    {
        XWikiContext xwikiContext = this.xcontextProvider.get();
        XWiki xwikiApi = new XWiki(xwikiContext.getWiki(), xwikiContext);

        List<SearchResult> result = new ArrayList<SearchResult>();

        if (query == null || query.trim().startsWith("select")) {
            return result;
        }

        Formatter f = new Formatter();
        if (distinct) {
            f.format("select distinct doc.fullName, doc.space, doc.name, doc.language from XWikiDocument as doc %s",
                query);
        } else {
            f.format("select doc.fullName, doc.space, doc.name, doc.language from XWikiDocument as doc %s", query);
        }
        String squery = f.toString();

        if (!hasProgrammingRights) {
            squery
                .replace("where ",
                    "where doc.space<>'XWiki' and doc.space<>'Admin' and doc.space<>'Panels' and doc.name<>'WebPreferences' and ");
        }

        List<Object> queryResult = null;

        queryResult =
            this.queryManager.createQuery(squery, this.queryLanguage).setLimit(number).setOffset(start).execute();

        /* Build the result. */
        for (Object object : queryResult) {
            Object[] fields = (Object[]) object;

            String spaceId = (String) fields[1];
            String pageName = (String) fields[2];
            String language = (String) fields[3];
            
            List<String> spaces = Utils.getSpacesFromSpaceId(spaceId);

            String pageId = Utils.getPageId(wikiName, spaces, pageName);
            String pageFullName = Utils.getPageFullName(wikiName, spaces, pageName);

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
                searchResult.setSpace(spaceId);
                searchResult.setPageName(pageName);
                searchResult.setVersion(doc.getVersion());
                searchResult.setAuthor(doc.getAuthor());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(doc.getDate());
                searchResult.setModified(calendar);

                if (withPrettyNames) {
                    searchResult.setAuthorName(xwikiApi.getUserName(doc.getAuthor(), false));
                }

                /*
                 * Avoid to return object information if the user is not authenticated. This will prevent crawlers to
                 * retrieve information such as email addresses and passwords from user's profiles.
                 */
                if (className != null && !className.equals("") && xwikiContext.getUserReference() != null) {
                    XWikiDocument xdocument =
                        xwikiContext.getWiki().getDocument(doc.getDocumentReference(), xwikiContext);
                    BaseObject baseObject = xdocument.getObject(className);
                    if (baseObject != null) {
                        searchResult.setObject(DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(),
                            xwikiContext, doc, baseObject, false, xwikiApi, false));
                    }
                }

                String pageUri = null;
                if (StringUtils.isBlank(language)) {
                    pageUri =
                        Utils.createURI(uriInfo.getBaseUri(), PageResource.class, wikiName, spaces, pageName)
                            .toString();
                } else {
                    searchResult.setLanguage(language);
                    pageUri =
                        Utils.createURI(uriInfo.getBaseUri(), PageTranslationResource.class, wikiName, spaces,
                            pageName, language).toString();
                }

                Link pageLink = new Link();
                pageLink.setHref(pageUri);
                pageLink.setRel(Relations.PAGE);
                searchResult.getLinks().add(pageLink);

                result.add(searchResult);
            }
        }

        return result;
    }
}
