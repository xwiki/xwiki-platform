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
package com.xpn.xwiki.internal.export;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQueryParameter;
import org.xwiki.tree.EntityTreeFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Resolves the list of documents selected by the current request.
 * 
 * @version $Id$
 * @since 11.10
 */
@Component(roles = DocumentSelectionResolver.class)
@Singleton
public class DocumentSelectionResolver
{
    /**
     * Used to separate page arguments in {@code excludes} request parameter.
     */
    private static final String PAGE_SEPARATOR = "&";

    /**
     * The symbol used to match any chars in a document reference.
     */
    private static final String ANY_CHARS = "%";

    /**
     * The request parameter used to specify the list of pages to export.
     */
    private static final String REQUEST_PARAMETER_PAGES = "pages";

    /**
     * The request parameter used to specify the entity tree filter to apply.
     */
    private static final String REQUEST_PARAMETER_FILTER = "filter";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("document")
    private QueryFilter documentQueryFilter;

    /**
     * We need to use a provider instead of a direct injection because the default implementation uses
     * {@link ComponentInstantiationStrategy#PER_LOOKUP} (because it caches the user preference regarding hidden pages).
     * Basically we need a new filter instance each time the selection is resolved because the current user is usually
     * different.
     */
    @Inject
    @Named("hidden/document")
    private Provider<QueryFilter> hiddenDocumentQueryFilterProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    /**
     * @return {@code true} if the current request specifies a selection
     */
    public boolean isSelectionSpecified()
    {
        Map<String, String[]> parameters = this.xcontextProvider.get().getRequest().getParameterMap();
        return parameters.containsKey(REQUEST_PARAMETER_PAGES)
            || !parameters.getOrDefault(REQUEST_PARAMETER_FILTER, new String[] {""})[0].isEmpty();
    }

    /**
     * @return the list of documents selected by the current request
     */
    public Collection<DocumentReference> getSelectedDocuments()
    {
        return getSelectedDocuments(false);
    }

    /**
     * @param filterHiddenDocuments {@code true} to apply the hidden document filter, based on the user preferences, to
     *            the documents that are not selected explicitly (by an exact match) but implicitly, by a partial match,
     *            {@code false} otherwise
     * @return the list of documents selected by the current request
     */
    public Collection<DocumentReference> getSelectedDocuments(boolean filterHiddenDocuments)
    {
        Map<DocumentReference, Collection<DocumentReference>> selection = getSelectionFromRequest();
        EntityTreeFilter filter = getFilterFromRequest();
        if (filter != null) {
            extendSelection(selection, filter);
        }

        Set<DocumentReference> selectedDocuments = new LinkedHashSet<>();

        // Extract the exact match first.
        selectedDocuments.addAll(selection.keySet().stream().filter(this::isExactMatch).collect(Collectors.toSet()));
        selection.keySet().removeAll(selectedDocuments);

        // Add the partial match.
        selectedDocuments.addAll(getSelectedDocuments(selection, filterHiddenDocuments));

        return selectedDocuments;
    }

    private boolean isExactMatch(DocumentReference documentReference)
    {
        return documentReference != null
            && !this.localEntityReferenceSerializer.serialize(documentReference).contains(ANY_CHARS);
    }

    private Collection<DocumentReference> getSelectedDocuments(
        Map<DocumentReference, Collection<DocumentReference>> selection, boolean filterHiddenDocuments)
    {
        // Reuse the same hidden document query filter for all wikis because it's for the same user.
        Optional<QueryFilter> hiddenDocumentQueryFilter =
            filterHiddenDocuments ? Optional.of(this.hiddenDocumentQueryFilterProvider.get()) : Optional.empty();
        Map<String, Object[]> queriesByWiki = buildQueriesByWiki(selection);
        return queriesByWiki.entrySet().stream().map(entry -> executeQuery(entry, hiddenDocumentQueryFilter))
            .flatMap(Set::stream).collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object[]> buildQueriesByWiki(Map<DocumentReference, Collection<DocumentReference>> selection)
    {
        Map<String, Object[]> queriesByWiki = new LinkedHashMap<>();

        for (Map.Entry<DocumentReference, Collection<DocumentReference>> entry : selection.entrySet()) {
            String wikiName;
            if (entry.getKey() == null) {
                wikiName = this.xcontextProvider.get().getWikiId();
            } else {
                wikiName = entry.getKey().getWikiReference().getName();
            }

            Object[] query = queriesByWiki.get(wikiName);
            if (query == null) {
                query = new Object[] {new StringBuilder(), new ArrayList<>()};
                queriesByWiki.put(wikiName, query);
            }

            StringBuilder statement = (StringBuilder) query[0];
            List<Object> parameters = (List<Object>) query[1];

            List<String> constraints = extendQuery(entry.getKey(), entry.getValue(), parameters);
            if (!constraints.isEmpty()) {
                statement.append(statement.length() == 0 ? "where (" : " or (");
                statement.append(StringUtils.join(constraints, " and "));
                statement.append(')');
            }
        }

        return queriesByWiki;
    }

    private List<String> extendQuery(DocumentReference includedDocumentReference,
        Collection<DocumentReference> excludedDocumentReferences, List<Object> parameters)
    {
        List<String> constraints = new LinkedList<>();

        // The included document reference is null if only the export filter is applied.
        if (includedDocumentReference != null) {
            String includedPage = this.localEntityReferenceSerializer.serialize(includedDocumentReference);
            parameters.add(new DefaultQueryParameter(null).like(includedPage));
            // The included document reference must be a partial patch, otherwise it wouldn't get here.
            constraints.add("doc.fullName like ?" + parameters.size());
        }

        // Process the excluded pages associated with the included page.
        Set<DocumentReference> exactMatch =
            excludedDocumentReferences.stream().filter(this::isExactMatch).collect(Collectors.toSet());
        Set<DocumentReference> partialMatch = new LinkedHashSet<>(excludedDocumentReferences);
        partialMatch.removeAll(exactMatch);

        if (!exactMatch.isEmpty()) {
            parameters.add(
                exactMatch.stream().map(this.localEntityReferenceSerializer::serialize).collect(Collectors.toSet()));
            constraints.add("doc.fullName not in (?" + parameters.size() + ")");
        }

        for (DocumentReference excludedDocumentReference : partialMatch) {
            parameters.add(new DefaultQueryParameter(null)
                .like(this.localEntityReferenceSerializer.serialize(excludedDocumentReference)));
            constraints.add("doc.fullName not like ?" + parameters.size());
        }

        return constraints;
    }

    private Set<DocumentReference> executeQuery(Map.Entry<String, Object[]> entry,
        Optional<QueryFilter> hiddenDocumentQueryFilter)
    {
        String wikiName = entry.getKey();
        String statement = entry.getValue()[0].toString();
        @SuppressWarnings("unchecked")
        List<Object> parameters = (List<Object>) entry.getValue()[1];

        try {
            Query query = this.queryManager.createQuery(statement, Query.HQL);
            query.setWiki(wikiName).bindValues(parameters).addFilter(this.documentQueryFilter);
            if (hiddenDocumentQueryFilter.isPresent()) {
                query.addFilter(hiddenDocumentQueryFilter.get());
            }
            return new LinkedHashSet<>(query.execute());
        } catch (QueryException e) {
            this.logger.error("Failed to retrieve the selected documents from wiki [{}].", wikiName, e);
            return Collections.emptySet();
        }
    }

    private Map<DocumentReference, Collection<DocumentReference>> getSelectionFromRequest()
    {
        XWikiRequest request = this.xcontextProvider.get().getRequest();
        String[] pages = request.getParameterValues(REQUEST_PARAMETER_PAGES);
        String[] excludes = request.getParameterValues("excludes");

        Map<DocumentReference, Collection<DocumentReference>> selection = new LinkedHashMap<>();

        if (pages != null) {
            for (int i = 0; i < pages.length; i++) {
                DocumentReference includedPage = this.currentDocumentReferenceResolver.resolve(pages[i]);
                Collection<DocumentReference> excludedPages;

                if (excludes != null && i < excludes.length) {
                    excludedPages = this.decodeExcludes(excludes[i]);
                    // Ignore excluded pages that are not from the same wiki as the included page.
                    excludedPages.removeIf(
                        excludedPage -> !includedPage.getWikiReference().equals(excludedPage.getWikiReference()));
                } else {
                    excludedPages = new LinkedHashSet<>();
                }

                selection.put(includedPage, excludedPages);
            }
        }

        return selection;
    }

    /**
     * Decode an URIEncoded String and split it based on the {@link #PAGE_SEPARATOR}. Returns a list of decoded string.
     *
     * @param encodedString the excludes string to decode
     * @return the list of excluded pages
     */
    private Collection<DocumentReference> decodeExcludes(String encodedString)
    {
        String encoding = this.xcontextProvider.get().getRequest().getCharacterEncoding();
        Set<DocumentReference> excludedPages = new LinkedHashSet<>();
        for (String page : encodedString.split(PAGE_SEPARATOR)) {
            try {
                String decoded = URLDecoder.decode(page, encoding);
                if (!decoded.isEmpty()) {
                    excludedPages.add(this.currentDocumentReferenceResolver.resolve(decoded));
                }
            } catch (UnsupportedEncodingException e) {
                this.logger.warn("Failed to decode excluded page [{}] using [{}]. Root cause is [{}].", page, encoding,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return excludedPages;
    }

    private EntityTreeFilter getFilterFromRequest()
    {
        String filterHint = this.xcontextProvider.get().getRequest().getParameter(REQUEST_PARAMETER_FILTER);
        if (!StringUtils.isEmpty(filterHint)) {
            try {
                return this.contextComponentManagerProvider.get().getInstance(EntityTreeFilter.class, filterHint);
            } catch (ComponentLookupException e) {
                this.logger.warn("Failed to lookup EntityTreeFilter with hint [{}]. Root cause is [{}].", filterHint,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return null;
    }

    private void extendSelection(Map<DocumentReference, Collection<DocumentReference>> selection,
        EntityTreeFilter filter)
    {
        if (selection.isEmpty()) {
            WikiReference currentWikiReference = this.xcontextProvider.get().getWikiReference();
            if (currentWikiReference != null) {
                selection.put(null, getDocumentExclusions(filter.getDescendantExclusions(currentWikiReference)));
            }
        } else {
            for (Map.Entry<DocumentReference, Collection<DocumentReference>> entry : selection.entrySet()) {
                extendExclusions(entry.getKey(), entry.getValue(), filter);
            }
        }
    }

    private void extendExclusions(DocumentReference includedDocumentReference,
        Collection<DocumentReference> excludedDocumentReferences, EntityTreeFilter filter)
    {
        if (ANY_CHARS.equals(includedDocumentReference.getName())) {
            excludedDocumentReferences.addAll(getDocumentExclusions(
                filter.getDescendantExclusions(includedDocumentReference.getLastSpaceReference())));
        }
    }

    private Set<DocumentReference> getDocumentExclusions(Set<EntityReference> exclusions)
    {
        Set<DocumentReference> excludedDocumentReferences = new LinkedHashSet<>();
        exclusions.forEach(excludedEntityReference -> {
            if (excludedEntityReference instanceof DocumentReference) {
                excludedDocumentReferences.add((DocumentReference) excludedEntityReference);
            }
        });
        return excludedDocumentReferences;
    }
}
