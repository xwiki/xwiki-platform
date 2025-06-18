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

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.ws.rs.core.UriBuilderException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.resources.spaces.SpaceResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.internal.store.hibernate.query.HqlQueryUtils;

import static org.xwiki.rest.internal.resources.KeywordSearchScope.NAME;
import static org.xwiki.rest.internal.resources.KeywordSearchScope.TITLE;

/**
 * A {@link KeywordSearchSource} implementation that searches in the database.
 *
 * @version $Id$
 * @since 17.5.0RC1
 */
// This is old code from BaseSearchResult copied to a new class. While some checkstyle violations were fixed, most of
// the following aren't that simple to fix.
@SuppressWarnings({ "checkstyle:ExecutableStatementCount", "checkstyle:JavaNCSS", "checkstyle:NPathComplexity",
    "checkstyle:CyclomaticComplexity", "checkstyle:ClassFanOutComplexity", "checkstyle:MultipleStringLiterals" })
@Component
@Singleton
@Named("database")
public class DatabaseKeywordSearchSource implements KeywordSearchSource
{
    @Inject
    protected ContextualAuthorizationManager authorizationManager;

    @Inject
    @Named("hidden/space")
    private Provider<QueryFilter> hiddenSpaceFilterProvider;

    @Inject
    @Named("hidden/document")
    private Provider<QueryFilter> hiddenDocumentFilterProvider;

    @Inject
    private LocalizationContext localizationContext;

    @Inject
    private ModelFactory modelFactory;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("secure")
    private QueryManager secureQueryManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public List<SearchResult> search(String keywords, KeywordSearchOptions options, URI baseURI)
        throws XWikiRestException
    {
        XWikiContext context = this.contextProvider.get();
        String database = context.getWikiId();

        try {
            if (options.wikiName() != null) {
                context.setWikiId(options.wikiName());
            }

            boolean hasProgrammingRights = this.authorizationManager.hasAccess(Right.PROGRAM);

            List<SearchResult> result = new ArrayList<>(searchPages(options, keywords, baseURI));

            if (options.searchScopes().contains(KeywordSearchScope.SPACES)) {
                result.addAll(searchSpaces(keywords, options.wikiName(), options.number(), options.start(), baseURI));
            }

            if (options.searchScopes().contains(KeywordSearchScope.OBJECTS)) {
                result.addAll(searchObjects(keywords, options, hasProgrammingRights, baseURI));
            }

            return result;
        } catch (Exception e) {
            throw new XWikiRestException("Database keyword search failed.", e);
        } finally {
            context.setWikiId(database);
        }
    }

    /**
     * Search for keyword in the given scopes. Limit the search only to Pages. Search for keyword
     *
     * @param options the options for the keyword search
     * @param keywords the string that will be used in a "like" XWQL clause.
     * @param baseURI the base URI to use for building links
     * @return the results.
     */
    private List<SearchResult> searchPages(KeywordSearchOptions options, String keywords, URI baseURI)
        throws QueryException, IllegalArgumentException,
        UriBuilderException, XWikiException
    {
        String database = this.contextProvider.get().getWikiId();

        /* This try is just needed for executing the finally clause. */
        try (Formatter f = new Formatter()) {
            if (keywords == null) {
                return new ArrayList<>();
            }

            QueryManager finalQueryManager = this.queryManager;

            String orderField = options.orderField();
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
            if (options.searchScopes().contains(NAME)) {
                // Join the space to get the last space name.
                addSpace = "left join XWikiSpace as space on doc.space = space.reference";
            }

            if (options.space() != null) {
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
            for (int i = 0; i < options.searchScopes().size(); i++) {
                KeywordSearchScope scope = options.searchScopes().get(i);

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
                        if (Boolean.TRUE.equals(options.isLocaleAware())) {
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
                    default:
                        // Ignore other scopes.
                }

                if (i != options.searchScopes().size() - 1) {
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
                orderClause =
                    String.format("doc.%s %s", orderField, HqlQueryUtils.getValidQueryOrder(options.order(), "asc"));

                if (!StringUtils.isAlphanumeric(orderField)) {
                    finalQueryManager = this.secureQueryManager;
                }
            }

            // Add ordering
            f.format(") order by %s", orderClause);
            String queryString = f.toString();

            Query query = finalQueryManager.createQuery(queryString, Query.HQL)
                .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase()))
                .addFilter(this.hiddenDocumentFilterProvider.get()).setOffset(options.start())
                // Worst case scenario when making the locale aware query:
                // e.g.: Search matches a document translated in fr_CA and fr
                .setLimit(options.number() * 2);

            if (options.space() != null) {
                query.bindValue("space", options.space());
            }

            if (options.searchScopes().contains(NAME)) {
                query.bindValue("defaultDocName",
                    this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName());
            }

            // Search only pages translated in the user locale (e.g. fr_CA)
            if (options.isLocaleAware() && options.searchScopes().contains(TITLE)) {
                Locale userLocale = this.localizationContext.getCurrentLocale();
                query.bindValue("locale", userLocale.toString());
                query.bindValue("language", userLocale.getLanguage());
            }

            return getPagesSearchResults(query.execute(), options.wikiName(), options.withPrettyNames(),
                options.number(), options.isLocaleAware(), baseURI);
        } finally {
            this.contextProvider.get().setWikiId(database);
        }
    }

    /**
     * Process the results of the query made by {@link #searchPages}.
     *
     * @param queryResult results of the {@link #searchPages} query
     * @param wikiName the wiki name
     * @param withPrettyNames render the author name
     * @param limit the maximum number of results
     * @param withUniquePages add pages only once
     * @param baseURI the base URI for URLs in the search results
     * @return the list of {@link SearchResult}
     * @throws XWikiException
     */
    private List<SearchResult> getPagesSearchResults(List<Object> queryResult, String wikiName,
        Boolean withPrettyNames, int limit, Boolean withUniquePages, URI baseURI) throws XWikiException
    {
        List<SearchResult> result = new ArrayList<>();
        Set<String> seenPages = new HashSet<>();
        XWikiContext context = this.contextProvider.get();
        XWiki xwikiApi = new XWiki(context.getWiki(), context);
        ObjectFactory objectFactory = new ObjectFactory();

        for (Object object : queryResult) {
            // Stop if there's a limit specified and we reach it.
            if (limit > 0 && result.size() >= limit) {
                break;
            }

            Object[] fields = (Object[]) object;

            String spaceId = (String) fields[1];
            List<String> spaces = Utils.getSpacesFromSpaceId(spaceId);
            String pageName = (String) fields[2];
            String language = (String) fields[3];

            String pageId = Utils.getPageId(wikiName, spaces, pageName);
            String pageFullName = Utils.getPageFullName(wikiName, spaces, pageName);

            if (withUniquePages && seenPages.contains(pageFullName)) {
                continue;
            }
            seenPages.add(pageFullName);

            /* Check if the user has the right to see the found document */
            if (xwikiApi.hasAccessLevel("view", pageId)) {
                Document doc = xwikiApi.getDocument(pageFullName).getTranslatedDocument();
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

                if (Boolean.TRUE.equals(withPrettyNames)) {
                    searchResult.setAuthorName(context.getWiki().getPlainUserName(doc.getAuthorReference(), context));
                }

                String pageUri;
                if (StringUtils.isBlank(language)) {
                    pageUri = Utils.createURI(baseURI, PageResource.class, wikiName,
                        Utils.getSpacesURLElements(spaces), pageName).toString();
                } else {
                    searchResult.setLanguage(language);
                    pageUri = Utils.createURI(baseURI, PageTranslationResource.class, wikiName,
                        Utils.getSpacesURLElements(spaces), pageName, language).toString();
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
    private List<SearchResult> searchSpaces(String keywords, String wikiName, int number, int start, URI baseURI)
        throws QueryException, IllegalArgumentException, UriBuilderException, XWikiException
    {
        List<SearchResult> result = new ArrayList<>();

        if (StringUtils.isEmpty(keywords)) {
            return result;
        }
        String escapedKeywords = keywords.replaceAll("([%_!])", "!$1");

        String query = "select space.reference from XWikiSpace as space"
            + " where lower(space.name) like lower(:keywords) escape '!'"
            + " or lower(space.reference) like lower(:prefix) escape '!'"
            + " order by lower(space.reference), space.reference";

        List<Object> queryResult = this.queryManager.createQuery(query, Query.HQL)
            .bindValue("keywords", String.format("%%%s%%", escapedKeywords))
            .bindValue("prefix", String.format("%s%%", escapedKeywords))
            .setWiki(wikiName).setLimit(number).setOffset(start)
            .addFilter(this.hiddenSpaceFilterProvider.get()).execute();

        XWikiContext context = this.contextProvider.get();
        XWiki xwikiApi = new XWiki(context.getWiki(), context);
        ObjectFactory objectFactory = new ObjectFactory();
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
                    Utils.createURI(baseURI, SpaceResource.class, wikiName, restSpacesValue).toString());
                searchResult.getLinks().add(spaceLink);

                // Add a link to the home page if it exists and it is viewable.
                if (spaceDoc != null && !spaceDoc.isNew()) {
                    Link pageLink = new Link();
                    pageLink.setHref(Utils.createURI(baseURI, PageResource.class, wikiName,
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
     * @return the results
     */
    private List<SearchResult> searchObjects(String keywords, KeywordSearchOptions options,
        boolean hasProgrammingRights, URI baseURI)
        throws QueryException, IllegalArgumentException, UriBuilderException, XWikiException
    {
        XWikiContext context = this.contextProvider.get();

        String database = context.getWikiId();

        try (Formatter f = new Formatter()) {
            if (keywords == null) {
                return new ArrayList<SearchResult>();
            }

            QueryManager finalQueryManager = this.queryManager;

            String orderField = options.orderField();
            /*
             * If the order field is already one of the field hard coded in the base query, then do not add it to the
             * select clause.
             */
            String addColumn =
                (orderField.isEmpty() || orderField.equals("fullName") || orderField.equals("name") || orderField
                    .equals("space")) ? "" : ", doc." + orderField;

            if (options.space() != null) {
                f.format("select distinct doc.fullName, doc.space, doc.name, obj.className, obj.number");
                f.format(addColumn);
                f.format(" from XWikiDocument as doc, BaseObject as obj, StringProperty as sp, "
                    + "LargeStringProperty as lsp where doc.space = :space and obj.name=doc.fullName "
                    + "and sp.id.id = obj.id and lsp.id.id = obj.id "
                    + "and (upper(sp.value) like :keywords or upper(lsp.value) like :keywords) ");
            } else {
                f.format("select distinct doc.fullName, doc.space, doc.name, obj.className, obj.number");
                f.format(addColumn);
                f.format(" from XWikiDocument as doc, BaseObject as obj, StringProperty as sp, "
                    + "LargeStringProperty as lsp where obj.name=doc.fullName and sp.id.id = obj.id "
                    + "and lsp.id.id = obj.id and (upper(sp.value) like :keywords "
                    + "or upper(lsp.value) like :keywords) ");
            }

            /* Build the order clause. */
            String orderClause;
            if (StringUtils.isBlank(orderField)) {
                orderClause = "doc.fullName asc";
            } else {
                orderClause =
                    String.format("doc.%s %s", orderField, HqlQueryUtils.getValidQueryOrder(options.order(), "asc"));

                if (!StringUtils.isAlphanumeric(orderField)) {
                    finalQueryManager = this.secureQueryManager;
                }
            }

            /* Add some filters if the user doesn't have programming rights. */
            if (hasProgrammingRights) {
                f.format(" order by %s", orderClause);
            } else {
                f.format(" and doc.space<>'XWiki' and doc.space<>'Admin' and doc.space<>'Panels' "
                        + "and doc.name<>'WebPreferences' order by %s",
                    orderClause);
            }

            String query = f.toString();

            List<Object> queryResult = null;

            /* This is needed because if the :space placeholder is not in the query, setting it would cause an
            exception */
            if (options.space() != null) {
                queryResult =
                    finalQueryManager.createQuery(query, Query.XWQL)
                        .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase()))
                        .bindValue("space", options.space()).setLimit(options.number()).execute();
            } else {
                queryResult =
                    finalQueryManager.createQuery(query, Query.XWQL)
                        .bindValue("keywords", String.format("%%%s%%", keywords.toUpperCase()))
                        .setLimit(options.number())
                        .execute();
            }

            /* Build the result. */
            return searchObjects(options, baseURI, queryResult, context);
        } finally {
            this.contextProvider.get().setWikiId(database);
        }
    }

    private List<SearchResult> searchObjects(KeywordSearchOptions options, URI baseURI, List<Object> queryResult,
        XWikiContext context) throws UriBuilderException, XWikiException
    {
        List<SearchResult> result = new ArrayList<>();

        XWiki xwikiApi = new XWiki(context.getWiki(), context);

        /* Build the result. */
        ObjectFactory objectFactory = new ObjectFactory();
        for (Object object : queryResult) {
            Object[] fields = (Object[]) object;

            String spaceId = (String) fields[1];
            List<String> spaces = Utils.getSpacesFromSpaceId(spaceId);
            String pageName = (String) fields[2];
            String className = (String) fields[3];
            int objectNumber = (Integer) fields[4];

            String id = Utils.getObjectId(options.wikiName(), spaces, pageName, className, objectNumber);

            String pageId = Utils.getPageId(options.wikiName(), spaces, pageName);
            String pageFullName = Utils.getPageFullName(options.wikiName(), spaces, pageName);

            /*
             * Check if the user has the right to see the found document. We also prevent guest users to access object
             * data in order to avoid leaking important information such as emails to crawlers.
             */
            if (xwikiApi.hasAccessLevel("view", pageId) && context.getUserReference() != null) {
                Document doc = xwikiApi.getDocument(pageFullName);
                String title = doc.getDisplayTitle();
                SearchResult searchResult = objectFactory.createSearchResult();
                searchResult.setType("object");
                searchResult.setId(id);
                searchResult.setPageFullName(pageFullName);
                searchResult.setTitle(title);
                searchResult.setWiki(options.wikiName());
                searchResult.setSpace(spaceId);
                searchResult.setPageName(pageName);
                searchResult.setVersion(doc.getVersion());
                searchResult.setClassName(className);
                searchResult.setObjectNumber(objectNumber);
                searchResult.setAuthor(doc.getAuthor());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(doc.getDate());
                searchResult.setModified(calendar);

                if (options.withPrettyNames()) {
                    searchResult.setAuthorName(context.getWiki().getPlainUserName(doc.getAuthorReference(), context));
                }

                List<String> restSpacesValue = Utils.getSpacesURLElements(spaces);

                String pageUri = Utils
                    .createURI(baseURI, PageResource.class, options.wikiName(), restSpacesValue, pageName).toString();
                Link pageLink = new Link();
                pageLink.setHref(pageUri);
                pageLink.setRel(Relations.PAGE);
                searchResult.getLinks().add(pageLink);

                String objectUri = Utils.createURI(baseURI, ObjectResource.class, options.wikiName(), restSpacesValue,
                    pageName, className, objectNumber).toString();
                Link objectLink = new Link();
                objectLink.setHref(objectUri);
                objectLink.setRel(Relations.OBJECT);
                searchResult.getLinks().add(objectLink);

                result.add(searchResult);
            }
        }

        return result;
    }
}
