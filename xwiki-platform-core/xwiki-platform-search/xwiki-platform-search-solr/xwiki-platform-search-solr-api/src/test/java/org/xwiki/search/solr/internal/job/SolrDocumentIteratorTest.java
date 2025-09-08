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
package org.xwiki.search.solr.internal.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SolrDocumentIterator}.
 * 
 * @version $Id$
 * @since 5.4.5
 */
@ComponentTest
class SolrDocumentIteratorTest
{
    @MockComponent
    private SolrReferenceResolver resolver;

    @MockComponent
    private SolrInstance solrInstance;

    @MockComponent
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @MockComponent
    private SolrConfiguration configuration;

    @InjectMockComponents
    private SolrDocumentIterator solrIterator;

    @Test
    void size() throws Exception
    {
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(12L);

        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(results);

        when(this.solrInstance.query(any(SolrQuery.class))).thenReturn(response);

        DocumentIterator<String> iterator = this.solrIterator;

        WikiReference rootReference = new WikiReference("wiki");
        iterator.setRootReference(rootReference);

        assertEquals(12, iterator.size());

        verify(this.resolver).getQuery(rootReference);
    }

    @Test
    void sizeWithException()
    {
        assertThrows(IllegalStateException.class, () -> this.solrIterator.size(), "Failed to query the Solr index.");
    }

    @Test
    void iterate() throws Exception
    {
        int limit = 42;
        when(this.configuration.getSynchronizationBatchSize()).thenReturn(limit);

        SolrDocumentList firstResults = new SolrDocumentList();
        firstResults.add(createSolrDocument("chess", Arrays.asList("A", "B"), "C", "", "1.3"));
        firstResults.add(createSolrDocument("chess", Arrays.asList("M"), "N", "en", "2.4"));

        QueryResponse firstResponse = mock(QueryResponse.class);
        when(firstResponse.getNextCursorMark()).thenReturn("foo");
        when(firstResponse.getResults()).thenReturn(firstResults);

        SolrDocumentList secondResults = new SolrDocumentList();
        secondResults.add(createSolrDocument("tennis", Arrays.asList("X", "Y", "Z"), "V", "fr", "1.1"));

        QueryResponse secondResponse = mock(QueryResponse.class);
        when(secondResponse.getNextCursorMark()).thenReturn("bar");
        when(secondResponse.getResults()).thenReturn(secondResults);

        when(this.solrInstance.query(any(SolrQuery.class))).thenReturn(firstResponse, secondResponse, secondResponse);

        DocumentIterator<String> iterator = this.solrIterator;

        WikiReference rootReference = new WikiReference("wiki");
        iterator.setRootReference(rootReference);

        List<Pair<DocumentReference, String>> actualResult = new ArrayList<>();
        while (iterator.hasNext()) {
            actualResult.add(iterator.next());
        }

        assertThrows(NoSuchElementException.class, iterator::next);

        verify(this.resolver).getQuery(rootReference);

        List<Pair<DocumentReference, String>> expectedResult = new ArrayList<>();
        DocumentReference documentReference = new DocumentReference("chess", Arrays.asList("A", "B"), "C");
        expectedResult.add(new ImmutablePair<>(documentReference, "1.3"));
        documentReference = new DocumentReference("chess", Arrays.asList("M"), "N", Locale.ENGLISH);
        expectedResult.add(new ImmutablePair<>(documentReference, "2.4"));
        documentReference = new DocumentReference("tennis", Arrays.asList("X", "Y", "Z"), "V", Locale.FRENCH);
        expectedResult.add(new ImmutablePair<>(documentReference, "1.1"));

        assertEquals(expectedResult, actualResult);

        verify(this.solrInstance, times(4)).query(argThat(query ->
            query instanceof SolrQuery solrQuery && solrQuery.getRows() == limit));
    }

    private SolrDocument createSolrDocument(String wiki, List<String> spaces, String name, String locale,
        String version)
    {
        SolrDocument doc = new SolrDocument();
        DocumentReference docRef = new DocumentReference(wiki, spaces, name);
        if (!StringUtils.isEmpty(locale)) {
            docRef = new DocumentReference(docRef, LocaleUtils.toLocale(locale));
        }
        when(this.solrDocumentReferenceResolver.resolve(doc)).thenReturn(docRef);
        doc.setField(FieldUtils.VERSION, version);
        return doc;
    }
}
