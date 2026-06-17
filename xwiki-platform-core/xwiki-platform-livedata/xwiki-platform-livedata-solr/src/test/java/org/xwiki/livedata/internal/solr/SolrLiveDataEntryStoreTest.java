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
package org.xwiki.livedata.internal.solr;

import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.security.SecurityConfiguration;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SolrLiveDataEntryStore}.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@ComponentTest
class SolrLiveDataEntryStoreTest
{
    private static final String SOLR_LANGUAGE = "solr";

    @InjectMockComponents
    private SolrLiveDataEntryStore store;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private SolrLiveDataDocumentFormatter documentFormatter;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    private SolrUtils solrUtils;

    @MockComponent
    private SecurityConfiguration securityConfiguration;

    private SecureQuery mockEmptyQuery(String solrQueryString) throws Exception
    {
        SecureQuery solrQuery = mock(SecureQuery.class);
        when(this.queryManager.createQuery(solrQueryString, SOLR_LANGUAGE)).thenReturn(solrQuery);
        UserProperties userProperties = mock(UserProperties.class);
        when(this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userProperties);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        when(solrQuery.execute()).thenReturn(List.of(response));
        // A generous configured limit so the (initialized) limit of 15 is accepted.
        when(this.securityConfiguration.getQueryItemsLimit()).thenReturn(1000);
        return solrQuery;
    }

    @SuppressWarnings("unchecked")
    private List<String> captureFilterQueries(SecureQuery solrQuery)
    {
        ArgumentCaptor<Object> fqCaptor = ArgumentCaptor.forClass(Object.class);
        verify(solrQuery).bindValue(eq("fq"), fqCaptor.capture());
        return (List<String>) fqCaptor.getValue();
    }

    @Test
    void getMapsDocumentsToEntries() throws Exception
    {
        SecureQuery solrQuery = mock(SecureQuery.class);
        when(this.queryManager.createQuery("links_extended:foo", SOLR_LANGUAGE)).thenReturn(solrQuery);
        UserProperties userProperties = mock(UserProperties.class);
        when(this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userProperties);
        when(this.securityConfiguration.getQueryItemsLimit()).thenReturn(1000);

        SolrDocument document = new SolrDocument();
        // The Solr document id is the idProperty of the source, always read from the "id" field.
        document.setField("id", "xwiki:Space.Page_en");
        // The title is read from the generic "title_" field (aggregates all locales).
        document.setField("title_", "My Title");
        document.setField("fullname", "Space.Page");
        SolrDocumentList documents = new SolrDocumentList();
        documents.add(document);
        documents.setNumFound(42);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(documents);
        when(solrQuery.execute()).thenReturn(List.of(response));
        // The view URL is built by the document formatter (which resolves the reference).
        when(this.documentFormatter.getDocumentUrl(document)).thenReturn("/xwiki/bin/view/Space/Page");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        query.setProperties(List.of("doc.title", "doc.fullName"));
        Source source = new Source("solr");
        source.setParameter("query", "links_extended:foo");
        query.setSource(source);

        LiveData liveData = this.store.get(query);

        assertEquals(42, liveData.getCount());
        assertEquals(1, liveData.getEntries().size());
        Map<String, Object> entry = liveData.getEntries().get(0);
        assertEquals("My Title", entry.get("doc.title"));
        assertEquals("/xwiki/bin/view/Space/Page", entry.get("doc.url"));
        // The Solr document id is always present since it is the idProperty of the source.
        assertEquals("xwiki:Space.Page_en", entry.get("doc.id"));
        // The full name is present because it was among the requested columns.
        assertEquals("Space.Page", entry.get("doc.fullName"));

        verify(solrQuery).checkCurrentUser(true);
    }

    @Test
    void getTranslatesFiltersAndSort() throws Exception
    {
        SecureQuery solrQuery = mockEmptyQuery("*:*");
        // The current user did not opt to display hidden documents, so they must be excluded (default mock: false).
        // The "contains" operator is applied per word (split on whitespace), each word surrounded by wildcards.
        when(this.solrUtils.toFilterQueryString("hello")).thenReturn("hello");
        when(this.solrUtils.toFilterQueryString("world")).thenReturn("world");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        query.getFilters().add(new Filter("doc.title", "contains", "hello world"));
        query.getSort().add(new SortEntry("doc.title", true));
        query.setOffset(30L);
        query.setLimit(15);

        this.store.get(query);

        // The title property maps to the generic "title_" field; each word becomes a *word* substring clause, AND-ed.
        assertEquals(List.of("type:(\"DOCUMENT\")", "hidden:(false)", "(title_:*hello* AND title_:*world*)"),
            captureFilterQueries(solrQuery));

        verify(solrQuery).bindValue("sort", "title_sort desc");
        verify(solrQuery).setOffset(30);
        verify(solrQuery).setLimit(15);
    }

    @Test
    void getTranslatesContainsAsSubstringForSingleWord() throws Exception
    {
        SecureQuery solrQuery = mockEmptyQuery("*:*");
        when(this.solrUtils.toFilterQueryString("Ba")).thenReturn("Ba");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        // A partial term must match: "Ba" becomes the substring query title_:*Ba* (Solr lowercases wildcard terms on
        // the tokenized title field, so it matches "Banana").
        query.getFilters().add(new Filter("doc.title", "contains", "Ba"));

        this.store.get(query);

        assertEquals(List.of("type:(\"DOCUMENT\")", "hidden:(false)", "(title_:*Ba*)"),
            captureFilterQueries(solrQuery));
    }

    @Test
    void getTranslatesFullNameContainsAgainstNameAndFullName() throws Exception
    {
        SecureQuery solrQuery = mockEmptyQuery("*:*");
        when(this.solrUtils.toFilterQueryString("Ba")).thenReturn("Ba");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        // The full name column is filtered against the tokenized (case-insensitive) "name" field and the "fullname"
        // string field, OR-ed, so a lowercase partial term still matches the page name.
        query.getFilters().add(new Filter("doc.fullName", "contains", "Ba"));

        this.store.get(query);

        assertEquals(List.of("type:(\"DOCUMENT\")", "hidden:(false)", "((name:*Ba* OR fullname:*Ba*))"),
            captureFilterQueries(solrQuery));
    }

    @Test
    void getTranslatesFullNameMultiWordContainsAsAndOfOrGroups() throws Exception
    {
        SecureQuery solrQuery = mockEmptyQuery("*:*");
        when(this.solrUtils.toFilterQueryString("ba")).thenReturn("ba");
        when(this.solrUtils.toFilterQueryString("na")).thenReturn("na");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        query.getFilters().add(new Filter("doc.fullName", "contains", "ba na"));

        this.store.get(query);

        // Each word becomes an OR group over the two fields, and the words are AND-ed together.
        assertEquals(
            List.of("type:(\"DOCUMENT\")", "hidden:(false)",
                "((name:*ba* OR fullname:*ba*) AND (name:*na* OR fullname:*na*))"),
            captureFilterQueries(solrQuery));
    }

    @Test
    void getTranslatesFullNameEqualsAsPhraseOnNameAndFullName() throws Exception
    {
        SecureQuery solrQuery = mockEmptyQuery("*:*");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        query.getFilters().add(new Filter("doc.fullName", "equals", "Space.Page"));

        this.store.get(query);

        assertEquals(
            List.of("type:(\"DOCUMENT\")", "hidden:(false)", "((name:\"Space.Page\" OR fullname:\"Space.Page\"))"),
            captureFilterQueries(solrQuery));
    }

    @Test
    void getTranslatesDateBeforeAndAfterAsOpenRanges() throws Exception
    {
        SecureQuery solrQuery = mockEmptyQuery("*:*");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        // A timezone offset is normalized to a UTC instant; "before" and "after" become open-ended ranges.
        query.getFilters().add(new Filter("doc.date", "after", "2023-06-15T14:30:45+02:00"));
        query.getFilters().add(new Filter("doc.creationDate", "before", "2023-06-15T00:00:00Z"));

        this.store.get(query);

        assertEquals(
            List.of("type:(\"DOCUMENT\")", "hidden:(false)",
                "(date:[2023-06-15T12:30:45Z TO *])",
                "(creationdate:[* TO 2023-06-15T00:00:00Z])"),
            captureFilterQueries(solrQuery));
    }

    @Test
    void getTranslatesDateBetweenWithOffsetlessValue() throws Exception
    {
        SecureQuery solrQuery = mockEmptyQuery("*:*");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        // A value without a timezone offset is assumed to be UTC rather than dropped (which would disable the filter).
        query.getFilters().add(new Filter("doc.date", "between", "2023-01-01T00:00:00/2023-12-31T23:59:59"));

        this.store.get(query);

        assertEquals(
            List.of("type:(\"DOCUMENT\")", "hidden:(false)",
                "(date:[2023-01-01T00:00:00Z TO 2023-12-31T23:59:59Z])"),
            captureFilterQueries(solrQuery));
    }

    @Test
    void getTranslatesEqualsOperatorAsPhrase() throws Exception
    {
        SecureQuery solrQuery = mockEmptyQuery("*:*");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        query.getFilters().add(new Filter("doc.title", "equals", "My Title"));

        this.store.get(query);

        // The "equals" operator produces an exact phrase query (no SolrUtils tokenization).
        assertEquals(List.of("type:(\"DOCUMENT\")", "hidden:(false)", "(title_:\"My Title\")"),
            captureFilterQueries(solrQuery));
    }

    @Test
    void getTranslatesStartsWithOperatorAsPrefix() throws Exception
    {
        SecureQuery solrQuery = mockEmptyQuery("*:*");
        when(this.solrUtils.toFilterQueryString("hel")).thenReturn("hel");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        query.getFilters().add(new Filter("doc.title", "startsWith", "hel"));

        this.store.get(query);

        // The "startsWith" operator appends a trailing wildcard to the escaped value (prefix query).
        assertEquals(List.of("type:(\"DOCUMENT\")", "hidden:(false)", "(title_:hel*)"),
            captureFilterQueries(solrQuery));
    }

    @Test
    void getTranslatesDateBetweenAsRange() throws Exception
    {
        SecureQuery solrQuery = mockEmptyQuery("*:*");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        // The live data date filter serializes a "between" range as two ISO 8601 instants separated by a slash.
        query.getFilters().add(
            new Filter("doc.creationDate", "between", "2023-01-01T00:00:00Z/2023-12-31T23:59:59Z"));

        this.store.get(query);

        assertEquals(
            List.of("type:(\"DOCUMENT\")", "hidden:(false)",
                "(creationdate:[2023-01-01T00:00:00Z TO 2023-12-31T23:59:59Z])"),
            captureFilterQueries(solrQuery));
    }

    @Test
    void getDefaultsLimitWhenNotSpecified() throws Exception
    {
        SecureQuery solrQuery = mock(SecureQuery.class);
        when(this.queryManager.createQuery("*:*", SOLR_LANGUAGE)).thenReturn(solrQuery);
        UserProperties userProperties = mock(UserProperties.class);
        when(this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userProperties);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        when(solrQuery.execute()).thenReturn(List.of(response));

        // A query with no offset/limit specified (not initialized to defaults).
        LiveDataQuery query = new LiveDataQuery();
        query.setSource(new Source("solr"));

        LiveData liveData = this.store.get(query);

        // An empty Solr result is mapped to an empty live data (count 0, no entries).
        assertEquals(0, liveData.getCount());
        assertTrue(liveData.getEntries().isEmpty());
        // A null limit must not degrade to Solr's small default: it falls back to the live data default page size.
        verify(solrQuery).setLimit(15);
        verify(solrQuery).setOffset(0);
    }

    @Test
    void getThrowsWhenLimitExceedsConfiguredMaximum() throws Exception
    {
        SecureQuery solrQuery = mock(SecureQuery.class);
        when(this.queryManager.createQuery("*:*", SOLR_LANGUAGE)).thenReturn(solrQuery);
        UserProperties userProperties = mock(UserProperties.class);
        when(this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userProperties);
        when(this.securityConfiguration.getQueryItemsLimit()).thenReturn(1000);

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        query.setLimit(2000);

        // A limit larger than the configured maximum is rejected to avoid denial of service.
        assertThrows(LiveDataException.class, () -> this.store.get(query));
    }

    @Test
    void getThrowsOnUnsupportedEntityType()
    {
        // Only the "document" entity type is supported for now; any other value is rejected.
        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        Source source = new Source("solr");
        source.setParameter("type", "attachment");
        query.setSource(source);

        assertThrows(LiveDataException.class, () -> this.store.get(query));
    }
}
