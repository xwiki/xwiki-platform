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

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import jakarta.inject.Singleton;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.search.solr.internal.api.FieldUtils;

import com.xpn.xwiki.XWikiException;

/**
 * Converts Solr search results into {@link SearchResult} objects for REST API responses.
 *
 * @version $Id$
 * @since 17.5.0RC1
 */
@Component(roles = SearchResultConverter.class)
@Singleton
public class SearchResultConverter
{
    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    private ModelFactory modelFactory;

    private final ObjectFactory objectFactory = new ObjectFactory();

    /**
     * Convert Solr search results from the given query to {@link SearchResult} objects.
     *
     * @param withPrettyNames whether to include pretty names in the search results
     * @param query the Solr query to execute
     * @param baseUri the base URI for constructing links
     * @param includeHierarchy whether to include hierarchy information in the search results
     * @return a list of {@link SearchResult} objects representing the search results
     * @throws XWikiException if an error occurs while executing the query or processing the results
     */
    public List<SearchResult> getSolrSearchResults(Boolean withPrettyNames, Query query, URI baseUri,
        boolean includeHierarchy)
        throws XWikiException
    {
        List<SearchResult> result = new ArrayList<>();

        try {
            QueryResponse response = (QueryResponse) query.execute().get(0);

            SolrDocumentList documents = response.getResults();

            for (SolrDocument document : documents) {
                result.add(convertSolrDocumentToSearchResult(document, baseUri, withPrettyNames, includeHierarchy));
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Error performing solr search", e);
        }

        return result;
    }

    private SearchResult convertSolrDocumentToSearchResult(SolrDocument document, URI baseUri, Boolean withPrettyNames,
        boolean includeHierarchy)
    {
        SearchResult searchResult = this.objectFactory.createSearchResult();

        DocumentReference documentReference = this.solrDocumentReferenceResolver.resolve(document);
        searchResult.setPageFullName(this.localEntityReferenceSerializer.serialize(documentReference));
        searchResult.setWiki(documentReference.getWikiReference().getName());
        searchResult.setSpace(this.localEntityReferenceSerializer.serialize(documentReference.getParent()));
        searchResult.setPageName(documentReference.getName());
        searchResult.setVersion((String) document.get(FieldUtils.VERSION));

        List<String> spaces = Utils.getSpaces(documentReference);

        searchResult.setType("page");
        searchResult.setId(Utils.getPageId(searchResult.getWiki(), spaces, searchResult.getPageName()));

        searchResult.setScore(((Number) document.get(FieldUtils.SCORE)).floatValue());
        searchResult.setAuthor((String) document.get(FieldUtils.AUTHOR));
        searchResult.setModified(DateUtils.toCalendar((Date) document.get(FieldUtils.DATE)));

        if (Boolean.TRUE.equals(withPrettyNames)) {
            searchResult.setAuthorName((String) document.get(FieldUtils.AUTHOR_DISPLAY));
        }

        Locale docLocale = LocaleUtils.toLocale((String) document.get(FieldUtils.DOCUMENT_LOCALE));
        Locale locale = LocaleUtils.toLocale((String) document.get(FieldUtils.LOCALE));

        searchResult.setTitle((String) document.getFirstValue(
            FieldUtils.getFieldName(FieldUtils.TITLE, locale)));

        String pageUri;
        if (Locale.ROOT == docLocale) {
            pageUri = Utils.createURI(baseUri, PageResource.class, searchResult.getWiki(),
                Utils.getSpacesURLElements(spaces), searchResult.getPageName()).toString();
        } else {
            searchResult.setLanguage(docLocale.toString());
            pageUri =
                Utils.createURI(baseUri, PageTranslationResource.class, searchResult.getWiki(),
                    Utils.getSpacesURLElements(spaces), searchResult.getPageName(), docLocale).toString();
        }

        searchResult.getLinks().add(this.objectFactory.createLink().withHref(pageUri).withRel(Relations.PAGE));

        if (includeHierarchy) {
            searchResult.setHierarchy(this.modelFactory.toRestHierarchy(documentReference, withPrettyNames));
        }
        return searchResult;
    }
}
