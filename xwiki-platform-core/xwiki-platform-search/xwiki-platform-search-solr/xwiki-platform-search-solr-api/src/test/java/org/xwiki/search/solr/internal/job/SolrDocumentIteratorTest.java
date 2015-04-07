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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link SolrDocumentIterator}.
 * 
 * @version $Id$
 * @since 5.4.5
 */
public class SolrDocumentIteratorTest
{
    @Rule
    public MockitoComponentMockingRule<DocumentIterator<String>> mocker =
        new MockitoComponentMockingRule<DocumentIterator<String>>(SolrDocumentIterator.class);

    private SolrInstance solr;

    @Before
    public void configure() throws Exception
    {
        solr = mock(SolrInstance.class);

        Provider<SolrInstance> solrInstanceProvider =
            mocker.registerMockComponent(new DefaultParameterizedType(null, Provider.class, SolrInstance.class));
        when(solrInstanceProvider.get()).thenReturn(solr);
    }

    @Test
    public void size() throws Exception
    {
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(12L);

        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(results);

        when(solr.query(any(SolrQuery.class))).thenReturn(response);

        DocumentIterator<String> iterator = mocker.getComponentUnderTest();

        WikiReference rootReference = new WikiReference("wiki");
        iterator.setRootReference(rootReference);

        assertEquals(12, iterator.size());

        SolrReferenceResolver resolver = mocker.getInstance(SolrReferenceResolver.class);
        verify(resolver).getQuery(rootReference);
    }

    @Test
    public void sizeWithException() throws Exception
    {
        assertEquals(0, mocker.getComponentUnderTest().size());
    }

    @Test
    public void iterate() throws Exception
    {
        SolrDocumentList firstResults = new SolrDocumentList();
        firstResults.add(createSolrDocument("chess", "A", "B", "", "1.3"));
        firstResults.add(createSolrDocument("chess", "M", "N", "en", "2.4"));

        QueryResponse firstResponse = mock(QueryResponse.class);
        when(firstResponse.getNextCursorMark()).thenReturn("foo");
        when(firstResponse.getResults()).thenReturn(firstResults);

        SolrDocumentList secondResults = new SolrDocumentList();
        secondResults.add(createSolrDocument("tennis", "X", "Y", "fr", "1.1"));

        QueryResponse secondResponse = mock(QueryResponse.class);
        when(secondResponse.getNextCursorMark()).thenReturn("bar");
        when(secondResponse.getResults()).thenReturn(secondResults);

        when(solr.query(any(SolrQuery.class))).thenReturn(firstResponse, secondResponse, secondResponse);

        DocumentIterator<String> iterator = mocker.getComponentUnderTest();

        WikiReference rootReference = new WikiReference("wiki");
        iterator.setRootReference(rootReference);

        List<Pair<DocumentReference, String>> actualResult = new ArrayList<Pair<DocumentReference, String>>();
        while (iterator.hasNext()) {
            actualResult.add(iterator.next());
        }

        SolrReferenceResolver resolver = mocker.getInstance(SolrReferenceResolver.class);
        verify(resolver).getQuery(rootReference);

        List<Pair<DocumentReference, String>> expectedResult = new ArrayList<Pair<DocumentReference, String>>();
        DocumentReference documentReference = new DocumentReference("chess", "A", "B");
        expectedResult.add(new ImmutablePair<DocumentReference, String>(documentReference, "1.3"));
        documentReference = new DocumentReference("chess", "M", "N", "en");
        expectedResult.add(new ImmutablePair<DocumentReference, String>(documentReference, "2.4"));
        documentReference = new DocumentReference("tennis", "X", "Y", "fr");
        expectedResult.add(new ImmutablePair<DocumentReference, String>(documentReference, "1.1"));

        assertEquals(expectedResult, actualResult);
    }

    private SolrDocument createSolrDocument(String wiki, String space, String name, String locale, String version)
    {
        SolrDocument doc = new SolrDocument();
        doc.setField(FieldUtils.WIKI, wiki);
        doc.setField(FieldUtils.SPACE, space);
        doc.setField(FieldUtils.NAME, name);
        doc.setField(FieldUtils.DOCUMENT_LOCALE, locale);
        doc.setField(FieldUtils.VERSION, version);
        return doc;
    }
}
