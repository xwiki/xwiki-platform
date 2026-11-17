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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SolrResponseRightsFilter}.
 *
 * @version $Id$
 */
@ComponentTest
class SolrResponseRightsFilterTest
{
    private static final String NESTED = "nested";

    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "User");

    private static final List<DocumentReference> USERS_TO_CHECK = List.of(USER);

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private SolrResponseRightsFilter filter;

    @MockComponent
    private DocumentReferenceResolver<SolrDocument> documentReferenceResolver;

    @MockComponent
    private AuthorizationManager authorization;

    @Test
    void filterResultsWithoutRawResponse()
    {
        SolrDocument allowed = createDocument("Allowed", true);
        SolrDocument other = createDocument("Other", false);
        SolrDocumentList results = documentList(allowed, other);
        results.setNumFound(10);

        QueryResponse response = mock();
        when(response.getResults()).thenReturn(results);

        this.filter.filter(response, USERS_TO_CHECK);

        assertEquals(List.of(allowed), results);
        assertEquals(9, results.getNumFound());
    }

    @Test
    void filterNestedDocuments()
    {
        SolrDocument parent = createDocument("Parent", true);
        SolrDocument viewableNested = createDocument("ViewableNested", true);
        SolrDocument otherNested = createDocument("OtherNested", false);
        SolrDocument deeplyNested = createDocument("DeeplyNested", false);
        SolrDocument singleNested = createDocument("SingleNested", false);
        SolrDocument listNested = createDocument("ListNested", false);
        SolrDocument viewableChild = createDocument("ViewableChild", true);
        SolrDocument otherChild = createDocument("OtherChild", false);

        // Documents nested in a document list, e.g., added by the [subquery] document transformer.
        SolrDocumentList nestedList = documentList(viewableNested, otherNested);
        nestedList.setNumFound(5);
        parent.setField(NESTED, nestedList);

        // A document nested in a document that is nested itself.
        viewableNested.setField(NESTED, documentList(deeplyNested));

        parent.setField("single", singleNested);
        parent.setField("mixed", new ArrayList<>(List.of("value", listNested, viewableNested)));
        // The collection is replaced only when a value is removed, so immutable collections are supported.
        SolrDocument otherListNested = createDocument("OtherListNested", false);
        parent.setField("immutable", List.of("value1", "value2", otherListNested, "value3"));
        List<String> plainValues = List.of("value1", "value2");
        parent.setField("plain", plainValues);
        parent.addChildDocuments(List.of(viewableChild, otherChild));

        SolrDocumentList results = documentList(parent);
        QueryResponse response = mock();
        when(response.getResults()).thenReturn(results);

        this.filter.filter(response, USERS_TO_CHECK);

        assertEquals(List.of(parent), results);
        assertEquals(1, results.getNumFound());

        SolrDocumentList filteredNestedList = (SolrDocumentList) parent.getFieldValue(NESTED);
        assertEquals(List.of(viewableNested), filteredNestedList);
        assertEquals(4, filteredNestedList.getNumFound());

        SolrDocumentList filteredDeeplyNestedList = (SolrDocumentList) viewableNested.getFieldValue(NESTED);
        assertEquals(List.of(), filteredDeeplyNestedList);
        assertEquals(0, filteredDeeplyNestedList.getNumFound());

        assertFalse(parent.containsKey("single"));
        assertNull(parent.getFieldValue("single"));
        assertEquals(List.of("value", viewableNested), parent.getFieldValue("mixed"));
        assertEquals(List.of("value1", "value2", "value3"), parent.getFieldValue("immutable"));
        assertSame(plainValues, parent.getFieldValue("plain"));
        assertEquals(List.of(viewableChild), parent.getChildDocuments());
    }

    @Test
    void filterNestedDocumentsWithoutReference()
    {
        SolrDocument parent = createDocument("Parent", true);
        SolrDocument nestedWithoutReference = new SolrDocument();
        nestedWithoutReference.setField("title_", "Title");
        when(this.documentReferenceResolver.resolve(nestedWithoutReference))
            .thenThrow(new IllegalArgumentException("Missing reference fields"));
        parent.setField(NESTED, documentList(nestedWithoutReference));

        QueryResponse response = mock();
        when(response.getResults()).thenReturn(documentList(parent));

        this.filter.filter(response, USERS_TO_CHECK);

        assertEquals(List.of(), parent.getFieldValue(NESTED));
        assertEquals("Removing bad result: [SolrDocument{title_=Title}]", this.logCapture.getMessage(0));
    }

    @Test
    void filterAdditionalResults()
    {
        SolrDocument viewable = createDocument("Viewable", true);
        SolrDocument other = createDocument("Other", false);
        SolrDocument viewableGrouped = createDocument("ViewableGrouped", true);
        SolrDocument otherGrouped = createDocument("OtherGrouped", false);
        SolrDocument viewableSimilar = createDocument("ViewableSimilar", true);
        SolrDocument otherSimilar = createDocument("OtherSimilar", false);
        SolrDocument viewableExpanded = createDocument("ViewableExpanded", true);
        SolrDocument otherExpanded = createDocument("OtherExpanded", false);
        SolrDocument viewableCustom = createDocument("ViewableCustom", true);
        SolrDocument otherCustom = createDocument("OtherCustom", false);

        NamedList<Object> rawResponse = new NamedList<>();
        rawResponse.add("response", documentList(viewable, other));

        NamedList<Object> group = new SimpleOrderedMap<>();
        group.add("groupValue", "group");
        group.add("doclist", documentList(viewableGrouped, otherGrouped));
        NamedList<Object> groupCommand = new SimpleOrderedMap<>();
        groupCommand.add("matches", 2);
        groupCommand.add("groups", new ArrayList<>(List.of(group)));
        NamedList<Object> grouped = new SimpleOrderedMap<>();
        grouped.add("field", groupCommand);
        rawResponse.add("grouped", grouped);

        NamedList<Object> expanded = new SimpleOrderedMap<>();
        expanded.add("first", documentList(viewableExpanded, otherExpanded));
        expanded.add("second", documentList(otherExpanded));
        rawResponse.add("expanded", expanded);

        NamedList<Object> moreLikeThis = new SimpleOrderedMap<>();
        moreLikeThis.add("Viewable", documentList(viewableSimilar, otherSimilar));
        moreLikeThis.add("Other", documentList(viewableSimilar));
        rawResponse.add("moreLikeThis", moreLikeThis);

        NamedList<Object> highlighting = new SimpleOrderedMap<>();
        highlighting.add("Viewable", new SimpleOrderedMap<>());
        highlighting.add("ViewableGrouped", new SimpleOrderedMap<>());
        highlighting.add("Other", new SimpleOrderedMap<>());
        highlighting.add("OtherGrouped", new SimpleOrderedMap<>());
        rawResponse.add("highlighting", highlighting);

        NamedList<Object> explain = new SimpleOrderedMap<>();
        explain.add("Viewable", "explanation");
        explain.add("Other", "explanation");
        NamedList<Object> debug = new SimpleOrderedMap<>();
        debug.add("explain", explain);
        rawResponse.add("debug", debug);

        // A section that isn't known by the filter.
        NamedList<Object> custom = new SimpleOrderedMap<>();
        custom.add("documents", new ArrayList<>(List.of(viewableCustom, otherCustom)));
        rawResponse.add("custom", custom);

        QueryResponse response = new QueryResponse();
        response.setResponse(rawResponse);

        this.filter.filter(response, USERS_TO_CHECK);

        assertEquals(List.of(viewable), response.getResults());
        assertEquals(1, response.getResults().getNumFound());
        assertEquals(List.of(viewableGrouped),
            response.getGroupResponse().getValues().getFirst().getValues().getFirst().getResult());
        assertEquals(List.of("first"), List.copyOf(response.getExpandedResults().keySet()));
        assertEquals(List.of(viewableExpanded), response.getExpandedResults().get("first"));
        assertEquals(1, response.getMoreLikeThis().size());
        assertEquals(List.of(viewableSimilar), response.getMoreLikeThis().get("Viewable"));
        assertEquals(Set.of("Viewable", "ViewableGrouped"), response.getHighlighting().keySet());
        assertEquals(Set.of("Viewable"), response.getExplainMap().keySet());
        assertEquals(List.of(viewableCustom), response.getResponse().findRecursive("custom", "documents"));
    }

    @Test
    void filterValuesOfRemovedDocuments()
    {
        SolrDocument viewable = createDocument("Viewable", true);
        SolrDocument other = createDocument("Other", false);

        NamedList<Object> rawResponse = new NamedList<>();
        rawResponse.add("response", documentList(viewable, other));

        NamedList<Object> sortValues = new SimpleOrderedMap<>();
        sortValues.add("title_sort", new ArrayList<>(List.of("viewable title", "other title")));
        rawResponse.add("sort_values", sortValues);

        NamedList<Object> interestingTerms = new SimpleOrderedMap<>();
        interestingTerms.add("Viewable", List.of("viewable"));
        interestingTerms.add("Other", List.of("other"));
        rawResponse.add("interestingTerms", interestingTerms);

        NamedList<Object> viewableGroup = group("viewable value", documentList(createDocument("Grouped", true)));
        NamedList<Object> otherGroup = group("other value", documentList(createDocument("OtherGrouped", false)));
        // A group without documents, e.g., when the number of documents per group is 0.
        NamedList<Object> emptyGroup = group("empty value", documentList());
        NamedList<Object> groupCommand = new SimpleOrderedMap<>();
        groupCommand.add("matches", 3);
        groupCommand.add("groups", new ArrayList<>(List.of(viewableGroup, otherGroup, emptyGroup)));
        NamedList<Object> grouped = new SimpleOrderedMap<>();
        grouped.add("field", groupCommand);
        rawResponse.add("grouped", grouped);

        QueryResponse response = new QueryResponse();
        response.setResponse(rawResponse);

        this.filter.filter(response, USERS_TO_CHECK);

        assertEquals(List.of(viewable), response.getResults());
        assertNull(response.getResponse().get("sort_values"));
        assertEquals(1, interestingTerms.size());
        assertEquals("Viewable", interestingTerms.getName(0));
        assertEquals(List.of(viewableGroup, emptyGroup), groupCommand.get("groups"));
    }

    @Test
    void keepSortValuesWhenNoDocumentIsRemoved()
    {
        NamedList<Object> rawResponse = new NamedList<>();
        rawResponse.add("response", documentList(createDocument("Viewable", true)));
        NamedList<Object> sortValues = new SimpleOrderedMap<>();
        sortValues.add("title_sort", new ArrayList<>(List.of("viewable title")));
        rawResponse.add("sort_values", sortValues);

        QueryResponse response = new QueryResponse();
        response.setResponse(rawResponse);

        this.filter.filter(response, USERS_TO_CHECK);

        assertSame(sortValues, response.getResponse().get("sort_values"));
    }

    @Test
    void keepNullValues()
    {
        SolrDocument viewable = createDocument("Viewable", true);
        SolrDocument other = createDocument("Other", false);
        viewable.setField("empty", null);
        viewable.setField("values", new ArrayList<>(Arrays.asList("value", null, other)));

        NamedList<Object> rawResponse = new NamedList<>();
        rawResponse.add("response", documentList(viewable));
        // The stats component returns null for the fields that don't have any value.
        NamedList<Object> statsFields = new SimpleOrderedMap<>();
        statsFields.add("field", null);
        NamedList<Object> stats = new SimpleOrderedMap<>();
        stats.add("stats_fields", statsFields);
        rawResponse.add("stats", stats);

        QueryResponse response = new QueryResponse();
        response.setResponse(rawResponse);

        this.filter.filter(response, USERS_TO_CHECK);

        assertEquals(List.of(viewable), response.getResults());
        assertTrue(viewable.containsKey("empty"));
        assertEquals(Arrays.asList("value", null), viewable.getFieldValue("values"));
        assertEquals(1, statsFields.size());
        assertNull(statsFields.get("field"));
    }

    private NamedList<Object> group(String value, SolrDocumentList documentList)
    {
        NamedList<Object> group = new SimpleOrderedMap<>();
        group.add("groupValue", value);
        group.add("doclist", documentList);
        return group;
    }

    private SolrDocumentList documentList(SolrDocument... documents)
    {
        SolrDocumentList documentList = new SolrDocumentList();
        documentList.addAll(List.of(documents));
        documentList.setNumFound(documents.length);
        return documentList;
    }

    private SolrDocument createDocument(String name, boolean allowed)
    {
        SolrDocument document = new SolrDocument();
        document.setField("id", name);
        document.setField("name", name);
        DocumentReference documentReference = new DocumentReference("wiki", "Space", name);
        when(this.documentReferenceResolver.resolve(document)).thenReturn(documentReference);
        when(this.authorization.hasAccess(Right.VIEW, USER, documentReference)).thenReturn(allowed);
        return document;
    }
}
