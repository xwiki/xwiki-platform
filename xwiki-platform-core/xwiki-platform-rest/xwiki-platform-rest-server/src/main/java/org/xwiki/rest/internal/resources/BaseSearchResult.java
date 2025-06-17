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
import java.util.EnumSet;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.core.UriBuilderException;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.search.SearchSource;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.resources.spaces.SpaceResource;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.internal.store.hibernate.query.HqlQueryUtils;

import static org.xwiki.rest.internal.resources.BaseSearchResult.SearchScope.CONTENT;
import static org.xwiki.rest.internal.resources.BaseSearchResult.SearchScope.NAME;
import static org.xwiki.rest.internal.resources.BaseSearchResult.SearchScope.TITLE;

/**
 * @version $Id$
 */
public class BaseSearchResult extends XWikiResource
{
    protected static final String SEARCH_TEMPLATE_INFO =
        "q={keywords}(&scope={content|name|title|spaces|objects})*(&number={number})(&start={start})(&orderField={fieldname}(&order={asc|desc}))(&prettyNames={false|true})";

    protected static final String QUERY_TEMPLATE_INFO =
        "q={query}(&type={type})(&number={number})(&start={start})(&orderField={fieldname}(&order={asc|desc}))(&distinct=1)(&prettyNames={false|true})(&wikis={wikis})(&className={classname})";

    protected enum SearchScope
    {
        SPACES,
        NAME,
        CONTENT,
        TITLE,
        OBJECTS
    }

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    @Named("hidden/space")
    private Provider<QueryFilter> hiddenSpaceFilterProvider;

    @Inject
    private LocalizationContext localizationContext;

    @Inject
    private ModelFactory modelFactory;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    @Named("secure")
    private QueryManager secureQueryManager;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentDocumentReferenceResolver;

    /**
     * Search for keyword in the given scopes. See {@link SearchScope} for more information.
     * 
     * @param number number of results to be returned.
     * @param start 0-based start offset.
     * @param withPrettyNames true if the users are displayed with their full name
     * @param isLocaleAware If true, fetches the documents with the best language (the one from the user
     * or the default one from the document).
     */
    protected List<SearchResult> search(List<SearchScope> searchScopes, String keywords, String wikiName, String space,
        boolean hasProgrammingRights, int number, int start, boolean distinct, String orderField, String order,
        Boolean withPrettyNames, Boolean isLocaleAware) throws IllegalArgumentException, UriBuilderException,
            QueryException, XWikiException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        /* This try is just needed for executing the finally clause. */
        try {
            if (wikiName != null) {
                Utils.getXWikiContext(componentManager).setWikiId(wikiName);
            }

            List<SearchResult> result =
                new ArrayList<>(searchPagesSolr(searchScopes, keywords, wikiName, space, number, start,
                    orderField, order, withPrettyNames, isLocaleAware));

            if (searchScopes.contains(SearchScope.SPACES)) {
                result.addAll(searchSpaces(keywords, wikiName, number, start));
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

    private List<SearchResult> searchPagesSolr(List<SearchScope> searchScopes, String keywords, String wikiName,
        String space, int number, int start, String orderField, String order, Boolean withPrettyNames,
        Boolean isLocaleAware) throws QueryException, XWikiException
    {
        if (StringUtils.isBlank(keywords)) {
            return List.of();
        }

        List<String> filterQueries = new ArrayList<>();
        filterQueries.add("type:DOCUMENT");
        filterQueries.add("hidden:false");
        if (StringUtils.isNotBlank(wikiName)) {
            filterQueries.add("wiki:" + this.solrUtils.toCompleteFilterQueryString(wikiName));
        }
        if (StringUtils.isNotBlank(space)) {
            filterQueries.add("space_exact:" + this.solrUtils.toCompleteFilterQueryString(space));
        }

        String escapedKeyWords = this.solrUtils.toFilterQueryString(keywords);
        Set<SearchScope> supportedScopes = EnumSet.of(TITLE, NAME, CONTENT);
        String queryString = searchScopes.stream()
            .filter(supportedScopes::contains)
            .map(scope -> switch (scope) {
                // Consider matches in the title as way more important.
                case TITLE -> "(title:" + escapedKeyWords + "*)^4";
                // Prefer matching in the name over the spaces, but only if the name is not "WebHome".
                case NAME -> "spaces:" + escapedKeyWords + "* OR (name:" + escapedKeyWords + "* -name_exact:WebHome)^2";
                case CONTENT -> "doccontent:" + escapedKeyWords;
                default -> throw new IllegalStateException("Unexpected value: " + scope);
            })
            .collect(Collectors.joining(" OR "));

        if (queryString.isEmpty()) {
            // No supported scope.
            return List.of();
        }

        if (Boolean.TRUE.equals(isLocaleAware)) {
            // TODO: this might not be the right way to filter by language.
            Locale currentLocale = this.localizationContext.getCurrentLocale();
            filterQueries.add("locale:(\"\" OR %s OR %s)".formatted(currentLocale, currentLocale.getLanguage()));
        }

        Query query = this.queryManager.createQuery(queryString, "solr");
        query.setLimit(number * 2);
        query.setOffset(start);
        query.bindValue("fq", filterQueries);
        query.bindValue("fl", String.join(",", FieldUtils.WIKI, FieldUtils.SPACES, FieldUtils.NAME,
            FieldUtils.DOCUMENT_LOCALE, FieldUtils.TYPE));
        List<Object> results = query.execute();
        List<DocumentReference> documentReferences = ((QueryResponse) results.get(0)).getResults().stream()
            .map(this.solrDocumentDocumentReferenceResolver::resolve)
            .toList();

        return getPagesSearchResults(documentReferences, wikiName, withPrettyNames, number, isLocaleAware);
    }

    /**
     * Search for keyword in the given scopes. Limit the search only to Pages. Search for keyword
     * 
     * @param keywords the string that will be used in a "like" XWQL clause.
     * @param number number of results to be returned.
     * @param start 0-based start offset.
     * @param orderField the field to be used to order the results.
     * @param order "asc" or "desc"
     * @param isLocaleAware If true, fetches the documents with the best language (the one from the user
     * or the default one from the document).
     * @return the results.
     */
    protected List<SearchResult> searchPages(List<SearchScope> searchScopes, String keywords, String wikiName,
        String space, boolean hasProgrammingRights, int number, int start, String orderField, String order,
        Boolean withPrettyNames, Boolean isLocaleAware) throws QueryException, IllegalArgumentException,
            UriBuilderException, XWikiException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        /* This try is just needed for executing the finally clause. */
        try {
            if (keywords == null) {
                return new ArrayList<>();
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

            String addSpace = "";
            if (searchScopes.contains(NAME)) {
                // Join the space to get the last space name.
                addSpace = "left join XWikiSpace as space on doc.space = space.reference";
            }

            if (space != null) {
                f.format("select distinct doc.fullName, doc.space, doc.name, doc.language");
                f.format(addColumn);
                f.format(" from XWikiDocument as doc %s where doc.space = :space and ( ", addSpace);
            } else {
                f.format("select distinct doc.fullName, doc.space, doc.name, doc.language");
                f.format(addColumn);
                f.format(" from XWikiDocument as doc %s where ( ", addSpace);
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
                        String matchTerminalPage = "doc.name <> :defaultDocName and upper(doc.name) like :keywords";
                        String matchNestedPage = "doc.name = :defaultDocName and upper(space.name) like :keywords";
                        f.format("((%s) or (%s)) ", matchTerminalPage, matchNestedPage);
                        acceptedScopes++;
                        break;
                    case TITLE:
                        f.format("(upper(doc.title) like :keywords");
                        if (isLocaleAware) {
                            f.format(" and (");
                            // In Oracle database, an empty language is stored as null.
                            String emptyLanguageCondition = "(doc.language = '' or doc.language is null)";
                            f.format("(doc.language = :locale or (%s and doc.defaultLanguage = :locale)) ",
                                emptyLanguageCondition);
                            f.format("or (doc.language = :language or (%s and doc.defaultLanguage = :language)) ",
                                emptyLanguageCondition);
                            f.format(("or (%s and not exists("
                                    + " from XWikiDocument as doc2"
                                    + " where doc2.fullName = doc.fullName"
                                    + " and (doc2.language = :locale or doc2.language = :language)))"
                                    + ")"), emptyLanguageCondition);
                        }
                        f.format(") ");
                        acceptedScopes++;
                        break;
                }

                if (i != searchScopes.size() - 1) {
                    f.format(" or ");
                }
            }

            /* If we don't find any scope related to pages then return empty results */
            if (acceptedScopes == 0) {
                return new ArrayList<>();
            }

            /* Build the order clause. */
            String orderClause;
            if (StringUtils.isBlank(orderField)) {
                orderClause = "doc.fullName asc";
            } else {
                orderClause = String.format("doc.%s %s", orderField, HqlQueryUtils.getValidQueryOrder(order, "asc"));
            }

            // Add ordering
            f.format(") order by %s", orderClause);
            String queryString = f.toString();

            Query query = this.secureQueryManager.createQuery(queryString, Query.HQL)
                    .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase()))
                    .addFilter(Utils.getHiddenQueryFilter(this.componentManager)).setOffset(start)
                    // Worst case scenario when making the locale aware query:
                    // e.g.: Search matches a document translated in fr_CA and fr
                    .setLimit(number * 2);

            if (space != null) {
                query.bindValue("space", space);
            }

            if (searchScopes.contains(NAME)) {
                query.bindValue("defaultDocName",
                    this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName());
            }

            // Search only pages translated in the user locale (e.g. fr_CA)
            if (isLocaleAware && searchScopes.contains(TITLE)) {
                Locale userLocale = localizationContext.getCurrentLocale();
                query.bindValue("locale", userLocale.toString());
                query.bindValue("language", userLocale.getLanguage());
            }

            List<DocumentReference> documentReferences = query.execute().stream()
                .map(object -> {
                    Object[] fields = (Object[]) object;

                    String spaceId = (String) fields[1];
                    List<String> spaces = Utils.getSpacesFromSpaceId(spaceId);
                    String pageName = (String) fields[2];
                    String language = (String) fields[3];

                    return new DocumentReference(wikiName, spaces, pageName, LocaleUtils.toLocale(language));
                })
                .toList();

            return getPagesSearchResults(documentReferences, wikiName, withPrettyNames, number, isLocaleAware);
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }
    }

    /**
     * Process the results of the query made by {@link #searchPages}
     *
     * @param queryResult results of the {@link #searchPages} query
     * @param wikiName the wiki name
     * @param withPrettyNames render the author name
     * @param limit the maximum number of results
     * @param withUniquePages add pages only once
     * @return the list of {@link SearchResult}
     * @throws XWikiException
     */
    protected List<SearchResult> getPagesSearchResults(List<DocumentReference> queryResult, String wikiName,
        Boolean withPrettyNames, int limit, Boolean withUniquePages) throws XWikiException
    {
        List<SearchResult> result = new ArrayList<>();
        Set<DocumentReference> seenPages = new HashSet<>();
        XWiki xwikiApi = Utils.getXWikiApi(this.componentManager);

        for (DocumentReference ref : queryResult) {
            // Stop if there's a limit specified and we reach it.
            if (limit > 0 && result.size() >= limit) {
                break;
            }

            if (Boolean.TRUE.equals(withUniquePages) && seenPages.contains(ref)) {
                continue;
            }

            seenPages.add(ref);

            if (this.authorizationManager.hasAccess(Right.VIEW, ref)) {
                Document doc = xwikiApi.getDocument(ref).getTranslatedDocument();
                String title = doc.getDisplayTitle();
                SearchResult searchResult = this.objectFactory.createSearchResult();
                searchResult.setType("page");
                searchResult.setId(doc.getPrefixedFullName());
                searchResult.setPageFullName(doc.getFullName());
                searchResult.setTitle(title);
                searchResult.setWiki(wikiName);
                searchResult.setSpace(doc.getSpace());
                String pageName = doc.getDocumentReference().getName();
                searchResult.setPageName(pageName);
                searchResult.setVersion(doc.getVersion());
                searchResult.setAuthor(doc.getAuthor());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(doc.getDate());
                searchResult.setModified(calendar);

                if (Boolean.TRUE.equals(withPrettyNames)) {
                    searchResult.setAuthorName(Utils.getAuthorName(doc.getAuthorReference(), this.componentManager));
                }

                String pageUri;
                if (!doc.isTranslation()) {
                    pageUri = Utils.createURI(this.uriInfo.getBaseUri(), PageResource.class, wikiName,
                        Utils.getSpacesURLElements(ref), pageName).toString();
                } else {
                    String language = doc.getRealLocale().toString();
                    searchResult.setLanguage(language);
                    pageUri = Utils.createURI(this.uriInfo.getBaseUri(), PageTranslationResource.class, wikiName,
                        Utils.getSpacesURLElements(ref), pageName, language).toString();
                }

                Link pageLink = new Link();
                pageLink.setHref(pageUri);
                pageLink.setRel(Relations.PAGE);
                searchResult.getLinks().add(pageLink);

                searchResult
                    .setHierarchy(this.modelFactory.toRestHierarchy(doc.getDocumentReference(), withPrettyNames));

                result.add(searchResult);
            }
        }

        return result;
    }

    /**
     * Search for keyword in the given scopes. Limit the search only to spaces.
     * 
     * @param keywords the string that will be used in a "like" XWQL clause
     * @param number number of results to be returned
     * @param start 0-based start offset
     * @return the results.
     */
    protected List<SearchResult> searchSpaces(String keywords, String wikiName, int number, int start)
        throws QueryException, IllegalArgumentException, UriBuilderException, XWikiException
    {
        List<SearchResult> result = new ArrayList<SearchResult>();

        if (StringUtils.isEmpty(keywords)) {
            return result;
        }
        String escapedKeywords = keywords.replaceAll("([%_!])", "!$1");

        String query = "select space.reference from XWikiSpace as space"
            + " where lower(space.name) like lower(:keywords) escape '!'"
            + " or lower(space.reference) like lower(:prefix) escape '!'"
            + " order by lower(space.reference), space.reference";

        List<Object> queryResult = this.secureQueryManager.createQuery(query, Query.HQL)
            .bindValue("keywords", String.format("%%%s%%", escapedKeywords))
            .bindValue("prefix", String.format("%s%%", escapedKeywords))
            .setWiki(wikiName).setLimit(number).setOffset(start)
            .addFilter(this.hiddenSpaceFilterProvider.get()).execute();

        XWiki xwikiApi = Utils.getXWikiApi(componentManager);
        for (Object object : queryResult) {
            String spaceId = (String) object;
            List<String> spaces = Utils.getSpacesFromSpaceId(spaceId);
            SpaceReference spaceReference = new SpaceReference(wikiName, spaces);

            if (this.authorizationManager.hasAccess(Right.VIEW, spaceReference)) {
                Document spaceDoc = xwikiApi.getDocument(spaceReference);

                SearchResult searchResult = objectFactory.createSearchResult();
                searchResult.setType("space");
                searchResult.setId(spaceId);
                searchResult.setWiki(wikiName);
                searchResult.setSpace(spaceId);
                searchResult.setTitle(spaceDoc != null ? spaceDoc.getPlainTitle() : spaceReference.getName());

                List<String> restSpacesValue = Utils.getSpacesURLElements(spaces);

                // Add a link to the space information.
                Link spaceLink = new Link();
                spaceLink.setRel(Relations.SPACE);
                spaceLink.setHref(
                    Utils.createURI(uriInfo.getBaseUri(), SpaceResource.class, wikiName, restSpacesValue).toString());
                searchResult.getLinks().add(spaceLink);

                // Add a link to the home page if it exists and it is viewable.
                if (spaceDoc != null && !spaceDoc.isNew()) {
                    Link pageLink = new Link();
                    pageLink.setHref(Utils.createURI(uriInfo.getBaseUri(), PageResource.class, wikiName,
                        restSpacesValue, spaceDoc.getDocumentReference().getName()).toString());
                    pageLink.setRel(Relations.HOME);
                    searchResult.getLinks().add(pageLink);
                }

                result.add(searchResult);
            }
        }

        return result;
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
                    this.secureQueryManager.createQuery(query, Query.XWQL)
                        .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase()))
                        .bindValue("space", space).setLimit(number).execute();
            } else {
                queryResult =
                    this.secureQueryManager.createQuery(query, Query.XWQL)
                        .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase())).setLimit(number)
                        .execute();
            }

            /* Build the result. */
            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;

                String spaceId = (String) fields[1];
                List<String> spaces = Utils.getSpacesFromSpaceId(spaceId);
                String pageName = (String) fields[2];
                String className = (String) fields[3];
                int objectNumber = (Integer) fields[4];

                String id = Utils.getObjectId(wikiName, spaces, pageName, className, objectNumber);

                String pageId = Utils.getPageId(wikiName, spaces, pageName);
                String pageFullName = Utils.getPageFullName(wikiName, spaces, pageName);

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
                    searchResult.setSpace(spaceId);
                    searchResult.setPageName(pageName);
                    searchResult.setVersion(doc.getVersion());
                    searchResult.setClassName(className);
                    searchResult.setObjectNumber(objectNumber);
                    searchResult.setAuthor(doc.getAuthor());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(doc.getDate());
                    searchResult.setModified(calendar);

                    if (withPrettyNames) {
                        searchResult.setAuthorName(Utils.getAuthorName(doc.getAuthorReference(), componentManager));
                    }

                    List<String> restSpacesValue = Utils.getSpacesURLElements(spaces);

                    String pageUri =
                        Utils.createURI(uriInfo.getBaseUri(), PageResource.class, wikiName, restSpacesValue, pageName)
                            .toString();
                    Link pageLink = new Link();
                    pageLink.setHref(pageUri);
                    pageLink.setRel(Relations.PAGE);
                    searchResult.getLinks().add(pageLink);

                    String objectUri = Utils.createURI(uriInfo.getBaseUri(), ObjectResource.class, wikiName,
                        restSpacesValue, pageName, className, objectNumber).toString();
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
            if (wikiName != null) {
                Utils.getXWikiContext(componentManager).setWikiId(wikiName);
            }

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
            searchScopes.add(CONTENT);
        }

        return searchScopes;
    }
}
