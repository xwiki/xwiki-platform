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
package org.xwiki.search.solr.internal.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.query.solr.internal.SolrQueryExecutor;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.search.AbstractSearchSource;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 * @since 6.4M1
 */
@Component
@Named("solr")
@Singleton
public class SOLRSearchSource extends AbstractSearchSource
{
    @Inject
    protected QueryManager queryManager;

    @Override
    public List<SearchResult> search(String queryString, String defaultWikiName, String wikis,
        boolean hasProgrammingRights, String orderField, String order, boolean distinct, int number, int start,
        Boolean withPrettyNames, String className, UriInfo uriInfo) throws Exception
    {
        List<SearchResult> result = new ArrayList<SearchResult>();

        if (queryString == null) {
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

        Query query = this.queryManager.createQuery(queryString, SolrQueryExecutor.SOLR);

        List<String> fq = new ArrayList<String>();

        // We want only documents
        fq.add("{!tag=type}type:(\"DOCUMENT\")");

        // Additional filter for non PR users
        if (!hasProgrammingRights) {
            fq.add("{!tag=hidden}hidden:(false)");
        }

        // Wikis
        if (StringUtils.isNotBlank(wikis)) {
            String[] strings = StringUtils.split(wikis, ',');
            if (strings.length == 1) {
                fq.add("{!tag=wiki}wiki:(\"" + strings[0] + "\")");
            } else if (strings.length > 1) {
                StringBuilder builder = new StringBuilder();
                for (String str : strings) {
                    if (builder.length() > 0) {
                        builder.append(" OR ");
                    }
                    builder.append('\'');
                    builder.append(str);
                    builder.append('\'');
                }
                fq.add("{!tag=wiki}wiki:(" + builder + ")");
            }
        }

        // TODO: current locale filtering ?

        query.bindValue("fq", fq);

        // Boost
        // FIXME: take it from configuration
        query.bindValue("qf",
            "title^10.0 name^10.0 doccontent^2.0 objcontent^0.4 filename^0.4 attcontent^0.4 doccontentraw^0.4 "
                + "author_display^0.08 creator_display^0.08 " + "comment^0.016 attauthor_display^0.016 space^0.016");

        // Order
        if (!StringUtils.isBlank(orderField)) {
            if ("desc".equals(order)) {
                query.bindValue("sort", orderField + " desc");
            } else {
                query.bindValue("sort", orderField + " asc");
            }
        }

        // Limit
        query.setLimit(number).setOffset(start);

        try {
            QueryResponse response = (QueryResponse) query.execute().get(0);

            SolrDocumentList documents = response.getResults();

            for (SolrDocument document : documents) {
                SearchResult searchResult = this.objectFactory.createSearchResult();

                searchResult.setPageFullName((String) document.get("fullname"));
                searchResult.setTitle((String) document.get("title"));
                searchResult.setWiki((String) document.get("wiki"));
                searchResult.setSpace((String) document.get("space"));
                searchResult.setPageName((String) document.get("name"));
                searchResult.setVersion((String) document.get("version"));

                searchResult.setType("page");
                searchResult.setId(Utils.getPageId(searchResult.getWiki(), searchResult.getSpace(),
                    searchResult.getPageName()));

                searchResult.setScore(((Number) document.get("score")).floatValue());
                searchResult.setAuthor((String) document.get("author"));
                Calendar calendar = Calendar.getInstance();
                calendar.setTime((Date) document.get("date"));
                searchResult.setModified(calendar);

                if (withPrettyNames) {
                    searchResult.setAuthorName((String) document.get("author_display"));
                }

                Locale locale = LocaleUtils.toLocale((String) document.get("doclocale"));

                String pageUri = null;
                if (Locale.ROOT == locale) {
                    pageUri =
                        Utils.createURI(uriInfo.getBaseUri(), PageResource.class, searchResult.getWiki(),
                            searchResult.getSpace(), searchResult.getPageName()).toString();
                } else {
                    searchResult.setLanguage(locale.toString());
                    pageUri =
                        Utils.createURI(uriInfo.getBaseUri(), PageTranslationResource.class, searchResult.getSpace(),
                            searchResult.getPageName(), locale).toString();
                }

                Link pageLink = new Link();
                pageLink.setHref(pageUri);
                pageLink.setRel(Relations.PAGE);
                searchResult.getLinks().add(pageLink);

                result.add(searchResult);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Error performing solr search", e);
        }

        return result;
    }
}
