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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.resources.KeywordSearchOptions;
import org.xwiki.rest.internal.resources.KeywordSearchScope;
import org.xwiki.rest.internal.resources.KeywordSearchSource;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserPropertiesResolver;

import com.xpn.xwiki.XWikiException;

import static java.util.function.Predicate.not;
import static org.xwiki.rest.internal.resources.KeywordSearchScope.CONTENT;
import static org.xwiki.rest.internal.resources.KeywordSearchScope.NAME;
import static org.xwiki.rest.internal.resources.KeywordSearchScope.TITLE;

/**
 * A {@link KeywordSearchSource} implementation that uses Solr to search for wiki pages.
 *
 * @version $Id$
 * @since 17.5.0
 */
@Component
@Singleton
@Named("solr")
public class SolrKeywordSearchSource implements KeywordSearchSource
{
    protected static final Set<KeywordSearchScope> PAGE_SCOPES = EnumSet.of(TITLE, NAME, CONTENT);

    private static final String WIKI_SEPARATOR = ":";

    private static final String ASC = "asc";

    /**
     * Mapping of database fields that are supported as order parameters with corresponding Solr fields.
     */
    private static final Map<String, String> ORDER_FIELD_DATABASE_TO_SOLR = Map.ofEntries(
        Map.entry("fullName", FieldUtils.FULLNAME),
        Map.entry("name", FieldUtils.NAME_EXACT),
        Map.entry("title", FieldUtils.TITLE_SORT),
        Map.entry("language", FieldUtils.LANGUAGE),
        Map.entry("date", FieldUtils.DATE),
        Map.entry("creationDate", FieldUtils.CREATIONDATE),
        Map.entry("author", FieldUtils.AUTHOR_DISPLAY_SORT),
        Map.entry("creator", FieldUtils.CREATOR),
        Map.entry("space", FieldUtils.SPACE_EXACT),
        Map.entry("version", FieldUtils.VERSION),
        Map.entry("hidden", FieldUtils.HIDDEN)
    );

    private static final String SEARCH_WILDCARD = "*";

    private static final String SORT_PARAMETER = "sort";

    @Inject
    private LocalizationContext localizationContext;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    private SolrUtils solrUtils;

    /**
     * The query manager to be used to perform low-level queries for retrieving information about wiki content.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * Used to retrieve user preference regarding hidden documents.
     */
    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Inject
    @Named("database")
    private KeywordSearchSource databaseKeywordSearchSource;

    @Inject
    private SearchResultConverter searchResultConverter;

    @Override
    public List<SearchResult> search(String keywords, KeywordSearchOptions options, URI baseURI)
        throws XWikiRestException
    {
        List<SearchResult> results = new ArrayList<>();

        if (options.searchScopes().stream().anyMatch(PAGE_SCOPES::contains)) {
            try {
                results.addAll(searchPages(keywords, options, baseURI));
            } catch (Exception e) {
                throw new XWikiRestException("Error searching pages", e);
            }
        }

        // Scopes beyond the page name/title/content aren't implemented yet in Solr, fall back to database search.
        List<KeywordSearchScope> otherScopes =
            options.searchScopes().stream().filter(not(PAGE_SCOPES::contains)).toList();
        if (!otherScopes.isEmpty()) {
            KeywordSearchOptions databaseSearchOptions = options.but().searchScopes(otherScopes).build();
            results.addAll(this.databaseKeywordSearchSource.search(keywords, databaseSearchOptions, baseURI));
        }

        return results;
    }

    private List<SearchResult> searchPages(String keywords, KeywordSearchOptions options, URI baseURI)
        throws XWikiException, QueryException
    {
        List<String> filterQueries = new ArrayList<>();
        filterQueries.add("type:DOCUMENT");
        if (!this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE).displayHiddenDocuments()) {
            filterQueries.add("hidden:false");
        }
        if (StringUtils.isNotBlank(options.wikiName())) {
            filterQueries.add("wiki:" + this.solrUtils.toCompleteFilterQueryString(options.wikiName()));
        }
        if (StringUtils.isNotBlank(options.space())) {
            filterQueries.add("space_exact:" + this.solrUtils.toCompleteFilterQueryString(options.space()));
        }

        if (Boolean.TRUE.equals(options.isLocaleAware())) {
            // Match translations but also the main document when no translation is available by using the locales
            // field.
            String currentLocale =
                this.solrUtils.toCompleteFilterQueryString(this.localizationContext.getCurrentLocale());
            filterQueries.add("locales:%s".formatted(currentLocale));
        }

        String queryString = getQueryString(options.searchScopes(), keywords, options.wikiName());

        Query query = this.queryManager.createQuery(queryString, "solr");
        ((SecureQuery) query).checkCurrentUser(true);
        query.setLimit(options.number());
        query.setOffset(options.start());
        query.bindValue("fq", filterQueries);
        addSortValue(options.orderField(), options.order(), query);
        return this.searchResultConverter.getSolrSearchResults(options.withPrettyNames(), query, baseURI, true);
    }

    private String getQueryString(List<KeywordSearchScope> searchScopes, String keywords, String wikiName)
        throws XWikiException
    {
        if (StringUtils.isBlank(keywords)) {
            return SEARCH_WILDCARD;
        }

        // Wildcard queries completely bypass the tokenizer. For this reason, we can't just take the full query and
        // append a wildcard as it wouldn't match anything when the query is more than a single word. Instead, we
        // perform a hybrid approach: we pass the query without wildcards to use the regular tokenizer, and we add a
        // wildcard to the last token of the query as this is the word that is most likely incomplete.
        String wildcardQuery =
            this.solrUtils.toCompleteFilterQueryString(getLastTerm(keywords).toLowerCase()) + SEARCH_WILDCARD;

        // The passed query could also be a local or absolute document reference.
        // Convert a local reference to an absolute reference.
        String escapedReference = getEscapedAbsoluteReferenceForWiki(keywords, wikiName);

        String webHome = this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();

        String escapedKeyWords = this.solrUtils.toFilterQueryString(keywords);
        return searchScopes.stream()
            .filter(PAGE_SCOPES::contains)
            .map(scope ->
                switch (scope) {
                    // Consider matches in the title as way more important.
                    // Use title_ for wildcard matches to ensure we're using the same tokenizer as the field.
                    case TITLE -> "(title:" + escapedKeyWords + " OR title_:" + wildcardQuery + ")^4";
                    // Prefer matching in the name over the spaces, but only if the name is not "WebHome".
                    case NAME -> "spaces:" + escapedKeyWords + " OR spaces:" + wildcardQuery
                        + " OR (name:" + escapedKeyWords + " OR name:" + wildcardQuery + " -name_exact:" + webHome
                        + ")^2"
                        // Try matching the exact reference - if this matches, it should be the first result.
                        + " OR (reference:document\\:" + escapedReference + ")^1000";
                    // No wildcard for the content as it might be too expensive.
                    case CONTENT -> "doccontent:" + escapedKeyWords;
                    default -> throw new IllegalStateException("Unexpected value: " + scope);
                })
            .collect(Collectors.joining(" OR "));
    }

    private void addSortValue(String orderField, String order, Query query)
    {
        // Ordering was designed for database search. It might behave very differently here.
        // Still, if explicitly requested and supported by Solr, simulate ordering by a similar field.
        if (StringUtils.isNotBlank(orderField) && ORDER_FIELD_DATABASE_TO_SOLR.containsKey(orderField)) {
            query.bindValue(SORT_PARAMETER, ORDER_FIELD_DATABASE_TO_SOLR.get(orderField) + " " + getValidOrder(order));
        } else if (SEARCH_WILDCARD.equals(query.getStatement())) {
            // Return the most recently modified documents when the query is empty.
            query.bindValue(SORT_PARAMETER, "date desc");
        }
    }

    private String getEscapedAbsoluteReferenceForWiki(String keywords, String wikiName)
    {
        String absoluteReference;
        if (!StringUtils.startsWith(keywords, wikiName + WIKI_SEPARATOR)) {
            absoluteReference = wikiName + WIKI_SEPARATOR + keywords;
        } else {
            absoluteReference = keywords;
        }
        return this.solrUtils.toCompleteFilterQueryString(absoluteReference);
    }

    private static String getLastTerm(String keywords) throws XWikiException
    {
        List<String> tokens = new ArrayList<>();

        try (Analyzer analyzer = new StandardAnalyzer();
             TokenStream tokenStream = analyzer.tokenStream("content", keywords))
        {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                tokens.add(tokenStream.getAttribute(CharTermAttribute.class).toString());
            }
            tokenStream.end();
        } catch (IOException e) {
            throw new XWikiException("Failed to tokenize the query", e);
        }

        // Take the last token, if there is no token, just take the full query.
        String lastTerm;
        if (!tokens.isEmpty()) {
            lastTerm = tokens.get(tokens.size() - 1);
        } else {
            lastTerm = keywords;
        }
        return lastTerm;
    }

    /**
     * Validates the order parameter and returns either "asc" or "desc".
     *
     * @param order The order string to validate
     * @return "asc" or "desc", defaulting to "asc" if the input is invalid
     */
    private String getValidOrder(String order)
    {
        return ASC.equals(order) || "desc".equals(order) ? order : ASC;
    }
}
