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
package com.xpn.xwiki.plugin.lucene.internal.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.search.AbstractSearchSource;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.plugin.lucene.LucenePlugin;
import com.xpn.xwiki.plugin.lucene.SearchResults;

/**
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("lucene")
@Singleton
public class LuceneSearchSource extends AbstractSearchSource
{
    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Override
    public List<SearchResult> search(String query, String defaultWikiName, String wikis, boolean hasProgrammingRights,
        String orderField, String order, boolean distinct, int number, int start, Boolean withPrettyNames,
        String className, UriInfo uriInfo) throws Exception
    {
        XWikiContext xwikiContext = this.xcontextProvider.get();
        XWiki xwikiApi = new XWiki(xwikiContext.getWiki(), xwikiContext);

        List<SearchResult> result = new ArrayList<SearchResult>();

        if (query == null) {
            return result;
        }

        /*
         * One of the two must be non-null. If default wiki name is non-null and wikis is null, then it's a local search
         * in a specific wiki. If wiki name is null and wikis is non-null it's a global query on different wikis. If
         * both of them are non-null then the wikis parameter takes the precedence.
         */
        if (defaultWikiName == null && wikis == null) {
            return result;
        }

        if (!hasProgrammingRights) {
            query += " AND NOT space:XWiki AND NOT space:Admin AND NOT space:Panels AND NOT name:WebPreferences";
        }

        try {
            LucenePlugin lucene = (LucenePlugin) xwikiContext.getWiki().getPlugin("lucene", xwikiContext);

            /*
             * Compute the parameter to be passed to the plugin for ordering: orderField (for ordering on orderField in
             * ascending order) or -orderFiled (for descending order)
             */
            String orderParameter = "";
            if (!StringUtils.isBlank(orderField)) {
                if ("desc".equals(order)) {
                    orderParameter = String.format("-%s", orderField);
                } else {
                    orderParameter = orderField;
                }
            }

            SearchResults luceneSearchResults =
                lucene.getSearchResults(query, orderParameter, (wikis == null) ? defaultWikiName : wikis, "",
                    xwikiContext);

            /*
             * Return only the first 20 results otherwise specified. It also seems that Lucene indexing starts at 1
             * (though starting from 0 works as well, and gives the samer results as if starting from 1). To keep things
             * consistent we add 1 to the passed start value (which is always 0-based).
             */
            List<com.xpn.xwiki.plugin.lucene.SearchResult> luceneResults =
                luceneSearchResults.getResults(start + 1, (number == -1) ? 20 : number);

            /* Build the result. */
            for (com.xpn.xwiki.plugin.lucene.SearchResult luceneSearchResult : luceneResults) {
                String wikiName = luceneSearchResult.getWiki();
                String spaceName = luceneSearchResult.getSpace();
                String pageName = luceneSearchResult.getName();
                String pageFullName = Utils.getPageFullName(wikiName, spaceName, pageName);
                String pageId = Utils.getPageId(wikiName, spaceName, pageName);

                /* Check if the user has the right to see the found document */
                if (xwikiApi.hasAccessLevel("view", pageId)) {
                    Document doc = xwikiApi.getDocument(pageId);
                    String title = doc.getDisplayTitle();

                    SearchResult searchResult = objectFactory.createSearchResult();

                    searchResult.setPageFullName(pageFullName);
                    searchResult.setTitle(title);
                    searchResult.setWiki(wikiName);
                    searchResult.setSpace(spaceName);
                    searchResult.setPageName(pageName);
                    searchResult.setVersion(doc.getVersion());

                    /*
                     * Check if the result is a page or an attachment, and fill the corresponding fields in the result
                     * accordingly.
                     */
                    if (luceneSearchResult.getType().equals(LucenePlugin.DOCTYPE_WIKIPAGE)) {
                        searchResult.setType("page");
                        searchResult.setId(Utils.getPageId(wikiName, spaceName, pageName));
                    } else {
                        searchResult.setType("file");
                        searchResult.setId(String.format("%s@%s", Utils.getPageId(wikiName, pageFullName),
                            luceneSearchResult.getFilename()));
                        searchResult.setFilename(luceneSearchResult.getFilename());

                        String attachmentUri =
                            Utils.createURI(uriInfo.getBaseUri(), AttachmentResource.class, wikiName, spaceName,
                                pageName, luceneSearchResult.getFilename()).toString();

                        Link attachmentLink = new Link();
                        attachmentLink.setHref(attachmentUri);
                        attachmentLink.setRel(Relations.ATTACHMENT_DATA);
                        searchResult.getLinks().add(attachmentLink);
                    }

                    searchResult.setScore(luceneSearchResult.getScore());
                    searchResult.setAuthor(luceneSearchResult.getAuthor());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(doc.getDate());
                    searchResult.setModified(calendar);

                    if (withPrettyNames) {
                        searchResult.setAuthorName(xwikiApi.getUserName(luceneSearchResult.getAuthor(), false));
                    }

                    String language = luceneSearchResult.getLanguage();
                    if (language.equals("default")) {
                        language = "";
                    }

                    String pageUri = null;
                    if (StringUtils.isBlank(language)) {
                        pageUri =
                            Utils.createURI(uriInfo.getBaseUri(), PageResource.class, wikiName, spaceName, pageName)
                                .toString();
                    } else {
                        searchResult.setLanguage(language);
                        pageUri =
                            Utils.createURI(uriInfo.getBaseUri(), PageTranslationResource.class, wikiName, spaceName,
                                pageName, language).toString();
                    }

                    Link pageLink = new Link();
                    pageLink.setHref(pageUri);
                    pageLink.setRel(Relations.PAGE);
                    searchResult.getLinks().add(pageLink);

                    result.add(searchResult);
                }
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Error performing lucene search", e);
        }

        return result;
    }
}
