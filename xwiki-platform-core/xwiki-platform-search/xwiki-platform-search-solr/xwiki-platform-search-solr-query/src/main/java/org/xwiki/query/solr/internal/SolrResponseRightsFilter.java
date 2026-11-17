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
package org.xwiki.query.solr.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;

import javax.inject.Inject;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Removes from a Solr response the documents that the given users aren't allowed to view.
 * <p>
 * All the documents of the response are checked, wherever they are: besides the main results, this includes for
 * instance grouped, expanded and "more like this" results, child documents and documents nested in field values
 * (e.g., added by a {@code [subquery]} document transformer), as well as documents in sections of the response that
 * aren't known. The documents whose reference can't be resolved are removed. The highlighting and explain information
 * is kept only for the documents that are kept.
 * <p>
 * The component keeps the state of the response that is being filtered, so each thread needs its own instance.
 *
 * @version $Id$
 * @since 18.10.0RC1
 * @since 18.4.9
 * @since 17.10.16
 * @since 16.10.20
 */
@Component(roles = SolrResponseRightsFilter.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class SolrResponseRightsFilter
{
    /**
     * The fields of the documents of the response that the filter relies on: the fields from which the entity
     * reference is resolved, and the id. A document is kept or removed based on their values, so they must hold the
     * values that are indexed for the document.
     */
    public static final Set<String> CHECKED_FIELDS = Set.of(FieldUtils.ID, FieldUtils.TYPE, FieldUtils.WIKI,
        FieldUtils.SPACES, FieldUtils.NAME, FieldUtils.DOCUMENT_LOCALE, FieldUtils.FILENAME, FieldUtils.CLASS,
        FieldUtils.NUMBER, FieldUtils.PROPERTY_NAME);

    private static final String DEBUG = "debug";

    private static final String DOCUMENT_LIST = "doclist";

    /**
     * The sections of the response whose entries are identified by the id of the document they are about.
     */
    private static final List<List<String>> DOCUMENT_ID_SECTIONS = List.of(List.of("highlighting"),
        List.of("moreLikeThis"), List.of("interestingTerms"), List.of(DEBUG, "explain"),
        List.of(DEBUG, "explainOther"));

    @Inject
    private DocumentReferenceResolver<SolrDocument> documentReferenceResolver;

    @Inject
    private AuthorizationManager authorization;

    @Inject
    private Logger logger;

    /**
     * The users whose view right is checked for the response that is being filtered.
     */
    private List<DocumentReference> usersToCheck;

    /**
     * The identifiers of the documents that are kept in the response that is being filtered.
     */
    private final Set<String> keptIds = new HashSet<>();

    /**
     * The document lists of the response that is being filtered that became empty because documents were removed.
     */
    private final Set<SolrDocumentList> emptiedDocumentLists = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Whether documents were removed from the response that is being filtered.
     */
    private boolean removedDocuments;

    /**
     * Removes the documents that the given users aren't allowed to view from the given response, which is modified in
     * place.
     *
     * @param response the Solr response to filter
     * @param usersToCheck the users who must all be allowed to view a document for it to be kept
     */
    public void filter(QueryResponse response, List<DocumentReference> usersToCheck)
    {
        this.usersToCheck = usersToCheck;
        this.keptIds.clear();
        this.emptiedDocumentLists.clear();
        this.removedDocuments = false;

        NamedList<Object> rawResponse = response.getResponse();
        if (rawResponse != null) {
            filterValue(rawResponse);

            // The entries of these sections are identified by the id of the document they are about.
            for (List<String> path : DOCUMENT_ID_SECTIONS) {
                if (rawResponse.findRecursive(path.toArray(new String[0])) instanceof NamedList<?> section) {
                    removeEntries(section, (key, value) -> !this.keptIds.contains(key));
                }
            }

            // The keys are the values of the collapse field, remove the groups that don't have any result left.
            if (rawResponse.get("expanded") instanceof NamedList<?> expanded) {
                removeEntries(expanded,
                    (key, value) -> value instanceof SolrDocumentList documentList && documentList.isEmpty());
            }

            // The group values are field values of the grouped documents, remove the groups whose documents were all
            // removed.
            if (rawResponse.get("grouped") instanceof NamedList<?> grouped) {
                filterGroups(grouped);
            }

            // The sort values are listed in the order of the results, so they don't match the results anymore once
            // results have been removed.
            if (this.removedDocuments) {
                rawResponse.remove("sort_values");
            }

            // Update the information that the query response extracts from the raw response.
            response.setResponse(rawResponse);
        } else {
            filterValue(response.getResults());
        }
    }

    /**
     * Filters the documents found in the given value, recursively.
     *
     * @return the filtered value, possibly the same instance, or {@code null} if a non-null value should be removed
     */
    private Object filterValue(Object value)
    {
        Object result = value;

        if (value instanceof SolrDocumentList documentList) {
            filterDocumentList(documentList);
        } else if (value instanceof SolrDocument document) {
            result = isKept(document) ? document : null;
            this.removedDocuments |= result == null;
        } else if (value instanceof NamedList<?> namedList) {
            filterNamedList(namedList);
        } else if (value instanceof Collection<?> collection) {
            result = filterCollection(collection);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private void filterNamedList(NamedList<?> namedList)
    {
        // The iterator of NamedList doesn't support removal.
        for (int i = namedList.size() - 1; i >= 0; i--) {
            Object value = namedList.getVal(i);
            Object filteredValue = filterValue(value);
            if (filteredValue == null && value != null) {
                namedList.remove(i);
            } else if (filteredValue != value) {
                ((NamedList<Object>) namedList).setVal(i, filteredValue);
            }
        }
    }

    private Collection<?> filterCollection(Collection<?> collection)
    {
        // Create a new collection only when a value is filtered, as the given one could be immutable and most
        // collections are multivalued fields that don't contain any document.
        List<Object> filteredCollection = null;
        int index = 0;
        for (Object value : collection) {
            Object filteredValue = filterValue(value);
            if (filteredValue != value && filteredCollection == null) {
                filteredCollection = new ArrayList<>(collection.size());
                collection.stream().limit(index).forEach(filteredCollection::add);
            }
            if (filteredCollection != null && (filteredValue != null || value == null)) {
                filteredCollection.add(filteredValue);
            }
            index++;
        }

        return filteredCollection != null ? filteredCollection : collection;
    }

    private void filterDocumentList(SolrDocumentList results)
    {
        if (results == null) {
            return;
        }

        long numResults = results.size();

        // FIXME: We should update maxScore as well when removing the top scored item. How do we do that?
        // Sorting based on score might be a not so expensive option.

        // FIXME: What about facets and the other aggregated data inside the QueryResponse?
        results.removeIf(result -> !isKept(result));

        long numFilteredResults = numResults - results.size();
        if (numFilteredResults > 0) {
            this.removedDocuments = true;
            if (results.isEmpty()) {
                this.emptiedDocumentLists.add(results);
            }
        }

        // Update the number of results, excluding the filtered ones.
        // Lower bound guard for the total number of results.
        long numFound = Math.max(0, results.getNumFound() - numFilteredResults);

        results.setNumFound(numFound);
    }

    private boolean isKept(SolrDocument result)
    {
        boolean keep = false;
        try {
            DocumentReference resultDocumentReference = this.documentReferenceResolver.resolve(result);

            keep = isAllowed(resultDocumentReference);
        } catch (Exception e) {
            // Don't take any risk of including a result for which we cannot determine the document reference and
            // thus cannot determine if the given users have access to it or not.
            this.logger.warn("Removing bad result: [{}]", result, e);
        }

        if (keep) {
            Object id = result.getFieldValue(FieldUtils.ID);
            if (id != null) {
                this.keptIds.add(String.valueOf(id));
            }

            // Filter the documents nested in the field values and the child documents.
            Iterator<Entry<String, Object>> fieldIterator = result.entrySet().iterator();
            while (fieldIterator.hasNext()) {
                Entry<String, Object> field = fieldIterator.next();
                Object filteredValue = filterValue(field.getValue());
                if (filteredValue == null && field.getValue() != null) {
                    fieldIterator.remove();
                } else if (filteredValue != field.getValue()) {
                    field.setValue(filteredValue);
                }
            }
            if (result.hasChildDocuments()) {
                this.removedDocuments |= result.getChildDocuments().removeIf(child -> !isKept(child));
            }
        }

        return keep;
    }

    private boolean isAllowed(DocumentReference resultDocumentReference)
    {
        for (DocumentReference user : this.usersToCheck) {
            if (!this.authorization.hasAccess(Right.VIEW, user, resultDocumentReference)) {
                return false;
            }
        }

        return true;
    }

    private void filterGroups(NamedList<?> grouped)
    {
        for (Entry<String, ?> command : grouped) {
            if (command.getValue() instanceof NamedList<?> commandResult
                && commandResult.get("groups") instanceof List<?> groups) {
                groups.removeIf(group -> group instanceof NamedList<?> groupResult
                    && groupResult.get(DOCUMENT_LIST) instanceof SolrDocumentList documentList
                    && this.emptiedDocumentLists.contains(documentList));
            }
        }
    }

    private void removeEntries(NamedList<?> namedList, BiPredicate<String, Object> filter)
    {
        // The iterator of NamedList doesn't support removal.
        for (int i = namedList.size() - 1; i >= 0; i--) {
            if (filter.test(namedList.getName(i), namedList.getVal(i))) {
                namedList.remove(i);
            }
        }
    }
}
