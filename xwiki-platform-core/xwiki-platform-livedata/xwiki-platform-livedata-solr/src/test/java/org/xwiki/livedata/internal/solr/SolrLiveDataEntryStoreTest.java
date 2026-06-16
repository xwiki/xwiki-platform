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
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SolrLiveDataEntryStore}.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@ComponentTest
class SolrLiveDataEntryStoreTest
{
    @InjectMockComponents
    private SolrLiveDataEntryStore store;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    private SolrUtils solrUtils;

    @Test
    void getMapsDocumentsToEntries() throws Exception
    {
        SecureQuery solrQuery = mock(SecureQuery.class);
        when(this.queryManager.createQuery("links_extended:foo", "solr")).thenReturn(solrQuery);
        UserProperties userProperties = mock(UserProperties.class);
        when(this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userProperties);

        SolrDocument document = new SolrDocument();
        // The title is read from the generic "title_" field (aggregates all locales).
        document.setField("title_", "My Title");
        // The full name is the idProperty of the source, always read from the "fullname" field.
        document.setField("fullname", "Space.Page");
        SolrDocumentList documents = new SolrDocumentList();
        documents.add(document);
        documents.setNumFound(42);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(documents);
        when(solrQuery.execute()).thenReturn(List.of(response));

        DocumentReference reference = new DocumentReference("xwiki", "Space", "Page");
        when(this.solrDocumentReferenceResolver.resolve(document)).thenReturn(reference);
        when(this.documentAccessBridge.getDocumentURL(reference, "view", null, null))
            .thenReturn("/xwiki/bin/view/Space/Page");

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        query.setProperties(List.of("doc.title"));
        Source source = new Source("solr");
        source.setParameter("query", "links_extended:foo");
        query.setSource(source);

        LiveData liveData = this.store.get(query);

        assertEquals(42, liveData.getCount());
        assertEquals(1, liveData.getEntries().size());
        Map<String, Object> entry = liveData.getEntries().get(0);
        assertEquals("My Title", entry.get("doc.title"));
        assertEquals("/xwiki/bin/view/Space/Page", entry.get("url"));
        // The full name is always present since it is the idProperty of the source.
        assertEquals("Space.Page", entry.get("doc.fullName"));

        verify(solrQuery).checkCurrentUser(true);
    }

    @Test
    void getTranslatesFiltersAndSort() throws Exception
    {
        SecureQuery solrQuery = mock(SecureQuery.class);
        when(this.queryManager.createQuery("*:*", "solr")).thenReturn(solrQuery);
        // The current user did not opt to display hidden documents, so they must be excluded.
        UserProperties userProperties = mock(UserProperties.class);
        when(userProperties.displayHiddenDocuments()).thenReturn(false);
        when(this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userProperties);
        // SolrUtils escapes the value (here the space), without introducing wildcards.
        when(this.solrUtils.toFilterQueryString("hello world")).thenReturn("hello\\ world");

        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        when(solrQuery.execute()).thenReturn(List.of(response));

        LiveDataQuery query = new LiveDataQuery();
        query.initialize();
        query.getFilters().add(new Filter("doc.title", "contains", "hello world"));
        query.getSort().add(new SortEntry("doc.title", true));
        query.setOffset(30L);
        query.setLimit(15);

        this.store.get(query);

        // The title property maps to the generic "title_" Solr field and matches via a tokenized query (no wildcard).
        ArgumentCaptor<Object> fqCaptor = ArgumentCaptor.forClass(Object.class);
        verify(solrQuery).bindValue(eq("fq"), fqCaptor.capture());
        @SuppressWarnings("unchecked")
        List<String> filterQueries = (List<String>) fqCaptor.getValue();
        assertEquals(List.of("type:(\"DOCUMENT\")", "hidden:(false)", "(title_:hello\\ world)"), filterQueries);

        verify(solrQuery).bindValue("sort", "title_sort desc");
        verify(solrQuery).setOffset(30);
        verify(solrQuery).setLimit(15);
    }

    @Test
    void getDefaultsLimitWhenNotSpecified() throws Exception
    {
        SecureQuery solrQuery = mock(SecureQuery.class);
        when(this.queryManager.createQuery("*:*", "solr")).thenReturn(solrQuery);
        UserProperties userProperties = mock(UserProperties.class);
        when(this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userProperties);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        when(solrQuery.execute()).thenReturn(List.of(response));

        // A query with no offset/limit specified (not initialized to defaults).
        LiveDataQuery query = new LiveDataQuery();
        query.setSource(new Source("solr"));

        this.store.get(query);

        // A null limit must not degrade to Solr's small default: it falls back to the live data default page size.
        verify(solrQuery).setLimit(15);
        verify(solrQuery).setOffset(0);
    }
}
