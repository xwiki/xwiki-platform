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
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.HiddenDocumentFilter;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

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
    @Named("secure")
    protected QueryManager queryManager;

    @Inject
    @Named(HiddenDocumentFilter.HINT)
    protected QueryFilter hiddenFilter;

    @Inject
    protected ModelFactory modelFactory;

    @Inject
    protected DocumentReferenceResolver<String> resolver;

    @Inject
    protected ContextualAuthorizationManager authorization;

    protected final String queryLanguage;

    public AbstractDatabaseSearchSource(String queryLanguage)
    {
        this.queryLanguage = queryLanguage;
    }

    protected abstract String resolveQuery(boolean distinct, String query);

    @Override
    public List<SearchResult> search(String partialQueryString, String wikiName, String wikis,
        boolean hasProgrammingRights, String orderField, String order, boolean distinct, int number, int start,
        Boolean withPrettyNames, String className, UriInfo uriInfo) throws Exception
    {
        XWikiContext xwikiContext = this.xcontextProvider.get();
        XWiki xwikiApi = new XWiki(xwikiContext.getWiki(), xwikiContext);

        if (partialQueryString == null || StringUtils.startsWithIgnoreCase(partialQueryString, "select")) {
            return Collections.emptyList();
        }

        String queryString = resolveQuery(distinct, partialQueryString);

        Query query = this.queryManager.createQuery(queryString, this.queryLanguage);

        query.setLimit(number).setOffset(start);
        query.setWiki(wikiName);

        List<Object> queryResult = query.execute();

        WikiReference wikiReference = new WikiReference(wikiName);

        /* Build the result. */
        List<SearchResult> result = new ArrayList<>();
        for (Object object : queryResult) {
            Object[] fields = (Object[]) object;

            String fullName = (String) fields[0];
            String language = (String) fields[3];

            DocumentReference documentReference = this.resolver.resolve(fullName, wikiReference);

            /* Check if the user has the right to see the found document */
            if (this.authorization.hasAccess(Right.VIEW, documentReference)) {
                Document doc = xwikiApi.getDocument(documentReference);
                String title = doc.getDisplayTitle();

                SearchResult searchResult = this.objectFactory.createSearchResult();
                searchResult.setType("page");
                searchResult.setId(doc.getPrefixedFullName());
                searchResult.setPageFullName(doc.getFullName());
                searchResult.setTitle(title);
                searchResult.setWiki(wikiName);
                searchResult.setSpace(doc.getSpace());
                searchResult.setPageName(doc.getDocumentReference().getName());
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
                if (StringUtils.isNotEmpty(className) && xwikiContext.getUserReference() != null) {
                    XWikiDocument xdocument =
                        xwikiContext.getWiki().getDocument(doc.getDocumentReference(), xwikiContext);
                    BaseObject baseObject = xdocument.getObject(className);
                    if (baseObject != null) {
                        searchResult.setObject(
                            this.modelFactory.toRestObject(uriInfo.getBaseUri(), doc, baseObject, false, false));
                    }
                }

                String pageUri;
                if (StringUtils.isBlank(language)) {
                    pageUri = Utils.createURI(uriInfo.getBaseUri(), PageResource.class, wikiName,
                        Utils.getSpacesURLElements(documentReference.getLastSpaceReference()),
                        documentReference.getName()).toString();
                } else {
                    searchResult.setLanguage(language);
                    pageUri = Utils.createURI(uriInfo.getBaseUri(), PageTranslationResource.class, wikiName,
                        Utils.getSpacesURLElements(documentReference.getLastSpaceReference()),
                        documentReference.getName(), language).toString();
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
